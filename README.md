# Enterprise Bank Communication & Collections Platform

[![Java](https://img.shields.io/badge/Java-17%2F21-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-KRaft-231F20?style=flat&logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![Flyway](https://img.shields.io/badge/Flyway-10.10-CC0200?style=flat&logo=flyway&logoColor=white)](https://flywaydb.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)

A production-grade, event-driven banking communication and recovery platform designed for enterprise, high-throughput financial environments. Engineered to orchestrate customer outreach campaigns, automated voice calls (IVR), multi-channel notifications (SMS), collection case lifecycles, and human-agent recovery workqueues with zero data loss.

---

## 🏛 Key Engineering Highlights

- **Transactional Outbox Pattern**: Solves dual-write hazards across relational persistence and Apache Kafka. Events are committed atomically with business entities and dispatched by an autonomous lock-free publisher (`FOR UPDATE SKIP LOCKED`).
- **Consumer-Side Idempotency**: Deduplicates event deliveries using composite tracking keys in `processed_events`, ensuring exactly-once processing semantics across at-least-once message pipelines.
- **Concurrent-Safe Schedulers**: Eliminates distributed locking overhead by leveraging PostgreSQL `FOR UPDATE SKIP LOCKED` for batch job claims across horizontal cluster nodes.
- **Strict State Machine Enforcements**: Campaigns, jobs, and collection cases transition strictly through defined lifecycle states, preventing invalid intermediate states.
- **Financial Precision**: All monetary balances use `NUMERIC(19,4)` in PostgreSQL and `BigDecimal` in Java, guaranteeing zero floating-point rounding errors.
- **Replay-Protected Webhook Callbacks**: Validates unique cryptographic nonces for incoming delivery receipts to thwart replay attacks.
- **Data Minimization & Compliance**: Automatic PII masking (`CustomerMapper`), active suppression list checks prior to communication dispatch, and immutable regulatory audit trail logging (`audit_records`).

---

## 📁 Project Architecture

```
ford/
├── .github/
│   └── workflows/
│       └── ci.yml               # Automated GitHub Actions build & test pipeline
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/ford/bank/
│   │   │   │   ├── audit/           # Audit trail logging & inspection
│   │   │   │   ├── campaign/        # Campaign orchestration & cohort targeting
│   │   │   │   ├── collections/     # Delinquent loan cases & agent task queue
│   │   │   │   ├── common/          # Global exceptions, standard page envelopes
│   │   │   │   ├── communication/   # Jobs, attempts, schedulers & webhooks
│   │   │   │   ├── customer/        # Customer profiles, contacts & suppressions
│   │   │   │   ├── kafka/           # Outbox publisher, consumers & event envelopes
│   │   │   │   ├── provider/        # Voice IVR & SMS gateway adapters
│   │   │   │   └── security/        # JWT stateless auth & dev token dispenser
│   │   │   └── resources/
│   │   │       ├── application.yml  # Application properties & Kafka topic maps
│   │   │       └── db/migration/    # Flyway versioned SQL migrations (V1 - V6)
│   │   └── test/java/com/ford/bank/ # Unit tests (JUnit 5, Mockito)
│   ├── Dockerfile                   # Multi-stage production container build
│   └── pom.xml                      # Maven configuration with Jacoco coverage
├── docs/
│   ├── ARCHITECTURE.md              # In-depth architectural blueprint & diagrams
│   ├── API_DOCUMENTATION.md         # Complete REST API specifications & contracts
│   └── RUNBOOK.md                   # Operations guide, deployment & troubleshooting
├── docker-compose.yml               # PostgreSQL 16, Kafka KRaft, and App orchestration
└── README.md                        # Project documentation
```

---

##  Getting Started

### 1. Launch Infrastructure
```bash
docker compose up -d postgres kafka
```

### 2. Run the Application
```bash
cd backend
mvn spring-boot:run
```

The application will start on port `8080`.

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **API Docs:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)
- **Actuator Health:** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

### 3. Generate a Test JWT Token
```bash
curl -X POST "http://localhost:8080/api/v1/auth/token?username=admin&roles=ADMIN,CAMPAIGN_MANAGER,COLLECTION_AGENT"
```

Use the returned token in the `Authorization: Bearer <TOKEN>` header for subsequent requests.

---

##  Testing & Verification

Run automated test suites with code coverage generation:
```bash
cd backend
mvn clean test
```

Jacoco test coverage reports are generated at:
`backend/target/site/jacoco/index.html`

---

## 📖 Detailed Documentation

- [Architecture Blueprint](file:///docs/ARCHITECTURE.md)
- [REST API Specifications](file:///docs/API_DOCUMENTATION.md)
- [Engineering Runbook](file:///docs/RUNBOOK.md)
