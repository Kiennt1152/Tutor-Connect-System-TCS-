# Tutor Connect System (TCS)

A full-stack platform connecting students/parents with tutors and tutoring centers. Built as a capstone project.

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|--------|
| Backend | Spring Boot | 4.0.6 |
| Language | Java | 21 |
| Database | MySQL | 8.x |
| Frontend | React | 19.x |
| Build Tool | Vite | 8.x |
| Type System | TypeScript | 6.x |
| API Docs | Springdoc OpenAPI | 2.8.0 |
| Migration | Flyway | Built-in |
| Auth | JWT + Google OAuth 2.0 | — |
| AI | Groq (LLaMA 3.3) + Gemini 2.0 Flash | — |
| Realtime | WebSocket (STOMP) | — |

## Project Structure

```
Tutor-Connect-System-TCS-/
├── backend/          # Spring Boot API server
│   ├── src/main/     # Application source code
│   ├── src/test/     # Unit & integration tests
│   └── pom.xml       # Maven dependencies
├── frontend/         # React SPA
│   ├── src/          # Components, pages, hooks
│   └── package.json  # npm dependencies
├── docs/             # Project reports & documentation
├── docker-compose.yml
├── Dockerfile
└── .github/          # CI/CD & PR template
```

## Prerequisites

- **Java 21** (Eclipse Temurin recommended)
- **Node.js 22+** with npm
- **MySQL 8.x** running on `localhost:3306`
- **Maven** (included via `mvnw` wrapper)

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Kiennt1152/Tutor-Connect-System-TCS-.git
cd Tutor-Connect-System-TCS-
```

### 2. Configure environment

```bash
cp backend/.env.example backend/.env
# Edit .env with your actual credentials
```

`JWT_SECRET` is required and must contain at least 32 random characters. The backend loads
`backend/.env` when started from either the repository root (such as IntelliJ) or the
`backend` directory. Environment variables still take precedence in deployed environments.

### 3. Set up the database

```sql
CREATE DATABASE tutorconnectsystem;
```

Flyway will automatically run migrations on startup.

### 4. Run the Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend runs at `http://localhost:8080`.
Swagger UI at `http://localhost:8080/swagger-ui.html`.

### 5. Run the Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at `http://localhost:3000` with API proxy to backend.

### 6. Run with Docker (optional)

```bash
docker compose up --build
```

This starts MySQL, Backend, and Frontend (Nginx) together.

## Key Features

| Module | Description |
|--------|------------|
| Identity & RBAC | Registration, Login, OAuth, OTP verification, Role-based access |
| Profile Management | User profiles, CCCD verification, parent-child linking |
| Marketplace | Tutoring classes, tutor search, class enrollment |
| Contracts | Contract generation, OTP signing, lesson scheduling |
| Finance | Wallet, deposits (SePay), withdrawals, escrow, settlements |
| AI Assistant | RAG-powered chatbot with tutor/class recommendations |
| Support | Ticket system, FAQ management, SLA tracking |
| Platform Admin | Dashboard, user management, audit trails, analytics |

## Business Flow Map

This repository is organized around the main business flows below. If you want to understand a feature end to end,
start with the backend service listed in the middle column and then follow the UI page and downstream handlers.

| Flow | Backend entry points | Frontend entry points | What it covers |
|------|----------------------|-----------------------|----------------|
| Verification | `backend/src/main/java/com/tcs/module/identity/controller/VerificationController.java`<br>`backend/src/main/java/com/tcs/module/identity/service/impl/VerificationServiceImpl.java`<br>`backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java` | `frontend/src/features/identity/pages/VerificationPage.tsx` | Upload documents, submit verification, admin review, and supersede older approvals when a newer one is approved. |
| Contract signing & payout info | `backend/src/main/java/com/tcs/module/contract/service/impl/ContractServiceImpl.java` | `frontend/src/features/contract/pages/ContractDetailPage.tsx`<br>`frontend/src/features/teaching/pages/ContractSigningPage.tsx` | OTP signing, contract activation, and storing payout information for refund / settlement flows. |
| Wallet activation & transaction history | `backend/src/main/java/com/tcs/module/finance/service/impl/FinanceServiceImpl.java` | `frontend/src/features/finance/pages/FinancePage.tsx` | Wallet creation, balances, transaction history, and the role-specific wallet dashboard. |
| QR top-up / payment confirmation | `backend/src/main/java/com/tcs/module/finance/service/impl/FinanceServiceImpl.java` | `frontend/src/features/finance/components/DepositModal.tsx` | Create a QR payment session, match the incoming webhook, and turn a pending top-up into a confirmed payment / escrow funding event. |
| Withdrawal / admin transfer | `backend/src/main/java/com/tcs/module/finance/service/impl/FinanceServiceImpl.java` | `frontend/src/features/finance/components/WithdrawalModal.tsx`<br>`frontend/src/features/platform/pages/PlatformWithdrawalsPage.tsx` | Payment method management, payout cooldown, withdrawal request creation, admin approval, and SePay outgoing confirmation. |
| Escrow, dispute, early termination, refund | `backend/src/main/java/com/tcs/module/finance/service/impl/FinanceServiceImpl.java`<br>`backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java` | `frontend/src/features/dispute/components/ClassIssueModal.tsx`<br>`frontend/src/features/platform/pages/PlatformReportsPage.tsx` | Hold escrow, create dispute / early-termination requests, review evidence, decide refund / settlement, and notify the related users. |

## How the finance flow works

1. A user creates a wallet only for roles that can earn or receive money.
2. A center can create a QR top-up session. The backend stores a pending payment transaction first and waits for the webhook.
3. When SePay confirms the payment, the service matches the reference / amount / account information and then funds the related escrow or wallet.
4. A payout method must be saved before withdrawal. The withdrawal request freezes the requested amount first, then waits for admin review or SePay transfer confirmation.
5. Refunds and early termination reuse the same payout information model so the system can notify the right recipient and keep the transfer history consistent.

## Reading the code in order

If you are new to the codebase, this is the shortest path:

1. `backend/src/main/java/com/tcs/module/identity/controller/VerificationController.java`
2. `backend/src/main/java/com/tcs/module/identity/service/impl/VerificationServiceImpl.java`
3. `backend/src/main/java/com/tcs/module/contract/service/impl/ContractServiceImpl.java`
4. `backend/src/main/java/com/tcs/module/finance/service/impl/FinanceServiceImpl.java`
5. `backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java`

Those five files cover the main lifecycle of a user in TCS: verify identity, sign contracts, pay by QR, hold escrow,
handle disputes / early termination, and settle or withdraw money.

## API Documentation

Once the backend is running, visit:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## License

This project is developed for academic purposes as part of a capstone project at FPT University.
