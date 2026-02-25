# Pipeline Guide

The adversarial pipeline is a single-prompt, idea-to-ship orchestrator that runs
every SDLF phase automatically, applies adversarial challenge reviews between phases,
and surfaces human decision gates at each stage boundary.

Start with a plain English idea. The pipeline does the rest.

---

## Quick start

```
/pipeline

> Idea: Add email verification to the user registration flow
```

That is the entire input. Everything from requirements analysis to release PR is
driven by the pipeline prompt.

---

## What "adversarial" means here

Every phase output is reviewed by a second agent playing an **opposing role** before
it is sealed. The reviewer is not trying to approve — they are trying to find flaws
from a specific professional perspective.

| Phase | Challenger role(s) |
|-------|--------------------|
| requirements | architect (feasibility), security (risk surface) |
| design-feature | ba (requirements coverage), security, performance |
| ui-design | accessibility, ba (user flows match AC?) |
| api-contract | security, ba (each AC maps to an endpoint?) |
| db-migration | security, architect (consistent with ADR?) |
| new-endpoint | code-review (4-severity) |
| ui-implement | accessibility, code-review |
| add-tests | architect (risk coverage), ba (AC scenario coverage) |
| acceptance | ba (challenges each original criterion independently) |
| security-audit | performance (security vs perf tradeoffs), architect |
| accessibility-audit | ba (which AC do failures block?) |
| performance-audit | architect (requires ADR revision?) |

If a challenger **REJECTS**, the proposer revises and a round 2 challenge runs.
If round 2 also **REJECTS**, the pipeline escalates to a human checkpoint automatically.
The pipeline never ignores a rejection — it never reinforces a bad signal.

---

## State files

| File | Purpose |
|------|---------|
| `.pipeline-state.json` | Machine-readable run state — phases, verdicts, decisions, human interventions |
| `.pipeline-discussion.md` | Running log of all challenge reports and verdicts |
| `.agent-handoff.md` | Scratch file — latest phase output; overwritten each phase |

All three are gitignored. They exist only for the duration of the pipeline run.

---

## Full worked example

### The idea

> "Add email verification to the user registration flow."

The pipeline initialises its state and begins DISCOVER.

---

### DISCOVER stage

**Phase: `requirements`**

The requirements agent produces:

> **US-1**: As a new user, I want to verify my email address before I can log in,
> so that only real email addresses are accepted.
>
> **US-2**: As a returning user who hasn't verified, I want to be able to resend
> the verification email, so that I can recover from a missed email.
>
> **Acceptance criteria for US-1:**
> - AC1.1: Unverified users are blocked from authenticated endpoints
> - AC1.2: A verification token is emailed on registration
> - AC1.3: Clicking the link marks the account as verified
> - AC1.4: Tokens expire after 72 hours

---

**Challenge: `requirements` ← `architect` (round 1)**

```
## Challenge Report
Artifact: requirements
Challenger role: architect
Round: 1

[F1] — CRITICAL
Claim: No email sending infrastructure exists in this application.
Evidence: application.yaml has no SMTP, SES, or SendGrid config.
  The entire user registration flow is in-process; there is no
  outbound messaging layer.
Risk: requirements assume email delivery capability that requires
  non-trivial infrastructure work (SMTP server, credentials,
  transactional email provider). If this is not scoped, implementation
  will stall at the email-sending step.
Remediation: Requirements must either (a) scope the email delivery
  infrastructure, or (b) decouple "store and flag token" from
  "send email" — ship the storage/flag logic first, then email delivery
  as a second slice once infrastructure is provisioned.

[F2] — SIGNIFICANT
Claim: AC1.4 specifies a 72-hour expiry, but no rationale is given.
Evidence: "Tokens expire after 72 hours" — no industry standard cited,
  no threat model context.
Risk: 72 hours is longer than common practice (24h). Prolonged token
  validity increases the window for account takeover via email interception.
Remediation: Default to 24 hours unless a business reason for 72h is stated.
  Either reduce to 24h or add explicit AC: "Expiry window is configurable."

Verdict: REJECTED

Blocking findings: F1, F2
Round 2 must address:
- Explicit infrastructure scope or explicit deferral of email delivery
- Token expiry reduced to 24h or explicitly justified
```

