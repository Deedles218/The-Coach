# PR #25: expected Android login goals — 2026-10-24

## 1. Executive summary

**Decision: Insufficient evidence; keep Draft, no merge to main.**
The user clarified that two native goal questions are expected after Android login.
The earlier blanket rejection of `tvAnswerNum` / `STEP 1/17` was a test-contract
error, not evidence that the account or server needed repair. Ordinary Android
login now handles exactly that native goal flow, optional offers and real Today.
It does not complete a full questionnaire or invoke fixture preparation/restart.
Earlier failures remain historical evidence, not passing runs.
The user then requested finishing the code/PR work without further test checks.
All execution results below predate that request; no additional test or live UI
run is required or claimed for this handoff.

## 2. Scope and evidence

- Initial head `6597749`, branch `codex/android-bookmarks-runner`, PR #25 Draft.
- Owned API-36 AVDs only: `TheCoach_PR25_API36` / `emulator-5580` and
  `TheCoach_PR25_Play_API36` / `emulator-5582`; Appium 2.19.0 on port 4755,
  UiAutomator2 4.2.9, male APK 1.43.3 / versionCode 352.
- Approved existing Bookmarks CONTENT identity, primary goal `Improve sex skills`.
  Primary iOS regression / Kegel / accepted 8% identities were not used.
- Private local artifacts: `target/android-pr25-login-goals.CvAz5w/`.
  State projections also use unique `goals-*.json` names under the previous
  `target/android-pr25-auth-followup.0q5jPZ/` root. No raw credentials are committed.

| Check | Actual result / local evidence |
| --- | --- |
| Post-OTP native goal flow | First and additional goal screens captured; existing helper answered only these and reached E-workbook, not the runtime questionnaire; `auth-before-goals/`, `two-native-goals/` |
| Current-program state around the two answers | Same verified CONTENT identity, entitlement, `sex_skill_man / day 2`, normalized status/progress; `goals-probe-before/after.json` |
| Google APIs E-workbook close diagnostic | Close gesture, native element click and touch-pointer probe did not dismiss the offer. `native-goals-destination/` preserves screenshot/XML; no Back/purchase/restart used to make this successful |
| First real parity-runner auth attempt on Play AVD | 1 JUnit error during post-goal transition; screenshot/XML show `The Coach isn't responding`; event log records `am_anr`, `No response to onStartJob`. One APK cleanup recovery also failed; `play-normal-login/`, `play-anr-events.txt` |
| Changed-condition recovery attempt | Second AVD stopped and selected Play app force-stopped after retaining ANR evidence. Next auth attempt reached program intro but failed waiting for Today; `play-normal-login-recovered/`. State projection remained unchanged |
| Transition-fixed live diagnostic | Ordinary-login setup probe still failed at `Android workbook close did not dismiss the offer`, before Profile or Bookmarks. Current-state projection remained unchanged; `play-tooltip-transition-fixed/`, `transition-fixed-live.log`, `state-after-play-tooltip-transition-fixed.json` |
| Play billing environment observation | Owned Play AVD had no Google accounts; log recorded `In-app billing API version 3 is not supported on this device`. Possible environment factor, not a proven cause of the close failure |
| Auth-contract red/green | Old code: 3 methods, 2 failures; corrected auth/goal/readiness group: 26 passed; `auth-contract-red.log`, `goals-unit-green.log` |
| Transition unit selection | 30 passed, including four transition cases; `transitions-unit.log` |
| Full Java unit selection | `mvn -q '-Dtest=*UnitTests' test`: 186 passed, 0 failures/errors/skips; `all-java-unit.log` |
| Android Python selection | `python3 -B -m unittest discover -s scripts -p 'test_android*.py'`: 39 passed; `android-python-all.log` |

Repeated unit selections are not additional distinct regression coverage. Local
probes are not JUnit passes. The real runner uses `--suite auth --method
test05ExistingProgressUserLoginWithEmailAndOtp`, a private flat CONTENT Keychain
record, explicit primary goal/APK/serial and the isolated Appium URL. BASELINE's
distinct email only satisfies the runner's unused no-progress environment field
for this one method; no BASELINE login or readiness claim is made.
There is no fresh passing Bookmarks JUnit run after the final login/transition
changes. Bookmarks successes at initial head `6597749` remain historical evidence
in the earlier report, not validation of the current changes.

## 3. Assumptions and limitations

`should_show_goals=true` is expected and is not required to become false. The
ordinary handler requires a fixture-matching primary goal. It preserves an already
enabled additional-goals Continue; otherwise it uses the existing named
`Increase self confidence` option, never an arbitrary first answer. A resumed or
unrecognized question, third native question or full Web questionnaire is rejected.
Full new-user preparation remains a separate explicit opt-in API.

No training completion, FINISH, progress reset, purchase, restore-purchases,
entitlement mutation, catalog sweep or selecting-program/day API was used in this
continuation. Only identity-verified current-state reads without selectors were
used for comparison. The projection omits per-response `rating_page` configuration;
it is not an all-program completion snapshot. No iOS device was operated or its
primary baseline independently remeasured.

