# Onboarding goal to Today program E2E

`tests.OnboardingGoalProgramTests` covers the new anonymous-user journey:

1. Tap the first `Start now` action.
2. Select the configured primary goal and a deterministic additional goal.
3. Complete the runtime-driven questionnaire.
4. Close every onboarding paywall and optional popup.
5. Verify that Today is selected and its active program matches the primary goal.

Supported mappings:

| Onboarding goal | Program code | Accepted Today title |
| --- | --- | --- |
| Beat premature ejaculation | LL | LL / Last Longer |
| Beat erectile dysfunction | KIH | KIH / Keep It Hard |
| Boost overall health | OH | OH / Overall Health |
| Overcome porn addiction | Unhooked | Unhooked |
| Improve sex skills | SIAS | SIAS / Sex Is a Skill |

The default goal is `Boost overall health`. Override it with
`-Donboarding.goal="<goal>"`.

## Appium

Start Appium at the repository-configured root URL:

```bash
appium --base-path /
```

## Android 16 / API 36

The Android test enforces a clean install. It fails before the journey when
the APK, `fullReset=true`, or `noReset=false` is missing.

```bash
mvn \
  -Dtest=tests.OnboardingGoalProgramTests \
  -Dplatform=android \
  -Dandroid.app=/Users/deedles/Downloads/app-1.40.8-manProd-release.apk \
  -Dandroid.deviceName=TheCoach_API_36_ARM \
  -Dandroid.udid=emulator-5554 \
  -Dandroid.platformVersion=16 \
  -Dandroid.autoGrantPermissions=true \
  -Dandroid.noReset=false \
  -Dandroid.fullReset=true \
  -Donboarding.goal="Boost overall health" \
  test
```

## iPhone 11 / TestFlight 1.13.29 (3398)

TestFlight is attached by bundle ID; do not pass `ios.app`. For a genuinely
new user, uninstall The Coach and reinstall the same build from TestFlight
before the run. Appium cannot restore a deleted TestFlight app from a bundle
ID alone.

The run accepts every early iOS permission prompt. The page object also taps
the positive in-app notification pre-prompt and accepts system alerts.

```bash
mvn \
  -Dtest=tests.OnboardingGoalProgramTests \
  -Dplatform=ios \
  -Dios.bundleId=com.vamapps.The-Coach \
  -Dios.deviceName="iPhone Daria (2)" \
  -Dios.platformVersion=18.0 \
  -Dios.udid=00008030-000929442E01802E \
  -Dios.noReset=true \
  -Dios.fullReset=false \
  -Dios.autoAcceptAlerts=true \
  -Dios.useNewWDA=false \
  -Dios.xcodeOrgId=QS3D868T5W \
  -Dios.xcodeSigningId="Apple Development" \
  -Donboarding.goal="Boost overall health" \
  test
```

The test deliberately never taps a subscription CTA. It recognizes the
regular TestFlight paywall and the follow-up special offer by their close
controls, closes both, and requires at least one paywall to have appeared.
