---
description: Add a new REST endpoint end-to-end (backend + frontend)
---

# New Endpoint: ${input:entityName} — ${input:description}

Add a complete new endpoint to the spring-boot-angular stack following the established patterns.

## Scope

Entity: **${input:entityName}**
HTTP method + path: **${input:method} /api/${input:path}**
Description: **${input:description}**

## Backend tasks (worktree: /tmp/sba-be-${input:entityName})

Use `git worktree add /tmp/sba-be-${input:entityName} -b feat/be-${input:entityName}` first.

1. Add JPA entity in `model/` with `@Entity`, `@Table`, sequence generator
2. Add `JpaRepository` in `dao/`
3. Add service interface + `@Service @Transactional` impl in `service/`
4. Add Java record DTO in `dto/` with Bean Validation annotations
5. Add `@Component` converters in `converter/${input:entityName}/`
6. Add controller method in existing or new `@RestController` with correct HTTP status codes
7. Update `GlobalExceptionHandler` if new exception types arise
8. Write `@WebMvcTest` controller test (happy path + 404 + validation 400)
9. Write `@DataJpaTest` DAO test for any custom query methods
10. Run `JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew check` — must be `BUILD SUCCESSFUL`

## Frontend tasks (worktree: /tmp/sba-fe-${input:entityName})

Use `git worktree add /tmp/sba-fe-${input:entityName} -b feat/fe-${input:entityName}` first.

1. Add method to `user.service.ts` (or create new service) following existing pattern:
   - `tap(() => ...)` not `tap(_ => ...)`
   - `catchError(this.handleError<T>('operationName', fallback))`
2. Add route to `app-routing.module.ts` if a new view is needed
3. Add component with `standalone: false`, declare in `app.module.ts`
4. Write service spec with `provideHttpClientTesting()` + `HttpTestingController`
5. Write component spec with `jasmine.SpyObj` + `NO_ERRORS_SCHEMA`
6. Run `export PATH="/opt/homebrew/opt/node@22/bin:$PATH" && npm test` — must be `TOTAL: N SUCCESS`

## Integration

After both agents succeed:
1. Cherry-pick both commits into `modernize/full-update`
2. Review diffs
3. Push: `git push origin modernize/full-update`
4. Clean up worktrees

## Constraints

- Do not use `@SpringBootTest` for controller tests
- Do not use `HttpClientModule` or `RouterTestingModule`
- Do not add fallback defaults for any new secrets in `application.yaml`
- Converters must be pure mapping functions — no service calls inside converters
