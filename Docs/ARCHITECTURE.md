# ARCHITECTURE.md — Argent System Architecture

---

## High-Level System Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      CLIENT APPLICATIONS                        │
│   Food App   Marketplace   Gaming   Ride Sharing   SaaS         │
└───────────────────────────────┬─────────────────────────────────┘
                                │ REST API + API Key Auth
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API GATEWAY / LOAD BALANCER                │
│                     (Nginx — V1 single instance)                │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SPRING BOOT APPLICATION                       │
│                   (Modular Monolith)                            │
│                                                                 │
│  ┌─────────────┐ ┌──────────────┐ ┌───────────────┐            │
│  │   Auth       │ │   Wallet     │ │  Transaction  │            │
│  │   Module     │ │   Module     │ │  Module       │            │
│  └──────┬──────┘ └──────┬───────┘ └───────┬───────┘            │
│         │               │                  │                     │
│  ┌──────┴──────┐ ┌──────┴───────┐ ┌───────┴───────┐            │
│  │   Ledger    │ │   Balance    │ │   Audit       │            │
│  │   Module    │ │   Module     │ │   Module      │            │
│  └──────┬──────┘ └──────┬───────┘ └───────┬───────┘            │
│         │               │                  │                     │
│  ┌──────┴──────┐ ┌──────┴───────┐ ┌───────┴───────┐            │
│  │   Webhook   │ │   Reporting  │ │   Identity    │            │
│  │   Module    │ │   Module     │ │   Module      │            │
│  └─────────────┘ └──────────────┘ └───────────────┘            │
│                                                                 │
└───────────┬───────────────┬───────────────┬─────────────────────┘
            │               │               │
            ▼               ▼               ▼
┌───────────────┐ ┌───────────────┐ ┌───────────────┐
│  PostgreSQL   │ │    Redis      │ │   RabbitMQ    │
│  (Primary DB) │ │  (Cache +     │ │  (Async       │
│               │ │   Sessions)   │ │   Messaging)  │
└───────────────┘ └───────────────┘ └───────────────┘
                                              │
                                              ▼
                                     ┌───────────────┐
                                     │  Background   │
                                     │  Workers      │
                                     │  (Webhooks,   │
                                     │   Reports)    │
                                     └───────────────┘
