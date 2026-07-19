<div align="center">
  <h1>Argent Financial Infrastructure</h1>
  <p>
    <b>A developer-first financial infrastructure API, built for scale, immutability, and speed.</b>
  </p>
  <p>
    <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java" alt="Java 21" />
    <img src="https://img.shields.io/badge/Spring_Boot-3.4.1-brightgreen?style=for-the-badge&logo=spring-boot" alt="Spring Boot 3" />
    <img src="https://img.shields.io/badge/React-18-blue?style=for-the-badge&logo=react" alt="React 18" />
    <img src="https://img.shields.io/badge/PostgreSQL-16-blue?style=for-the-badge&logo=postgresql" alt="PostgreSQL" />
    <img src="https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge&logo=docker" alt="Docker Enabled" />
  </p>
</div>

<br />

Argent is a modern financial infrastructure system engineered to provide a robust **Ledger**, **Wallet**, and **Transaction Engine**. Designed with strict immutability, idempotent transaction guarantees, and deep environment scoping, Argent allows businesses to securely manage multi-tenant financial data across sandbox and production environments.

> ⚠️ **Note:** This project is actively developed. It is fully dockerized and ready for local testing but has not yet been deployed to a live production environment.

---

## ✨ Key Architectural Highlights

We engineered Argent to solve the hardest problems in financial tech: **race conditions, data mutation, and environment bleed.**

- **Immutable Ledger Engine:** Double-entry accounting where `LedgerEntry` records are completely immutable. Backed by PostgreSQL `BEFORE UPDATE` and `BEFORE DELETE` triggers to strictly enforce database-level immutability against malicious or accidental mutations.
- **Robust Transaction Engine:** A polymorphic Strategy Pattern implementation supporting Deposits, Withdrawals, Transfers, Refunds, and Adjustments. Features built-in idempotency to prevent duplicate charging.
- **Multi-Environment Scoping:** Strict separation of data between `SANDBOX` and `PRODUCTION`. The environment flows logically from `Wallet` → `Account` → `LedgerEntry` ensuring API keys can never access out-of-scope data.
- **Role-Based Access Control (RBAC):** Native JWT and API key authentication supporting `OWNER`, `ADMIN`, and `DEVELOPER` roles for multi-tenant organizational access.
- **Caching & High Availability:** Redis caching applied to balances to dramatically decrease DB load, coupled with smart cache invalidation upon ledger entry creation.

---

## 🛠️ Technology Stack

### **Backend**
- **Java 21** & **Spring Boot 3.4.1**
- **Spring Security** (JWT + API Keys)
- **Spring Data JPA** & **Hibernate**
- **Flyway** (Database migrations)
- **PostgreSQL 16** (Primary Datastore)
- **Redis 7** (Caching Layer)
- **RabbitMQ 3** (Event Messaging)

### **Frontend Dashboard**
- **React 18** (via Vite)
- **TypeScript**
- **Tailwind CSS** (Utility-first styling)
- **Zustand** (Global state management)
- **TanStack Query** (Server state & data fetching)

### **Infrastructure & Quality**
- **Docker & Docker Compose** (Containerized micro-services)
- **JUnit 5 / MockMvc / Testcontainers** (180+ comprehensive backend tests)
- **Vitest & React Testing Library** (Frontend Component & Integration testing)

---

## 🚀 Getting Started

Getting the entire Argent infrastructure running on your local machine takes less than 5 minutes. The environment is entirely containerized using Docker Compose.

### Prerequisites
- Docker
- Docker Compose

### Running the Stack

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/argent.git
   cd argent
   ```

2. **Spin up the infrastructure:**
   ```bash
   # This will pull postgres, redis, rabbitmq, and build the backend/frontend containers
   docker-compose up --build -d
   ```

3. **Verify running containers:**
   ```bash
   docker-compose ps
   ```

4. **Access the Application:**
   - **Frontend Dashboard:** [http://localhost:3000](http://localhost:3000)
   - **Backend API:** [http://localhost:8080](http://localhost:8080)
   - **RabbitMQ Management:** [http://localhost:15672](http://localhost:15672) *(u: argent, p: devpassword)*

---

## 🧪 Testing and Quality Assurance

Argent prioritizes extreme stability due to the critical nature of financial data.

- **Backend:** Over **180+ tests** (Unit + Integration) covering the core Ledger and Transaction engines, asserting atomicity, isolation, and handling optimistic locking failures (`409 Conflict`). Run via `./gradlew test`.
- **Frontend:** Component tests and full mocked routing integration tests utilizing `vitest` and `jsdom`.

## 🗺️ Roadmap (V2)
While V1 provides a comprehensive Dashboard and API for ledger and wallet management, future versions will implement:
- **Webhook Subscriptions:** Asynchronous notifications for balance thresholds and transaction status changes.
- **Idempotency Keys:** Client-provided UUID headers for exact API retry safety.
- **Advanced Reporting:** Exporting general ledgers in various financial formats.

---
*Argent is built by engineers passionate about highly resilient, developer-first tooling.*
