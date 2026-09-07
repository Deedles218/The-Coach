# The Coach: smoke automation contract

## P0 smoke

The fast smoke suite validates the launch/authentication path and the critical
Today/Profile/Kegel user journeys. The current app navigation has exactly
three main tabs, but Explore and Shop are release-smoke surfaces; `Feed` is
not a smoke surface.

| Priority | Case | Automated assertion |
|---|---|---|
| BLOCKER | Welcome / Login validation | Welcome → Login opens; empty email leaves Continue disabled and the disabled tap is a no-op. |
| BLOCKER | Authorization | Existing-progress email + OTP opens the authorized Daily Plan entry point. Credentials are secret-backed. |
| BLOCKER | Today / Daily Plan | Deterministic day 1 fixture shows the current day switcher, Daily Lessons and Daily Practice. |
| BLOCKER | Kegel player | Start opens the default stretching video, waits for and closes the completion popup with `LET’S GO!`, then verifies the Kegel player controls, pause/play, sound/vibration and safe exit. |
| BLOCKER | Profile / Logout | Profile opens from an accessibility locator and displays primary progress/settings content; Logout returns to Welcome. Coordinate tapping and Delete Account are excluded. |
| BLOCKER | Clean install / in-app push prompt | A real clean install completes configured onboarding, displays the in-app push prompt, and leaves the authorized Daily Plan underneath it. OS permission acceptance and scheduled delivery stay in the separate push suite. |

## P1 release smoke

`suites.ReleaseSmokeSuite` covers the current three-tab navigation (`Today`,
`Explore`, `Shop`), current Explore Recommended/Custom Kegel, the My Workbook
PDF paywall for a user without PDF entitlement, and an update that preserves
authorization and progress. Legacy Explore expectations are not used to reject
the current UI.

The paywall case is currently blocked and ignored until the dedicated no-PDF
account is provisioned. The update test is configuration-gated by two
same-bundle app artifacts; it is not part of the fast P0 suite.

The executable smoke entry point is `suites.SmokeSuite`.

## Separate suites

### COA-7918: Push permission

`suites.PushPermissionSuite` is intentionally outside ordinary smoke because
the iOS permission is one-time OS state. It requires:

```text
-Dios.fullReset=true
-Dios.noReset=false
-Dios.app=/path/to/The-Coach.app
-Dios.onboarding.steps=id:<step1>,id:<step2>,...
```

The test verifies the in-app push permission screen, the iOS system dialog,
and the reinstall path when `-Dios.app=/path/to/The-Coach.app` is supplied.
Push delivery according to an external schedule cannot be proven by a local
Appium UI session; that part needs a notification-provider/device integration
check.

### COA-872 / COA-755: StoreKit

`suites.StoreKitPurchaseSuite` is a StoreKit/Sandbox integration suite and is
never part of ordinary smoke. It requires explicit opt-in because it can create
a sandbox transaction:

```text
-Dstorekit.sandbox=true
-Dstorekit.allowPurchases=true
-Dios.onboarding.steps=id:<step1>,id:<step2>,...
```

The suite covers the selected-period free-trial purchase and `RESTORE
PURCHASES`. A restore is accepted when it restores premium access or returns an
explicit Sandbox result such as an expired subscription.

## App-side accessibility/test ID contract

The application source is not part of this repository, so the automation
cannot add identifiers to UIKit/SwiftUI views directly. The following IDs are
the required app-side contract. The test code tries these IDs first and keeps
only stable, non-coordinate fallbacks where those names already exist. Shop
content and the in-app push allow action are strict IDs: a tab or a close
button must not be mistaken for a successful content/permission action.