```

### Request Flow

1. Client application sends HTTP request with API Key header.
2. Nginx routes to Spring Boot application.
3. Auth module validates API Key, resolves Organization.
4. Appropriate service module handles the business logic.
5. Ledger module creates immutable entries.
6. Balance module updates balances atomically.
7. Audit module records the action.
8. Response returned to client.
9. Async events published to RabbitMQ for webhooks and reporting.

---

## Tech Stack

| Layer | Technology | Justification |
|---|---|---|
| **Language** | Java 21 | Long-term support, strong typing, excellent ecosystem for financial systems, virtual threads for concurrency. |
| **Framework** | Spring Boot 3.x | Industry standard for Java backend. Excellent dependency injection, transaction management, and ecosystem. |
| **Database** | PostgreSQL 16 | ACID compliance is non-negotiable for financial data. JSONB for metadata. Mature, reliable, open source. |
| **Cache** | Redis 7 | Fast balance lookups, session storage, rate limiting counters. Simple and battle-tested. |
| **Message Queue** | RabbitMQ | Reliable async processing for webhooks and reporting. Supports acknowledgments and retries. |
| **Frontend** | React 18 + TypeScript | Large ecosystem, type safety, component-based. TypeScript catches errors at compile time. |
| **Styling** | Tailwind CSS | Utility-first, consistent design system, no CSS-in-JS runtime overhead. |
| **Build Tool** | Gradle | Flexible, faster than Maven for multi-module projects, good dependency management. |
| **API Docs** | OpenAPI 3.0 + SpringDoc | Auto-generated from code, always accurate, Swagger UI for exploration. |
| **Containerization** | Docker + Docker Compose | Reproducible environments, simple deployment for V1. |
| **CI/CD** | GitHub Actions | Free for open source, tight GitHub integration, sufficient for V1 scale. |

### Why Not Alternatives?

- **Kotlin over Java:** Java 21 with virtual threads closes most of Kotlin's concurrency advantages. Java's ecosystem is larger for financial systems. Can revisit for V2.
- **MongoDB over PostgreSQL:** Financial data requires ACID transactions and relational integrity. NoSQL introduces consistency risks that are unacceptable for ledger systems.
- **Kafka over RabbitMQ:** RabbitMQ is simpler to operate for V1 scale. Kafka adds complexity (partitioning, consumer groups) that isn't needed until 10K+ messages/second.
- **Next.js over plain React:** Argent's dashboard is an internal tool, not a public-facing website. SSR provides no value. Plain React with Vite is simpler and faster to develop.

---

## Folder / File Structure

```
argent/
├── backend/                          # Spring Boot application
│   ├── build.gradle.kts              # Root build config
│   ├── settings.gradle.kts           # Module definitions
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/argent/
│   │   │   │   ├── ArgentApplication.java          # Entry point
│   │   │   │   │
│   │   │   │   ├── common/                          # Shared utilities
│   │   │   │   │   ├── exception/                   # Global exceptions
│   │   │   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   │   │   ├── BadRequestException.java
│   │   │   │   │   │   ├── NotFoundException.java
│   │   │   │   │   │   ├── ConflictException.java
│   │   │   │   │   │   └── UnauthorizedException.java
│   │   │   │   │   ├── response/                    # Standard API response wrappers
│   │   │   │   │   │   ├── ApiResponse.java
│   │   │   │   │   │   └── PagedResponse.java
│   │   │   │   │   ├── util/                        # Utilities (IdempotencyKey, etc.)
│   │   │   │   │   └── config/                      # Cross-cutting configs
│   │   │   │   │       ├── SecurityConfig.java
│   │   │   │   │       ├── RedisConfig.java
│   │   │   │   │       └── RabbitMQConfig.java
│   │   │   │   │
│   │   │   │   ├── module/                          # Feature modules
│   │   │   │   │   ├── auth/                        # Authentication & authorization
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   └── security/                # JWT, API Key filters
│   │   │   │   │   │
│   │   │   │   │   ├── organization/                # Organization management
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   └── repository/
│   │   │   │   │   │
│   │   │   │   │   ├── wallet/                      # Wallet CRUD + management
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   └── repository/
│   │   │   │   │   │
│   │   │   │   │   ├── transaction/                 # Transaction processing
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   └── engine/                  # Transaction processing engine
│   │   │   │   │   │       ├── DepositEngine.java
│   │   │   │   │   │       ├── TransferEngine.java
│   │   │   │   │   │       ├── WithdrawalEngine.java
│   │   │   │   │   │       └── RefundEngine.java
│   │   │   │   │   │
│   │   │   │   │   ├── ledger/                      # Double-entry ledger
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   └── repository/
│   │   │   │   │   │
│   │   │   │   │   ├── balance/                     # Balance management
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   └── entity/
│   │   │   │   │   │
│   │   │   │   │   ├── audit/                       # Audit logging
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   └── repository/
│   │   │   │   │   │
│   │   │   │   │   ├── webhook/                     # Webhook management
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   └── repository/
│   │   │   │   │   │
│   │   │   │   │   └── reporting/                   # Reports and exports
│   │   │   │   │       ├── controller/
│   │   │   │   │       ├── service/
│   │   │   │   │       └── dto/
│   │   │   │   │
│   │   │   │   └── ArgentApplication.java
│   │   │   │
│   │   │   └── resources/
│   │   │       ├── application.yml                  # Main config
│   │   │       ├── application-dev.yml              # Dev profile
│   │   │       ├── application-test.yml             # Test profile
│   │   │       ├── db/migration/                    # Flyway migrations
│   │   │       │   ├── V1__create_organizations.sql
│   │   │       │   ├── V2__create_users.sql
│   │   │       │   ├── V3__create_api_keys.sql
│   │   │       │   ├── V4__create_wallets.sql
│   │   │       │   ├── V5__create_accounts.sql
│   │   │       │   ├── V6__create_transactions.sql
│   │   │       │   ├── V7__create_ledger_entries.sql
│   │   │       │   ├── V8__create_balances.sql
│   │   │       │   ├── V9__create_audit_logs.sql
│   │   │       │   └── V10__create_webhooks.sql
│   │   │       └── openapi.yaml                    # Generated API spec
│   │   │
│   │   └── test/
│   │       └── java/com/argent/
│   │           ├── module/
│   │           │   ├── auth/
│   │           │   ├── wallet/
│   │           │   ├── transaction/
│   │           │   ├── ledger/
│   │           │   └── balance/
│   │           └── integration/                     # Integration tests
│   │
│   └── docker-compose.yml                           # Local dev stack
│
├── frontend/                         # React dashboard
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   ├── tailwind.config.ts
│   ├── src/
│   │   ├── main.tsx                  # Entry point
│   │   ├── App.tsx                   # Root component + router
│   │   ├── api/                      # API client layer
│   │   │   ├── client.ts             # Axios/fetch wrapper
│   │   │   ├── wallets.ts
│   │   │   ├── transactions.ts
│   │   │   └── auth.ts
│   │   ├── components/               # Reusable UI components
│   │   │   ├── ui/                   # Base components (Button, Input, etc.)
│   │   │   ├── layout/               # Layout components (Sidebar, Header)
│   │   │   └── charts/               # Chart components for reporting
│   │   ├── pages/                    # Route-level pages
│   │   │   ├── Dashboard.tsx
│   │   │   ├── Wallets.tsx
│   │   │   ├── Transactions.tsx
│   │   │   ├── Ledger.tsx
│   │   │   ├── Settings.tsx
│   │   │   └── Login.tsx
│   │   ├── hooks/                    # Custom React hooks
│   │   ├── store/                    # State management (Zustand)
│   │   ├── types/                    # TypeScript type definitions
│   │   └── utils/                    # Utility functions
│   └── public/
│
├── docs/                             # Project documentation
│   ├── PRD.md
│   ├── ARCHITECTURE.md
│   ├── RULES.md
│   ├── PHASES.md
│   ├── DESIGN.md
│   ├── AGENTS.md
│   ├── MEMORY.md
│   └── PHASE_PROMPTS.md
│
├── .github/
│   └── workflows/
│       ├── ci.yml                    # Run tests on PR
│       └── deploy.yml                # Deploy on merge to main
│
├── .gitignore
├── .env.example                      # Environment variable template
└── README.md
```

### Module Boundaries

Each module in `backend/src/main/java/com/argent/module/` follows the same internal structure:

```
module/
├── controller/    # REST endpoints — thin, delegates to service
├── service/       # Business logic — the real work happens here
├── dto/           # Request/Response objects — never expose entities directly
├── entity/        # JPA entities — mapped to database tables
├── repository/    # Spring Data JPA repositories — data access only
└── engine/        # (transaction module only) — processing engines for each transaction type
```

**Rule:** Modules communicate through service interfaces, not direct repository calls. This keeps the module boundaries clean and allows future extraction into microservices.

---

## Data Model

### Entity Relationship Overview

```
Organization (1) ──── (N) User
Organization (1) ──── (N) ApiKey
Organization (1) ──── (N) Webhook
Organization (1) ──── (N) Wallet

