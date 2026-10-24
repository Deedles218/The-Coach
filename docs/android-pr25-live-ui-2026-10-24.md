# PR #25: Android authorization/onboarding continuation — 2026-10-24

> Historical run. The user subsequently clarified that two native goal questions
> are expected after Android login. The classification of `should_show_goals=true`
> and any `tvAnswerNum` as a fixture/server blocker below is superseded by the
> [two-goal login correction and fresh checks](android-pr25-login-goals-2026-10-24.md).
> Original execution failures and counts remain unchanged as historical evidence.

## 1. Executive summary

**Decision: Insufficient evidence; keep Draft, do not merge main.**
Two distinct existing male Bookmarks methods passed on an explicitly prepared,
identity-verified current session. Navigation also passed with normal preparation
and verified logout: three successful JUnit executions, two distinct methods.
This is useful feature evidence, not proof of
strict fresh login or full Android parity. The server questionnaire flag and the
blank-paywall observation remain unresolved; production code was not changed.

## 2. Scope and evidence

- Branch `codex/android-bookmarks-runner`, starting head `b29522e`; PR #25.
- Owned API-36 Android AVDs only, serial `emulator-5580`, Appium 2.19.0 on
  port 4755, UiAutomator2 4.2.9, male APK 1.43.3 / versionCode 352.
- Play Store AVD was stopped before reusing the serial for the rootable
  `TheCoach_PR25_API36` Google APIs userdebug AVD. No other AVD was erased.
- Approved shared male Bookmarks CONTENT role only for paid fixture preparation
  and feature checks; the primary iOS 8% / regression / Kegel identities were not used.
- Local artifacts: `target/android-pr25-auth-followup.0q5jPZ/`, with private
  probes, per-attempt logs, XML/screenshots and Allure results. These are not committed.
- Corrected the private Android CONTENT `onboardingGoal` metadata to
  `Improve sex skills`, matching its known original program. Credentials and
  server state were not changed by this local Keychain metadata correction.

| Check | Actual result / local artifact |
| --- | --- |
| Existing strict auth guard after a fresh app-local installation | Reached post-auth questionnaire and rejected it as intended; `content-auth-fresh.log`. Not a successful existing-progress login |
| Explicit paid CONTENT onboarding continuation | Reached native intro/Today without purchase; `content-intro-finish.log`. Earlier permission-window failure retained in `content-post-auth.log` |
| Profile identity and UI baseline | Exact configured email verified without printing it; `Overall Health / Stage 1 of 2 / 6%` on Today; each current-session attempt has `identity.log` |
| First current-session Bookmarks entry | Error at `My Bookmarks did not open`; XML exposes only `balloon_text`, screenshot shows the real saved list; `bookmarks-current-navigation-fixed/` |
| Separate tooltip setup | Existing blank-area dismissal helper, exact saved card and Profile verified; `content-tooltip-preparation.log` |
| `bookmarksOpenFromTodayAndExplore` after setup | Passed, 1 JUnit case, 0 failures/errors/skips; `bookmarks-current-navigation-prepared/` |
| `cancelKeepsSelectedBookmark` | Passed, 1 JUnit case, 0 failures/errors/skips; `bookmarks-current-cancel/`. Both cards retained, saved lesson opens as bookmarked; no FINISH |
| Normal Bookmarks preparation and cleanup | `bookmarksOpenFromTodayAndExplore` passed without current-account mode, 1 JUnit case, including exact Profile identity, close-Profile/Today transition and normal logout; `bookmarks-normal-preparation/`. Existing authenticated session, not fresh OTP login |
| Fresh `CoachAuthorizationTests#test05ExistingProgressUserLoginWithEmailAndOtp` | Failed, 1 JUnit case: `PostAuthQuestionnaireException`; `strict-relogin/`. No new OTP-spinner timeout in this attempt, but returning-user login is not green |
| Questionnaire cleanup recovery | Allure records initial `state=onboarding`, then `Android cleanup recovered after one reinstall`, `state=welcome`; the original auth failure remains failed, no suppressed cleanup failure |
| Server state around feature attempts | `content-live-before/after.json`, `content-cancel-before/after.json`: same verified identity, entitlement, current program/day and normalized current-program status/progress fields |
| Final server state after normal logout and strict-login cleanup | `content-final-before.json`, `content-after-normal-cleanup.json`, `content-final-after.json`: unchanged program/day, entitlement and normalized status/progress. Identity-verified requests use the original token held only in process memory, not a new login to repair the result |
| Profile navigation red/green | 2 methods, 1 expected red against old code; corrected affected group: 30 passed; `bookmarks-profile-red.log`, `bookmarks-profile-green.log` |
| All Java unit tests | `mvn -q '-Dtest=*UnitTests' test`: 169 passed, 0 failures/errors/skips; `final-all-unit.log` |
| Android Python tests | `python3 -B -m unittest discover -s scripts -p 'test_android*.py'`: 37 passed; `final-python-tests.log` |
| For Her package offline subset | Five affected unit classes, 30 passed in a separate JVM; `female-unit.log`. No female live feature pass claimed |

Repeated attempts/subsets and local probes are not additional distinct passing
regression cases. The measured JUnit commands use exact `class#method`, the explicit
Android package/serial/Appium URL and secret-backed child environments. The two
current-session successes use the existing `coach.bookmarks.useCurrentAccount=true`.
The five measured JUnit executions in this continuation contain three passes,
one Bookmarks-entry error (popup) and one strict-login failure (questionnaire),
with zero skips. The preliminary identity-probe failure is not a JUnit execution.

## 3. Assumptions, state safety and limits

