```prompt
---
description: OWASP-aligned security audit with severity-ranked findings and remediation
---

# Security Audit

## Scope

${input:scope}

If no scope is specified, audit the **entire stack**: all backend Java source files,
all frontend TypeScript source files, all configuration files, and all CI/CD workflows.

---

## Audit framework

### A — Secrets and configuration hygiene

Check these specific things in this project:

```bash
# Scan for potential secrets in tracked files
git log --all --full-history -- '*.yaml' '*.yml' '*.properties' '*.env' | head -20
grep -rn "password\|secret\|token\|key" backend/src/main/resources/ --include="*.yaml"
grep -rn "password\|secret\|token\|key" frontend/src/environments/ --include="*.ts"
```

| Item | Expected | Finding |
|------|----------|---------|
| `DB_PASSWORD` has no default in `application.yaml` | ✓ | |
| No credentials in `docker-compose.yml` beyond `.env` reference | ✓ | |
| `backend/docker/.env` is in `.gitignore` | ✓ | |
| No API keys or tokens in `environment.ts` / `environment.prod.ts` | ✓ | |
| CI `secrets:` used for any sensitive workflow values | ✓ | |

---

### B — Input validation (OWASP A03: Injection)

Backend — check every controller method:

```
backend/src/main/java/…/controller/UserController.java
```

| Item | Check |
|------|-------|
| All request bodies validated with `@Valid` or `@Validated` | ✓/✗ |
| `@NotBlank` / `@Size` / `@Pattern` on all string DTO fields | ✓/✗ |
| `GlobalExceptionHandler` returns 400 on `MethodArgumentNotValidException` | ✓/✗ |
| No `String.format` used to build JPQL/SQL (use named params or Spring Data) | ✓/✗ |
| Path variables validated (e.g., `@PathVariable @Positive Integer id`) | ✓/✗ |

Frontend — check that user-supplied values bound to the DOM are safe:

| Item | Check |
|------|-------|
| No `[innerHTML]` bindings with unescaped user data | ✓/✗ |
| No `bypassSecurityTrust*` calls without justification | ✓/✗ |
| Form inputs use reactive forms or template-driven with proper binding | ✓/✗ |

---

### C — Authentication and authorization (OWASP A01: Broken Access Control)

This project is currently unprotected (no auth layer). Flag this explicitly:

```
FINDING [HIGH]: No authentication or authorization mechanism exists.
All endpoints are publicly accessible.
Remediation path: Add Spring Security with JWT (future feature — track as ADR).
```

Check for any partial/accidental protection:
- No `@PreAuthorize` or `@Secured` without a working `SecurityFilterChain`
- No front-end route guards that imply auth without a real backend check

---

### D — CORS configuration (OWASP A05: Security Misconfiguration)

Read `backend/src/main/java/…/config/CorsConfig.java`.

| Item | Expected | Finding |
|------|----------|---------|
| `allowedOrigins` reads from `${CORS_ALLOWED_ORIGINS}` env var | ✓ | |
| Default value in `application.yaml` is only `http://localhost:4200` | ✓ | |
| No wildcard `*` in production origin config | ✓ | |
| `allowedMethods` is restricted (not `*`) | check | |
| `allowCredentials` is explicitly configured | check | |

---

### E — Dependency vulnerabilities (OWASP A06: Vulnerable Components)

#### Backend

```bash
cd backend
JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew dependencyInsight --dependency [suspectedLib] 2>&1 | head -30
# For full CVE scan, check if OWASP dependency-check plugin is configured
cat build.gradle | grep -i "owasp\|dependency-check"
```

Check Spring Boot BOM for known CVEs at: https://spring.io/security

#### Frontend

```bash
export PATH="/opt/homebrew/opt/node@22/bin:$PATH"
cd frontend
npm audit 2>&1
```

Classify each finding: critical / high / moderate / low.
For `npm audit` findings, note whether they are in `devDependencies` (test-time only) or `dependencies` (runtime).

---

### F — Error handling and information disclosure (OWASP A09)

| Item | Check |
|------|-------|
| `GlobalExceptionHandler` returns `ProblemDetail` — not raw stack traces | ✓/✗ |
| No `e.printStackTrace()` calls in production code | ✓/✗ |
| Log messages don't contain user-supplied data unescaped | ✓/✗ |
| Swagger UI is not exposed in a hypothetical production profile | check |

---

### G — CI/CD security (OWASP A08: Software Integrity)

Read `.github/workflows/*.yml`:

| Item | Check |
|------|-------|
| Third-party actions pinned to commit SHA, not floating tag | ✓/✗ |
| No `secrets.*` printed with `echo` | ✓/✗ |
| Dependabot enabled for `github-actions` ecosystem | ✓/✗ |
| CodeQL analysis runs on push and PR | ✓/✗ |

---

## Output format

Group all findings by severity:

### 🔴 Critical (exploit risk — remediate before any deployment)
[Finding N]: [title]
- Location: [file:line]
- Risk: [what an attacker can do]
- Remediation: [specific code change]

### 🟠 High (significant risk — remediate in current sprint)
...

### 🟡 Medium (should fix — track in backlog)
...

### 🔵 Low / Informational (hardening, best practice)
...

---

## On Completion → Write Handoff

Write `.agent-handoff.md`:

```markdown
## Completed prompt: security-audit
## Scope: ${input:scope}

## Findings summary
- Critical: N
- High: N
- Medium: N
- Low: N

## Top 3 findings
[title + one-line description each]

## Recommended next prompt
fix-issues — with the critical and high findings as input
```
```
