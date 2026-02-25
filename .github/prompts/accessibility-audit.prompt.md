```prompt
---
description: WCAG 2.1 AA compliance audit — keyboard navigation, ARIA, color contrast, screen reader
---

# Accessibility Audit

## Scope

${input:scope}

If no scope is specified, audit all Angular components in `frontend/src/app/`.

**Target standard:** WCAG 2.1 Level AA

---

## Step 1 — Component inventory

```bash
find frontend/src/app -name "*.component.html" | sort
```

For each component, read the HTML template and evaluate the dimensions below.

---

## Dimension 1 — Keyboard navigation (WCAG 2.1.1 / 2.4.3)

Every user action reachable by mouse must be reachable by keyboard alone.

Read each template. For each interactive element, verify:

| Element | Expected behaviour | Issue if... |
|---------|-------------------|-------------|
| `<button>` | Focusable, activates with Enter/Space | Missing if using `<div (click)>` instead |
| `<a routerLink>` | Focusable, activates with Enter | Missing if `href` is absent |
| Form inputs | Focusable in logical DOM order | Issue if `tabindex=-1` without justification |
| Custom interactive elements | Must have `tabindex="0"` + keyboard handler | Issue if only `(click)` without `(keydown.enter)` and `(keydown.Space)` |

```bash
# Find non-semantic interactive elements
grep -rn "(click)" frontend/src/app --include="*.html" | grep -v "<button\|<a "
```

Flag every `(click)` handler on a non-interactive element (`<div>`, `<span>`, `<li>`, etc.)
without a corresponding `tabindex="0"` + keyboard event handler.

---

## Dimension 2 — Focus management (WCAG 2.4.3 / 2.4.7)

```bash
# Find negative tabindex usage
grep -rn "tabindex" frontend/src/app --include="*.html"
```

| Issue | Risk |
|-------|------|
| `tabindex="-1"` on a visible interactive element | Keyboard inaccessible |
| Focus not moved to new content after route navigation | User loses context |
| Modal/dialog (if any) doesn't trap focus | Focus escapes to background |
| `tabindex` values > 0 | Breaks natural DOM order — use sparingly |

For Angular SPA navigation: when the route changes, focus should move to the
main heading or the top of the new view. Check if any focus management exists:

```bash
grep -rn "focus\|scrollIntoView" frontend/src/app --include="*.ts" | grep -v spec
```

---

## Dimension 3 — Labels and names (WCAG 1.3.1 / 4.1.2)

Every interactive element needs an accessible name — either visible text, `<label>`,
`aria-label`, or `aria-labelledby`.

```bash
# Find unlabelled buttons (button with no text content)
grep -n "<button" frontend/src/app/**/*.html 2>/dev/null || \
grep -rn "<button" frontend/src/app --include="*.html"

# Find inputs without labels
grep -rn "<input" frontend/src/app --include="*.html"
```

For each `<button>`:
- Does it have visible text? If icon-only → must have `aria-label`
- If the label is dynamic (e.g., "Delete Alice"): `[attr.aria-label]="'Delete ' + user.name"`

For each `<input>`:
- Is there a `<label for="[id]">`? Placeholder alone does not satisfy WCAG.
- Is `id` set on the input to match the `for` attribute?

For each form:
- Is the form element wrapped in a `<form>` tag?
- Are required fields marked with `required` attribute AND communicated to screen readers?

---

## Dimension 4 — Images and icons (WCAG 1.1.1)

```bash
grep -rn "<img\|<svg\|mat-icon\|fa-icon" frontend/src/app --include="*.html"
```

| Element | Requirement |
|---------|-------------|
| `<img>` conveying content | Must have descriptive `alt` |
| `<img>` that is decorative | Must have `alt=""` |
| Icon inside labelled `<button>` | Icon should have `aria-hidden="true"` |
| Standalone icon with no text | Needs `aria-label` on the button |

---

## Dimension 5 — Live regions and dynamic content (WCAG 4.1.3)

```bash
grep -rn "aria-live\|role=\"alert\"\|role=\"status\"" frontend/src/app --include="*.html"
```

Check every component that has a loading state or a success/error message:

| Pattern | Expected ARIA | Issue if missing |
|---------|---------------|-----------------|
| Loading spinner or message | `role="status"` + `aria-live="polite"` | Screen reader silent during load |
| Error message | `role="alert"` (implicitly `aria-live="assertive"`) | Screen reader doesn't announce error |
| Success toast/confirmation | `aria-live="polite"` | Screen reader silent on success |
| Search results updating | `aria-live="polite"` + count announcement | User doesn't know results changed |

---

## Dimension 6 — Color contrast (WCAG 1.4.3 / 1.4.11)

Read `frontend/src/styles.css` and each component `.css` file.

| Requirement | Threshold |
|-------------|-----------|
| Normal text (< 18pt / < 14pt bold) | 4.5:1 contrast ratio |
| Large text (≥ 18pt or ≥ 14pt bold) | 3:1 contrast ratio |
| UI components (buttons, inputs, focus rings) | 3:1 |

For each `color` / `background-color` pair, estimate the contrast ratio
using the relative luminance formula or flag for manual tool verification
(https://webaim.org/resources/contrastchecker/).

Key areas to check:
- Error message text colour on white background
- Disabled element text/border
- Focus ring visibility against the background
- Placeholder text in inputs (commonly fails — must be 4.5:1)

---

## Dimension 7 — Page structure (WCAG 1.3.1 / 2.4.2 / 2.4.6)

```bash
grep -rn "<h[1-6]\|role=\"main\"\|role=\"navigation\"\|<nav\|<main\|<header\|<footer" \
  frontend/src/app --include="*.html"
```

| Requirement | Check |
|-------------|-------|
| `<title>` set for each route | ✓/✗ |
| Single `<h1>` per view | ✓/✗ |
| Heading hierarchy (no skipping h2→h4) | ✓/✗ |
| `<main>` landmark present | ✓/✗ |
| `<nav>` with `aria-label` if multiple navs | ✓/✗ |

---

## Severity classification

### 🔴 Blocker (legal / functional barrier)
- Interactive element unreachable by keyboard
- Form field has no label
- Error message not announced to screen reader
- Color contrast < 3:1 for any text

### 🟠 Significant (degrades accessibility materially)
- Focus ring absent or invisible
- `(click)` on non-semantic element without keyboard handler
- `aria-label` missing on icon-only button
- Route change doesn't move focus

### 🟡 Minor (best practice gap)
- Placeholder used as label substitute (no `<label>`)
- Decorative image missing `alt=""`
- Loading state missing `aria-live`
- Heading hierarchy has minor gaps

---

## On Completion → Write Handoff

Write `.agent-handoff.md`:

```markdown
## Completed prompt: accessibility-audit
## Scope: ${input:scope}

## Findings summary
- Blockers: N
- Significant: N
- Minor: N

## Blocker findings
[list — each needs fix-issues before ship]

## WCAG 2.1 AA compliance
[COMPLIANT / PARTIALLY COMPLIANT (list exceptions) / NON-COMPLIANT (list blockers)]

## Recommended next prompt
fix-issues — with blocker findings as input
```
```
