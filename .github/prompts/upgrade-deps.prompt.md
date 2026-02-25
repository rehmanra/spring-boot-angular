```prompt
---
description: Upgrade backend (Gradle) and frontend (npm) dependencies with regression verification
---

# Upgrade Dependencies

## Scope

${input:scope}

Valid values: `backend` | `frontend` | `both` (default: `both`)

Proceed with **both** layers unless scope is explicitly restricted.

---

## Strategy

Upgrade in this order: **patch → minor → major**. Each tier is a separate commit.
Never batch patch + major into one commit — regressions must be attributable.

---

## Backend (Gradle)

### Step 1 — Inventory current versions

```bash
cd backend
JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew dependencies --configuration runtimeClasspath 2>&1 | grep -E "^\-\-\-|^\\+" | head -60
```

Read `backend/build.gradle` and list all explicitly declared dependencies with their current versions.

### Step 2 — Check available updates

```bash
# If the ben-manes versions plugin is configured:
JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew dependencyUpdates 2>&1 | grep -A2 "The following dependencies"

# Otherwise, check manually:
# Spring Boot: https://spring.io/projects/spring-boot (latest 4.x)
# Springdoc: https://github.com/springdoc/springdoc-openapi/releases
```

Spring Boot manages most transitive versions via its BOM. Upgrading the
`org.springframework.boot` plugin version is usually sufficient for the entire graph.

### Step 3 — Apply and verify patches

```bash
# Edit build.gradle — bump patch version only
JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew check 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL`. If not: revert and document the conflict.

### Step 4 — Apply and verify minor/major (if any)

Major upgrades (e.g., Spring Boot 4.x → 5.x) require:
1. Check Spring Boot migration guide
2. Check Springdoc compatibility matrix
3. Check Jackson 3 compatibility (already on `tools.jackson.*`)
4. Run full `./gradlew check` — address all compilation and test failures

### Step 5 — Commit per tier

```bash
git commit -m "chore(deps): bump Spring Boot to X.Y.Z"
git commit -m "chore(deps): bump springdoc-openapi to X.Y.Z"
```

---

## Frontend (npm)

### Step 1 — Inventory and audit

```bash
export PATH="/opt/homebrew/opt/node@22/bin:$PATH"
cd frontend
npm outdated 2>&1
npm audit 2>&1 | head -40
```

Note: separate `devDependencies` (test/build only) from `dependencies` (runtime bundle).

### Step 2 — Apply patch updates

```bash
npm update --save 2>&1  # patch updates only (within semver range)
npm test -- --watch=false --browsers=ChromeHeadless 2>&1 | tail -10
```

### Step 3 — Apply minor updates

Edit `package.json` manually for each minor version bump. Upgrade one package at a time
if there are known incompatibilities:

Priority order:
1. `@angular/core` + all `@angular/*` packages together (they must be in sync)
2. `typescript`
3. `karma` ecosystem (`karma`, `karma-chrome-launcher`, `karma-coverage`)
4. `eslint` + `@eslint/js` (must match major version)

```bash
npm install
npm test -- --watch=false --browsers=ChromeHeadless 2>&1 | tail -10
```

Angular upgrades: check `ng update` compatibility:
```bash
node node_modules/.bin/ng update 2>&1 | head -30
```

### Step 4 — Address npm audit findings

For each `npm audit` finding:
- `devDependencies` only: low urgency — note it and move on
- `dependencies` (runtime): fix immediately per the audit recommendation

```bash
npm audit fix 2>&1          # auto-fix where semver-compatible
npm audit fix --force 2>&1  # only if manual review confirms safety
```

### Step 5 — Commit per tier

```bash
git commit -m "chore(deps): bump Angular to XX.X.X"
git commit -m "chore(deps): update npm devDependencies (patch)"
```

---

## Verification checklist before pushing

- [ ] Backend `./gradlew check` → `BUILD SUCCESSFUL`
- [ ] Frontend `npm test` → `TOTAL: N SUCCESS, 0 FAILED` (N ≥ pre-upgrade count)
- [ ] `npm audit` clean or remaining findings are `devDependencies` only
- [ ] No `SNAPSHOT` or `RC` versions in final state (production artifacts only)
- [ ] Commit messages use `chore(deps):` prefix

---

## On Completion → Write Handoff

Write `.agent-handoff.md`:

```markdown
## Completed prompt: upgrade-deps
## Scope: ${input:scope}

## Upgrades applied
Backend:
- [old] → [new]: [package]

Frontend:
- [old] → [new]: [package]

## Audit status
- npm audit: [clean / N remaining (devDeps only)]
- Backend CVEs: [none identified / list]

## Test results
- Backend: BUILD SUCCESSFUL — N tests passing
- Frontend: TOTAL: N SUCCESS, 0 FAILED

## Pending major upgrades (not applied)
[list with reason why deferred]

## Recommended next prompt
security-audit — verify no new vulnerabilities introduced
```
```
