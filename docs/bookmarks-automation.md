# Bookmarks automation

`BookmarksTests` contains 30 UI methods linked to 21 unique Xray cases, COA-9647 through COA-9667. `BookmarksExpiryTests` adds a dedicated run for COA-9668. Additional methods are variants, not additional Xray Tests. A linked method alone does not establish complete coverage of every variant in its Xray case; required fixture states and the assertions below must also be verified. The trial variant from COA-9667 and live acceptance of COA-9668 still require controlled entitlement fixtures. The source cases are labelled iOS; `AndroidBookmarksTests` inherits the shared scenarios with the candidate-contract limitations below.

For paid access, set `coach.bookmarks.accessVariant=active-subscription` or `active-trial` and select a prepared account through `coach.bookmarks.availableAccountRole`. The default roles are `ACTIVESUBSCRIPTION` and `ACTIVETRIAL`; both variants verify the Premium badge after login. Confirm the subscription or trial state during fixture preparation.

For [COA-9667](https://the-coach.atlassian.net/browse/COA-9667), the base account opens and saves the program's first lesson from Today without a paywall. Tapping its saved card in My Bookmarks shows the subscription paywall. With an active subscription or trial, the saved card opens the lesson at its saved slide. Start with an empty list or declare all existing bookmark titles, separated by `|`, in `COACH_BOOKMARKS_AVAILABLE_INITIAL_CARDS` / `coach.bookmarks.availableInitialCards`. The test checks the initial and resulting lists. Cleanup removes the created bookmark and checks the full initial list again.

The [COA-9668](https://the-coach.atlassian.net/browse/COA-9668) test saves the available lesson while access is active and verifies account identity and subscription state through read-only `/user/` evidence. After the fixture's expiry transition, My Bookmarks and its saved card remain accessible without a paywall; tapping that card must show the subscription paywall. Cleanup restores the entitlement snapshot, removes the test bookmark, and verifies the empty list. This follows main's expired-access contract and is separate from the no-subscription entry-point variant.

### Dedicated expiry run

Prepare an iOS Him build on a dedicated Simulator or a paired test iPhone and an approved account with a restorable active subscription or trial. The selected Today stage must contain the configured lesson. Start with an empty My Bookmarks list and previously dismissed tooltip. Give the test the dedicated credentials through `COACH_EXISTING_PROGRESS_EMAIL` and `COACH_EXISTING_PROGRESS_OTP`, the UID through `COACH_EXPECTED_ACCOUNT_UID`, and the UI strings through `COACH_BOOKMARKS_LESSON_TITLE` and `COACH_BOOKMARKS_TOOLTIP_TEXT`. On a physical iPhone the reader uses the installed version and build from the device selected by Appium. It verifies the session UID and the response UID before using the subscription status.

`COACH_BOOKMARKS_EXPIRY_CONTROL` points to an executable supplied by the fixture owner. It receives `<expire|restore> <ios-device-udid> <bundle-id>` and inherits `COACH_EXPECTED_ACCOUNT_UID`. Scope both actions to that approved UID and environment. `expire` takes the entitlement through expiry while preserving the bookmark and sign-in. `restore` reinstates the original entitlement snapshot and must tolerate a partially failed expiry. Each action exits successfully only after its result is available for read-back. The test independently checks `subscriptionActive` on the same UID before and after the transition and after restoration.

```bash
TEST_ISOLATION_MODE=logout mvn -q test \
  -Dtest=tests.BookmarksExpiryTests#bookmarkSurvivesExpiryAndOpeningLessonShowsPaywall \
  -Dplatform=ios -Dios.udid='<ios-device-udid>' -Dios.bundleId=com.vamapps.The-Coach \
  -Dappium.url='<appium-url>' -Dios.noReset=true -Dios.fullReset=false \
  -Dcoach.ios.postAuthGoal='<fixture-matching primary goal>'
```

## Runner

Run one case first:

```bash
mvn -Dtest=tests.BookmarksTests#emptyListNeverShowsTooltip \
  -Dplatform=ios \
  -Dios.udid=<UDID> \
  -Dios.deviceName=<device-name> \
  -Dios.bundleId=<bundle-id> \
  -Dcoach.ios.postAuthGoal='<fixture-matching primary goal>' \
  test
```

The regression controller separates Bookmarks into nine installation jobs.

```bash
python3 scripts/ios_regression/run_regression.py --config <local-config.json> \
  --only BookmarksTests --only BookmarksTooltipFirstEntry \
  --only BookmarksTooltipNonEmptyFirstEntry --only BookmarksTooltipDismissal \
  --only BookmarksTooltipDismissalCard --only BookmarksTooltipDismissalBookmark \
  --only BookmarksTooltipDismissalNavigation --only BookmarksUpdate --only BookmarksReinstall
```

These jobs cover the 30 methods per configured app variant, with two executions of COA-9666. Each of the six tooltip jobs uses its own fresh installation. `BookmarksUpdate` starts with `oldApp` and `BookmarksReinstall` starts with the current `app`. Both supply the current `app` as `ios.update.app`. Run the two jobs with separate prepared installations. The `UPDATE` account must have the lesson and a previously dismissed tooltip at the start of either job. A single `suites.BookmarksSuite` invocation shares one capability configuration and cannot supply the different installation states. Run targeted methods on a dedicated device with the prepared local history and server snapshot.

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
| `TOOLTIP` | One saved lesson and one saved practice; tooltip has never been shown. Each tooltip variant needs independent local history |
| `RECURRING` | Known repeating content on earlier/later days. COA-9663 starts without its bookmark; COA-9664 needs a separately prepared saved later occurrence and incomplete days. Restore the matching snapshot between cases |
| `SWITCHA` | Exactly one bookmark, different from `SWITCHB` |
| `SWITCHB` | Exactly one bookmark, different from `SWITCHA` |
| `UPDATE` | Exactly one saved lesson; tooltip already consumed; update build configured |
| `NOSUBSCRIPTION` | Account with onboarding completed, an empty Bookmarks list, and no active, trial, lifetime, or expired entitlement; the configured base lesson is available without paid access |

Cases that require a bookmarkable practice (`COA-9649`, `COA-9651`, `COA-9657`, `COA-9663`, `COA-9664`) need an actual Practice card with a bookmark control. Last Longer's Stage 1 cards do not meet this prerequisite. The men's Sex Is a Skill program exposes Pleasure Spot Mapping, a bookmarkable six-slide Practice; it can supply the ordinary practice scenarios. A recurring fixture for COA-9664 requires proof that the practice occurs on both configured days.

COA-9652 has separate empty-first and nonempty-first methods. COA-9653 has separate blank, card, bookmark and navigation methods. Before each iOS tooltip method and each Android COA-9652 method, prepare independent app-local tooltip history. For Android COA-9653, select a disposable emulator with `-Dandroid.udid=emulator-<port>` and pass `-Dandroid.tooltip.apk=<path>` for the same package, version and build already installed. Each method records the Bookmarks layout, reinstalls that APK, verifies the same `TOOLTIP` account, and measures one tap on the fresh installation. Both saved cards must exist before the method and after reopening the list.

On a paired test iPhone, pass `-Dcoach.bookmarks.tooltipTestFlightBuild='<version> (<build>)'` or set `COACH_BOOKMARKS_TOOLTIP_TEST_FLIGHT_BUILD` and select one tooltip method per run. The test checks that the exact production build is installed and available in TestFlight, removes Coach from the selected phone, reinstalls that build, and signs in to the specified fixture role. Use this option for a run whose setup includes clearing the app's local data.

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

Ordinary save/removal scenarios restore the declared saved slide through the UI, including after assertion failures. Run COA-9658, COA-9659 and COA-9660 individually with a fixture owner who can restore completion as well as bookmarks. Recurring completion has a separate contract below.

Set `COACH_BOOKMARKS_COMPLETION_ACCOUNT_ROLE` / `coach.bookmarks.completionAccountRole` to the approved role, for example `COMPLETION` for `COACH_BOOKMARKS_COMPLETION_EMAIL/OTP`. The default `CONTENT` role also needs an approved snapshot for the selected method. Supply its UID in `COACH_EXPECTED_ACCOUNT_UID` and set `COACH_BOOKMARKS_COMPLETION_SNAPSHOT` and `COACH_BOOKMARKS_COMPLETION_CONTROL` to the snapshot ID and owner's executable. The latter two settings also accept the properties `coach.bookmarks.completionSnapshot` and `coach.bookmarks.completionControl`.

The executable receives `<verify|restore> <snapshot-id> <app-id> <issue> <variant> <content-title>` and inherits the approved UID and the selected role's email as `COACH_BOOKMARKS_COMPLETION_EMAIL`. Scope both commands to that account, app, and environment. `verify` compares progress, completion, and bookmarks with the named snapshot and verifies the app's selected fixture for the case. `restore` reinstates that snapshot, including after a partial failure. Each command has up to 30 seconds. The test verifies before login and after restoration, and requires the tooltip to have been dismissed during fixture preparation.

| Case | Variant | Snapshot at the start of the run |
| --- | --- | --- |
| COA-9658 | `lesson`, `practice` | The selected content is saved and ready to finish from My Bookmarks |
| COA-9659 | `lesson` | An incomplete, unsaved lesson with one program/day binding |
| COA-9660 | `list`, `reader` | The selected lesson is saved and incomplete, with one program/day binding. Each method completes it from My Bookmarks before exercising its removal path |

For COA-9661, configure the approved Premium role through `COACH_BOOKMARKS_KEGEL_ACCOUNT_ROLE`, for example `KEGEL` with `COACH_BOOKMARKS_KEGEL_EMAIL/OTP`. The existing default role is CONTENT. Set `COACH_BOOKMARKS_KEGEL_TITLE` and `COACH_BOOKMARKS_KEGEL_SPORT_TITLE` to the visible titles of two incomplete cards. Point `COACH_BOOKMARKS_KEGEL_DAILY_PLAN_CAPTURE` and `COACH_BOOKMARKS_KEGEL_SPORT_DAILY_PLAN_CAPTURE` at local copies of the Daily Plan v2 response bodies for those cards under the approved account and build. Each copy needs `program_section` with the program headline, day or module-stage metadata and the matching entry in `questions`. The test checks the card's `name`, `headline` and the required `link_type.kegel` or `link_type.kegel_sport` before selecting that workout in the UI.

These copies describe expectations and cannot establish current workout type by themselves.
Include `program_section.section_id` and the global `day_in_program` alongside module-stage metadata.
After UI selection, `read_bookmarks_kegel.py` verifies the installed build and current SDK UID against
`COACH_EXPECTED_ACCOUNT_UID`, then checks the configured account email and Premium status through
read-only `/user/` evidence. A fresh authenticated catalog GET must match the program, global day,
visible day label, unique title, card ID and legacy link type before the UI assertions continue.
This is catalog corroboration, not captured app traffic. It uses the existing iOS session readers;
Android supports the reviewed rooted Him emulator or an explicitly APK-bound preprod debug package.
No fixture, installation, account, subscription or progress is modified by this evidence reader.

The iOS scenario checks the workout overview, an exercise instruction, stretching screens when enabled, the active and paused player, and the player info sheet. It uses Quit and compares the workout completion marker and program progress after exit, then restores the original program/day selection. Run `tests.BookmarksTests#kegelAndKegelSportCannotBeBookmarked` on the prepared iOS Him fixture.

COA-9659 can use another UI-verified incomplete lesson on the chosen account: set `COACH_BOOKMARKS_COMPLETION_LESSON_TITLE` / `coach.bookmarks.completionLessonTitle` together with `COACH_BOOKMARKS_COMPLETION_LESSON_SAVED_SLIDE_TEXT` / `coach.bookmarks.completionLessonSavedSlideText`. Both are scoped to this completion test; the other Bookmarks cases keep their ordinary lesson fixture. If the chosen lesson requires a final answer, supply its exact visible text through `COACH_BOOKMARKS_COMPLETION_ANSWER` / `coach.bookmarks.completionAnswer`. A custom lesson does not inherit the ordinary lesson's answer. When no custom title is supplied, the existing lesson, saved-slide and answer configuration remains supported.

For content with a required final question, supply the exact visible answer through `COACH_BOOKMARKS_LESSON_COMPLETION_ANSWER` / `coach.bookmarks.lessonCompletionAnswer`, or `COACH_BOOKMARKS_RECURRING_COMPLETION_ANSWER` / `coach.bookmarks.recurringCompletionAnswer`. The Page Object selects it only when FINISH is disabled, then waits for the button to become enabled. It does not guess an answer or treat a visible disabled button as a missing locator.

For a program without modules, set `COACH_BOOKMARKS_PRACTICE_GROUP_TITLE` / `coach.bookmarks.practiceGroupTitle` to its visible program heading. The grouping check verifies the nearest program/module heading and category preceding the specific card in the observed native list, plus the card composition. A heading or category belonging to another card cannot satisfy it. Without this option, `moduleTitle` remains required. Use the rendered heading text, including capitalization (the current male module headings are uppercase). A pass on Path to Desire, which has no modules, does not establish cross-module coverage of COA-9651. The mixed CONTENT fixture has both Lessons and Practices. The test switches to the separately prepared lesson-only UPDATE role to verify Lessons/card composition and absence of Practices; an arbitrary nonexistent category name cannot satisfy this check. Additional cross-module and practice-only variants still need their own exact fixture sets.

### Full COA-9651 grouping run

Run `tests.BookmarksGroupingTests#cardsMatchApprovedGroupingFixture` for each of `mixed`, `lessons-only` and `practices-only`. Supply the prepared account's email and OTP through `COACH_EXISTING_PROGRESS_EMAIL` and `COACH_EXISTING_PROGRESS_OTP`, its login goal through the ordinary platform configuration, and the tooltip text through `COACH_BOOKMARKS_TOOLTIP_TEXT`. The account starts with the declared cards and the tooltip already dismissed. The test reads the list from top to bottom, checks each card's program, module and category, and leaves saved data in place.

Point `COACH_BOOKMARKS_GROUPING_FIXTURE` at a local UTF-8 properties file. For example, a lessons-only fixture with one card could contain the following fields.

```properties
variant=lessons-only
card.count=1
card.1.title=Starting Point
card.1.program=Last Longer
card.1.module=MODULE 1: PF & AROUSAL FUNDAMENTALS
card.1.category=Lessons
card.1.cover=references/starting-point.png
```

The mixed fixture has four entries, with one lesson and one practice in one module, a lesson in another module of that program, and a practice in a second program. The other two variants list every card in the single visible category. Reference paths are relative to the fixture file. Use independently verified cover crops at the size rendered on the selected device and build. The check compares decoded pixels from the specific cover element; review a size or image mismatch against the expected artwork.

```bash
TEST_ISOLATION_MODE=logout mvn -q test \
  -Dtest=tests.BookmarksGroupingTests#cardsMatchApprovedGroupingFixture \
  -Dplatform=ios -Dios.udid='<test-device-udid>' -Dios.bundleId=com.vamapps.The-Coach \
  -Dappium.url='<appium-url>' -Dios.noReset=true -Dios.fullReset=false \
  -Dcoach.ios.postAuthGoal='<fixture-matching primary goal>'
```

Supply only the values needed by the selected method: `COACH_BOOKMARKS_LESSON_TITLE`, `SECOND_LESSON_TITLE`, optional `SECOND_CONTENT_TITLE` (for two-card removal checks), `PRACTICE_TITLE`, `RECURRING_PRACTICE_TITLE`, first/middle/last or saved slide texts, `PROGRAM_TITLE`, `MODULE_TITLE`, `TOOLTIP_TEXT`, `KEGEL_TITLE`, `KEGEL_SPORT_TITLE`, `LESSON_ID`, `SAVED_SLIDE_NUMBER`, `PROGRAM_ID`, `PROGRAM_DAY`, `SAVED_DAY_TITLE`, and `PREVIOUS_DAY_TITLE`. System properties use the equivalent lower-camel names under `coach.bookmarks.*`. Recurring content can override `recurringLessonId`, `recurringProgramId`, `recurringProgramDay`, `recurringSavedSlideNumber`, and `recurringPracticeSavedSlideText` (or their `COACH_BOOKMARKS_RECURRING_*` environment equivalents), so its expected payload is not confused with the ordinary lesson fixture.

For COA-9654 and COA-9655, set `COACH_BOOKMARKS_LESSON_SLIDE_MARKERS` and `COACH_BOOKMARKS_PRACTICE_SLIDE_MARKERS` to unique visible reader texts in first-to-last order, separated by `|`. The test advances one slide per action and verifies the visible FINISH control on the final slide. Prepare the CONTENT account with its tooltip dismissed. When the account has additional cards, declare the complete list of bookmark titles with `COACH_BOOKMARKS_CANCEL_INITIAL_CARDS` or `COACH_BOOKMARKS_REMOVAL_INITIAL_CARDS`, also separated by `|`. The default roster consists of the cards used in the two variants of the selected case.

For COA-9655, set each `LESSON` and `PRACTICE` fixture's `COACH_BOOKMARKS_<KIND>_REMOVAL_NEIGHBOR_TITLE`, `COACH_BOOKMARKS_<KIND>_REMOVAL_GROUP_TITLE`, and `COACH_BOOKMARKS_<KIND>_REMOVAL_NEIGHBOR_SAVED_SLIDE_TEXT`. Both cards must appear in the same category and module. Supply the selected card's original saved slide through `COACH_BOOKMARKS_<KIND>_SAVED_SLIDE_TEXT`. The test checks both saved positions before the removal, compares the card set after REMOVE and re-save, and reopens the neighbor at its original slide. If the scenario fails before re-saving the selected card, cleanup uses the declared original saved position.

For COA-9657, prepare distinct saved lesson and practice cards with the tooltip dismissed. Use the ordered `COACH_BOOKMARKS_LESSON_SLIDE_MARKERS` and `COACH_BOOKMARKS_PRACTICE_SLIDE_MARKERS` and set the corresponding `COACH_BOOKMARKS_<KIND>_SAVED_SLIDE_TEXT`. The lesson uses an interior slide and the practice uses a different position. The test opens both cards and checks the saved positions without writing bookmarks. It uses `CONTENT` by default and also accepts a prepared role through `COACH_BOOKMARKS_SAVED_SLIDE_ACCOUNT_ROLE` or `coach.bookmarks.savedSlideAccountRole`.

For a direct COA-9666 run, select `-Dcoach.bookmarks.installVariant=update` or `-Dcoach.bookmarks.installVariant=reinstall`. The existing method also supports a sequential run when this property is omitted. Set `IOS_UPDATE_APP` or `ios.update.app` to the requested `.app` for a Simulator or `.ipa` for the explicitly selected physical iPhone. The update checks the newer numeric build and verifies the same Profile account before any fresh login. The reinstall starts on the build it will reinstall, checks that the app is removed, verifies the resulting build, and signs in to the same account to check the returned tooltip and bookmark. CI placeholder build values block verification.

On a paired test iPhone the same method can install the exact production Coach build offered in TestFlight. Set `COACH_BOOKMARKS_TEST_FLIGHT_BUILD` or pass `-Dcoach.bookmarks.testFlightBuild='<version> (<build>)'`. Select the test iPhone by UDID and prepare the `UPDATE` account and tooltip history as usual. For update, start with the older installed build. For reinstall, start with the requested build. The test matches the version, build, and app row in TestFlight before the operation and reads the installed metadata afterwards. The reinstall variant removes the selected Coach app and reinstalls the offered build via TestFlight.

For lesson and practice fixtures in different programs, set `COACH_BOOKMARKS_LESSON_PROGRAM_TITLE` and `COACH_BOOKMARKS_PRACTICE_PROGRAM_TITLE` (or `coach.bookmarks.lessonProgramTitle` and `practiceProgramTitle`). Every open from Today, including restoration of the saved slide, selects the declared program after login. A custom completion lesson retains its own completion program; ordinary fixture options do not redirect it. These options preserve existing program selection when absent.

For a completion lesson in another program, set `COACH_BOOKMARKS_COMPLETION_PROGRAM_TITLE` / `coach.bookmarks.completionProgramTitle`. The test selects that explicit program after ordinary email/OTP login and Profile identity verification, before checking that the lesson is incomplete. This avoids relying on a program choice made before logout; no program is changed when the option is absent. An ordinary completion run must omit `coach.bookmarks.useCurrentAccount` and the local `BOOKMARKS_KEEP_SESSION` runner override.

The active-program reader prefers the app-side identifier, then reads the observed Today header beside the Bookmarks entry. It supports restored program names outside the old label catalog, including the male/female Sexting programs. The older-build label fallback is restricted to selected Today. Selecting a fixture program uses the existing hittable native-tap helper and still requires the selector to close and the expected program to become active.

iOS login waits for a hittable Welcome control and verifies the email form, rather than accepting a successful click alone. The user confirmed on 18 November that the two native goal questions after OTP are ordinary returning-user login, as on Android. Supply the primary goal matching the selected fixture through `coach.ios.postAuthGoal`; the existing additional-goals handler selects the named Increase self confidence answer. Ordinary login rejects missing goals, resumed secondary-only state, and a full questionnaire after the two questions. Bookmarks controller jobs set `coach.ios.preparePostAuthQuestionnaire=false`; the separate explicit preparation path remains available outside these jobs. Post-OTP readiness requires rendered Today cards and a Profile control with no visible auth form. Opening Profile waits for populated content before the separate, strict account-identity assertion; persistent tabs or an empty navigation container are insufficient. Entry and Profile use one native tap at the observed control's center, sharing the existing iOS browser-close tap implementation.

`openCard` requires the reader to open and captures XML and a screenshot if a paywall appears. The base variant of COA-9667 requires the paywall through `openCardExpectingPaywall` and captures the same evidence before cleanup. Detection and dismissal reuse the iOS onboarding paywall handlers, including the close/RESTORE marker on both builds. Cleanup dismisses the paywall and removes the created bookmark from Today.

## Completion indicator after FINISH

The primary completion check uses the named `dailyCompletedCheckmark*` image inside the exact Daily Plan card. On the current builds, a newly completed card can render a visible tick as an unnamed `XCUIElementTypeOther` until the next login. For this observed state, the Page Object checks the rendered indicator immediately following that card's title against a cropped tick reference. It compares the white glyph's shape independently of the male/female circle color; an unnamed container alone is never evidence of completion. The same check protects the incomplete precondition. Successful visual checks attach the actual indicator crop to Allure, without refreshing the screen, restarting the app, or logging in again.

The reference and negative fixtures under `src/test/resources/bookmarks/completion` contain only status icons cropped from the two builds. Unit tests reject unfinished arrows, blank/solid indicators, a different glyph, and a completed neighboring card. A stable accessibility identifier on the live status indicator remains preferable; if the UI glyph changes, review the visual reference and its negative fixtures before updating it.

## Fresh analytics and request evidence

Select functional scenarios by method for the current Bookmarks acceptance run. COA-9662 is the pre-existing analytics test.

COA-9662 reads append-only JSONL from `COACH_BOOKMARKS_ANALYTICS_LOG`; COA-9663 reads JSONL from `COACH_BOOKMARKS_NETWORK_LOG`. The tests checkpoint the file before the action and accept only complete records appended afterwards. Each save/delete event must contain the expected lesson and slide in that same event; each POST must contain the expected path, program, day, and slide in that same request.

For COA-9663, set `COACH_BOOKMARKS_RECURRING_PROGRAM_TITLE`, `COACH_BOOKMARKS_RECURRING_PREVIOUS_PROGRAM_DAY` (D1), and `COACH_BOOKMARKS_RECURRING_PROGRAM_DAY` (D2). D1 and D2 must be distinct positive days of the same program with D1 before D2. Supply the exact practice and slide along with their expected request IDs and slide number. The practice must be initially unsaved. Leave `COACH_BOOKMARKS_RECURRING_INITIAL_CARDS` unset for an empty My Bookmarks list, or supply the existing titles separated by `|`. The test checks both occurrences, saves only from D2, verifies the new card and the captured POST, and restores the initial list and the original Today program and day.

### COA-9664 recurring completion

Prepare the approved `RECURRING` account with the practice already saved from D2, both occurrences incomplete, and the tooltip dismissed. Supply the practice and program titles, their IDs, both absolute program days, and `COACH_BOOKMARKS_PREVIOUS_DAY_TITLE` / `COACH_BOOKMARKS_SAVED_DAY_TITLE` with the exact Today labels. The test leaves Today on D1 before opening the saved D2 card and checks the two completion indicators after Finish.

Set `COACH_EXPECTED_ACCOUNT_UID`, `COACH_BOOKMARKS_RECURRING_COMPLETION_SNAPSHOT`, and `COACH_BOOKMARKS_RECURRING_COMPLETION_CONTROL`. The control is the environment owner's executable and receives `<verify|restore> <snapshot-id> <bundle-id> <program-id> <lesson-id> <D1> <D2>`. It uses the approved UID and `COACH_BOOKMARKS_RECURRING_EMAIL` from its environment. `verify` reads the fixture, checks its identity and environment, confirms the two incomplete occurrences and exact D2 bookmark binding, and compares the progress and bookmarks with the named snapshot. `restore` restores that same snapshot for that account, including the D2 bookmark. The runner calls `verify` before the UI scenario and again after `restore`, including when a Finish or completion assertion fails. Both commands must return success within 30 seconds after their action is confirmed. Keep the snapshot and executable with the fixture owner.

```bash
TEST_ISOLATION_MODE=logout mvn -q test \
  -Dtest=tests.BookmarksTests#recurringPracticeFinishCompletesSavedDay \
  -Dplatform=ios -Dios.udid='<test-device-udid>' -Dios.bundleId=com.vamapps.The-Coach \
  -Dappium.url='<appium-url>' -Dios.noReset=true -Dios.fullReset=false \
  -Dcoach.ios.postAuthGoal='<fixture-matching primary goal>'
```

## Completion work — 2026-11-18, UI acceptance pending

The additional variants cover lesson/practice toggling and list removal, inactive state on every traversed slide, list/inside deletion of the last lesson/practice, practice Finish, completed-state preservation after inside removal, and the full A → B → A account switch. COA-9649 also checks one whole-content card, rejects slide-text cards and reopens Daily Plan to verify persisted state. Removal assertions never repair the state they are measuring.

These changes are implementation, not new accepted automation or Xray labels. Still prepare and verify: exact cross-program/module and single-category card/image fixtures for COA-9651; legacy Kegel and Kegel+sport for COA-9661; recurring earlier/later incomplete occurrences for COA-9663/9664; genuine old/new builds and independent install state for COA-9666; trial and active-to-expired fixtures for COA-9667/9668. Completion methods need isolated snapshots. Use the latest production builds for Bookmarks; keep the new onboarding Dev build separate.

### Verification and device prerequisites

The 18 November checks passed `test-compile`, 85 targeted Java unit tests and 11 controller unit tests. At that time, the XPath replay tests needed `-Djdk.xml.xpathExprOpLimit=1000`; the default limit rejected two unchanged Today-header expressions. The negative checks covered stale/offscreen bookmarks, incomplete list scans, action-specific evidence, missing goals, an unexpected full questionnaire, and the scoped main-program introduction.

The 26 November integration simplifies that Today-header XPath and passes with the default JDK limits. The latest targeted reports contain 187 passing Java unit tests across 24 classes and 78 passing Python tests for Bookmarks Kegel evidence, update-account reading, the iOS controller, Android module evidence and email evidence. Existing test methods remain present. Review corrections cover the expired-access paywall oracle, custom-role credential redaction, current Kegel account/build/card corroboration, failed-update build restoration, and background-only reader recovery. These checks used no apps, devices, account transitions, fixture controls or TestFlight installations; the UI acceptance prerequisites above remain open.

Production Him 1.15.0 (3597) and Her 2.2.0 (3596), plus Him Dev 1.15.0 (3608), were installed and verified on the test iPhone 11. Android Him 1.44.5 was installed on Galaxy A32. No new full Xray scenario was accepted or labelled in this work: the Android baseline stopped on the startup network/VPN error; the iOS diagnostics exposed the two ordinary login goals and current first-install popups, then lost WDA transport. Two concurrent Appium sessions were confirmed on the same iPhone, production on task-owned port 4793 and Dev on port 4728. The task-owned server was stopped without a device command. Resume UI acceptance on an exclusively assigned device after network preparation; existing regression labels are not changed by these diagnostics. Local evidence is under `target/bookmarks-completion-20261118` and `target/appium-logs` in the task worktree.

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

In the October runs, Starting Point (male) and The Basics of Sexual Desire (female) opened from Daily Plan and could be saved. Tapping their saved cards showed the subscription paywall. Those attempts failed the earlier assertion and restored the empty bookmark fixture. The November access clarification established that the observed paywall is expected for the base account.

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
- The Android no-subscription candidate checks for a paywall at the Bookmarks
  entry point. The iOS base variant checks the paywall after tapping the saved
  card. Confirm the Android entry-point expectation against the Android contract.

Start with an already prepared `BASELINE` role and one non-completing method:

```bash
mvn -q test -Dtest=tests.AndroidBookmarksTests#emptyListNeverShowsTooltip \
  -Dplatform=android -Dandroid.udid=emulator-5554 \
  -Dandroid.appPackage=com.vamapps.thecoach \
  -Dandroid.appActivity=com.vamapps.thecoach.MainActivity \
  -DpostAuth.questionnaire.goal='<fixture-matching primary goal>' \
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
app-local state. COA-9666 uses the same `coach.bookmarks.installVariant` selector.
Start update on the prepared old installation and pass both `-Dandroid.update.old=<old.apk>`
and `-Dandroid.update.new=<new.apk>`. For standalone reinstall, start on the
prepared new installation and pass `-Dandroid.update.new=<new.apk>`. The standard
Android SDK `aapt` command is used to validate package, version and build before
changing the installation. Set `-Dandroid.aapt=/path/to/aapt` or `AAPT` if it is outside `PATH`.
Do not run the full class against shared accounts: completion cases need dedicated
snapshots and the update case removes/reinstalls the selected test app.

The generic `run_android_parity.py` suite manifest is unchanged; select the new
Bookmarks class with Maven as above. Existing parity jobs gain these explicit
configuration options:

| Option | Effect |
| --- | --- |
| `--post-auth-goal <goal>` | Primary goal for the two native Android login questions. Otherwise uses the record's `onboardingGoal`, then an explicitly supplied `POST_AUTH_QUESTIONNAIRE_GOAL`. No male/female goal is guessed. Does **not** enable full questionnaire preparation. |
| `--confirm-immersive-mode` | Opt-in `adb settings put secure immersive_mode_confirmations confirmed` on the selected `emulator-*`, after account/APK checks. Default runs do not write this setting. |
| `-Dcoach.android.allowProfileTooltipFallback=true` (direct Maven) | Allows the previously observed Her-only coordinate fallback, only after a failed Profile action and when semantic `btnOk` is absent. Disabled by default; never a general retry. |

Surefire 2.12.4 supports one exact `--method` in a single class, not
`method1+method2`. Invalid selectors fail before Keychain/device access.
Runner output redacts configured email, OTP and UID, and preserves Maven's exit code.

Ordinary Android `loginWithEmailAndOtp` includes the **two native goal questions**
after OTP. The user clarified this expected returning-user contract on 24 October;
`tvAnswerNum` / `STEP 1/17` or `should_show_goals=true` alone is not a fixture blocker.
Supply a primary goal matching the existing fixture through
`postAuth.questionnaire.goal` / `POST_AUTH_QUESTIONNAIRE_GOAL`. If the secondary
screen requires a choice, the existing named `Increase self confidence` option
is used; an arbitrary first option is not selected. Missing goals/options fail
before guessing. The handler closes optional offers through existing helpers and
still requires Today; it never invokes the full questionnaire, app restart or purchases.
Unexpected native questions or a full Web questionnaire remain failures, even
when a setup flag is present.

For a **separate, explicitly approved mutable fixture** needing full onboarding,
setup code can call `AndroidCoachFlowPageObject.loginForFixturePreparation` with
both `coach.android.preparePostAuthQuestionnaire=true` (or
`COACH_ANDROID_PREPARE_POST_AUTH_QUESTIONNAIRE=true`) and an explicit
`postAuth.questionnaire.goal` / `POST_AUTH_QUESTIONNAIRE_GOAL`. Validation runs
before authentication or questionnaire actions; there is no guessed first primary
goal. This API is not called by ordinary regression tests or the parity runner.
After full setup, verify an independent ordinary re-login, including its expected
two goals; preparation alone does not prove the returning-user path. No full setup
or account mutation was performed while preparing the initial port.

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
access expectation. Those fixture-backed checks and the Android access-contract
review remain prerequisites for declaring full UI parity.

The follow-up recognizes the current Android `tvEmailNotfoundError` rather than
waiting for an OTP screen after the app has already rejected the email. Hidden
or empty error views cannot satisfy it. Onboarding also dismisses the observed
System UI full-screen hint only when `immersive_cling_title` is visible, through
System UI's own `ok` id, and verifies its disappearance. This is not a generic
Got-it/OK tap and does not write device settings from the Page Object.

### Android continuation — 2026-10-24

See the separate [authorization/onboarding and Bookmarks report](android-pr25-live-ui-2026-10-24.md)
for historical measured results, and the [two-goal login correction](android-pr25-login-goals-2026-10-24.md)
for the clarified current contract and fresh follow-up. The earlier persistent-goals
finding is superseded, not an outstanding backend defect. Android Profile hides the bottom tabs:
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
