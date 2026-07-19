# PHASE_PROMPTS.md — Copy-Paste Prompts for Each Phase

---

## How to Use

1. Copy the prompt for the phase you want to execute.
2. Paste it into the coding agent session.
3. The agent will read the required docs, build the phase, write tests, and report results.
4. Review the results before proceeding to the next phase.

---

## Phase 1: Project Scaffolding + Database Foundation

```
You are working on Argent, a developer-first financial infrastructure API.

BEFORE STARTING, READ:
- AGENTS.md (project vision and constraints)
- RULES.md (coding conventions and approved libraries)
- MEMORY.md (current state and recent decisions)
- ARCHITECTURE.md (folder structure and tech stack)

YOUR TASK: Execute Phase 1 — Project Scaffolding + Database Foundation.

REFERENCE: See PHASES.md, Phase 1 for full acceptance criteria.

WHAT TO BUILD:
1. Spring Boot application with Gradle build (Java 21, Spring Boot 3.x)
2. PostgreSQL database with Docker Compose (include Redis and RabbitMQ)
3. Flyway migration setup with all 10 database migrations (V1 through V10)
4. Base application configuration (application.yml, application-dev.yml, application-test.yml)
5. Common exception classes and GlobalExceptionHandler
6. Standard API response wrapper (ApiResponse, PagedResponse)
7. Health check endpoint via Spring Actuator
8. Redis connection configuration
9. Basic security configuration (initially disabled, enabled in Phase 2)
10. Frontend project with Vite, React 18, TypeScript, Tailwind CSS
11. Frontend development server configured and running

DATABASE MIGRATIONS TO CREATE:
- V1__create_organizations.sql
- V2__create_users.sql
- V3__create_api_keys.sql
- V4__create_wallets.sql
- V5__create_accounts.sql
- V6__create_transactions.sql
- V7__create_ledger_entries.sql
- V8__create_balances.sql
- V9__create_audit_logs.sql
- V10__create_webhooks.sql

Use UUID for all primary keys. Use BigDecimal-compatible types (NUMERIC) for financial amounts. Include created_at and updated_at timestamps on all tables. Include foreign key constraints. Follow ARCHITECTURE.md data model exactly.

ACCEPTANCE CRITERIA (ALL MUST PASS):
- [ ] docker-compose up starts PostgreSQL, Redis, RabbitMQ
- [ ] Backend starts without errors on port 8080
- [ ] /actuator/health returns 200
- [ ] All 10 migrations run successfully against fresh database
- [ ] Frontend starts without errors on port 3000
- [ ] Frontend displays a basic landing page

TESTS: Verify migrations run cleanly. Verify all services start.

SCOPE: ONLY Phase 1. Do not build authentication, wallets, transactions, or any business logic. Stop after scaffolding is complete.

Before stopping, append a dated entry to MEMORY.md's Progress Log describing what was built and any issues encountered. Then report to me: what was built, what was verified, test results, and any deviations from the plan.
```

---

## Phase 2: Authentication + Organization Management

```
You are working on Argent, a developer-first financial infrastructure API.

BEFORE STARTING, READ:
- AGENTS.md (project vision and constraints)
- RULES.md (coding conventions and approved libraries)
- MEMORY.md (current state and recent decisions)
- ARCHITECTURE.md (folder structure, data model, API design)
- PHASES.md, Phase 2 (acceptance criteria and test requirements)

YOUR TASK: Execute Phase 2 — Authentication + Organization Management.

PREREQUISITE: Phase 1 must be complete and working.

WHAT TO BUILD:
1. User entity, repository, service, controller (with registration and login)
2. Organization entity, repository, service, controller
3. ApiKey entity, repository, service, controller
4. JWT authentication (access token 15min + refresh token 7 days)
5. API Key authentication filter
6. Security configuration protecting endpoints
7. Registration endpoint (POST /auth/register)
8. Login endpoint (POST /auth/login)
9. Refresh token endpoint (POST /auth/refresh)
10. Organization CRUD (create, get, list)
11. API Key generation and revocation
12. Role-based access control (Owner, Admin, Developer)
13. Audit logging for auth events and organization changes

API ENDPOINTS:
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/organizations
GET    /api/v1/organizations/{id}
GET    /api/v1/organizations
POST   /api/v1/api-keys
GET    /api/v1/api-keys
DELETE /api/v1/api-keys/{id}

ACCEPTANCE CRITERIA (ALL MUST PASS):
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

TESTS REQUIRED:
- Unit tests: AuthServiceTest (register, login, refresh), ApiKeyServiceTest (generate, revoke), OrganizationServiceTest (create, only owner can generate keys)
- Integration tests: AuthIntegrationTest (register → login → access protected), OrganizationIntegrationTest (create org → generate API key → use API key), MultiTenancyTest (User A cannot access User B's org)
- All tests must pass before marking complete.

SCOPE: ONLY Phase 2. Do not build wallets, transactions, or any financial logic. Stop after auth and organizations are complete.

Before stopping, append a dated entry to MEMORY.md's Progress Log. Then report: what was built, what tests were written, test results, and any deviations.
```

