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
