```prompt
---
description: Angular component implementation — templates, CSS, routing, driven by ui-design spec
---

# UI Implement: ${input:featureName}

## Prerequisites

This prompt requires the `ui-design` phase to be complete. Read `.agent-handoff.md`
before starting — it contains the component specifications, ARIA requirements,
view state definitions, and routing decisions that drive this implementation.

If no handoff exists: run `/ui-design` first.

---

## Setup

```bash
git worktree add /tmp/sba-ui-${input:featureName} -b feat/ui-${input:featureName}
cd /tmp/sba-ui-${input:featureName}/frontend
export PATH="/opt/homebrew/opt/node@22/bin:$PATH"
npm ci
```

---

## Step 1 — Implement each specified component

For each component in the ui-design spec, create three files:

### TypeScript (`[name].component.ts`)

```typescript
import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { [Service] } from '../[service].service';
import { [Model] } from '../[model]';

@Component({
  selector: 'app-[name]',
  templateUrl: './[name].component.html',
  styleUrls: ['./[name].component.css'],
  standalone: false   // ← required: this project is module-based, not standalone
})
export class [Name]Component implements OnInit {
  items: [Model][] = [];
  loading = false;
  error = '';

  constructor(private [service]: [Service]) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.[service].getItems().subscribe({
      next: (data) => { this.items = data; this.loading = false; },
      error: () => { this.error = 'Failed to load.'; this.loading = false; }
    });
  }
}
```

Rules:
- `standalone: false` on every component
- All HTTP goes through a service — never `HttpClient` directly in a component
- `loading`, `error`, and content states are always explicit boolean/string fields
- Use `subscribe({ next, error })` — never `.subscribe(data => ...)` without an error handler

### Template (`[name].component.html`)

Follow the HTML structure outline from the ui-design spec exactly. Do not improvise
ARIA structure — implement what was specified.

Required patterns:

```html
<!-- Loading state — always first -->
<div *ngIf="loading" role="status" aria-live="polite" class="loading">
  Loading…
</div>

<!-- Error state — always before content -->
<div *ngIf="error && !loading" role="alert" class="error">
  {{ error }}
</div>

<!-- Empty state — explicit, not silent -->
<p *ngIf="!loading && !error && items.length === 0">
  No [items] found.
</p>

<!-- Content state -->
<ul *ngIf="!loading && !error && items.length > 0"
    role="list" aria-label="[descriptive label]">
  <li *ngFor="let item of items">
    <!-- content -->
    <button type="button"
            [attr.aria-label]="'[action] ' + item.name"
            (click)="onAction(item)">
      [Label]
    </button>
  </li>
</ul>
```

### CSS (`[name].component.css`)

Scoped to this component only. Follow the density and spacing of the nearest
equivalent existing component. Do not use global class names or `!important`.

```css
/* [ComponentName] */
.container {
  /* layout */
}

.loading {
  /* subtle, non-blocking */
}

.error {
  color: #c0392b;  /* accessible red — meets WCAG AA on white */
}
```

---

## Step 2 — Register in AppModule

Open `frontend/src/app/app.module.ts`:

1. Import the new component class
2. Add to `declarations: [...]`
3. If a new service was created, add to `providers: [...]` (usually not needed — use `providedIn: 'root'`)

---

## Step 3 — Register routes (if new views)

Open `frontend/src/app/app-routing.module.ts`:

```typescript
const routes: Routes = [
  // existing routes...
  { path: '[new-path]', component: [NewComponent] },
];
```

If the component appears in navigation, add a `routerLink` to the nav template.

---

## Step 4 — Write the component spec

### Component spec (`[name].component.spec.ts`)

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { [Name]Component } from './[name].component';
import { [Service] } from '../[service].service';
import { of, throwError } from 'rxjs';

describe('[Name]Component', () => {
  let component: [Name]Component;
  let fixture: ComponentFixture<[Name]Component>;
  let serviceSpy: jasmine.SpyObj<[Service]>;

  beforeEach(async () => {
    serviceSpy = jasmine.createSpyObj('[Service]', ['getItems', 'deleteItem']);
    serviceSpy.getItems.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      declarations: [[Name]Component],
      providers: [{ provide: [Service], useValue: serviceSpy }],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent([Name]Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display items returned by service', () => {
    const items = [{ id: 1, name: 'Alice' }];
    serviceSpy.getItems.and.returnValue(of(items));
    component.ngOnInit();
    fixture.detectChanges();
    expect(component.items.length).toBe(1);
  });

  it('should set error message when service call fails', () => {
    serviceSpy.getItems.and.returnValue(throwError(() => new Error('fail')));
    component.ngOnInit();
    fixture.detectChanges();
    expect(component.error).toBeTruthy();
  });

  it('should show loading state during fetch', () => {
    // verify loading flag is set before data arrives
    component.loading = true;
    fixture.detectChanges();
    const el = fixture.nativeElement.querySelector('[role="status"]');
    expect(el).not.toBeNull();
  });
});
```

---

## Step 5 — Run and verify

```bash
export PATH="/opt/homebrew/opt/node@22/bin:$PATH"
cd /tmp/sba-ui-${input:featureName}/frontend

# Lint
npm run lint 2>&1 | tail -20

# Test
npm test -- --watch=false --browsers=ChromeHeadless 2>&1 | tail -20

# Production build (catches template compilation errors)
npm run build:prod 2>&1 | tail -20
```

All three must succeed. Template compilation errors in `build:prod` are not
caught by `npm test` alone — the prod build is mandatory.

---

## Step 6 — Commit

```bash
git add frontend/src/app/[component]/ frontend/src/app/app.module.ts frontend/src/app/app-routing.module.ts
git commit -m "feat(frontend): implement [feature] UI components

- [ComponentName]: [purpose]
- Route: /[path]
- ARIA: [key accessibility decisions]
- View states: loading, error, empty, populated"
```

---

## Step 7 — Cherry-pick and verify in main tree

```bash
git cherry-pick <ui-commit-hash>
export PATH="/opt/homebrew/opt/node@22/bin:$PATH"
cd frontend && npm test -- --watch=false --browsers=ChromeHeadless 2>&1 | tail -10
cd frontend && npm run build:prod 2>&1 | tail -5
git push origin modernize/full-update

git worktree remove /tmp/sba-ui-${input:featureName}
git branch -D feat/ui-${input:featureName}
```

---

## On Completion → Write Handoff

Write `.agent-handoff.md`:

```markdown
## Completed prompt: ui-implement
## Feature: ${input:featureName}

## Components implemented
[list: ComponentName — file path]

## Routes added
[list or none]

## Test results
Frontend: TOTAL: N SUCCESS, 0 FAILED
Build: npm run build:prod — SUCCESS

## Accessibility implementation
[list ARIA decisions implemented]

## Deviations from ui-design spec
[list or "implemented as specified"]

## Recommended next prompt
accessibility-audit — verify ARIA implementation and keyboard navigation
```
```