---

## Phase 3: Wallet Engine

```
You are working on Argent, a developer-first financial infrastructure API.

BEFORE STARTING, READ:
- AGENTS.md (project vision and constraints)
- RULES.md (coding conventions and approved libraries)
- MEMORY.md (current state and recent decisions)
- ARCHITECTURE.md (folder structure, data model, API design)
- PHASES.md, Phase 3 (acceptance criteria and test requirements)

YOUR TASK: Execute Phase 3 — Wallet Engine.

PREREQUISITE: Phase 2 must be complete (wallets require authenticated organization context).

WHAT TO BUILD:
1. Wallet entity, repository, service, controller
2. Account entity, repository, service
3. Balance entity, repository, service
4. Wallet CRUD (create, get, list, update metadata)
5. Wallet status transitions (active → frozen → closed)
6. Internal account creation on wallet creation
7. Balance initialization on wallet creation
8. Wallet metadata support (JSONB)
9. Audit logging for wallet events
10. Pagination for wallet listing

API ENDPOINTS:
POST   /api/v1/wallets
GET    /api/v1/wallets
GET    /api/v1/wallets/{id}
PATCH  /api/v1/wallets/{id}
POST   /api/v1/wallets/{id}/freeze
POST   /api/v1/wallets/{id}/unfreeze
POST   /api/v1/wallets/{id}/close

ACCEPTANCE CRITERIA (ALL MUST PASS):
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

TESTS REQUIRED:
- Unit tests: WalletServiceTest (create creates account and balance, freeze prevents transaction, close prevents unfreeze, cannot create for different org), BalanceServiceTest (initialize sets all to zero)
- Integration tests: WalletIntegrationTest (create → retrieve → verify account and balance), list returns only org's wallets, freeze → attempt transaction → verify rejection, close → verify cannot be unfrozen, update metadata → verify changes persisted
- All tests must pass before marking complete.

SCOPE: ONLY Phase 3. Do not build transactions, ledger entries, or any financial processing. Stop after wallets are complete.

Before stopping, append a dated entry to MEMORY.md's Progress Log. Then report: what was built, what tests were written, test results, and any deviations.
```

---

## Phase 4: Ledger Engine

```
You are working on Argent, a developer-first financial infrastructure API.

BEFORE STARTING, READ:
- AGENTS.md (project vision and constraints)
- RULES.md (coding conventions and approved libraries)
- MEMORY.md (current state and recent decisions)
- ARCHITECTURE.md (folder structure, data model, API design)
- PHASES.md, Phase 4 (acceptance criteria and test requirements)

YOUR TASK: Execute Phase 4 — Ledger Engine.

PREREQUISITE: Phase 3 must be complete (ledger entries are created against accounts from wallets).

WHAT TO BUILD:
1. LedgerEntry entity, repository, service
2. Ledger entry creation (debit and credit)
3. Balance verification (debits must equal credits per transaction)
4. Immutable entry enforcement (no updates or deletes)
5. Balance recalculation from ledger entries
6. Reconciliation check endpoint
7. Ledger query by account, transaction, date range

API ENDPOINTS:
GET    /api/v1/ledger/entries
GET    /api/v1/ledger/entries/{id}
GET    /api/v1/ledger/reconcile

ACCEPTANCE CRITERIA (ALL MUST PASS):
- [ ] Creating a transaction produces both debit and credit ledger entries
- [ ] Debit and credit entries for a transaction always sum to zero (balanced)
- [ ] Ledger entries are immutable (application layer prevents updates/deletes)
- [ ] Balance can be recalculated from ledger entries at any time
- [ ] Reconciliation endpoint verifies ledger balance matches stored balance
- [ ] Ledger entries can be queried by account, transaction ID, or date range
- [ ] All ledger entries include organization ID for multi-tenant isolation

TESTS REQUIRED:
- Unit tests: LedgerServiceTest (create entries validates debit/credit balance, stores with correct amounts and types, cannot modify existing, query filters by account/transaction/date), BalanceServiceTest (recalculateFromLedger computes correct balance)
- Integration tests: LedgerIntegrationTest (create two entries → verify they balance, attempt to modify → verify rejection, create entries → recalculate balance → verify matches stored, reconciliation passes when balances match, reconciliation fails when tampered)
- All tests must pass before marking complete.

SCOPE: ONLY Phase 4. Do not build transactions, deposits, transfers, or any financial processing. Stop after ledger engine is complete.

Before stopping, append a dated entry to MEMORY.md's Progress Log. Then report: what was built, what tests were written, test results, and any deviations.
```

