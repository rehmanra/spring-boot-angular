---
description: Investigate, fix, and verify one or more issues across the stack
---

# Fix Issues

## Issues to fix

${input:issues}

## Process

### Step 1 — Triage

For each issue, determine:
- Which layer(s) are affected: backend only / frontend only / both
- Whether changes are in non-overlapping files (if yes → parallel agents)
- Whether any test needs updating / adding to prevent regression

### Step 2 — Worktrees

If backend + frontend are both affected (typical), create parallel worktrees:

```bash
git worktree add /tmp/sba-fix-be -b fix/backend-$(date +%s)
git worktree add /tmp/sba-fix-fe -b fix/frontend-$(date +%s)
```

If only one layer, one worktree is sufficient.

### Step 3 — Dispatch agents

**Backend agent** (if needed):
- Working directory: `/tmp/sba-fix-be/backend/`
- Java: `JAVA_HOME=~/.sdkman/candidates/java/current`
- Verify: `./gradlew check` → `BUILD SUCCESSFUL`
- Commit with conventional commit message

**Frontend agent** (if needed):
- Working directory: `/tmp/sba-fix-fe/frontend/`
- Node: `export PATH="/opt/homebrew/opt/node@22/bin:$PATH"`
- Verify: `npm test` → `TOTAL: N SUCCESS, 0 FAILED`
- Commit with conventional commit message

### Step 4 — Review

Before merging:
- Read every changed file
- Verify test names and assertions match the fix intent
- Check no extraneous changes slipped in

### Step 5 — Cherry-pick + push

```bash
git cherry-pick <be-hash> <fe-hash>   # order: backend first
git push origin modernize/full-update
git worktree remove /tmp/sba-fix-be
git worktree remove /tmp/sba-fix-fe
git branch -D <be-branch> <fe-branch>
```

## Quality bar

Every fix must include at minimum one regression test that would have caught
the original issue. If the fix is in a method that already has a test, update
that test to cover the bug case explicitly.
