# Jira/Xray automation handoff

This handoff is based on the local Xray snapshot in `xray-export/`. The
Atlassian connector currently reports that the workspace is not connected, so
live Jira status, current issue fields and remote links were not changed.
Before a Jira update, re-read each issue from `the-coach.atlassian.net` and
preserve the current product owner decisions below.

## Atlassian connection prerequisite

The Codex Atlassian/Rovo connector must be connected to the Atlassian account
that can access `the-coach.atlassian.net`. The user needs to sign in to
Atlassian, authorize the site/workspace for the connector and complete any
workspace-admin approval for the remote integration. Read access is enough to
re-analyze cases; edit access is needed only when Jira/Xray fields or comments
should be updated. After connection, verify that the connector can list the
accessible cloud resource before attempting any write.

## Automation decision

| Jira case | Automation decision | Smoke mapping / note |
|---|---|---|
| COA-7914 | `needs_clarification` | Clean-install launch. The build artifact, exact onboarding IDs, Firebase environment and expected post-onboarding state must be fixed before execution. |
| COA-7954 | `ready_for_automation` after Jira step expectations are filled | Welcome → Login entry; empty-state assertion is in `SmokeTests`. |
| COA-7948 | `ready_for_automation` after Jira step expectations are filled | Empty email → Continue disabled; P0. |
| COA-7950 | `ready_for_automation` after Jira step expectations are filled | Invalid email → Continue remains disabled; authorization regression. |
| COA-7949 / COA-7951 | `needs_clarification` | Valid email/OTP and Cancel are separate outcomes in the export; define the approved OTP source and whether the email is an existing-progress or no-progress fixture. |
| COA-7952 | `ready_for_automation` after Jira step expectations are filled | Resend code on OTP screen; keep outside the minimal P0 if runtime cost is material. |
| COA-7935 | `needs_clarification` | Existing-progress login on a new device; test data is now secret-backed, but deterministic account state and OTP delivery still need an owner. |
| COA-7956 | `ready_for_automation` after fixture confirmation | Today shows the current date/day switcher. |
| COA-7966 | `needs_clarification` | Daily Lessons must be present in the deterministic fixture and its open/return expected result must be written per step. |
| COA-7967 | `ready_for_automation` after fixture confirmation | Daily Practice card is visible; opening the player/start screen is covered separately. |
| COA-8075 / COA-8087 | `ready_for_automation` after fixture confirmation | Kegel Start Workout → default stretching video → `LET’S GO!` completion popup → player. |
| COA-8132 / COA-8163 / COA-8138 | `ready_for_automation` after fixture confirmation | Pause/play, independent sound/vibration and safe exit are one P0 control flow. |
| COA-7205 | `needs_clarification` | Split into Profile content, Account Settings, Support, FAQ/Terms, My Workbook and Logout cases. Do not automate the overloaded issue as one test. |
| COA-8500 | `ready_for_automation` after Jira step expectations are filled | Logout → Welcome; P0. |
| COA-8502 | `ready_for_automation` after destructive-action wording is confirmed | Verify Delete Account confirmation and cancel only; never confirm deletion. |
| COA-8503 | `ready_for_automation` after Jira step expectations are filled | Profile access from current main tabs; P0 checks primary content only. Delete Account remains outside Smoke. |
| COA-7930 / COA-7931 / COA-8182 / COA-8184 | `needs_clarification` | Existing expectations describe retired Explore structure. Replace with the current three-tab/Explore contract; current UI is the source of truth. |
| COA-7918 | `separate_suite` | Clean install + in-app push prompt + iOS OS permission. External scheduled delivery remains a device/provider integration check. |
| COA-872 / COA-755 | `separate_suite` | StoreKit/Sandbox purchase and Restore Purchases; explicit opt-in because state and transactions are external. |
| COA-8174 | `release_smoke` | Explore → Custom Kegel. |
| COA-8870 | `release_smoke` | My Workbook → PDF paywall for a user without PDF entitlement → close. |

## Required Jira/Xray edits

For every case promoted to `ready_for_automation`, fill the following fields
before handing it to development:

1. Each step has its own expected result. If a step says “open Explore”, the
   expected result is “Explore is selected and its current content is visible”.
2. Preconditions identify platform, build, bundle ID, device, Firebase
   environment, feature-flag snapshot, subscription state and clean-install
   mode.
3. Test data names a fixture, not a real email/OTP. Credentials are retrieved
   from CI secret storage.
4. Cleanup states whether the test logs out, removes the app, restores the
   fixture or leaves StoreKit/permission state for a dedicated suite.
5. The issue description or Xray metadata links to the automation class and
   suite: `tests.SmokeTests`, `tests.ReleaseSmokeTests`,
   `tests.PushPermissionTests` or `tests.StoreKitPurchaseTests`.

## Explicit blockers

- Atlassian workspace connection is required for a live status/field update.
- iOS build artifact, target device/UDID and Appium server are not available
  in the current shell session.
- Firebase flags were not modified; their authenticated read-only snapshot is
  still required.
- The repository has no fixture seed/reset API, so Daily Lessons/Kegel/PDF
  entitlement states must be supplied by the environment owner.
- App-side accessibility IDs must be shipped for `profile_button`,
  `shop_screen`, `push_permission_allow` and the subscription controls listed
  in `docs/smoke-automation-matrix.md`.