Wallet (1) ──── (1) Account
Account (1) ──── (N) Transaction
Account (1) ──── (N) LedgerEntry
Account (1) ──── (1) Balance

Transaction (1) ──── (N) LedgerEntry
Transaction (1) ──── (N) AuditLog

Organization (1) ──── (N) AuditLog
```

### Entity Details

#### Organization

| Field | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| name | String | Organization name |
| slug | String | Unique identifier, URL-friendly |
| status | Enum | ACTIVE, SUSPENDED, CLOSED |
| createdAt | Timestamp | Immutable |
| updatedAt | Timestamp | Auto-updated |

#### User

| Field | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| organizationId | UUID | FK → Organization |
| email | String | Unique within organization |
| name | String | Display name |
| role | Enum | OWNER, ADMIN, DEVELOPER |
| passwordHash | String | BCrypt hash |
| status | Enum | ACTIVE, INVITED, DISABLED |
| createdAt | Timestamp | Immutable |

#### ApiKey

| Field | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| organizationId | UUID | FK → Organization |
| name | String | Human-readable label |
| keyHash | String | Hashed API key (never store raw) |
| keyPrefix | String | First 8 chars for identification |
| environment | Enum | SANDBOX, PRODUCTION |
| permissions | JSON | Scope of access |
| expiresAt | Timestamp | Optional |
| status | Enum | ACTIVE, REVOKED |
| createdAt | Timestamp | Immutable |

#### Wallet

| Field | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| organizationId | UUID | FK → Organization |
| accountId | UUID | FK → Account (internal) |
| label | String | Human-readable name |
| type | Enum | CUSTOMER, MERCHANT, ESCROW, REWARD, CREDIT, PLATFORM |
| status | Enum | ACTIVE, FROZEN, CLOSED |
| metadata | JSONB | Flexible key-value pairs (customerId, email, tags) |
| createdAt | Timestamp | Immutable |
| updatedAt | Timestamp | Auto-updated |

#### Account

| Field | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| organizationId | UUID | FK → Organization |
| type | Enum | ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE |
| name | String | Internal name |
| status | Enum | ACTIVE, FROZEN, CLOSED |
| createdAt | Timestamp | Immutable |

#### Transaction

| Field | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| organizationId | UUID | FK → Organization |
| type | Enum | DEPOSIT, WITHDRAWAL, TRANSFER, REFUND, ADJUSTMENT, FEE, COMMISSION, BONUS, REVERSAL |
| status | Enum | PENDING, COMPLETED, FAILED, CANCELLED |
| sourceWalletId | UUID | FK → Wallet (nullable for deposits) |
| destinationWalletId | UUID | FK → Wallet (nullable for withdrawals) |
| amount | BigDecimal | Always positive |
| idempotencyKey | String | Unique per organization |
| reference | String | External reference |
| description | String | Human-readable description |
| metadata | JSONB | Additional data |
| createdAt | Timestamp | Immutable |
| completedAt | Timestamp | When transaction finalized |

#### LedgerEntry

| Field | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| organizationId | UUID | FK → Organization |
| transactionId | UUID | FK → Transaction |
| accountId | UUID | FK → Account |
| type | Enum | DEBIT, CREDIT |
| amount | BigDecimal | Always positive |
| balanceAfter | BigDecimal | Running balance snapshot |
| description | String | Human-readable |
| createdAt | Timestamp | Immutable — never modified |

#### Balance

| Field | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| accountId | UUID | FK → Account (unique) |
| current | BigDecimal | Current balance |
| available | BigDecimal | Available for use |
| pending | BigDecimal | In-flight transactions |
| reserved | BigDecimal | Held for future use |
| updatedAt | Timestamp | Auto-updated |

#### AuditLog

| Field | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| organizationId | UUID | FK → Organization |
| entityType | String | e.g., "WALLET", "TRANSACTION" |
| entityId | UUID | ID of affected entity |
| action | String | e.g., "CREATED", "UPDATED", "FROZEN" |
| performedBy | UUID | User or API Key ID |
| previousState | JSONB | Snapshot before change |
| newState | JSONB | Snapshot after change |
| ipAddress | String | Request origin |
| userAgent | String | Client identifier |
| createdAt | Timestamp | Immutable |

#### Webhook

| Field | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| organizationId | UUID | FK → Organization |
| url | String | Endpoint to notify |
| events | JSON | List of event types to subscribe to |
| secret | String | For signature verification |
| status | Enum | ACTIVE, DISABLED |
| createdAt | Timestamp | Immutable |

---

## API Design

### Approach

REST API with JSON request/response bodies.

### Naming Conventions

- Resources are plural nouns: `/wallets`, `/transactions`, `/balances`
- Nested resources for scoping: `/wallets/{id}/transactions`
- Actions via POST to a sub-resource: `/transfers`, `/refunds`
- Query parameters for filtering: `?status=ACTIVE&type=CUSTOMER`

### Versioning

URL-based versioning: `/api/v1/wallets`

V1 is the only version during initial development. New versions introduced only for breaking changes.

### Standard Response Envelope

```json
{
  "success": true,
  "data": { },
  "error": null,
  "meta": {
    "page": 1,
    "pageSize": 20,
    "total": 150,
    "totalPages": 8
  }
}
```

### Error Response

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INSUFFICIENT_BALANCE",
    "message": "Wallet does not have sufficient balance for this transaction",
    "details": {
      "available": "50.00",
      "requested": "100.00"
    }
  }
}
```

