# Masterclasses

The suite covers opening an unpaid offer, purchased access persistence, player
controls, and the locked next day before completion. Configure and execute each
supported iOS/Android and Him/Her build explicitly.

New `tests.MasterclassTests` reuses CoreTestCase and MainPageObject. Existing tests
are unchanged. By default it requires an already authenticated account and does not log it
out, reset app data, or buy anything. Playback can record viewing progress, so
use an isolated purchased fixture for that test. It requires at least 120 seconds
remaining and closes the iOS player during teardown. Run only the methods
appropriate to the fixture; no skip is reported as coverage.

Optional `-Dmasterclass.login=true` uses the existing CoachFlow authentication
helper and `COACH_EXISTING_PROGRESS_EMAIL` / `COACH_EXISTING_PROGRESS_OTP` from
the environment. This can normalize/logout the prior iOS account before login.
Leave the flag off for a user-prepared purchased session. Complete any mandatory
post-auth questionnaire separately. Only after approving test answers, use
`-Dmasterclass.prepareOnboarding=true` on Android to resume the questionnaire,
select Boost overall health and reuse the existing first-answer helper. Leave
this flag off after fixture preparation. It does not purchase optional upsells.

Required properties: `masterclass.card`, `masterclass.course`, `masterclass.lesson`,
`masterclass.entitlement` (`unpaid`/`purchased`). Unpaid additionally requires
`masterclass.offerTitle`, `masterclass.price` (exact configured visible text).
Android uses system Back by default; `masterclass.backLocator` (typed id:/xpath:)
can override it for a build with a dedicated back control. The return assertion
checks the WebView heading, since the catalog card can share the course title.
Gating requires `masterclass.nextDay`, `masterclass.lockedMessage`,
`masterclass.currentDay`, and an incomplete first-day purchased fixture.
Player requires `masterclass.positionLocator`, optional `positionAttribute`, and
platform-specific `playerStartLocator` / `player.Pause`, `player.Play`,
`player.Go Forward 10 Seconds`, `player.Close` typed locators. iOS defaults for
controls cover observed AVPlayer English labels, including iOS 27 Play/Pause,
Skip Forward and Close Button. Pausing is asserted using a stable playback clock.
Android controls and `masterclass.durationLocator` (optional `durationAttribute`)
must be observed on its purchased course before configuring them.

Example purchased female Dev iOS:

```sh
mvn -Dtest=MasterclassTests#testPurchasedAccessSurvivesReopenAndRestart test \
  -Dplatform=ios -Dios.bundleId=com.vamapps.develop.The-Coach-for-her \
  -Dios.platformVersion=27.0 -Dios.noReset=true \
  -Dios.xcodeOrgId=QS3D868T5W -Dios.xcodeSigningId='Apple Development' \
  -Dmasterclass.entitlement=purchased -Dmasterclass.card='FirstMasterClass' \
  -Dmasterclass.course='The Art of Sensual Massage' \
  -Dmasterclass.lesson='The Art of Erotic Massage'
```

Android: same method and fixture properties, `-Dplatform=android`, actual udid,
platformVersion, appPackage/appActivity, and `-Dandroid.app=/path/to.apk`.
Do not infer a masterclass entitlement from the general Premium subscription.

Verified purchased Android Him 1.41.10 on the Android 16 emulator, using the
already authenticated session:

```sh
mvn -Dtest=MasterclassTests#testPurchasedAccessSurvivesReopenAndRestart test \
  -Dplatform=android -Dandroid.udid=emulator-5558 -Dandroid.platformVersion=16 \
  -Dmasterclass.entitlement=purchased \
  -Dmasterclass.card='The Art of Sensual Massage' \
  -Dmasterclass.course='The Art of Sensual Massage' \
  -Dmasterclass.lesson='The Art of Erotic Massage'
```

The negative day gate also passed with `masterclass.nextDay=2`,
`masterclass.lockedMessage=Complete Day 1 to unlock`, and
`masterclass.currentDay=Day 1 of 38`. The video poster is exposed as
`xpath://*[@resource-id='videoThumb']`; native `id:videoThumb` is auto-prefixed by
UiAutomator2 and does not resolve this WebView element. Video playback was
observed visually, but its clock and controls are absent from the native tree
and this release exposes only NATIVE_APP. The fallback accessibility text
"Unable to play media." is present even while frames visibly advance; it alone
must not be treated as a playback failure. Full Android player assertions need
accessible controls or a separate implementation using a debuggable WebView.

Purchase testing still needs the exact product/price and sandbox or Google Play license
tester fixture. The positive completion/unlock/restart path needs a separate
resettable purchased-progress fixture and the agreed completion criterion. The
implemented gate test covers only the negative pre-completion path. Neither scenario
is fully automated by the current suite. Compilation alone does not validate
platform locators, entitlements, or backend behavior.

The supplied `The Coach_for_him.app` is iPhoneSimulator 1.13.33, not an installable
physical-device build. Its simulator run cannot validate current TestFlight.
