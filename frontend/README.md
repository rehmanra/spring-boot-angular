# Frontend

Angular 19.2 SPA generated with [Angular CLI](https://github.com/angular/angular-cli) 19.2.

Requires **Node.js 22+**. Install dependencies with `npm ci` before running any scripts.

## Development server

```bash
npm start
```

Serves at `http://localhost:4200` with live reload. API calls to `/spring-boot-angular/api/**` are proxied to `http://localhost:8080` via `src/proxy.conf.json` — the backend must be running.

## Linting

```bash
npm run lint
```

Runs ESLint 9 (flat config) with `@angular-eslint` rules across all `src/**/*.ts` and `src/app/**/*.html` files.

## Unit tests

```bash
npm test          # single run, ChromeHeadless (used by CI)
npm run test:watch  # watch mode
```

Tests run via [Karma](https://karma-runner.github.io) + Jasmine with ChromeHeadless. Coverage reports are written to `coverage/frontend/`.

## Building

```bash
npm run build:prod   # production bundle to dist/
npm run build:ci     # clean → test → build:prod (CI pipeline)
```

Production build uses `--configuration production`: AOT compilation, tree-shaking, content hashing, and file replacement for `environment.prod.ts`.

## Code scaffolding

```bash
npx ng generate component components/my-component
```

See the [Angular CLI reference](https://angular.dev/tools/cli) for all generator options.
