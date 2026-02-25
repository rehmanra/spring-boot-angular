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
DISCOVER ────────────────────────────────────────────────
  requirements      → User stories, acceptance criteria, DoD, stakeholder context. No code.

DESIGN ──────────────────────────────────────────────────
  design-feature    → ADR, API shape, data model, component list. No code.

UX ──────────────────────────────────────────────────────
  ui-design         → User flows, component spec, ARIA requirements, HTML structure.

CONTRACT ────────────────────────────────────────────────
  api-contract      → OpenAPI-first contract before writing implementations.

IMPLEMENT ───────────────────────────────────────────────
  db-migration      → Schema change, SQL script, entity/DTO/DAO sync.
  new-endpoint      → Full-stack endpoint: backend + frontend in parallel worktrees.
  ui-implement      → Angular templates, CSS, routing driven by ui-design spec.

QUALITY ─────────────────────────────────────────────────
  add-tests         → Coverage expansion for a specific target.
  refactor          → Behavior-preserving restructuring.
  code-review       → 4-severity review: Critical / Significant / Minor / Informational.
  acceptance        → Validate implementation against original AC — close the DoD loop.

FIX ─────────────────────────────────────────────────────
  fix-issues        → Triage → worktrees → agents → cherry-pick workflow.

SECURITY & COMPLIANCE ───────────────────────────────────
  security-audit    → OWASP-aligned audit + severity-ranked findings.
  accessibility-audit → WCAG 2.1 AA: keyboard nav, ARIA, contrast, screen reader.

PERFORMANCE ─────────────────────────────────────────────
  performance-audit → Bundle analysis, change detection, N+1 queries, indexes.

OPERATIONAL ─────────────────────────────────────────────
  observability     → Structured logging, health endpoints, metrics, Angular error handler.

MAINTENANCE ─────────────────────────────────────────────
  upgrade-deps      → Gradle + npm upgrade cycle, patch → minor → major.

SHIP ────────────────────────────────────────────────────
  release           → Changelog, version bump, tag, release PR to main.

SYNC ────────────────────────────────────────────────────
  sync-instructions → Detect drift in copilot-instructions.md; reconcile after decisions.

ORCHESTRATE ─────────────────────────────────────────────
  handoff           → THIS FILE. Read handoff, route, execute, write new handoff.
```

---

## Recommended SDLC flows

### New feature (greenfield — full path)
```
requirements → design-feature → ui-design → api-contract
  → db-migration* → new-endpoint → ui-implement
  → add-tests → code-review → acceptance
  → security-audit → accessibility-audit → performance-audit
  → release → sync-instructions
  (* db-migration only if schema changes)
```

### New feature (pragmatic path — skip what's already green)
```
requirements → design-feature → api-contract → new-endpoint
  → add-tests → code-review → acceptance → release
```

### Bug fix
```
fix-issues → code-review → release
```

### Maintenance sprint
```
upgrade-deps → security-audit → observability → refactor → code-review → release
```

### Tech debt
```
refactor → add-tests → code-review → sync-instructions
```

### Security response
```
security-audit → fix-issues → code-review → release
```

### Post-milestone instructions sync
```
sync-instructions  (run after any 3+ commit milestone)
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
