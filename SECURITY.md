# Security Policy

## Supported Versions

| Version | Supported |
| ------- | --------- |
| 1.0.x   | ✅ yes    |

## Reporting a Vulnerability

Please **do not** open a public GitHub issue for security vulnerabilities. Instead, use [GitHub's private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing/privately-reporting-a-security-vulnerability) for this repository.

You can expect an acknowledgement within 5 business days and a fix or mitigation plan within 30 days depending on severity.

## Security Controls in This Application

### Credentials

- Database credentials are never committed to source control. A `.env.example` template is provided; developers copy it to `backend/docker/.env` (gitignored) and set their own values before starting Docker.
- Application datasource URL, username, and password are read from environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) with safe localhost-only defaults.

### CORS

- Cross-origin access is restricted to a configurable allow-list via the `CORS_ALLOWED_ORIGINS` environment variable (default: `http://localhost:4200`).
- `Access-Control-Allow-Credentials: true` is set alongside specific origins — wildcard origins are not permitted.
- CORS is enforced by Spring's `WebMvcConfigurer` (`CorsConfig`), not by a servlet filter, ensuring it applies to all request paths including error responses.

### Input Validation

- All inbound request bodies are validated with Jakarta Bean Validation (`@Valid`). Constraint violations return RFC 9457 `ProblemDetail` responses with HTTP 400 and a list of field-level errors — no stack traces are exposed.

### Dependency Management

- Dependabot is enabled for both Gradle and npm dependencies.
- Dependabot PRs are automatically approved and merged for **patch** version updates only, and only after CI is green.
- Minor and major updates require human review.

### SAST

- CodeQL analysis runs on every pull request and on a weekly schedule, scanning both Java (`java-kotlin`) and TypeScript/JavaScript (`javascript-typescript`) code.