---

## Phase 5: Transaction Engine

```
You are working on Argent, a developer-first financial infrastructure API.

BEFORE STARTING, READ:
- AGENTS.md (project vision and constraints)
- RULES.md (coding conventions and approved libraries)
- MEMORY.md (current state and recent decisions)
- ARCHITECTURE.md (folder structure, data model, API design)
- PHASES.md, Phase 5 (acceptance criteria and test requirements)

YOUR TASK: Execute Phase 5 — Transaction Engine.

PREREQUISITE: Phases 3 (wallets) and 4 (ledger) must be complete.

WHAT TO BUILD:
1. Transaction entity, repository, service, controller
2. Transaction engines: DepositEngine, WithdrawalEngine, TransferEngine, RefundEngine, AdjustmentEngine
3. Idempotency key handling (prevent duplicate transactions)
4. Atomic balance updates with optimistic locking
5. Balance validation (sufficient funds, wallet active)
6. Transaction status lifecycle (PENDING → COMPLETED / FAILED)
7. Audit logging for all transaction events

API ENDPOINTS:
POST   /api/v1/deposits
POST   /api/v1/transfers
POST   /api/v1/withdrawals
POST   /api/v1/refunds
POST   /api/v1/adjustments
GET    /api/v1/transactions
GET    /api/v1/transactions/{id}

ACCEPTANCE CRITERIA (ALL MUST PASS):
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

TESTS REQUIRED:
- Unit tests: DepositEngineTest (valid deposit creates entries and updates balance), TransferEngineTest (valid transfer debits source credits destination, insufficient balance throws, same wallet rejected), WithdrawalEngineTest (valid withdrawal creates entries, insufficient balance throws), RefundEngineTest (valid refund reverses, cannot refund already refunded), TransactionServiceTest (idempotency returns cached, frozen wallet rejected, closed wallet rejected)
- Integration tests: TransferIntegrationTest (create wallets → deposit → transfer → verify balances, insufficient balance → verify no change, concurrent transfers → verify no race condition), RefundIntegrationTest (transfer → refund → verify balances), IdempotencyIntegrationTest (send twice → verify one transaction), EndToEndTransactionTest (deposit → transfer → withdraw → verify full lifecycle)
- All tests must pass before marking complete.

SCOPE: ONLY Phase 5. Do not build balance caching, reporting, or frontend. Stop after transaction engine is complete.

Before stopping, append a dated entry to MEMORY.md's Progress Log. Then report: what was built, what tests were written, test results, and any deviations.
```

---

## Phase 6: Balance Engine

```
You are working on Argent, a developer-first financial infrastructure API.

BEFORE STARTING, READ:
- AGENTS.md (project vision and constraints)
- RULES.md (coding conventions and approved libraries)
- MEMORY.md (current state and recent decisions)
- ARCHITECTURE.md (folder structure, data model, API design)
- PHASES.md, Phase 6 (acceptance criteria and test requirements)

YOUR TASK: Execute Phase 6 — Balance Engine.

PREREQUISITE: Phase 5 must be complete (balances are updated by transactions).

WHAT TO BUILD:
1. Balance query service with Redis caching
2. Available balance calculation (current - pending - reserved)
3. Pending balance tracking
4. Reserved balance tracking
5. Cache invalidation on balance changes
6. Balance history endpoint

API ENDPOINTS:
GET    /api/v1/balances/{walletId}
GET    /api/v1/balances/{walletId}/history

ACCEPTANCE CRITERIA (ALL MUST PASS):
- [ ] Balance query returns current, available, pending, and reserved amounts
- [ ] Available balance = current - pending - reserved
- [ ] Balance is cached in Redis for fast retrieval
- [ ] Cache is invalidated when balance changes
- [ ] Balance query returns correct data immediately after transaction
- [ ] Balance history shows balance changes over time
- [ ] Balance queries are isolated per organization

TESTS REQUIRED:
- Unit tests: BalanceServiceTest (available = current - pending - reserved, cache hit returns cached, cache invalidation on update)
- Integration tests: BalanceIntegrationTest (create wallet → deposit → query balance → verify all fields, transfer → query both → verify correctness, cache in Redis → update → cache invalidated → fresh value, concurrent queries return consistent results)
- All tests must pass before marking complete.

SCOPE: ONLY Phase 6. Do not build reporting, frontend, or audit logs. Stop after balance engine is complete.

Before stopping, append a dated entry to MEMORY.md's Progress Log. Then report: what was built, what tests were written, test results, and any deviations.
```

---

## Phase 7: Audit + Reporting