### Idempotency

All write endpoints accept an `Idempotency-Key` header. If a request with the same key is received within 24 hours, the previous response is returned without re-executing the operation.

### Authentication

**Dashboard users:** JWT token in `Authorization: Bearer <token>` header.

**Client applications:** API key in `X-Api-Key` header. API keys are scoped to sandbox or production.

### Key Endpoints

```
POST   /api/v1/organizations                    # Create organization
GET    /api/v1/organizations/{id}               # Get organization

POST   /api/v1/auth/login                       # Login
POST   /api/v1/auth/register                    # Register

POST   /api/v1/api-keys                         # Generate API key
GET    /api/v1/api-keys                         # List API keys
DELETE /api/v1/api-keys/{id}                    # Revoke API key

POST   /api/v1/wallets                          # Create wallet
GET    /api/v1/wallets                          # List wallets
GET    /api/v1/wallets/{id}                     # Get wallet
PATCH  /api/v1/wallets/{id}                     # Update wallet metadata
POST   /api/v1/wallets/{id}/freeze              # Freeze wallet
POST   /api/v1/wallets/{id}/unfreeze            # Unfreeze wallet
POST   /api/v1/wallets/{id}/close               # Close wallet

POST   /api/v1/deposits                         # Deposit funds
POST   /api/v1/transfers                        # Transfer between wallets
POST   /api/v1/withdrawals                      # Withdraw funds
POST   /api/v1/refunds                          # Refund a transaction
POST   /api/v1/adjustments                      # Manual adjustment

GET    /api/v1/transactions                     # List transactions
GET    /api/v1/transactions/{id}                # Get transaction

GET    /api/v1/balances/{walletId}              # Get wallet balance

GET    /api/v1/ledger/entries                   # Query ledger entries
GET    /api/v1/ledger/reconcile                 # Reconciliation check

GET    /api/v1/audit-logs                       # Query audit logs

GET    /api/v1/reports/daily-volume             # Daily volume report
GET    /api/v1/reports/wallet-growth            # Wallet growth report
GET    /api/v1/reports/transactions             # Transaction report
GET    /api/v1/statements                       # Export statements (CSV)

POST   /api/v1/webhooks                         # Create webhook
GET    /api/v1/webhooks                         # List webhooks
DELETE /api/v1/webhooks/{id}                    # Delete webhook
```

