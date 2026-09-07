# The Coach: Men's Health tracker
 The Coach life style app Test Automation Project. Tests for iOS platform. 
Java/Appium/Maven/Jenkins/Allure

## Run iOS tests for a specific build

The bundle ID can be supplied either as the `ios.bundleId` Maven property or
as the `IOS_BUNDLE_ID` environment variable. The Maven property takes
precedence over the environment variable.

```bash
mvn test -Dplatform=ios -Dios.bundleId=com.vamapps.preprod.The-Coach
```

```bash
IOS_BUNDLE_ID=com.vamapps.The-Coach mvn test -Dplatform=ios
```

Known bundle IDs:

- `com.vamapps.preprod.The-Coach`
- `com.vamapps.The-Coach`
- `com.vamapps.The-Coach-for-her`

If neither option is supplied, `com.vamapps.preprod.The-Coach` is used.

## Smoke and isolated integration suites

The P0 smoke contract is implemented by `suites.SmokeSuite` and covers
clean-install onboarding, Welcome/Login, Today/Daily Plan, Kegel and Profile
with Logout. Explore, Shop and update coverage are implemented by the P1
`suites.ReleaseSmokeSuite`. The
one-time push permission flow and StoreKit flows are intentionally separate:

```bash
mvn test -Dtest=suites.SmokeSuite -Dplatform=ios \
  -Dios.app=/path/to/The-Coach.app \
  -Dios.fullReset=true -Dios.noReset=false \
  -Dios.onboarding.steps=id:<step1>,id:<step2>
```

```bash
mvn test -Dtest=suites.PushPermissionSuite -Dplatform=ios \
  -Dios.app=/path/to/The-Coach.app \
  -Dios.fullReset=true -Dios.noReset=false \
  -Dios.onboarding.steps=id:<step1>,id:<step2>
```

```bash
mvn test -Dtest=suites.StoreKitPurchaseSuite -Dplatform=ios \
  -Dstorekit.sandbox=true -Dstorekit.allowPurchases=true \
  -Dios.onboarding.steps=id:<step1>,id:<step2>
```

```bash
mvn test -Dtest=suites.ReleaseSmokeSuite -Dplatform=ios \
  -Dios.app=/path/to/old/The-Coach.app \
  -Dios.update.app=/path/to/new/The-Coach.app \
  -Dios.noReset=true -Dios.fullReset=false
```

The release suite needs two builds with the same bundle ID for the update
scenario. Credentials should be injected through the environment variables in
`docs/smoke-environment.md`; they are intentionally absent from the command
line.

Firebase launch configuration is read-only for test analysis. The app-side
accessibility/test-id contract and the expected smoke cases are documented in
`docs/smoke-automation-matrix.md`.

Before running Smoke or Release Smoke, validate the secret-backed accounts and
deterministic fixture metadata without printing credential values:

```bash
./scripts/validate_smoke_test_data.sh
```

Run `./scripts/prepare_smoke_fixture.sh` before the suite when the environment
provides the configured fixture reset hook. It fails closed for
`COACH_FIXTURE_RESET_MODE=backend_api` unless that hook is available.

`TEST_ISOLATION_MODE=logout` is the default. Set it to `reinstall` only when
`IOS_APP`/`-Dios.app` is available and each test must reinstall the app. The
fixture reset/seed operation is external to this repository; its contract is
described in `docs/smoke-environment.md`. For the quick P0 path, inject the
approved existing-progress account into `COACH_EXISTING_PROGRESS_EMAIL` and
omit the optional Kegel account variables to reuse that account. The P1 PDF
paywall test remains blocked until the no-PDF account is provisioned.

## iOS IPA on a real device

`/Users/deedles/Downloads/The Coach.ipa` is a device-only `arm64` build:
`CFBundleIdentifier=com.vamapps.The-Coach`, `DTPlatformName=iphoneos`, minimum
iOS `15.0`. It cannot be installed in an iOS Simulator. The real-device
profile is kept separate from the Android profile and from the existing
pre-production iOS bundle defaults.

Start Appium 2 with the root path used by this project:

```bash
appium --base-path /
```

Run a minimal iOS connection check against the IPA:

```bash
IOS_UDID=00008150-00084CDE0CF0401C \
./ci-scripts/run-ios-ipa.sh \
  -Dtest=tests.SmokeTests#testLoginWithEmptyEmailKeepsContinueDisabled \
  -Dios.deviceName="iPhone Daria" \
  -Dios.platformVersion=26.6 \
  -Dios.noReset=true -Dios.fullReset=false
```

The launcher validates the IPA and passes its actual Bundle ID to Maven. To
use another build, set `IOS_APP=/path/to/build.ipa`; to use another phone,
override `IOS_UDID`, `IOS_DEVICE_NAME` and `-Dios.platformVersion`. The
existing real-device signing overrides remain available:
`-Dios.xcodeOrgId=...`, `-Dios.xcodeSigningId=...` and
`-Dios.useNewWDA=false`.

