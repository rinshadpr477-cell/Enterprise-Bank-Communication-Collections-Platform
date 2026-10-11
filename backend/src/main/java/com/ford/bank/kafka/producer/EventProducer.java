package com.ford.bank.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ford.bank.kafka.event.EventEnvelope;
import com.ford.bank.kafka.outbox.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Kafka event producer.
 * Routes events to topics based on event type.
 * Uses synchronous send with timeout to detect failures during outbox publishing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventProducer {

    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.campaign-scheduled}")
    private String campaignScheduledTopic;

    @Value("${app.kafka.topics.job-created}")
    private String jobCreatedTopic;

    @Value("${app.kafka.topics.job-requested}")
    private String jobRequestedTopic;

    @Value("${app.kafka.topics.attempt-completed}")
    private String attemptCompletedTopic;

    @Value("${app.kafka.topics.attempt-failed}")
    private String attemptFailedTopic;

    @Value("${app.kafka.topics.agent-task-created}")
    private String agentTaskCreatedTopic;

    @Value("${app.kafka.topics.collection-case-updated}")
    private String collectionCaseUpdatedTopic;

    /**
     * Send an outbox event to the appropriate Kafka topic.
     * Blocks until broker acknowledges (acks=all) with a 10-second timeout.
     * Throws an exception on failure so the outbox publisher can retry.
     */
    @SuppressWarnings("unchecked")
    public void send(OutboxEvent outboxEvent) {
        Map<String, Object> payload = outboxEvent.getPayload();

        EventEnvelope envelope = EventEnvelope.of(
                outboxEvent.getEventType(),
                outboxEvent.getAggregateType(),
                outboxEvent.getAggregateId(),
                (String) payload.getOrDefault("correlationId", null),
                payload
        );

        String topic = resolveTopic(outboxEvent.getEventType());
        String key = outboxEvent.getAggregateId();

        try {
            SendResult<String, EventEnvelope> result = kafkaTemplate
                    .send(topic, key, envelope)
                    .get(10, TimeUnit.SECONDS);
            log.debug("Published event id={} type={} to topic={} partition={} offset={}",
                    outboxEvent.getId(), outboxEvent.getEventType(), topic,
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
        } catch (TimeoutException e) {
            throw new RuntimeException("Kafka send timed out for event " + outboxEvent.getId(), e);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Kafka send failed for event " + outboxEvent.getId(), e);
        }
    }

    private String resolveTopic(String eventType) {
        String normalized = eventType != null ? eventType.toUpperCase().replace("_", "") : "";
        return switch (normalized) {
            case "CAMPAIGNSCHEDULED" -> campaignScheduledTopic;
            case "COMMUNICATIONJOBCREATED" -> jobCreatedTopic;
            case "COMMUNICATIONJOBREQUESTED" -> jobRequestedTopic;
            case "COMMUNICATIONATTEMPTCOMPLETED", "COMMUNICATIONJOBCOMPLETED" -> attemptCompletedTopic;
            case "COMMUNICATIONATTEMPTFAILED", "COMMUNICATIONJOBFAILED" -> attemptFailedTopic;
            case "AGENTTASKCREATED" -> agentTaskCreatedTopic;
            case "COLLECTIONCASEUPDATED", "COLLECTIONCASECREATED" -> collectionCaseUpdatedTopic;
            default -> {
                log.warn("No topic mapping for event type: {}; using job-created as fallback", eventType);
                yield jobCreatedTopic;
            }
        };
    }
}
