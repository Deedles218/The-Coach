# Mobile automation project rules

## Repository freshness and Git safety

- At the start of a new code-change task, when resuming after a long break,
  and before preparing a PR, inspect the repository root, current branch
  (including detached HEAD), worktrees, and staged/unstaged/untracked changes.
  Run `git fetch origin` before choosing a base or claiming the checkout is current.
- If fetch fails, report that remote freshness is unverified. Do not apply
  automatic updates using stale remote-tracking refs.
- After fetch, compare the current HEAD with `origin/main` and, if configured,
  the current branch's upstream. Distinguish remote-tracking refs, local branches,
  and the files actually checked out; report any ahead/behind commits.
- Automatically fast-forward only an already checked-out `main`, and only when
  the index and working tree are clean (including untracked files), no Git
  operation is in progress, HEAD is an ancestor of `origin/main`, and no other
  active chat is using that checkout. If exclusive use cannot be confirmed,
  report the proposed update instead of applying it. Use only
  `git merge --ff-only --no-overwrite-ignore origin/main`; never overwrite ignored files.
- With local changes, divergent history, a feature branch, or detached HEAD,
  limit automatic synchronization to fetch and a status report. Merge, rebase,
  switching branches, or restoring local changes requires a task that explicitly
  authorizes that operation.
- Start new independent implementation tasks on a `codex/...` branch based on
  freshly fetched `origin/main`, unless continuing a specified branch or PR.
  Use a separate worktree for parallel tasks; reuse a suitable task-owned worktree
  when available. Do not move an existing task or its changes without a request.
- Never switch branches in another active chat's checkout or move a branch
  checked out in another worktree. Do not automatically stash/pop, reset/clean,
  force-push, or delete backups; these operations require explicit authorization.
- Commit, push, open a PR, and merge into remote `main` only when requested.
- Git synchronization alone never authorizes running tests, starting apps or
  devices, reinstalling/resetting apps, or changing test accounts or their progress.

## Test architecture

- Follow the existing Page Object / Screen Object architecture.
- Reuse existing fixtures, helpers, drivers, waits, and test-data utilities.
- Do not duplicate existing functionality.
- Keep tests independent and deterministic.
- Avoid hard-coded sleeps.
- Use the project's existing explicit wait strategy.
- Do not change production application code unless explicitly requested.

## Existing tests

- Never delete existing tests.
- Never disable or skip existing tests to make a run pass.
- Preserve existing test behavior unless the task explicitly changes it.

## iOS / Android

Before adding platform-specific logic:
1. Check whether an abstraction already exists.
2. Prefer shared behavior.
3. Add platform branching only where behavior genuinely differs.

## Test failures

For a failing test, determine whether the likely source is:

- application behavior;
- locator;
- synchronization/wait;
- test data;
- environment;
- driver/Appium;
- platform-specific behavior;
- test implementation.

Do not immediately rewrite the test just because it failed.

## Test execution

Run targeted tests first.

Prefer:
single test
→ affected test class/suite
→ affected feature suite
→ full regression only when justified.

## Reporting

For a failed run, report:

- failed test;
- failure stage;
- relevant error;
- likely cause;
- evidence;
- whether the failure is product/test/environment related;
- recommended next investigation step.
