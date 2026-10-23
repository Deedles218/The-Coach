# PR #25: Android live UI follow-up — 2026-10-23

## 1. Executive summary

**Decision: Insufficient evidence; keep Draft, do not merge main.**

Dedicated Android UI execution found and corrected two test-infrastructure issues:
an unrecognized email-not-found response and an unhandled native full-screen hint.
The measured Bookmarks attempts stopped during account preparation, before any
Bookmarks assertion. They are not passing Bookmarks coverage. No production app
code, purchase, entitlement change, lesson completion or progress reset was used.

## 2. Scope and evidence

- Branch: `codex/android-bookmarks-runner`, PR #25; initial head `7257cb8`.
- Appium 2.19.0, UiAutomator2 4.2.9, Android 16/API 36 ARM64, explicit serial
  `emulator-5580`, isolated Appium endpoint `http://127.0.0.1:4755/`.
- Created AVDs `TheCoach_PR25_API36` (Google APIs) and
  `TheCoach_PR25_Play_API36` (Google Play Store). The first was stopped before
  the second reused the serial. No existing AVD was wiped or replaced.
- Male APK 1.43.3 and For Her APK 2.18.2, both versionCode 352.
- After user approval, imported the existing iOS Bookmarks role references into
  local Keychain for Android. Only male CONTENT/BASELINE and female CONTENT were
  attempted. No primary regression/Kegel or restricted Premium identity was
  imported into those roles. Credentials remain outside the repository.
- Local evidence root: `target/android-live-pr25.tYnqTH/`. Screenshots, XML,
  per-attempt logs, Allure results and local diagnostic helpers are not committed.

### Executed checks

| Check | Actual result / evidence relative to the local evidence root |
| --- | --- |
| `CoachAuthorizationTests#testStartNowOpensQuestionnaireWithoutLogin` | Passed, 1 JUnit case; `start.log` |
| `CoachAuthorizationTests#test01LoginEntryAndEmptyEmailContinueDisabled` | Passed, 1 JUnit case; `empty-email.log` |
| Real parity runner selecting `test02InvalidEmailKeepsContinueDisabled` | Passed, 1 JUnit case, runner exit 0; `runner-invalid-email.log` |
| Local unregistered-email probe after locator correction | Passed; recognized the exact rejection instead of timing out on OTP; `unknown-email-fixed/` and `.log` (not a JUnit case) |
| Male `AndroidBookmarksTests#bookmarksOpenFromTodayAndExplore` | Failed before Bookmarks, `PostAuthQuestionnaireException`; `male-entry-initial/` |
| Female `AndroidBookmarksTests#bookmarksOpenFromTodayAndExplore` | Failed before Bookmarks, same exception; `female-entry-initial/` |
| Male `AndroidBookmarksTests#emptyListNeverShowsTooltip` | Error before Bookmarks: post-OTP readiness timeout, screenshot/tree stayed on security-code loading; `male-empty-initial/` |
| Strict auth diagnostic on the non-Premium returning-user fixture | Rejected post-auth questionnaire even with setup opt-in present; `readonly-strict.log`; not a passing existing-progress login |
| New-user onboarding probes | Failures retained: return-from-login entry transition, action-18 WebView without accessible actions, then System UI full-screen hint; `new-user/`, `new-user-clean/`, `new-user-skill/` |
| New-user follow-up after hint fix | Native hint no longer blocked the run; OS confirmation became `confirmed`, then blank paywall did not dismiss; `new-user-fixed/` |
| Play Store AVD follow-up | Failed at questionnaire action 2: the question/answers are visible in the screenshot but absent from the native accessibility tree; `play-new-user-fixed/`. Paywall not reached, so its behavior on this AVD is unverified |
| Login-error red/green unit check | New positive test failed against old selector; corrected group: 7 passed; `login-error-red.log`, `login-error-green.log` |
| Native-hint unit group | 3 passed; `hint-unit.log` |
| `mvn -q '-Dtest=*UnitTests' test` | 160 passed, 0 failures/errors/skips; `final-all-unit.log` |
| `python3 -B -m unittest discover -s scripts -p 'test_android*.py'` | 37 passed; `final-python-tests.log` |
| Android locator/auth/hint unit classes with For Her package in a separate JVM | 21 passed; `female-unit.log` (subset/repeated execution, not additional unique tests) |
| iOS preservation and `git diff --check` | All 21 original iOS test bodies remain identical to main; no changes to iOS POs, lifecycle/data or Maven dependencies |

The three successful JUnit UI cases are distinct. Local diagnostic probes and
targeted/repeated unit groups are not additional passing regression cases.

Commands used for the JUnit cases were `mvn -q test -Dtest=<class>#<method>` with
the explicit Android serial/package/version/Appium URL above. The real runner
used `--suite auth --method test02InvalidEmailKeepsContinueDisabled`, an explicit
Keychain fixture service and `--post-auth-goal`; it did not enable questionnaire
preparation or use `--confirm-immersive-mode`. Secrets were child-environment only.

## 3. Assumptions and limitations

Fact: iOS-ready role accounts did not provide ready Android existing-progress
fixtures. A read-only identity-verified `/api/v1/user/` response corroborated
`should_show_goals=true` for male CONTENT. No endpoint selecting a program/day or
writing account progress was used. A complete before/after progress snapshot was
not available, so the shared accounts' questionnaires were not completed as a
workaround. All new-user questionnaire choices were confined to fresh anonymous
accounts on the dedicated Android emulator, not the shared iOS roles.

iOS devices/simulators were not operated. The primary 8% baseline was not logged
into or used for completion, selection, reset or fixture preparation. This run
does not independently remeasure that baseline on iOS. New Android-only alias
metadata was stored privately, but successful registration/re-login was not
established; it must not be advertised as a prepared fixture.