The Play ANR is directly observed; its root cause and any relation to the earlier
Google APIs close no-op are not established. `dumpsys activity lastanr` returned no
retained report despite the captured dialog/event, so no Android Java ANR stack is
claimed. JUnit stacks, screenshots and XML are retained. Resource recovery was a
separate changed-condition attempt, not an automatic retry policy.
The final diagnostic still could not dismiss E-workbook, so the transition fix is
unit-verified but not established as an end-to-end login fix. Manual Google Play
sign-in was offered by the user, then further testing was stopped at their request.
The agent did not perform Google sign-in, make a purchase or continue device checks.
The owned Appium process was stopped at handoff. The exact temporary observer
Keychain cache was deleted and its absence confirmed; reusable fixture records
were retained. The Play emulator was left untouched for the user's manual work.

## 4. Overall assessment

| Area | Score | Evidence / boundary |
| --- | ---: | --- |
| Requirements quality | 4/5 | Explicit user contract: two native questions, not full onboarding |
| Risk coverage | 2/5 | Expected login and safe feature scope, not complete Android parity |
| Test design | 3/5 | Positive/direct-dashboard/unexpected-question/failed-transition cases |
| Functional coverage | 2/5 | Goal transition observed; fresh returning-user Bookmarks replay deferred at user request |
| Non-functional coverage | N/A | No performance/security/accessibility audit; ANR investigation only |
| Automation architecture | 3/5 | Existing Page Objects, waits, fixture fields and runner reused |
| Reliability/flakiness | 2/5 | Captured app ANR and transition failures, no hidden retry |
| Diagnostics | 3/5 | Fresh per-attempt artifacts and current-state comparisons |
| Maintainability | 3/5 | Bounded handler, named secondary option, no dependency additions |
| CI/CD | N/A | Local runs, not a CI verification |
| Release readiness | 1/5 | Draft maintained; remaining gates not waived |

## 5. Coverage map

| Requirement / risk | Level and evidence | Boundary |
| --- | --- | --- |
| Two expected post-login goals | UI probe plus auth/goal unit tests | No full questionnaire consumed |
| Missing/unknown goal or extra questions | Unit negative paths | No default program/first-answer guess |
| Delayed offer → intro/Today transition | Four passing unit cases; live follow-up blocked at E-workbook | Actual Today still required; no live pass claimed |
| Runner data precedence | Two new Python tests | CLI → record → explicit environment, no gender default |
| iOS preservation | Current change scope excludes iOS sources; earlier audit remains historical | No iOS runtime operated or new test-body audit claimed |

## 6. Findings

| ID | Category | Severity / priority / confidence | Evidence / impact | Recommendation / owner / effort |
| --- | --- | --- | --- | --- |
| G-01 | Test contract, corrected | High / P1 / high | Blanket marker rejection misclassified expected native goals | Bounded ordinary-login handler; preserve historical failures / automation / S |
| G-02 | Synchronization, corrected | Medium / P1 / high for source gap | Handler previously broke immediately between screens; failed snapshot contains program intro | Explicit wait for next ready action/destination, preserve failure propagation / automation / S |
| G-03 | Fixture configuration, corrected | High / P1 / high | Gender default could choose a program unrelated to shared fixture | Remove invented goal fallback, use explicit metadata/environment / automation / S |
| G-04 | App/environment, unresolved root cause | High / P1 / high observation | Play dialog and `am_anr: No response to onStartJob` | Correlate app/SDK startup and device resources; do not accept ANR as login success / Android + QA / M |
| G-05 | App/environment, unresolved root cause | High / P1 / high observation | E-workbook close still failed after transition fix; billing API unavailable on the owned Play image | Diagnose offer-close behavior and billing environment when testing resumes; do not bypass offer with purchase or report a Bookmarks pass / Android + QA / M |

## 7. Missing and weak tests

Female live feature parity, isolated empty/first-tooltip roles, reversible save/delete,
saved-slide/grouping variants, fresh Android analytics/network evidence and genuine
upgrade/reinstall remain separate gates. Completion/recurrence need immutable
fixtures and are not run on shared iOS accounts. Payment cases remain excluded.
The latest user instruction defers further execution; these are outstanding
verification boundaries, not additional work performed during this handoff.

## 8. Test architecture assessment

Normal login calls a bounded Android-only handler. Existing full onboarding and its
explicit approval guard remain separate. Native goal selection is reused, with
arbitrary secondary fallback disallowed for ordinary login. Optional-action waiting
uses existing explicit waits and the same selectors as action dispatch; product
slides remain destinations, not controls to consume. No production/iOS code,
dependency, sleep, disabled test or new skip is introduced.

## 9. Release recommendation

**Insufficient evidence.** Keep Draft; main and PR #24 must remain unchanged.
Do not use persistent goals flag as a backend defect or prerequisite for merge.
Evaluate actual ordinary-login/Today/identity and affected feature results instead.

## 10. Prioritized action plan

| Horizon | Action | Owner / exit criterion |
| --- | --- | --- |
| Immediate | Publish corrected goal contract, tests and measured follow-up | Automation / PR #25 updated without merge |
| Deferred by user | Resolve remaining app/transition failures when test execution is requested again | Android + QA / fresh login reaches actual Today, same identity/state |
| Medium-term | Expand non-completing Bookmarks to both variants and prepared roles | Automation / independent login, precise assertions and verified isolation |
| Long-term | Isolate completion/update/evidence fixtures and establish CI gates | QA infrastructure / reproducible owned data, fresh artifacts |
