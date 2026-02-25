```prompt
---
description: Business analysis — user stories, acceptance criteria, definition of done, stakeholder context
---

# Requirements: ${input:featureName}

## Your role in this phase

You are the **business analyst**. Your output is the requirements document that the
architect, designer, and developers will work from. No code, no API shapes, no data
models — those are the architect's job. Your job is to capture:

1. What the user/stakeholder needs
2. How we'll know when we've delivered it
3. What is explicitly out of scope

This document is the source of truth for the `acceptance` prompt that runs near the
end of the SDLC to verify delivery against these criteria.

---

## Step 1 — Understand the business context

Answer these questions before writing any stories:

- **Who requested this feature?** (user role, stakeholder, compliance requirement)
- **What problem does it solve?** Not the solution — the underlying problem.
- **What is the business value?** (efficiency, revenue, compliance, UX improvement)
- **What happens if we don't build it?** (status quo cost)
- **Are there external constraints?** (regulatory, deadline, dependency on another team)
- **Who are the affected user personas?**

---

## Step 2 — Write user stories

Use the standard format: **As a [persona], I want [capability], so that [outcome].**

Write one story per distinct user action. Break large capabilities into multiple stories.
Each story must be small enough to be implemented and verified independently.

Example structure:
```
Story: ${input:featureName} — [capability]

As a [persona]
I want [specific capability]
So that [business outcome]

Background / context:
[Any context the developer needs — links, references, current behavior to change]
```

---

## Step 3 — Write acceptance criteria for each story

Use **Given / When / Then** (Gherkin-style). Be specific enough that a tester
who has never spoken to the stakeholder can verify each criterion independently.

```gherkin
Scenario: [scenario name]
  Given [precondition — system state + user state]
  When  [user action]
  Then  [observable outcome]
  And   [additional assertion if needed]

Scenario: [error/edge case]
  Given [precondition]
  When  [invalid or boundary action]
  Then  [error handling outcome]
```

Rules for good AC:
- Every AC is verifiable (binary pass/fail, not "should feel fast")
- Every AC covers one behaviour, not several
- Error paths get their own scenarios
- Performance requirements are quantified if present (e.g., "response < 500ms")
- Accessibility requirements are stated explicitly (e.g., "keyboard-navigable", "screen-reader label present")

---

## Step 4 — Define out of scope

Explicitly list what is NOT being built. This prevents scope creep and
misaligned expectations.

```
OUT OF SCOPE for this deliverable:
- [thing that might seem related but isn't included]
- [future enhancement explicitly deferred]
- [integration with system X — future sprint]
```

---

## Step 5 — Definition of Done

The universal DoD for this project. Every story must meet all of these before
it can be considered complete:

- [ ] All acceptance criteria pass (verified by a human/agent against `/acceptance`)
- [ ] All backend tests pass (`./gradlew check` → `BUILD SUCCESSFUL`)
- [ ] All frontend tests pass (`npm test` → `TOTAL: N SUCCESS, 0 FAILED`)
- [ ] Code review passed (`/code-review` → APPROVE or APPROVE WITH MINOR COMMENTS)
- [ ] Security audit passed for any new input surface (`/security-audit`)
- [ ] Accessibility: keyboard-navigable + ARIA labels on interactive elements (`/accessibility-audit`)
- [ ] No regression in existing functionality
- [ ] `copilot-instructions.md` updated if new patterns were established (`/sync-instructions`)
- [ ] Merged to `modernize/full-update` and CI green

---

## Step 6 — Priority and dependencies

Rate each story:

| Story | Priority | Depends on | Blocking |
|-------|----------|------------|---------|
| [story 1] | Must / Should / Could / Won't | [story or external dep] | [what it blocks] |

Use MoSCoW: **Must** (MVP blocker) / **Should** (strong need) / **Could** (nice-to-have) / **Won't** (deferred)

---

## On Completion → Write Handoff

Write `.agent-handoff.md`:

```markdown
## Completed prompt: requirements
## Feature: ${input:featureName}

## Stories
[one-line per story: As a [X] I want [Y]]

## Acceptance criteria count
[N scenarios across N stories]

## Out of scope
[key deferrals]

## Priority
[Must: N / Should: N / Could: N]

## Key constraints or decisions the architect must honour
[list — these flow into design-feature inputs]

## Recommended next prompt
design-feature — requirements are baselined; architect can now design the solution
```
```