The same capabilities are stored in
[`appium/ios-ipa-real-device.json`](/Users/deedles/IdeaProjects/The-Coach/appium/ios-ipa-real-device.json)
and in Appium Inspector as `The Coach iOS IPA`. In Inspector use Remote Host
`127.0.0.1`, port `4723`, path `/`, and keep `appium:noReset=true` for a
non-destructive first connection. Set `appium:fullReset=true` and
`appium:noReset=false` only for an intentional clean install.

This particular IPA contains an App Store/distribution provisioning profile
without a `ProvisionedDevices` list. If Appium reports an installation or
verification error, request an Ad Hoc/development/enterprise IPA for the
phone, or install the exact signed build on the phone first and attach by
Bundle ID without the `appium:app` capability.

## iOS Simulator (like an Android emulator)

The iOS equivalent of an Android emulator is an Xcode **Simulator**. It does
not accept the device IPA above: the Simulator requires a separate `.app`
bundle built for `iphonesimulator`. The local machine has an `iPhone 17 Pro`
Simulator on iOS `26.5` with UDID
`00CA21E8-4A92-4607-A941-E5FD2E29DAC5`; no `iPhone Daria` is used by this
profile.

Start Appium and run a Simulator build like this:

```bash
appium --base-path /
```

```bash
IOS_SIMULATOR_APP="/Users/deedles/Downloads/The Coach.app" \
./ci-scripts/run-ios-simulator.sh \
  -Dtest=tests.SmokeTests#testLoginWithEmptyEmailKeepsContinueDisabled
```

The launcher boots the selected Simulator, validates that the app is really
compiled for `iphonesimulator`, and passes its Bundle ID and Simulator UDID to
Maven. Override `IOS_SIMULATOR_APP`, `IOS_SIMULATOR_UDID`,
`IOS_SIMULATOR_DEVICE_NAME` or `IOS_SIMULATOR_PLATFORM_VERSION` when using a
different Simulator. The ready-to-copy capabilities are in
[`appium/ios-simulator.json`](/Users/deedles/IdeaProjects/The-Coach/appium/ios-simulator.json)
and are saved in Appium Inspector as `The Coach iOS Simulator`.

The current `/Users/deedles/Downloads/The Coach.app` was inspected locally and
is marked `DTPlatformName=iphoneos`/`iPhoneOS`, so it is still a real-device
build despite the `.app` extension. The launcher will reject it until a
separate `.app` compiled for `iphonesimulator` is placed at this path (or
provided through `IOS_SIMULATOR_APP`).

The real-device IPA launcher and `The Coach iOS IPA` Inspector profile remain
available separately.

## Android Explore 1.40.3

The configurable Explore contract is covered by
`tests.AndroidExploreTests`. Appium 2 is expected at the root path (`/`), and
the Android package/activity default to `com.vamapps.thecoach` and
`com.vamapps.thecoach.MainActivity`.

Inject the existing-progress account through environment variables; do not
place credentials in source files or command history:

```bash
COACH_EXISTING_PROGRESS_EMAIL="..." \
COACH_EXISTING_PROGRESS_OTP="..." \
mvn test -Dtest=tests.AndroidExploreTests -Dplatform=android \
  -Dandroid.app=/path/to/app-1.40.3-manProd-release.apk \
  -Dandroid.deviceName=TheCoach_API_30_ARM \
  -Dandroid.udid=emulator-5554 \
  -Dandroid.noReset=true -Dandroid.fullReset=false
```

The tests validate the configured section titles, removal of the legacy
programs/Courses blocks, the three card templates, card title availability
for the `title` analytics property, navigation to lesson/practice or WebView
destinations, and the Quick Tips `coach_video` redirect into the native video
player.


## Ready Jira/Xray iOS autotests

`suites.TestModelAutomationSuite` contains eight completed scenarios:
COA-7947, COA-8235, COA-8517, COA-8228, COA-8230, COA-8227, COA-8232 and COA-8231.
COA-8229 is also checked inside COA-8227. All scenarios retain their Jira keys in
Allure. The unfinished authorization/customization scenarios are not part of this suite.

The explicit-case macOS runner reads credentials from Keychain and passes them
through the child environment. Start Appium and supply a Simulator build:

```bash
python3 scripts/run_test_model_local.py COA-8232 COA-8231 --provision --app "/path/to/The Coach.app" --maven=-q
python3 scripts/run_test_model_local.py COA-7947 COA-8235 COA-8517 COA-8228 COA-8230 COA-8227 COA-8232 COA-8231 --app "/path/to/The Coach.app" --maven=-q
```

Provisioning is explicit and preserves already confirmed accounts. Read-only
selector tests reuse COA-8235. Program changes use dedicated UID-verified accounts
and restore the source program, day and progress even on failure. COA-8517 uses
its own resettable recovery fixture and checks every applicable program day.
Tests run sequentially against one Simulator; the runner never implicitly starts
the full suite. Video checks require `ffmpeg`, `xcrun` and Python 3.

See [ready coverage, setup and validation](docs/ready-ios-autotests.md) and
[Today selector implementation](docs/today-program-selector-automation.md).
Generated screenshots, recordings, cache evidence and reports stay local under `target/`.
