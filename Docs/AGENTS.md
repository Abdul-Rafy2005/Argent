# AGENTS.md — AI Agent Context

---

## Project Vision

Argent is a developer-first financial infrastructure API that enables startups to build wallets, credits, rewards, internal payments, refunds, commissions, and balance management without implementing financial accounting themselves. It provides double-entry bookkeeping, immutable ledger entries, and balance management behind a simple REST API — so developers can focus on their product, not their books.

---

## Document Map

| Document | Purpose | When to Consult |
|---|---|---|
| `PRD.md` | Product requirements, user stories, scope | Before starting any new feature to verify it's in scope |
| `ARCHITECTURE.md` | Tech stack, folder structure, data model, API design | Before creating new files, modules, or endpoints |
| `RULES.md` | Coding conventions, banned libraries, security rules, testing requirements | Before writing any code — this is the constraint reference |
| `PHASES.md` | Build plan with phases, acceptance criteria, and test requirements | Before starting any new phase or task |
| `DESIGN.md` | Visual system, colors, typography, components, layout | Before building any UI component or page |
| `MEMORY.md` | Running log of decisions, progress, and tech debt | At the start of every session to understand current state |
| `PHASE_PROMPTS.md` | Copy-paste prompts for executing each phase | When starting a new phase — use the prompt for that phase |

---

## Current Phase

**Phase 7 — COMPLETE (Audit + Reporting)**

Audit + Reporting is operational: AuditService with JPQL filter queries, ReportingService with daily volume/wallet growth/transaction reports, StatementExportService for CSV export. AuditController exposes 2 endpoints (GET /audit-logs, GET /audit-logs/{id}), ReportingController exposes 4 endpoints (GET /reports/daily-volume, GET /reports/wallet-growth, GET /reports/transactions, GET /statements). Org isolation enforced. All 187 tests passing. Ready for Phase 8 (Webhook Engine).

---

## Hard Constraints (Read Every Session)

1. **BigDecimal for all financial amounts.** Never float, never double. No exceptions.
2. **UUID for all primary keys.** Never auto-increment integers.
3. **Ledger entries are immutable.** No UPDATE or DELETE on ledger_entries. Ever.
4. **Idempotency keys on all write operations.** Prevent duplicate transactions.
5. **No new dependencies without asking.** Check RULES.md approved list first.
6. **No touching financial code without asking.** Transaction engine, ledger service, balance service require human review.
7. **Tests must pass before marking any phase complete.** See PHASES.md for specific test requirements.

---

## Session Protocol

1. **Read MEMORY.md first** to understand current state and recent decisions.
2. **Read the current phase in PHASES.md** to know what to build.
3. **Read RULES.md** before writing any code.
4. **Build only the current phase.** No scope creep into future phases.
5. **Write tests as specified in PHASES.md.** Run them. They must pass.
6. **Update MEMORY.md** before ending the session — add a dated Progress Log entry.
7. **Report to the user** what was built, what tests were written, test results, and any deviations.

---

## What This Agent Should Never Do

- Mark a phase complete without tests passing
- Add dependencies not on the approved list
- Modify database migrations
- Change API contracts without confirmation
- Touch authentication or payment code without discussion
- Skip ahead to future phases
- Make architectural decisions not documented in these files