---

## Auth Strategy

### Dashboard Users (JWT)

1. User registers or logs in via `/auth/login`.
2. Server validates credentials, returns JWT access token (15 min) and refresh token (7 days).
3. JWT claims: `userId`, `organizationId`, `role`, `permissions`.
4. All dashboard requests include `Authorization: Bearer <token>`.
5. Refresh token endpoint issues new access token without re-authentication.

### Client Applications (API Keys)

1. Developer generates API key via dashboard.
2. Key is shown once (raw value), stored as hash in database.
3. Client application includes `X-Api-Key: <key>` in all requests.
4. Server looks up key by prefix, verifies hash, resolves organization and environment.
5. Revoked or expired keys are rejected immediately.

### Roles

| Role | Permissions |
|---|---|
| OWNER | Full access. Can manage organization, billing, team, all resources. |
| ADMIN | Can manage wallets, transactions, API keys, webhooks. Cannot delete organization. |
| DEVELOPER | Can create wallets, initiate transactions, view balances and reports. Cannot manage team or API keys. |

### Environment Scoping

API keys are scoped to either `SANDBOX` or `PRODUCTION`. A sandbox API key can only access sandbox wallets, accounts, and ledger entries; a production key can only access production resources. Cross-environment access is rejected with `403 FORBIDDEN`.

