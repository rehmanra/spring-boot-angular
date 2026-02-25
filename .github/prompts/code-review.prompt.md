---
description: Deep code review of a file, diff, or feature
---

# Code Review

## What to review

${input:target}

## Review dimensions (evaluate all, flag findings by severity)

### 🔴 Critical (must fix before merge)
- Secrets or credentials in tracked files
- SQL injection / XSS / CSRF vulnerabilities
- Data loss risk (missing transactions, uncaught exceptions swallowing errors)
- HTTP status codes that mislead clients (e.g., 200 on failure)

### 🟠 Significant (should fix)
- Circular dependencies between layers (e.g., converter → service → converter)
- Missing existence checks before writes (silent create instead of 404)
- Uncovered error paths (exception types not handled by GlobalExceptionHandler)
- N+1 query risk (missing `@EntityGraph` or join fetch)
- `any` type in TypeScript where `unknown` is correct
- Unused imports

### 🟡 Minor (fix before shipping to production)
- String concatenation in log calls (use SLF4J parameterised args)
- Instance log field instead of static
- Missing `@Column` annotation on entity fields
- `tap(_ => ...)` where `tap(() => ...)` is correct
- Test names that don't describe behaviour (should be `methodName_condition_expectedResult`)

### 🔵 Informational (code smell, tech debt)
- Methods doing more than one thing
- Tests with multiple unrelated assertions (should be separate test methods)
- Missing edge case coverage
- Inconsistency with established patterns in this codebase

## Project context

Refer to `.github/copilot-instructions.md` for the authoritative conventions,
test patterns, and "Do Not" list for this project.

## Output format

Group findings by severity. For each finding include:
- File and line reference
- What the issue is
- Why it matters
- Specific fix (code snippet where appropriate)

End with a summary verdict: APPROVE / APPROVE WITH MINOR COMMENTS / REQUEST CHANGES