The requirements agent revises. Round 2:

---

**Challenge: `requirements` ← `architect` (round 2)**

The revised requirements now read:

> **US-1 revised:** As a new user, I want my email address to be captured and
> a verification token generated on registration. The UI displays the token link
> inline (no email delivery in this sprint — email sending is deferred until
> SendGrid is configured, tracked as separate infrastructure task).
>
> **AC1.4 revised:** Tokens expire after 24 hours (configurable via
> `app.verification.token-expiry-hours`).

```
## Challenge Report
Artifact: requirements (revised)
Challenger role: architect
Round: 2

[F1] — MINOR
Claim: The "display token link inline" approach is a test/dev convenience,
  not a production-ready pattern.
Evidence: US-1 revised — link displayed in UI response.
Risk: If this ships without a clear marker that it's temporary, it will
  stay in production. Users will be confused by a verification link that
  appears in the UI instead of their inbox.
Remediation: Add AC: "Display of inline link is only active when
  app.verification.token-expiry-hours is present AND
  VERIFICATION_EMAIL_ENABLED=false."
Note: This is advisory, not a blocker.

Verdict: CONDITIONAL

Conditions:
- [ ] C1: Inline link display is gated on VERIFICATION_EMAIL_ENABLED=false
- [ ] C2: Token expiry is configurable and defaults to 24h
```

Architect CONDITIONAL — phase sealed with two conditions.

---

**Challenge: `requirements` ← `security` (round 1)**

```
## Challenge Report
Artifact: requirements (revised)
Challenger role: security
Round: 1

[F1] — SIGNIFICANT
Claim: No rate limiting specified for token resend (US-2).
Evidence: US-2 — "resend the verification email" — no frequency constraint.
Risk: Without rate limiting, an attacker can trigger unlimited email sends
  to any address (email bombing, SendGrid bill exhaustion when email is enabled).
Remediation: Add AC2.1: "Resend is rate-limited to 3 requests per hour per account."

[F2] — MINOR
Claim: No explicit statement that verification tokens are single-use.
Evidence: AC1.3 — "Clicking the link marks the account as verified." No statement
  that the token is then invalidated.
Risk: Replay attack: if token is kept active after use, someone with access
  to old browser history can re-verify a deregistered account.
Remediation: Add AC1.5: "Verification tokens are invalidated immediately on use."

Verdict: CONDITIONAL

Conditions:
- [ ] C1: AC2.1 added — resend rate-limited to 3/hour per account
- [ ] C2: AC1.5 added — tokens invalidated on first use
```

Security CONDITIONAL — phase sealed with conditions.

---

**📍 CHECKPOINT GATE — DISCOVER**

```
╔══════════════════════════════════════════════════════════════════╗
║  CHECKPOINT GATE — DISCOVER                                      ║
╚══════════════════════════════════════════════════════════════════╝

WHAT JUST COMPLETED
───────────────────
requirements: 2 user stories, 7 acceptance criteria
  └─ challenged by architect → CONDITIONAL
     C1: Inline link gated on VERIFICATION_EMAIL_ENABLED=false
     C2: Token expiry configurable, default 24h
  └─ challenged by security → CONDITIONAL
     C1: Resend rate-limited to 3/hour
     C2: Tokens single-use (invalidated on use)

KEY DECISIONS LOCKED IN BY THIS STAGE
──────────────────────────────────────
• Email delivery is OUT OF SCOPE for this sprint — deferred to infrastructure task
• Inline token link is a dev-mode convenience, not a production flow
• Token lifetime default: 24h, configurable
• Resend endpoint: rate-limited to 3/hour

INTENTIONALLY DEFERRED
──────────────────────
• Actual email sending — requires SendGrid configuration (separate task)
• Email delivery failure handling

WHAT HAPPENS NEXT IF YOU APPROVE
─────────────────────────────────
DESIGN stage runs: design-feature produces the ADR, data model, and API shape.
ui-design produces user flows and component spec. Each is challenged by the
appropriate adversarial roles. No code is written in this stage.

OPEN CONDITIONS FROM CHALLENGES
────────────────────────────────
[ ] AC1.5: tokens invalidated on use — owner: implementation
[ ] AC2.1: resend rate-limited 3/hour — owner: implementation
[ ] Inline link gated on env var — owner: implementation
[ ] Token expiry configurable — owner: implementation

──────────────────────────────────────────────────────────────────
OPTIONS
  A) APPROVE       → Pipeline continues to DESIGN
  B) REDIRECT      → You provide steering context; affected phases re-run
  C) ABORT         → Pipeline halts; state is preserved for manual continuation
──────────────────────────────────────────────────────────────────
```

