```prompt
---
description: Frontend bundle analysis, Angular change detection audit, backend query performance review
---

# Performance Audit

## Scope

${input:scope}

Valid values: `frontend` | `backend` | `both` (default: `both`)

---

## Frontend Performance

### A — Bundle analysis

```bash
export PATH="/opt/homebrew/opt/node@22/bin:$PATH"
cd frontend

# Production build with stats
npm run build:prod -- --stats-json 2>&1 | tail -20

# Inspect the stats file
ls dist/*/stats.json 2>/dev/null || ls dist/*/*/stats.json 2>/dev/null
```

If `webpack-bundle-analyzer` is available:
```bash
npx webpack-bundle-analyzer dist/*/stats.json
```

Otherwise, inspect the build output size manually:
```bash
ls -lah dist/spring-boot-angular/browser/*.js | sort -k5 -rh | head -20
```

**Thresholds (flag if exceeded):**
| Bundle | Warning | Error |
|--------|---------|-------|
| Initial JS (total) | 500 KB | 1 MB |
| Any single chunk | 250 KB | 500 KB |
| Initial CSS | 100 KB | 200 KB |

For each oversized bundle, identify the root cause:
- Large third-party library? → consider lazy loading or lighter alternative
- Duplicate code? → check for multiple versions of same package
- Feature code in main bundle? → move to lazy-loaded route chunk

---

### B — Lazy loading audit

Read `frontend/src/app/app-routing.module.ts`:

```typescript
// Check for lazy-loaded routes — should use loadChildren:
{ path: 'heavy-feature', loadChildren: () => import('./feature/feature.module').then(m => m.FeatureModule) }
```

This project currently uses eager loading (module-based, not standalone). For each route:
- Is this component used on first paint? (if yes: keep eager)
- Is this component only accessed after interaction? (if yes: candidate for lazy loading)

Document findings — do not change routing structure without an ADR.

---

### C — Change detection audit

Read every component that uses `*ngFor` or complex template expressions:

```bash
grep -rn "ngFor\|ngIf\|async pipe\|ChangeDetectionStrategy" frontend/src/app --include="*.ts" --include="*.html"
```

**Issues to flag:**

| Pattern | Risk | Recommendation |
|---------|------|----------------|
| `*ngFor="let x of getItems()"` — method call in template | High — runs on every CD cycle | Move to component property |
| Missing `trackBy` on `*ngFor` over large or frequently-changing arrays | Medium — full DOM re-render | Add `trackBy: trackById` |
| `ChangeDetectionStrategy.Default` on frequently-updating components | Medium | Consider `OnPush` |
| `async` pipe without `trackBy` on collections | Medium | Add `trackBy` |

---

### D — Observable subscription hygiene

Unmanaged subscriptions cause memory leaks:

```bash
grep -rn "\.subscribe(" frontend/src/app --include="*.ts" | grep -v "spec.ts"
```

For each `.subscribe()` call in a component (not a service):
- Is it in a component that is destroyed? → must be unsubscribed in `ngOnDestroy`
- Is it wrapped in `takeUntil(this.destroy$)`?
- Or using `async` pipe (preferred — auto-unsubscribes)?

Flag any component with `subscribe()` that does not have `ngOnDestroy` with cleanup.

---

## Backend Performance

### A — N+1 query detection

Read all repository methods:

```bash
grep -rn "@Query\|findBy\|findAll\|findById" backend/src/main/java --include="*.java"
```

For each service method that iterates over a collection and calls the DAO inside the loop:

```java
// BAD: N+1 — calls DB once per item
users.forEach(user -> {
    List<Orders> orders = orderDao.findByUserId(user.getId());
});

// GOOD: single query
List<Order> allOrders = orderDao.findByUserIdIn(userIds);
```

Flag any pattern that issues more than one DB call per item in a collection.

---

### B — Missing indexes

Read the schema (from `db/migration/` or `docker/db/01-init.sh`):

```bash
cat backend/src/main/resources/db/migration/*.sql 2>/dev/null
cat backend/docker/db/01-init.sh
```

For each column used in `WHERE`, `ORDER BY`, or `JOIN` clauses — verify an index exists.

Current DAO methods to check:
- `findByNameContainingIgnoreCase` → does `users.name` have an index?

---

### C — Transaction scope review

Read all `@Transactional` annotations:

```bash
grep -rn "@Transactional" backend/src/main/java --include="*.java"
```

**Issues to flag:**

| Pattern | Risk |
|---------|------|
| `@Transactional` on controller methods | Transaction spans HTTP layer — wrong boundary |
| Read methods without `@Transactional(readOnly = true)` | Misses DB-level read optimisation |
| Transactions that span external HTTP calls | Holds DB connection during network I/O |
| `@Transactional` on `private` methods | Spring AOP can't intercept — annotation is silently ignored |

---

### D — Connection pool sizing

Read `application.yaml`:

```bash
grep -A10 "datasource\|hikari" backend/src/main/resources/application.yaml
```

If `spring.datasource.hikari.maximum-pool-size` is not set, note that the default (10)
 may be inadequate under load. Recommend explicit configuration with justification.

---

## Output format

Group findings by layer and severity:

### Frontend
🔴 High impact: [finding — change detection method call in template, large bundle]
🟡 Medium impact: [finding — missing trackBy, unmanaged subscription]
🔵 Low impact: [finding — lazy loading opportunity]

### Backend
🔴 High impact: [finding — N+1 query in service method]
🟡 Medium impact: [finding — missing index, missing readOnly]
🔵 Low impact: [finding — pool size not configured]

---

## On Completion → Write Handoff

Write `.agent-handoff.md`:

```markdown
## Completed prompt: performance-audit
## Scope: ${input:scope}

## Critical findings
[list — each needs fix-issues]

## Medium findings
[list — prioritise for next sprint]

## Frontend bundle size
- Total initial JS: [N KB]
- Largest chunk: [name — N KB]

## Backend query issues
[list N+1 risks or missing indexes]

## Recommended next prompt
fix-issues — with the critical performance findings as input
```
```
