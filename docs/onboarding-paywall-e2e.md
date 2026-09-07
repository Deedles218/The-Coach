# New-user onboarding paywall E2E

`tests.OnboardingPaywallTests` covers Android and iOS with the existing
Java/Appium/JUnit 4 Page Object stack.

| Case | Test method | Expected result |
| --- | --- | --- |
| 1 | `test01NewUserPurchasesTopSubscription` | New anonymous user completes onboarding, selects the visually top plan, confirms a test-store purchase and reaches premium content. |
| 2 | `test02NewUserPurchasesBottomSubscription` | New anonymous user completes onboarding, selects the visually bottom plan, confirms a test-store purchase and reaches premium content. |
| 3 | `test03TermsAndPrivacyOpens` | Terms & Privacy opens from the onboarding paywall. |
| 4 | `test04AutoRenewalDisclosureIsExact` | The normalized text equals `Subscription renews automatically. Cancel anytime. Privacy Policy & Terms of Service`. |
| 5 | `test05RestoreResponds` | Restore is visible/enabled and produces restored access or an explicit no-purchase/error result. |

The plan implementation does not depend on localized price text. Android
finds visible `layoutSubscription` cards and sorts them by vertical position;
iOS uses the stable plan names exposed by the current TestFlight build.

## Android 1.40.8 on Android 16 / API 36

Start Appium at the repository root URL:

```bash
appium --base-path /
```

Run a non-purchase case:

```bash
mvn \
  -Dtest=tests.OnboardingPaywallTests#test04AutoRenewalDisclosureIsExact \
  -Dplatform=android \
  -Dandroid.app=/Users/deedles/Downloads/app-1.40.8-manProd-release.apk \
  -Dandroid.deviceName=TheCoach_API_36_Play \
  -Dandroid.udid=emulator-5554 \
  -Dandroid.platformVersion=16 \
  -Dandroid.autoGrantPermissions=true \
  -Dandroid.noReset=false \
  -Dandroid.fullReset=true \
  -Donboarding.goal="Boost overall health" \
  test
```

For case 1 or 2, the device must use a Google Play system image, the signed-in
Google account must be an approved license tester, and the Play dialog must
show that the transaction is a test purchase. Only then add:

```text
-Dgoogleplay.licenseTester=true -Dpurchase.allow=true
```

Use the local `TheCoach_API_36_Play` AVD. It is based on
`system-images;android-36;google_apis_playstore;arm64-v8a` and exposes a current
Google Play client. Sign in interactively with the approved license-tester
account before running the suite; keep the account email and password out of
the Maven command and repository. Without a signed-in Play account, the 1.40.8
paywall container opens but product details do not populate, and the bounded
paywall-ready check fails instead of returning a false pass.

## iPhone 11 / TestFlight

The inspected device currently exposes:

- device: `iPhone Daria (2)`, UDID `00008030-000929442E01802E`;
- iOS: `18.0.1` (use `18.0` in the Appium capability);
- app: TestFlight `1.13.29 (3398)`, bundle ID `com.vamapps.The-Coach`.

Attach by bundle ID. Do not pass `ios.app`:

```bash
mvn \
  -Dtest=tests.OnboardingPaywallTests#test04AutoRenewalDisclosureIsExact \
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

Every method requires the fresh `START NOW` screen. iOS cannot clear an
installed TestFlight app's data through Appium without also uninstalling the
app, and Appium cannot reinstall TestFlight by bundle ID. Reinstall the same
build from TestFlight before each method. Purchase cases also need a clean
sandbox transaction state or a distinct Sandbox Apple Account.

For case 1 or 2, explicitly enable the otherwise blocked sandbox confirmation:

```text
-Dstorekit.sandbox=true -Dpurchase.allow=true
```

TestFlight purchases use StoreKit sandbox, but a physical iPhone may still
require a side-button, biometric or account confirmation that XCUITest cannot
complete unattended. The test reports that as a test-lab prerequisite rather
than treating initiation as a successful purchase.

### Unattended iOS purchase automation

Do not store an Apple Account password in the repository, CI variables or
Appium capabilities. A TestFlight payment sheet is owned by iOS and may not be
present in the app's XCUITest accessibility tree, so Appium cannot reliably
confirm it on a physical device.

For unattended regression, request a development-signed QA build with a
synced or local `.storekit` configuration selected in its Xcode scheme. Drive
transactions with `SKTestSession`, set `disableDialogs = true`, and clear the
session before every case. Appium can cover the app-owned journey and attach to
that QA build; StoreKit Test/XCTest owns deterministic transaction setup and
cleanup. Keep one short manual TestFlight purchase smoke per SKU as the final
App Store integration check.

A dedicated Sandbox Apple Account is useful for TestFlight: sign out of the
production account under Media & Purchases, then sign in under Developer >
Sandbox Apple Account. This keeps transactions free and makes purchase history
resettable, but it does not make the system confirmation sheet fully
automatable by Appium.

## Observed iOS paywall contract

The real TestFlight hierarchy exposed:

- top plan: `One Month Plan`;
- bottom plan: `One Year Plan`;
- purchase CTA: `CONTINUE`;
- restore action: `RESTORE`;
- disclosure: `Subscription renews automatically. Cancel anytime.` plus
  `Privacy Policy & Terms of Service`.

The current build exposes Privacy Policy and Terms of Service only inside one
attributed static-text node. The Page Object first performs a semantic element
click, then uses a relative tap inside that same element as a temporary
fallback. For a durable accessibility contract, add separate identifiers such
as `subscription_privacy_policy` and `subscription_terms_of_service`.

## Test isolation

Run purchase cases individually. Reset sandbox purchase history or use
separate test-store accounts between top-plan and bottom-plan scenarios;
clearing application data alone does not clear Google Play or StoreKit
subscription state. Restore also mutates the effective entitlement and must run
after paywall-only checks, or in a separate account/session. If iOS reaches
Today instead of a paywall, the test fails immediately with an active-
entitlement diagnostic instead of waiting for a generic timeout. Failure
artifacts are written to `target/screenshots`, `target/page-source`,
`target/appium-logs`, and `target/allure-results`.
