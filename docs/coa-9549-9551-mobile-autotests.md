# COA-9549 / COA-9551 mobile autotests

New JUnit4/Appium tests are in `tests.ProgramSwitchFeatureTests` and `tests.VideoPlayerFeatureTests`. They use the existing `CoreTestCase`, onboarding/account helpers, and Daily Plan Page Objects. Existing selector, player, and smoke tests remain in place.

## Scope

| Feature | Automated checks |
| --- | --- |
| COA-9549 | First tooltip absent before and shown after completing a Daily Plan item; first switch confirmation; Cancel keeps the program and requires confirmation again; confirmed switch shows second tooltip; OK closes it; later switch and relaunch do not repeat the flow; first tooltip for a new anonymous account after reinstall. |
| COA-9551 | Exact speed choices `x0.5, x0.75, x1, x1.25, x1.5, x2`; speed does not rewind and survives pause/background; seek to the middle and resume after leaving; ±15 boundary; independent positions of two lessons; reopening a lesson after 95% starts at the beginning. |

Audio quality, an incoming phone call, analytics events, switching between two linked accounts, and a same-account reinstall need a controlled device/event receiver or account fixture and are not asserted by these tests. On iOS the speed button cycles through values; the test records a full cycle. The iOS timeline has no accessible slider element in the captured build; the test taps its position relative to the two accessible time labels. A stable slider accessibility identifier would make this check less fragile. Android tooltips were visible in prior screenshots but absent from the ADB accessibility dump. The Page Object enables Appium's multi-window source and requires the tooltip text and OK button to be accessible; if the test fails there, the app needs accessible identifiers for these popup controls.

## Fixtures

COA-9549 requires a clean new-user start and one unfinished Daily Plan item. The test chooses the configured onboarding goal; by default it opens the first `Lesson 1` card, or uses `-Dcoach.program.itemTitle` to select a known accessible item. Set `-Dcoach.program.target` to a different program available in the selector. The main iOS test can attach to a freshly installed TestFlight Preprod build with `-Dcoach.program.freshStartPrepared=true`; for an already-onboarded 0-progress Today profile, use `-Dcoach.program.preparedToday=true`. An automated reinstall on iOS requires a signed local app artifact supplied as `-Dios.app`; a TestFlight-only installation cannot be reinstalled by Appium from its bundle ID. Run the two tests independently against fresh installations. The iOS flow sends the lesson rating to complete the item; closing the rating leaves it incomplete.

COA-9551 requires dedicated premium test-account credentials in `COACH_COA9551_EMAIL` and `COACH_COA9551_OTP`, or a device already logged into a paid test profile with `-Dcoach.video.useCurrentAccount=true`. The latter mode preserves that login during teardown. Supply `-Dcoach.video.lessonTitle` and, for the two-lesson test, `-Dcoach.video.secondLessonTitle`. Both titles must identify visible Daily Plan items containing a video. The 95% test needs a lesson of at least three minutes. Use a fixture whose lessons can be replayed; tests normalize their own speed/position when possible. Never put credentials in commands or source control.

## Targeted runs

Start Appium 2 at the root path (`appium --base-path /`). Android examples use the APKs supplied for this task; they are named `manProd` and should not be described as Preprod:

```sh
mvn test -Dtest=tests.ProgramSwitchFeatureTests#testFirstTooltipCancelConfirmAndNoRepeat \
  -Dplatform=android -Dandroid.udid=emulator-5554 \
  -Dandroid.app=/Users/deedles/Downloads/app-1.41.2-manProd-release.apk \
  -Dandroid.noReset=false -Dandroid.fullReset=true \
  -Donboarding.goal='Beat erectile dysfunction' \
  -Dcoach.program.itemTitle='Magnesium: for stronger erections' \
  -Dcoach.program.target='Sex Is a Skill'

mvn test -Dtest=tests.VideoPlayerFeatureTests#testSpeedOptionsAndPauseBackground \
  -Dplatform=android -Dandroid.udid=emulator-5554 \
  -Dandroid.noReset=true -Dcoach.video.useCurrentAccount=true \
  -Dcoach.video.lessonTitle='Main principles of resistance training'

mvn test -Dtest=tests.VideoPlayerFeatureTests#testPositionsBelongToIndividualLessons \
  -Dplatform=android -Dandroid.udid=emulator-5554 \
  -Dandroid.noReset=true -Dcoach.video.useCurrentAccount=true \
  -Dcoach.video.lessonTitle='Three rules to remember' \
  -Dcoach.video.secondLessonTitle='Main principles of HIIT'
```

