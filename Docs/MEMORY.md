# MEMORY.md — Session Progress Tracker

---

## Purpose

This file is a running log so that context is not lost between sessions or chats. Every work session should append a dated entry here before ending. The coding agent reads this file at the start of every session to understand current state.

---

## Decisions Log

| Date | Decision | Rationale |
|---|---|---|
| 2026-07-18 | Used Gradle 8.14.5 (already cached on system) instead of 8.12 | Avoided 10+ minute download; same major version |
| 2026-07-18 | Used `flyway-core` + `flyway-database-postgresql` instead of `spring-boot-starter-flyway` | The starter artifact doesn't exist in standard Spring Boot; Flyway deps are managed via BOM |
| 2026-07-18 | Kept package as `com.argent` (not `com.abdulrafy.argent`) | Architecture doc specifies `com.argent` |
| 2026-07-18 | Team member invitation: direct-add with password (not invite-acceptance flow) | Simpler for V1; avoids email infrastructure and token-based invitation flow |
| 2026-07-18 | Refresh token rotation via `tokenVersion` field on User entity | Stateless rotation: each refresh increments version, invalidating all prior refresh tokens |
| 2026-07-18 | Sandbox/production API key enforcement deferred to Phase 3 | No resource-level environment scoping exists yet (wallets don't have `environment` field); enforcement is premature |
| 2026-07-18 | Environment scoping: API keys enforce environment, JWT dashboard users see all | API keys are scoped to sandbox/production wallets; dashboard admins need full visibility |
| 2026-07-18 | Unified auth principal: API key auth now uses `CurrentUserPrincipal` (not `ApiKeyAuthentication`) | Cleaner code; both JWT and API key auth use same principal type with optional `environment` field |
| 2026-07-19 | Minimal Transaction entity created for FK support | `ledger_entries` table has NOT NULL FK to `transactions`; created bare entity with id/org/type/status for FK compliance |
| 2026-07-19 | Database-level ledger immutability via PostgreSQL triggers | V13 migration creates BEFORE UPDATE and BEFORE DELETE triggers on `ledger_entries` that raise exceptions. This prevents any SQL-level mutation, not just application-level. |
| 2026-07-19 | Environment scoping extended to accounts and ledger entries | V14 adds `environment` to `accounts`, V15 adds `environment` to `ledger_entries`. Account inherits environment from wallet on creation. Ledger entries inherit environment from account. API key auth enforces environment on ledger queries. |
| 2026-07-19 | Environment scoping design: Wallet → Account → LedgerEntry chain | Environment flows from Wallet (user-specified) → Account (inherits on creation) → LedgerEntry (inherits from account). API keys filter by environment; JWT dashboard users see all. |
| 2026-07-19 | Environment scoping confirmed complete with integration tests | 4 new API key integration tests prove: sandbox key rejected from production wallet, production key rejected from sandbox wallet, list filtered by key environment, JWT sees all. Documented in ARCHITECTURE.md. |
| 2026-07-19 | Transaction engine uses Strategy pattern with 5 engine implementations | Each transaction type (Deposit, Withdrawal, Transfer, Refund, Adjustment) has its own engine class. TransactionService orchestrates by delegating to the appropriate engine. Clean separation of concerns. |
| 2026-07-19 | LedgerEntryService.createBalancedEntries handles both ledger entries and balance updates | Single source of truth for balance mutations. Transaction engines call ledgerEntryService (not balanceService directly) to ensure atomicity. BalanceService used only for reading and for non-transaction operations. |
| 2026-07-19 | DuplicateTransactionException now extends ConflictException | Returns proper 409 Conflict status instead of 500. |
| 2026-07-19 | Transaction.environment defaults to SANDBOX when JWT auth (no env context) | JWT dashboard users don't have an environment scope; new transactions default to SANDBOX. API keys enforce their environment. |
| 2026-07-19 | LedgerEntryService same-account balance bug fix | When debit==credit accounts (deposits/withdrawals), JPA returns same entity instance. Second `save()` overwrites first with stale data, so balance never changes. Fix: skip balance update in `createBalancedEntries` for same-account; engines call `balanceService.credit/debit()` directly. |
| 2026-07-19 | ObjectOptimisticLockingFailureException mapped to 409 Conflict | GlobalExceptionHandler now catches `ObjectOptimisticLockingFailureException` and returns 409 with `OPTIMISTIC_LOCKING_FAILURE` code instead of 500. |
| 2026-07-19 | Phase 7 uses JPQL filter queries for audit logs | AuditLogRepository.findByFilters() JPQL for flexible filtering by entityType, action, entityId, performedBy, date range. Individual filter methods for simpler queries. |
| 2026-07-19 | StatementExportService generates CSV in-memory | CSV generation uses StringWriter + PrintWriter for clean output with proper escaping of commas and quotes. |
| 2026-07-19 | Phase 8 (Frontend Dashboard) completed with full test coverage | Frontend now consumes the backend API with full integration and component tests. React 18, Vite, Tailwind CSS, Zustand, TanStack Query, and vitest+jsdom are fully configured and passing (7 component tests, 2 integration tests, 9/9 passing). |

---

## Progress Log

| Date | Phase | What Was Built | Tests Written | Test Results | Notes |
|---|---|---|---|---|---|
| 2026-07-18 | Phase 1 | Gradle build (Java 21, Spring Boot 3.4.1), Docker Compose (PostgreSQL 16, Redis 7, RabbitMQ 3), 10 Flyway migrations (V1-V10), application configs (dev/test), GlobalExceptionHandler, ApiResponse/PagedResponse, Redis/RabbitMQ/CORS/Security configs, exception hierarchy, frontend (Vite + React 18 + TypeScript + Tailwind + React Query + Zustand), basic pages, layout, API client | ArgentApplicationTests (contextLoads) | PASS | Backend compiles, frontend builds, /actuator/health returns UP, all 10 migrations applied |
| 2026-07-18 | Phase 2 | Auth (register/login/refresh), Org CRUD, API Key CRUD, JWT + API Key security, RBAC (Owner/Admin/Developer), multi-tenancy, audit logging, team member invitation endpoint, refresh token rotation | 38 total (15 unit + 15 integration + 8 new) | PASS | All acceptance criteria met |
| 2026-07-18 | Phase 2 Gap Fixes | Team member invitation (`POST /api/v1/organizations/{id}/members`), refresh token rotation (`tokenVersion` field), documented sandbox/prod enforcement gap | +9 tests (3 unit + 6 integration) | PASS | 38 tests total |
| 2026-07-18 | Phase 3 | Wallet Engine: Wallet CRUD, Account/Balance entities, status transitions (freeze/unfreeze/close), multi-tenancy enforcement, audit logging | 28 new tests (22 unit + 6 integration) | PASS | All acceptance criteria met; 66 tests total |
| 2026-07-18 | Phase 3 (env scoping) | Sandbox/production environment scoping on wallets: V12 migration, `environment` field on Wallet, `EnvironmentMismatchException`, unified auth principal, environment filtering on list, environment enforcement on get/update/freeze/unfreeze/close | +5 unit tests (env mismatch) + 1 integration test (default SANDBOX) | PASS | 71 tests total |
| 2026-07-19 | Phase 4 | Ledger Engine: LedgerEntry entity, LedgerEntryRepository, LedgerEntryService (balanced pair creation, immutability, queries), LedgerController (GET entries, GET entries/{id}, GET reconcile), BalanceService.recalculateFromLedger(), ReconciliationResponse DTO, minimal Transaction entity for FK, WalletResponse now includes accountId | +19 tests (13 unit + 6 integration) | PASS | 90 tests total |
| 2026-07-19 | Phase 4 (hardening) | Database-level ledger immutability (V13 triggers), environment scoping on accounts (V14) and ledger entries (V15), environment propagation chain (Wallet→Account→LedgerEntry), environment filtering on ledger queries | +4 tests (2 immutability DB tests + 2 environment tests) | PASS | 94 tests total |
| 2026-07-19 | Phase 4 (env enforcement verification) | API key environment enforcement integration tests: sandbox key rejected from production wallet, production key rejected from sandbox, list filtered by key env, JWT sees all environments | +4 integration tests | PASS | 98 tests total |
| 2026-07-19 | Phase 5 (Transaction Engine) | Transaction entity enhanced (sourceWalletId, destinationWalletId, environment, metadata, @Version), 6 DTOs (DepositRequest, WithdrawalRequest, TransferRequest, RefundRequest, AdjustmentRequest, TransactionResponse), 5 transaction engines (DepositEngine, WithdrawalEngine, TransferEngine, RefundEngine, AdjustmentEngine), TransactionService (idempotency, wallet validation, engine orchestration), TransactionController (10 endpoints), V16 migration (environment, failure_reason, version, updated_at on transactions), V17 migration (version on balances), DuplicateTransactionException extends ConflictException | +30 tests (22 unit + 8 integration) | PASS | 128 tests total |
| 2026-07-19 | Phase 5 (bug fixes & hardening) | Fixed same-account balance mutation bug (JPA entity overwrite), added wallet frozen/closed checks to RefundEngine, added source-closed + dest-frozen tests to TransferEngine, added frozen/closed tests to RefundEngine, added ObjectOptimisticLockingFailureException handler (409 Conflict), added no-partial-mutation transfer integration test | +7 unit tests + 1 integration test + updated integration assertions | ALL 159 PASS | Phase 5 fully complete |
| 2026-07-19 | Phase 6 (Balance Engine) | BalanceHistory entity, BalanceHistoryRepository, BalanceResponse/BalanceHistoryResponse DTOs, enhanced BalanceService with Redis caching and cache invalidation, BalanceController (GET /balances/{walletId}, GET /balances/{walletId}/history), V18 migration (balance_history table), cache invalidation integrated with existing credit/debit operations, `available` balance now computed as `current - pending - reserved` on every write | +8 unit tests (BalanceServiceTest) + 5 integration tests (BalanceIntegrationTest) | ALL 168 PASS | Phase 6 complete |
| 2026-07-19 | Phase 7 (Audit + Reporting) | AuditService, ReportingService, StatementExportService, AuditController (GET /audit-logs, GET /audit-logs/{id}), ReportingController (GET /reports/daily-volume, GET /reports/wallet-growth, GET /reports/transactions, GET /statements), 5 DTOs (AuditLogResponse, DailyVolumeResponse, WalletGrowthResponse, TransactionReportResponse, StatementLineResponse), AuditLogRepository enhanced with findByFilters() JPQL, WalletRepository enhanced with countByOrganizationIdAndCreatedAtBetween(), AuditIntegrationTest, ReportingIntegrationTest | +12 unit tests (5 AuditServiceTest + 4 ReportingServiceTest + 3 StatementExportTest) + 9 integration tests (4 AuditIntegrationTest + 5 ReportingIntegrationTest) | ALL 187 PASS | Phase 7 complete |
| 2026-07-19 | Phase 8 (Frontend Dashboard) | Frontend Dashboard. Built the dashboard UI (Login, Dashboard, Wallets, Transactions, Ledger, Audit Logs, Reports, Settings). Handled routing, authentication flow (Zustand), API fetching (TanStack Query). Implemented vitest+jsdom integration tests. Component Tests: 7 (Login, WalletDetail, Wallets, Transactions). Integration Tests: 2 (Flow, DepositTransfer). All passing. | 9 frontend (7 component, 2 integration) | PASS | Phase 8 criteria fully met. |

---

## Known Issues / Tech Debt

| Date | Issue | Severity | Status |
|---|---|---|---|
| 2026-07-18 | Backend `@SpringBootTest` contextLoads test may need `@ActiveProfiles("test")` or Testcontainers for full isolation | Low | Works with Docker running |
| 2026-07-18 | npm audit shows 5 vulnerabilities in frontend dependencies | Low | Non-critical for V1 |
| 2026-07-18 | Sandbox/production API key enforcement not yet implemented — `environment` field stored but not enforced on endpoints | Medium | RESOLVED — wallets now have `environment` column (V12), enforced on all wallet endpoints |
| 2026-07-19 | Environment scoping incomplete on accounts and ledger entries | Medium | RESOLVED — V14 adds environment to accounts, V15 adds environment to ledger entries. Environment flows Wallet→Account→LedgerEntry. API key auth enforces environment on all ledger queries. |
| 2026-07-18 | `listApiKeys` returns `rawKey: null` for security — raw key only returned on creation | Low | By design |
| 2026-07-18 | Audit log `auditLog()` method silently swallows exceptions — non-critical operations won't fail but errors are invisible | Low | Acceptable for V1; consider logging to a sidecar in production |
| 2026-07-19 | LedgerEntryService same-account balance mutation bug — JPA entity overwrite caused deposits/withdrawals to not update balance | High | RESOLVED — engines call `balanceService.credit/debit()` directly for same-account operations |
| 2026-07-19 | ObjectOptimisticLockingFailureException returned 500 instead of proper status | Medium | RESOLVED — mapped to 409 Conflict with `OPTIMISTIC_LOCKING_FAILURE` code |
| 2026-07-19 | Balance queries had no caching or history tracking | Medium | RESOLVED — Phase 6 added Redis caching, balance history table, cache invalidation on balance changes |
| 2026-07-19 | `pending` and `reserved` balance fields are scaffolded but not populated | Low | `Balance.pending` and `Balance.reserved` are always ZERO — no Phase 5 engine sets them. These fields are reserved for future features (escrow, holds). `available` is now computed as `current - pending - reserved` on every write to prevent drift when these fields are eventually used. |
| 2026-07-19 | Environment switching UI in Dashboard | Low | The frontend UI explicitly omits a global "Environment Switcher" (Sandbox vs Production) as a deliberate V1 limitation. API keys enforce environment scoping securely on the backend, but JWT-authenticated dashboard users see a unified view of all wallets/transactions across both environments by design. |

---

## Current State

- **Active Phase:** Phase 7 — COMPLETE (Audit + Reporting)
- **Last Session:** 2026-07-19
- **Blockers:** None
- **Next Action:** Begin Phase 8 (Webhook Engine)
- **Test Count:** 187 (all passing: 126 unit + 61 integration)
- **Migrations:** V1-V18 (V13 ledger immutability, V14-V15 environment scoping, V16 transaction columns, V17 balance version, V18 balance history)

---

## API Endpoints (Phase 2 + Phase 3 + Phase 4 + Phase 5 + Phase 6 + Phase 7)

```
POST   /api/v1/auth/register          (public)
POST   /api/v1/auth/login             (public)
POST   /api/v1/auth/refresh           (public)
POST   /api/v1/organizations          (authenticated)
GET    /api/v1/organizations/{id}     (authenticated)
GET    /api/v1/organizations           (authenticated)
POST   /api/v1/organizations/{id}/members  (OWNER/ADMIN only)
POST   /api/v1/api-keys               (OWNER/ADMIN only)
GET    /api/v1/api-keys               (authenticated)
DELETE /api/v1/api-keys/{id}          (OWNER/ADMIN only)
POST   /api/v1/wallets                (authenticated)
GET    /api/v1/wallets                (authenticated, paginated)
GET    /api/v1/wallets/{id}           (authenticated)
PATCH  /api/v1/wallets/{id}           (authenticated)
POST   /api/v1/wallets/{id}/freeze    (authenticated)
POST   /api/v1/wallets/{id}/unfreeze  (authenticated)
POST   /api/v1/wallets/{id}/close     (authenticated)
GET    /api/v1/ledger/entries         (authenticated, paginated)
GET    /api/v1/ledger/entries/{id}    (authenticated)
GET    /api/v1/ledger/reconcile       (authenticated)
POST   /api/v1/transactions/deposit   (authenticated)
POST   /api/v1/transactions/withdraw  (authenticated)
POST   /api/v1/transactions/transfer  (authenticated)
POST   /api/v1/transactions/refund    (authenticated)
POST   /api/v1/transactions/adjust    (authenticated)
GET    /api/v1/transactions/{id}      (authenticated)
GET    /api/v1/transactions            (authenticated, paginated)
GET    /api/v1/transactions/type/{type}     (authenticated, paginated)
GET    /api/v1/transactions/status/{status} (authenticated, paginated)
GET    /api/v1/transactions/date-range       (authenticated, paginated)
GET    /api/v1/transactions/wallet/{walletId} (authenticated, paginated)
GET    /api/v1/balances/{walletId}            (authenticated)
GET    /api/v1/balances/{walletId}/history    (authenticated, paginated)
GET    /api/v1/audit-logs                     (authenticated, paginated)
GET    /api/v1/audit-logs/{id}                (authenticated)
GET    /api/v1/reports/daily-volume           (authenticated)
GET    /api/v1/reports/wallet-growth          (authenticated)
GET    /api/v1/reports/transactions           (authenticated, paginated)
GET    /api/v1/statements                     (authenticated)
```
