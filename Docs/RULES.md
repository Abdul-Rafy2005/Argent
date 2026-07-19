# RULES.md — Constraints for the AI Agent

---

## Approved Libraries & Frameworks

### Backend (Java/Spring Boot)

| Category | Approved | Banned |
|---|---|---|
| Framework | Spring Boot 3.x | Micronaut, Quarkus, Vert.x |
| Language | Java 21 | Kotlin (revisit V2) |
| Database | PostgreSQL 16 + Flyway | MySQL, MongoDB, H2 in production |
| Cache | Redis 7 + Spring Data Redis | EhCache, Caffeine (for distributed data) |
| Messaging | RabbitMQ + Spring AMQP | Kafka, ActiveMQ, ZeroMQ |
| Auth | Spring Security + JWT (jjwt) | OAuth2 providers, Keycloak (V1) |
| Validation | Jakarta Validation (Hibernate) | Custom manual validation |
| Testing | JUnit 5 + Mockito + Testcontainers | JUnit 4, EasyMock, PowerMock |
| Build | Gradle (Kotlin DSL) | Maven |
| API Docs | SpringDoc OpenAPI | Swagger 2, manual OpenAPI |
| Logging | SLF4J + Logback | System.out.println, Log4j 1.x |
| Mapping | MapStruct | ModelMapper, manual mapping |
| HTTP Client | Spring WebClient (for webhooks) | OkHttp, Apache HttpClient |

### Frontend (React/TypeScript)

| Category | Approved | Banned |
|---|---|---|
| Framework | React 18 + TypeScript | Vue, Angular, Svelte |
| Build | Vite | Webpack, CRA |
| Styling | Tailwind CSS | CSS-in-JS (styled-components, Emotion), CSS Modules |
| State (server) | TanStack Query (React Query) | SWR, Redux for server state |
| State (client) | Zustand | Redux, Jotai, Recoil |
| Forms | React Hook Form + Zod | Formik, custom form handling |
| Tables | TanStack Table | AG Grid, react-table v7 |
| Routing | React Router v6 | TanStack Router, file-based routing |
| Icons | Lucide React | FontAwesome, Material Icons, emoji as icons |
| HTTP | Axios | fetch API (use axios for interceptors) |
| Date/Date | date-fns | moment.js, dayjs |
| Charts | Recharts | Chart.js, D3, Victory |
| UI Components | Custom components + Tailwind | shadcn/ui, Radix (build from scratch for consistency) |

---

## Coding Conventions

### General

- **Language:** English for all code, comments, commit messages, documentation.
- **No comments** unless explaining a non-obvious business rule or workaround. Code should be self-documenting. If you need a comment, consider whether the code can be renamed or restructured instead.
- **No premature optimization.** Write clear code first. Optimize only when a specific performance problem is identified.

### Java (Backend)

- **Naming:**
  - Classes: PascalCase (`WalletService`, `TransactionController`)
  - Methods: camelCase (`createWallet`, `processTransfer`)
  - Constants: UPPER_SNAKE_CASE (`MAX_RETRY_COUNT`)
  - Packages: lowercase (`com.argent.module.wallet`)
  - DTOs: Request suffix for inputs (`CreateWalletRequest`), Response suffix for outputs (`WalletResponse`)
- **File size limit:** 300 lines maximum per file. If a file exceeds this, split it.
- **Method size limit:** 40 lines maximum per method. Extract private methods for clarity.
- **No public fields** on entities or DTOs. Use Lombok `@Data` or explicit getters/setters.
- **Always use BigDecimal** for financial amounts. Never float or double.
- **Always use UUID** for primary keys. Never auto-increment integers.
- **Always use enums** for status fields and type discriminators. Never raw strings.
- **Exception handling:** Throw specific business exceptions (e.g., `InsufficientBalanceException`, `WalletFrozenException`). Never catch and swallow exceptions silently.
- **Logging:** Use structured logging with MDC context (organizationId, transactionId, userId). Log at appropriate levels: ERROR for failures, WARN for recoverable issues, INFO for business events, DEBUG for detailed flow.

