```prompt
---
description: OpenAPI-first contract definition and validation before writing implementation code
---

# API Contract: ${input:resourceName}

## Context

Contract-first means the HTTP interface is agreed and validated **before** any implementation
is written. This avoids rework when implementation assumptions diverge from consumer expectations.

In this project Springdoc 3.0.0 auto-generates the spec from annotations at
`/swagger-ui/index.html` (requires a running backend). Use this prompt to design the contract
before writing those annotations.

---

## Step 1 — Read the existing contract

If the backend is running (`./gradlew bootRun` from `backend/`):

```bash
# Fetch the current live spec
curl -s http://localhost:8080/spring-boot-angular/v3/api-docs | python3 -m json.tool
```

If not running, read the controller annotations directly:
```
backend/src/main/java/…/controller/UserController.java
```

Catalogue every existing endpoint:
- Method + path
- Request body shape (if any)
- Response shape
- Status codes

---

## Step 2 — Define the new / changed contract

For each new or changed endpoint, write a full OpenAPI 3.0 path object.

Template:
```yaml
/spring-boot-angular/api/${input:resourceName}/{id}:
  get:
    summary: Get ${input:resourceName} by ID
    operationId: get${input:resourceName}ById
    parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int32
    responses:
      '200':
        description: Found
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/${input:resourceName}DTO'
      '404':
        description: Not found
        content:
          application/problem+json:
            schema:
              $ref: '#/components/schemas/ProblemDetail'
```

Add a `components/schemas` entry for every new DTO:
```yaml
components:
  schemas:
    ${input:resourceName}DTO:
      type: object
      required:
        - name
      properties:
        id:
          type: integer
          format: int32
          readOnly: true
        name:
          type: string
          minLength: 1
          maxLength: 255
```

---

## Step 3 — Validate against project conventions

Check each proposed endpoint against these rules:

| Rule | Check |
|------|-------|
| GET returns 200 or 404 | ✓/✗ |
| POST create returns 201 | ✓/✗ |
| POST with `id` in body returns 400 | ✓/✗ |
| PUT update returns 200 or 404 | ✓/✗ |
| DELETE returns 204 | ✓/✗ |
| Error bodies use `ProblemDetail` (RFC 9457) | ✓/✗ |
| Base path prefix `/spring-boot-angular/api/` | ✓/✗ |
| Plural resource name in path | ✓/✗ |

Flag any deviations and propose corrections.

---

## Step 4 — Write contract tests (Spring MockMvc stubs)

For each endpoint, produce a `@WebMvcTest` stub that pins the contract. These are
**contract tests** — they assert shape and status code, not business logic:

```java
@Test
void get_existingId_returns200WithCorrectShape() throws Exception {
    // Arrange: mock service to return a known DTO
    // Act: perform GET /spring-boot-angular/api/${input:resourceName}s/1
    // Assert: status 200, JSON path $.id exists, $.name is non-blank
}

@Test
void get_missingId_returns404WithProblemDetail() throws Exception {
    // Assert: status 404, content-type application/problem+json
}
```

---

## Step 5 — Frontend type alignment

Verify the Angular model interface matches the DTO:

```typescript
// frontend/src/app/${input:resourceName}.ts
export interface ${input:resourceName} {
  id?: number;      // optional — absent on create request
  name: string;
  // add any new fields here
}
```

Any field with `readOnly: true` in the OpenAPI spec must map to an optional (`?`) TypeScript field.

---

## Step 6 — Sign off checklist

Before proceeding to `new-endpoint`:

- [ ] All status codes match project conventions
- [ ] All error responses use `application/problem+json`
- [ ] DTO schema defined with validation constraints
- [ ] Contract tests written (even if not yet passing)
- [ ] Angular model interface updated to match DTO shape
- [ ] No breaking changes to existing consumers (or breaking changes explicitly noted)

---

## On Completion → Write Handoff

Write `.agent-handoff.md`:

```markdown
## Completed prompt: api-contract
## Resource: ${input:resourceName}

## Endpoints defined
[Method + path + status codes for each]

## DTO schema
[Field list with types and constraints]

## Breaking changes
[none / list]

## Recommended next prompt
new-endpoint — contract is finalized, ready for implementation
```
```
