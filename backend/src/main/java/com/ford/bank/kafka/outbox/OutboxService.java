package com.ford.bank.kafka.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Outbox service for publishing events transactionally.
 *
 * Usage: Call publishEvent() within the same database transaction as your
 * business operation. Both the business change and the outbox record will
 * be committed atomically. The OutboxPublisher scheduler will then send
 * the event to Kafka and mark it as published.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;

    /**
     * Create an outbox record in the current transaction.
     * MANDATORY propagation ensures this runs within the caller's transaction.
     * If no transaction exists, an exception is thrown immediately.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishEvent(String aggregateType, String aggregateId,
                              String eventType, Map<String, Object> payload) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(payload);
        outboxEventRepository.save(event);
        log.debug("Outbox event queued: type={} aggregateId={}", eventType, aggregateId);
    }
}