```
You are working on Argent, a developer-first financial infrastructure API.

BEFORE STARTING, READ:
- AGENTS.md (project vision and constraints)
- RULES.md (coding conventions and approved libraries)
- MEMORY.md (current state and recent decisions)
- ARCHITECTURE.md (folder structure, data model, API design)
- PHASES.md, Phase 7 (acceptance criteria and test requirements)

YOUR TASK: Execute Phase 7 — Audit + Reporting.

PREREQUISITE: Phases 2-6 must be complete (audit logs and reports depend on all prior data).

WHAT TO BUILD:
1. Audit log query service (filter by entity, action, user, date)
2. Audit log detail view (before/after state comparison)
3. Transaction report (daily volume, by type, by status)
4. Wallet report (growth, active, frozen, closed)
5. Statement export (CSV format)
6. Report endpoints with date range filtering

API ENDPOINTS:
GET    /api/v1/audit-logs
GET    /api/v1/audit-logs/{id}
GET    /api/v1/reports/daily-volume
GET    /api/v1/reports/wallet-growth
GET    /api/v1/reports/transactions
GET    /api/v1/statements

ACCEPTANCE CRITERIA (ALL MUST PASS):
- [ ] Audit logs are queryable by entity type, action, user, date range
- [ ] Audit log detail shows before and after state for modifications
- [ ] Daily volume report shows total transactions and amount per day
- [ ] Wallet growth report shows new wallets per day
- [ ] Transaction report filters by type, status, date range
- [ ] Statement export returns CSV with all transactions for a date range
- [ ] All reports are scoped to authenticated organization
- [ ] Reports handle empty data gracefully (return empty arrays, not errors)

TESTS REQUIRED:
- Unit tests: AuditServiceTest (query filters by entity, action, user, date), ReportingServiceTest (daily volume aggregates correctly, empty data returns empty results), StatementExportTest (CSV format is correct)
- Integration tests: AuditIntegrationTest (create entity → query → verify entry, update entity → query → verify before/after state), ReportingIntegrationTest (create transactions → query daily volume → verify aggregation, export statement → verify CSV, query with no data → verify empty results)
- All tests must pass before marking complete.

SCOPE: ONLY Phase 7. Do not build frontend. Stop after audit and reporting are complete.

Before stopping, append a dated entry to MEMORY.md's Progress Log. Then report: what was built, what tests were written, test results, and any deviations.
```

---

## Phase 8: Frontend Dashboard

```
You are working on Argent, a developer-first financial infrastructure API.

BEFORE STARTING, READ:
- AGENTS.md (project vision and constraints)
- RULES.md (coding conventions and approved libraries)
- MEMORY.md (current state and recent decisions)
- ARCHITECTURE.md (folder structure and frontend setup)
- DESIGN.md (visual system, colors, typography, components, layout)
- PHASES.md, Phase 8 (acceptance criteria and test requirements)

YOUR TASK: Execute Phase 8 — Frontend Dashboard.

PREREQUISITE: Phases 1-7 must be complete (frontend consumes the backend API).

WHAT TO BUILD:
1. Login / registration pages
2. Dashboard overview (total wallets, total balance, recent transactions)
3. Wallet management page (list, create, freeze, close)
4. Transaction history page (list, filter, search)
5. Balance detail page
6. Ledger view page
7. Audit log viewer
8. Statement export
9. Settings page (organization, API keys)
10. Responsive layout (sidebar navigation)
11. Error handling and loading states

PAGES:
- /login — Email + password login
- /register — New account registration
- / — Overview stats + recent activity
- /wallets — List + create + manage wallets
- /wallets/:id — Single wallet with balance + transactions
- /transactions — Full transaction history with filters
- /ledger — Ledger entries view
- /audit — Audit trail viewer
- /reports — Charts and data tables
- /settings — Organization + API key management

DESIGN REQUIREMENTS (from DESIGN.md):
- Dark sidebar (neutral-950) with white text
- Indigo brand color (#6366F1) for primary actions
- Inter font for UI, JetBrains Mono for financial data
- 8px border radius, no rounded-xl everything
- Dense, information-heavy layouts
- Financial amounts right-aligned in monospace
- Lucide icons only (no emoji, no FontAwesome)
- No gradient hero sections, no decorative illustrations
- Skeleton loading states, not spinners
- Toast notifications for success/error feedback

ACCEPTANCE CRITERIA (ALL MUST PASS):
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
- [ ] Responsive layout works on tablet and desktop

TESTS REQUIRED:
- Component tests: Login form submits and redirects, Wallet list renders from API, Create wallet form validates and calls API, Transaction table renders correct data, Balance display shows all types
- Integration tests: Full login → dashboard → create wallet → deposit → transfer flow via UI, Error states display correctly, Loading states display during API calls
- All tests must pass before marking complete.

SCOPE: ONLY Phase 8. All backend phases must already be complete. Stop after frontend is complete.

Before stopping, append a dated entry to MEMORY.md's Progress Log. Then report: what was built, what tests were written, test results, and any deviations.
```
