# Mobile automation project rules

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
