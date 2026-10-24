# Bookmarks automation

`BookmarksTests` contains 21 scenarios linked to Xray cases COA-9647 through COA-9666 and the available base-lesson save check from COA-9667. A linked method alone does not establish complete coverage of every variant in its Xray case; required fixture states and the assertions below must also be verified. Trial variants from COA-9667 and COA-9668 (expired subscription) remain outside this run. The suite is iOS-only because the source cases are labelled iOS and the verified accessibility identifiers come from the iOS build.

The current [COA-9667](https://the-coach.atlassian.net/browse/COA-9667) requires saving and reopening a bookmarkable lesson that is already available to the account, including an available base lesson without a paid subscription. The test verifies the empty initial list, opens that lesson without a paywall, saves it, reopens it from Bookmarks at the saved slide, and removes the created bookmark during cleanup. It does not infer a paywall at the Bookmarks entry point from the absence of a subscription. An unavailable base lesson is a fixture/access blocker, not permission to change account entitlements.

Expired access has a different product contract and is not represented by the no-subscription test: My Bookmarks opens and displays saved cards, but opening a saved lesson is blocked by the subscription paywall. If COA-9668 is automated later, it must assert both stages instead of expecting the Bookmarks entry point itself to be paywalled.

## Runner

Run one case first:

```bash
mvn -Dtest=tests.BookmarksTests#emptyListNeverShowsTooltip \
  -Dplatform=ios \
  -Dios.udid=<UDID> \
  -Dios.deviceName=<device-name> \
  -Dios.bundleId=<bundle-id> \
  test
```

For a complete run, use the regression controller with the four Bookmarks jobs:

```bash
python3 scripts/ios_regression/run_regression.py --config <local-config.json> \
  --only BookmarksTests --only BookmarksTooltipFirstEntry \
  --only BookmarksTooltipDismissal --only BookmarksUpdate
```

These jobs select all 21 methods once per configured variant. The two tooltip jobs use fresh installations; the update job starts with `oldApp` and supplies the current `app` as `ios.update.app`. A single `suites.BookmarksSuite` invocation shares one capability configuration and cannot supply these different installation states. Run both variants sequentially through the controller, or use separate simulators, Appium/WDA ports, output directories, and project checkouts for parallel runs.

For an app-only fresh installation on a dedicated simulator that is already booted, use:

```bash
python3 scripts/prepare_bookmarks_simulator.py --udid <UDID> \
  --bundle-id <bundle-id> --app '<simulator-app-path>'
```

This removes the selected application's local state and installs the unchanged `.app`; server-side bookmarks and completion remain fixture prerequisites. It validates the bundle ID and booted simulator before changing anything. It does not reboot or erase the simulator. Use `-Dios.noReset=true -Dios.fullReset=false` for the following Appium session and ordinary login; repeat preparation before each method when app-local isolation is required. Do not perform this preparation between the update and reinstall phases of COA-9666. This is useful when Appium's simulator reset causes cold-boot startup delays. No other running Appium session may use that simulator while preparation runs.

Bookmarks-only jobs read their eight account roles from the environment, optionally through per-variant `environmentBindings` in the local configuration. They do not require the general regression account registry or its Keychain services. See [ios-full-regression.md](ios-full-regression.md) for controller configuration and resume behavior. Maven failures that produce no fresh test cases stop the queue for investigation.

For local locator work against an already authorized account, add `-Dcoach.bookmarks.useCurrentAccount=true` and run only one compatible method. Multi-account tests reject this mode.

## Secret-backed accounts

The content scenarios reuse seven subscribed account roles per app variant. The access-control scenario uses one additional `NOSUBSCRIPTION` role. Each role uses `COACH_BOOKMARKS_<ROLE>_EMAIL` and `COACH_BOOKMARKS_<ROLE>_OTP` (or matching lowercase `coach.bookmarks.<role>.email/otp` properties). Supply the male or female set that matches `ios.bundleId`; credentials are not stored in the repository.

| Role | Required initial state |
| --- | --- |
| `BASELINE` | Empty list and a saveable lesson; used by empty-state and reversible save/delete checks |
| `CONTENT` | Saved lesson and bookmarkable practice at known non-default slides; grouping additionally requires multiple programs/modules. A second lesson alone cannot satisfy practice scenarios |
| `TOOLTIP` | One saved lesson; tooltip has never been shown |
| `RECURRING` | Repeating content saved from the later day/stage; adjacent-day state is known |
| `SWITCHA` | Exactly one bookmark, different from `SWITCHB` |
| `SWITCHB` | Exactly one bookmark, different from `SWITCHA` |
| `UPDATE` | Exactly one saved lesson; tooltip already consumed; update build configured |
| `NOSUBSCRIPTION` | Account with onboarding completed, an empty Bookmarks list, and no active, trial, lifetime, or expired entitlement; the configured base lesson is available without paid access |

Cases that require a bookmarkable practice (`COA-9649`, `COA-9651`, `COA-9657`, `COA-9663`, `COA-9664`) need an actual Practice card with a bookmark control. Last Longer's Stage 1 cards do not meet this prerequisite. The men's Sex Is a Skill program exposes Pleasure Spot Mapping, a bookmarkable six-slide Practice; it can supply the ordinary practice scenarios. This does not establish the separate adjacent-day recurrence fixture required by COA-9664.

The one `TOOLTIP` account is sufficient for the representative blank-area dismissal in COA-9653. Re-running the tooltip-first scenarios requires resetting the app-local tooltip state or reprovisioning that role; the suite never manufactures extra accounts.

Missing CONTENT/SWITCH bookmarks can be prepared explicitly through the existing UI Page Objects, before the measured regression. After `mvn test-compile dependency:build-classpath -Dmdep.outputFile=target/test-classpath.txt`, run the following with the same iOS capabilities and secret-backed role environment as the tests:

```bash
java -Dplatform=ios -Dcoach.bookmarks.prepareFixtures=true \
  -Dappium.url=<appium-url> -Dios.deviceName=<device-name> -Dios.platformVersion=<version> \
  -Dios.udid=<UDID> -Dios.bundleId=<bundle-id> -Dios.app=<app-path> \
  -Dios.noReset=true -Dios.fullReset=false \
  -cp "target/test-classes:$(cat target/test-classpath.txt)" \
  lib.BookmarksFixturePreparation CONTENT '<second-content-title>' \
  SWITCHA '<first-lesson-title>' SWITCHB '<second-lesson-title>'
```

The utility verifies the logged-in Profile identity, adds only a missing bookmark, and confirms its presence in My Bookmarks. It preserves existing bookmarks and their saved positions, does not complete lessons, and does not repair unexpected extra cards. It is not part of the regression matrix and cannot turn a failed account-switch assertion into a pass by seeding the destination after the measured switch. Completion snapshots, program/module selection, and entitlement provisioning remain separate fixture prerequisites.

Saving/removal scenarios restore the declared saved slide through the UI, including after assertion failures. They do not reset completion. COA-9659 requires a lesson that is not completed and an account snapshot/reset between runs; running it after another case completes that lesson is an invalid fixture. Recurring completion similarly requires a saved later occurrence and an incomplete earlier occurrence. Missing practice, program/module, or adjacent-day fixtures remain failures rather than substituted content or skipped cases.

To isolate the completion transition from other cases that finish CONTENT's lesson, set `COACH_BOOKMARKS_COMPLETION_ACCOUNT_ROLE` / `coach.bookmarks.completionAccountRole` to a separately prepared role. For example, `COMPLETION` reads `COACH_BOOKMARKS_COMPLETION_EMAIL/OTP`; an existing role can be reused only if its initial and final state satisfy both scenarios. The incomplete-lesson precondition remains mandatory. Kegel-only fixtures similarly use `COACH_BOOKMARKS_KEGEL_ACCOUNT_ROLE` / `coach.bookmarks.kegelAccountRole`, for example `KEGEL` with `COACH_BOOKMARKS_KEGEL_EMAIL/OTP`. Each defaults to CONTENT for existing configurations. Set optional `COACH_BOOKMARKS_KEGEL_PROGRAM_TITLE` / `coach.bookmarks.kegelProgramTitle` to select its program after login; otherwise the account must already select the required program. Its configured card titles must identify the intended regular and sport variants; two cards in the new `kegel_workout` format alone do not establish legacy variant coverage.

COA-9659 can use another UI-verified incomplete lesson on the chosen account: set `COACH_BOOKMARKS_COMPLETION_LESSON_TITLE` / `coach.bookmarks.completionLessonTitle` together with `COACH_BOOKMARKS_COMPLETION_LESSON_SAVED_SLIDE_TEXT` / `coach.bookmarks.completionLessonSavedSlideText`. Both are scoped to this completion test; the other Bookmarks cases keep their ordinary lesson fixture. If the chosen lesson requires a final answer, supply its exact visible text through `COACH_BOOKMARKS_COMPLETION_ANSWER` / `coach.bookmarks.completionAnswer`. A custom lesson does not inherit the ordinary lesson's answer. When no custom title is supplied, the existing lesson, saved-slide and answer configuration remains supported. Select the lesson before the run, verify its initial state in the test, and provision a fresh incomplete lesson or restore its snapshot before another completion run; do not retry a completed lesson as if it were incomplete.

For content with a required final question, supply the exact visible answer through `COACH_BOOKMARKS_LESSON_COMPLETION_ANSWER` / `coach.bookmarks.lessonCompletionAnswer`, or `COACH_BOOKMARKS_RECURRING_COMPLETION_ANSWER` / `coach.bookmarks.recurringCompletionAnswer`. The Page Object selects it only when FINISH is disabled, then waits for the button to become enabled. It does not guess an answer or treat a visible disabled button as a missing locator.

For a program without modules, set `COACH_BOOKMARKS_PRACTICE_GROUP_TITLE` / `coach.bookmarks.practiceGroupTitle` to its visible program heading. The grouping check verifies the nearest program/module heading and category preceding the specific card in the observed native list, plus the card composition. A heading or category belonging to another card cannot satisfy it. Without this option, `moduleTitle` remains required. Use the rendered heading text, including capitalization (the current male module headings are uppercase). A pass on Path to Desire, which has no modules, does not establish cross-module coverage of COA-9651. The mixed CONTENT fixture has both Lessons and Practices. The test switches to the separately prepared lesson-only UPDATE role to verify Lessons/card composition and absence of Practices; an arbitrary nonexistent category name cannot satisfy this check. Additional cross-module and practice-only variants still need their own exact fixture sets.

Supply only the values needed by the selected method: `COACH_BOOKMARKS_LESSON_TITLE`, `SECOND_LESSON_TITLE`, optional `SECOND_CONTENT_TITLE` (for two-card removal checks), `PRACTICE_TITLE`, `RECURRING_PRACTICE_TITLE`, first/middle/last or saved slide texts, `PROGRAM_TITLE`, `MODULE_TITLE`, `TOOLTIP_TEXT`, `KEGEL_TITLE`, `KEGEL_SPORT_TITLE`, `LESSON_ID`, `SAVED_SLIDE_NUMBER`, `PROGRAM_ID`, `PROGRAM_DAY`, `SAVED_DAY_TITLE`, and `PREVIOUS_DAY_TITLE`. System properties use the equivalent lower-camel names under `coach.bookmarks.*`. Recurring content can override `recurringLessonId`, `recurringProgramId`, `recurringProgramDay`, `recurringSavedSlideNumber`, and `recurringPracticeSavedSlideText` (or their `COACH_BOOKMARKS_RECURRING_*` environment equivalents), so its expected payload is not confused with the ordinary lesson fixture.

COA-9666 requires `IOS_UPDATE_APP`/`ios.update.app` and intentionally removes then reinstalls the app during its reinstall phase. Simulator `.app` updates validate installed and requested `CFBundleVersion` values and require distinct real build numbers. A placeholder such as `BITRISE_BUILD_NUMBER` blocks update verification; changing bundle metadata locally would not prove an application update.

For lesson and practice fixtures in different programs, set `COACH_BOOKMARKS_LESSON_PROGRAM_TITLE` and `COACH_BOOKMARKS_PRACTICE_PROGRAM_TITLE` (or `coach.bookmarks.lessonProgramTitle` and `practiceProgramTitle`). Every open from Today, including restoration of the saved slide, selects the declared program after login. A custom completion lesson retains its own completion program; ordinary fixture options do not redirect it. `COACH_BOOKMARKS_RECURRING_PROGRAM_TITLE` / `coach.bookmarks.recurringProgramTitle` selects the explicitly prepared program for the original-program/day request check. It does not move the current day or prepare the recurring completion fixture. These options are optional and preserve existing program selection when absent.

For a completion lesson in another program, set `COACH_BOOKMARKS_COMPLETION_PROGRAM_TITLE` / `coach.bookmarks.completionProgramTitle`. The test selects that explicit program after ordinary email/OTP login and Profile identity verification, before checking that the lesson is incomplete. This avoids relying on a program choice made before logout; no program is changed when the option is absent. An ordinary completion run must omit `coach.bookmarks.useCurrentAccount` and the local `BOOKMARKS_KEEP_SESSION` runner override.

The active-program reader prefers the app-side identifier, then reads the observed Today header beside the Bookmarks entry. It supports restored program names outside the old label catalog, including the male/female Sexting programs. The older-build label fallback is restricted to selected Today. Selecting a fixture program uses the existing hittable native-tap helper and still requires the selector to close and the expected program to become active.

iOS login waits for a hittable Welcome control and verifies the email form, rather than accepting a successful click alone. Post-OTP readiness requires rendered Today cards and a Profile control with no visible auth form. Opening Profile waits for populated content before the separate, strict account-identity assertion; persistent tabs or an empty navigation container are insufficient. Entry and Profile use one native tap at the observed control's center, sharing the existing iOS browser-close tap implementation.

When a bookmarked card opens a subscription paywall, `openCard` fails explicitly and attaches the blocking XML and screenshot before cleanup. Detection and dismissal reuse the iOS onboarding paywall handlers, including the close/RESTORE marker on both builds. Dismissal runs only in preparation/cleanup; it does not turn a paywalled open into a successful content assertion. The available-base-lesson case removes its own created bookmark after dismissing the blocker.

## Completion indicator after FINISH

The primary completion check uses the named `dailyCompletedCheckmark*` image inside the exact Daily Plan card. On the current builds, a newly completed card can render a visible tick as an unnamed `XCUIElementTypeOther` until the next login. For this observed state, the Page Object checks the rendered indicator immediately following that card's title against a cropped tick reference. It compares the white glyph's shape independently of the male/female circle color; an unnamed container alone is never evidence of completion. The same check protects the incomplete precondition. Successful visual checks attach the actual indicator crop to Allure, without refreshing the screen, restarting the app, or logging in again.

The reference and negative fixtures under `src/test/resources/bookmarks/completion` contain only status icons cropped from the two builds. Unit tests reject unfinished arrows, blank/solid indicators, a different glyph, and a completed neighboring card. A stable accessibility identifier on the live status indicator remains preferable; if the UI glyph changes, review the visual reference and its negative fixtures before updating it.

## Fresh analytics and request evidence

COA-9662 reads append-only JSONL from `COACH_BOOKMARKS_ANALYTICS_LOG`; COA-9663 reads JSONL from `COACH_BOOKMARKS_NETWORK_LOG`. The tests checkpoint the file before the action and accept only complete records appended afterwards. Each save/delete event must contain the expected lesson and slide in that same event; each POST must contain the expected path, program, day, and slide in that same request.

The read-only Simulator observer extracts matching fresh SDK events and successful cached requests without writing credentials to the output:

```bash
python3 scripts/observe_bookmarks_evidence.py --udid <UDID> \
  --bundle-id <bundle-id> --output <evidence-directory> --timeout 3600
```

Point the two environment variables at `analytics.jsonl` and `network.jsonl` in that directory. Start the observer before the tests; use one observer/output directory per simulator. It follows container changes after reinstallation and filters by account identity, timestamps, host, method, response status, and body shape. A fresh cached `/api/v1/user/` response corroborates the current account against its request JWT and response UID; alternatively, the authenticated Profile `/api/v1/user/achievments/` response corroborates it with the returned email. The most recent anonymous response invalidates the preceding signed-in identity. API `ios_`-prefixed UIDs are checked against the request UID, and SDK events are matched to the Firebase UID without that API prefix. This supports SDK events when the SDK has not persisted `store.user_id`, and request observation does not depend on the SDK database existing. SDK queue polling runs independently of cache parsing. The observer reads local databases and sends no requests or credentials to its output. An optional `--stop-file <path>` ends observation when that file appears.

SDK events are buffered in memory for at most five seconds while the current identity is being corroborated. No record is exported until its UID matches; an expired candidate cannot restart its buffer window. This avoids losing an SDK row that is uploaded before the independent cache reader obtains the identity. The SDK queue is sampled at 1 ms; this is evidence collection, not a retry of a UI action.

Expected record shapes (additional observer metadata is permitted):

```json
{"event":"BookmarksOpened","properties":{}}
{"event":"BookmarkSaved","properties":{"lesson_id":"example_lesson","slide_number":2}}
{"event":"BookmarkDeleted","properties":{"lesson_id":"example_lesson","slide_number":2}}
{"method":"POST","path":"/api/v1/coachprogram/lessons/example_lesson/bookmark","body":{"program_id":"example_program","program_day":2,"slide_number":2}}
```

## Verified local run — 2026-10-22

The current simulator builds (male 1.13.34 and female 2.0.33, iOS 26.5) were checked with ordinary email/OTP login and Profile identity verification. Each measured scenario started with an app-only fresh installation. Dedicated role accounts were reused with their declared bookmark state checked/prepared; these were not newly registered identities. Completion used previously unfinished Mastering Her Orgasm (male) and Erogenous Zones (female), both in Sex Is a Skill.

| Build | Passed | Failed | Blocked before execution |
| --- | ---: | ---: | ---: |
| Male 1.13.34 | 18 | 1 | 2 |
| Female 2.0.33 | 18 | 1 | 2 |

There were 38 distinct executed UI scenarios and 46 measured attempts including corrections and related checks. Recurring completion (COA-9664) remains the accepted data blocker on each variant. Update/reinstall (COA-9666) is blocked on each variant because both supplied old/current pairs contain the literal `CFBundleVersion=BITRISE_BUILD_NUMBER`; bundle metadata was not edited. These four blocked slots are not passing or skipped JUnit executions.

The remaining two failures are COA-9667's available-base-lesson variant: Starting Point (male) and The Basics of Sexual Desire (female) open from Daily Plan and can be saved, but opening their saved cards shows a subscription paywall. The final attempts fail explicitly with `Subscription paywall blocked bookmarked content`, preserve the blocking XML/screenshot, and successfully restore the empty bookmark fixture. Both final attempts contain zero suppressed cleanup errors. Investigate why access to the same lesson differs between Daily Plan and My Bookmarks; no production application code or entitlements were changed.

Male Kegel required correcting the restored-program locator and using a native tap on its selector card; the final Kegel checks pass on both variants. Related paid lesson/practice saved-slide checks also pass on both variants after these changes. The female analytics attempt initially captured only BookmarkDeleted during the parallel run. An isolated follow-up with a fresh observer captured all three required events with corroborated account identity and passed the unchanged assertions. The reason for the original incomplete capture is unconfirmed; retain this as an observability instability rather than a proven missing-product-event defect.

Targeted local checks passed: 14 Java Bookmarks locator/evidence unit tests and 5 Python simulator-preparation unit tests. Offline checks additionally validated card grouping on both captured native lists and header matching/exclusion on the failing Today and Bookmarks snapshots. The UI commands used `mvn -q test -Dtest=tests.BookmarksTests#<method> -Dplatform=ios` with per-variant capabilities and secret-backed environments; the unit checks used JUnitCore and `python3 -m unittest discover -s scripts -p test_prepare_bookmarks_simulator.py -v`.

Local artifacts for this run are under `target/bookmarks-full-20261022-1227`: `full-report.html`/`full-status.json` preserve the 42-slot matrix and attempt history; `allure-report-latest` contains only the latest 38 executed scenarios. Raw attempts, diagnostics and account configuration remain local. The grouping, modern Kegel and base-access checks are representative data variants; they do not establish every cross-module/single-category, legacy regular/sport Kegel, or trial variant in Xray.

## Android port — separate candidate contracts (2026-10-23)

`tests.BookmarksTests` and `suites.BookmarksSuite` retain the current iOS contract.
`tests.AndroidBookmarksTests` inherits its 21 methods and overrides only the two
platform-specific scenarios below. The bodies of all 21 iOS test methods remain
unchanged; saved-slide restoration, exact card grouping, completion checks,
identity verification and fresh structured analytics/request evidence are retained.
This is a selective port of the local Android work, not a replacement of the
newer iOS files with older copies.

- COA-9666 uses separate old/new APKs on an explicitly selected test emulator,
  an in-place `adb install -r` upgrade, a higher installed `versionCode`, and
  Profile identity verification **without re-login after upgrade**. The later
  reinstall phase explicitly signs in and checks the app-local tooltip state.
  The original iOS numeric-build validation is unchanged.
- The local Android no-subscription candidate expects a paywall at the Bookmarks
  entry point. It overrides the inherited method only in the Android subclass
  and has an explicit Android display name. It does **not** replace the iOS
  available-base-lesson requirement or turn the known iOS access failure green.
  This Android expectation still requires Product/QA confirmation before merge;
  an iOS-labelled Jira case alone does not establish the Android contract.

Start with an already prepared `BASELINE` role and one non-completing method:

```bash
mvn -q test -Dtest=tests.AndroidBookmarksTests#emptyListNeverShowsTooltip \
  -Dplatform=android -Dandroid.udid=emulator-5554 \
  -Dandroid.appPackage=com.vamapps.thecoach \
  -Dandroid.appActivity=com.vamapps.thecoach.MainActivity \
  -Dandroid.noReset=true -Dandroid.fullReset=false
```

For Her uses `com.vamapps.thecoach.forher` and its own approved role accounts.
Use one app variant per JVM, as in the existing Page Object architecture.
The same `COACH_BOOKMARKS_<ROLE>_EMAIL/OTP` and content fixture variables apply;
do not reuse the module/test-model Premium account or the primary iOS regression
account. Missing roles/content, recurring-day state and fresh Android analytics/
request observation are real prerequisites, not reasons to skip or loosen tests.
The Simulator observer above is iOS-only and must not be used as Android evidence.
Run tooltip-first and update/reinstall cases separately with their required
app-local state. COA-9666 additionally needs `-Dandroid.update.old=<old.apk>` and
`-Dandroid.update.new=<new.apk>` for genuine builds of the configured package.
Do not run the full class against shared accounts: completion cases need dedicated
snapshots and the update case removes/reinstalls the selected test app.

The generic `run_android_parity.py` suite manifest is unchanged; select the new
Bookmarks class with Maven as above. Existing parity jobs gain these explicit
configuration options:

| Option | Effect |
| --- | --- |
| `--post-auth-goal <goal>` | Sets `POST_AUTH_QUESTIONNAIRE_GOAL`; otherwise uses the account record's `onboardingGoal`, then the male/female default. This **does not enable** questionnaire preparation. |
| `--confirm-immersive-mode` | Opt-in `adb settings put secure immersive_mode_confirmations confirmed` on the selected `emulator-*`, after account/APK checks. Default runs do not write this setting. |
| `-Dcoach.android.allowProfileTooltipFallback=true` (direct Maven) | Allows the previously observed Her-only coordinate fallback, only after a failed Profile action and when semantic `btnOk` is absent. Disabled by default; never a general retry. |

Surefire 2.12.4 supports one exact `--method` in a single class, not
`method1+method2`. Invalid selectors fail before Keychain/device access.
Runner output redacts configured email, OTP and UID, and preserves Maven's exit code.

Ordinary `loginWithEmailAndOtp` and existing-progress authentication remain strict:
a post-auth questionnaire raises `PostAuthQuestionnaireException`, even when a
setup flag is present. For a **separate, explicitly approved mutable fixture**,
setup code can call `AndroidCoachFlowPageObject.loginForFixturePreparation` with
both `coach.android.preparePostAuthQuestionnaire=true` (or
`COACH_ANDROID_PREPARE_POST_AUTH_QUESTIONNAIRE=true`) and an explicit
`postAuth.questionnaire.goal` / `POST_AUTH_QUESTIONNAIRE_GOAL`. Validation runs
before authentication or questionnaire actions; there is no guessed first primary
goal. This API is not called by ordinary regression tests or the parity runner.
After setup, verify an independent strict re-login; preparation alone does not
prove an existing-progress fixture is ready. No setup or account mutation was
performed while preparing this port.

### Verification boundary

Offline verification covers Android locators, row-owned grouping, saved-slide
body scoping, negative completion markers, factory selection, 21-method inheritance,
and strict/setup authentication separation. Existing iOS locator, rendered
completion-marker and readiness unit tests remain in the unit selection.
The native captured XML replay is read-only historical evidence, not a fresh
Appium pass. The initial port had no attached Android target. The subsequent
[live Android follow-up](android-pr25-live-ui-2026-10-23.md) uses dedicated API-36
emulators and distinguishes successful authorization checks from Bookmarks
attempts blocked during login. It does not establish full Bookmarks parity,
completion, analytics/request collection, update/reinstall or the candidate
access expectation. Keep this port Draft until those fixture-backed checks and
contract review are done.

The follow-up recognizes the current Android `tvEmailNotfoundError` rather than
waiting for an OTP screen after the app has already rejected the email. Hidden
or empty error views cannot satisfy it. Onboarding also dismisses the observed
System UI full-screen hint only when `immersive_cling_title` is visible, through
System UI's own `ok` id, and verifies its disappearance. This is not a generic
Got-it/OK tap and does not write device settings from the Page Object.

### Android continuation — 2026-10-24

See the separate [authorization/onboarding and Bookmarks report](android-pr25-live-ui-2026-10-24.md)
for measured results and remaining gates. Android Profile hides the bottom tabs:
after identity verification, the Android Bookmarks login closes Profile through
`returnToMainScreenFromProfile()` before selecting Today. This does not change
the shared iOS login or its 21 test bodies.

An unfinished Android questionnaire cannot be reported as logged out merely by
navigating Back to Welcome. Cleanup now rejects that state, allowing the existing
bounded recovery controller to use the configured test APK. App-local reinstall
recovery is not a server progress reset and does not make the failed scenario pass.
Post-questionnaire readiness also recognizes the observed notification permission
dialog and System UI full-screen hint, so existing handlers can process them.

For a separate accessibility diagnostic, opt in with
`-Dandroid.disableSuppressAccessibilityService=true` or
`ANDROID_DISABLE_SUPPRESS_ACCESSIBILITY_SERVICE=true`. This preserves configured
accessibility services instead of UiAutomator2 suppressing them; it neither enables
a service nor guarantees that WebView exposes every screen. The default capability
is unchanged, and iOS never receives it. The report records the separate, temporary
Chromium userdebug override used locally; do not apply it to a personal/production
device or treat that execution as default-device parity.

On a fresh Android installation, the CONTENT account can show an app-local
`Remove from bookmarks` popup whose accessibility window hides the list beneath.
Record this separately from a missing Bookmarks screen. For a non-tooltip diagnostic,
prepare/dismiss that popup explicitly with the existing UI helper **before** the
measured attempt; do not silently dismiss it inside tooltip assertions. Current-account
mode remains diagnostic only, not evidence of strict independent re-login.

**State-safety warning:** a catalog GET for another program was observed to change
the server's selected program. Never enumerate catalogs as a supposedly read-only
progress observer. Use identity-verified current-state reads without program/day
selectors; record and restore any intentionally selected program/day separately.
Do not infer an all-program completion snapshot from a current-program response.
