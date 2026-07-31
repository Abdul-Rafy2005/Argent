<div align="center">

# Argent

### Developer-First Financial Infrastructure

**The invisible accounting engine behind your product.**

Build wallets, credits, rewards, transfers, refunds, and balance management — without ever writing a ledger.

[![Java 21](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.1-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=flat-square&logo=redis&logoColor=white)](https://redis.io/)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react&logoColor=black)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.3-3178C6?style=flat-square&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)
[![Tests](https://img.shields.io/badge/Tests-190+-2ECC71?style=flat-square&logo=junit5&logoColor=white)](#testing)

<br />

<a href="#quick-start">Quick Start</a> · <a href="#architecture">Architecture</a> · <a href="#api-examples">API Examples</a> · <a href="#data-model">Data Model</a> · <a href="#deployment">Deployment</a>

<br />

![Apex Dashboard](Argent.png)

</div>

---

## What Argent Solves

Every startup eventually builds the same financial primitives: wallets, credits, loyalty points, refunds, transfers, merchant balances. And every team discovers the same bugs: incorrect balances, race conditions, missing audit trails, duplicated transactions, and reconciliation nightmares.

**Argent eliminates this.** It provides double-entry bookkeeping, immutable ledger entries, and balance management behind a simple REST API — so developers can focus on their product, not their books.

```
Your App ──── REST API ──── Argent ──── Double-Entry Ledger
                                   ──── Immutable Audit Trail
                                   ──── Balance Management
                                   ──── Environment Isolation
```

---

## Architecture

### System Pipeline Achitecture

![Apex architecture](architecture_argent.png)

### System Overview

```mermaid
graph TB
    subgraph Client["Client Applications"]
        A1[Food Delivery App]
        A2[Marketplace]
        A3[SaaS Platform]
        A4[Gaming Backend]
    end

    subgraph Gateway["API Gateway"]
        B1[Nginx Reverse Proxy]
    end

    subgraph Backend["Spring Boot Application"]
        C1[Auth Module<br/>JWT + API Keys]
        C2[Wallet Module<br/>CRUD + Status]
        C3[Transaction Module<br/>Strategy Pattern]
        C4[Ledger Module<br/>Double-Entry]
        C5[Balance Module<br/>Cache + Compute]
        C6[Audit Module<br/>Full Trail]
        C7[Reporting Module<br/>CSV Export]
    end

    subgraph Data["Data Layer"]
        D1[(PostgreSQL 16<br/>ACID + Triggers)]
        D2[(Redis 7<br/>Balance Cache)]
    end

    subgraph Infra["Infrastructure"]
        E1[Docker Compose]
        E2[Render Blueprint]
    end

    Client --> Gateway
    Gateway --> Backend
    Backend --> Data
    Backend --> Infra
```

### Transaction Flow

Every transaction follows this atomic path through the system:

```mermaid
sequenceDiagram
    participant C as Client
    participant A as Auth
    participant T as TransactionService
    participant E as Engine (Deposit/Transfer/...)
    participant L as LedgerEntryService
    participant B as BalanceService
    participant D as PostgreSQL

    C->>A: POST /api/v1/deposits<br/>+ Idempotency-Key
    A->>T: Validate + Route
    T->>E: Execute transaction type
    E->>L: createBalancedEntries()
    
    Note over L,D: BEGIN TRANSACTION
    L->>D: INSERT debit entry
    L->>D: INSERT credit entry
    L->>D: UPDATE account A balance
    L->>D: UPDATE account B balance
    Note over L,D: COMMIT (atomic)
    
    L-->>E: Entries created
    E-->>T: Transaction complete
    T-->>C: 201 Created + idempotency key
```

### Request Processing Pipeline

```mermaid
flowchart LR
    A[HTTP Request] --> B{Auth Type?}
    B -->|JWT| C[Validate Token<br/>Extract User]
    B -->|API Key| D[Hash + Lookup<br/>Resolve Org]
    C --> E[RBAC Check<br/>Role Permissions]
    D --> F[Env Scope Check<br/>Sandbox/Production]
    E --> G[Service Layer<br/>Business Logic]
    F --> G
    G --> H{Write Operation?}
    H -->|Yes| I[Idempotency Check]
    H -->|No| J[Cache Lookup]
    I --> K[Execute +<br/>Audit Log]
    J --> L[Return Response]
    K --> L
```

---

## Why Argent?

### Double-Entry Bookkeeping

Every transaction creates **two** ledger entries (debit + credit) that always balance. If the sum of debits != sum of credits, the transaction is rejected. Period.

```mermaid
graph LR
    subgraph Deposit["Deposit $100"]
        D1[Debit: Platform Account<br/>-$100] --> C1[Credit: Customer Wallet<br/>+$100]
    end

    subgraph Transfer["Transfer $50"]
        D2[Debit: Sender Wallet<br/>-$50] --> C2[Credit: Receiver Wallet<br/>+$50]
    end

    subgraph Withdrawal["Withdrawal $30"]
        D3[Debit: Customer Wallet<br/>-$30] --> C3[Credit: Platform Account<br/>+$30]
    end

    style D1 fill:#ff6b6b,color:#fff
    style D2 fill:#ff6b6b,color:#fff
    style D3 fill:#ff6b6b,color:#fff
    style C1 fill:#51cf66,color:#fff
    style C2 fill:#51cf66,color:#fff
    style C3 fill:#51cf66,color:#fff
```

### Immutable Ledger

Ledger entries are **physically impossible to modify** at the database level. PostgreSQL `BEFORE UPDATE` and `BEFORE DELETE` triggers raise exceptions if any mutation is attempted — even direct SQL cannot alter them.

```sql
-- This will FAIL at the database level:
UPDATE ledger_entries SET amount = 999 WHERE id = '...';
-- Error: ledger_entries are immutable

DELETE FROM ledger_entries WHERE id = '...';
-- Error: ledger_entries are immutable
```

### Strategy Pattern Transaction Engine

Each transaction type is an independent engine class with its own business logic:

```
TransactionService
    ├── DepositEngine     (funds in)
    ├── WithdrawalEngine  (funds out)
    ├── TransferEngine    (wallet-to-wallet)
    ├── RefundEngine      (reverse transaction)
    └── AdjustmentEngine  (manual correction)
```

Adding a new transaction type = adding one new engine class. Zero changes to existing code.

### Multi-Environment Isolation

API keys are scoped to `SANDBOX` or `PRODUCTION`. A sandbox key cannot touch production data. The environment chain flows:

```
Wallet(environment) → Account(inherits) → LedgerEntry(inherits)
```

Dashboard users (via JWT) see **all** environments — operators need full visibility, while client applications stay isolated.

---

## Data Model

```mermaid
erDiagram
    Organization ||--o{ User : has
    Organization ||--o{ ApiKey : issues
    Organization ||--o{ Wallet : owns
    Organization ||--o{ AuditLog : generates

    Wallet ||--|| Account : linked_to
    Wallet ||--o{ Wallet : "transfers between"
    Account ||--o{ Transaction : processes
    Account ||--o{ LedgerEntry : records
    Account ||--|| Balance : tracks

    Transaction ||--o{ LedgerEntry : "creates (2 entries)"
    Transaction ||--o{ AuditLog : "audited by"

    Organization {
        uuid id PK
        string name
        string slug UK
        enum status
    }

    User {
        uuid id PK
        uuid organizationId FK
        string email
        enum role "OWNER|ADMIN|DEVELOPER"
        string passwordHash
    }

    Wallet {
        uuid id PK
        uuid organizationId FK
        string label
        enum type "CUSTOMER|MERCHANT|ESCROW|PLATFORM"
        enum status "ACTIVE|FROZEN|CLOSED"
        jsonb metadata
    }

    Account {
        uuid id PK
        uuid organizationId FK
        enum type "ASSET|LIABILITY|EQUITY"
        enum environment "SANDBOX|PRODUCTION"
    }

    Transaction {
        uuid id PK
        uuid organizationId FK
        enum type "DEPOSIT|WITHDRAWAL|TRANSFER|REFUND"
        enum status "PENDING|COMPLETED|FAILED"
        decimal amount
        string idempotencyKey UK
    }

    LedgerEntry {
        uuid id PK
        uuid transactionId FK
        uuid accountId FK
        enum type "DEBIT|CREDIT"
        decimal amount
        decimal balanceAfter
    }

    Balance {
        uuid id PK
        uuid accountId FK "unique"
        decimal current
        decimal available
        decimal pending
        decimal reserved
    }

    AuditLog {
        uuid id PK
        uuid organizationId FK
        string entityType
        uuid entityId
        string action
        jsonb previousState
        jsonb newState
    }
```

---

## Tech Stack

| Layer | Technology | Why This |
|---|---|---|
| **Language** | Java 21 | Virtual threads, LTS, bulletproof BigDecimal handling |
| **Framework** | Spring Boot 3.4.1 | Industry standard for financial backends |
| **Database** | PostgreSQL 16 | ACID compliance, JSONB metadata, trigger-based immutability |
| **Cache** | Redis 7 | Sub-millisecond balance lookups with smart invalidation |
| **Frontend** | React 18 + TypeScript | Type-safe dashboard, Vite for speed |
| **Styling** | Tailwind CSS | Consistent design system, zero runtime overhead |
| **State** | Zustand + TanStack Query | Client state + server state separated cleanly |
| **Build** | Gradle (Kotlin DSL) | Multi-module support, faster builds than Maven |
| **Containerization** | Docker Compose | Reproducible local dev, one-command setup |
| **Deployment** | Render (Blueprint) | Free tier, auto-deploy from `render.yaml` |

---

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Git

### One Command

```bash
git clone https://github.com/Abdul-Rafy2005/Argent.git && cd Argent && docker compose up --build
```

That's it. The entire stack boots in under 2 minutes:

| Service | URL | Purpose |
|---|---|---|
| **Frontend** | [localhost:3000](http://localhost:3000) | React Dashboard |
| **Backend API** | [localhost:8080](http://localhost:8080) | REST API |
| **Swagger UI** | [localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | API Explorer |
| **Health Check** | [localhost:8080/actuator/health](http://localhost:8080/actuator/health) | Service Status |

### Default Credentials

| Field | Value |
|---|---|
| Email | `admin@argent.com` |
| Password | `admin123` |

---

## API Examples

### Create a Wallet

```bash
curl -X POST http://localhost:8080/api/v1/wallets \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "label": "Main Wallet",
    "type": "CUSTOMER",
    "metadata": {
      "customerId": "cust_123",
      "email": "user@example.com"
    }
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "label": "Main Wallet",
    "type": "CUSTOMER",
    "status": "ACTIVE",
    "environment": "SANDBOX",
    "balance": 0,
    "metadata": {
      "customerId": "cust_123",
      "email": "user@example.com"
    },
    "createdAt": "2026-07-21T22:00:00Z"
  }
}
```

### Deposit Funds

```bash
curl -X POST http://localhost:8080/api/v1/deposits \
  -H "X-Api-Key: <sandbox-key>" \
  -H "Idempotency-Key: dep_001" \
  -H "Content-Type: application/json" \
  -d '{
    "walletId": "550e8400-e29b-41d4-a716-446655440000",
    "amount": "100.00",
    "description": "Initial deposit",
    "reference": "order_789"
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "txn_a1b2c3d4",
    "type": "DEPOSIT",
    "status": "COMPLETED",
    "amount": "100.00",
    "idempotencyKey": "dep_001",
    "ledgerEntries": [
      {
        "type": "DEBIT",
        "amount": "100.00",
        "accountType": "PLATFORM"
      },
      {
        "type": "CREDIT",
        "amount": "100.00",
        "accountType": "CUSTOMER"
      }
    ]
  }
}
```

### Transfer Between Wallets

```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "X-Api-Key: <sandbox-key>" \
  -H "Idempotency-Key: trf_001" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceWalletId": "wallet_sender",
    "destinationWalletId": "wallet_receiver",
    "amount": "25.50",
    "description": "Payment for services"
  }'
```

### Query Ledger Entries

```bash
curl "http://localhost:8080/api/v1/ledger/entries?walletId=wallet_123&page=0&size=20" \
  -H "X-Api-Key: <sandbox-key>"
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "entry_001",
      "type": "DEBIT",
      "amount": "25.50",
      "balanceAfter": "74.50",
      "description": "Transfer to wallet_receiver",
      "createdAt": "2026-07-21T22:05:00Z"
    },
    {
      "id": "entry_002",
      "type": "CREDIT",
      "amount": "25.50",
      "balanceAfter": "125.50",
      "description": "Transfer from wallet_sender",
      "createdAt": "2026-07-21T22:05:00Z"
    }
  ],
  "meta": {
    "page": 0,
    "size": 20,
    "total": 2,
    "totalPages": 1
  }
}
```

### Get Balance

```bash
curl http://localhost:8080/api/v1/balances/wallet_123 \
  -H "X-Api-Key: <sandbox-key>"
```

```json
{
  "success": true,
  "data": {
    "walletId": "wallet_123",
    "current": "125.50",
    "available": "125.50",
    "pending": "0.00",
    "reserved": "0.00",
    "currency": "USD"
  }
}
```

### Generate CSV Export

```bash
curl "http://localhost:8080/api/v1/statements?walletId=wallet_123&from=2026-07-01&to=2026-07-31" \
  -H "X-Api-Key: <sandbox-key>" \
  -o statement.csv
```

---

## Project Structure

```
Argent/
├── backend/                              # Spring Boot application
│   ├── src/main/java/com/argent/
│   │   ├── common/                       # Shared utilities & config
│   │   │   ├── config/                   # Security, Redis, CORS, DataSource
│   │   │   ├── exception/                # Global exception hierarchy
│   │   │   └── response/                 # ApiResponse, PagedResponse
│   │   └── module/                       # Feature modules
│   │       ├── auth/                     # JWT + API Key authentication
│   │       ├── wallet/                   # Wallet CRUD & status management
│   │       ├── transaction/              # Transaction processing engine
│   │       │   └── engine/               # Strategy pattern engines
│   │       ├── ledger/                   # Double-entry ledger system
│   │       ├── balance/                  # Balance management & cache
│   │       ├── audit/                    # Full audit trail
│   │       └── reporting/                # Reports & CSV export
│   ├── src/main/resources/
│   │   └── db/migration/                 # 18 Flyway migrations (V1–V18)
│   └── src/test/                         # 190 tests (unit + integration)
│
├── frontend/                             # React dashboard
│   ├── src/
│   │   ├── pages/                        # Dashboard, Wallets, Transactions...
│   │   ├── components/                   # Reusable UI components
│   │   ├── api/                          # Axios client + interceptors
│   │   ├── store/                        # Zustand auth store
│   │   └── types/                        # TypeScript definitions
│   └── nginx.conf                        # Production reverse proxy
│
├── render.yaml                           # Render Blueprint (deploy config)
├── docker-compose.yml                    # Local development stack
└── Docs/                                 # Architecture, PRD, Rules, Memory
```

---

## Testing

### Test Coverage

| Category | Count | Framework |
|---|---|---|
| **Unit Tests** | ~128 | JUnit 5 + Mockito |
| **Integration Tests** | ~62 | JUnit 5 + Testcontainers |
| **Frontend Tests** | ~18 | Vitest + React Testing Library |
| **Total** | **190+** | |

### Key Test Scenarios

- **Ledger Immutability**: DB-level trigger rejection (UPDATE + DELETE both blocked)
- **Double-Entry Balance**: Every transaction produces balanced debit/credit pairs
- **Environment Scoping**: Sandbox key cannot access production data
- **Idempotency**: Duplicate requests return cached response (no double-charge)
- **Optimistic Locking**: Concurrent balance updates return 409 Conflict
- **Platform Wallet**: Lazy-created counterparty for deposits/withdrawals
- **Audit Trail**: Every write operation logged with before/after state

### Run Tests

```bash
# Backend (all 190 tests)
cd backend && ./gradlew test

# Frontend
cd frontend && npm test
```

---

## Environment Configuration

### Environment Variables

| Variable | Description | Default |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Spring profile | `dev` |
| `DATABASE_URL` | PostgreSQL connection | `jdbc:postgresql://localhost:5432/argent_dev` |
| `SPRING_DATA_REDIS_HOST` | Redis host | `localhost` |
| `ARGENT_JWT_SECRET` | JWT signing secret | *(required)* |
| `ARGENT_CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `http://localhost:3000` |
| `BACKEND_URL` | Backend URL for nginx proxy | `http://localhost:8080` |

### API Authentication

```
# Dashboard users (JWT)
Authorization: Bearer <access-token>

# Client applications (API Key)
X-Api-Key: <api-key>
```

---

## Deployment

### Render (Production)

The project includes a `render.yaml` Blueprint that provisions:

- **PostgreSQL 16** (free tier)
- **Redis 7** (free tier)
- **Backend** (Docker, Spring Boot)
- **Frontend** (Docker, React + Nginx)

```bash
# Deploy via Render Dashboard
# 1. Push to GitHub
# 2. Create Blueprint from render.yaml
# 3. Set ARGENT_CORS_ALLOWED_ORIGINS to your frontend URL
```

### Docker Compose (Local)

```bash
docker compose up --build -d
docker compose ps          # Verify all services running
docker compose logs -f     # Watch logs
docker compose down        # Stop everything
```

---

## Design Principles

| Principle | Implementation |
|---|---|
| **BigDecimal everywhere** | No `float` or `double` for financial data. Ever. |
| **UUID primary keys** | All entities use UUID. No auto-increment integers. |
| **Immutable ledger** | PostgreSQL triggers prevent any UPDATE or DELETE on `ledger_entries`. |
| **Idempotency required** | All write endpoints accept `Idempotency-Key` header. |
| **Environment isolation** | API keys scoped to SANDBOX or PRODUCTION. Cross-env = 403. |
| **Audit everything** | Every write operation logged with who, what, when, before, after. |
| **Fail loudly** | Optimistic locking failures → 409. Invalid states → 400. No silent errors. |

---

## Roadmap

### V1 (Current)
- [x] Multi-tenant organization management
- [x] JWT + API Key authentication with RBAC
- [x] Wallet CRUD with status lifecycle
- [x] Double-entry ledger with immutable entries
- [x] Transaction engine (Deposit, Withdrawal, Transfer, Refund, Adjustment)
- [x] Balance management with Redis caching
- [x] Full audit trail
- [x] Reporting & CSV export
- [x] React dashboard with 7 pages
- [x] Docker Compose local development
- [x] Render Blueprint deployment

### V2 (Planned)
- [ ] Webhook subscriptions for event notifications
- [ ] Reserved balances for hold/freeze scenarios
- [ ] Scheduled & recurring transfers
- [ ] Multi-currency support
- [ ] Rate limiting per API key
- [ ] Advanced analytics dashboard

### V3 (Future)
- [ ] Stripe / payment gateway integration
- [ ] SDKs (Java, Node.js, Python)
- [ ] AML/KYC compliance hooks
- [ ] Enterprise SSO & custom roles

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing`)
5. Open a Pull Request

### Development Rules

- **No new dependencies** without discussion
- **Tests must pass** before merge
- **BigDecimal only** for financial amounts
- **UUID only** for primary keys
- **No secrets** in code or config files

---

## License

MIT License - see [LICENSE](LICENSE) for details.

---

<div align="center">

**Built with care by [Abdul-Rafy](https://github.com/Abdul-Rafy2005)**

*Financial infrastructure that developers can trust.*

</div>
