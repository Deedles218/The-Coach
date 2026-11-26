# Онбординг программ: UI v3 / COA-9583

Основание — «Доработки и баги онбординга.docx» и актуализированные ручные
кейсы COA-9786–9805 от 24.11.2026 и уточнение заказчика от 25.11.2026:
в модалке Stage для программ со стадиями, Day для программ с днями.
Проверки используют существующие
JUnit 4, CoreTestCase, Page Object, явные ожидания и Allure.

## Контракт

- После Paywall обязателен первый interstitial с Show my journey.
- Recommendation: Sounds good / Choose a different journey закреплены под
  прокручиваемым содержимым; старый ведущий заголовок удалён.
- Main picker: текущая Main первая, выбрана, Recommended; Change main journey
  disabled до смены выбора, Go back возвращает без подтверждения выбора.
- После принятия или замены Main — отдельный interstitial с фактическим именем
  программы и Choose additional journeys.
- При нуле Secondary Continue disabled; переход в Summary выполняется через
  отдельную Skip. Счётчик и выбор сохраняются после Learn more.
- Summary информационный: Main Journey / Added, без Edit My Programs,
  Go to my daily plan и новый footer. Заголовок может появиться до загрузки
  строк: тест ожидает данные и затем строго проверяет весь состав.
- После уточнения COA-9868 от 26.11.2026 и `+ Add a program`, и `ALL`
  открывают All Programs. Explore проверяется отдельно через основную навигацию.
- All Programs: недобавленные первыми, добавленные последними с Added и без
  выбора; Add Program (N) возвращает раскрытый My programs.
- Explore: Add to Daily Plan без подтверждения добавления; Remove from Daily
  Plan с отменой/подтверждением. Удаляется только неактивная Secondary,
  поскольку поведение удаления Main/Active в требованиях не определено.
- My programs показывает ту же единицу и счётчик, что Daily Plan конкретной
  программы: Day либо Stage. Для Day используются `current_program_day` и
  `total_program_stages`, для Stage — `current_stage` и `total_stages`.
  Наличие метаданных модуля само по себе не определяет тип программы.
  Абсолютный день остаётся отдельным инвариантом сохранности прогресса.
- Прогресс `in_progress` рассчитывается по абсолютному дню:
  `min(round(100 * (day - 1) / total_days), 99)`. `round` соответствует Python
  (половины округляются к чётному): последний день 60/60 даёт 98%, а не 99%.
  Для `completed` ожидается 100% и подпись `Completed` без счётчика модуля.
  `ProgramSelectionState` независимо проверяет формулу API перед сравнением
  UI/API; совпадение двух неверных процентов не считается успехом.

## Android

`AndroidProgramOnboardingTests` создаёт отдельный чистый анонимный профиль для
каждого метода. Нужны явно указанные APK, `fullReset=true`, `noReset=false`,
цель и независимое ожидаемое имя рекомендованной Main. Покупки не выполняются:
используется доступный в приложенных preprod debug сборках выход из Paywall.
Успешная покупка не считается покрытой этим набором.

| Метод | Основные кейсы |
| --- | --- |
| interstitialsAndMainReplacementUseTheSavedJourney | COA-9787/9788/9789 |
| zeroSecondaryUsesSkipWhileContinueIsDisabled | COA-9790 |
| previewOfAnotherProgramPreservesSelectionWhenItsActionRequiresScrolling | COA-9790/9792 |
| secondaryCounterPreviewAndSummaryPreserveTheChosenSet | COA-9790/9791/9792/9803 |
| myProgramsUsesEachProgramsDayOrStage | COA-9793/9804 |
| allProgramsOrdersMembershipAndHidesSelectionForAddedPrograms | COA-9797/9798/9805 |
| allProgramsAddsTwoJourneysAndReturnsTheExpandedSelector | COA-9797 |
| exploreAddsWithoutConfirmationAndRemovesOnlyAfterConfirmation | COA-9796/9797/9798 |
| exploreOrdersAddedProgramsAfterUnaddedPrograms | COA-9798 |
| firstEntrySelectorTutorialDoesNotRepeatAfterColdLaunch | COA-9802 |

