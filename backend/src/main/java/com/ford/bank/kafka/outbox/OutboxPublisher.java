package com.ford.bank.kafka.outbox;

import com.ford.bank.kafka.producer.EventProducer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled outbox publisher.
 *
 * Algorithm:
 * 1. Query for unpublished records (using SKIP LOCKED to allow multiple instances)
 * 2. Lock the records by setting locked_by = this instance ID
 * 3. For each record: publish to Kafka, then mark as published in DB
 *
 * Crash window: between step 3's Kafka ack and DB update, a crash will cause
 * republication. Consumers use ProcessedEvent to handle this safely.
 *
 * Publisher idempotence (enable.idempotence=true) prevents broker-side duplicates
 * within a single producer session but does NOT prevent duplicates across crashes
 * or consumer database boundaries. Consumers must still handle at-least-once delivery.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final EventProducer eventProducer;
    private final MeterRegistry meterRegistry;

    @Value("${app.outbox.batch-size:100}")
    private int batchSize;

    @Value("${app.outbox.lock-timeout-seconds:30}")
    private int lockTimeoutSeconds;

    private final String instanceId = UUID.randomUUID().toString();

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("outbox.backlog", outboxEventRepository, OutboxEventRepository::countUnpublished)
                .description("Number of unpublished outbox events")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    public void publishPendingEvents() {
        OffsetDateTime lockExpiry = OffsetDateTime.now().minusSeconds(lockTimeoutSeconds);

        List<OutboxEvent> events = claimBatch(lockExpiry);
        if (events.isEmpty()) {
            return;
        }

        log.info("OutboxPublisher[{}] publishing {} events", instanceId, events.size());
        int published = 0;
        int failed = 0;

        for (OutboxEvent event : events) {
            try {
                eventProducer.send(event);
                markPublished(event.getId());
                published++;
            } catch (Exception e) {
                log.error("Failed to publish outbox event id={} type={}: {}",
                        event.getId(), event.getEventType(), e.getMessage(), e);
                incrementAttempts(event.getId());
                failed++;
            }
        }

        log.info("OutboxPublisher cycle complete: published={} failed={}", published, failed);
        meterRegistry.counter("outbox.published").increment(published);
        meterRegistry.counter("outbox.publish_failed").increment(failed);
    }

    @Transactional
    public List<OutboxEvent> claimBatch(OffsetDateTime lockExpiry) {
        List<OutboxEvent> events = outboxEventRepository.findAndClaimUnpublished(lockExpiry, batchSize);
        if (!events.isEmpty()) {
            List<UUID> ids = events.stream().map(OutboxEvent::getId).toList();
            outboxEventRepository.lockForPublisher(ids, instanceId, OffsetDateTime.now());
        }
        return events;
    }

    @Transactional
    public void markPublished(UUID eventId) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.markPublished();
            outboxEventRepository.save(event);
        });
    }

    @Transactional
    public void incrementAttempts(UUID eventId) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.incrementAttempts();
            event.setLockedBy(null);
            event.setLockedAt(null);
            outboxEventRepository.save(event);
        });
    }
}