**Human types:**

> APPROVE — and use SendGrid specifically, not SMTP. Make that a standing constraint.

Pipeline records: `human_approved = true`, adds standing constraint:
> "Email delivery, when implemented, must use SendGrid SDK. Do not design for SMTP."

---

### DESIGN stage

`design-feature` produces an ADR covering the new `email_verifications` table,
`VerificationToken` entity, updated `User.verified` boolean, and a
`VerificationService`. BA and security challengers approve with minor conditions.

`ui-design` produces flows for the inline-link screen and the "already verified"
state. Accessibility challenger flags that the success/error states need
`aria-live="polite"` — CONDITIONAL.

---

**📍 CHECKPOINT GATE — DESIGN**

The human approves without redirect.

---

### CONTRACT stage

`api-contract` produces OpenAPI specification:

```yaml
POST /spring-boot-angular/api/verification/initiate
  request: { userId: integer }
  response 201: { token: string, expiresAt: datetime, verifyUrl: string }
  response 400: ProblemDetail (user already verified, user not found)

POST /spring-boot-angular/api/verification/confirm
  request: { token: string }
  response 200: { verified: true }
  response 410: ProblemDetail (token expired)
  response 404: ProblemDetail (token not found — do NOT distinguish from expired)

POST /spring-boot-angular/api/verification/resend
  request: { userId: integer }
  response 202: { message: "resend queued" }
  response 429: ProblemDetail (rate limit exceeded)
```

---

**Challenge: `api-contract` ← `security` (round 1)**

```
[F1] — SIGNIFICANT
Claim: The /confirm endpoint returns 404 for token-not-found AND 410 for expired.
  The spec says "do NOT distinguish" — but it then defines two response codes that
  do distinguish. This is contradictory.
Evidence: api-contract — confirm endpoint response map.
Risk: Attacker can enumerate whether a token exists by probing for 404 vs 410.
  User enumeration via token validity.
Remediation: Collapse to a single 400 or 404 for all invalid/expired/used token
  states. No distinguishing status code.

Verdict: CONDITIONAL
C1: /confirm returns one status code for all failed token states (suggested: 400)
```

Security CONDITIONAL — contract sealed after revision.

**Challenge: `api-contract` ← `ba` (round 1)**

```
[F1] — MINOR
Claim: AC2.1 (resend rate limiting) has no corresponding error contract in the spec.
Evidence: /resend shows 202 and 429 but ProblemDetail schema for 429 is not defined.
Remediation: Add 429 ProblemDetail schema with retryAfterSeconds field.
Verdict: CONDITIONAL
C1: 429 response includes retryAfterSeconds in ProblemDetail detail map
```

---

**📍 CHECKPOINT GATE — CONTRACT**

```
╔══════════════════════════════════════════════════════════════════╗
║  CHECKPOINT GATE — CONTRACT                                      ║
╚══════════════════════════════════════════════════════════════════╝

WHAT JUST COMPLETED
───────────────────
api-contract: 3 endpoints defined, OpenAPI spec produced
  └─ challenged by security → CONDITIONAL
     C1: /confirm uses single status code for all failure states
  └─ challenged by ba → CONDITIONAL
     C1: 429 response includes retryAfterSeconds

KEY DECISIONS LOCKED IN BY THIS STAGE
──────────────────────────────────────
• Contract is FROZEN after this gate — endpoint shapes cannot change in implementation
• Three endpoints: /initiate, /confirm, /resend
• /confirm failure is opaque — single 400 for all invalid token states
• Standing constraint: email delivery via SendGrid SDK only

WHAT HAPPENS NEXT IF YOU APPROVE
─────────────────────────────────
IMPLEMENT stage: db-migration (schema + entity), new-endpoint (service + controller),
ui-implement (Angular components). Backend phases can parallel with ui if worktrees
are available. Each faced with adversarial code-review challenge before sealing.

──────────────────────────────────────────────────────────────────
OPTIONS
  A) APPROVE       → Pipeline continues to IMPLEMENT
  B) REDIRECT      → You provide steering context; affected phases re-run
  C) ABORT         → Pipeline halts
──────────────────────────────────────────────────────────────────
```

