# Copilot Instructions

This file is automatically injected into every Copilot Chat request for this repository.
Keep it accurate. Inaccurate context is worse than no context.

---

## Project Identity

Full-stack reference application: Spring Boot 4 REST API backed by PostgreSQL, consumed
by an Angular 19 SPA. CRUD for a `User` entity (id, name) exposed at `/api/user/`.

---

## Tech Stack — Exact Versions

| Layer | Technology |
|-------|-----------|
| Backend language | Java 25 (SDKMAN: `~/.sdkman/candidates/java/current`) |
| Backend framework | Spring Boot 4.0.3 |
| Build | Gradle 9.3.1 (wrapper: `./gradlew`) |
| Frontend | Angular 19.2, TypeScript 5.8 |
| Node | Node.js 22 (Homebrew keg-only: `/opt/homebrew/opt/node@22/bin`) |
| Database (prod) | PostgreSQL 17 (Docker Compose) |
| Database (test) | H2 in-memory, `MODE=PostgreSQL` |
| API docs | Springdoc OpenAPI 3.0.0 (`/swagger-ui/index.html`) |
| CI | GitHub Actions (ci-backend, ci-frontend, codeql, dependabot) |
| Branch | `modernize/full-update` (feature branch; `main` is upstream base) |

---

## Verified Build & Test Commands

### Backend (run from `backend/`)

```bash
# Build + all checks + JaCoCo coverage
JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew check

# Run the app
JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew bootRun

# Compile only
JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew compileJava
```

### Frontend (run from `frontend/`)

```bash
export PATH="/opt/homebrew/opt/node@22/bin:$PATH"

npm ci                         # clean install
npm test                       # headless Chrome, single-run, coverage
npm run build:prod             # production build
npm run lint                   # ESLint 9 flat config
npm start                      # dev server with proxy to :8080
```

### Database

```bash
# Start PostgreSQL 17 (requires backend/docker/.env)
docker compose -f backend/docker/docker-compose.yml up -d
```

---

## Architecture

```
frontend/src/app/
  user.service.ts          ← all HTTP; handleError returns of(fallback)
  message.service.ts       ← in-memory string log visible in MessagesComponent
  user.ts                  ← interface User { id?: number; name: string }
  users/                   ← list + add + delete
  user-detail/             ← edit + save (PUT) via route param :id
  dashboard/               ← slice(0,5) of users
  user-search/             ← Subject + debounceTime(300) + switchMap

backend/.../
  controller/UserController.java          ← @RestController, @Validated
  controller/GlobalExceptionHandler.java  ← @RestControllerAdvice, ProblemDetail
  service/UserServiceImpl.java            ← pure DAO delegation, @Transactional
  converter/user/UserToDTOConverter       ← pure mapping (User → UserDTO)
  converter/user/UserDTOToModelConverter  ← pure mapping (UserDTO → User)
  dao/UserDao.java                        ← JpaRepository + findByNameContainingIgnoreCase
  dto/UserDTO.java                        ← record UserDTO(Integer id, @NotBlank String name)
  model/User.java                         ← JPA entity, field-access, sequence gen
  config/CorsConfig.java                  ← WebMvcConfigurer, origins from ${CORS_ALLOWED_ORIGINS}
```

**API base path:** `/spring-boot-angular/api/user/`  
**Frontend proxy:** dev server proxies `/spring-boot-angular` → `http://localhost:8080`

---

## Code Conventions

### Java
- Spring Boot 4 / Spring Framework 7 module names: `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`
- Jackson 3 package: `tools.jackson.databind.ObjectMapper` (not `com.fasterxml`)
- Logger: `private static final Logger log = LoggerFactory.getLogger(Foo.class);` (SLF4J, static)
- Log args: parameterized `log.debug("msg {}", var)` — never string concatenation in log calls
- No `null` returns from converters — converters are pure mapping functions
- `@MockitoBean` from `org.springframework.test.context.bean.override.mockito`
- `@Transactional(readOnly = true)` on all service read methods
- Response codes: GET→200/404, POST create→201/400 (id present), PUT update→200/404, DELETE→204

