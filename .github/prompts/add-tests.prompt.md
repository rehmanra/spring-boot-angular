```prompt
---
description: Expand test coverage for a specific file, class, or module
---

# Add Tests: ${input:target}

## Objective

Add missing tests for **${input:target}** following the project's established
test patterns. The existing test suite must still pass after this work.

---

## Step 1 — Audit current coverage

### Backend target (if applicable)

Find the source file and its spec:
```bash
# List test results to see current pass/fail state
find backend/build/test-results -name "*.xml" 2>/dev/null | head -20
```

Read the source file. Read the existing test file (if any). Build a two-column matrix:

| Scenario | Covered? |
|----------|----------|
| Happy path | ✓/✗ |
| Not found (404) | ✓/✗ |
| Validation failure (400) | ✓/✗ |
| Concurrent modification | ✓/✗ |
| [other edge cases] | ✓/✗ |

### Frontend target (if applicable)

Read the source `.ts` file and its `.spec.ts`. Cover the same matrix.

---

## Step 2 — Identify the correct test slice

### Backend

| What to test | Annotation | What it gives you |
|---|---|---|
| Controller HTTP contract | `@WebMvcTest(TargetController.class)` | MockMvc, no DB |
| DAO custom queries | `@DataJpaTest` | H2 in-memory, real JPA |
| Service logic (non-trivial) | `@ExtendWith(MockitoExtension.class)` | Pure unit, mock DAO |
| Converter mapping | Plain JUnit 5 | No Spring context needed |

Do **not** use `@SpringBootTest` for anything except `contextLoads()`.

Required imports for controller tests:
```java
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;  // Jackson 3
```

### Frontend

| What to test | Pattern |
|---|---|
| Service HTTP calls | `provideHttpClientTesting()` + `HttpTestingController` |
| Component rendering | `jasmine.SpyObj<ServiceType>` + `NO_ERRORS_SCHEMA` |
| Search debounce | `fakeAsync` + `tick(300)` |

---

## Step 3 — Write the missing tests

Follow naming convention: `methodName_condition_expectedResult`

Examples:
```java
// Backend
void getUser_existingId_returns200WithDTO()
void getUser_missingId_returns404()
void createUser_validBody_returns201()
void createUser_idInBody_returns400()
void createUser_blankName_returns400()
void updateUser_existingId_returns200()
void updateUser_missingId_returns404()
void deleteUser_existingId_returns204()
```

```typescript
// Frontend service
it('getUsers returns user array on success')
it('getUsers returns empty array on HTTP error')
it('deleteUser calls correct URL')
it('deleteUser logs error on HTTP 404')
```

Each test must:
- Be independent (no shared mutable state between tests)
- Assert a specific observable outcome (not just "no exception")
- Mock exactly what is needed, no more

---

## Step 4 — Run and verify

### Backend

```bash
cd backend
JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew test --tests "*.${input:target}*" 2>&1 | tail -30
# Then run the full suite
JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew check 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL` with all tests passing. Zero failures.

### Frontend

```bash
export PATH="/opt/homebrew/opt/node@22/bin:$PATH"
cd frontend
npm test -- --watch=false --browsers=ChromeHeadless 2>&1 | tail -20
```

Expected: `TOTAL: N SUCCESS, 0 FAILED` where N increased from the baseline.

---

## Step 5 — Report coverage delta

After tests pass, state:

```
Before: N tests
After:  N+M tests
New scenarios covered: [list]
Remaining gaps (if any): [list or "none identified"]
```

---

## On Completion → Write Handoff

Write `.agent-handoff.md`:

```markdown
## Completed prompt: add-tests
## Target: ${input:target}

## Tests added
[list of new test method names]

## Test results
- Backend: BUILD SUCCESSFUL — N tests passing
- Frontend: TOTAL: N SUCCESS, 0 FAILED

## Remaining coverage gaps
[list or none]

## Recommended next prompt
code-review — review the new tests for quality and completeness
```
```