**Human types:**

> REDIRECT: Actually, I want this to be async. The verification email (when we add it)
> should go through a queue, not direct SendGrid call in the request thread.
> Redesign the contract for async and make sure the implementation accounts for it.

---

### REDIRECT handling — `/resume`

The resume prompt reads the intervention:

```
╔══════════════════════════════════════════════════════════════════╗
║  RESUME PLAN                                                     ║
╚══════════════════════════════════════════════════════════════════╝

HUMAN CONTEXT RECEIVED
──────────────────────
"Actually, I want this to be async. The verification email (when we add it)
should go through a queue, not direct SendGrid call in the request thread.
Redesign the contract for async and make sure the implementation accounts for it."

INTERPRETATION
──────────────
Type: B (constraint change) + D (decision reversal on sync delivery)
Core change: Email sending must be decoupled from the request path. The
initiate/resend endpoints must return immediately; sending (when enabled)
happens asynchronously via a queue.

PHASES REMAINING SEALED (not affected)
────────────────────────────────────────
• requirements — user stories focus on registration flow, not delivery mechanism
• design-feature — entity model and VerificationService unaffected by async;
  only the sending integration layer changes

PHASES MARKED STALE (will re-run)
───────────────────────────────────
• api-contract — /initiate and /resend shapes must reflect async semantics
  Was: synchronous 201 with verifyUrl in response body
  Will re-run with: endpoints return 202 Accepted immediately; no verifyUrl in
  body (link is delivered via queue worker when email is enabled; in dev mode,
  link still returned inline if VERIFICATION_EMAIL_ENABLED=false)

STANDING CONSTRAINT (injected into all downstream phases)
──────────────────────────────────────────────────────────
"Email sending is asynchronous. Sending events are placed on a queue (Spring
ApplicationEvent or a future message broker). Request threads must return 202
immediately. The verifyUrl is only returned inline when VERIFICATION_EMAIL_ENABLED=false."

RE-RUN ORDER
────────────
[1] api-contract (async semantics; security + ba challenge)
[2] new-endpoint (implementation must publish events not call SendGrid directly)
[3] ui-implement (no verifyUrl guaranteed in response — UI must handle missing field)

──────────────────────────────────────────────────────────────────
Confirm this interpretation is correct, or clarify further.
Type CONFIRMED to begin re-running stale phases.
──────────────────────────────────────────────────────────────────
```

**Human types:** `CONFIRMED`

The pipeline re-runs `api-contract` with the async constraint injected,
challenges it again (security + ba), seals the revised contract, then
continues through IMPLEMENT with the standing constraint active.

---

### IMPLEMENT stage (parallel worktrees)

Two worktrees run in parallel:

**Worktree 1 — backend** (`/tmp/sba-backend-verification`):
- `db-migration` — adds `email_verifications` table, `User.verified` column
- `new-endpoint` — implements `VerificationController`, `VerificationService`,
  `VerificationTokenDao`, `ApplicationEvent` publisher
- Both challenged: db-migration ← security + architect; new-endpoint ← code-review

**Worktree 2 — frontend** (`/tmp/sba-frontend-verification`):
- `ui-implement` — `VerificationBannerComponent`, inline link handling,
  `VERIFICATION_EMAIL_ENABLED` guard
- Challenged: ui-implement ← accessibility + code-review

After both worktrees complete their challenge cycles and seal, the pipeline
cherry-picks both branches into `modernize/full-update`.

---

### QUALITY stage

