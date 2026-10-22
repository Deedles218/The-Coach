# Bookmarks iOS automation

`BookmarksTests` contains 21 scenarios linked to Xray cases COA-9647 through COA-9666 and the no-subscription access check from COA-9667. A linked method alone does not establish complete coverage of every variant in its Xray case; required fixture states and the assertions below must also be verified. Trial variants from COA-9667 and COA-9668 (expired subscription) are intentionally excluded by the agreed scope. The suite is iOS-only because the source cases are labelled iOS and the verified accessibility identifiers come from the iOS build.

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
| `NOSUBSCRIPTION` | Newly provisioned account with onboarding completed and no active, trial, lifetime, or expired entitlement; Today is available |

Cases that require a bookmarkable practice (`COA-9649`, `COA-9651`, `COA-9657`, `COA-9663`, `COA-9664`) must run with the Coach for Her fixture unless the selected men's program exposes an equivalent bookmarkable practice. The current men's Stage 1 does not.

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

To isolate the completion transition from other cases that finish CONTENT's lesson, set `COACH_BOOKMARKS_COMPLETION_ACCOUNT_ROLE` / `coach.bookmarks.completionAccountRole` to a separately prepared role. For example, `COMPLETION` reads `COACH_BOOKMARKS_COMPLETION_EMAIL/OTP`; an existing role can be reused only if its initial and final state satisfy both scenarios. The incomplete-lesson precondition remains mandatory. Kegel-only fixtures similarly use `COACH_BOOKMARKS_KEGEL_ACCOUNT_ROLE` / `coach.bookmarks.kegelAccountRole`, for example `KEGEL` with `COACH_BOOKMARKS_KEGEL_EMAIL/OTP`. Each defaults to CONTENT for existing configurations. The Kegel fixture must already select the required program, and its configured card titles must identify the intended regular and sport variants; two cards in the new `kegel_workout` format alone do not establish legacy variant coverage.

For content with a required final question, supply the exact visible answer through `COACH_BOOKMARKS_LESSON_COMPLETION_ANSWER` / `coach.bookmarks.lessonCompletionAnswer`, or `COACH_BOOKMARKS_RECURRING_COMPLETION_ANSWER` / `coach.bookmarks.recurringCompletionAnswer`. The Page Object selects it only when FINISH is disabled, then waits for the button to become enabled. It does not guess an answer or treat a visible disabled button as a missing locator.

Supply only the values needed by the selected method: `COACH_BOOKMARKS_LESSON_TITLE`, `SECOND_LESSON_TITLE`, optional `SECOND_CONTENT_TITLE` (for two-card removal checks), `PRACTICE_TITLE`, `RECURRING_PRACTICE_TITLE`, first/middle/last or saved slide texts, `PROGRAM_TITLE`, `MODULE_TITLE`, `EMPTY_CATEGORY_TITLE`, `TOOLTIP_TEXT`, `KEGEL_TITLE`, `KEGEL_SPORT_TITLE`, `LESSON_ID`, `SAVED_SLIDE_NUMBER`, `PROGRAM_ID`, `PROGRAM_DAY`, `SAVED_DAY_TITLE`, and `PREVIOUS_DAY_TITLE`. System properties use the equivalent lower-camel names under `coach.bookmarks.*`. Recurring content can override `recurringLessonId`, `recurringProgramId`, `recurringProgramDay`, `recurringSavedSlideNumber`, and `recurringPracticeSavedSlideText` (or their `COACH_BOOKMARKS_RECURRING_*` environment equivalents), so its expected payload is not confused with the ordinary lesson fixture.

COA-9666 requires `IOS_UPDATE_APP`/`ios.update.app` and intentionally removes then reinstalls the app during its reinstall phase. Simulator `.app` updates validate installed and requested `CFBundleVersion` values and require distinct real build numbers. A placeholder such as `BITRISE_BUILD_NUMBER` blocks update verification; changing bundle metadata locally would not prove an application update.

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