For iOS TestFlight, prepare a fresh Preprod installation before the COA-9549 main test. Use the appropriate bundle ID and the documented real-device capabilities, then pass `-Dcoach.program.freshStartPrepared=true` or `-Dcoach.program.preparedToday=true` for a prepared Today profile. The COA-9551 class attaches to a configured premium account and uses the lesson titles available on that platform. Build numbers in the Jira comments are #3435/#3437 for COA-9549 and the later #3455/#3457 for COA-9551. To attach to the already signed-in male production build on iPhone 11:

```sh
mvn test -Dtest=tests.VideoPlayerFeatureTests#testSpeedOptionsAndPauseBackground \
  -Dplatform=ios -Dios.udid=00008030-000929442E01802E \
  -Dios.bundleId=com.vamapps.The-Coach -Dios.noReset=true \
  -Dcoach.video.useCurrentAccount=true \
  -Dcoach.video.lessonTitle='Three rules to remember'
```

Runtime on 2026-09-29: Android 1.41.2 on the Google Play API 36 emulator, with the Google Play account retained, passed onboarding after selector updates. A guarded test purchase (`-Dcoach.program.purchaseFixture=true -Dgoogleplay.licenseTester=true -Dpurchase.allow=true`) reached Google Play, which reported that billing is paused in Russia. After VPN was enabled, the same fresh-install test was run again; Google Play returned the same billing-paused message. Premium Daily Plan content could not be activated, so the full tooltip flow was blocked before its assertion. The latest Android error screenshot is `target/screenshots/testFirstTooltipCancelConfirmAndNoRepeat.png`. In a later signed-in Android 1.41.3 profile, completing a video lesson did show the first "Tap to explore other programs" tooltip on Today; this is partial evidence only, on a different build, and does not verify the confirmation or second tooltip. On iPhone 11 (`00008030-000929442E01802E`) male Preprod 3465, later than the Jira build, the first Daily Plan lesson was completed and marked with an orange check, but the first tooltip did not appear within 15 seconds. The targeted test failed at that assertion. This observation is for 3465, not the Jira-specified 3435 build.

Android 1.41.3 was then signed into an existing Coach test profile, without removing the Google Play account. All four targeted COA-9551 tests passed individually: `testSpeedOptionsAndPauseBackground`, `testSeekBoundsAndResumeFromSavedPosition`, `testFrom95PercentReopensAtStart` with the 5:16 "Main principles of resistance training" video, and `testPositionsBelongToIndividualLessons` with "Three rules to remember" and "Main principles of HIIT" in Module 3 Stage 2. These are Android `manProd` results, not Preprod results. The video Page Object handles Android's auto-hiding controls and the lesson-rating screen that can appear after video completion.

The installed iOS production builds are male 3462 and female 3463. Paid test profiles were signed in without storing credentials in the repository. In male 3462, all four COA-9551 tests passed individually: `testSpeedOptionsAndPauseBackground`, `testSeekBoundsAndResumeFromSavedPosition`, `testFrom95PercentReopensAtStart`, and `testPositionsBelongToIndividualLessons`. The speed test verified the six specified values against `Renew Her Arousal` and `Three rules to remember`. The other tests used the 9:47 `Three rules to remember` video and, for position isolation, the 2:32 `Main principles of HIIT` video. The iOS seek implementation uses the two visible time labels to locate the unlabeled timeline; one tap may reveal hidden controls and a second tap applies the seek. The boundary test pauses the original lesson near its end before pressing +15 and accepts either its final in-range position or the next lesson opening. Reopen assertions allow for actual playback time while Appium navigates. The female Today lessons inspected were text-only, so no female player assertion was run. Production builds are newer and a different environment from the specified Preprod builds.
