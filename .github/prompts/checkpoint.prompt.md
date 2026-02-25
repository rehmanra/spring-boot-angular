```prompt
---
description: Human gate — present a stage decision brief and capture approve/redirect/abort
---

# Checkpoint: ${input:stage}

## What this prompt does

You are the **human interface layer** of the pipeline. You have arrived at a stage
boundary where autonomous execution stops until a human makes a deliberate decision.

This is NOT a status report. It is a **decision brief** — the minimum information
a human needs to make a confident choice. You present it concisely, surface the
live options, and record the response precisely so the pipeline can resume correctly.

**Stage**: `${input:stage}`

Valid stages: `DISCOVER` | `DESIGN` | `CONTRACT` | `IMPLEMENT` | `QUALITY` | `AUDIT`

---

## Step 1 — Build the decision brief

Read all sources:

```bash
cat .pipeline-state.json | python3 -m json.tool
cat .pipeline-discussion.md 2>/dev/null
cat .agent-handoff.md 2>/dev/null
```

From these, extract:

**a) What just happened** — summarise the sealed phases in this stage, one sentence each.

**b) What was challenged and the outcome** — for each challenge round in this stage:
- Which artifact was challenged
- Which role challenged it
- Outcome and any conditions attached

**c) Key decisions made** — list every decision that locks in future work.
These are the things that will be hard to change after the next stage starts.

**d) What was intentionally deferred** — if any requirements are not being addressed
until later, state that explicitly.

**e) What the next stage will do** — one paragraph describing what autonomous
work will happen after approval. Make it concrete enough that the human knows
what they're authorising.

---

## Step 2 — Present the checkpoint to the human

Format the output exactly like this — concise, scannable, no padding:

```
╔══════════════════════════════════════════════════════════════════╗
║  CHECKPOINT GATE — [STAGE]                                       ║
╚══════════════════════════════════════════════════════════════════╝

WHAT JUST COMPLETED
───────────────────
[phase]: [one sentence]
[phase]: [one sentence]
  └─ challenged by [role] → [APPROVED | CONDITIONAL]
     [condition if any — phrased as one line]

KEY DECISIONS LOCKED IN BY THIS STAGE
──────────────────────────────────────
• [decision]: [implication if changed later]
• [decision]: [implication if changed later]

INTENTIONALLY DEFERRED
──────────────────────
• [item] — [when/why deferred]
  (none if nothing deferred)

WHAT HAPPENS NEXT IF YOU APPROVE
─────────────────────────────────
[Concrete description of the next stage's autonomous work. Name the prompts
that will run and what they will produce. Mention any parallel execution.]

OPEN CONDITIONS FROM CHALLENGES
────────────────────────────────
[ ] [condition text] — owner: [pipeline | human | deferred]
[ ] [condition text]
  (none if no open conditions)

──────────────────────────────────────────────────────────────────
OPTIONS
  A) APPROVE       → Pipeline continues to [next stage]
  B) REDIRECT      → You provide steering context; affected phases re-run
  C) ABORT         → Pipeline halts; state is preserved for manual continuation
──────────────────────────────────────────────────────────────────
```

Wait for human response.

---

## Step 3 — Process the response

### If APPROVE

Update `.pipeline-state.json`:

```json
"checkpoint_gates": {
  "[stage]": {
    "status": "approved",
    "human_approved": true,
    "approved_at": "[ISO timestamp]",
    "notes": "[any human commentary captured verbatim]"
  }
}
```

Write `.agent-handoff.md`:
```markdown
## Checkpoint: [stage] — APPROVED
**Approved at**: [timestamp]
**Human notes**: [verbatim if any, else "none"]
**Pipeline action**: Continue to [next stage]. All prior conditions remain tracked.
```

### If REDIRECT

Capture the human's steering context verbatim. Do not interpret or summarise yet —
that is the `resume` prompt's job.

Update `.pipeline-state.json`:

```json
"status": "redirected",
"checkpoint_gates": {
  "[stage]": {
    "status": "redirected",
    "human_approved": false,
    "redirected_at": "[ISO timestamp]",
    "human_context": "[verbatim human input — character for character]"
  }
},
"human_interventions": [
  {
    "at_stage": "[stage]",
    "at_phase": "[current_phase]",
    "context": "[verbatim human input]",
    "timestamp": "[ISO timestamp]"
  }
]
```

Write `.agent-handoff.md`:
```markdown
## Checkpoint: [stage] — REDIRECTED
**Human context**: [verbatim]
**Pipeline action**: Invoke /resume with this context. Do not proceed to next stage.
```

Then immediately invoke `/resume` with the human context.

### If ABORT

Update `.pipeline-state.json`:

```json
"status": "aborted",
"checkpoint_gates": {
  "[stage]": {
    "status": "aborted",
    "human_approved": false,
    "aborted_at": "[ISO timestamp]",
    "reason": "[human reason if provided]"
  }
}
```

Write `.agent-handoff.md`:
```markdown
## Checkpoint: [stage] — ABORTED
**Reason**: [human reason or "none given"]
**Pipeline state**: preserved in .pipeline-state.json — resume with /pipeline when ready
**To resume**: run /pipeline — it will read existing state and ask where to continue from
```

Print to the user:
```
Pipeline halted at [stage]. State preserved in .pipeline-state.json.
To resume later: run /pipeline — it will read the existing run and resume from this gate.
To start fresh: delete .pipeline-state.json and run /pipeline with a new idea.
```
```
