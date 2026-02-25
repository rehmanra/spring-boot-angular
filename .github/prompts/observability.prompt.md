```prompt
---
description: Structured logging completeness, health endpoints, metrics hooks, operational readiness
---

# Observability Audit & Setup

## Scope

${input:scope}

Valid values: `logging` | `health` | `metrics` | `all` (default: `all`)

---

## Why this matters for this project

This app currently has no observability. When it runs on a server you can't see:
- Why a request failed
- How long database queries take
- Whether a background process is healthy
- What error rate looks like over time

This prompt establishes the minimum viable observability baseline.

---

## Part 1 — Structured Logging Audit

### A — Coverage check

Every significant application event must be logged. Read all Java source files:

```bash
grep -rn "log\.\|logger\." backend/src/main/java --include="*.java" | grep -v "private static final Logger"
```

**Required log events per layer:**

| Layer | Event | Level |
|-------|-------|-------|
| Controller | Request received (method + path + sanitised params) | `DEBUG` |
| Controller | Validation error returned | `WARN` |
| Controller | Unexpected exception escaped to handler | `ERROR` |
| Service | Start of significant operations (creates, updates, deletes) | `DEBUG` |
| Service | Entity not found | `DEBUG` |
| GlobalExceptionHandler | Every exception caught | `WARN` or `ERROR` depending on status |

**Do NOT log:**
- Passwords, tokens, or any secret values
- Full request/response bodies unless behind a debug flag
- PII fields (email, phone) in production-level logs — only DEBUG

### B — Log format consistency

Every log call in production code must use SLF4J parameterised format:

```java
// CORRECT
log.debug("Getting user id={}", id);
log.warn("User not found id={}", id);
log.error("Unexpected error processing request", e);  // exception as last arg

// WRONG — flag all of these
log.debug("Getting user id=" + id);           // string concat
log.warn("User not found id: " + id);         // string concat
System.out.println("...");                     // not a logger at all
e.printStackTrace();                           // raw stack trace to stdout
```

```bash
# Find violations
grep -rn '\.debug(".*"\s*+\|\.warn(".*"\s*+\|\.error(".*"\s*+' \
  backend/src/main/java --include="*.java"
grep -rn "printStackTrace\|System\.out\|System\.err" \
  backend/src/main/java --include="*.java"
```

### C — Log level strategy

Read `backend/src/main/resources/application.yaml` for logging config.

Recommended baseline:
```yaml
logging:
  level:
    root: WARN
    com.springbootangular: DEBUG  # our code — verbose
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG      # enable to see queries during development
    org.hibernate.type.descriptor.sql: TRACE  # enable to see query parameters
```

Check: is there a separate logging config for tests? Tests should suppress most
Spring startup noise.

---

## Part 2 — Health Endpoints

### A — Check current actuator state

```bash
grep -rn "actuator\|health\|management" backend/build.gradle backend/src/main/resources/application.yaml
```

If Spring Boot Actuator is **not present**:

Add to `backend/build.gradle`:
```groovy
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

Add to `backend/src/main/resources/application.yaml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info
      base-path: /spring-boot-angular/actuator
  endpoint:
    health:
      show-details: when-authorized   # never "always" in production
  info:
    app:
      name: spring-boot-angular
      version: '@project.version@'
```

If Actuator **is present**: verify the above settings are correctly configured.

### B — Database health indicator

If Actuator is active, verify the built-in `DataSourceHealthIndicator` is working:

```bash
# With the app running:
curl -s http://localhost:8080/spring-boot-angular/actuator/health | python3 -m json.tool
```

Expected:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "PostgreSQL" } }
  }
}
```

### C — Security: do not expose sensitive actuator endpoints

Verify these are **not** in the `include` list unless intentionally secured:
- `env`, `beans`, `configprops`, `mappings` — expose configuration internals
- `heapdump`, `threaddump` — expose memory internals
- `shutdown` — can kill the process via HTTP

---

## Part 3 — Metrics

### A — Baseline metrics via Micrometer

Spring Boot Actuator includes Micrometer. If Actuator is now present:

```bash
# Check available metrics
curl -s http://localhost:8080/spring-boot-angular/actuator/metrics | python3 -m json.tool | head -40
```

Key metrics already available without custom code:
- `http.server.requests` — request count + latency per endpoint + status code
- `jvm.memory.used` — heap usage
- `system.cpu.usage` — CPU load
- `hikaricp.connections.*` — connection pool health

### B — Custom business metrics (if warranted)

For this project, consider:

```java
// In UserServiceImpl — inject MeterRegistry
private final MeterRegistry meterRegistry;

// In createUser():
meterRegistry.counter("user.created").increment();

// In deleteUser():
meterRegistry.counter("user.deleted").increment();
```

This is optional — document as a recommendation if not implementing now.

### C — Metrics export target

If this app will run beyond local development:
- Prometheus scrape endpoint: add `io.micrometer:micrometer-registry-prometheus` to confirm export format
- Document metric scrape URL: `GET /spring-boot-angular/actuator/prometheus`

---

## Part 4 — Frontend Error Observability

### A — Unhandled errors

Read `frontend/src/app/user.service.ts` — the `handleError` method:

```typescript
private handleError<T>(operation = 'operation', result?: T) {
  return (error: unknown): Observable<T> => {
    // Does this log to a real observability backend?
    // Currently it uses message.service — fine for dev, not for production
    ...
  };
}
```

For production readiness, HTTP errors should be observable externally.
Flag if there's no structured error logging (e.g., to a remote error tracker).

### B — Angular error handler

Check if a global `ErrorHandler` is registered in `app.module.ts`:

```typescript
// A production-grade Angular app should have:
{ provide: ErrorHandler, useClass: GlobalErrorHandler }
```

If not present: document as a recommendation. A custom `ErrorHandler` catches
all unhandled errors (including template errors) and can route them to structured
logging or an error reporting service.

---

## Output format

### Summary table

| Area | Status | Priority actions |
|------|--------|-----------------|
| Logging coverage | 🔴/🟡/✅ | [list] |
| Log format compliance | 🔴/🟡/✅ | [list violations] |
| Health endpoint | 🔴/🟡/✅ | [add actuator / configure] |
| Metrics | 🔴/🟡/✅ | [baseline available / custom needed] |
| Frontend error obs. | 🔴/🟡/✅ | [custom ErrorHandler needed] |

---

## On Completion → Write Handoff

Write `.agent-handoff.md`:

```markdown
## Completed prompt: observability
## Scope: ${input:scope}

## Changes made (if any)
[Actuator added Y/N, logging config updated Y/N, custom metrics added Y/N]

## Remaining recommendations
[items documented but not yet implemented]

## Health endpoint
[URL + expected response]

## Recommended next prompt
sync-instructions — update copilot-instructions.md with any new logging patterns or actuator config
```
```
