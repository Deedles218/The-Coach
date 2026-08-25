# QA coverage review: iOS smoke и Android Explore v7

Дата проверки: 2026-08-25  
Ветка: `codex/android-new-explore`  
Целевой APK: `/Users/deedles/Downloads/app-1.40.3-manProd-release.apk`  
SHA-256: `53f75eaa6ac20b9b7ff42c07036139800104506bf6d6e11d688c79c21c99c8b2`  
Package/activity: `com.vamapps.thecoach` / `com.vamapps.thecoach.MainActivity`  
Устройство: `emulator-5554`, `sdk_gphone_arm64`, Android 11/API 30; Appium 2.19.0, UiAutomator2.

## Итоговое решение

- **Explore v7: No-Go.** В APK 1.40.3 не отображается обязательный `Browse Programs`; вместо него остаются `ALL PROGRAMS` и `rvActivePrograms`.
- **Video redirect: Pass с условиями.** Карточка Quick Tips `Finish Too Soon` на APK 1.40.3 открывает нативный Android video player; проверены `viewPlayer`, play/pause control и back control.
- **Общий Android release: Insufficient evidence.** Базовые smoke-проверки проходят изолированно на подготовленном авторизованном состоянии, но чистая авторизация после OTP попадает в questionnaire `STEP 1/17`. Стабильного Android onboarding/fixture setup в проекте нет.

## Объём и источники фактов

В scope вошли:

1. ТЗ Explore v7: секции и порядок, `Browse Programs`, удаление Courses/active program, card layouts, lesson/practice/WebView/video redirects и analytics title.
2. Существующие iOS P0/P1 smoke tests.
3. Добавленные Android smoke, Daily Plan и Explore checks.
4. Реальный прогон на подключённом Android и APK 1.40.3.

Основные исходники:

- iOS P0 smoke: [SmokeTests.java](/Users/deedles/IdeaProjects/The-Coach/src/test/java/tests/SmokeTests.java:24).
- iOS release smoke: [ReleaseSmokeTests.java](/Users/deedles/IdeaProjects/The-Coach/src/test/java/tests/ReleaseSmokeTests.java:28).
- Android P0 checks: [AndroidSmokeTests.java](/Users/deedles/IdeaProjects/The-Coach/src/test/java/tests/AndroidSmokeTests.java:34).
- Android Explore checks: [AndroidExploreTests.java](/Users/deedles/IdeaProjects/The-Coach/src/test/java/tests/AndroidExploreTests.java:43).
- Android Explore page object и video-player contract: [AndroidExplorePageObject.java](/Users/deedles/IdeaProjects/The-Coach/src/test/java/lib/ui/android/AndroidExplorePageObject.java:157).
- Android Daily Plan page object: [AndroidDailyPlanPageObject.java](/Users/deedles/IdeaProjects/The-Coach/src/test/java/lib/ui/android/AndroidDailyPlanPageObject.java:1).
- Secret-backed data contract: [smoke-environment.md](/Users/deedles/IdeaProjects/The-Coach/docs/smoke-environment.md:16).

Credential values в отчёт и исходники не включены.

## Факты, выводы и рекомендации

### Факты

- iOS smoke содержит 8 сценариев в `SmokeTests`: clean install/onboarding, Today/Daily Plan, empty email, email+OTP, Profile, logout и Kegel player.
- iOS release smoke содержит main-tab navigation, текущий Explore, Recommended/Custom Kegel, игнорируемый PDF paywall и update smoke.
- На Android добавлены 5 P0 сценариев, Android Daily Plan factory/page object и 7 Explore v7 сценариев.
- APK 1.40.3: Android Explore suite — **7 тестов, 6 pass, 1 error** (`testBrowseProgramsReplacesLegacyBlock`).
- APK 1.40.3: изолированные Android smoke `test01`, `test03`, `test04`, `test05` — pass; `test02` — fail-fast на post-auth questionnaire.

### Выводы

- iOS smoke хорошо покрывает базовый happy path авторизации, Today, профиль и player, но не является доказательством Android parity.
- В прежних Android/iOS-shared классах платформенные guards часто возвращают управление без assertion; запуск такого класса на Android мог выглядеть зелёным без фактической проверки.
- Авторизованный dashboard нельзя определять только по трём bottom tabs: anonymous Android shell показывает те же tabs. Новый Android flow проверяет Profile и `Connect your email` marker.
- Dynamic Daily Practice state требует fixture contract: локатор теперь ищет карточку внутри секции `DAILY PRACTICE`, но состав и порядок карточек остаются данными backend.

### Рекомендации

1. До release Explore v7 включить в Android config/runtime `Browse Programs` и убрать legacy active-program block.
2. Предоставить воспроизводимый Android onboarding fixture или backend reset hook, включая состояние после OTP и завершённый questionnaire.
3. Добавить отдельную проверку analytics payload: `ExploreExerciseLauch` и `ExploreExerciseCompleted` должны содержать title карточки.
4. После исправления конфигурации повторить Explore suite на clean install и no-reset авторизованном fixture.