The blank native paywall and incomplete native accessibility trees are observations,
not proof of a particular backend or billing defect. In particular, the Play Store
AVD screenshot visibly contains questionnaire answers which the native tree does
not expose. The additional AVD does not establish that missing Play Store caused
the earlier paywall failure. No blind retry loop, timeout increase, weakened oracle,
new skip or authentication bypass was added.

## 4. Overall assessment

| Area | Score | Evidence / boundary |
| --- | ---: | --- |
| Requirements quality | 2/5 | Android no-subscription candidate still needs confirmation |
| Risk coverage | 2/5 | Login entry verified; feature scenarios blocked before Bookmarks |
| Test design | 3/5 | Existing independent roles/restoration retained; prerequisites remain visible |
| Functional coverage | 2/5 | 3 successful auth UI cases, no successful Bookmarks UI case |
| Non-functional coverage | N/A | No performance/security/accessibility audit; accessibility used for diagnosis only |
| Automation architecture | 3/5 | Existing Page Objects and explicit waits, Android-only fixes |
| Reliability/flakiness | 2/5 | OTP loading and onboarding transitions remain unresolved |
| Diagnostics | 3/5 | Fresh per-attempt XML/screenshots/logs; failures retained |
| Maintainability | 3/5 | Stable native ids; 5 added negative/positive unit methods |
| CI/CD | N/A | Local execution, not a CI run |
| Release readiness | 1/5 | Required fixture-backed feature gates are not met |

## 5. Coverage map

| Risk / case | Evidence | Gap |
| --- | --- | --- |
| Welcome/Login controls | Three successful JUnit UI cases | Successful returning-user login still needed |
| Email rejection diagnostics | Red/green unit and successful live rejection probe | No broad email-validation matrix claimed |
| System full-screen overlay | Captured native ids, dismissal follow-up and 3 unit tests | Whole onboarding is not green |
| COA-9647 / COA-9648 | Three attempted Bookmarks JUnit executions | Blocked at authentication, not Bookmarks assertions |
| Other Bookmarks, completion, analytics, recurrence and update | Existing source/offline evidence only | Not executed in this continuation |

## 6. Findings

| ID | Category | Severity | Priority | Confidence | Evidence / impact | Next action / owner / effort |
| --- | --- | --- | --- | --- | --- | --- |
| A-01 | Test locator, corrected | Medium | P2 | High | `guard.log`: app showed `tvEmailNotfoundError`, old test waited for OTP | Android-only selector + two unit methods; live rejection check passed / automation / S |
| A-02 | Device overlay, corrected | Medium | P2 | High | `new-user-skill/...xml`: `com.android.systemui:id/immersive_cling_title` and `ok` | Title-gated semantic dismissal, no generic OK tap; three unit methods / automation / S |
| A-03 | Test data / platform onboarding | High | P1 | High | Male/female CONTENT login throws `PostAuthQuestionnaireException`; male server flag corroborated | Supply Android-ready roles or a verified non-destructive preparation/restore workflow / QA + backend / M |
| A-04 | Auth readiness, unresolved | Medium | P1 | Medium | `male-empty-initial/`: OTP spinner and readiness timeout | Correlate app/backend auth logs; do not bypass or retry into green / app + backend QA / M |
| A-05 | WebView/driver accessibility integration, unresolved | High | P1 | High for observation; cause unconfirmed | `play-new-user-fixed/`: screenshot has the question/answers, native XML does not expose them | Investigate WebView accessibility/UiAutomator2 export before changing selectors or adding another interaction path / Android + automation / M |
| A-06 | Contract gap, unchanged | Medium | P1 | High | Android COA-9667 override remains a candidate, not an approved cross-platform oracle | Confirm Android contract before merge / Product + QA / S |
| A-07 | Native paywall/application or environment, unresolved | High | P1 | Medium | `new-user-fixed/`: `clPaywallContainer` with empty content, visible enabled close button, unchanged after click | Correlate paywall configuration/loading/close handler; Play Store AVD did not reach this stage / Android + QA / M |

## 7. Missing and weak tests

Missing evidence: successful strict Android login after independent re-login,
fixture baseline verification, live Bookmarks open/save/remove/resume/grouping on
both variants, and the separately isolated completion/recurrence/update/evidence
scenarios. Do not execute completion on shared roles just to increase coverage.

## 8. Test architecture assessment

Fixes are confined to Android Page Objects and unit tests. The runner manifest,
default opt-in safeguards, shared reversible Bookmarks behavior and all original
iOS test bodies are preserved. A configured setup flag still cannot silently turn
strict regression login into onboarding preparation. Local probes are diagnostic
tools, not replacements for the committed JUnit suite or evidence collectors.

## 9. Release recommendation

**Insufficient evidence.** Keep PR #25 Draft. Reconsider only after Android-ready
fixtures, successful affected UI cases, resolved onboarding/auth blockers and
the no-subscription contract decision. Unit success is not proof of device parity.

## 10. Prioritized action plan

| Horizon | Action | Owner | Exit criterion |
| --- | --- | --- | --- |
| Immediate | Preserve current failures and publish the scoped fixes/report | Automation | PR #25 updated, main and PR #24 unchanged |
| Short-term | Prepare Android roles without resetting shared iOS progress | QA + backend | Baseline recorded; independent strict login opens Today |
| Short-term | Diagnose OTP and blank-paywall behavior | Android + backend QA | Fresh captures show a reliable authorized/onboarded destination |
| Medium-term | Run targeted Bookmarks, then the affected feature selection | Automation + QA | Measured assertions and restoration pass on both variants |
| Long-term | Establish immutable completion fixtures and CI evidence gates | QA infrastructure | Reproducible isolated runs; no shared-progress contamination |
