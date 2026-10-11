# Enterprise Bank Communication & Collections Platform - Architecture Specification

## 1. System Overview

The **Enterprise Bank Communication & Collections Platform** is an event-driven, fault-tolerant banking backend system engineered to orchestrate large-scale customer communication campaigns, multi-channel notifications (Voice call requests and SMS), collections case management, and human-agent recovery workflows.

Built on **Spring Boot 3.2**, **PostgreSQL 16**, and **Apache Kafka**, the platform enforces enterprise data integrity, asynchronous decoupling, regulatory compliance (TRAI/DLT and RBI guidelines), and zero-loss message processing through the **Transactional Outbox Pattern**.

```
+----------------------------------------------------------------------------------------------------+
|                                    CLIENT APPLICATION / API CONSUMERS                              |
|                          (Admin Console, Campaign Managers, Collections Agents)                    |
+----------------------------------------------------------------------------------------------------+
                                                  |
                                            HTTPS / REST (JWT Bearer)
                                                  v
+----------------------------------------------------------------------------------------------------+
|                                      SPRING BOOT CORE PLATFORM                                     |
|                                                                                                    |
|  +--------------------+  +----------------------+  +---------------------+  +--------------------+ |
|  |  Customer Module   |  |   Campaign Module    |  | Communication Engine|  | Collections Module | |
|  | - Preference mgmt  |  | - Target cohorts     |  | - Multi-channel IVR |  | - Delinquent cases | |
|  | - PII masking      |  | - State transitions  |  | - Jittered backoff  |  | - Agent tasks queue| |
|  | - Suppression list |  | - Quota verification |  | - Replay protection |  | - High-precision  | |
|  +--------------------+  +----------------------+  +---------------------+  +--------------------+ |
|            |                         |                        |                        |           |
|            +-------------------------+------------------------+------------------------+           |
|                                                  |                                                 |
|                                      MANDATORY @Transactional                                      |
|                                                  v                                                 |
|                              +---------------------------------------+                             |
|                              |      Transactional Outbox Table       |                             |
|                              |  (Atomic commit with business entities)|                             |
|                              +---------------------------------------+                             |
+----------------------------------------------------------------------------------------------------+
                   |                                                              |
    FOR UPDATE SKIP LOCKED                                                 PostgreSQL 16
                   v                                                              |
+------------------------------------+                                            |
|       Outbox Event Publisher       |                                            |
|   (Crash-resilient poll & batch)   |                                            |
+------------------------------------+                                            |
                   |                                                              |
              Kafka acks=all                                                      |
                   v                                                              v
+------------------------------------+                             +-----------------------------+
|        Apache Kafka Cluster        |                             |     PostgreSQL Database     |
| - bank.campaign.scheduled          |                             | - customers                 |
| - bank.communication.job.requested |                             | - contact_preferences       |
| - bank.communication.job.created   |                             | - suppression_entries       |
| - bank.collection.case.updated     |                             | - campaigns & targets       |
| - bank.agent.task.created          |                             | - communication_jobs        |
| - bank.dead.letter (DLT)           |                             | - communication_attempts    |
+------------------------------------+                             | - collection_cases          |
                   |                                               | - agent_tasks               |
     @KafkaListener (Idempotent)                                   | - outbox_events             |
                   v                                               | - processed_events (dedup)  |
+------------------------------------+                             | - audit_records (immutable) |
|   Asynchronous Event Consumers     |---------------------------->| - webhook_nonces            |
+------------------------------------+                             +-----------------------------+
                   |
       Provider Integrations
                   v
+------------------------------------+
|  Telephony / SMS Gateway Adapters  |
| - Voice Provider (IVR)             |
| - SMS Provider (DLT compliant)     |
+------------------------------------+
```

---

## 2. Core Architectural Pillars

### 2.1 Transactional Outbox Pattern
Directly publishing events to Kafka during an HTTP request introduces the **Dual-Write Hazard**:
- If the database commit succeeds but the network drops before Kafka receives the message, data is lost.
- If Kafka succeeds but the database rollback triggers, invalid phantom events exist on the broker.

**Platform Solution:**
1. Business logic mutations and outbox event insertions execute within the same database transaction (`Propagation.MANDATORY` in `OutboxService`).
2. An autonomous background publisher (`OutboxPublisher`) polls pending events using `FOR UPDATE SKIP LOCKED`.
3. Events are published to Kafka with synchronous acknowledgment (`acks=all`).
4. Upon broker ack, the record is flagged with `published_at = NOW()`.
5. If the publisher crashes between Kafka ack and DB commit, events are republished. Consumers maintain consumer-side idempotency (`processed_events` table).

### 2.2 Concurrency & Scheduler Safety via SKIP LOCKED
High-throughput banking schedulers run across multiple horizontal replicas:
- `communication_jobs` table supports concurrent claiming through:
  ```sql
  SELECT * FROM communication_jobs
  WHERE status IN ('PENDING', 'RETRYING')
    AND (scheduled_at IS NULL OR scheduled_at <= :now)
    AND (next_retry_at IS NULL OR next_retry_at <= :now)
  ORDER BY scheduled_at ASC NULLS FIRST, id ASC
  LIMIT :batchSize
  FOR UPDATE SKIP LOCKED;
  ```
- Each scheduler instance obtains a non-overlapping slice of due jobs with zero row-lock contention.

### 2.3 Strict State Machine Transitions
Campaigns and Communication Jobs follow strict state machines:
- **Campaign States:** `DRAFT -> SCHEDULED -> RUNNING -> PAUSED -> COMPLETED / CANCELLED`.
  Invalid transitions (such as `DRAFT` directly to `COMPLETED`) raise `InvalidStateTransitionException` and are rejected before any database modification.
- **Communication Job States:** `PENDING -> QUEUED -> IN_PROGRESS -> DELIVERED / COMPLETED / RETRYING / FAILED / UNCERTAIN / SUPPRESSED`.
- **Handling Provider Uncertainty:** When an external telephony provider times out, the attempt is marked `TIMEOUT` and job enters `UNCERTAIN` until a signed callback webhook arrives. This prevents duplicate robocalls to customers.

### 2.4 Replay Attack Protection for Webhooks
Provider status callbacks accept incoming delivery notifications:
- Each webhook request requires a unique `nonce` and timestamp.
- The platform records nonces in `webhook_nonces`. Any repeated nonce within its expiration window is immediately rejected with HTTP 409 Conflict.

### 2.5 Monetary Precision & Collections Integrity
- In accordance with standard financial accounting principles, currency values are never stored as floating-point numbers.
- `outstanding_amount` uses `NUMERIC(19,4)` in PostgreSQL and `java.math.BigDecimal` in the Java domain layer.
- ISO 4217 currency codes are stored alongside all balance fields.

---

## 3. Data Governance & Security

### 3.1 Role-Based Access Control (RBAC)
Stateless JWT Bearer authentication verifies caller claims:
- `ROLE_ADMIN`: Full system management, campaign and customer configuration.
- `ROLE_CAMPAIGN_MANAGER`: Campaign creation, target cohort definitions, scheduling.
- `ROLE_COLLECTION_AGENT`: Collection cases and agent workqueue resolution.
- `ROLE_AUDITOR`: Read-only access to immutable compliance records and system metrics.

### 3.2 Data Minimization & Privacy
- Phone numbers returned in API responses are masked (`****3210`) by `CustomerMapper`.
- Full PII, payment card details, and credentials are strictly excluded from Audit records and Kafka payloads.
- Contact suppression lists (`suppression_entries`) are verified immediately prior to communication dispatch to enforce customer opt-outs and regulatory DND restrictions.
