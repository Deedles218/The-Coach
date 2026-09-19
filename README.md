# The Coach: Men's Health tracker
 The Coach life style app Test Automation Project. Tests for iOS and Android.
Java/Appium/Maven/Jenkins/Allure

## Настройка и первый запуск

**Начните с [пошаговой инструкции для тестировщика](docs/local-setup.md).**
В ней описаны установка инструментов, настройка iOS Simulator / iPhone / Android,
подготовка тестовых аккаунтов, первый тест, запуск наборов и просмотр отчётов.
Пути, UDID и профили ниже содержат примеры с компьютера автора; для нового
компьютера используйте свои значения по инструкции.

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

## New-user onboarding paywall suite (Android and iOS)

`tests.OnboardingPaywallTests` implements the five onboarding/paywall cases:
top-plan purchase, bottom-plan purchase, Terms & Privacy, the exact renewal
disclosure, and Restore. Android uses a clean install of 1.40.8 on Android 16;
iOS attaches to the TestFlight app by bundle ID.

Purchase confirmation fails closed. For iOS, pass both
`-Dstorekit.sandbox=true` and `-Dpurchase.allow=true`. For Android, use a
Google Play license-tester account on a Google Play system image and pass both
`-Dgoogleplay.licenseTester=true` and `-Dpurchase.allow=true`. Android also
checks that the Play dialog is visibly marked as a test purchase/order/card
before it can tap Subscribe.

Exact commands, environment limitations and the accessibility contract are
documented in
[`docs/onboarding-paywall-e2e.md`](docs/onboarding-paywall-e2e.md).
The latest device-run matrix and risk-based QA backlog are in
[`docs/onboarding-paywall-test-report-2026-08-30.md`](docs/onboarding-paywall-test-report-2026-08-30.md).

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

## Reviewed Jira/Xray test-model suite

`suites.TestModelAutomationSuite` is the dedicated entry point for the original eight
reviewed cases: COA-7947, COA-7949, COA-7950, COA-8235, COA-8511, COA-8512,
COA-8517 and COA-8518, plus the iOS selector cases COA-8228, COA-8230, COA-8227,
COA-8232 and COA-8231.
Each test keeps its Jira key in Allure via `@Issue` and its test name.
COA-8518 is currently deferred by the user: it applies only to Coach for Her.
It is ignored by JUnit, and the local runner refuses to run/provision it.

```bash
./scripts/validate_test_model_data.sh
mvn test -Dtest=suites.TestModelAutomationSuite -Dplatform=ios
```

For local runs, the explicit-case runner reads the approved account registry
from macOS Keychain and supplies credentials through the child environment:

```bash
python3 scripts/run_test_model_local.py --status
python3 scripts/run_test_model_local.py COA-8517 --provision
python3 scripts/run_test_model_local.py COA-8235 COA-8517 --maven=-q
python3 scripts/run_test_model_local.py COA-8230 COA-8228 COA-8227 --maven=-q
python3 scripts/run_test_model_local.py COA-8232 COA-8231 --provision --maven=-q
python3 scripts/run_test_model_local.py COA-8232 COA-8231 --maven=-q
python3 scripts/run_test_model_local.py COA-8511 --inspect-premium --maven=-q
python3 scripts/run_test_model_local.py COA-8512 --inspect-premium --inspect-programs --maven=-q
python3 scripts/run_test_model_local.py COA-8511 COA-8512 --maven=-q
```

Read-only selector cases reuse the COA-8235 returning-user fixture and run sequentially.
COA-8232/8231 use separate provisioned accounts, verify the UID before switching
Last Longer to Keep It Hard, and restore the original program, day and progress
in `finally`, including on failure. They never change the shared COA-8235 account.
Motion checks require `ffmpeg` and `xcrun` in PATH. COA-8229's outside-tap
animation is checked within COA-8227. See [selector coverage and evidence](docs/today-program-selector-automation.md).

Provisioning preserves confirmed accounts and verifies email confirmation and
re-login before recording success. It does not delete accounts or buy subscriptions.
COA-8235 needs no nonzero-progress fixture. COA-8517 prepares its own removed-card
state through the documented preprod API, restores through the UI, verifies
the rendered day-2 card and every applicable program day, then returns the
fixture to day 2. COA-8518 requires fresh simulator
analytics evidence; an old log file can no longer make it pass or be skipped.

COA-8511/8512 now use the user-approved Kegel Challenge shared Premium fixture:
UI Restart Program, API preparation of the first move, and the tested second
move through UI on day 2. COA-8512 additionally requires exactly one pending task.
The original selected program/day/progress are checked during restoration.
Run these cases sequentially with the explicit local command above. See
[the Kegel execution report](docs/test-model-kegel-2026-09-07.md) and
[the five-case completion audit](docs/test-model-completion-audit-2026-09-07.md).
The [7 September follow-up](docs/test-model-follow-up-2026-09-07.md) records the
confirmed CONNECT authorization flow, current validation mismatches, anonymous
fixture ownership guard, and fresh entitlement checks for the remaining cases.
The approved shared Premium account is stored separately in Keychain. The
`--inspect-premium` mode verifies its identity/access without resetting or
mutating program state; this inspection is not an end-to-end case result.
Adding `--inspect-programs` temporarily views Overall Health/Kegel and requires
restoration of the original program, day and progress. Normal COA-8511/8512
runs require the separate `kegelMutationScope` approval and cannot silently use old aliases.
COA-8512 may use any suitable practice, but still requires exactly one pending
task and unchanged observed progress throughout the scenario. The user clarified
that the module-specific baseline replaces literal 2%. Overall Health variant,
day and progress reset are explicitly allowed on the approved shared account;
Kegel Challenge preparation was subsequently approved too, in a separate scope.
Other programs and account deletion remain outside fixture scope.

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

## Modules (isolated iOS suite)

`suites.ModulesSuite` adds module header, intra-module navigation and locked-next-module
checks from the September handoff. It uses its own COA-9044 account through the
existing Keychain runner. See [coverage, fixture requirements and commands](docs/modules-automation.md).
Completion, calendar transitions and the device matrix remain explicitly uncovered.

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


## Previously validated Jira/Xray iOS subset

The earlier ready-only publication validated these eight scenarios:
COA-7947, COA-8235, COA-8517, COA-8228, COA-8230, COA-8227, COA-8232 and COA-8231.
COA-8229 is also checked inside COA-8227. All scenarios retain their Jira keys in
Allure. The full suite now also includes the authorization/customization tests
described above: COA-7949/7950 retain their failing expectations on the current
build, COA-8511/8512 passed with Kegel Challenge, and COA-8518 remains deferred.
The user accepted keeping these tests as-is for inspection in a later run report.
Use the explicit-case command below to run only the earlier validated subset.

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