Сервер Appium запускается отдельно, например `appium --base-path / --port 4825`.
Команда для Her:

```sh
mvn -B -ntp -Dtest=tests.AndroidProgramOnboardingTests \
  -Dplatform=android -Dandroid.udid=DEVICE_SERIAL \
  -Dandroid.deviceName=DEVICE_NAME -Dandroid.platformVersion=13 \
  -Dappium.url=http://127.0.0.1:4825/ \
  -Dandroid.app=/path/to/app-2.20.6-herPreprod-debug.apk \
  -Dandroid.appPackage=com.vamapps.thecoach.forher \
  -Dandroid.appActivity=com.vamapps.thecoach.MainActivity \
  -Dandroid.fullReset=true -Dandroid.noReset=false \
  -Donboarding.goal='Cultivate desire' \
  -Dcoach.programOnboarding.expectedMain='Path to Desire' \
  -Dallure.results.directory=target/onboarding-her/allure-results test
```

Для Him используются APK `app-1.45.6-manPreprod-debug.apk`, пакет
`com.vamapps.thecoach`, цель `Boost overall health` и ожидаемая Main
`Overall Health`. Наборы выполняются последовательно на одном телефоне.
Один сценарий запускается через стандартный фильтр Surefire
`-Dtest=tests.AndroidProgramOnboardingTests#methodName`.

Skip и подтверждённая замена Main теперь проверяются до правильного Daily Plan,
состава My programs и холодного запуска. Отдельный сценарий открывает Learn More
другой программы в конце списка и проверяет сохранение выбора и Summary.
Прокрутка ищет саму кнопку/чекбокс: частично видимая карточка не означает,
что её действия уже доступны в нативном дереве. Стартовый системный запрос
уведомлений Samsung/Android обрабатывается до Welcome; неожиданные запросы
других разрешений приводят к ошибке.

`fullReset=true` здесь означает настоящую чистую установку на выделенном
устройстве. Очистка данных/изолированный профиль поверх обновлённого APK может
оставлять `is_new_user=false`, поскольку приложение сравнивает firstInstallTime
с lastUpdateTime. Такой профиль не заменяет clean-install fixture. Существующий
профиль с прогрессом для этого набора использовать нельзя. Продуктовое
ожидание COA-9862 (Added и отсутствие выбора у добавленных) остаётся строгим.

## Android Restart / COA-9864 и COA-9866

`AndroidProgramRestartTests` содержит отдельный сценарий
`completedMainRestartsOnlyAfterConfirmationAndPreservesSecondaryProgress`:
Explore → шестерёнка → Reset Progress → NO сохраняет полный API snapshot;
повторное открытие → YES переводит Main из Completed/100% в первый день/0%.
Проверяются неизменность Main/Secondary/Active, прогресса других программ,
Daily Plan и My programs, а также полное состояние после холодного запуска.

Для каждого запуска нужен отдельный подготовленный QA-профиль: Main завершена
после прохождения урока последнего дня, она же Active; хотя бы одна Secondary
имеет ненулевой прогресс. Привязки UID/backend/APK и типы Day/Stage задаются
так же, как для `AndroidProgramProgressTests`. Проверка изменяет прогресс Main;
API-reader остаётся read-only, сброс выполняется только через UI. Тест не
подготавливает серверные дни, не восстанавливает завершённый прогресс и не
используется на отложенных аккаунтах Retain. Перед повтором нужен заново
подготовленный Completed fixture. Все единицы программ проверяются до Reset.

```sh
mvn -B -ntp -Dtest=tests.AndroidProgramRestartTests \
  -Dplatform=android -Dandroid.udid=DEVICE_SERIAL \
  -Dandroid.platformVersion=13 -Dappium.url=http://127.0.0.1:4825/ \
  -Dandroid.appPackage=com.vamapps.thecoach.forher \
  -Dandroid.noReset=true -Dandroid.fullReset=false \
  -Dcoach.programProgress.unit.for_her=Day \
  -Dcoach.programProgress.unit.sex_skill_app=Day \
  -Dcoach.programProgress.unit.sexting_woman=Day test
```

Пример предполагает именно указанный набор Main/Secondary. Для другого
набора нужно объявить тип каждой сохранённой программы. Секреты, UID и hash
привязки в команду и исходники не включаются — используются переменные окружения.

