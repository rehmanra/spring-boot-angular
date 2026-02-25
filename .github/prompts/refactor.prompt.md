```prompt
---
description: Behavior-preserving code restructuring — no functional changes
---

# Refactor: ${input:target}

## Goal

${input:goal}

**Constraint**: Tests must pass before AND after. No functional changes are permitted.
If you discover something that should be fixed, note it in the handoff — do not fix it here.

---

## Step 1 — Establish the green baseline

Run the relevant test suite **before touching any code**. Record the result.

### Backend
```bash
cd backend
JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew check 2>&1 | tail -15
```

### Frontend
```bash
export PATH="/opt/homebrew/opt/node@22/bin:$PATH"
cd frontend && npm test -- --watch=false --browsers=ChromeHeadless 2>&1 | tail -10
```

Record: `[N] tests passing before refactor`. If tests are already failing, **stop** — 
write the handoff noting the pre-existing failures and recommend `fix-issues` first.

---

## Step 2 — Read the target and understand intent

Read `${input:target}` in full. Also read every file that imports from or depends on it.

Document:
- What the code currently does (public contract)
- What the code currently looks like (structure)
- What the refactoring goal requires changing
- What must NOT change (public API, observable behavior)

---

## Step 3 — Plan the refactoring

Choose the applicable technique(s):

| Technique | When to use |
|-----------|-------------|
| Extract method | Method longer than ~20 lines; multiple concerns |
| Extract class | Class has more than one responsibility |
| Inline method | Method adds no clarity, called in one place |
| Rename | Name doesn't reflect current purpose |
| Move | Wrong layer (e.g., business logic in controller) |
| Introduce parameter object | Method has >3 related params |
| Replace conditional with polymorphism | Repeated `if/switch` on type |
| Consolidate conditional | Redundant boolean guards |

State your plan explicitly before making any changes:
```
I will:
1. [specific change]
2. [specific change]
I will NOT:
- Change public method signatures (without updating all callers)
- Change HTTP status codes
- Change DTO field names (breaking API change)
```

---

## Step 4 — Apply changes incrementally

Make one logical change at a time. After each change:

```bash
# Backend: compile check (fast)
JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew compileJava 2>&1 | tail -10
```

Do not accumulate multiple unverified changes.

---

## Step 5 — Verify layering rules

After refactoring, confirm the architecture invariants still hold:

| Rule | Check |
|------|-------|
| Converters are pure mapping functions (no service calls) | ✓/✗ |
| Controllers delegate to services; no business logic in controllers | ✓/✗ |
| Services do not import from controllers | ✓/✗ |
| DAOs do not import from services or controllers | ✓/✗ |
| Frontend services handle all HTTP; components call only services | ✓/✗ |

---

## Step 6 — Run the full suite again

```bash
# Backend
JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew check 2>&1 | tail -15

# Frontend
npm test -- --watch=false --browsers=ChromeHeadless 2>&1 | tail -10
```

**Required outcome**: same number of passing tests as Step 1. Zero new failures.
If any test fails: revert that change immediately and investigate before proceeding.

---

## Step 7 — Commit

```bash
git add [changed files]
git commit -m "refactor([scope]): ${input:goal}"
```

Commit message must use `refactor` type prefix. The subject line should describe
what changed structurally, not what functionally changed (because nothing functional changed).

---

## On Completion → Write Handoff

Write `.agent-handoff.md`:

```markdown
## Completed prompt: refactor
## Target: ${input:target}
## Goal: ${input:goal}

## Changes made
[list of structural changes]

## Test results
- Before: N tests passing
- After: N tests passing (unchanged)
- Commit: [hash]

## Issues discovered (not fixed — for fix-issues)
[list or none]

## Recommended next prompt
code-review — verify refactoring quality and layering rules
```
```