### TypeScript (Frontend)

- **Naming:**
  - Components: PascalCase (`WalletCard`, `TransactionTable`)
  - Functions/hooks: camelCase (`useWallets`, `formatCurrency`)
  - Types/interfaces: PascalCase, no `I` prefix (`Wallet`, not `IWallet`)
  - Files: PascalCase for components (`WalletCard.tsx`), camelCase for utilities (`formatCurrency.ts`)
- **File size limit:** 250 lines maximum per component file.
- **Props:** Always define explicit TypeScript interfaces for component props. No `any` types.
- **No inline styles.** All styling through Tailwind classes.
- **No relative imports.** Use path aliases (`@/components/Button`).

### SQL (Migrations)

- **File naming:** `V{number}__{description}.sql` (Flyway convention)
- **Always use UUID** for primary keys
- **Always include** `created_at` and `updated_at` timestamps
- **Always use** `NOT NULL` constraints where applicable
- **Always include** foreign key constraints
- **Never modify** an existing migration. Create a new one.

---

## Error Handling Philosophy

### Principle: Fail Loud, Recover Gracefully

1. **Never swallow exceptions.** Every caught exception must be either re-thrown or logged at ERROR level with full context.
2. **Never return null** as a success indicator. Use empty Optional or throw specific exceptions.
3. **Business exceptions** should carry error codes that map to API error responses.
4. **System exceptions** (database down, connection timeout) should be caught at the boundary and returned as generic 500 errors without exposing internals.
5. **All errors** returned to clients must include:
   - Machine-readable error code (e.g., `INSUFFICIENT_BALANCE`)
   - Human-readable message
   - Relevant context (not stack traces)

### Exception Hierarchy

```
ArgentException (base)
├── ValidationException          (400)
├── UnauthorizedException        (401)
├── ForbiddenException           (403)
├── NotFoundException             (404)
├── ConflictException            (409)
├── InsufficientBalanceException (422)
├── WalletFrozenException        (422)
├── WalletClosedException        (422)
├── DuplicateTransactionException (409)
└── InternalServerException      (500)
```

---

## Security Rules

### Absolute Rules — Never Violate

1. **No secrets in code.** No API keys, passwords, JWT secrets, or database credentials in source files. Use environment variables.
2. **No secrets in git.** The `.env` file is gitignored. Never commit it. If a secret is accidentally committed, rotate it immediately and clean git history.
3. **Parameterized queries only.** Never construct SQL with string concatenation. Use JPA/Hibernate parameterized queries or Spring Data repositories.
4. **Input validation at every boundary.** Every controller method must validate its input using Jakarta Validation annotations. Never trust client data.
5. **Sanitize all user input.** Escape output, validate input types, enforce length limits.
6. **API keys are hashed.** Never store raw API keys. Store only the hash. Show the raw key once at creation, never again.
7. **Passwords are hashed.** BCrypt with sufficient work factor. Never MD5, SHA-1, or plain text.
8. **No sensitive data in logs.** Never log passwords, API keys, JWT tokens, or full credit card numbers.
9. **CORS is restricted.** Only allow known origins. Never use `*` in production.
10. **Rate limiting is mandatory.** All public endpoints must have rate limiting to prevent abuse.

### Financial Security

1. **ACID transactions** for all financial operations. No exceptions.
2. **Idempotency keys** on all write operations. Prevent duplicate transactions.
3. **Double-entry invariant** must be enforced at the database level, not just in application code.
4. **Optimistic locking** on balance updates to prevent race conditions.
5. **Ledger entries are append-only.** No UPDATE or DELETE operations on the ledger_entries table.

---

## What the Agent Must NEVER Do Without Asking

The following actions require explicit human approval before execution:

1. **Delete or modify database migrations.** Never alter an existing migration file. Always create a new one.
2. **Change the database schema** outside of a migration (e.g., adding columns directly to entities without a migration).
3. **Add a new dependency** to build.gradle.kts or package.json without confirmation.
4. **Modify authentication or authorization logic** without review.
5. **Touch payment/financial processing code** (transaction engine, ledger service, balance service) without discussing the change first.
6. **Change the API contract** (endpoint paths, request/response formats) without confirmation.
7. **Modify Docker or deployment configuration** without confirmation.
8. **Delete files** that are not clearly temporary (e.g., not test files or build artifacts).
9. **Change the tech stack** (e.g., switching from PostgreSQL to MySQL).
10. **Add new modules or services** without confirmation.

---

## Testing Requirements

### Hard Rule: No Phase is Complete Without Passing Tests

Every phase in PHASES.md specifies exactly which tests must be written. Do not skip them.

### Unit Tests

- **Coverage target:** 80% minimum for service layer, 60% overall.
- **What to unit test:**
  - Service methods with business logic
  - Validation logic
  - Balance calculation logic
  - Transaction type routing
  - Edge cases (insufficient balance, frozen wallet, duplicate idempotency key)
- **What NOT to unit test:**
  - Controllers (test via integration tests)
  - Repository methods (test via integration tests)
  - DTOs and mappers (trivial code)
- **Framework:** JUnit 5 + Mockito
- **Naming:** `should_[expectedBehavior]_when_[condition]`

### Integration Tests

- **What to integration test:**
  - Full API request → response flows
  - Database operations with real PostgreSQL (via Testcontainers)
  - Authentication and authorization flows
  - Transaction processing end-to-end
  - Balance updates after transactions
- **Framework:** JUnit 5 + Spring Boot Test + Testcontainers
- **Database:** Fresh PostgreSQL container per test class (or per test for isolation)
- **API tests:** MockMvc for controller tests, or WebTestClient for full HTTP tests

### Frontend Tests

- **Component tests:** React Testing Library for component rendering and interaction
- **Hook tests:** React Testing Library for custom hooks
- **E2E tests:** Not required for V1 (add in V2 with Playwright)
- **Coverage target:** 70% minimum for components with business logic

### Test File Organization

```
src/test/java/com/argent/
├── module/
│   ├── wallet/
│   │   ├── WalletServiceTest.java          # Unit tests
│   │   └── WalletControllerTest.java       # Integration tests
│   ├── transaction/
│   │   ├── TransactionServiceTest.java
│   │   └── TransactionControllerTest.java
│   └── ...
└── integration/
    └── WalletTransactionIntegrationTest.java  # Cross-module tests
```

### Running Tests

```bash
# Backend
./gradlew test                    # Unit tests
./gradlew integrationTest         # Integration tests
./gradlew testIntegrationTest     # All tests

# Frontend
npm run test                      # Unit tests
npm run test:coverage             # With coverage report
```

---

## Git Workflow

### Branch Naming

```
feat/wallet-crud           # New feature
fix/balance-race-condition  # Bug fix
refactor/transaction-engine # Code improvement
test/ledger-integration     # Test additions
docs/api-documentation      # Documentation
chore/dependency-update     # Maintenance
```

### Commit Messages

Format: `<type>(<scope>): <description>`

Examples:
```
feat(wallet): add freeze and unfreeze endpoints
fix(transaction): prevent duplicate transfers with idempotency check
refactor(ledger): extract entry creation to separate service
test(balance): add integration tests for concurrent balance updates
```

### Pull Request Rules

- One logical change per PR. Don't bundle unrelated changes.
- PR title matches commit message format.
- All tests must pass before merge.
- No direct pushes to `main`.
- PR description explains what changed and why.

### Protected Branches

- `main` is protected. No direct pushes. All changes via PR.
- `develop` branch for integration (optional for V1 — can work directly off `main` with feature branches).

---

## Documentation Requirements

- **API endpoints:** Documented via OpenAPI annotations in controller classes. SpringDoc generates the spec automatically.
- **Business rules:** Documented in code comments where the rule is not obvious from the code itself.
- **Architecture decisions:** Recorded in MEMORY.md's Decision Log.
- **Setup instructions:** In README.md with clear steps to run locally.
- **No standalone API documentation files** that can go stale. The OpenAPI spec is the source of truth.
