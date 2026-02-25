```prompt
---
description: Cut a release — changelog, version bump, tag, and release PR to main
---

# Release: ${input:version}

## Release type

${input:releaseType}

Valid values: `patch` | `minor` | `major`

Use [Semantic Versioning](https://semver.org):
- `patch` — bug fixes only, no new features, no breaking changes
- `minor` — new features, backward compatible
- `major` — breaking changes

---

## Pre-release checklist

Run these before starting. All must pass.

```bash
# Backend
cd backend
JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew check 2>&1 | tail -5
# Expected: BUILD SUCCESSFUL

# Frontend
export PATH="/opt/homebrew/opt/node@22/bin:$PATH"
cd frontend && npm test -- --watch=false --browsers=ChromeHeadless 2>&1 | tail -5
# Expected: TOTAL: N SUCCESS, 0 FAILED

# Check current branch
git branch --show-current
# Must be: modernize/full-update (or release/* branch)

# Check nothing uncommitted
git status --short
# Must be empty
```

If any check fails: **stop**. Do not cut a release over a broken build.

---

## Step 1 — Find the last release tag

```bash
git tag --sort=-creatordate | head -10
git log $(git describe --tags --abbrev=0)..HEAD --oneline
```

If no prior tags exist, use `git log --oneline` from the beginning.

---

## Step 2 — Generate the changelog

Parse commits since the last tag. Group by conventional commit type:

```bash
git log $(git describe --tags --abbrev=0 2>/dev/null || echo "")..HEAD \
  --pretty=format:"- %s (%h)" \
  --no-merges
```

Group into sections:

```markdown
## [${input:version}] — YYYY-MM-DD

### Breaking Changes
- [commits with type: feat! or BREAKING CHANGE footer]

### Features
- [commits with type: feat]

### Bug Fixes
- [commits with type: fix]

### Performance
- [commits with type: perf]

### Refactoring
- [commits with type: refactor]

### Tests
- [commits with type: test]

### CI & Dependencies
- [commits with type: ci, chore(deps), chore(workspace)]

### Documentation
- [commits with type: docs]
```

Append this block to the top of `CHANGELOG.md` (create it if it doesn't exist).

---

## Step 3 — Bump version numbers

### Backend — `backend/build.gradle`

```groovy
// Change:
version = 'CURRENT_VERSION'
// To:
version = '${input:version}'
```

### Frontend — `frontend/package.json`

```json
{
  "version": "${input:version}"
}
```

Verify both files reflect the new version string.

---

## Step 4 — Final build with new version

```bash
# Backend rebuild with new version
cd backend
JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew check 2>&1 | tail -5

# Frontend rebuild
cd frontend && npm run build:prod 2>&1 | tail -10
```

---

## Step 5 — Commit and tag

```bash
git add CHANGELOG.md backend/build.gradle frontend/package.json
git commit -m "chore(release): ${input:version}"
git tag -a "v${input:version}" -m "Release ${input:version}"
```

---

## Step 6 — Create release PR to main

```bash
git push origin modernize/full-update
git push origin "v${input:version}"
```

Then open a PR: `modernize/full-update` → `main`

PR title: `Release ${input:version}`

PR body template:
```markdown
## Release ${input:version}

### Summary
[2-3 sentence summary of what this release contains]

### Changelog
[paste the changelog section from Step 2]

### Checklist
- [ ] Backend tests pass (BUILD SUCCESSFUL)
- [ ] Frontend tests pass (TOTAL: N SUCCESS, 0 FAILED)
- [ ] Version bumped in build.gradle and package.json
- [ ] CHANGELOG.md updated
- [ ] Tag v${input:version} pushed
```

---

## Post-release

After PR is merged:

```bash
# Tag is already on the commit; GitHub auto-creates a release from the tag if configured
# Archive the release in GitHub Releases with the changelog body
```

---

## On Completion → Write Handoff

Write `.agent-handoff.md`:

```markdown
## Completed prompt: release
## Version: ${input:version}
## Type: ${input:releaseType}

## Commits in this release
[count and conventional commit type breakdown]

## Tag
v${input:version} — pushed to origin

## PR
[PR URL or "not yet created"]

## Test results at release cut
- Backend: BUILD SUCCESSFUL
- Frontend: TOTAL: N SUCCESS, 0 FAILED

## Recommended next prompt
security-audit — post-release audit of the tagged state
```
```
