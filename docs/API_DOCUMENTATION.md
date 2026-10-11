# REST API Documentation

The platform provides OpenAPI 3.0 documented REST endpoints available at `/swagger-ui/index.html` and `/api-docs`.

## Base URL
`http://localhost:8080/api/v1`

## Authentication
All requests (except `/api/v1/auth/**`, `/actuator/**`, and webhook callbacks) require an `Authorization: Bearer <JWT>` header.

In development mode (`SECURITY_DEV_MODE=true`), you can generate test tokens via:
```http
POST /api/v1/auth/token?username=admin-user&roles=ADMIN,CAMPAIGN_MANAGER,COLLECTION_AGENT,AUDITOR
```

---

## 1. Customer Management (`/api/v1/customers`)

### Create Customer
```http
POST /api/v1/customers
Content-Type: application/json
Authorization: Bearer <TOKEN>

{
  "customerReference": "CUST-987654",
  "fullName": "Rajesh Kumar",
  "email": "rajesh.kumar@example.com",
  "phoneNumber": "+919876543210",
  "preferredLanguage": "en"
}
```
**Response (201 Created):**
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "customerReference": "CUST-987654",
  "fullName": "Rajesh Kumar",
  "email": "rajesh.kumar@example.com",
  "phoneNumber": "****3210",
  "preferredLanguage": "en",
  "status": "ACTIVE",
  "createdAt": "2026-10-11T08:00:00Z",
  "updatedAt": "2026-10-11T08:00:00Z"
}
```

### List Customers
```http
GET /api/v1/customers?status=ACTIVE&search=Rajesh&page=0&size=20
```

---

## 2. Campaign Management (`/api/v1/campaigns`)

### Create Campaign
```http
POST /api/v1/campaigns
Content-Type: application/json

{
  "name": "Q4 Delinquency Outreach",
  "campaignType": "PAYMENT_REMINDER",
  "description": "Targeting 30-day overdue loan accounts",
  "scheduledAt": "2026-10-15T09:00:00Z"
}
```

### State Transitions
- **Schedule:** `POST /api/v1/campaigns/{id}/schedule?scheduledAt=2026-10-15T09:00:00Z`
- **Pause:** `POST /api/v1/campaigns/{id}/pause`
- **Resume:** `POST /api/v1/campaigns/{id}/resume`
- **Cancel:** `POST /api/v1/campaigns/{id}/cancel`

### Add Campaign Targets (Batch)
```http
POST /api/v1/campaigns/{id}/targets/batch
Content-Type: application/json

[
  "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "8a78e722-124b-4a52-9214-e053f316bf82"
]
```

---

## 3. Communication Jobs (`/api/v1/communication`)

### Create Communication Job
```http
POST /api/v1/communication/jobs
Content-Type: application/json

{
  "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "campaignId": "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed",
  "channel": "VOICE",
  "scheduledAt": "2026-10-11T10:00:00Z",
  "maxAttempts": 3,
  "idempotencyKey": "DISPATCH-JOB-20261011-001"
}
```

### Trigger Dispatch
```http
POST /api/v1/communication/jobs/{id}/dispatch
```

### Provider Webhook Status Callback (Replay Protected)
```http
POST /api/v1/communication/webhooks/callback
Content-Type: application/json

{
  "provider": "TELEPHONY_PROVIDER",
  "providerReference": "SIM-VOICE-A1B2C3D4",
  "eventType": "CALL_STATUS_UPDATE",
  "status": "DELIVERED",
  "failureCode": null,
  "failureReason": null,
  "nonce": "NONCE-550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1791696000000,
  "signature": "sha256=abc..."
}
```

---

## 4. Collections & Agent Workqueue (`/api/v1/collections`)

### Create Collection Case
```http
POST /api/v1/collections/cases
Content-Type: application/json

{
  "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "externalAccountReference": "LOAN-AUTO-4091",
  "outstandingAmount": 45250.7500,
  "currency": "INR",
  "dueDate": "2026-10-05",
  "priority": "HIGH",
  "notes": "EMI missed for October 2026"
}
```

### Assign & Update Case
```http
PATCH /api/v1/collections/cases/{id}
Content-Type: application/json

{
  "status": "IN_PROGRESS",
  "assignedAgentId": "AGENT-401",
  "priority": "CRITICAL"
}
```

### Human-Agent Task Workqueue
```http
GET /api/v1/collections/tasks?agentId=AGENT-401&status=ASSIGNED&page=0&size=20
```

---

## 5. Audit & Compliance Inspection (`/api/v1/audit`)
```http
GET /api/v1/audit?entityType=Campaign&entityId=1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed
```
