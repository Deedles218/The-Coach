# COA-9222 — Android email enforcement

Android reuses the reviewed iOS Xray scenarios through `EmailEnforcementScreen` and
`AndroidEmailEnforcementTests`. Only the UI implementation and installation/account
reader differ. The Android implementation task is COA-9619. No Android application
code or dependencies are changed.

## Scope

Both mobile platforms run the same ten release scenarios from
`EmailReleaseRegression` for Him and Her. Android uses
`AndroidEmailReleaseRegressionSuite`. For iOS, see [iOS release execution](ios-email-enforcement.md#релизный-сьют-для-him-и-her).
Run with the email collection feature enabled. The preprod debug suite is below.

| Jira | Release regression |
| --- | --- |
| COA-9781 | I’ve Already Purchased → Please Start Here → questionnaire → close paywall. The mandatory form blocks Android Back and has no skip actions. |
| COA-9782, COA-9764, COA-9765, COA-9766 | Update the anonymous installation from the previous release. Daily opens before the configured first request. Use X and Remind Me Later in separate scenarios. Check the E2 warning, return to Daily without another request, and mandatory E3 after restart. Android requires the warning on the form and also checks system Back. |
| COA-9700, COA-9699 | Show the first request on the previous release and update in place. The next cold launch opens E2. Restart with E2 still open to verify E3. |
| COA-9806 | Submit a known occupied address. Android Back or iOS Cancel returns to the mandatory form, then a different address opens OTP. |
| COA-9807 | Him and Her in es-ES show the existing-account question and the Login action in Spanish. |
| COA-9767 | Change Email restores an empty mandatory form and disables Continue for empty or invalid input. |
| COA-9762 | Resend stays on OTP, restarts the countdown and delivers a second code email. |
| COA-9768 | After entering the first digit with the numeric keyboard open, Support is below the OTP input. After valid OTP, the keyboard closes and Support is below the confirmation icon. |
| COA-9761 | Open and leave Support, link and confirm a free email, verify Profile and Daily on cold launch, then sign into the same account from a clean local installation. |

### Release execution

Use a dedicated Android 13+ test installation with Google Play Billing available
and Appium UiAutomator2. The test resets the selected Coach package for each
method. Use an explicit emulator serial or set
`-Dcoach.emailRelease.dedicatedDevice="$ANDROID_UDID"` for a designated USB
device. Install the target APK before running. Use a test-only account of the
correct variant in `COACH_EXISTING_PROGRESS_EMAIL` for the occupied-address
checks and a free QA address in `COACH_VALID_EMAIL_WITHOUT_PROGRESS` for
COA-9806 and COA-9767. The delivery and confirmation methods create independent
test mailboxes with [Mail.tm](https://mail.tm/).

Resend waits for the action to become available before capturing mailbox IDs.
The next delivery must have a new ID and a `createdAt` timestamp at or after
the tap boundary, using the [Mail.tm message metadata](https://docs.mail.tm/api/messages).
Keep the runner clock synchronized with the mail service; missing or invalid
delivery timestamps cannot establish a successful resend.

```sh
mkdir -p target/email-release
swiftc -O scripts/read_android_questionnaire_text.swift -o target/email-release/read-questionnaire-text
mvn -q test -Dplatform=android -Dandroid.udid="$ANDROID_UDID" \
  -Dandroid.platformVersion=13 -Dandroid.noReset=true -Dandroid.fullReset=false \
  -Dandroid.appPackage=com.vamapps.thecoach -Dappium.url=http://127.0.0.1:4725/ \
  -Dcoach.emailRelease.dedicatedDevice="$ANDROID_UDID" \
  -Dcoach.emailRelease.version=1.44.5 \
  -Dcoach.emailRelease.previousVersion=1.44.4 \
  -Dcoach.emailRelease.previousApk="$COACH_PREVIOUS_APK" \
  -Dcoach.emailRelease.updateApk="$COACH_TARGET_APK" \
  "-Dcoach.emailRelease.questionnaireReader=$PWD/target/email-release/read-questionnaire-text" \
  -Dtest=suites.AndroidEmailReleaseRegressionSuite
```

Use the device's actual Android version in `android.platformVersion`. For Her,
set `android.appPackage=com.vamapps.thecoach.forher` and use versions 2.19.4
and 2.19.5 with the matching APKs. Both release APKs have version code 356.
Set `coach.emailRelease.firstRequestLaunch` if the active configuration specifies
a value other than 3. The suite runs the X and Remind Me Later scenarios independently.
For a single method, pass `-Dtest=tests.AndroidEmailReleaseRegression#testMethod`.
COA-9767 accepts `coach.emailRelease.language=en|es|fr|de|it` on Him and
`en|es|fr` on Her. The common release matrix uses English for COA-9767,
COA-9768 and COA-9806, with an `es` override for a targeted run on either
variant. COA-9807 always uses Spanish on both variants.

For a targeted rerun of a form or OTP scenario blocked at onboarding, set
`-Dcoach.emailRelease.allowE3Preparation=true`. If onboarding lands in Daily,
the setup reaches E3 through real cold launches and records the starting
condition in Allure. COA-9781 still requires N immediately after onboarding.

The questionnaire reader uses macOS Vision when the WebView omits a rendered
control from accessibility. The tests use Appium locators when the controls
are exposed. On a host with accessible questionnaire controls, omit the reader.
Run Appium with `skipLogcatCapture=true` and warning-level logging for OTP runs.
Reports are in `target/surefire-reports` and `target/allure-results`.

## Preprod debug scope

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
COA-9700 (update) is `Deferred` in the preprod debug suite. `AndroidEmailEnforcementSuite` excludes both
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
