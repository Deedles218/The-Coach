# Localization UI smoke

`suites.LocalizationSuite` is the separate Appium/JUnit 4 entry point, selected by
`mvn test -Plocalization`. It contains the parameterized `tests.LocalizationTests`.
Existing suites and their English locators retain their behavior.
Runs are sequential on one local iOS Simulator or Android test emulator.

## Build matrix

| `localization.variant` | Default languages | Notes |
| --- | --- | --- |
| `male` | `en,fr,de,it,es` | Men's build |
| `female` | `en,es,fr` | Latest women's build; default women's matrix |
| `female-legacy` | `en` | Explicit opt-in for older English-only women's builds |

`-Dlocalization.locales=fr` selects one language. Unsupported, duplicate, or empty
values fail configuration. iOS bundle ID / Android package must match the fixture;
the variant is explicit and is never inferred from an account or package spelling.

## Scenarios

| `localization.scenarios` entry | Checks | Account |
| --- | --- | --- |
| `welcome` | Device language → Welcome CTAs, Login title/Continue, empty/malformed email disabled states | No credentials; starts logged out |
| `dashboard` | Device language → Today/Explore/Shop labels and Profile account/support/terms/logout labels, repeated after cold restart | Existing returning user |
| `switch` | App language selection through Android Profile / iOS Settings, priority over a different device language, labels and cold-restart persistence | Existing returning user |

