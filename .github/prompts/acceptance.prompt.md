```prompt
---
description: Validate implementation against original acceptance criteria — close the requirements loop
---

# Acceptance Validation: ${input:featureName}

## Purpose

This is the **Definition of Done gate**. It re-reads the original acceptance criteria
from the `requirements` phase and verifies each one against the implemented code.

This prompt closes the loop between what was promised and what was delivered.
If criteria are not met, this is a `fix-issues` input — not a reason to lower the bar.

---

## Step 1 — Retrieve the original requirements

Check these sources in order:

1. `.agent-handoff.md` (if the `requirements` agent wrote one recently)
2. Git log for a commit with `docs(requirements):` or `chore(requirements):` prefix
3. Any ADR file in the repository matching the feature name

```bash
git log --oneline --all | grep -i "${input:featureName}"
cat .agent-handoff.md 2>/dev/null
```

If no requirements document exists: **stop**. Write the handoff noting this gap and
recommend running `/requirements` retroactively to baseline what was built.

---

## Step 2 — Verify each acceptance criterion

For each Gherkin scenario from the requirements, verify it against the implementation.

### Approach: code inspection

Read the relevant controller, service, and component files. For each scenario:

**Happy path scenarios** — verify:
- The controller endpoint exists at the correct path with the correct HTTP method
- The service method delegates correctly
- The frontend component calls the correct service method
- The component template renders the correct data

**Error scenarios** — verify:
- The controller returns the specified HTTP status on failure
- `GlobalExceptionHandler` covers the exception type
- The frontend component handles the error state and displays a user-visible message

**Validation scenarios** — verify:
- Bean Validation annotations match the specified constraints
- The 400 response is returned (not 500) on validation failure
- Frontend form validation prevents submission for the same constraints

**Navigation scenarios** — verify:
- Routes are registered in `app-routing.module.ts`
- Navigation links exist in the correct component templates

**Accessibility scenarios** — verify:
- ARIA labels match what was specified
- Keyboard interaction flow works (check template for `tabindex`, `(keydown)` for custom interactions)

---

## Step 3 — Produce a verification matrix

```
Feature: ${input:featureName}

| # | Scenario | Status | Evidence |
|---|----------|--------|----------|
| 1 | [scenario name] | ✅ PASS / ❌ FAIL / ⚠️ PARTIAL | [file:line or "not found"] |
| 2 | ...             | ...                            | ... |
```

For each FAIL or PARTIAL:
- State exactly what is missing or wrong
- State the specific file and location where the fix belongs
- Assign severity: **blocking** (must fix before ship) / **minor** (can ship with note)

---

## Step 4 — Non-functional criteria check

| Criterion | Verified | Notes |
|-----------|----------|-------|
| All backend tests pass | ✓/✗ | `./gradlew check` result |
| All frontend tests pass | ✓/✗ | `npm test` result |
| CI workflows green | ✓/✗ | Check GitHub Actions status |
| No new `console.error` / stack traces in app start | ✓/✗ | |
| `npm run build:prod` succeeds | ✓/✗ | |
| OpenAPI spec updated (if new endpoints) | ✓/✗ | `/swagger-ui/index.html` |

---

## Step 5 — Verdict

**ACCEPTED** — All Must criteria pass; all Should criteria pass or have agreed deferrals.

**ACCEPTED WITH CONDITIONS** — All Must criteria pass; some Should criteria are deferred
with owner and timeline noted.

**REJECTED** — One or more Must criteria fail. List each one. Route to `fix-issues`
with the failing criteria as input.

---

## Step 6 — Update DoD tracking

If ACCEPTED or ACCEPTED WITH CONDITIONS, confirm:

- [ ] All acceptance criteria outcomes recorded here
- [ ] Any deferred items added as GitHub Issues or noted in handoff
- [ ] `copilot-instructions.md` reflects any new patterns established (route `/sync-instructions`)
- [ ] `release` is the recommended next step if this is the last feature in the sprint

---

## On Completion → Write Handoff

Write `.agent-handoff.md`:

```markdown
## Completed prompt: acceptance
## Feature: ${input:featureName}

## Verdict
[ACCEPTED / ACCEPTED WITH CONDITIONS / REJECTED]

## Criteria results
- Total scenarios: N
- Pass: N
- Fail: N
- Partial: N

## Blocking failures (if any)
[list — each needs a fix-issues run]

## Conditions / deferrals (if any)
[list with owner + target sprint if known]

## Recommended next prompt
[release — if ACCEPTED]
[fix-issues — if REJECTED, with failing criteria as input]
[sync-instructions — if new patterns were established during implementation]
```
```
