```prompt
---
description: UX design phase — user flows, component specification, visual hierarchy, accessibility requirements
---

# UI Design: ${input:featureName}

## Your role in this phase

You are the **UX designer and front-end architect**. Your output is a component
specification document — not HTML, not TypeScript. The `ui-implement` prompt
will turn this spec into code.

**Input required from prior phases:**
- User stories and AC from `requirements`
- API shape from `api-contract` or `design-feature`
- Existing component patterns (read `frontend/src/app/`)

---

## Step 1 — Audit the existing UI patterns

Read these files to understand the established component vocabulary:

```bash
# List existing components
find frontend/src/app -name "*.component.html" | sort

# Understand the routing structure
cat frontend/src/app/app-routing.module.ts

# Understand the global styles
cat frontend/src/styles.css
```

Document the existing patterns:
- How are lists rendered? (`users.component.html`)
- How are detail/edit forms rendered? (`user-detail.component.html`)
- How is search rendered? (`user-search.component.html`)
- What CSS conventions exist? (class naming, layout approach)

New UI must be consistent with these patterns unless there is a specific UX reason to deviate.

---

## Step 2 — User flow diagram

For each user story, define the step-by-step user journey as a numbered flow.
No wireframes needed — prose and ASCII diagrams are sufficient.

```
Flow: [story name]

1. User is on [starting view]
   → sees: [key visible elements]

2. User [action — click, type, navigate]
   → system: [immediate feedback — loading state, validation message, etc.]
   → route change (if any): [from → to]

3. Success state
   → sees: [confirmation, updated data, redirect target]

4. Error state
   → sees: [specific error message or inline validation]
   → user can: [recovery action]
```

---

## Step 3 — Component specification

For each new or substantially changed component, write a spec:

```
Component: [ComponentName]
File: frontend/src/app/[path]/[name].component.{ts,html,css}
Route (if new view): /[path]
Declared in: app.module.ts

PURPOSE
[One-paragraph description of what this component does and why it exists]

INPUTS (@Input properties)
- [name]: [type] — [what it controls]

OUTPUTS (@Output events)
- [name]: EventEmitter<[type]> — [when it emits, what payload]

SERVICE DEPENDENCIES
- [ServiceName] — [which methods are called and why]

VIEW STATES
- Loading: [what the user sees while data is fetching]
- Empty: [what the user sees when there's no data]
- Populated: [the primary content state]
- Error: [what the user sees when a service call fails]

KEY INTERACTIONS
- [user action] → [component behaviour] → [visual outcome]

VALIDATION (for forms)
- [field name]: [constraints] — [error message text]
```

---

## Step 4 — HTML structure outline

Not full HTML — just the semantic structure with key ARIA annotations:

```html
<!-- [ComponentName] structure -->
<section aria-labelledby="[heading-id]">
  <h2 id="[heading-id]">[Section heading]</h2>

  <!-- Loading state -->
  <div *ngIf="loading" role="status" aria-live="polite">Loading…</div>

  <!-- Error state -->
  <div *ngIf="error" role="alert">[Error message]</div>

  <!-- Primary content -->
  <ul role="list" aria-label="[descriptive label]">
    <li *ngFor="let item of items">
      <!-- item content -->
    </li>
  </ul>

  <!-- Action -->
  <button type="button" [attr.aria-label]="'[action] ' + item.name">
    [label]
  </button>
</section>
```

Key ARIA rules for this project:
- Every interactive element has a visible or `aria-label` label
- Dynamic content regions use `aria-live="polite"` (non-critical) or `aria-live="assertive"` (errors)
- Lists of items use `role="list"` + descriptive `aria-label`
- Form fields have associated `<label for="...">` — not just placeholder text
- Loading indicators use `role="status"`
- Error messages use `role="alert"`

---

## Step 5 — CSS approach

Describe layout and visual treatment — not specific pixel values, but the structural intent:

```
Layout: [flex row / flex column / grid N-col / none]
Responsive breakpoint: [if applicable — below Npx, switch to single column]
Spacing: [consistent with existing components — match [reference component]]
New CSS classes needed (if any):
  .[name] — [purpose]
```

Do not introduce a new CSS framework or utility library without an ADR.
This project uses plain CSS. Keep new rules minimal and co-located in the component's `.css` file.

---

## Step 6 — Accessibility checklist (design phase)

Produce this checklist now. The `accessibility-audit` prompt will verify implementation against it.

| Requirement | Design decision |
|-------------|----------------|
| Keyboard navigation | Tab order: [describe expected focus flow] |
| Focus visible | All interactive elements have visible focus ring |
| Color contrast | Text on background meets WCAG AA (4.5:1 for normal, 3:1 for large text) |
| Screen reader labels | Every action button has unique `aria-label` |
| Form errors | Inline, associated with the field via `aria-describedby` |
| Loading states | `role="status"` + `aria-live="polite"` |
| Empty states | Not silent — user is explicitly told why no data is shown |

---

## Step 7 — Navigation and routing

If new routes are added:

```
New route: /[path]
Module: AppRoutingModule
Component: [ComponentName]
Guard required: [yes/no — if yes, describe the condition]
Link in nav: [yes/no — which nav component, what label]
Browser title: [page title for <title> tag]
```

---

## On Completion → Write Handoff

Write `.agent-handoff.md`:

```markdown
## Completed prompt: ui-design
## Feature: ${input:featureName}

## New components
[list: ComponentName — one-line purpose]

## New routes
[list: /path → ComponentName]

## Accessibility requirements
[key ARIA decisions made in this design]

## View states defined
[list: component → [loading, empty, populated, error]]

## Design decisions that deviate from existing patterns
[list or "none — consistent with existing patterns"]

## Recommended next prompt
ui-implement — spec is ready; implement templates, CSS, and component logic
```
```