## Анализ существующих iOS smoke

| Область | Тесты/ID | Что покрыто | Существенный пробел |
|---|---|---|---|
| Clean install | `SmokeTests:31–44`, COA-7914 | onboarding, push prompt, authorized dashboard | Android clean-install/onboarding отсутствует |
| Today/Daily Plan | `SmokeTests:53–65`, COA-7956 | day switcher, current day, lessons, practice | update/fixture reset зависит от внешней среды |
| Login validation | `SmokeTests:74–84`, COA-7948 | empty email, disabled Continue, no-op tap | нет Android parity в старом классе |
| Email + OTP | `SmokeTests:93–99`, COA-7935 | authorized Daily Plan entry point | нет проверки questionnaire transition |
| Profile | `SmokeTests:108–114`, COA-8503 | accessibility open, metrics/settings | нет Android parity в этом iOS-only тесте |
| Logout | `SmokeTests:123–130`, COA-8500 | Profile → logout → Welcome | no-reset fixture lifecycle не полностью deterministic |
| Player | `SmokeTests:139–153`, COA-8087 | start, controls, safe exit | Explore video redirect не покрывался |
| Release/Explore/update | `ReleaseSmokeTests:35–130` | iOS tabs, current Explore, update, PDF (ignored) | Android config v7, analytics и update не покрыты |

Ограничение: `SmokeTests` и `ReleaseSmokeTests` явно требуют iOS; часть legacy Android-compatible классов делает `return` при не-iOS, что снижает доверие к общему suite status.

## Risk-based coverage map

Оценка риска: 0 — незначительный, 5 — критический. `Pass` означает pass в проверенном состоянии, а не доказанную release readiness.

| Риск/требование | Риск | Условие/тест | APK 1.40.3 | Остаточный пробел |
|---|---:|---|---|---|
| Auth после clean install и OTP | 5 | iOS COA-7914/7935; Android `test02` | **Fail**: questionnaire `STEP 1/17`; full ordered run `1 failure + 3 cascade errors` | Нужен Android onboarding fixture и recovery после анализа |
| Empty email validation | 4 | Android `test01`, COA-7948 | **Pass** isolated | Нет negative email/server error и keyboard/accessibility matrix |
| Main navigation Today/Explore/Shop | 4 | Android `test03`, COA-8178 | **Pass** isolated | Не покрыты deep links, rotation/background/offline |
| Today/Daily Practice/Kegel entry | 5 | Android `test04`, COA-8087 | **Pass** isolated | Нет deterministic backend setup и полного player control matrix |
| Profile metrics/settings/logout | 4 | Android `test05`, COA-8500/8503 | **Pass** isolated | Нет anonymous-vs-authorized regression в отдельном suite |
| Configured Explore sections | 4 | Android Explore sections/Courses/card templates | **Pass**: Quick Tips, Master Classes, Private coaching; Courses absent | Config version/config fetch not asserted directly |
| Browse Programs replaces legacy | 5 | `testBrowseProgramsReplacesLegacyBlock` | **Error**: `Browse Programs` отсутствует; есть `ALL PROGRAMS`/`rvActivePrograms` | Product/config fix required |
| Quick Tips `coach_video` redirect | 5 | `testQuickTipVideoCardOpensVideoPlayer` | **Pass**: `viewPlayer`, play/pause, back | Не проверены actual stream start/completion analytics |
| Master Class / private coaching redirects | 4 | Explore destination tests | **Pass**: lesson/WebView and booking action | Нет redirect type/title analytics assertion |
| Update preservation | 5 | iOS `ReleaseSmokeTests:110–130` | Android **not run** | Two Android APKs + update fixture required |
| Push permissions, PDF entitlement, purchases | 4–5 | iOS separate/ignored suites | Android **not covered** | OS permission, entitlement and billing fixtures absent |

## Android additions

### Smoke controls

[AndroidSmokeTests.java](/Users/deedles/IdeaProjects/The-Coach/src/test/java/tests/AndroidSmokeTests.java:41) adds:

- empty email: visible/enabled state, disabled tap no-op, return to auth entry;
- email + OTP: explicit auth assertion rather than tabs-only detection;
- Today/Explore/Shop semantic navigation;
- Daily Practice card visibility/enabled/tap and Custom Kegel start screen;
- Profile metrics/settings, scroll to Logout, confirmation and Welcome destination.

[AndroidCoachFlowPageObject.java](/Users/deedles/IdeaProjects/The-Coach/src/test/java/lib/ui/android/AndroidCoachFlowPageObject.java:109) contains Android auth normalization, OTP custom-view typing, anonymous-state detection, diagnostics and fail-fast questionnaire detection. [DailyPlanPageObjectFactory.java](/Users/deedles/IdeaProjects/The-Coach/src/test/java/lib/ui/factories/DailyPlanPageObjectFactory.java:13) now routes Android to the dedicated page object.

### Explore v7 controls

[AndroidExploreTests.java](/Users/deedles/IdeaProjects/The-Coach/src/test/java/tests/AndroidExploreTests.java:43) covers:

- required section titles and Courses removal;
- legacy active-program replacement;
- square/challenge/coaching card templates, image areas and non-empty titles;
- Quick Tips `coach_video` opening the native video player and checking player/back controls;
- Master Class lesson/WebView destination;
- Private coaching WebView and booking action.

Video support is implemented in [AndroidExplorePageObject.java](/Users/deedles/IdeaProjects/The-Coach/src/test/java/lib/ui/android/AndroidExplorePageObject.java:267). Test cleanup handles the unfinished-activity confirmation after leaving a video player, preventing a failed navigation test from poisoning the next test.

## Findings

| ID | Приоритет | Статус | Evidence / impact |
|---|---|---|---|
| F-01 | P0 | Open | APK 1.40.3 shows `ALL PROGRAMS` and `rvActivePrograms`; required `Browse Programs` assertion fails. Explore v7 release-blocking. |
| F-02 | P1 | Resolved in 1.40.3 | Quick Tips `Finish Too Soon` now opens `viewPlayer`; native controls are accessible and enabled. Regression is now automated. |
| F-03 | P0 | Open / environment-product boundary | Fresh Android auth reaches `tvAnswerNum` questionnaire after OTP. The run cannot prove authorized smoke until this fixture is completed; manual state is not stable across logout/re-auth. |
| F-04 | P1 | Fixed in test architecture | Bottom tabs alone falsely classified anonymous Android shell as authorized; Profile marker check now prevents false green. |
| F-05 | P1 | Mitigated | Daily Practice is lazy/dynamic; locator is scoped to the section, but backend fixture still controls card presence/order. |
| F-06 | P1 | Open | Android clean install/update, push, billing, PDF entitlement, analytics events, offline and accessibility semantics are not covered. |
| F-07 | P2 | Mitigated | Explore video cleanup and `@After` isolation prevent detail/dialog state from cascading into the next test. |

## Quality assessment (0–5)

| Dimension | Score | Basis |
|---|---:|---|
| iOS smoke functional coverage | 3 | Strong P0 auth/Today/Profile/player baseline; release/update has ignored or environment-gated paths. |
| Android smoke functional coverage | 2 | Core paths added and isolated pass, but clean auth fixture blocks a repeatable suite. |
| Explore v7 coverage | 3 | Sections, cards, redirects and video are covered; Browse Programs fails and analytics are absent. |
| Test data determinism | 1 | No repository-owned Android reset/seed hook; dynamic Daily Practice and questionnaire state observed. |
| Diagnostics/traceability | 3 | Allure, screenshots/page source, issue IDs and file-level locators are available; external product defect IDs are still needed. |
| Release confidence | 1 | One P0 Explore mismatch plus insufficient Android clean-install/update evidence. |

## Verification log

Compilation and static checks:

```text
mvn -q -DskipTests test-compile                         PASS
git diff --check                                       PASS
```

Representative Android commands used the secure environment variables
`COACH_EXISTING_PROGRESS_EMAIL` and `COACH_EXISTING_PROGRESS_OTP`; their values
are intentionally omitted here.

```text
mvn -Dtest=tests.AndroidExploreTests -Dplatform=android \
  -Dandroid.app=/Users/deedles/Downloads/app-1.40.3-manProd-release.apk \
  -Dandroid.deviceName=TheCoach_API_30_ARM -Dandroid.udid=emulator-5554 \
  -Dandroid.noReset=true -Dandroid.fullReset=false test
  7 run, 6 pass, 1 error: F-01

mvn -Dtest=tests.AndroidSmokeTests#test01EmptyEmailKeepsContinueDisabled ... test
  1 run, 1 pass
mvn -Dtest=tests.AndroidSmokeTests#test02EmailAndOtpOpenAuthorizedToday ... test
  1 run, 1 fail-fast: F-03
mvn -Dtest=tests.AndroidSmokeTests#test03MainThreeTabNavigation ... test
  1 run, 1 pass (pre-seeded authorized state)
mvn -Dtest=tests.AndroidSmokeTests#test04DailyPracticeOpensCustomKegelStartScreen ... test
  1 run, 1 pass (pre-seeded authorized state)
mvn -Dtest=tests.AndroidSmokeTests#test05ProfileAndLogout ... test
  1 run, 1 pass (pre-seeded authorized state)
```

Generated evidence is available under `target/surefire-reports`,
`target/page-source` and `target/screenshots` for the last local runs.

## Action plan

### P0 — before Explore v7 release

- Fix/configure `Browse Programs`; remove legacy active-program title and collection.
- Supply Android post-auth onboarding fixture or make the existing-progress account bypass questionnaire deterministically.
- Re-run full ordered Android smoke and Explore suite on APK 1.40.3+.

### P1 — before broad Android release claim

- Add Android update smoke with old/new APKs.
- Add analytics capture/assertions for Explore open, launch and completed events with card title.
- Add push permission, offline/error/loading, duplicate-tap and full video playback/completion checks.
- Provision billing/PDF entitlement fixtures and add Android coverage.

