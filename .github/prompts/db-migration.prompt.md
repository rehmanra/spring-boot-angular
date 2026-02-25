```prompt
---
description: Database schema evolution — SQL migration, entity sync, DAO update, H2 test verification
---

# DB Migration: ${input:changeName}

## Change description

${input:sqlChange}

---

## This project's migration approach

This project uses **Flyway** for schema versioning (or manual scripts if Flyway is not yet integrated).
Tests use H2 in-memory with `jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1`.

Check current state:
```bash
# Is Flyway present?
grep -i flyway backend/build.gradle
ls backend/src/main/resources/db/migration/ 2>/dev/null || echo "No migration directory"
```

---

## Step 1 — Write the migration SQL

### If Flyway IS configured

Create the next versioned script:
```
backend/src/main/resources/db/migration/V[NEXT]__[change_name].sql
```

Naming: `V2__add_email_to_user.sql` (incrementing integer version, double underscore)

SQL must be:
- PostgreSQL 17 compatible
- H2 (`MODE=PostgreSQL`) compatible — avoid PostgreSQL-specific functions H2 can't handle
- Idempotent where possible (`IF NOT EXISTS`, `IF EXISTS`)
- Reversible (include a comment showing the `DOWN` equivalent for reference)

```sql
-- V[NEXT]__${input:changeName}.sql
-- Down: [reverse SQL]

${input:sqlChange}
```

### If Flyway NOT configured

Write the SQL to `backend/src/main/resources/db/schema-delta.sql` and note that
introducing Flyway is a prerequisite before this change goes to any shared or production environment.
File this as an open item in the handoff.

---

## Step 2 — Update the JPA entity

Read `backend/src/main/java/…/model/User.java` for the canonical pattern.

```java
// Add new column(s) to the entity
@Column(name = "email", nullable = false, length = 255)
private String email;

// If nullable (for gradual rollout):
@Column(name = "email")
private String email;
```

Rules:
- Use field access (`@Id` on field, not getter)
- `@Column` annotation on every non-default field
- Sequence generator pattern: `@SequenceGenerator` + `@GeneratedValue` — do not change existing generators
- No `@Transient` on fields that are in the migration script

---

## Step 3 — Update the DTO

Add the new field to the record DTO with appropriate Bean Validation:

```java
// backend/src/main/java/…/dto/[Entity]DTO.java
public record UserDTO(
    Integer id,
    @NotBlank String name,
    @Email @NotBlank String email  // new field
) {}
```

---

## Step 4 — Update converters (both directions)

`UserToDTOConverter`:
```java
return new UserDTO(user.getId(), user.getName(), user.getEmail());
```

`UserDTOToModelConverter`:
```java
User user = new User();
user.setName(dto.name());
user.setEmail(dto.email());  // new field
return user;
```

Converters remain **pure mapping functions** — no service calls.

---

## Step 5 — Update the DAO (if new query methods needed)

```java
// Example: find by new indexed field
List<User> findByEmailIgnoreCase(String email);
Optional<User> findByEmail(String email);
```

---

## Step 6 — Update test data and test assertions

Backend tests that construct `UserDTO` or `User` must be updated to include the new field.

```java
// In @WebMvcTest and @DataJpaTest
UserDTO dto = new UserDTO(null, "Alice", "alice@example.com");
User user = new User();
user.setName("Alice");
user.setEmail("alice@example.com");
```

---

## Step 7 — Verify H2 compatibility

Run the full test suite. H2 `MODE=PostgreSQL` handles most constructs but not all.

Known incompatible PostgreSQL-isms to avoid:
- `UUID` type (use `VARCHAR(36)` or `BIGINT` for H2 compatibility)
- `JSONB` type (not supported in H2)
- `TIMESTAMP WITH TIME ZONE` → use `TIMESTAMP` in H2 context

```bash
cd backend
JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew check 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

---

## Step 8 — Frontend type alignment

Update the Angular model interface:

```typescript
// frontend/src/app/user.ts
export interface User {
  id?: number;
  name: string;
  email: string;  // new field
}
```

Then update any component templates that display/edit `User` objects.

---

## Step 9 — Commit

```bash
# Separate commits for migration and application code
git commit -m "chore(db): add V[N]__${input:changeName} migration script"
git commit -m "feat(backend): sync entity, DTO, converters for ${input:changeName}"
git commit -m "feat(frontend): update User interface and templates for ${input:changeName}"
```

---

## On Completion → Write Handoff

Write `.agent-handoff.md`:

```markdown
## Completed prompt: db-migration
## Change: ${input:changeName}

## Migration script
[filename and one-line summary of SQL change]

## Files updated
- Entity: [field added/modified]
- DTO: [field added/modified]
- Converters: [both directions updated Y/N]
- DAO: [new query methods Y/N]
- Frontend model: [updated Y/N]

## H2 compatibility
[verified — all tests pass / issues encountered]

## Test results
Backend: BUILD SUCCESSFUL — N tests passing

## Flyway status
[configured and running / not yet configured — open item]

## Recommended next prompt
new-endpoint — schema is ready, implement the feature
```
```
