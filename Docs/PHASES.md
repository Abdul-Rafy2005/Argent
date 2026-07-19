# PHASES.md — Build Plan

---

## Overview

The MVP is broken into 8 phases. Each phase produces a working, tested, shippable increment. No phase depends on untested code from a future phase. Phases are ordered so each builds on a working foundation.

**Dependencies:** Phase 1 is foundational. Phases 2-4 can proceed in sequence. Phases 5-7 depend on earlier phases but are somewhat independent of each other. Phase 8 is final integration.

---

## Phase 1: Project Scaffolding + Database Foundation

**Goal:** Set up the project structure, database, and base configuration so all subsequent phases can build immediately.

### What Gets Built

- Spring Boot application with Gradle build
- PostgreSQL database with Docker Compose
- Flyway migration setup
- Base application configuration (dev, test profiles)
- Common exception classes and global exception handler
- Standard API response wrapper
- Health check endpoint
- Redis connection setup
- Basic security configuration (disabled for now, enabled in Phase 2)
- Frontend project with Vite, React, TypeScript, Tailwind
- Frontend development server configured

### Database Migrations Created

- `V1__create_organizations.sql`
- `V2__create_users.sql`
- `V3__create_api_keys.sql`
- `V4__create_wallets.sql`
- `V5__create_accounts.sql`
- `V6__create_transactions.sql`
- `V7__create_ledger_entries.sql`
- `V8__create_balances.sql`
- `V9__create_audit_logs.sql`
- `V10__create_webhooks.sql`

### Acceptance Criteria

- [ ] `docker-compose up` starts PostgreSQL, Redis, RabbitMQ
- [ ] Backend starts without errors on port 8080
- [ ] `/actuator/health` returns 200
- [ ] All 10 migrations run successfully against fresh database
- [ ] Frontend starts without errors on port 3000
- [ ] Frontend displays a basic landing page

### What Must Be Tested

- **Unit tests:** N/A (no business logic yet)
- **Integration tests:** Database migration runs cleanly on fresh PostgreSQL
- **Manual verification:** Docker Compose stack starts and all services are healthy

---

## Phase 2: Authentication + Organization Management

**Goal:** Users can register, log in, create organizations, and generate API keys. Multi-tenancy is established.

### Dependencies

- Phase 1 must be complete.

### What Gets Built

- User entity, repository, service, controller
- Organization entity, repository, service, controller
- ApiKey entity, repository, service, controller
- JWT authentication (access token + refresh token)
- API Key authentication filter
- Security configuration (protect endpoints, public endpoints)
- Registration endpoint (`POST /auth/register`)
- Login endpoint (`POST /auth/login`)
- Refresh token endpoint (`POST /auth/refresh`)
- Organization CRUD (create, get, list)
- API Key generation and revocation
- Role-based access control (Owner, Admin, Developer)
- Audit logging for auth events and organization changes

### API Endpoints

```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/organizations
GET    /api/v1/organizations/{id}
GET    /api/v1/organizations
POST   /api/v1/api-keys
GET    /api/v1/api-keys
DELETE /api/v1/api-keys/{id}
```

### Acceptance Criteria

- [ ] A new user can register with email and password
- [ ] A registered user can log in and receive JWT tokens
- [ ] JWT tokens can be used to access protected endpoints
- [ ] Refresh token issues new access token
- [ ] A user can create an organization and becomes OWNER
- [ ] An OWNER can generate API keys for sandbox and production
- [ ] API keys authenticate client application requests
- [ ] Revoked API keys are rejected
- [ ] All auth and organization actions create audit logs
- [ ] Users can only access their own organization's data (multi-tenancy)

### What Must Be Tested

- **Unit tests:**
  - `AuthServiceTest`: Registration validates input, hashes password, creates user
  - `AuthServiceTest`: Login validates credentials, returns tokens
  - `AuthServiceTest`: Refresh token issues new access token
  - `ApiKeyServiceTest`: Generate creates key, returns prefix + secret
  - `ApiKeyServiceTest`: Revoke disables key
  - `OrganizationServiceTest`: Create sets owner, returns organization
  - `OrganizationServiceTest`: Only owner/admin can generate API keys

- **Integration tests:**
  - `AuthIntegrationTest`: Full register → login → access protected endpoint flow
  - `OrganizationIntegrationTest`: Create org → generate API key → use API key to access wallet endpoints (wallet not implemented yet, but 401 vs 200 proves auth works)
  - `MultiTenancyTest`: User A's API key cannot access User B's organization data

---

## Phase 3: Wallet Engine

**Goal:** Developers can create, manage, and query wallets. Each wallet is backed by an internal account.

### Dependencies

- Phase 2 must be complete (wallets require authenticated organization context).

### What Gets Built