`add-tests` produces: `VerificationControllerTest`, `VerificationTokenDaoTest`,
`VerificationServiceTest`, `VerificationBannerComponent.spec.ts`.
BA challenger verifies each AC has a test. Architect challenger verifies
the async path has a failure-mode test (queue full, event publish fails).

`acceptance` re-reads the original requirements (not the revised ones) and
challenges each original AC independently. Result: AC1.3 (token link marks
account verified) passes. AC1.2 (link is emailed) is marked as DEFERRED with
explicit tracker entry — not failed, not silently dropped.

---

### AUDIT stage (parallel)

Security, accessibility, and performance audits run in parallel:

- `security-audit` — flags token storage hashing (should be stored as a hash,
  not plaintext). SIGNIFICANT finding → `fix-issues` runs before sealing.
- `accessibility-audit` — confirms aria-live on verification banner. APPROVED.
- `performance-audit` — confirms no N+1 in token lookup. APPROVED.

All three audits have cross-challenges before sealing.

---

### SHIP

`release` produces a changelog entry, bumps the patch version, creates a PR
to `main` with a summary of all ADR decisions, open conditions, and the
deferred email delivery tracker entry.

`sync-instructions` updates `copilot-instructions.md` with the new entity
(`VerificationToken`), new service layer (`VerificationService`), and the
standing pattern for async event publishing.

---

## Discussion log excerpt

Here is what `.pipeline-discussion.md` looks like mid-run:

```markdown
---
## Challenge: requirements — Round 1
**Role**: architect
**Outcome**: REJECTED

[F1] — CRITICAL
Claim: No email sending infrastructure exists in this application.
...
Verdict: REJECTED

---
## Challenge: requirements (revised) — Round 2
**Role**: architect
**Outcome**: CONDITIONAL

[F1] — MINOR
Claim: "Display of inline link" pattern not gated on env var.
...
Verdict: CONDITIONAL
C1: Inline link gated on VERIFICATION_EMAIL_ENABLED=false
C2: Token expiry configurable, default 24h

---
## Challenge: requirements — Round 1
**Role**: security
**Outcome**: CONDITIONAL

[F1] — SIGNIFICANT
Claim: No rate limiting on resend endpoint.
...

---
## Checkpoint: DISCOVER — APPROVED
Human notes: "use SendGrid specifically, not SMTP"
Standing constraint added.

---
## Challenge: api-contract — Round 1
**Role**: security
**Outcome**: CONDITIONAL
...

---
## Checkpoint: CONTRACT — REDIRECTED
Human context: "Actually, I want this to be async..."

---
## api-contract RE-RUN after human intervention at CONTRACT
Human context: async queue constraint
Outcome: SEALED — APPROVED
How prior artifact changed: /initiate and /resend return 202 immediately;
  verifyUrl omitted from production response body
```

---

## Prompt reference

| Prompt | Role in pipeline |
|--------|-----------------|
| `/pipeline` | Master orchestrator — run this with your idea |
| `/challenge` | Run by pipeline automatically — never invoked directly |
| `/checkpoint` | Run by pipeline at each stage gate — you respond here |
| `/resume` | Run by pipeline after a REDIRECT — you confirm the plan |
| `/handoff` | Step-by-step chaining without full pipeline — manual SDLC |

---

## Guardrails

**What the pipeline will NOT do:**

- Silently accept a challenge rejection and continue (always blocks or escalates)
- Re-run a challenge with the same unchanged artifact (round 2 must show specific revisions)
- Approve a phase after two rejections (mandatory human checkpoint)
- Seal the CONTRACT stage and then allow endpoint shape changes in IMPLEMENT
- Drop an acceptance criterion without an explicit tracked deferral entry
- Commit code without passing `./gradlew check` and `npm test`

**Signal reinforcement prevention:**

The adversarial challenger deliberately adopts a role that is NOT the proposer's
own perspective. An architect proposing a design is challenged by the BA (user value)
and security (risk surface) — never by another architect. This prevents the pipeline
from confirming its own biases.

Round 2 challengers are instructed to hold revised artifacts to a **higher standard**,
not a lower one. A reworded finding presented as a resolved condition is explicitly
called out as insufficient.
