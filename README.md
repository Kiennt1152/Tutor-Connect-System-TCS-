# 🎓 Tutor Connect System (TCS)

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x%20%2F%206.x-blue.svg)](https://www.typescriptlang.org/)
[![Database](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![SonarQube](https://img.shields.io/badge/SonarQube%20Security-Grade%20A-success.svg)](http://localhost:9000)
[![License](https://img.shields.io/badge/License-Academic%20Capstone-lightgrey.svg)]()

> **Tutor Connect System (TCS)** is an integrated, enterprise-grade hybrid platform combining an **Open Tutoring Marketplace (C2C/B2C)** with a **B2B SaaS CRM for Tutoring Centers**, fortified by a **FinTech-enabled Smart Escrow Wallet** and an **AI-powered Matchmaking & Advisory Engine**.

---

## 📑 Table of Contents

- [1. Problem Statement & Value Proposition](#1-problem-statement--value-proposition)
- [2. System Architecture & Actors](#2-system-architecture--actors)
- [3. Core Business Flows (BF-01 → BF-10)](#3-core-business-flows-bf-01--bf-10)
- [4. Tech Stack & Version Compatibility](#4-tech-stack--version-compatibility)
- [5. Code Quality & SonarQube Metrics](#5-code-quality--sonarqube-metrics)
- [6. Project Structure](#6-project-structure)
- [7. Getting Started & Installation](#7-getting-started--installation)
  - [Prerequisites](#prerequisites)
  - [1. Clone & Configure Environment](#1-clone--configure-environment)
  - [2. Database Setup & Flyway Migration](#2-database-setup--flyway-migration)
  - [3. Run the Backend](#3-run-the-backend)
  - [4. Run the Frontend](#4-run-the-frontend)
  - [5. Run with Docker Compose](#5-run-with-docker-compose)
- [8. API Documentation (Swagger / OpenAPI)](#8-api-documentation-swagger--openapi)
- [9. Testing & Quality Assurance](#9-testing--quality-assurance)
- [10. Development Team](#10-development-team)

---

## 1. Problem Statement & Value Proposition

### The Market Challenge
The private tutoring market in Vietnam exceeds hundreds of millions of USD annually but remains heavily fragmented, operating largely through informal Facebook groups and traditional intermediary centers. This leads to critical vulnerabilities:
1. **Financial Fraud & Deposit Scams (GAP-01):** Tutors and clients routinely suffer from fee disputes, upfront deposit scams, and unpaid tuition.
2. **Lack of Identity & Credential Verification (GAP-02):** Unverified profiles lead to unqualified tutors, falsified diplomas, and zero consumer trust.
3. **Fragmented User Experience (GAP-03):** Manual coordination without intelligent recommendations or 24/7 automated guidance.
4. **Administrative Overhead for Centers (GAP-04):** Traditional tutoring centers manage hundreds of students and tutors using spreadsheets and paper logs, resulting in severe operational bottlenecks.

### The TCS Solution
TCS bridges these gaps by providing:
- 🛡️ **Smart Escrow Wallet & E-Contracting:** Tuition funds are locked in escrow upon matching and disbursed lesson-by-lesson, eliminating **90%** of financial scams.
- 🔍 **Strict Profile Verification (Zero-Trust):** Digital KYC/KYB with CCCD/citizen ID, degree certificates, and business licenses verified by administrators.
- 🏢 **B2B SaaS CRM for Tutoring Centers:** Bulk Excel student import, internal tutor roster management, and automated recruitment pipelines, reducing administrative overhead by **70%**.
- 🤖 **AI Assistant & Dynamic Matchmaking:** RAG-powered intelligent conversational assistant with weighted multi-criteria tutor search.

---

## 2. System Architecture & Actors

TCS serves **4 primary actors** with complete tenant isolation and strict Role-Based Access Control (RBAC):

```
                       ┌─────────────────────────────────────────┐
                       │       Tutor Connect System (TCS)        │
                       │    Hybrid Marketplace + B2B SaaS CRM    │
                       └────────────────────┬────────────────────┘
                                            │
         ┌──────────────────┬───────────────┴───────────────┬──────────────────┐
         ▼                  ▼                               ▼                  ▼
┌─────────────────┐ ┌─────────────────┐           ┌─────────────────┐ ┌─────────────────┐
│     Client      │ │      Tutor      │           │  Tutor Center   │ │ Platform Admin  │
│ (Parent/Student)│ │ (Freelance/Org) │           │ (Educational B2B│ │ (Governance &   │
│                 │ │                 │           │  Institution)   │ │  Compliance)    │
└─────────────────┘ └─────────────────┘           └─────────────────┘ └─────────────────┘
```

---

## 3. Core Business Flows (BF-01 → BF-10)

The system is architected around **10 end-to-end Business Flows** covering **71 Use Cases** and **51 Functional Requirements**:

| Flow Code | Business Flow Name | Description | Key Modules Involved |
|:---:|---|---|---|
| **BF-01** | User Registration & Verification | Email OTP registration, Google OAuth, KYC credential & center license review. | Identity, Profile, Platform |
| **BF-02** | On-Demand Private Class Lifecycle | Client posts request $\rightarrow$ Tutors apply $\rightarrow$ Selection $\rightarrow$ E-Contract $\rightarrow$ Escrow $\rightarrow$ Completion. | Marketplace, Contract, Finance |
| **BF-03** | Tutor Recruitment for Center Workforce | Center posts hiring campaigns $\rightarrow$ Tutors apply $\rightarrow$ 48h cooperation contract signing $\rightarrow$ Roster. | Center, Contract, Profile |
| **BF-04** | Center Curriculum Class Lifecycle | Center creates structured curriculum classes $\rightarrow$ Bulk student import $\rightarrow$ Timetable slotting. | Center, Marketplace |
| **BF-05** | Center On-Request Class Lifecycle | Client requests custom class from Center $\rightarrow$ Center assigns internal tutor $\rightarrow$ Escrow lock. | Center, Finance, Marketplace |
| **BF-06** | Wallet & Smart Escrow Management | SePay automated top-up webhook, Escrow deposit lock, milestone release, withdrawal requests. | Finance, Smart Escrow |
| **BF-07** | Review & Reputation Management | Multi-criteria lesson reviews, tutor response threads, anti-manipulation rating calculation. | Contract, Review Engine |
| **BF-08** | Dispute, Issue & Refund Handling | Client/Tutor submits complaint $\rightarrow$ Admin mediation $\rightarrow$ Automated partial/full escrow refund. | Finance, Support, Platform |
| **BF-09** | User Support & AI Assistant | Ticket SLA tracking, FAQ knowledge base, RAG-powered LLM chatbot support. | AI Assistant, Messaging |
| **BF-10** | Platform Administration & Compliance | System audit logs, circumvention scanner, commission fee tuning, user penalty enforcement. | Platform Admin, Catalog |

---

## 4. Tech Stack & Version Compatibility

| Layer | Technology | Version | Description / Role |
|---|---|:---:|---|
| **Backend Framework** | Spring Boot | `4.0.6` | REST API, Security, Scheduling, WebSockets |
| **Runtime Language** | Java (JDK) | `21` | Modern LTS Java runtime (Virtual Threads ready) |
| **Database** | MySQL Server | `8.0 CE` | Primary relational store (InnoDB, UTF8MB4) |
| **Schema Migration** | Flyway | `4.0.6` | Automated versioned migrations (`V1` $\rightarrow$ `V35`) |
| **Frontend Framework** | React SPA | `19.x` | Modern reactive frontend |
| **Frontend Language** | TypeScript | `5.x / 6.x` | Strongly typed frontend development |
| **Build Tooling** | Vite | `8.x` | High-speed frontend bundling and HMR |
| **Authentication** | Spring Security + JJWT | `0.12.6` | Stateless JWT tokens + Google OAuth 2.0 |
| **Realtime Messaging** | Spring WebSocket (STOMP) | `BOM` | Realtime internal chat & push notifications |
| **Payment Gateway** | SePay Webhook Integration | `v1` | Automated QR banking deposit detection |
| **AI / LLM Integration** | Groq (LLaMA 3.3) / Gemini Flash | `API` | RAG query rewriting, semantic retrieval & routing |
| **Static Code Review** | SonarQube Community Edition | `9.9.8 LTS` | Continuous static analysis (SAST) & Quality Gates |

---

## 5. Code Quality & SonarQube Metrics

The entire codebase underwent comprehensive static analysis via **SonarQube 9.9 LTS** across **69,966 Lines of Code**:

| Metric | Result | Evaluation |
|---|:---:|---|
| **Vulnerabilities** | **0** | ⭐ **Grade A (1.0 - Zero Vulnerabilities)** |
| **Security Rating** | **Grade A** | ⭐ Fully hardened endpoint & JWT security |
| **Code Duplication Density** | **0.5%** | ⭐ Industry standard benchmark (< 3.0%) |
| **Maintainability Rating** | **Grade A (1.0)** | ⭐ High cohesion, decoupled service architecture |
| **Security Hotspots Audited** | **5 / 5** | ✅ 100% reviewed and mitigated |
| **Total Lines of Code (NCLOC)** | **69,966** | Large-scale fullstack production implementation |

---

## 6. Project Structure

```
Tutor-Connect-System-TCS-/
├── backend/                              # Spring Boot Java 21 Backend
│   ├── src/main/java/com/tcs/
│   │   ├── config/                       # Security, CORS, WebSocket, Flyway, AppConfig
│   │   ├── common/                       # Shared DTOs, domain events, utilities
│   │   ├── exception/                    # GlobalExceptionHandler, Custom business exceptions
│   │   ├── security/                     # JWT filter, UserPrincipal, AuthHelper
│   │   └── module/                       # Domain Modules (DDD-lite architecture)
│   │       ├── ai/                       # RAG Pipeline, Intent Classifier, LLM integration
│   │       ├── catalog/                  # Master data: Subjects, Grades, Locations, Fees
│   │       ├── center/                   # B2B Center CRM, Recruitment, Roster management
│   │       ├── contract/                 # E-Contract generation, OTP signatures, Reviews
│   │       ├── finance/                  # Smart Escrow, Wallet, SePay Webhook, Settlements
│   │       ├── identity/                 # Auth, Registration, OTP, Google OAuth
│   │       ├── marketplace/              # Classes, Applications, Matchmaking, Lessons
│   │       ├── messaging/                # Internal Chat, Realtime Notifications, Tickets
│   │       ├── platform/                 # Admin Dashboard, Audit Logs, Circumvention Detection
│   │       └── profile/                  # User Profiles, Credentials, KYC Documents
│   ├── src/main/resources/
│   │   ├── application.properties        # Application configuration
│   │   └── db/migration/                 # Flyway SQL migrations (V1__... to V35__...)
│   ├── src/test/java/com/tcs/            # Unit & Integration Tests (JUnit 5, Mockito, MockMvc)
│   └── pom.xml                           # Maven dependencies & build lifecycle
├── frontend/                             # React 19 + TypeScript + Vite SPA
│   ├── src/
│   │   ├── api/                          # Axios API clients & typed endpoints
│   │   ├── components/                   # Reusable UI components (Navbar, Modal, Tables, Forms)
│   │   ├── context/                      # AuthContext, NotificationContext, SocketContext
│   │   ├── hooks/                        # Custom React hooks
│   │   ├── pages/                        # Role-scoped pages (Admin, Client, Tutor, Center)
│   │   └── types/                        # TypeScript domain interfaces & DTOs
│   ├── package.json                      # Frontend dependencies
│   └── vite.config.ts                    # Vite build configuration & proxy rules
├── docs/                                 # Capstone Project Reports & Specifications
├── docker-compose.yml                    # Multi-container production deployment definition
├── sonar-project.properties              # SonarQube unified analysis configuration
└── README.md                             # Project documentation
```

---

## 7. Getting Started & Installation

### Prerequisites
- **JDK 21** (Eclipse Temurin or OpenJDK recommended)
- **Node.js 20+** and **npm**
- **MySQL 8.0+** running on port `3306`
- **Docker & Docker Compose** (Optional, for containerized run)

---

### 1. Clone & Configure Environment

```bash
git clone https://github.com/Kiennt1152/Tutor-Connect-System-TCS-.git
cd Tutor-Connect-System-TCS-
```

Create `backend/.env` file with your configuration:

```ini
# Database Configuration
DB_URL=jdbc:mysql://localhost:3306/tutorconnectsystem?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

# Security & JWT (Secret must be at least 32 characters)
JWT_SECRET=your_super_secret_jwt_key_with_at_least_32_characters_long
JWT_EXPIRATION_MS=86400000

# Mail Service (Gmail SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_google_app_password

# Payment (SePay Webhook Integration)
SEPAY_WEBHOOK_API_KEY=your_sepay_api_key

# AI Assistant (Groq / Gemini)
GROQ_API_KEY=your_groq_api_key
GEMINI_API_KEY=your_gemini_api_key
```

---

### 2. Database Setup & Flyway Migration

Create the database in MySQL:

```sql
CREATE DATABASE tutorconnectsystem CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> 💡 **Note:** Database migrations (`V1` through `V35`) apply automatically when Spring Boot starts up.

---

### 3. Run the Backend

```bash
cd backend
./mvnw.cmd spring-boot:run
# On Linux/macOS: ./mvnw spring-boot:run
```

- API Server runs at: **`http://localhost:8080`**
- Swagger API Explorer: **`http://localhost:8080/swagger-ui.html`**

---

### 4. Run the Frontend

```bash
cd frontend
npm install
npm run dev
```

- Frontend Web App runs at: **`http://localhost:3000`**

---

### 5. Run with Docker Compose

To spin up the entire stack (MySQL 8, Spring Boot Backend, Nginx + React Frontend) in one command:

```bash
docker compose up --build -d
```

---

## 8. API Documentation (Swagger / OpenAPI)

TCS provides interactive Swagger/OpenAPI documentation. Once the backend is running, visit:
- **Swagger UI Interactive Explorer**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI Specification JSON**: `http://localhost:8080/v3/api-docs`

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

---

## 9. Testing & Quality Assurance

The project adheres to a **Shift-Left "Review First – Test Later"** testing strategy across 4 levels:

```
[Document Review] ──► [SonarQube SAST & PR Review] ──► [L1 Unit] ──► [L2 Integration] ──► [L3 System] ──► [L4 UAT]
```

### Running Backend Unit & Integration Tests:
```bash
cd backend
./mvnw.cmd clean test
```

### Running SonarQube Quality Scan:
```bash
# Ensure SonarQube Docker container is running on port 9000
npx sonar-scanner
```

---

## 10. Development Team

| Full Name | Role | Responsibilities |
|---|:---:|---|
| **Nguyen Trung Kien** | Business Analyst / PM | System Architecture, Escrow FinTech, AI Engine, Deployment |
| **Hoang Khoi Nguyen** | Tech Lead / Developer | Product Requirements (PRD), Use Cases (UCS), Testing Plan |
| **Development Team Members** | Dev & QA | Feature Engineering, L1/L2 Test Suite, Quality Assurance |

---

*This project is developed as part of the Capstone Graduation Project at FPT University.*
