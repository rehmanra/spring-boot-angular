# spring-boot-angular

A full-stack reference application: Spring Boot 4 REST API backed by PostgreSQL, consumed by an Angular 19 SPA.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 4.0.3 · Gradle 9.3.1 · Java 21 |
| Frontend | Angular 19.2 · TypeScript 5.8 · Node.js 22 |
| Database | PostgreSQL 17 (Docker) |
| API Docs | Springdoc OpenAPI / Swagger UI |
| CI | GitHub Actions (backend check, frontend lint/test/build, CodeQL SAST) |

---

## Prerequisites

- **Docker** with Docker Compose CLI plugin
- **Java 21+** (e.g. [Eclipse Temurin](https://adoptium.net/) or via [SDKMAN](https://sdkman.io/))
- **Node.js 22+** (e.g. via [nvm](https://github.com/nvm-sh/nvm) or [Homebrew](https://brew.sh/))

Angular CLI is installed locally as a dev dependency — `npx ng` or `npm run ng` works without a global install.

---

## Local Development Setup

### 1 — Configure database credentials

Copy the example env file and set your own passwords before starting Docker:

```bash
cp backend/docker/.env.example backend/docker/.env
# edit backend/docker/.env and replace the change-me values
```

The `.env` file is gitignored and never committed.

### 2 — Start PostgreSQL

```bash
docker compose -f backend/docker/docker-compose.yml up -d
```

This starts PostgreSQL 17 on `localhost:5432`, creates the `spring_boot_angular` database, and seeds the schema via `backend/docker/db/01-init.sh`.

### 3 — Start the Spring Boot backend

```bash
cd backend
./gradlew bootRun
```

The API is available at `http://localhost:8080/spring-boot-angular`. Set the following environment variables if your `.env` credentials differ from the defaults:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/spring_boot_angular
export DB_USERNAME=dbo_user
export DB_PASSWORD=<your-password>
```

Swagger UI: `http://localhost:8080/spring-boot-angular/swagger-ui/index.html`

### 4 — Start the Angular frontend

```bash
cd frontend
npm ci
npm start
```

Opens at `http://localhost:4200`. API calls are proxied to `http://localhost:8080` via the dev server proxy config (`src/proxy.conf.json`).

---

## Available Scripts (frontend)

| Script | Description |
|--------|-------------|
| `npm start` | Dev server with proxy to backend |
| `npm run lint` | ESLint + angular-eslint on all TypeScript and component templates |
| `npm test` | Unit tests (Karma + ChromeHeadless, single run) |
| `npm run test:watch` | Unit tests in watch mode |
| `npm run build:prod` | Production build to `dist/` |
| `npm run build:ci` | `clean` → `test` → `build:prod` (used by CI) |

---

## Running Backend Tests

Tests run against an in-memory H2 database — no Docker required:

```bash
cd backend
./gradlew check
```

Test reports are written to `backend/build/reports/tests/`.

---

## CI / Branch Protection

Four GitHub Actions workflows run on every push and pull request to `main`:

| Workflow | Triggers | What it checks |
|----------|----------|----------------|
| **Backend CI** | push / PR | `./gradlew check` (compile + unit tests) |
| **Frontend CI** | push / PR | lint → unit tests → production build |
| **CodeQL** | push / PR / weekly | SAST on Java and TypeScript/JavaScript |
| **Dependabot auto-merge** | Dependabot PRs | Auto-approves; auto-merges patch updates only after CI is green |

Required branch protection rules to configure in GitHub → Settings → Branches → `main`:

- ✅ `Backend CI / Compile, Test & Check`
- ✅ `Frontend CI / Lint, Test & Build`

---

## Project Structure

```
.
├── backend/                  Spring Boot application
│   ├── src/main/java/…       Controllers, services, DTOs, converters, model
│   ├── src/main/resources/   application.yaml
│   ├── src/test/resources/   application.yaml (H2 override for tests)
│   ├── docker/
│   │   ├── docker-compose.yml
│   │   ├── .env.example      Credential template (copy to .env)
│   │   └── db/01-init.sh     Schema init script
│   └── build.gradle
└── frontend/                 Angular SPA
    ├── src/app/              Components, services, routing
    ├── src/proxy.conf.json   Dev server proxy to backend
    ├── eslint.config.js      ESLint 9 flat config
    └── package.json
```
