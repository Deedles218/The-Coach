# COA-9222 — Android email enforcement

Android reuses the reviewed iOS Xray scenarios through `EmailEnforcementScreen` and
`AndroidEmailEnforcementTests`. Only the UI implementation and installation/account
reader differ. The Android implementation task is COA-9619. No Android application
code or dependencies are changed.

## Scope

| Xray | Independent datasets per variant | Check |
| --- | --- | --- |
| COA-9411 | N | Free email, OTP, Change email validation, same account/subscription/progress, suppression after confirmation/restart |
| COA-9424 | N | Occupied email offers Login without linking or switching the anonymous account |
| COA-9702 | N | Actual Support destination, Back to mandatory form, then full confirmation |
| COA-9696 | E1/cross, E1/later | E1 → E2 warning → E3; E3 persists after restart |
| COA-9699 | E1, E2, E3 | Kill while the form is shown; next stage without dismissal |
| COA-9701 | E1, E2 | Three Explore/Today trips in one process; next cold launch advances once |

COA-9697 (OFF), COA-9698 (Offline), COA-9415 (repeated submit) remain
`NotRequired`, matching the existing Jira label `не_требует_автоматизации`.
COA-9700 (update) is `Deferred`. `AndroidEmailEnforcementSuite` excludes both
categories; the runner only selects the six active methods. Do not execute the
unfiltered inherited test class: it also contains the excluded drafts.

## Reviewed device and builds

- Samsung Galaxy A32 (SM-A325F), Android 13.
- Him: `com.vamapps.thecoach`, preprod debug 1.44.1 (353).
- Her: `com.vamapps.thecoach.forher`, preprod debug 2.19.1 (353).
- English UI; fresh Him goal Beat premature ejaculation (resulting program: Last Longer).
  The Her template uses Cultivate desire and still needs a fresh-installation run with ON.
- Active Android flags are JSON `abtest_force_connect_email[_for_her].isEnabled`.
  The reader uses the **activated** Firebase cache. It does not treat the separate
  iOS-shaped `force_connect_email_config` as the Android switch or edit any flag.

The properties under `src/test/resources/email-enforcement/android-preprod-*-353.properties`
are selector/build templates, not reusable account snapshots. Their version/build
guards intentionally fail if another artifact is installed.

## Independent fixtures

The runner requires `--reset-dedicated-test-installation` because each dataset clears
**only the selected test package's data**. Never point it at a personal installation.
It then invokes `AndroidEmailEnforcementPreparation`, which uses existing onboarding
Page Objects to create a new anonymous user through the UI, handles the notification
permission dialog, closes paywalls and stops at mandatory N without dismissing it.
No purchase, backend account edit or progress completion occurs.

For E1/E2/E3, after proving the account has no email and zero control progress, the
preparation script changes only the debug installation's `is_new_user` marker from
true to false while the app is stopped. Real cold launches reach the requested
stage. Firebase credentials, enforcement counters and Remote Config are untouched.
This is a seeded existing installation, **not evidence of an upgrade or a genuinely
old production account**. The original marker is restored in `finally`; failure to
restore stops the runner before the next dataset reset.

Before/after API evidence verifies both Firebase and backend identity digests,
actual subscription status, completed lessons/exercises and longest streak. Android
Firebase UID and API user_id differ; both must stay unchanged. Email and confirmation
state are checked through authenticated read-only preprod requests. Tokens and raw
identities are not written to evidence. An active subscription is recorded as active,
not described as an account without a subscription.

## Execution

Use the project's installed Appium/UiAutomator2, adb, Maven/JDK 8 and Python 3.
The reviewed debug APKs log authentication traffic; run this task's Appium server with
logcat capture disabled. The existing driver config accepts the server URL below.

```sh
appium --base-path / --port 4725 --log-level warn \
  --default-capabilities '{"appium:skipLogcatCapture":true}'

python3 scripts/run_android_email_enforcement.py \
  --device "$ANDROID_UDID" --variant both \
  --output target/android-email-enforcement/run-YYYYMMDD-HHMMSS \
  --reset-dedicated-test-installation
```

Start with `--variant him --dataset 9424-N-cross` for one scenario. Each output root
must be new: previous fixture evidence cannot be silently overwritten. Credentials
come from the existing `COACH_EXISTING_PROGRESS_EMAIL` / `COACH_EXISTING_PROGRESS_OTP`
environment variables or the project's macOS Keychain account registry (COA-8235).
Free addresses are unique plus aliases of that owned test mailbox; credentials are
passed in the child environment, not CLI arguments or committed properties.

For an independently prepared installation, the JUnit method accepts the existing
`coach.emailEnforcement.fixture` property and `COACH_EMAIL_ENFORCEMENT_UID` guard.
Use `android.noReset=true`, `android.fullReset=false` and an explicit `android.udid`.
Creating a new Appium session must preserve the already running request; no
`forceAppLaunch=true` override is allowed between preparation and measurement.

Each dataset retains redacted baseline/after JSON, fixture properties, Maven logs,
JUnit XML and its own Allure results. Preparation results are in a separate folder
and do not count as scenario passes. Distinct variant/stage/action values have
distinct Allure history IDs. OTP failure screenshots are omitted and single digits
are masked in the XML, including Android's custom `smsVerifyView`.

## Local verification

```sh
mvn -q test -Dplatform=android \
  -Dtest=tests.EmailEnforcementFixtureUnitTests,tests.EmailEnforcementScopeUnitTests,tests.EmailEnforcementArtifactUnitTests
python3 -m unittest discover -s scripts -p 'test_*email_enforcement.py'
```

The scope test checks the six Xray keys in both platform suites. Evidence tests cover
wrong/expired identity, active Android config keys/types and strict account state.
Artifact tests exercise the real failure capture path for iOS and Android OTP screens.

## Observed run on 2026-11-06

Local verification: 10 Java tests and 11 Python tests passed. A fresh anonymous Him
account was created through onboarding, with an active preprod subscription and
zero control progress confirmed by API. Today/Explore navigation was also inspected.

The complete feature run is **blocked by environment configuration**. The newly
fetched active template 1802 has both Android email flags OFF. The untouched Her
installation still holds older template 1800 with ON; that cached value does not
establish ON for new fixtures. COA-9424 was invoked directly and failed in
`EmailEnforcementFixture.requireEnabled`, before Appium/session creation, with
`COA-9222 is disabled in the active app config expected:<true> but was:<false>`.
No feature scenario is recorded as passed, and E1/E2/E3, OTP and Her end-to-end
behavior have not been verified by this new automation run.

Artifacts: `target/android-email-enforcement/environment/`,
`target/android-email-enforcement/environment-guard/test.log` and the three
preparation attempts under `smoke-*`. Earlier preparation failures identified and
fixed notification-dialog handling and the fresh Him goal label. The environment
owner must enable `abtest_force_connect_email` and
`abtest_force_connect_email_for_her` before resuming with the runner above.
