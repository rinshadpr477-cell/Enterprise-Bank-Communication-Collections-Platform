package com.ford.bank.communication.domain;

/**
 * Communication job lifecycle states.
 *
 * PENDING: Created, waiting to be picked up by the scheduler
 * QUEUED: Submitted to Kafka; not yet processed by a consumer
 * IN_PROGRESS: Consumer is actively processing
 * COMPLETED: Successfully delivered
 * FAILED: Attempt failed; may be eligible for retry
 * RETRYING: Scheduled for retry with backoff
 * CANCELLED: Manually cancelled; no further processing
 * UNCERTAIN: Provider accepted the request but no delivery confirmation received.
 *            Do NOT retry blindly - could result in duplicate delivery.
 *            Requires reconciliation through provider status lookup or manual review.
 * SUPPRESSED: Contact preference or suppression list prevented dispatch
 */
public enum JobStatus {
    PENDING,
    QUEUED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    RETRYING,
    CANCELLED,
    UNCERTAIN,
    SUPPRESSED
}
