# Product onboarding slides: COA-9145 / COA-9604

The new `tests.OnboardingSlidesTests` uses the existing Java/Appium/JUnit 4
architecture and `iOSOnboardingPageObject` questionnaire/paywall helpers.
No application code, Firebase configuration or existing tests are changed.

## Coverage

| Xray case | Automated behavior |
| --- | --- |
| [COA-9428](https://the-coach.atlassian.net/browse/COA-9428) | A fresh anonymous user reaches the first configured product slide after the questionnaire and paywalls. |
| [COA-9427](https://the-coach.atlassian.net/browse/COA-9427) | Swipes visit every configured slide forwards and backwards; each transition checks its header and page number. |
| [COA-9429](https://the-coach.atlassian.net/browse/COA-9429) | The configured CTA is enabled, stays at the same bottom position and advances the carousel. |
| [COA-9430](https://the-coach.atlassian.net/browse/COA-9430) | The native indicator exposes the current page and total matching the active configuration. Direct dot tapping and cyclic navigation are not claimed. |
| [COA-9432](https://the-coach.atlassian.net/browse/COA-9432) | No early Skip/Close; only the last CTA completes onboarding. Today opens, slide headers disappear and remain absent after restart. |
| [COA-9431](https://the-coach.atlassian.net/browse/COA-9431) | A separately configured existing-progress account logs in, opens Today and has no product slide headers or indicator. |
| [COA-9604](https://the-coach.atlassian.net/browse/COA-9604) | The visible media region has content. GIFs must change pixels inside the image bounds; first/changed screenshots are attached. |

The owner explicitly selected [COA-9145](https://the-coach.atlassian.net/browse/COA-9145)
over conflicting TMS expectations on 5 October 2026. In particular, COA-9428's
Skip button is not required, and COA-9430's cyclic 4-to-1 expectation is not used.
The actual configuration can contain more than four slides.

The new-user checks share one lifecycle method because onboarding can only be
completed once per identity. The existing-user method has its own account and
session. Do not run the whole class sequentially without providing both fixture
states. Each new-user run needs a fresh first historical Today visit. An old
account or a logged-out Start screen is not proof of this prerequisite.

Media errors are collected while navigation/completion continue and fail the
test after the restart check. Recording starts before paywalls are closed. If a
finite GIF finishes before accessibility assertions, the test inspects recorded
frames with the same slide header and checks motion inside the media bounds.
This fallback requires `ffmpeg` on the Appium/test host. Header matching uses
average color distance to tolerate screenshot scaling and video compression;
media motion uses a separate changed-pixel check. The image check does not measure CPU, memory,
frame rate, download time or transient button flicker; no quantitative
performance acceptance threshold was supplied. Analytics events and the upgrade
journey for an existing user are not covered by these tests. Different goals
require their own fresh identity and goal-specific configuration capture.

## Installed builds checked on 5 October 2026

Device: **iPhone Daria (2)**, iPhone 11, iOS **18.0.1**, UDID
`00008030-000929442E01802E`. Installed app metadata was read with `devicectl`.

| Build | Bundle ID | Observations |
| --- | --- | --- |
| Preprod #3469, 1.13.32 | `com.vamapps.preprod.The-Coach` | COA-9431 passed. After the owner reinstalled the build, all five configured headers, indicators, fixed CTA, forward/backward swipes, completion and restart persistence passed. The JUnit result failed only on first-GIF recording header matching. That comparison was corrected and replayed successfully against the saved recording, with negative controls. A complete device rerun of the corrected test is pending another fresh installation. |
| Preprod #3471, 2.0.30 | `com.vamapps.preprod.The-Coach-for-her` | Active female configuration contains variants for BEAT PREMATURE EJACULATION and BOOST OVERALL HEALTH. For Cultivate desire, only common orders 3 and 4 match, so the sequence is invalid. The physical-device JUnit run fails on this configuration assertion before navigation/media checks. |

Before reinstall, male failure: `Product onboarding did not appear`, before any carousel
assertion. The pre-run preferences contain `dailyPlanOnboardingShownKey=true`
despite a distinct anonymous identity and reset questionnaire flags after normal
UI logout. The native recording shows ordinary paywall → special offer → Today,
without product slides. Logout therefore does not provide a fresh installation
state. The owner reinstalled both builds and the next male run reached all five
slides. Reinstall #3469 before each subsequent full run; neither device preferences nor
backend state were edited, and the app was not uninstalled by automation.

The earlier full carousel attempt reached Today after the last CTA, then failed
after restart on an ordinary paywall. The corrected restart check closes optional
paywalls while stopping on any expected slide, so it cannot silently consume a
repeated onboarding. GIF motion was observed on slides 2–5; slide 1's source has
34 frames, approximately seven seconds of playback and finite repetition. Native
recording works on the device. The run after reinstall verified the corrected
restart flow and reached the aggregated media assertion at the end (one failure).
It failed only because header comparison rejected the compressed first-slide
frames. Same-header average color distance was about 0.029, compared with 0.235
for the paywall frame. The corrected 0.04 header criterion accepts the recorded
GIF motion; paywall frames and duplicate static frames are rejected. The media
motion threshold is unchanged. This replay is an offline verification of actual
device evidence, not a passing replacement JUnit device run.

Female failure: `Active slide configuration is invalid: No complete ordered
slide sequence for selected goal: Cultivate desire; matching orders: [3, 4]`.
This is an active configuration/test-data blocker. Fix the goal-specific content
before claiming a For Her navigation or GIF result. The selected goal is a
deterministic fixture choice from the female questionnaire configuration; the
interrupted UI preparation did not confirm goal selection. After reinstall,
configuration capture and the targeted device run reproduced the same blocker.

## Configuration capture

Launch the installed app once so Firebase has an active configuration. The
capture script copies its cached Remote Config SQLite file in read-only mode and
exports only the selected goal's slide expectations to a local properties file.
It does not edit Firebase or the device container. Invalid sequences replace
any stale properties fixture with a validation error and exit nonzero; Java
rejects that error before using slide expectations.

```bash
python3 scripts/capture_onboarding_slides_config.py \
  --device 00008030-000929442E01802E \
  --bundle com.vamapps.preprod.The-Coach \
  --goal 'BOOST OVERALL HEALTH' \
  --output target/onboarding-slides/him.properties
```

For #3471 use the `-for-her` bundle, a supported female goal and a different
output path. The current config rejects `Cultivate desire`.

## Targeted execution

Start Appium with the project's root path:

```bash
appium --base-path / --log target/onboarding-slides/appium.log
```

For a genuinely fresh first visit on #3469:

```bash
mvn -q test \
  -Dtest=tests.OnboardingSlidesTests#testNewUserSlidesNavigationMediaAndCompletion \
  -Dplatform=ios \
  -Dios.bundleId=com.vamapps.preprod.The-Coach \
  -Dios.deviceName='iPhone Daria (2)' \
  -Dios.platformVersion=18.0 \
  -Dios.udid=00008030-000929442E01802E \
  -Dios.noReset=true -Dios.fullReset=false \
  -Dios.useNewWDA=false \
  -Dios.xcodeOrgId=QS3D868T5W \
  -Dios.xcodeSigningId='Apple Development' \
  -Dios.autoAcceptAlerts=true \
  -Donboarding.slides.fixture=target/onboarding-slides/him.properties \
  -Donboarding.slides.preserveSession=true \
  -Donboarding.slides.artifacts=target/onboarding-slides/him-evidence \
  -Dallure.results.directory=target/onboarding-slides/him-allure
```

Attach by bundle ID: no `ios.app`, no full reset or implicit uninstall.
Start from a fresh installation, not merely a new anonymous identity: the local
`dailyPlanOnboardingShownKey` survives logout in the checked build.
If a fresh user is already at the first uncompleted product slide, add
`-Donboarding.slides.prepared=true` to omit questionnaire preparation. This mode
still requires that the first slide appears; it cannot bypass a consumed visit.
Avoid backgrounding/switching apps between preparation and completion.

For COA-9431 use the same device options, replace the test selector with
`tests.OnboardingSlidesTests#testExistingProgressUserDoesNotSeeSlides`, and supply
`COACH_EXISTING_PROGRESS_EMAIL` and `COACH_EXISTING_PROGRESS_OTP` via the existing
secret-backed fixture mechanism. Keep credentials out of Maven arguments and
reports. The project runner's confirmed COA-8235 account can be reused without
resetting program state.

## Validation and local evidence

- `mvn -q test-compile -DskipTests`: passed.
- `python3 -m py_compile scripts/capture_onboarding_slides_config.py`: passed.
- Active male configuration capture: passed, five ordered slides.
- Active female configuration capture: rejected the missing goal-specific orders.
- `tests.OnboardingSlidesTests#testExistingProgressUserDoesNotSeeSlides`: passed on #3469 (1 test, 0 failures/errors/skips).
- `tests.OnboardingSlidesTests#testNewUserSlidesNavigationMediaAndCompletion`: executed on both builds, including after reinstall. No complete passing device result; male recording comparison was fixed and verified offline, female fails active-config validation.
- `ReplayRecordedMedia` diagnostic: actual first-GIF recording passes the corrected helper, while paywall-only frames and duplicate static slide frames fail as expected. See `him-media-replay.log`.
- GIF descriptor counts from the Python parser matched native Apple ImageIO for all five male assets: 34, 155, 164, 106, 193.
- No full suite was run.

Evidence is local under `target/onboarding-slides/`: initial/first-slide XML and
screenshots, sanitized run logs, per-build Allure folders, and copied Surefire
reports under `him-first-attempt`, `him-second-attempt`, `him-fresh-first-attempt`,
`him-final-result`, `him-reinstalled-result`, `existing-user-result`, `her-result`,
and `her-reinstalled-result`. Native video is in
`him-reinstalled-evidence/recording-1/media.mp4`; carousel screenshots are in
`him-reinstalled-evidence/`, and the corrected first-GIF evidence is in
`him-replayed-evidence/`.
The shared `target/surefire-reports` directory is overwritten by Maven; copied
reports preserve the earlier attempts. Raw device configuration/preferences
snapshots stay in ignored `target/` and should not be published as reports.
