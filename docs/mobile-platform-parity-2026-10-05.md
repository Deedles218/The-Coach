# Android / iOS: расширение автотестов, 5 октября 2026

Покрытие добавлено по уточнённому контракту: актуальный Explore содержит программы,
Quick Tips, Master Classes и Private coaching; Courses и Body/Mind Practices отсутствуют.
Старые тесты не удалены и не отключены. Новый набор запускается отдельно от исторического Explore.

| Набор | Android | iOS |
|---|---:|---:|
| `OnboardingSlidesTests` | 2 общих сценария | Те же 2 сценария |
| `ConfiguredExploreTests` | 12 общих сценариев | Те же 12 сценариев |
| `AndroidSmokeTests` / `IOSSmokeTests` | 5 существующих сценариев | 5 добавленных эквивалентов |

Это количество методов, а не подтверждённых успешных UI-прогонов.

Kegel уже покрыт общим `src/test/java/tests/KegelExerciseTests.java`: пять сценариев
проверяют плеер и выход, звук/вибрацию, информацию стартового экрана,
feedback интенсивности и инструкции упражнений. `DailyPlanPageObjectFactory`
выбирает `AndroidKegelPageObject` на Android и `iOSDailyPlanPageObject` на iOS.
Дополнительные проверки есть в `AndroidSmokeTests` и `SmokeTests`.
Для Android набор требует dedicated Kegel account и `COACH_ANDROID_MUTABLE_UID`,
сверяемый с текущим UID перед изменением состояния. Эти существующие тесты не менялись;
успешный Android UI-прогон Kegel в рамках этой задачи не подтверждён.

## Реализация

- Проверки слайдов и GIF вынесены из iOS Page Object в общий `OnboardingSlidesPageObject`.
  Платформенные классы задают локаторы, индикатор, жесты и запись экрана.
  Android IDs подтверждены в APK 1.41.10: `viewPager`, `tabLayoutIndicator`, `tvHeader`,
  `ivPhoneMockup`, `btnGotIt`. Проверяются конфигурация выбранной цели, CTA,
  свайпы в обе стороны, медиа, завершение и отсутствие повторного показа после restart.
- Закрытие paywall останавливается перед первым ожидаемым слайдом и перед любым
  повторно появившимся слайдом. Новый сценарий не может скрыть повторный показ,
  автоматически нажимая GOT IT после restart.
- Explore использует существующие Android проверки и новые iOS реализации через общий
  `ExplorePageObject`. Общий набор включает пять текущих программных сценариев
  и семь сценариев дополнительных Android карточек. Перед восстановлением
  состояния сохраняется скриншот. На Android восстанавливаются программа, этап и процент.
- iOS smoke переиспользует текущие Coach/DailyPlan/Onboarding Page Objects.
  Закрытие стартовых paywall выполняется до подготовки авторизации.
  Общий helper прокручивает Profile до FAQ и возвращается через Today перед logout/cleanup
  из Shop. В iOS Shop добавлен подтверждённый содержимым локатор ссылки `The Coach Store`.
- Добавлены `ExploreParitySuite`, `OnboardingSlidesSuite`, `IOSParitySmokeSuite`.
  Android runner направляет `--suite explore` в новый общий набор и поддерживает `--suite slides`.

Файлы реализации:

- Тесты: `OnboardingSlidesTests.java`, `ConfiguredExploreTests.java`, `IOSSmokeTests.java`,
  `OnboardingSlidesFixtureUnitTests.java` в `src/test/java/tests/`.
- Слайды: `OnboardingSlidesPageObject.java`, `android/AndroidOnboardingSlidesPageObject.java`,
  `ios/iOSOnboardingSlidesPageObject.java`, `factories/OnboardingSlidesPageObjectFactory.java`
  в `src/test/java/lib/ui/`.
- Подготовка и Explore: `OnboardingPageObject.java`, `android/AndroidOnboardingPageObject.java`,
  `ExplorePageObject.java`, `ios/iOSExplorePageObject.java`, `CoachFlowPageObject.java`,
  `ios/iOSCoachFlowPageObject.java` в `src/test/java/lib/ui/`.
- Suites: три указанных выше класса в `src/test/java/suites/`.
- Scripts: `capture_onboarding_slides_config.py`, `capture_android_onboarding_slides_config.py`,
  `run_android_parity.py`, `test_onboarding_slides_config.py`, `test_android_parity_runner.py`.

## Android-конфигурация слайдов

Нужна активная конфигурация именно проверяемой Android-сборки. iOS-файл не подходит:
Java проверяет `appPackage`; iOS продолжает проверять `bundleId`.

Для debuggable сборки:

```sh
python3 scripts/capture_android_onboarding_slides_config.py \
  --serial emulator-5554 --package com.vamapps.thecoach \
  --goal 'BOOST OVERALL HEALTH' --output target/onboarding-slides/android.properties
```

