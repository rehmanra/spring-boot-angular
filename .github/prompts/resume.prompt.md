```prompt
---
description: Ingest human steering context mid-pipeline, mark stale phases, re-run affected work
---

# Resume

## What this prompt does

A human has redirected the pipeline. Your job is to:

1. Understand precisely what changed
2. Identify which sealed phase artifacts are invalidated by the new context
3. Mark those phases stale in pipeline state
4. Inject the human context as a standing constraint for all downstream phases
5. Produce a concrete re-run plan
6. Execute that plan, skipping phases whose sealed artifacts are unaffected

---

## Step 1 — Read everything

```bash
cat .pipeline-state.json | python3 -m json.tool
cat .pipeline-discussion.md
cat .agent-handoff.md
```

Extract:
- `human_interventions[]` — the verbatim human context that triggered this
- `phases[]` — which phases are currently sealed
- `decisions[]` — what has been locked in
- `checkpoint_gates` — which stage just redirected

---

## Step 2 — Analyse the intervention

Re-read the human context from `human_interventions[-1].context`.

Classify the change:

### Type A — Scope change (adding or removing features)
What was added or removed from the feature scope?

### Type B — Constraint change (new non-functional requirement)
A new constraint was imposed (deadline, security requirement, technology restriction, etc.)

### Type C — Priority or sequencing change
The order or urgency of work has changed (e.g., "ship backend first, UI later")

### Type D — Decision reversal
A prior decision must be undone (e.g., "actually don't use that approach")

### Type E — Clarification
The human is adding detail that doesn't invalidate prior work — just informs future phases

A single intervention can be multiple types. Classify all that apply.

---

## Step 3 — Propagation analysis

For each currently sealed phase, ask: **"Does the human intervention change the
correctness, completeness, or assumptions of this phase's artifact?"**

Work backwards from the most recent sealed phase:

| Sealed phase | Artifact summary | Invalidated by intervention? | Reason |
|---|---|---|---|
| [phase] | [one-sentence artifact] | YES / NO / PARTIAL | [why] |

Rules:
- If a phase is invalidated, every phase that came after it AND depends on it is also invalidated
- Mark all invalidated phases as `stale` — they must be re-run
- Phases independent of the change remain sealed — do not re-run them unnecessarily
- When in doubt: mark stale. Re-running a phase is cheaper than shipping on wrong assumptions.

---

## Step 4 — Update pipeline state

For each stale phase, update `.pipeline-state.json`:

```json
"phases": {
  "[stale-phase]": {
    "status": "stale",
    "sealed": false,
    "stale_reason": "[verbatim or paraphrased human context that invalidates this]",
    "prior_artifact": "[preserve the old artifact summary for reference]",
    "prior_challenges": "[preserve prior challenges for comparison after re-run]"
  }
}
```

Add the human intervention as a standing constraint:

```json
"standing_constraints": [
  {
    "introduced_at": "[stage]",
    "context": "[verbatim human input]",
    "affects_phases": ["[phase1]", "[phase2]"],
    "type": "[A|B|C|D|E]"
  }
]
```

Update `current_phase` to the earliest stale phase.
Update `status` to `"running"`.

---

## Step 5 — Produce the re-run plan

Present to the human before executing (this is a REQUIRED confirmation — not optional):

```
╔══════════════════════════════════════════════════════════════════╗
║  RESUME PLAN                                                     ║
╚══════════════════════════════════════════════════════════════════╝

HUMAN CONTEXT RECEIVED
──────────────────────
[verbatim input]

INTERPRETATION
──────────────
Type: [A/B/C/D/E — with one-line description]
Core change: [one sentence — what this means for the pipeline]

PHASES REMAINING SEALED (not affected)
────────────────────────────────────────
• [phase] — [one-sentence reason it's unaffected]

PHASES MARKED STALE (will re-run)
───────────────────────────────────
• [phase] — [one-sentence reason it's invalidated]
  ↳ Was: [old artifact one-liner]
  ↳ Will re-run with: [how the human context changes what this phase produces]

STANDING CONSTRAINT (injected into all downstream phases)
──────────────────────────────────────────────────────────
"[verbatim human constraint — this exact text will be prepended to each stale phase's context]"

RE-RUN ORDER
────────────
[1] [earliest stale phase] (proposer → challenge → seal)
[2] [next phase]
...

Parallel where possible: [list any phases that can run concurrently]

──────────────────────────────────────────────────────────────────
Confirm this interpretation is correct, or clarify further.
Type CONFIRMED to begin re-running stale phases.
──────────────────────────────────────────────────────────────────
```

---

## Step 6 — Execute re-run (after human confirms)

For each stale phase in re-run order:

1. **Prepend the standing constraint to the phase prompt's context.**
   Before the phase executes, write to `.agent-handoff.md`:
   ```markdown
   ## Standing constraint (from human intervention at [stage])
   [verbatim constraint text]
   This constraint overrides any prior decisions that contradict it.
   ```

2. **Re-run the phase prompt.** The phase reads the standing constraint first.

3. **Re-run the adversarial challenge.** Use `${input:round}=1` — fresh slate.
   The challenger must also read the standing constraint.
   The challenger should explicitly verify: "Does this revised artifact correctly incorporate the human constraint?"

4. **If challenge APPROVED/CONDITIONAL**: re-seal the phase. Append to `.pipeline-discussion.md`:
   ```markdown
   ## [phase] RE-RUN after human intervention at [stage]
   **Human context**: [verbatim]
   **Outcome**: SEALED — [challenge verdict]
   **How prior artifact changed**: [compare old vs new artifact summary]
   ```

5. **If challenge REJECTED after 2 rounds**: escalate to `/checkpoint` with:
   > "Re-run of [phase] failed after intervention. Human context may be contradictory
   > with [specific prior sealed phase]. Needs human resolution."

---

## Step 7 — Resume normal pipeline

Once all stale phases are re-sealed, update state:

```json
"status": "running",
"current_stage": "[next stage after last stale phase]",
"current_phase": "[next phase]"
```

Write `.agent-handoff.md`:
```markdown
## Resume complete
**Human intervention at**: [stage]
**Phases re-run**: [list]
**Phases preserved**: [list]
**Standing constraint active**: [yes — verbatim]
**Resuming from**: [phase]
**Pipeline action**: Continue normal execution from [phase].
```

Return control to `/pipeline` to continue from `current_phase`.
```