Catalog GETs for different programs unexpectedly changed the selected server
program during attempted baseline collection. The original known selection was
`sex_skill_man / day 2`; it was explicitly restored and independently reread before
feature execution. Catalog sweeps were removed from the local snapshot helper.
No completion/reset endpoint or FINISH action was used. This incident is retained,
not reclassified as a harmless read. The all-program baseline failed and is not
claimed as verified. Current-program comparisons exclude the per-response random
`rating_page` feedback configuration and retain lesson identities/status/progress.

The Android UI's `Overall Health / Stage 1 / 6%` differs from the server's restored
`Sex Is a Skill / day 2`. This is not the protected primary 8% baseline and is not
evidence of its regression. Cache/selection consistency needs investigation before
day/program-dependent scenarios. iOS was not operated or independently remeasured.

## 4. Overall assessment

| Area | Score | Boundary |
| --- | ---: | --- |
| Requirements / risk coverage | 2/5 | Two non-completing male feature checks, no full parity |
| Test design / architecture | 3/5 | Existing Page Objects, explicit waits, strict identity and failure propagation |
| Functional coverage | 2/5 | Navigation and cancellation measured; write/completion/recurrence cases not run |
| Non-functional / CI | N/A | Local functional diagnostics, not a security/accessibility/performance or CI audit |
| Reliability | 2/5 | Fresh auth and native accessibility prerequisites remain |
| Diagnostics / maintainability | 3/5 | Red/green tests, fresh artifacts, explicit setup/result boundaries |
| Release readiness | 1/5 | Required fixture-backed gates remain open |

## 5. Coverage and findings

| Finding | Category / priority / confidence | Evidence and action |
| --- | --- | --- |
| Profile hides Today tab | Test navigation, corrected / P1 / high | Close verified Profile before Today in Android Bookmarks; two new unit checks, including identity-failure propagation |
| Back from questionnaire is not verified logout | Test isolation, corrected / P1 / high | Reject pending `tvAnswerNum` cleanup; existing bounded APK recovery remains responsible; two new unit checks |
| Notification window blocks readiness after relaunch | Synchronization/platform, corrected / P1 / high | Wait accepts observed OS dialogs, then existing handlers still require real Today; one new unit check |
| WebView native tree omits visible controls | Environment/driver integration, partial diagnostic workaround / P1 / high observation, uncertain cause | Real SelectToSpeak plus optional service-preservation capability helped one transition but later failed; not a general fix |
| Fresh install's Bookmarks popup hides underlying tree | Fixture/app-local state / P2 / high | Multi-window setting did not expose underlying title/cards; separate existing UI dismissal prepared CONTENT, unchanged navigation test then passed |
| Goals flag persists | Fixture/server onboarding / P1 / high observation, uncertain cause | Authenticated user response still returns `should_show_goals=true` after explicit preparation; do not consume it silently in strict regression |
| Empty native paywall / close no-op | App/environment / P1 / uncertain root cause | Rootable fresh-user flow reached paywall; semantic clickGesture and element click did not dismiss it. No purchase/restore/back bypass |
| Catalog GET selects program | State safety / P1 / high | Restore known original selection; do not use catalog enumeration for baseline collection |

## 6. Missing or weak evidence

Independent strict returning-user login, stable program/day restoration in UI,
female feature parity, empty-list/tooltip-first fixtures, save/delete/resume/grouping,
completion/recurrence, fresh Android analytics/network collection and genuine
upgrade/reinstall remain separate gates. Payment cases are excluded by user request.

## 7. Environment diagnosis and reproducibility

The optional `android.disableSuppressAccessibilityService` capability leaves the
driver default unchanged when absent and is never added to iOS (four new unit tests).
It does not install/enable an accessibility service. For the successful prepared
session, the owned **userdebug** AVD additionally used a temporary
`/data/local/tmp/webview-command-line` containing
`_ --force-renderer-accessibility=complete`, followed by target-app restart.
This is an environment diagnostic, not a production app patch or default-device pass.
Chromium documents the [accessibility switch](https://github.com/chromium/chromium/blob/main/docs/accessibility/overview.md)
and [WebView userdebug flag-file mechanism](https://github.com/chromium/chromium/blob/main/android_webview/docs/commandline-flags.md).
It must be limited to an explicitly owned rootable test emulator, preserving any
prior file and restoring it/its absence after the run. Do not use on a personal device.
At completion, the original flag-file absence was restored and verified on the
owned Google APIs AVD. The temporary Play AVD accessibility settings had already
been restored before that AVD was stopped. No temporary service or WebView override
is required/enabled by default in the committed runner. The owned Appium process
was stopped after verifying zero active sessions; the separate test emulator
remains booted, with the selected test application stopped after Welcome recovery.

## 8. Architecture assessment

No existing test was removed, disabled or skipped, and assertions were not weakened.
No new sleeps, production code or dependencies. iOS Page Objects remain unchanged;
all 21 original shared iOS @Test bodies are retained. The shared runner manifest and
strict-login/setup separation remain intact. Local current-account probes are not
substitutes for normal committed regression execution.
The scoped source/doc patch passes `git diff --check`. Generated tracked `target/`
reports remain unstaged; they are not source changes or PR content.

## 9. Recommendation

Keep PR #25 Draft. Successful prepared-session checks are not sufficient to approve
merge while fresh-login, selection consistency and remaining feature gates are open.
PR #24 and main must remain unchanged.

## 10. Prioritized next actions

1. Correlate persistent goals flag and selected-program mismatch with Android/backend
   account state; obtain Android-ready roles without resetting shared iOS progress.
2. Resolve WebView accessibility and blank-paywall behavior on the normal test image.
3. Run the affected non-completing Bookmarks selection after strict login works,
   then separately provision immutable completion/update/evidence fixtures.
4. Confirm the Android no-subscription contract before considering merge.