JWT dashboard sessions intentionally have **no environment scope** — dashboard users (owners, admins) can see and manage wallets across both environments. This asymmetry is by design: operators need full visibility, while client applications should be isolated to one environment.

The environment chain flows: `Wallet(environment)` → `Account(inherits from wallet)` → `LedgerEntry(inherits from account)`.

---

## Environment Configuration

### Local Development

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/argent_dev
    username: argent
    password: devpassword
  redis:
    host: localhost
    port: 6379
  rabbitmq:
    host: localhost
    port: 5672

argent:
  jwt:
    secret: dev-secret-do-not-use-in-production
    expiration: 900000  # 15 minutes
  api:
    sandbox-mode: true
```

### Secrets Management

- **Local:** `.env` file (gitignored), loaded via `docker-compose` or Spring profiles.
- **Production:** Environment variables injected by deployment platform (Render, Fly.io, or Kubernetes secrets).
- **Never:** Hardcoded secrets in application.yml or committed to repository.

### Docker Compose (Local Dev)

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: argent_dev
      POSTGRES_USER: argent
      POSTGRES_PASSWORD: devpassword
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  rabbitmq:
    image: rabbitmq:3-management-alpine
    ports:
      - "5672:5672"
      - "15672:15672"

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
    depends_on:
      - postgres
      - redis
      - rabbitmq

  frontend:
    build: ./frontend
    ports:
      - "3000:3000"
    depends_on:
      - backend
```

---

## Third-Party Integrations

### V1

| Service | Purpose | Required? |
|---|---|---|
| **PostgreSQL** | Primary data store | Yes |
| **Redis** | Caching, rate limiting, session storage | Yes |
| **RabbitMQ** | Async message processing | Yes |
| **Docker** | Containerization | Yes |
| **GitHub Actions** | CI/CD | Yes |

### V2+

| Service | Purpose | Required? |
|---|---|---|
| **Stripe** | Payment gateway integration | No — for V2 payment features |
| **SendGrid** | Transactional email | No — for V2 notifications |
| **DataDog / Grafana** | Monitoring and observability | No — for V2 production monitoring |

### Why Minimal Integrations?

Every third-party integration is a failure point, a security surface, and a maintenance burden. V1 should work with infrastructure components that are well-understood and self-hostable. External service integrations are added only when they directly enable a user-facing feature.

---

## State Management (Frontend)

**Library:** Zustand (lightweight, no boilerplate, TypeScript-first).

**Approach:**

- Global store for auth state (current user, organization, tokens).
- Feature-specific stores for wallets, transactions, balances.
- No Redux. Too much boilerplate for a dashboard app.
- Server state managed via React Query (TanStack Query) for caching, refetching, and optimistic updates.

**Pattern:**

```
React Query ←→ API Client ←→ Spring Boot REST API
     ↕
  Zustand (UI state only)
```

React Query handles all server state (fetching, caching, invalidation). Zustand handles client-only state (sidebar open/closed, selected filters, theme).
