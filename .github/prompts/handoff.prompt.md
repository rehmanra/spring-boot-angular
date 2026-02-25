```prompt
---
description: Read the current handoff context and execute or route to the next agent
---

# Agent Handoff Orchestrator

This prompt chains agents across the SDLC. It reads `.agent-handoff.md` from the
repo root (written by the previous agent), determines the next action, and either
executes it or dispatches it to the appropriate prompt.

---

## Step 1 — Read the current handoff

```bash
cat .agent-handoff.md 2>/dev/null || echo "No handoff file found — starting fresh"
```

If no handoff file exists, ask: *"Which SDLC phase are we starting from?"*
and refer to the **Prompt Map** below.

---

## Step 2 — Parse the handoff

Extract:
- `Completed prompt`: what just ran
- `Recommended next prompt`: what it said to do next
- `Open items`: anything unresolved
- `Test results`: were builds passing when the last agent stopped?

If test results show failures: **route to `fix-issues` before anything else.**
A broken build blocks all downstream work.

---

## Step 3 — Confirm or override the routing

Present the recommended next step to the user:

```
Last completed: [prompt name]
Recommended next: [prompt name] — [reason]

Proceed? [yes / override to: ___]
```

If the user approves, invoke the recommended prompt with inputs pre-populated
from the handoff context. If the user overrides, route accordingly.

---

## Step 4 — Execute

Invoke the target prompt. Pass all relevant context from the handoff as pre-filled
inputs where the target prompt uses `${input:...}` variables.

---

## Step 5 — After execution completes

Verify `.agent-handoff.md` was written by the agent that just ran.
If not, write it now based on what was accomplished.

---

## Prompt Map (full SDLC reference)

```
DESIGN ──────────────────────────────────────────────────
  design-feature    → Architecture, ADR, API shape, component list. No code.

CONTRACT ────────────────────────────────────────────────
  api-contract      → OpenAPI-first contract before writing implementations.

IMPLEMENT ───────────────────────────────────────────────
  db-migration      → Schema change, SQL script, entity/DTO/DAO sync.
  new-endpoint      → Full-stack endpoint: backend + frontend in parallel worktrees.

QUALITY ─────────────────────────────────────────────────
  add-tests         → Coverage expansion for a specific target.
  refactor          → Behavior-preserving restructuring.
  code-review       → 4-severity review: Critical / Significant / Minor / Informational.

FIX ─────────────────────────────────────────────────────
  fix-issues        → Triage → worktrees → agents → cherry-pick workflow.

SECURITY ────────────────────────────────────────────────
  security-audit    → OWASP-aligned audit + severity-ranked findings.

MAINTENANCE ─────────────────────────────────────────────
  upgrade-deps      → Gradle + npm upgrade cycle, patch → minor → major.

SHIP ────────────────────────────────────────────────────
  release           → Changelog, version bump, tag, release PR to main.

ORCHESTRATE ─────────────────────────────────────────────
  handoff           → THIS FILE. Read handoff, route, execute, write new handoff.
```

---

## Recommended SDLC flows

### New feature (greenfield)
```
design-feature → api-contract → db-migration* → new-endpoint → add-tests → code-review → security-audit → release
                                 * only if schema changes
```

### Bug fix
```
fix-issues → code-review → release
```

### Maintenance sprint
```
upgrade-deps → security-audit → refactor → code-review → release
```

### Tech debt
```
refactor → add-tests → code-review
```

### Security response
```
security-audit → fix-issues → code-review → release
```

---

## Handoff file format reference

Every prompt writes `.agent-handoff.md` in this format.
Copy this template when writing a handoff manually:

```markdown
## Completed prompt: [prompt-name]
## Timestamp: [ISO 8601 — e.g. 2026-02-25T14:32:00Z]
## Branch: [branch name]
## Commits: [hash — message, one per line]

## Summary
[Bullet list: what was done, concise]

## Decisions
[Key choices made and the reasoning — future agents need this context]

## Files changed
[List: path — what changed]

## Test results
- Backend: BUILD SUCCESSFUL — N tests passing / FAILED — see details
- Frontend: TOTAL: N SUCCESS, 0 FAILED / see details

## Open items
[Anything left undone, blockers, questions the next agent should know]

## Recommended next prompt
[prompt-name] — [one-line reason]

## Context for next agent
[Anything not captured in commits or test output that the next agent needs]
```

---

## Handoff file location

`.agent-handoff.md` is in the repo root and is gitignored.
It is a **scratch file** — not a permanent record. For permanent records, use ADRs and commit messages.

```bash
# Confirm it's gitignored
grep agent-handoff .gitignore
```
```