- Wallet entity, repository, service, controller
- Account entity, repository, service
- Balance entity, repository, service
- Wallet CRUD (create, get, list, update metadata)
- Wallet status transitions (active → frozen → closed)
- Internal account creation on wallet creation
- Balance initialization on wallet creation
- Wallet metadata support (JSONB)
- Audit logging for wallet events
- Pagination for wallet listing

### API Endpoints

```
POST   /api/v1/wallets
GET    /api/v1/wallets
GET    /api/v1/wallets/{id}
PATCH  /api/v1/wallets/{id}
POST   /api/v1/wallets/{id}/freeze
POST   /api/v1/wallets/{id}/unfreeze
POST   /api/v1/wallets/{id}/close
```

### Acceptance Criteria

- [ ] A developer can create a wallet with a label and type
- [ ] Wallet creation automatically creates an internal account and initializes balance
- [ ] A developer can retrieve a wallet by ID
- [ ] A developer can list all wallets in their organization with pagination
- [ ] A developer can update wallet metadata
- [ ] A developer can freeze a wallet (no transactions allowed)
- [ ] A developer can unfreeze a wallet
- [ ] A developer can close a wallet (permanent, no transactions)
- [ ] Wallet operations only work within the authenticated organization
- [ ] All wallet actions create audit logs
- [ ] Attempting to transact on a frozen/closed wallet returns appropriate error

### What Must Be Tested

- **Unit tests:**
  - `WalletServiceTest`: Create wallet creates account and balance
  - `WalletServiceTest`: Freeze prevents transaction (tested in Phase 5, mock here)
  - `WalletServiceTest`: Close prevents unfreeze
  - `WalletServiceTest`: Cannot create wallet for different organization
  - `BalanceServiceTest`: Initialize sets all balances to zero

- **Integration tests:**
  - `WalletIntegrationTest`: Create wallet → retrieve wallet → verify account and balance exist
  - `WalletIntegrationTest`: List wallets returns only authenticated organization's wallets
  - `WalletIntegrationTest`: Freeze wallet → attempt transaction → verify rejection
  - `WalletIntegrationTest`: Close wallet → verify cannot be unfrozen
  - `WalletIntegrationTest`: Update metadata → retrieve → verify changes persisted

---

## Phase 4: Ledger Engine

**Goal:** The double-entry ledger is operational. Every financial action creates immutable, balanced entries.

### Dependencies

- Phase 3 must be complete (ledger entries are created against accounts from wallets).

### What Gets Built

- LedgerEntry entity, repository, service
- Ledger entry creation (debit and credit)
- Balance verification (debits must equal credits per transaction)
- Immutable entry enforcement (no updates or deletes)
- Balance recalculation from ledger entries
- Reconciliation check endpoint
- Ledger query by account, transaction, date range

### API Endpoints

```
GET    /api/v1/ledger/entries
GET    /api/v1/ledger/entries/{id}
GET    /api/v1/ledger/reconcile
```

### Acceptance Criteria

- [ ] Creating a transaction produces both debit and credit ledger entries
- [ ] Debit and credit entries for a transaction always sum to zero (balanced)
- [ ] Ledger entries are immutable (application layer prevents updates/deletes)
- [ ] Balance can be recalculated from ledger entries at any time
- [ ] Reconciliation endpoint verifies ledger balance matches stored balance
- [ ] Ledger entries can be queried by account, transaction ID, or date range
- [ ] All ledger entries include organization ID for multi-tenant isolation

### What Must Be Tested

- **Unit tests:**
  - `LedgerServiceTest`: Create entries validates debit/credit balance
  - `LedgerServiceTest`: Create entries stores with correct amounts and types
  - `LedgerServiceTest`: Cannot modify existing entries
  - `LedgerServiceTest`: Query filters by account, transaction, date range
  - `BalanceServiceTest`: RecalculateFromLedger computes correct balance

- **Integration tests:**
  - `LedgerIntegrationTest`: Create two entries → verify they balance
  - `LedgerIntegrationTest`: Attempt to modify entry → verify rejection
  - `LedgerIntegrationTest`: Create entries → recalculate balance → verify matches stored balance
  - `LedgerIntegrationTest`: Reconciliation passes when balances match
  - `LedgerIntegrationTest`: Reconciliation fails when balances are tampered with

---

## Phase 5: Transaction Engine

**Goal:** Core financial operations work: deposits, transfers, withdrawals, refunds. Balances update correctly and atomically.

### Dependencies

- Phase 3 (wallets) and Phase 4 (ledger) must be complete.

### What Gets Built

- Transaction entity, repository, service, controller
- Transaction engines for each type:
  - DepositEngine: Add funds to a wallet
  - WithdrawalEngine: Remove funds from a wallet
  - TransferEngine: Atomic transfer between two wallets
  - RefundEngine: Reverse a completed transaction
  - AdjustmentEngine: Manual balance adjustment
