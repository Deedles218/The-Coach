# Android regression contracts

These opt-in contracts let a reviewed Android build use its observed Profile,
Explore, program-selector and Kegel layouts. The default paths retain the
legacy native assertions. Select a contract from independently reviewed build
and catalog evidence before running a UI case; missing UI is not a reason to
change an expectation.

| Property | Default | Explicit reviewed contract |
| --- | --- | --- |
| `coach.selector.contract` | `legacy`: active first, orange outline, observed loader, vertical motion | `activation-v3`: Main Journey stays first, one active mark, complete saved membership and ready destination content |
| `coach.profile.settings` | `program`: Program settings / Personalization | `general`: General / Language; progress, Account settings and Support remain required |
| `coach.kegel.completionFeedback` | `intensity`: Too Easy / Great / Too Hard | `questionnaire`: three numbered questions with nonempty prompts, retained answers, enabled FINISH and return to Today |
| `coach.kegel.instructionTitle` | unset: first exercise | exact exercise title selected within the workout's instruction list |
| `coach.kegel.instructionStepCount` | `3` | reviewed integer from 1 through 10; malformed values fail |
| `coach.kegelPlayer.workout` | first supported generated workout | exact supported workout within DAILY PRACTICE |
| `coach.explore.quickTip.redirect` | unset: existing video / configured-content behavior | `coach_lesson`: Finish Too Soon article; `coach_video`: native video controls; `absent`: no Quick Tips anywhere in the feed plus the other required sections/templates |
| `coach.profile.faq.url` / `.text` | existing native FAQ assertions | reviewed HTTPS destination and nonempty rendered text |
| `coach.explore.masterclass.url` / `.text` | existing native destination and card-title assertions | reviewed HTTPS destination and nonempty rendered topic text, independent of the card caption |

For selector v3, prepare a fixture whose saved-program membership and roles can
be read independently through the existing UID-bound program-selection reader.
A list that fits the viewport must expose the entire expected set before and
after a gesture. A scrollable list must move vertically, retain horizontal
position and expose the complete membership. Legacy mode still requires
vertical movement. Switching must preserve roles, per-program progress and the
original program/day/progress after restoration. The existing API identity,
source, build and analytics UID guards remain required.

V3 motion uses the sheet surface and a locally contrasting gray handle that
can fade while moving. Multiple intermediate positions and net movement are
still required; a fade at a stationary position fails. The legacy detector
remains the default.

The analytics reader accepts a `.tmp` open JSON array only when adding its
closing container yields valid JSON. It never exports a complete prefix when a
later event is truncated or malformed. Invalid encodings and non-JSON numeric
constants are rejected. Diagnostics contain counters, not queue payloads or
credentials. UID, timestamp, baseline fingerprint and program-ID matching
remain unchanged, including baseline events from open batches.

Use a dedicated Kegel account and its `COACH_ANDROID_MUTABLE_UID` binding.
Questionnaire completion additionally requires
`-Dcoach.kegel.completeWorkoutForFeedback=true` on a disposable fixture:
finishing a workout changes progress and submits QA answers. The instruction
case changes intensity and restores it; select a reviewed exercise title and
step count together when the default three-step exercise is unsuitable.
Exact workout selection retains the DAILY PRACTICE section boundary and cannot
select a similarly named item in a later section.

WebView verification is enabled only by an explicit destination property and
requires compatible Chromedriver support in the existing Appium environment.
It selects an attached, visible, nonempty page from the configured application's
own context, checks HTTPS host, port and path, selects that page's browser
window, then waits for a complete visible document and its expected rendered
text. Tracking query parameters are ignored; encoded path separators, another
host or a different page cannot satisfy the check. The original native context
is restored in `finally`. Default Profile/Explore paths continue to use native
locators. Do not change global remote configuration to make these cases pass.

Run these offline checks without a device, account, Appium or network:

```sh
PYTHONDONTWRITEBYTECODE=1 PYTHONPATH=scripts python3 -m unittest -v scripts/test_android_analytics.py
mvn -o -B -ntp -Djava.awt.headless=true -Dtest=tests.AndroidSelectorContractUnitTests,tests.AndroidSelectorLayoutLocatorUnitTests,tests.AndroidKegelCardLocatorUnitTests,tests.AndroidWebViewDestinationUnitTests,tests.AndroidSheetMotionUnitTests,tests.AndroidExploreContractUnitTests,tests.AndroidKegelContractUnitTests,tests.AndroidKegelQuestionnaireUnitTests test
```

Maven offline mode requires dependencies and plugins already cached locally.
These are mocked locator, contract, parser and image-oracle checks. They do
not establish that a build's UI, analytics delivery, fixture restoration or
Chromedriver integration works. Native validation uses the existing targeted
Maven/Appium entry points with explicit build/device/contract properties and
secret-backed fixtures; keep separate evidence for each APK and run one
session per device.

## Historical evidence and remaining validation

The imported author's 26 November 2026 notes associated these contracts with
Her 2.20.6, Him 1.45.6 and COA-9583. They cited General / Language, Waves with
four steps, Evening Kegel Workout, the Her FAQ path `/faq_forher`, and a catalog
without Her Quick Tips. These are historical examples to revalidate against
the target APK and activated catalog, not defaults or current execution proof.
No earlier PASS, prepared-account, Appium-server or coverage claim from those
notes is adopted here. This publication work only performs offline checks;
active iOS work is outside its scope.

No purchase or PDF entitlement is exercised by these additions. The existing
workbook paywall case still needs an independently verified no-PDF fixture;
absence of a purchase alone does not establish that precondition. Device runs,
fixture suitability and rendered WebView behavior remain separate validation
requirements.
