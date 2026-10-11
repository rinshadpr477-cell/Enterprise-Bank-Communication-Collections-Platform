package com.ford.bank.kafka.consumer;

import com.ford.bank.kafka.event.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Communication job event consumer.
 *
 * Idempotency approach:
 * 1. Extract eventId from the envelope
 * 2. Within a transaction, attempt to insert into processed_events
 * 3. If the insert fails with a unique constraint violation, the event was already processed - skip
 * 4. If insert succeeds, process the event and commit both changes atomically
 *
 * This handles at-least-once delivery safely.
 * It does NOT provide exactly-once semantics across external provider calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommunicationJobConsumer {

    private static final String CONSUMER_NAME = "CommunicationJobConsumer";

    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(
            topics = "${app.kafka.topics.job-requested}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onJobRequested(ConsumerRecord<String, EventEnvelope> record, Acknowledgment ack) {
        EventEnvelope envelope = record.value();

        if (envelope == null) {
            log.warn("Received null event envelope on topic={} partition={} offset={}",
                    record.topic(), record.partition(), record.offset());
            ack.acknowledge();
            return;
        }

        String eventId = envelope.eventId();
        log.debug("Received event id={} type={}", eventId, envelope.eventType());

        try {
            // Idempotency check: if already processed, skip
            if (processedEventRepository.existsByEventIdAndConsumerName(eventId, CONSUMER_NAME)) {
                log.info("Duplicate event id={} - already processed by {}. Skipping.", eventId, CONSUMER_NAME);
                ack.acknowledge();
                return;
            }

            // Record this event as processed (in the same transaction as business logic)
            processedEventRepository.save(new ProcessedEvent(eventId, CONSUMER_NAME));

            // Business logic: additional handling can be added here
            log.info("Processed job-requested event id={} aggregateId={}", eventId, envelope.aggregateId());

            ack.acknowledge();

        } catch (DataIntegrityViolationException e) {
            // Race condition: two consumers processed the same event simultaneously
            // The unique constraint prevented double-processing - treat as success
            log.info("Concurrent duplicate event id={} - handled safely by constraint", eventId);
            ack.acknowledge();
        } catch (Exception e) {
            // Do not acknowledge - message will be redelivered
            log.error("Failed to process event id={}: {}", eventId, e.getMessage(), e);
            throw e; // Spring Kafka error handler will manage retry/DLT
        }
    }
}
