# PRD.md — Argent Product Requirements Document

---

## Elevator Pitch

**Argent is a developer-first financial infrastructure API that lets startups build wallets, credits, rewards, internal payments, refunds, and balance management without implementing financial accounting themselves.**

---

## Problem Statement

Every startup eventually builds the same financial primitives: wallets, credits, loyalty points, rewards, refunds, internal transfers, merchant balances, escrow. Every team writes this logic differently. Eventually they discover incorrect balances, race conditions, missing audit logs, duplicated transactions, and reconciliation nightmares.

Argent eliminates this by providing a single, reliable API that handles double-entry accounting, immutable ledger entries, balance management, and transaction tracking — so developers can focus on their product, not their books.

---

## Target Users / Personas

### Persona 1: Startup Founder / Technical Co-founder

- **Goal:** Ship financial features (wallets, credits, transfers) fast without hiring an accountant or building accounting software.
- **Frustration:** Every previous attempt at building wallet logic ended with bugs, race conditions, and audit gaps. Doesn't want to become a payments expert just to offer basic financial features.
- **Success criteria:** Can integrate Argent in under a day and trust that balances are always correct.

### Persona 2: Backend Developer at a Growing Startup

- **Goal:** Integrate a reliable financial API into an existing application (food delivery, marketplace, SaaS billing).
- **Frustration:** Existing solutions are either too complex (banking APIs), too limited (simple payment processors), or require building from scratch.
- **Success criteria:** Clear API docs, sandbox environment, predictable behavior, and webhooks that actually work.

### Persona 3: Product Manager / CTO evaluating infrastructure

- **Goal:** Reduce engineering risk around financial features. Needs to trust that the system is auditable, scalable, and won't require a rewrite in 6 months.
- **Frustration:** Hard to evaluate financial infrastructure without seeing the accounting model. Worries about vendor lock-in.
- **Success criteria:** Transparent double-entry ledger, audit logs, clear data model, and a product roadmap that aligns with their growth.

---

## Core Features

### MVP (V1) — Launch

| Feature | Description |
|---|---|
| **Organizations** | Multi-tenant isolation. Each customer is an Organization. |
| **Authentication** | JWT-based auth for dashboard users. API Keys for client applications. |
| **Team Roles** | Owner, Admin, Developer roles within an Organization. |
| **Sandbox & Production** | Environment separation for safe testing. |
| **Wallet CRUD** | Create, freeze, close, list, and retrieve wallets with metadata. |
| **Double-Entry Ledger** | Immutable ledger entries for every transaction. Debit and credit entries that always balance. |
| **Transactions** | Deposit, Withdrawal, Transfer, Refund, Adjustment, Fee, Commission, Bonus, Reversal. |
| **Balance Engine** | Current, available, pending, and reserved balances per account. |
| **Transfer Engine** | Atomic transfers between wallets with balance validation. |
| **Refund Engine** | Reverse or partially reverse completed transactions. |
| **Audit Logs** | Every action recorded: who, what, when, where, previous state. |
| **Reporting** | Transaction history, daily volume, wallet growth, export statements. |
| **API Documentation** | OpenAPI/Swagger specs for all endpoints. |
| **Docker Deployment** | Docker Compose setup for local development and single-server deployment. |

### Post-MVP (V2)

| Feature | Description |
|---|---|
| **Webhooks** | Event-driven notifications for wallet, transaction, and balance events. |
| **Reserved Balances** | Hold funds against future transactions. |
| **Scheduled Transfers** | Time-delayed or recurring transfers. |
| **Dashboard Improvements** | Enhanced admin UI for wallet and transaction management. |

### Future (V3+)

| Feature | Description |
|---|---|
| **Multi-Currency** | Support for multiple currencies and exchange rates. |
| **Payment Gateway Integration** | Connect to Stripe, bank APIs, payment processors. |
| **SDKs** | Java, Node.js, Python client libraries. |
| **Advanced Analytics** | Real-time dashboards, cohort analysis, forecasting. |
| **Enterprise Features** | SSO, custom roles, SLA guarantees, dedicated support. |
| **Compliance** | AML/KYC hooks, fraud detection, regulatory reporting. |

---

## User Stories — MVP

### Identity & Organization

1. As a **founder**, I want to create an organization so that my team has an isolated workspace.
2. As an **organization owner**, I want to invite team members with specific roles so that access is controlled.
3. As a **developer**, I want to generate API keys so that my application can authenticate with Argent.
4. As a **developer**, I want to switch between sandbox and production environments so that I can test safely.

### Wallet Engine

5. As a **developer**, I want to create a wallet with metadata (customer ID, tags) so that I can track whose wallet it is.
6. As a **developer**, I want to freeze a wallet so that no transactions can occur on it.
7. As a **developer**, I want to close a wallet so that it is permanently disabled.
8. As a **developer**, I want to list all wallets in my organization so that I can manage them.