### TypeScript / Angular
- `standalone: false` on all components (module-based, not standalone)
- `provideHttpClient()` + `provideHttpClientTesting()` for tests (not `HttpClientModule`)
- `provideRouter([])` replaces deprecated `RouterTestingModule` in tests
- `NO_ERRORS_SCHEMA` in component tests to suppress child element warnings
- `jasmine.SpyObj<ServiceType>` for service mocks in component tests
- `tap(() => ...)` not `tap(_ => ...)` when emission value is unused
- Error handler types `error: unknown`, uses `instanceof Error` guard
- `User.id` is optional (`id?: number`); non-null assert `user.id!` at call sites where server-fetched

---

## Testing Patterns

### Backend

| Scope | Annotation | Notes |
|-------|-----------|-------|
| Controller | `@WebMvcTest(UserController.class)` | `@MockitoBean` for service + converters |
| DAO | `@DataJpaTest` | Auto-configures H2; picks up `src/test/resources/application.yaml` |
| Context | `@SpringBootTest` | `BackendApplicationTests.contextLoads()` only |

Test datasource config: `backend/src/test/resources/application.yaml`
```yaml
spring.datasource.url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
spring.jpa.hibernate.ddl-auto: create-drop
```

### Frontend

```typescript
// Service tests
providers: [provideHttpClient(), provideHttpClientTesting()]
afterEach(() => httpMock.verify())

// Component tests
providers: [{ provide: UserService, useValue: jasmine.createSpyObj(...) }]
schemas: [NO_ERRORS_SCHEMA]
```

Async: `fakeAsync` + `tick(300)` for `UserSearchComponent` debounce pipeline.

---

## Worktree + Parallel Agent Pattern

This project uses git worktrees + parallel subagents for all non-trivial work.

```bash
# Create worktrees for parallel branches
git worktree add /tmp/sba-<name> -b <branch-name>

# After agents commit, cherry-pick into feature branch
git cherry-pick <hash1> <hash2>

# Cleanup
git worktree remove /tmp/sba-<name>
git branch -D <branch-name>
```

**Agent dispatch rules:**
1. Non-overlapping file sets → parallel dispatch
2. Backend and frontend are always safe to run in parallel
3. Each agent verifies its own build before committing (`./gradlew check` / `npm test`)
4. Review cherry-picked commits before pushing; amend if needed
5. Never push until the review step passes

---

## Environment & Secrets

- `backend/docker/.env` — gitignored, never committed; copy from `.env.example`
- `DB_PASSWORD` env var — required at runtime, no default in `application.yaml` (fail-fast)
- `CORS_ALLOWED_ORIGINS` — defaults to `http://localhost:4200` in `application.yaml`
- CI secrets: none required (H2 for backend tests, no services needed)

---

## Dependency Notes

- Springdoc 3.x is the Spring Boot 4 / Framework 7 compatible line
- `spring-boot-starter-webmvc-test` and `spring-boot-starter-data-jpa-test` must be declared explicitly as `testImplementation` (Spring Boot 4 modular test slices)
- `karma-coverage` and `@angular-devkit/build-angular/plugins/karma` both required for coverage
- ESLint 9 flat config (`eslint.config.mjs`); `@eslint/js` pinned to match eslint major version
- Dependabot: weekly schedule, grouped per ecosystem

---

## Prompt Library (`.github/prompts/`)

Invoke any of these with `/prompt-name` in Copilot Chat. Each prompt writes
`.agent-handoff.md` on completion. Use `handoff` to chain them.