## Сохранённый прогресс / COA-9853

`AndroidProgramProgressTests` выполняется отдельно, **до чистых установок**.
Метод `allSavedProgramsUseTheDayFormulaAndDisplayItsPercentage` проверяет все
сохранённые Main/Secondary, включая завершённые программы, без переключения
Active Program и без изменения прогресса. `ProgramProgressFormulaUnitTests`
проверяет все 14 программ, первый/последний день, округление половин и cap 99.
Он принимает только установленное приложение с `noReset=true`,
`fullReset=false`, без `android.app`. Нужны Main и Secondary с различимыми
сохранёнными позициями Day/Stage и ненулевым прогрессом хотя бы в одной из
проверяемых программ. Тест сравнивает UI/API при переключении и холодном запуске
и восстанавливает исходную Active Program. Контент не завершается и данные
для позднего дня/стадии тестом не подготавливаются. Для программ со стадиями
отображение проверяется по Stage; доступный в API абсолютный день также
сравнивается независимо от подписи, чтобы сброс не скрывался за прежней стадией.

Привязки передаются через окружение, без секретов в Java/fixtures:

- `COACH_PROGRAM_SELECTION_CASE=COA-9794`;
- `COACH_PROGRAM_SELECTION_ANDROID_UID` — Firebase UID выделенного профиля;
- `COACH_PROGRAM_SELECTION_BACKEND_HASH` — SHA-256 backend user_id;
- `COACH_PROGRAM_SELECTION_ANDROID_APK_SHA256` — SHA-256 проверенного APK;
- для зарегистрированного профиля — подтверждённый
  `COACH_PROGRAM_SELECTION_EMAIL`; без email допустим только явно привязанный
  анонимный профиль этого сценария.

Для сравнений UI/API также задаётся проверенный тип **каждой** программы:
`-Dcoach.programProgress.unit.<program_id>=Day` либо `Stage`.
Без этой привязки проверка завершается ошибкой подготовки данных; тип нельзя
угадывать по наличию полей модуля. Это относится и к iOS-проверкам UI/API.
Проверка свежего Android-онбординга отдельно сравнивает модалку с Daily Plan
каждой программы, включая точные значения числителя и знаменателя.

```sh
mvn -B -ntp -Dtest=tests.AndroidProgramProgressTests \
  -Dplatform=android -Dandroid.udid=DEVICE_SERIAL \
  -Dandroid.deviceName=DEVICE_NAME -Dandroid.platformVersion=13 \
  -Dappium.url=http://127.0.0.1:4825/ \
  -Dandroid.appPackage=com.vamapps.thecoach.forher \
  -Dandroid.noReset=true -Dandroid.fullReset=false \
  -Dallure.results.directory=target/onboarding-progress/allure-results test
```

API-reader остаётся read-only на Android. Поддержка COA-9795 и его
подтверждённого cross-platform профиля сохранена. Анонимный режим добавлен
только для COA-9794, с UID, backend identity и APK binding. Нормальный запуск
приложения обновляет просроченный SDK-токен; reader не подменяет credentials.
При наличии SDK-записи `GET_TOKEN_RESPONSE.<uid>` reader использует её вместо
устаревшего снимка `cachedTokenState` в `FIREBASE_USER`. Запись должна относиться
к тому же UID; проверки subject, срока действия и привязок профиля сохраняются.

## Существующие iOS-сценарии

Ожидания приведены к новому тексту, interstitial, Skip, Summary, подписям
Day/Stage по типу программы и multi-add. Для COA-9791 нужны отдельные профили **на выборе Secondary**
с подготовленным исходным набором; редактирование через Summary удалено из
сценария. Варианты замены и очистки набора выполняются разными тестами.
Для multi-add COA-9797 fixture должен содержать `added.ids` минимум из двух
недобавленных программ. Контракт Explore Add to Daily Plan отделён от обоих входов в All Programs.
Актуальные результаты iOS: [основной ретест](coa-9583-ios-retest-2026-11-25.md)
и [свежий онбординг Her и контент](ios-backlog-content-retest-2026-11-26.md).