Для release-сборки передать `--config /absolute/path/android-active-config.json`
вместо `--serial`. Источник — экспорт активного Firebase-кеша этой Android-сборки,
с объектом `configs`, а не APK defaults или конфигурация другой платформы.
Экспорт валидирует включённый флаг, фильтр цели, порядок, заголовки и GIF metadata.
При ошибке успешная старая фикстура не сохраняется.

## Целевой запуск

Свежая установка на явно выбранном тестовом эмуляторе:

```sh
python3 scripts/run_android_parity.py --serial emulator-5554 --suite slides \
  --method testNewUserSlidesNavigationMediaAndCompletion \
  --slides-fixture target/onboarding-slides/android.properties \
  --new-apk /Users/deedles/Downloads/app-1.41.10-manProd-release.apk
```

Если первый непройденный слайд уже открыт, использовать `--prepared-slides`
вместо `--new-apk`. Обычный logout не доказывает свежесть первого посещения.
COA-9431 запускается отдельно через `--method testExistingProgressUserDoesNotSeeSlides`
с одобренным existing-progress аккаунтом из текущего secret-backed runner.

```sh
python3 scripts/run_android_parity.py --serial emulator-5554 --suite explore
mvn -q test -Dtest=suites.IOSParitySmokeSuite -Dplatform=ios
mvn -q test -Dtest=suites.ExploreParitySuite -Dplatform=ios
```

Для iOS указать bundle/UDID/версию ОС текущей сборки и existing-progress секреты,
как в существующих командах проекта. iOS-команды слайдов из
`onboarding-slides-automation.md` сохранены. Полная языковая/device matrix не запускалась.

## Проверка

Результаты конкретных запусков и оставшиеся ограничения фиксируются ниже.

- `mvn -q test-compile -DskipTests`: PASS.
- `mvn -q -Dtest=tests.OnboardingSlidesFixtureUnitTests test`: 5 PASS.
- `python3 -m unittest discover -s scripts -p test_onboarding_slides_config.py -v`: 5 PASS.
- `python3 -m unittest discover -s scripts -p test_android_parity_runner.py -v`: 3 PASS.
- `python3 -m py_compile scripts/capture_onboarding_slides_config.py scripts/capture_android_onboarding_slides_config.py scripts/run_android_parity.py`: PASS.
- `IOSSmokeTests#test01EmptyEmailKeepsContinueDisabled`: PASS на prod 1.13.33;
  после добавления закрытия paywall также PASS на preprod 1.13.29 (42.455 с).
- Первый полный запуск `IOSSmokeTests` на preprod: 5 ERROR в подготовке стартового
  экрана; в evidence виден paywall. Исправлено в новом тестовом helper через существующий
  `OnboardingPageObject.closePaywallsAndPopups()`; целевой повтор прошёл.
- После исправления стартового paywall полный preprod smoke: 2 PASS / 3 ERROR.
  Повтор `IOSSmokeTests#test03MainThreeTabNavigation+test05ProfileAndLogout` после исправлений
  FAQ, Shop и нормализации через Today: **2 PASS, 0 failures/errors/skips**.
  Итого четыре из пяти методов подтверждены успешными целевыми запусками.
  Оставшийся `test04DailyPracticeOpensCustomKegelStartScreen` остановился на
  `First Daily Practice item is not displayed`: у текущей фикстуры Sex Is a Skill,
  Stage 1 of 8, вместо ожидаемой Kegel-карточки. Для отдельного Kegel-аккаунта поддерживаются
  существующие `COACH_KEGEL_PLAYER_EMAIL` / `COACH_KEGEL_PLAYER_OTP`; прогресс не сбрасывался.
- Android baseline `ExploreTests#testCurrentExploreNavigationAcrossPlatforms`: ERROR
  до проверок Explore. `TimeoutException: Android authorization did not reach the dashboard
  or the post-auth questionnaire`; source показывает ENTER SECURITY CODE и `pbLoading`.
  Причина между тестовыми данными, сервисом и приложением не установлена; требуется
  проверка OTP/окружения и ответа авторизации. Не засчитывается как проверка нового набора.
- Android release 1.41.10: получение активного кеша заблокировано `run-as: package not debuggable`.
  Полный UI-прогон слайдов требует Android active-config export и свежего first-visit состояния.
- `ConfiguredExploreTests#testConfiguredSectionsAreDisplayed` на iOS preprod 1.13.29:
  **1 ERROR, 0 skipped** после авторизации, на проверке каталога.
  `TimeoutException: Cannot find a visible element by swiping up. Explore section absent: Quick Tips`.
  В source отображаются `ALL PROGRAMS`, `SEXUAL HEALTH`, `COURSES`, `VIDEO COURSE`:
  установленная сборка/активная конфигурация показывает исторический каталог,
  противоречащий уточнённому контракту. Следующий шаг — проверить build/config с актуальным
  Explore и повторить целевой сценарий; ожидания не ослаблялись под старый каталог.
  Новые iOS-локаторы конфигурируемых карточек Explore ещё требуют проверки на сборке
  с доступным актуальным каталогом; наличие реализации не считается PASS.

Локальные логи/Allure: `target/mobile-parity-20261005/`;
Android baseline archive: `target/android-parity-20261005-161022/`.