| Surface | Required ID |
|---|---|
| Today tab | `tab_today` |
| Selected Today tab | `tab_today_selected` |
| Explore tab | `tab_explore` |
| Selected Explore tab | `tab_explore_selected` |
| Shop tab | `tab_shop` |
| Selected Shop tab | `tab_shop_selected` |
| Profile button | `profile_button` |
| Profile screen root | `profile_screen` |
| Profile Account Settings | `profile_account_settings` |
| Profile Support | `profile_support` |
| Profile My Workbook | `profile_my_workbook` |
| Profile FAQ | `profile_faq` |
| Profile Terms | `profile_terms` |
| Profile Logout | `profile_logout` |
| Profile Logout confirmation | `profile_logout_confirm` |
| Profile browser close | `profile_browser_close` |
| Welcome screen root | `start_screen` |
| Welcome Start | `start_button` |
| Welcome Login | `login_button` |
| Login screen root | `login_screen` |
| Login email input | `login_email` |
| Login Continue | `login_continue` |
| OTP screen root | `otp_screen` |
| OTP Resend | `otp_resend` |
| Existing-account Login step | `auth_existing_account_login` |
| Send security code action | `auth_send_security_code` |
| Invalid-email validation error | `login_email_error` |
| Push prompt close | `push_permission_close` |
| Connect email Later | `connect_email_later` |
| Loading indicator | `loading_indicator` |
| Shop screen content root | `shop_screen` |
| In-app push allow action | `push_permission_allow` |
| Daily Plan current day | `daily_plan_current_day` |
| Daily Plan program selector | `daily_plan_program_selector` |
| Program selector modal | `program_selector_modal` |
| Program selector item | `program_selector_item` |
| Program selector close | `program_selector_close` |
| Daily Lessons section | `daily_lessons` |
| Daily Practice section | `daily_practice` |
| Explore program item | `explore_program_item` |
| Daily Plan TO CATCH-UP section | `daily_plan_catch_up` |
| Daily Plan catch-up card | `daily_plan_catch_up_card` |
| Postponed clock icon | `daily_plan_postponed_icon` |
| Single customization task | `daily_plan_customization_task` |
| Program progress value | `program_progress_percent` |
| Legacy postpone tooltip | `legacy_postpone_tooltip` |
| Locked next-day postpone popup GIF | `locked_next_day_popup_gif` |
| Overall Health program entry | `overall_health_program` |
| Program Settings action | `program_settings` |
| Program Settings screen | `program_settings_screen` |
| Removed exercises entry | `removed_exercises` |
| Removed exercises screen | `removed_exercises_screen` |
| Removed exercise card | `removed_exercise_card` |
| Removed exercise checkbox | `removed_exercise_checkbox` |
| Recover removed exercise | `recover_removed_exercise` |
| Program day container | `program_day_container` |
| Recovered exercise card | `recovered_exercise_card` |
| Daily Kegel card | `daily_practice_kegel` |
| Kegel start screen | `kegel_start_screen` |
| Kegel start | `kegel_start_workout` |
| Kegel close | `kegel_close` |
| Stretching completion action | `kegel_stretching_lets_go` |
| Kegel player back | `kegel_player_back` |
| Kegel player pause/play | `kegel_player_pause`, `kegel_player_play` |
| Kegel player sound state | `kegel_player_sound_on`, `kegel_player_sound_off` |
| Kegel player vibration state | `kegel_player_vibration_on`, `kegel_player_vibration_off` |
| Kegel player info | `kegel_player_info`, `kegel_player_info_close` |
| Kegel player exit | `kegel_player_exit_quit`, `kegel_player_exit_continue` |
| Subscription paywall root | `subscription_paywall` |
| Start free trial | `subscription_start_free_trial` |
| Selected subscription period | `subscription_selected_plan` |
| Restore purchases | `subscription_restore_purchases` |
| Successful restore result | `subscription_restored` |
| Onboarding actions | IDs passed through `ios.onboarding.steps` / `IOS_ONBOARDING_STEPS` |

Avoid replacing these IDs with coordinate taps or long XPath chains. If a
Firebase onboarding/paywall variant changes the number of questionnaire steps,
update only the configured accessibility-id sequence; do not change the smoke
assertions.

The reviewed test-model suite is implemented in
`tests.TestModelAutomationTests` and runs through
`suites.TestModelAutomationSuite`. The mutation scenarios use a separate
resettable fixture. Configure the required non-secret fixture values and run
`./scripts/validate_test_model_data.sh` before the full suite. For an explicit
local subset use `scripts/run_test_model_local.py`; see the dated fixture report.
The COA-8235 and COA-8517 locators have now been verified against the Simulator
accessibility tree on build 1.13.29. The old proposed customization `test_id`
values for COA-8511/8512 must not be treated as verified app identifiers.
The diagnostic COA-8511 fixture confirmed `ItemMovedForward` on Today and
`TitleBlock.Title` / label `TO CATCH-UP` in Explore; these now replace the
corresponding iOS placeholders. The exact catch-up card and sole-remaining-task
assertions on Today still require an entitled fixture and verification.

COA-8518 is implemented against its detailed expected result: the first tap
on a locked next-day arrow shows the new popup and leaves the current day
unchanged. The current Xray export has no contradictory expectation that the
locked day opens. COA-8356 explicitly distinguishes the first popup from the
legacy tooltip on subsequent taps. On 2026-09-06 the dedicated account received
a paywall and the active popup feature flag was off, so the popup/analytics
scenario has not been verified successfully.

On the local Simulator build `1.13.29` checked on 2026-09-02, COA-7950
exposed a disabled `CONTINUE` control but no validation-error node after
`not-an-email` was entered. The test intentionally keeps the Xray error
assertion, so this remains a product/build discrepancy rather than a relaxed
automation check.

### Inspector validation gate

Appium Inspector is not required to edit or compile the Page Objects. It is
required before the first device run after the app-side change: inspect the
accessibility tree and confirm that each required ID is visible on the intended
screen, maps to the intended control, and exposes the expected `enabled` state.
The current workspace contains the automation project but not the iOS app
source/build, so this gate remains a device/build handoff item.

## Read-only Firebase checks

The relevant launch configuration names from the local Xray export are
`launch_config`, `push_notification_view_config`, `onboarding_config` and
`paywall_v2`. The Firebase Console link supplied for this work must be used
only to read the selected environment. No feature flag or remote configuration
is changed by this project.