Нативный iOS-прогон требует актуальной iOS-сборки и существующих UID-bound
fixtures. Проверки StoreKit сохраняют прежние явные purchase/sandbox flags.

## Проверка кода и отчётность

```sh
mvn -B -ntp \
  -Dtest=tests.ProgramProgressFormulaUnitTests,tests.ProgramSelectionStateUnitTests,tests.ProgramProgressFixtureUnitTests,tests.ProgramRestartStateUnitTests,tests.AndroidProgramSelectionControlsUnitTests,tests.AndroidOnboardingNotificationUnitTests,tests.AndroidProgramCardSnapshotUnitTests,tests.IOSMyProgramsScrollUnitTests,tests.AndroidOnboardingTransitionUnitTests test
python3 -m unittest discover -s scripts -p test_program_selection_api.py -q
PYTHONPATH=scripts python3 -m unittest scripts/test_android_email_enforcement.py
```

Негативные проверки обнаруживают изменение только абсолютного дня,
отсутствующий/некорректный день, неправильную привязку профиля и попытку
создать driver с reset для сохранённого профиля. Скроллинг существующего
iOS-селектора проверяется его прежними offline тестами.

Известные расхождения сборок с документом остаются обычными падениями
assertions; Ignore, xfail и ослабление ожиданий для получения зелёного
прогона не используются. CoreTestCase сохраняет failure screenshot,
page source, журналы и Allure. При последовательных прогонах разных APK
JUnit XML из `target/surefire-reports` сохраняется отдельно после каждого
прогона, поскольку один и тот же класс создаёт одинаковое имя XML.

Полная пиксельная сверка шрифтов/цветов, A/B OFF, сетевые отказы,
успешная покупка и неопределённые shortcuts настроек/удаления не входят в
этот Android-набор. Существующие независимые API/миграционные кейсы не
переписываются под UI v3.

## Объединение iOS и Android, 26 ноября 2026

Ветка `codex/onboarding-ios-android-20261126` собрана от `origin/main` / `5550ffb`
из изменений `codex/coa-9583-onboarding-v3` и `codex/coa-9583-ios-v3-retest`.
В общих API-проверках сохранены обе группы unit-тестов и абсолютный день;
актуальный iOS-флоу включает уточнённый маршрут + Add → All Programs.
Исходные рабочие деревья и продолжающийся Android-прогон не изменялись.

Объединённые исходники проверены в изолированной копии с Java 8:
**100 Java unit-тестов и 58 Python-тестов прошли, без failures/errors/skipped**.
Maven скомпилировал все 294 Java-файла тестового проекта. Команды:

```sh
mvn -B -ntp -Dtest=tests.ProgramSelectionStateUnitTests test
mvn -B -ntp -Dtest=tests.ProgramProgressFormulaUnitTests,tests.ProgramProgressFixtureUnitTests,tests.ProgramRestartStateUnitTests,tests.AndroidProgramSelectionControlsUnitTests,tests.AndroidOnboardingNotificationUnitTests,tests.AndroidProgramCardSnapshotUnitTests,tests.AndroidSelectorLayoutLocatorUnitTests,tests.IOSProgramSelectionFlowUnitTests,tests.ProgramCounterUnitTests,tests.IOSExploreLocatorUnitTests,tests.IOSMyProgramsScrollUnitTests,tests.AndroidOnboardingTransitionUnitTests,tests.AndroidOnboardingHintUnitTests,tests.AndroidPostAuthPreparationUnitTests,tests.AndroidPostLoginGoalsUnitTests test
python3 -m unittest discover -s scripts -p test_program_selection_api.py -q
python3 -m unittest discover -s scripts -p test_android_email_enforcement.py -q
python3 -m unittest discover -s scripts -p test_ios_device_evidence.py -q
git diff --cached --check
```

Повторный UI-прогон при объединении не выполнялся. Предыдущие результаты
остаются отдельными свидетельствами: Android Her 2.20.6 / Him 1.45.6 —
24 passed и 2 продуктовых падения COA-9862; iOS — результаты и ограничения
в двух отчётах выше, включая расхождение Day/Stage COA-9833.
Unit-тесты не подтверждают исправление этих дефектов приложения.
