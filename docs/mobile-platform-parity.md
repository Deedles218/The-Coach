# The Coach for Him: проверка на физическом Android и iOS

Подтверждены **15 из 15** кейсов: каждый прошёл на Samsung Galaxy A32 по USB (Android 13, The Coach for Him 1.44.1, сборка 353) и на iPhone Simulator (iOS 26.5, The Coach for Him 1.13.33). Девять результатов Android получены в последней партии, шесть подтверждены предыдущими прогонами на том же телефоне.

В каждом журнале ниже один целевой тест без ошибок, падений или пропусков. Это результаты локальных прогонов до коммита. Журналы, результаты Allure для Android и индекс `verified-physical-android-ios-parity.json` сохранены локально в `target/mobile-next-runtime/`.

| Xray | Проверка | Galaxy A32 PASS | iOS Simulator PASS |
| --- | --- | --- | --- |
| [COA-7204](https://the-coach.atlassian.net/browse/COA-7204) | Profile открывается с основных вкладок | `physical15-COA-7204-android.log` | `physical15-COA-7204-ios.log` |
| [COA-7917](https://the-coach.atlassian.net/browse/COA-7917) | Today сохраняет оформление при смене системной темы | `physical15-COA-7917-android.log` | `parity-ready-7917-ios.log` |
| [COA-7947](https://the-coach.atlassian.net/browse/COA-7947) | Валидный email активирует Continue | `physical15-COA-7947-android.log` | `parity-test-model-7947-ios.log` |
| [COA-7980](https://the-coach.atlassian.net/browse/COA-7980) | Отметка о завершении урока сохраняется после перезапуска | `physical15-COA-7980-android.log` | `coa7980-test-fresh-device.log` |
| [COA-8228](https://the-coach.atlassian.net/browse/COA-8228) | Свайп вниз закрывает селектор программ с анимацией | `physical15-COA-8228-android.log` | `parity-ready-8228-ios.log` |
| [COA-8230](https://the-coach.atlassian.net/browse/COA-8230) | Селектор программ прокручивается вертикально | `physical15-COA-8230-android.log` | `parity-ready-8230-ios.log` |
| [COA-9329](https://the-coach.atlassian.net/browse/COA-9329) | Карточка коучинга открывает нужную страницу внутри приложения | `physical15-COA-9329-android.log` | `coa9329-ios-explore.log` |
| [COA-9331](https://the-coach.atlassian.net/browse/COA-9331) | В Explore отсутствует блок Active Program | `physical15-COA-9331-android.log` | `parity-nav-9331-ios.log` |
| [COA-9332](https://the-coach.atlassian.net/browse/COA-9332) | Карточки Browse Programs читаемы по всей карусели | `physical15-COA-9332-android.log` | `parity-nav-9332-ios.log` |
| [COA-9471](https://the-coach.atlassian.net/browse/COA-9471) | Шеринг с партнёром открывает и закрывает системное меню | `physical-COA-9471-android.log` | `parity-native-9471-ios.log` |
| [COA-9481](https://the-coach.atlassian.net/browse/COA-9481) | Завершение видео открывает SharingVideo | `physical-COA-9481-android.log` | `parity-final-share-9481-ios.log` |
| [COA-9482](https://the-coach.atlassian.net/browse/COA-9482) | Оба действия передают ожидаемые тексты и ссылки | `physical-COA-9482-android.log` | `parity-final-share-9482-ios.log` |
| [COA-9483](https://the-coach.atlassian.net/browse/COA-9483) | SharingVideo появляется после каждого из двух просмотров | `physical-COA-9483-android.log` | `parity-final-share-9483-ios.log` |
| [COA-9503](https://the-coach.atlassian.net/browse/COA-9503) | Баннер Explore открывает страницу Кэти внутри приложения | `physical-COA-9503-android.log` | `parity-banner-9503-ios.log` |
| [COA-9504](https://the-coach.atlassian.net/browse/COA-9504) | Баннер полностью виден и не перекрывает интерфейс | `physical-COA-9504-android.log` | `parity-banner-ios.log` |

В Android-прогоне COA-7980 тест завершил G-Zone Techniques (Sex Is a Skill, этап 3), перезапустил приложение, проверил тот же аккаунт и вернулся на исходный этап. В Allure сохранены обе галочки: до и после перезапуска. Успешный прогон выполнен после повторного подключения телефона; прерванная сессия сохранена в `physical15-usb-disconnect-COA-7980-android.log`. В COA-7947 тест проверил доступность Continue, не запрашивая код.

## Повторный запуск

Классы сценариев: `MobileNextCasesTests` (COA-7917, COA-7980), `CoachProfileTests` (COA-7204), `AndroidTestModelTests` и `TestModelAutomationTests` (COA-7947), `AndroidProgramSelectorTests` и `TodayProgramSelectorTests` (COA-8228, COA-8230), `ConfiguredExploreTests` (COA-9329, COA-9331, COA-9332, COA-9503, COA-9504), `MobileSharingTests` (COA-9471, COA-9481, COA-9482, COA-9483).

Использовать Appium, явный UDID устройства или симулятора и установленную сборку. Для подготовленного профиля передать `COACH_EXISTING_PROGRESS_EMAIL` из хранилища секретов и оставить `noReset=true`, `fullReset=false`, `TEST_ISOLATION_MODE=logout`. Тест проверит профиль через интерфейс и сохранит сессию. Ключ для Explore — `-Dcoach.explore.preparedProfile=true`, для SharingVideo — `-Dcoach.sharing.preparedProfile=true`, для COA-8228 и COA-8230 — `-Dcoach.selector.preparedProfile=true`.

Для шеринга открыть первый этап программы Sex Is a Skill: на Android проверен урок Her Pleasure Anatomy, на iOS — Mastering Her Orgasm. Обе кнопки проверяются в системном предпросмотре, сообщение не отправляется. На общем Android-профиле были выполнены только сценарии выхода; исходные программа, этап и процент восстановлены (`parity-restore-android-after-restart.log`).

```sh
mvn -DskipTests test-compile
mvn '-Dtest=ConfiguredExploreTests#testCoa9504ExploreBannerRendersIntact' -Dcoach.explore.preparedProfile=true surefire:test
mvn '-Dtest=MobileSharingTests#testCoa9482SharePayloadMatchesSelectedAction' -Dcoach.sharing.preparedProfile=true surefire:test
mvn '-Dtest=ExploreStateRestorerUnitTests,IOSConfiguredExploreContractUnitTests,IOSExploreRecoveryUnitTests,IOSExploreLocatorUnitTests,SelectorMotionUnitTests,MobileNextCasesUnitTests' surefire:test
```

В UI-команды добавить `-Dplatform=android` или `-Dplatform=ios`, адрес Appium и параметры выбранного устройства. На Galaxy A32 для COA-7917 и COA-7980 передать `-Dcoach.mobileCases.fixture=src/test/resources/mobile-next-cases/android-physical-1.44.1.properties` и соответствующий флаг `-Dcoach.coa7917.preparedProfile=true` или `-Dcoach.coa7980.preparedProfile=true`. COA-7980 требует отдельного профиля с незавершённым уроком: Finish меняет его прогресс, затем тест открывает исходный день после перезапуска. Для проверки системной темы и анимации селектора на USB-телефоне нужен `-Dcoach.android.physicalUiSettings=true`; тесты восстанавливают прежние настройки. COA-7204 использует `-Dcoach.profile.preparedProfile=true`. COA-7947 использует `COACH_VALID_EMAIL_WITHOUT_PROGRESS` и `-Dandroid.recovery.mode=relaunch`.

Для COA-9329 на этой Android-сборке передать `-Dcoach.explore.preparedProfile=true` и четыре параметра, заключив значения с пробелами в кавычки: `coach.explore.webview.title=QA coaching_session_card 04 -> webview`, `coach.explore.webview.section=QA Carousel - coaching_session_card`, `coach.explore.webview.heading=Bring the passion back, with a coach in your corner`, `coach.explore.webview.action=Book a private 1:1 session`.

Для существующего выражения XPath в тесте iOS-локатора локальному JDK нужен лимит выше стандартных 100 операторов:

```sh
mvn -B -Djdk.xml.xpathExprOpLimit=200 \
  -Dtest=DailyPlanCompletionMarkerUnitTests,BookmarksLocatorUnitTests,SelectorMotionUnitTests,MobileNextCasesUnitTests,ExploreStateRestorerUnitTests surefire:test
```

Пример для одного телефона, подключённого по USB. Предварительно открыть первый этап Sex Is a Skill на подготовленном QA-профиле и закрыть подсказку о переключении программ, если она появилась.

```sh
qa_android_udid=$(adb -d get-serialno) && [ -n "$qa_android_udid" ] &&
ANDROID_UDID="$qa_android_udid" TEST_ISOLATION_MODE=logout \
  mvn -B -Dplatform=android -Dandroid.platformVersion=13 \
  -Dandroid.appPackage=com.vamapps.thecoach -Dandroid.appActivity=.MainActivity \
  -Dandroid.noReset=true -Dandroid.fullReset=false -Dandroid.autoGrantPermissions=false \
  -Dappium.url=http://127.0.0.1:4790 -Dcoach.sharing.preparedProfile=true \
  '-Dtest=MobileSharingTests#testCoa9481SharingPopupOnNaturalCompletion' surefire:test
```
