```prompt
---
description: Detect drift in copilot-instructions.md and resync it after substantial decisions
---

# Sync Instructions

## Purpose

`copilot-instructions.md` is injected into **every** Copilot request. When it drifts
from reality, it silently misinforms every future agent and code generation.

This prompt detects drift and reconciles the file. It should run:
- After `design-feature` (new architectural decisions)
- After `db-migration` (new entities or schema patterns)
- After `new-endpoint` (new API patterns or conventions)
- After `upgrade-deps` (version numbers change)
- After any `fix-issues` or `refactor` that establishes a new "Do Not"
- On a regular cadence (monthly at minimum)

---

## Step 1 — Establish what has changed since the last sync

```bash
# Find the last commit that touched copilot-instructions.md
git log --oneline -- .github/copilot-instructions.md | head -5

# Find all commits since that hash
LAST_SYNC=$(git log --oneline -- .github/copilot-instructions.md | head -1 | awk '{print $1}')
git log ${LAST_SYNC}..HEAD --oneline
```

Also read `.agent-handoff.md` for any decisions recorded by recent agents.

---

## Step 2 — Audit each section of copilot-instructions.md

Read the full file, then scan the codebase to verify each claim is still true.

### Section: Tech Stack — Exact Versions

```bash
# Backend — check declared versions
grep -E "version|springBootVersion|springdocVersion" backend/build.gradle | head -20
cat backend/gradle/wrapper/gradle-wrapper.properties | grep distributionUrl

# Frontend — check declared versions
cat frontend/package.json | python3 -m json.tool | grep '"version"\|angular\|typescript'

# Java toolchain
grep "languageVersion" backend/build.gradle

# Node
node --version 2>/dev/null || /opt/homebrew/opt/node@22/bin/node --version
```

For each version in the instructions table: verify it matches what the build files say.
Flag any mismatch as **stale**.

### Section: Architecture

```bash
# Check the actual file structure matches the diagram
find frontend/src/app -name "*.ts" -not -name "*.spec.ts" | sort
find backend/src/main/java -name "*.java" | sort

# Check API base path
grep -rn "RequestMapping\|GetMapping\|PostMapping" backend/src/main/java --include="*.java" | head -20

# Check any new config classes
find backend/src/main/java -name "*.java" | xargs grep -l "@Configuration" 2>/dev/null
```

Flag any:
- New components, services, or modules not listed in the architecture map
- Renamed or deleted files still referenced in the map
- New API endpoints or path prefixes

### Section: Code Conventions

```bash
# Java — check for any new annotations or patterns established
git log ${LAST_SYNC}..HEAD --diff-filter=A -- "*.java" | head -20

# TypeScript — check for new conventions
git log ${LAST_SYNC}..HEAD --diff-filter=A -- "*.ts" | head -20
```

Look for new patterns in recently added files:
- New imports that should be prohibited (additions to "Do Not")
- New annotations or patterns that should be promoted to conventions

### Section: Testing Patterns

```bash
# Check what test annotations are actually in use
grep -rn "@WebMvcTest\|@DataJpaTest\|@SpringBootTest\|@ExtendWith" \
  backend/src/test/java --include="*.java" | head -20

# Check frontend test setup patterns
grep -rn "provideHttpClientTesting\|jasmine.createSpyObj\|NO_ERRORS_SCHEMA" \
  frontend/src/app --include="*.spec.ts" | head -10
```

Verify the documented patterns match actual usage. If code has diverged from the
documented patterns, determine which is correct:
- If code is correct → update the instructions
- If instructions represent the intended convention → file a `fix-issues` for the code

### Section: Prompt Library

```bash
ls .github/prompts/
```

Verify the prompt table lists every `.prompt.md` file present. Add any new prompts.
Remove entries for deleted prompts.

### Section: Environment & Secrets

```bash
grep -rn "DB_PASSWORD\|CORS_ALLOWED_ORIGINS\|env" \
  backend/src/main/resources/application.yaml
cat .gitignore | grep -i "env\|secret\|handoff"
```

Verify each stated environment variable and secret still exists in the documented location.

### Section: Dependency Notes

```bash
# Check if documented special-case deps are still in use
grep -E "springdoc|webmvc-test|data-jpa-test|karma-coverage" backend/build.gradle frontend/package.json 2>/dev/null
```

Verify the listed dependency exceptions are still relevant.

### Section: Do Not

Review the "Do Not" list against recent commits. Two directions:

1. **New violations found in code review / fix-issues** → add them as new "Do Not" items
2. **Old "Do Not" items that are no longer relevant** (e.g., deprecated library removed) → remove them

---

## Step 3 — Produce a diff proposal

For each detected drift, produce a specific proposed change:

```
STALE: Tech Stack table says "Spring Boot 4.0.3" but build.gradle declares 4.1.0
FIX:   Update table row: Spring Boot | 4.0.3 → 4.1.0

MISSING: New entity `Order` added in commit abc1234 — not in Architecture map
FIX:   Add to backend architecture: "dao/OrderDao.java ← JpaRepository + findByUserId"

NEW CONVENTION: Recent code reviews established "@Column on every entity field"
FIX:   Add to Java conventions: "@Column annotation on every non-default entity field"

STALE PROHIBITION: "Do not use sourceCompatibility" — still accurate, keep
NEW PROHIBITION: "Do not use @Transactional on private methods — Spring AOP silently ignores it"
FIX:   Add to Do Not list
```

---

## Step 4 — Apply the changes

Edit `.github/copilot-instructions.md` to apply all proposed changes.

Principles:
- **Do not soften specifics into generalities.** If the version is 4.1.0, say 4.1.0.
- **Do not let the file grow unbounded.** Remove stale content; don't just append.
- **Do not add opinion.** The file records facts, conventions, and prohibitions — not preferences.
- **Every "Do Not" must have a reason** (either in the text or obvious from context).

---

## Step 5 — Verify the updated file is accurate

After editing, re-read the entire file and answer:

- [ ] Every version number matches the build files
- [ ] Every file path in the Architecture section exists on disk
- [ ] Every "Do Not" is still applicable
- [ ] The prompt library table lists every prompt in `.github/prompts/`
- [ ] The recommended SDLC flows are still accurate
- [ ] No section is contradicted by another section

---

## Step 6 — Commit

```bash
git add .github/copilot-instructions.md
git commit -m "docs(workspace): sync copilot-instructions.md — [one-line summary of main changes]"
git push origin modernize/full-update
```

---

## On Completion → Write Handoff

Write `.agent-handoff.md`:

```markdown
## Completed prompt: sync-instructions
## Triggered by: ${input:trigger}

## Changes made to copilot-instructions.md
[bulleted list of each change — be specific]

## Sections reviewed as accurate (no changes)
[list]

## Items that require follow-up (not fixable in this file)
[e.g., "build.gradle declares RC version — needs upgrade-deps before next sync"]

## Recommended next prompt
[whatever the workflow was before sync-instructions was called — resume there]
```
```
