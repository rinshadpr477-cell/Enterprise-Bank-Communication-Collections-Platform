# Operations & Engineering Runbook

## 1. Quick Start (Local Development)

### Prerequisites
- Java 17 or Java 21
- Apache Maven 3.9+
- Docker and Docker Compose

### 1.1 Start Infrastructure Services
```bash
docker compose up -d postgres kafka
```
Verify that PostgreSQL and Kafka containers are healthy:
```bash
docker compose ps
```

### 1.2 Run Database Migrations
Migrations are managed automatically via Flyway on application startup.
Flyway will execute `V1` through `V6` scripts located in `src/main/resources/db/migration`:
- `V1`: Customers & contact preferences
- `V2`: Campaigns & targets
- `V3`: Communication jobs & attempts
- `V4`: Collection cases & agent tasks
- `V5`: Kafka outbox, idempotency dedup, audit records, and webhook nonces
- `V6`: Seed reference data and high-performance partial indexes

### 1.3 Launch the Spring Boot Backend
```bash
cd backend
mvn spring-boot:run
```

Once started:
- **Application URL:** `http://localhost:8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/api-docs`
- **Actuator Health:** `http://localhost:8080/actuator/health`

---

## 2. Testing & Verification

### Running Automated Unit & Integration Tests
```bash
mvn clean test
```

### Viewing Code Coverage
```bash
# Jacoco generates HTML reports in:
backend/target/site/jacoco/index.html
```

---

## 3. Production Deployment Checklist

| Domain | Production Requirement | Setting |
| :--- | :--- | :--- |
| **Security** | Disable Ephemeral Dev Keys | `SECURITY_DEV_MODE=false` |
| **Identity** | Configure Corporate OAuth2 Provider | `JWT_ISSUER_URI=https://auth.bank.com` |
| **CORS** | Restrict Allowed Origins | `CORS_ALLOWED_ORIGINS=https://portal.bank.com` |
| **Database** | Configure HikariCP Max Connections | `spring.datasource.hikari.maximum-pool-size=30` |
| **Kafka** | Enable SSL & SCRAM-SHA-512 | `spring.kafka.properties.security.protocol=SSL` |
| **Auditing** | Ensure Log Shipping to SIEM | Ship logstash-formatted JSON logs to Splunk/ELK |

---

## 4. Troubleshooting & Diagnostics

### Outbox Events Stalled / Not Publishing
1. Check `outbox_events` table for stale locks:
   ```sql
   SELECT id, event_type, locked_by, locked_at, publish_attempts 
   FROM outbox_events 
   WHERE published_at IS NULL;
   ```
2. Check if Kafka broker is reachable from the publisher node.
3. If an instance crashed holding a lock, `OutboxPublisher` automatically releases locks older than 30 seconds.

### Telephony / SMS Provider Timeouts
- If provider times out, communication jobs enter `UNCERTAIN` status.
- **Do not blindly replay immediately.** Await the callback webhook or query provider status endpoint to verify if the call was placed before retrying.

### Duplicate Event Rejection
- When a Kafka consumer re-delivers a message, `ProcessedEventRepository` detects the existing `(event_id, consumer_name)` primary key and acknowledges the broker without re-executing business effects.
