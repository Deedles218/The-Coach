# Smoke environment contract

## Confirmed by the repository

| Parameter | Value | Source / note |
|---|---|---|
| Primary platform | iOS | `Platform` creates `IOSDriver`; Android defaults to an unrelated Wikipedia app and is not a Coach target. |
| Automation | Appium + XCUITest | `pom.xml`, `Platform.java` |
| Java / test runner | Java 8 / JUnit 4 | `pom.xml` |
| Default bundle ID | `com.vamapps.preprod.The-Coach` | Override with `-Dios.bundleId` or `IOS_BUNDLE_ID`. |
| Default device | `iPhone Daria`, iOS `26.6` | Override with `IOS_DEVICE_NAME`, `IOS_PLATFORM_VERSION`, `IOS_UDID`. |
| App artifact | unknown until CI supplies it | Required for real clean install: `-Dios.app=/path/to/The-Coach.app` or `IOS_APP`. |
| Build number | unknown | Must be recorded by CI as `BUILD_NUMBER`/Allure environment metadata. |
| Appium URL | `http://127.0.0.1:4723/` | Appium 2 root path; override with `APPIUM_URL`. |

The local IPA `/Users/deedles/Downloads/The Coach.ipa` is a real-device-only
`arm64` build with Bundle ID `com.vamapps.The-Coach`. It must not be sent to an
iOS Simulator; a Simulator run requires a separate `.app` built for
`iphonesimulator`. The project keeps the existing real-device UDID/signing
overrides (`IOS_UDID`, `IOS_XCODE_ORG_ID`, `IOS_XCODE_SIGNING_ID`) intact.

For local Simulator execution, the configured default is `iPhone 17 Pro` on
iOS `26.5`, UDID `00CA21E8-4A92-4607-A941-E5FD2E29DAC5`. Use
`ci-scripts/run-ios-simulator.sh` with `IOS_SIMULATOR_APP` pointing to the
separate Simulator `.app`; this launcher passes Simulator-specific
`ios.deviceName`, `ios.platformVersion` and `ios.udid` overrides, so it does
not select `iPhone Daria` and does not change the real-device defaults.
The configured local path is `/Users/deedles/Downloads/The Coach.app`, but the
current artifact at that path is marked `iphoneos` and must be replaced with a
build marked `iphonesimulator` before a Simulator session can start.

## Secret-backed test data

No email or OTP is stored in Java source. The tests read the following values
from Maven properties or the CI secret store; Maven properties take precedence:

| Purpose | Maven property | Environment variable |
|---|---|---|
| Existing-progress account email | `coach.existingProgress.email` | `COACH_EXISTING_PROGRESS_EMAIL` |
| Existing-progress account OTP | `coach.existingProgress.otp` | `COACH_EXISTING_PROGRESS_OTP` |
| Valid email without progress (authorization suite) | `coach.validEmailWithoutProgress` | `COACH_VALID_EMAIL_WITHOUT_PROGRESS` |
| Invalid email test value | `coach.invalidEmail` | `COACH_INVALID_EMAIL` |
| Kegel-player account email (optional) | `coach.kegelPlayer.email` | `COACH_KEGEL_PLAYER_EMAIL` |
| Kegel-player account OTP (optional) | `coach.kegelPlayer.otp` | `COACH_KEGEL_PLAYER_OTP` |
| Account without PDF entitlement email (P1 only) | `coach.noPdfEntitlement.email` | `COACH_NO_PDF_ENTITLEMENT_EMAIL` |
| Account without PDF entitlement OTP (P1 only) | `coach.noPdfEntitlement.otp` | `COACH_NO_PDF_ENTITLEMENT_OTP` |

OTP must be obtained only through the approved test-account mailbox/API or a
secret-backed fixed test OTP configured by the test environment. It must not be
printed, committed, added to Allure labels, or passed in a public CI command.
For the quick P0 path, configure the approved existing-progress account in
`COACH_EXISTING_PROGRESS_EMAIL`; set `COACH_DAILY_PLAN_DAY=1` for the agreed
day-one fixture. The Kegel email/OTP variables may be omitted,
in which case Smoke Kegel deliberately reuses that account.
The full `KegelExerciseTests` regression class requires the dedicated Kegel
email/OTP variables and must not fall back to the protected existing-progress
account.

## Deterministic fixture contract

The suite requires the following non-secret fixture metadata. Values are read
from Maven properties or the CI environment, with Maven properties taking
precedence:

| Purpose | Maven property | Environment variable |
|---|---|---|
| Fixture bundle identifier (optional for `prebuilt`) | `coach.fixture.id` | `COACH_FIXTURE_ID` |
| Fixture environment (optional for `prebuilt`) | `coach.fixture.environment` | `COACH_FIXTURE_ENVIRONMENT` |
| Daily Plan fixture identifier (optional for `prebuilt`) | `coach.fixture.dailyPlan.id` | `COACH_DAILY_PLAN_FIXTURE_ID` |
| Expected current Daily Plan day | `coach.fixture.dailyPlan.day` | `COACH_DAILY_PLAN_DAY` |
| Kegel fixture identifier (optional for `prebuilt`) | `coach.fixture.kegel.id` | `COACH_KEGEL_FIXTURE_ID` |
| No-PDF entitlement fixture identifier (P1 only) | `coach.fixture.noPdfEntitlement.id` | `COACH_NO_PDF_ENTITLEMENT_FIXTURE_ID` |
| Fixture reset strategy | `coach.fixture.reset.mode` | `COACH_FIXTURE_RESET_MODE` |
| Backend reset hook (only for `backend_api`) | n/a | `COACH_FIXTURE_RESET_SCRIPT` |

The P0 fixture must provide:

- an existing-progress user with a visible Today / Daily Plan;
- the configured current day with Daily Lessons, Daily Practice and a Kegel
  practice;
- a Kegel practice available to the approved Smoke account; a dedicated
  Kegel-player account is optional for the quick path because the test exits
  before completion and must not change progress;
- a Kegel stretching flow that reaches the completion popup and exposes the
  `LET’S GO!` action before the main player.

The StoreKit Sandbox account and active/expired subscription state belong to
the separate purchase/restore suite, not to P0 Smoke.

The separate no-PDF user and entitlement fixture are P1 prerequisites. The P1
paywall test is currently blocked and is not executed until those values are
provided.

`COACH_FIXTURE_RESET_MODE` is one of `backend_api`, `prebuilt` or
`clean_install`. For `prebuilt`, fixture IDs default to non-secret labels when
they are not supplied; the account state and expected current day still must
already exist. The repository currently has no fixture provisioning API or
backend seed script, so the selected reset/seed operation must be supplied by
the environment owner and executed before the suite. For `backend_api`, expose
an executable `COACH_FIXTURE_RESET_SCRIPT`; the hook receives only fixture IDs
and the environment name as positional arguments, never account credentials.
The Java contract and preflight script validate that the fixture is declared;
they do not mutate Firebase, production data or the StoreKit account.

Run the preflight without exposing credentials:

```bash
./scripts/validate_smoke_test_data.sh
```

The default scope is P0. Validate the later paywall prerequisites explicitly
with `SMOKE_DATA_SCOPE=p1 ./scripts/validate_smoke_test_data.sh`.

To run the configured reset/seed hook:

```bash
./scripts/prepare_smoke_fixture.sh
```

`prebuilt` performs no backend mutation. `clean_install` delegates app-state
reset to the iOS capabilities. `backend_api` fails closed until the owner
provides the executable reset hook.

## Test isolation and repeatability

`TEST_ISOLATION_MODE=logout` is the default. After each iOS test the base test
case dismisses transient app prompts, closes an auth flow when possible and
logs out an authorized session. `TEST_ISOLATION_MODE=reinstall` additionally
removes and reinstalls the configured app after each test; it requires
`IOS_APP`/`-Dios.app` and is intended for suites that need a full local app
state reset. Neither mode replaces the external fixture reset operation.

The one-time iOS push permission is intentionally not reset by the normal
Smoke lifecycle. Use the separate clean-install/permissions suite when the OS
permission state itself must be tested.

The repository currently has no fixture provisioning API or backend seed script.
Until one is supplied, the secret-backed accounts plus the reset strategy are
environment preconditions, not proof that the backend state was recreated.

The Release Smoke update case also requires an old build and a new build with
the same bundle ID. Pass the old build through `-Dios.app`/`IOS_APP` and the
new build through `-Dios.update.app`/`IOS_UPDATE_APP`; both reset capabilities
must remain in the non-reset state.

## Firebase configuration (read-only)

The launch sequence is controlled outside this repository. The names observed
in the local Xray export are:

- `launch_config`;
- `push_notification_view_config`;
- `onboarding_config`;
- `paywall_v2`.

The Firebase Console URL supplied for this work is a read-only inspection
source. No remote values are modified by the automation project. The exact
flag values, subscription state, and push/paywall ordering remain `unknown`
until an authenticated read-only Firebase session is available.

## Clean install contract

P0 clean-install smoke requires all of the following:

```text
-Dios.fullReset=true
-Dios.noReset=false
-Dios.app=/path/to/The-Coach.app
-Dios.onboarding.steps=id:<step1>,id:<step2>,...
```

The test verifies Start screen → configured onboarding → in-app push prompt
with the authorized Daily Plan underneath it. The separate push suite
additionally verifies allowing the in-app prompt and the iOS system permission
dialog. A scheduled remote push cannot be proven by a single local UI session
and needs a notification-provider/device integration check.

## Required CI metadata

CI should publish at least:

- app artifact path, bundle ID and build number;
- device name, UDID and iOS version;
- Firebase environment name and a read-only flag snapshot;
- fixture identifier (without credentials);
- Appium server log path;
- `target/screenshots`, `target/page-source` and `target/appium-logs` on failure;
- Allure results and the exact Maven command.