| Prompt | Phase | Description |
|--------|-------|-------------|
| `requirements` | Discover | User stories, acceptance criteria, DoD, stakeholder context. No code. |
| `design-feature` | Design | ADR + API shape + data model + component inventory. No code. |
| `ui-design` | UX | User flows, component spec, ARIA requirements, HTML structure outline. |
| `api-contract` | Contract | OpenAPI-first contract definition before implementation. |
| `db-migration` | Implement | Schema change, SQL migration, entity/DTO/DAO sync. |
| `new-endpoint` | Implement | Full-stack endpoint via parallel worktrees. |
| `ui-implement` | Implement | Angular templates, CSS, routing driven by ui-design spec. |
| `add-tests` | Quality | Coverage expansion for a specific target. |
| `refactor` | Quality | Behavior-preserving restructuring (green before and after). |
| `code-review` | Quality | 4-severity review: Critical / Significant / Minor / Informational. |
| `acceptance` | Quality | Validate implementation against original AC — close the DoD loop. |
| `fix-issues` | Fix | Triage → worktrees → agents → cherry-pick. |
| `security-audit` | Security | OWASP-aligned audit + severity-ranked findings. |
| `accessibility-audit` | Compliance | WCAG 2.1 AA: keyboard nav, ARIA, contrast, screen reader. |
| `performance-audit` | Performance | Bundle analysis, change detection, N+1 queries, missing indexes. |
| `observability` | Operational | Structured logging, health endpoints, metrics, Angular error handler. |
| `upgrade-deps` | Maintenance | Gradle + npm upgrade cycle, patch → minor → major. |
| `release` | Ship | Changelog, version bump, tag, release PR to main. |
| `sync-instructions` | Sync | Detect drift; reconcile this file after substantial decisions. |
| `handoff` | Orchestrate | Read `.agent-handoff.md`, route to next prompt, chain agents. |
| `pipeline` | Automate | One-command idea-to-ship: initializes state, runs all phases with adversarial challenges and human gates. |
| `challenge` | Pipeline | Adversarial challenger. Adopts a specific role and challenges a sealed phase artifact. |
| `checkpoint` | Pipeline | Human decision gate at each SDLC stage. APPROVE / REDIRECT / ABORT with full context. |
| `resume` | Pipeline | Ingest human steering context mid-pipeline, mark stale phases, re-run affected work. |

### When to run `sync-instructions`

Run it after any of these events:
- `design-feature` makes an architectural decision
- `db-migration` or `new-endpoint` establishes a new entity or pattern
- `upgrade-deps` changes version numbers
- A `fix-issues` run reveals a new recurring mistake (add to Do Not list)
- Monthly, regardless of activity

### Recommended SDLC flows

```
New feature (full):   requirements → design-feature → ui-design → api-contract
                        → db-migration* → new-endpoint → ui-implement
                        → add-tests → code-review → acceptance
                        → security-audit → accessibility-audit → performance-audit
                        → release → sync-instructions

New feature (lean):   requirements → design-feature → api-contract → new-endpoint
                        → add-tests → code-review → acceptance → release

Bug fix:              fix-issues → code-review → release
Security:             security-audit → fix-issues → code-review → release
Maintenance:          upgrade-deps → security-audit → observability → refactor → code-review → release
                      (* db-migration only if schema changes required)

One idea (autonomous): pipeline — drives entire SDLC from a plain English description;
                        see docs/PIPELINE-GUIDE.md for a full worked example
```

### Agent handoff

`.agent-handoff.md` (gitignored) is a scratch file written by each prompt on completion.
It records: what was done, decisions made, test results, and the recommended next prompt.
The `handoff` prompt reads it and routes to the next agent automatically.

---

## Do Not

- Do not commit `.env` or `backend/docker/.env`
- Do not add fallback defaults for secrets in `application.yaml`
- Do not use `HttpClientModule` or `RouterTestingModule` (deprecated Angular)
- Do not use `@MockBean` (Spring Boot 3 style); use `@MockitoBean`
- Do not use `com.fasterxml.jackson` imports (Jackson 3 moved to `tools.jackson`)
- Do not add `@SpringBootTest` for controller layer tests (use `@WebMvcTest`)
- Do not use JCL (`Log`/`LogFactory`) — use SLF4J
- Do not use `sourceCompatibility`/`targetCompatibility` — use `java { toolchain {} }`