- Idempotency key handling (prevent duplicate transactions)
- Atomic balance updates with optimistic locking
- Balance validation (sufficient funds, wallet active)
- Transaction status lifecycle (PENDING → COMPLETED / FAILED)
- Audit logging for all transaction events

### API Endpoints

```
POST   /api/v1/deposits
POST   /api/v1/transfers
POST   /api/v1/withdrawals
POST   /api/v1/refunds
POST   /api/v1/adjustments
GET    /api/v1/transactions
GET    /api/v1/transactions/{id}
```

### Acceptance Criteria

- [ ] Deposit adds funds to wallet, creates ledger entries, updates balance
- [ ] Transfer moves funds atomically between wallets, creates ledger entries, updates both balances
- [ ] Withdrawal removes funds from wallet, creates ledger entries, updates balance
- [ ] Refund reverses a completed transaction, creates compensating ledger entries
- [ ] Adjustment manually changes balance with ledger entry
- [ ] Insufficient balance returns clear error without modifying any data
- [ ] Frozen wallet cannot receive or send funds
- [ ] Closed wallet cannot receive or send funds
- [ ] Duplicate idempotency key returns previous result without re-executing
- [ ] All operations are atomic (partial failure rolls back completely)
- [ ] Transaction history is queryable with filters (type, status, date range)
- [ ] Each transaction creates an audit log entry
- [ ] Transfer between same wallet is rejected

### What Must Be Tested

- **Unit tests:**
  - `DepositEngineTest`: Valid deposit creates entries and updates balance
  - `TransferEngineTest`: Valid transfer debits source, credits destination
  - `TransferEngineTest`: Insufficient balance throws exception
  - `TransferEngineTest`: Same wallet transfer is rejected
  - `WithdrawalEngineTest`: Valid withdrawal creates entries and updates balance
  - `WithdrawalEngineTest`: Insufficient balance throws exception
  - `RefundEngineTest`: Valid refund reverses original transaction
  - `RefundEngineTest`: Cannot refund already refunded transaction
  - `TransactionServiceTest`: Idempotency key returns cached result
  - `TransactionServiceTest`: Frozen wallet rejected for all transaction types
  - `TransactionServiceTest`: Closed wallet rejected for all transaction types

- **Integration tests:**
  - `TransferIntegrationTest`: Create two wallets → deposit → transfer → verify balances
  - `TransferIntegrationTest`: Transfer with insufficient balance → verify no balance change
  - `TransferIntegrationTest`: Concurrent transfers → verify no race condition (optimistic locking)
  - `RefundIntegrationTest`: Transfer → refund → verify original and refund balances correct
  - `IdempotencyIntegrationTest`: Send same request twice → verify only one transaction created
  - `EndToEndTransactionTest`: Deposit → transfer → withdraw → verify full lifecycle

---

## Phase 6: Balance Engine

**Goal:** Balance queries are fast and support multiple balance types (current, available, pending, reserved).

### Dependencies

- Phase 5 must be complete (balances are updated by transactions).

### What Gets Built

- Balance query service with caching
- Available balance calculation (current - pending - reserved)
- Pending balance tracking
- Reserved balance tracking
- Cache invalidation on balance changes
- Balance history endpoint (future: point-in-time queries)

### API Endpoints

```
GET    /api/v1/balances/{walletId}
GET    /api/v1/balances/{walletId}/history
```

### Acceptance Criteria

- [ ] Balance query returns current, available, pending, and reserved amounts
- [ ] Available balance = current - pending - reserved
- [ ] Balance is cached in Redis for fast retrieval
- [ ] Cache is invalidated when balance changes
- [ ] Balance query returns correct data immediately after transaction
- [ ] Balance history shows balance changes over time
- [ ] Balance queries are isolated per organization

### What Must Be Tested

- **Unit tests:**
  - `BalanceServiceTest`: Available = current - pending - reserved
  - `BalanceServiceTest`: Cache hit returns cached value
  - `BalanceServiceTest`: Cache invalidation on balance update

- **Integration tests:**
  - `BalanceIntegrationTest`: Create wallet → deposit → query balance → verify all fields
  - `BalanceIntegrationTest`: Transfer between wallets → query both balances → verify correctness
  - `BalanceIntegrationTest`: Balance cached in Redis → update balance → cache invalidated → fresh value returned
  - `BalanceIntegrationTest`: Concurrent balance queries return consistent results

---

## Phase 7: Audit + Reporting

**Goal:** Every action is traceable. Reports provide business visibility.

### Dependencies

- Phases 2-6 must be complete (audit logs and reports depend on all prior data).

