# Critical Smoke: COA-7944, COA-1953, COA-7915

The cases remain separate entry points: Welcome, real reinstall and in-place
upgrade require incompatible application installation modes. Do not add them
to one suite or use a full regression run as their acceptance gate.

| Xray Test | iOS method | Android method |
|---|---|---|
| COA-7944 | `CoachAuthorizationTests#testStartNowOpensQuestionnaireWithoutLogin` | same method |
| COA-1953 | `PushPermissionTests#testReinstallShowsPushPermissionPromptAgain` | `AndroidPushPermissionTests#testReinstallRequestsNotificationPermissionAgain` |
| COA-7915 | `ReleaseSmokeTests#testUpdatePreservesAuthorizationAndProgress` | `AndroidUpdateTests#testUpdatePreservesAuthorizationAndProgress` |

## Implementation files

- `src/test/java/tests/CoachAuthorizationTests.java`: isolated START NOW case.
- `src/test/java/tests/PushPermissionTests.java`,
  `src/test/java/tests/AndroidPushPermissionTests.java`: correct reinstall
  binding and first-grant / real reinstall / repeat-grant journey.
- `src/test/java/tests/ReleaseSmokeTests.java`,
  `src/test/java/tests/AndroidUpdateTests.java`: correct update binding, complete
  state comparison and before/after build evidence.
- `src/test/java/lib/ui/CoachFlowPageObject.java`: START NOW assertions.
- `src/test/java/lib/ui/ios/iOSCoachFlowPageObject.java`: observed iOS goal /
  notification locators and verified native permission handling.
- `src/test/java/lib/ui/ios/iOSOnboardingPageObject.java`: reach the notification
  screen without consuming its prompt.
- `src/test/java/lib/UpdateEvidence.java`, `scripts/read_update_account.py`:
  build validation and read-only account snapshots.
- `src/test/java/tests/UpdateEvidenceUnitTests.java`,
  `scripts/test_update_account.py`: offline positive / negative evidence tests.
- `docs/critical-smoke-automation.md`: entry points, preconditions and results.

## Preconditions

- Use the project's Java 8/Maven/Appium setup. Start Appium at `APPIUM_URL`
  (default `http://127.0.0.1:4723/`).
- iOS evidence uses an explicit **Simulator** `IOS_UDID` and a matching
  `IOS_BUNDLE_ID`, `IOS_DEVICE_NAME`, `IOS_PLATFORM_VERSION`. Supply a Simulator
  `.app`, not an IPA or an `iphoneos` build. `ios.isHeadless=true` is supported.
- Android uses an explicit test emulator `ANDROID_UDID=emulator-...`,
  `ANDROID_DEVICE_NAME`, `ANDROID_PLATFORM_VERSION`, and the male package
  `com.vamapps.thecoach`. The permission case requires Android 13+.
- Update evidence additionally requires rooted adbd on that Android emulator.
- Both update cases require an approved existing-progress account with an
  **active subscription**, provided through the existing secret-backed
  `COACH_EXISTING_PROGRESS_EMAIL` / `COACH_EXISTING_PROGRESS_OTP` mechanism.
  Provide `COACH_EXPECTED_ACCOUNT_UID`; do not reuse a case-restricted fixture
  without its owner's approval.
- The iOS update case also uses the existing deterministic fixture contract:
  `COACH_DAILY_PLAN_DAY` and `COACH_FIXTURE_RESET_MODE=prebuilt`.
- Supply two APKs with the same package/signing identity and increasing
  `versionCode`. Supply two Simulator `.app` builds with the same bundle ID and
  different genuine numeric `CFBundleVersion`. `BITRISE_BUILD_NUMBER` is not
  accepted as build evidence. Never edit an artifact's metadata to simulate an
  update.

## Minimal verification

From the repository root:

```bash
mvn -q -DskipTests test-compile
mvn -q test -Dtest=tests.UpdateEvidenceUnitTests
python3 -m unittest discover -s scripts -p 'test_update_account.py' -v
git diff --check
```

These offline tests validate evidence parsing and assertions. They are not a
substitute for the six platform-specific acceptance runs below.

### COA-7944: Welcome / START NOW

Use an installed build. The test normalizes the app to logged-out Welcome,
checks that START NOW is enabled, opens the goal questionnaire (not Login or
OTP), and returns to Welcome.

The fixture must be a **new user**, not just a visible Welcome screen. Use a
dedicated fresh Simulator/device fixture for this entry point; do not reuse the
anonymous onboarding identity from the notification/reinstall journey or an
update-account fixture. During local verification, reusing the notification
Simulator caused the entry check to fail and a subsequent diagnostic launch
showed a paywall instead of the new-user questionnaire. A separate fresh
Simulator restored the expected goal screen. The test does not clear a shared
Keychain, retry taps or increase timeouts to hide this precondition mismatch.

```bash
mvn -q test \
  -Dtest=tests.CoachAuthorizationTests#testStartNowOpensQuestionnaireWithoutLogin \
  -Dplatform=ios -Dios.isHeadless=true \
  -Dios.noReset=true -Dios.fullReset=false

mvn -q test \
  -Dtest=tests.CoachAuthorizationTests#testStartNowOpensQuestionnaireWithoutLogin \
  -Dplatform=android -Dandroid.noReset=true -Dandroid.fullReset=false
```

### COA-1953: notification permission after reinstall

