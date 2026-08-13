<<<<<<< Updated upstream
=======
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

## API Documentation

Once the backend is running, visit:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## License

This project is developed for academic purposes as part of a capstone project at FPT University.
>>>>>>> Stashed changes