### Transaction Engine

9. As a **developer**, I want to deposit funds into a wallet so that a customer has a balance.
10. As a **developer**, I want to transfer funds between two wallets atomically so that money is never lost or duplicated.
11. As a **developer**, I want to withdraw funds from a wallet so that a customer can cash out.
12. As a **developer**, I want to refund a transaction so that money is returned correctly.
13. As a **developer**, I want to apply fees or commissions so that the platform can monetize.

### Balance Engine

14. As a **developer**, I want to query the current balance of a wallet so that I can display it to users.
15. As a **developer**, I want to see available vs. pending balance so that I understand what can be spent.

### Ledger & Audit

16. As a **developer**, I want every transaction to create immutable ledger entries so that the books are always correct.
17. As a **developer**, I want to query the ledger for an account so that I can reconcile.
18. As an **admin**, I want to see a full audit log so that I know who did what and when.

### Reporting

19. As a **developer**, I want to export transaction history as CSV so that I can analyze it.
20. As an **admin**, I want to see daily volume reports so that I can monitor business health.

---

## Non-Functional Requirements

### Performance

- Balance lookups: < 50ms p99
- Transaction recording: < 200ms p99
- API response time: < 500ms p99 for all endpoints
- Support 10,000+ transactions per second at scale (V2+)

### Security

- JWT authentication for all dashboard access
- API Key authentication for all client application access
- All data encrypted at rest (PostgreSQL TDE) and in transit (TLS 1.2+)
- No secrets in code or environment variables committed to repository
- Input validation at every API boundary
- SQL injection prevention via parameterized queries
- Rate limiting on all public endpoints
- IP allowlisting for production API keys (V2)

### Data Integrity

- All financial operations use database transactions (ACID)
- Ledger entries are append-only, never modified or deleted
- Balances are derived from ledger entries, not stored independently
- Idempotency keys on all write operations to prevent duplicate transactions
- Double-entry invariant: total debits must always equal total credits

### Accessibility

- WCAG 2.1 AA compliance for the dashboard
- Keyboard navigable
- Screen reader compatible
- Sufficient color contrast (4.5:1 minimum)

### Supported Environments

- **Dashboard:** Chrome 90+, Firefox 90+, Safari 14+, Edge 90+
- **API:** Any HTTP client
- **Deployment:** Docker, Docker Compose, Kubernetes (future)

### Scalability

- Multi-tenant architecture with organization-level isolation
- Stateless backend services for horizontal scaling
- Read replicas for reporting queries (V2)
- Message queue for async operations (webhooks, reporting)

---

## Success Metrics

| Metric | Target |
|---|---|
| Developer integration time | < 1 hour for basic wallet + transfer flow |
| API uptime | 99.9% |
| Transaction accuracy | 100% (zero lost or duplicated transactions) |
| Balance consistency | 100% (ledger always reconciles with balances) |
| Audit coverage | 100% of write operations logged |
| Developer satisfaction | > 4.5/5 in post-integration survey |

---

## Explicit Out-of-Scope (V1)

These are intentionally excluded. Do not build them without explicit approval.

- ❌ Multi-currency support
- ❌ Exchange rate calculations
- ❌ Tax engine or tax calculations
- ❌ Payment gateway integrations (Stripe, PayPal, etc.)
- ❌ Banking integrations (SWIFT, SEPA, ACH)
- ❌ AML/KYC verification
- ❌ Settlement engine
- ❌ Fraud detection
- ❌ Real-time notifications (email, SMS, push) — webhooks only
- ❌ Mobile apps
- ❌ Public developer portal / developer accounts
- ❌ OAuth2 for third-party apps
- ❌ GraphQL API
- ❌ WebSocket real-time updates
- ❌ AI-powered features
- ❌ Blockchain or crypto features
- ❌ Microservices architecture (start as modular monolith)
- ❌ Kubernetes deployment (Docker Compose for V1)
- ❌ Rate limiting beyond basic protection
- ❌ Request signing
- ❌ IP restrictions
- ❌ Custom domain support
- ❌ White-labeling
- ❌ Multi-language API docs
- ❌ Client SDKs
- ❌ Event sourcing (use append-only ledger instead)
- ❌ CQRS pattern ( premature optimization for V1)

---

## Assumptions

1. Argent is a hosted SaaS product, not self-hosted.
2. There is a web dashboard built in React for managing the organization, wallets, and viewing reports.
3. Client applications integrate via REST API using API keys.
4. The initial deployment target is a single server or small cluster (Docker Compose).
5. The primary market is English-speaking developers and startups.
6. The team building this is small (1-3 developers) and needs a modular monolith, not microservices.
7. V1 launch target is a functional product that a developer can use to manage wallets and transactions — not a polished enterprise product.