`IOS_APP` / `ANDROID_APP` must reference the artifact to reinstall. Each method
grants permission on the first installation, removes the app, reinstalls the
same artifact and verifies the permission journey again. Android explicitly
checks that the notification grant is absent after reinstall. iOS must show
a real native Notifications alert, not just the in-app example dialog.

```bash
mvn -q test \
  -Dtest=tests.PushPermissionTests#testReinstallShowsPushPermissionPromptAgain \
  -Dplatform=ios -Dios.isHeadless=true \
  -Dios.fullReset=true -Dios.noReset=false \
  -Dios.autoAcceptAlerts=false -Dios.autoDismissAlerts=false

mvn -q test \
  -Dtest=tests.AndroidPushPermissionTests#testReinstallRequestsNotificationPermissionAgain \
  -Dplatform=android -Dandroid.fullReset=true -Dandroid.noReset=false \
  -Dandroid.autoGrantPermissions=false
```

The iOS default journey preserves the notification prompt rather than skipping
or auto-accepting it. Existing `ios.onboarding.steps` overrides remain supported.
First-install tests retain their separate COA-7918 binding. Native acceptance
selects the observed positive button explicitly (`Allow` / `Разрешить`); an
in-app mock dialog cannot satisfy the native-alert check.

### COA-7915: in-place upgrade

For iOS, set `IOS_APP` to the old build and `IOS_UPDATE_APP` to the new build.
The installed baseline must match the supplied old build. For Android, replace
the example paths with the actual approved old/new APK pair; the test first
establishes the old baseline on the selected disposable emulator.

```bash
mvn -q test \
  -Dtest=tests.ReleaseSmokeTests#testUpdatePreservesAuthorizationAndProgress \
  -Dplatform=ios -Dios.isHeadless=true \
  -Dios.fullReset=false -Dios.noReset=true

mvn -q test \
  -Dtest=tests.AndroidUpdateTests#testUpdatePreservesAuthorizationAndProgress \
  -Dplatform=android -Dandroid.fullReset=false -Dandroid.noReset=true \
  -Dandroid.update.old=/absolute/path/old.apk \
  -Dandroid.update.new=/absolute/path/new.apk
```

After the old baseline is captured, installation is strictly in-place, without
uninstalling the app. Compare UID, selected program, day, progress and active
subscription before/after. Verify the actual installed build; Android must
increase `versionCode`, iOS must install the requested different
`CFBundleVersion`.

On 2026-10-09 the user explicitly approved the existing
`the-coach-modules-premium` fixture for COA-7915. This run used the existing
Keychain launcher, without changing its stored case scope or exposing secrets:

```bash
python3 scripts/run_android_parity.py --serial emulator-5556 \
  --suite update --method testUpdatePreservesAuthorizationAndProgress \
  --old-apk /Users/deedles/Downloads/app-1.41.5-manProd-release.apk \
  --new-apk /Users/deedles/Downloads/app-1.42.1-manProd-release.apk \
  --fixture-service the-coach-modules-premium
```

`scripts/read_update_account.py` reads the selected device's authenticated
session and performs only a GET to `/api/v1/user/`. It refuses redirects,
anonymous/wrong-UID Android sessions, expired/wrong-UID iOS tokens, and missing
or non-boolean subscription state. The iOS session cache and identity database
are opened read-only. No token, email or full API response is emitted, stored
in a new file, or attached to Allure. Account evidence is exactly
`{uid, subscriptionActive}`.

## Acceptance and evidence

Each selected method has its exact `@Issue` and records platform / installed
version / build in Allure. Update methods record both builds and account
snapshots. Existing failure capture saves screenshot, page source and driver
logs under `target/`; also retain the Appium server log for the run.

Add `автоматизировано` to an Xray **Test** only after its full scenario passes
on **both** platforms. Preserve all existing labels and re-read the Test to
verify the write. A partial run, offline unit test or fixed annotation alone
does not qualify. Web and Корзина are outside this package.

## Local verification on 2026-10-09

- Baseline: `origin/main` at `dd25d5e3ab9bc0dc0c242307d49214cd4f04ae8c`;
  implementation and runs use a separate worktree. The user's dirty checkout
  is untouched.
- Offline: test compilation, four Java evidence tests and eleven Python
  evidence tests passed.
- COA-7944: PASS on Android 16 / app 1.42.1 (350), and iOS Simulator 26.5 /
  app 1.13.33 (`CFBundleVersion=BITRISE_BUILD_NUMBER`). The placeholder does not
  invalidate Welcome/permission tests, but cannot be used for upgrade evidence.
  Final iOS confirmation used a separate fresh `Coach Start Smoke` Simulator:
  two consecutive PASS runs (44.249 s, 22.099 s), without tap retries or timeout
  increases. Earlier failures on the reused permission Simulator and their
  diagnostics are retained. The Jira label was added and independently re-read.
- COA-1953: Android PASS (12.434 s); iOS PASS (93.770 s), including two verified
  native positive permission choices separated by a real uninstall/reinstall.
  The Jira label was added and independently re-read; `Chat` and `Test` retained.
- COA-7915: Android PASS (55.823 s), 1.41.5 (344) -> 1.42.1 (350). Package and
  signing certificate match; UID, selected program, day, progress and active
  subscription were preserved. The two account attachments contain only UID
  and `subscriptionActive`.
- COA-7915 iOS: not executed. The user chose to keep the Simulator target and
  will supply two `.app` builds with genuine different numeric build numbers.
  TestFlight/device-only artifacts are not used. **No Jira label for this case**
  until the full iOS scenario also passes.