The full matrix defaults to `welcome,dashboard,switch` (15 men's or 9 latest
women's parameterized cases). Select a smaller scenario explicitly for initial
verification. Unavailable credentials/selectors/expectations cause a failure,
never a passing assertion or a skipped case.

For `female-legacy`, explicitly select `welcome,dashboard`. A one-language app
cannot prove app-language priority against a second supported language, so its
`switch` configuration fails before device changes.

These scenarios check static strings and language persistence. Date, number,
currency, localized error messages and plural formatting require agreed screens, values and separate
expectations; those checks are not included yet. Text assertions normalize
line breaks and nonbreaking spaces, preserving case, punctuation and accents.

## Preliminary oracles

Fixtures are UTF-8 Java `.properties` files with `platform`, `variant`, `appId`,
`source.version`, `source.origin` and eleven strings for each selected locale.
The optional `<locale>.paywall.restore` string identifies the observed iOS
launch paywall together with its close icon; it is a navigation aid, not a
translation checkpoint. Launch readiness uses explicit waits that dismiss
these overlays before authentication and dashboard checks.
`launch.promo.title` optionally scopes the observed Masterclass modal's unnamed
X to its own container. It is also a navigation aid, not a translation checkpoint.
They are frozen before running assertions. Never regenerate a fixture from a
failing current build just to make a regression pass. Resource-derived oracles
verify application of translations; they require linguistic review to validate
the translation itself.

Current frozen fixtures live in `src/test/resources/localization/`:

| Platform / variant | Supplied build | Fixture |
| --- | --- | --- |
| iOS male | 1.13.33, `The Coach_for_him.app` | `ios-male-prod-1.13.33.properties` |
| iOS female | 2.0.31, `The Coach_for_her.app` | `ios-female-prod-2.0.31.properties` |
| Android male | 1.41.10 (347), manProd APK | `android-male-prod-1.41.10.properties` |
| Android female | 2.16.9 (348), herProd APK | `android-female-prod-2.16.9.properties` |

The iOS inputs are Simulator `.app` directories supplied under
`/Users/deedles/Downloads/IPA files`. Their version strings match the latest
observed production TestFlight versions: male 1.13.33 (3483), female 2.0.31 (3484).
The supplied Simulator apps contain `CFBundleVersion=BITRISE_BUILD_NUMBER`, so
equivalence to those exact TestFlight build numbers is not confirmed.

Welcome copy can come from remote configuration and was captured from the UI
for every locale on both platforms. The collectors apply native system language,
cold-launch the app and restore the original device preferences. Other strings
come from `.lproj` / APK resources, with view-specific uppercase handling for
the Login title and Continue. Women's iOS Login title preserves the resource's
sentence case; men's iOS and Android titles use uppercase. Android Login copy is additionally checked against
the requested APK locale during collection. Men's and women's Welcome wording
is captured independently. The earlier preprod fixture
`ios-male-1.13.29.properties` remains available for that specific older build.

Create a **new** iOS resource fixture from `.app` or `.ipa` without installing it:

```bash
python3 scripts/capture_localization_fixture.py \
  --app '/path/to/Coach for Her.app' --variant female \
  --output target/localization/ios-female-build.properties
```

For remote Welcome copy, install/boot the selected Simulator build, leave it on
the logged-out Welcome screen, and add
`--capture-welcome --udid <Simulator UDID>`. The collector attaches to the already
installed app by its bundle ID, snapshots/restores language preferences and
refuses to overwrite an existing fixture. It requires a Welcome layout with
two visible CTA buttons; verify their roles if a new build changes the layout.
It does not log out or provision accounts. An IPA can supply string resources,
but cannot be installed in a Simulator.

Create a new Android fixture from a supplied APK and logged-out emulator:

```bash
python3 scripts/capture_android_localization_fixture.py \
  --apk '/path/to/app-manProd-release.apk' \
  --aapt2 "$ANDROID_HOME/build-tools/30.0.2/aapt2" --variant male \
  --capture-ui --udid emulator-5554 \
  --output target/localization/android-male-new-build.properties
```

Default capture rejects missing resources. `--allow-missing` preserves a partial
oracle with an explicit `missing.<locale>.<key>` reason; it never substitutes
English. The corresponding dashboard/switch cases fail fixture validation,
while an independent locale or Welcome case can still run. Both supplied APKs
need this option because `shop_title` has no French resource.

Observed remote Welcome text is frozen as it currently appears, including the
English copy listed below. Passing a comparison against this provisional oracle
does **not** prove that those existing translations are acceptable.

## Language settings selectors

Android language controls and iOS Settings navigation vary by build/OS. Record
actual selectors from the supplied build in its fixture before enabling `switch`:

| Key | Meaning |
| --- | --- |
| `language.open.steps` | Sequence of observed `id:` / `xpath:` selectors separated by `\|\|`; Android starts from Profile, iOS from Settings |
| `language.open.steps.<locale>` | Optional navigation sequence when the app's settings row is localized and has no stable ID |
| `language.selected.locator` | Selected language option, with its visible human-readable label |
| `language.option.en` / `.fr` / `.de` / `.it` / `.es` | Locator for each supported language option |
| `language.confirm.locator` | Optional explicit confirmation button |
| `locator.<text checkpoint>` | Optional stable selector replacing a text fallback, e.g. `locator.login.title` |
| `locator.profile.open` | Optional build-specific stable Profile button selector |

Prefer observed stable resource/accessibility IDs. Current iOS text fallbacks
look for the exact expected label; a missing translation therefore reports a
missing localized element. They do not distinguish a changed label from a
missing element without inspecting the saved page source. The existing observed
Profile icon IDs and Android navigation/input IDs are reused. New language-switch
IDs are intentionally not guessed.

Both iOS prod fixtures include observed Settings selectors. Android Profile
selectors still require a prepared account that reaches Profile on this
emulator. APK layout evidence identifies `rvData`, `tvName`, `ivArrowStr` and
the Save controls, but the live picker, labels and selected state have not yet
been verified; these provisional IDs are not enabled as a passing switch test.

Android system-language runs require the in-app setting to follow the device
language initially. Prepare that setting on the dedicated test instance. The
switch scenario records and restores the previous selection, including the
System/Default option when present. The collector and assertions do not buy
subscriptions, complete practices, delete accounts or seed backend data.

English needs deliberate setup because authentication bootstraps in English.
On iOS, the scenario first puts the device in French, keeps French first when
adding a second preferred language to reveal Settings, and selects English for
the app. It then changes the device to English and back to French. Selecting
English while English is already the device default did not retain an explicit
override in the observed Simulator runs. On Android, the English scenario uses
an intermediate French selection and records the original selection only once.
The Android picker and its restoration still need live verification with the
prepared fixture described above.

## Targeted execution

Use `mvn test -Plocalization` with the platform/build parameters below to select
only `suites.LocalizationSuite`. The profile inherits the project's Surefire and
Allure configuration. Alternatively, select it explicitly with
`mvn test -Dtest=suites.LocalizationSuite` and the same parameters.

Use a preinstalled app and a dedicated test Simulator/emulator. Keep
`noReset=true`, `fullReset=false`, and the normal `logout` isolation configuration.
The host-side recovery restores only system language/region and the iOS app's
`AppleLanguages` key while preserving unrelated application preferences. It
reads and changes the iOS key through native `defaults`/CFPreferences, closes the
app and Settings before recovery, and verifies the restored snapshot. Direct
plist edits can be overwritten by cached native preferences. Recovery
requires Python 3 plus `xcrun`/`adb`. Android currently supports a single original
system language; a multi-language configuration fails before changes.

Minimal men's iOS Welcome test (no account required):

```bash
mvn test -Plocalization -Dplatform=ios \
  -Dios.udid='<Simulator UDID>' -Dios.deviceName='<Simulator name>' \
  -Dios.platformVersion='<iOS version>' \
  -Dios.bundleId=com.vamapps.The-Coach \
  -Dios.isHeadless=true -Dios.useNewWDA=true \
  -Dlocalization.variant=male -Dlocalization.locales=en \
  -Dlocalization.scenarios=welcome \
  -Dlocalization.fixture=src/test/resources/localization/ios-male-prod-1.13.33.properties
```

Omit `localization.locales` to run all five languages. `useNewWDA=true` addresses
the stale WDA reuse observed locally; it does not retry a failing test.

For authenticated scenarios inject the existing fixture through
`COACH_EXISTING_PROGRESS_EMAIL` and `COACH_EXISTING_PROGRESS_OTP` in the child
environment/secret manager. Do not put credentials on the command line or in
the locale oracle. Existing login helpers bootstrap in English, then the suite
applies the tested locale. Use `-Dlocalization.scenarios=dashboard` first, then
`switch` with observed settings selectors, then the full suite.

The iOS suite deliberately creates its session without XCUITest `language` and
`locale` capabilities: this driver's launch arguments pin the language on every
activation, masking later system-language changes. It applies the English
bootstrap and each tested language with `mobile: configureLocalization` after
session creation, then restarts the app. Native recovery restores the original
language order, region and app override after each case.

Latest women's run uses `-Dlocalization.variant=female`, its actual bundle/package,
and its own `localization.fixture`. Android additionally needs the configured
`android.udid`, `android.deviceName`, `android.platformVersion`, `android.appPackage`
and `android.appActivity` of the supplied build. Physical-device language-setting
automation is outside this first implementation; do not pass a real-device UDID.

Minimal Android Welcome run:

```bash
mvn test -Plocalization -Dplatform=android \
  -Dandroid.udid=emulator-5554 -Dandroid.deviceName=TheCoach_API_36_Play \
  -Dandroid.platformVersion=16 -Dandroid.appPackage=com.vamapps.thecoach \
  -Dandroid.appActivity=com.vamapps.thecoach.MainActivity \
  -Dlocalization.variant=male -Dlocalization.scenarios=welcome \
  -Dlocalization.fixture=src/test/resources/localization/android-male-prod-1.41.10.properties
```

Women's Android package is `com.vamapps.thecoach.forher`; its activity retains
`com.vamapps.thecoach.MainActivity`. The shared authorization and onboarding
Page Objects read the configured package. Run one app/variant per JVM, matching
the project's static locator architecture.

Android system-language changes use the installed Appium Settings helper and
verify the resulting locale. An old helper process that has not picked up the
driver's API policy is restarted once on `NoSuchMethodException`, matching its
client behavior. Restore Android locale before quitting UiAutomator2, which
resets that API policy; an unchanged locale requires no helper invocation.

## Findings in the supplied builds

| Build / locale | Evidence | Status |
| --- | --- | --- |
| Both Android APKs / French | `shop_title` has English, German, Spanish and Italian values, but no French value | Missing resource; French dashboard/switch cannot use a complete oracle |
| Android male / French, German, Italian | Welcome CTAs remain `START NOW` / `I'VE ALREADY PURCHASED`, while Login matches the selected locale | Candidate remote-copy localization defect; reflected in the provisional baseline |
| Android female / Spanish, French | Welcome CTAs remain the same English strings while Login is localized | Candidate remote-copy localization defect; reflected in the provisional baseline |
| Android male / English → Spanish | First Spanish cold launch shows English CTAs for at least 22 s; second Spanish cold launch shows `CREAR UNA CUENTA` / `YA HICE UNA COMPRA` | Reproduced first-launch localization failure; the test remains failing, without a restart retry |
| Android male / current returning-user fixture | OTP completes, then `tvAnswerNum` shows `STEP 1/17` | Fixture/onboarding blocker before Dashboard assertions; not a translation failure |
| iOS female / male returning-user fixture | Login shows `You should use the regular Coach app to sign in` | Incompatible test account; a women's fixture is required for authenticated cases |

Do not regenerate these baselines to hide a failed regression. Approved copy is
needed before treating the suite as a gate for linguistic correctness. Static
text checks also do not prove absence of visual clipping or overlapping glyphs.

## Offline verification and diagnostics

```bash
mvn -q -Dtest=tests.LocalizationFixtureUnitTests test
python3 -m unittest discover -s scripts -p 'test_localization_helpers.py' -v
```

Allure records platform, variant, locale, scenario and oracle provenance.
Failures capture screenshot/page source before language restoration through the
existing diagnostic hooks. A session-creation failure has no screenshot; inspect
Surefire and the server log. If host-side restoration fails, the error names a
temporary recovery JSON. Keep it and run:

```bash
python3 scripts/localization_device.py restore '/path/from/the-error.json'
```

The initial live attempt failed during session creation because Appium reused a
stale WDA (`ECONNREFUSED 127.0.0.1:8100`), before assertions. The original
Simulator languages (`ru-RU`, `en-RU`) and locale (`ru_RU`) were verified restored.
This is an environment failure, not evidence of a localization defect.

## Verified execution — 5 October 2026

Environment: iPhone 17 Pro Simulator / iOS 26.5, and
`TheCoach_API_36_Play` / Android 16 API 36 (`emulator-5554`).
Only localization tests and their helper unit tests were run; existing regression
suites were not run or modified.

| Scope | Actual result | Evidence |
| --- | --- | --- |
| iOS male 1.13.33 / all five locales, three scenarios | 14 PASS in the 15-case run; English switch then PASS separately after fixing preparation, and PASS again with native recovery | `target/localization/runs/ios-male-full-matrix-promo-fix/`, `target/localization/runs/ios-male-en-switch-french-device/`, `target/localization/runs/ios-male-en-switch-native-recovery/` |
| iOS female 2.0.31 / Welcome + Login, en/es/fr | 3 PASS | `target/localization/runs/ios-female-welcome-matrix/` |
| Android male 1.41.10 / Welcome + Login, en/fr/de/it/es | 4 PASS, 1 FAIL: first Spanish launch retains English CTAs | `target/localization/runs/android-male-welcome-matrix/`; confirmed again with text readiness wait in `android-male-es-welcome-text-wait/` |
| Android female 2.16.9 / Welcome + Login, en/es/fr | 3 PASS across an English run and an es/fr run | `target/localization/runs/android-female-en-welcome/`, `target/localization/runs/android-female-welcome-matrix/` |
| Java frozen-oracle validation | 9 PASS | `tests.LocalizationFixtureUnitTests` |
| Python collectors / native recovery helpers | 10 PASS | `scripts/test_localization_helpers.py` |
| iOS native recovery / both supplied apps | Temporary French override removed; unrelated preferences preserved; original state retained after cold launch | `target/localization/ios-native-recovery-roundtrip.json` |
| Android authenticated scenarios / current account | Blocked after OTP by STEP 1/17; Profile/picker not verified | `target/localization/runs/android-male-en-dashboard/` |
| Women's authenticated scenarios / current male account | iOS login rejects the account; women's fixture needed | `target/localization/runs/ios-female-en-dashboard/` |

The men's iOS result is **14 + 1 separate PASS**, not a new clean 15-case run
after the final English-preparation correction. The corrected English case
exercises both device-language changes as well as app restart. Other locale
selectors, text checks and scenarios were unchanged by that correction.

The initial full iOS run was interrupted after an observed Masterclass promo
hid the entry screen; its Appium session and recovery snapshot were closed and
restored. No final Surefire summary exists for that interrupted attempt.
The later transient Appium-server stop prevented a diagnostic probe from
creating a session; the server was restarted before the final successful case.

Spanish Android reproduction is saved as
`target/localization/android-es-transition-first-spanish-launch.xml` and
`target/localization/android-es-transition-second-spanish-launch.xml`: after an
English bootstrap, the first Spanish launch retained English CTAs for at least
22 s; a second cold launch produced the Spanish copy. Its likely source is app
or remote-copy update behavior; the exact implementation cause is not confirmed.
The suite does not add that second launch to turn the failed assertion green.

Native settings were checked after execution: Simulator preferred languages
`ru-RU,en-RU`, region `ru_RU`, no per-app language override for either iOS app;
both native preferences and persisted app plists were checked after the final
English-switch run. Android system locale is `en-US`, and no Appium sessions
remain active. Physical-device UI automation and exact TestFlight
binary equivalence remain unverified.