### What Gets Built

- Audit log query service (filter by entity, action, user, date)
- Audit log detail view (before/after state comparison)
- Transaction report (daily volume, by type, by status)
- Wallet report (growth, active, frozen, closed)
- Statement export (CSV format)
- Report endpoints with date range filtering

### API Endpoints

```
GET    /api/v1/audit-logs
GET    /api/v1/audit-logs/{id}
GET    /api/v1/reports/daily-volume
GET    /api/v1/reports/wallet-growth
GET    /api/v1/reports/transactions
GET    /api/v1/statements
```

### Acceptance Criteria

- [ ] Audit logs are queryable by entity type, action, user, date range
- [ ] Audit log detail shows before and after state for modifications
- [ ] Daily volume report shows total transactions and amount per day
- [ ] Wallet growth report shows new wallets per day
- [ ] Transaction report filters by type, status, date range
- [ ] Statement export returns CSV with all transactions for a date range
- [ ] All reports are scoped to authenticated organization
- [ ] Reports handle empty data gracefully (return empty arrays, not errors)

### What Must Be Tested

- **Unit tests:**
  - `AuditServiceTest`: Query filters by entity, action, user, date
  - `ReportingServiceTest`: Daily volume aggregates correctly
  - `ReportingServiceTest`: Empty data returns empty results
  - `StatementExportTest`: CSV format is correct

- **Integration tests:**
  - `AuditIntegrationTest`: Create entity → query audit logs → verify entry exists
  - `AuditIntegrationTest`: Update entity → query audit logs → verify before/after state
  - `ReportingIntegrationTest`: Create transactions → query daily volume → verify aggregation
  - `ReportingIntegrationTest`: Export statement → verify CSV contains all transactions
  - `ReportingIntegrationTest`: Query reports with no data → verify empty results (no errors)

---

## Phase 8: Frontend Dashboard

**Goal:** A functional web dashboard for managing wallets, viewing transactions, and monitoring balances.

### Dependencies

- Phases 1-7 must be complete (frontend consumes the backend API).

### What Gets Built

- Login / registration pages
- Dashboard overview (total wallets, total balance, recent transactions)
- Wallet management page (list, create, freeze, close)
- Transaction history page (list, filter, search)
- Balance detail page
- Ledger view page
- Audit log viewer
- Statement export
- Settings page (organization, API keys)
- Responsive layout (sidebar navigation)
- Error handling and loading states

### Pages

| Page | Route | Description |
|---|---|---|
| Login | `/login` | Email + password login |
| Register | `/register` | New account registration |
| Dashboard | `/` | Overview stats + recent activity |
| Wallets | `/wallets` | List + create + manage wallets |
| Wallet Detail | `/wallets/:id` | Single wallet with balance + transactions |
| Transactions | `/transactions` | Full transaction history with filters |
| Ledger | `/ledger` | Ledger entries view |
| Audit Log | `/audit` | Audit trail viewer |
| Reports | `/reports` | Charts and data tables |
| Settings | `/settings` | Organization + API key management |

### Acceptance Criteria

- [ ] User can register and log in
- [ ] Dashboard shows summary statistics
- [ ] User can create a wallet from the UI
- [ ] User can freeze/unfreeze/close a wallet from the UI
- [ ] Transaction history is filterable by type, status, date
- [ ] Balance is displayed correctly for each wallet
- [ ] Ledger entries are viewable per account
- [ ] Audit logs are viewable with before/after state
- [ ] Statement can be exported as CSV
- [ ] API keys can be generated and revoked from settings
- [ ] All pages handle loading states and errors gracefully
- [ ] Responsive layout works on tablet and desktop (no mobile for V1)

### What Must Be Tested

- **Component tests:**
  - Login form submits credentials and redirects on success
  - Wallet list renders wallets from API
  - Create wallet form validates input and calls API
  - Transaction table renders with correct data
  - Balance display shows all balance types

- **Integration tests:**
  - Full login → dashboard → create wallet → deposit → transfer flow via UI
  - Error states display correctly (network error, validation error)
  - Loading states display during API calls

---

## Phase Summary

| Phase | Name | Dependencies | Estimated Complexity |
|---|---|---|---|
| 1 | Scaffolding + Database | None | Low |
| 2 | Auth + Organizations | Phase 1 | Medium |
| 3 | Wallet Engine | Phase 2 | Medium |
| 4 | Ledger Engine | Phase 3 | High |
| 5 | Transaction Engine | Phases 3, 4 | High |
| 6 | Balance Engine | Phase 5 | Medium |
| 7 | Audit + Reporting | Phases 2-6 | Medium |
| 8 | Frontend Dashboard | Phases 1-7 | Medium-High |

**Total:** 8 phases, each independently testable and shippable.
