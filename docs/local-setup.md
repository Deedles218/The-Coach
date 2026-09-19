# Настройка окружения и запуск тестов The Coach

Инструкция для тестировщика, который впервые запускает этот репозиторий.
Команды выполняются из корня проекта, если не указано другое. Примеры рассчитаны
на macOS и Terminal с zsh/bash. Значения в `<...>` нужно заменить своими.

Порядок: получить сборку → установить инструменты → настроить устройство →
запустить Appium → выполнить один тест → подготовить аккаунты → запустить набор.

## 1. Что получить у команды заранее

| Что | Что именно запросить |
| --- | --- |
| Доступ к GitHub | Доступ к репозиторию и название рабочей ветки |
| Сборка приложения | Для Simulator — `.app`, собранный под `iphonesimulator`; для iPhone — подходящий подписанный `.ipa` или доступ к TestFlight; для Android — `.apk` |
| Стенд | Сборка и backend должны соответствовать друг другу; уточнить необходимость VPN |
| Идентификатор приложения | iOS bundle ID; для Android package и launch activity, если отличаются от значений ниже |
| Тестовые аккаунты | Email, способ получения OTP, разрешённые действия, программа, текущий день и подписка |
| Для полного iOS Smoke | Последовательность accessibility ID шагов onboarding для выбранной сборки и конфигурации |
| Для настоящего iPhone | Доступ к Apple Developer Team и подписи WebDriverAgent, если они ещё не настроены |

В репозитории находятся **автотесты**, а не исходники мобильного приложения.
Команда Maven не собирает `.app`, `.ipa` или `.apk`. Эти артефакты предоставляет команда приложения.
Для первого теста пустого email аккаунт и OTP не требуются.

## 2. Выбрать платформу

| Вариант | Компьютер и инструменты | Особенности |
| --- | --- | --- |
| iOS Simulator | Mac, полный Xcode, Simulator runtime, XCUITest | Основной путь для Jira/test-model: часть проверок читает локальный cache Simulator и записывает видео |
| Настоящий iPhone | Mac, Xcode, XCUITest, USB, подпись WebDriverAgent | Подходит для обычных iOS UI-тестов; Simulator evidence и соответствующие test-model сценарии сюда не переносятся |
| Android Emulator / телефон | Android Studio/SDK, UiAutomator2 | Запускать Android-классы; `suites.SmokeSuite` — iOS-набор |

iOS локально требует macOS. Android поддерживается инструментами Appium также
на Windows/Linux, но приведённые команды настройки путей предназначены для Mac.
Python runner с Keychain и iOS shell-скрипты рассчитаны на macOS.

## 3. Установить общие инструменты

### 3.1. Версии и зависимости

Ниже — ориентир по инструментам, обнаруженным на компьютере автора при подготовке
инструкции. Это не утверждение о совместимости всех возможных сборок и ОС.

| Инструмент | Ориентир | Для чего |
| --- | --- | --- |
| Git | Установленная поддерживаемая версия | Скачать проект |
| JDK | Java 8 для Maven этого проекта | В `pom.xml` target/source 8 и старый AspectJ 1.9.5 |
| Maven | 3.9.1 на компьютере автора; ветка 3.9.x | Зависимости, компиляция, запуск, Allure |
| Node.js / npm | 22.14.0 / 10.9.2 на компьютере автора | Appium server |
| Appium server | **2.19.0** | Сервер автоматизации |
| XCUITest driver | **9.10.5** | iOS |
| UiAutomator2 driver | **4.2.9** | Android |
| Python | 3.13.2 на компьютере автора | Локальный test-model runner и сбор evidence |
| ffmpeg | Доступен в PATH | Видео и кадры в тестах селектора/загрузки Simulator |
| IntelliJ IDEA | Необязательно | Редактирование и запуск Maven из IDE |
| Appium Inspector | Необязательно | Поиск локаторов и проверка capabilities |

Java-библиотеки ставятся Maven автоматически: Appium Java Client 8.6.0,
Selenium 4.13.0, JUnit 4.12, Allure JUnit 2.8.1. Отдельно устанавливать их не нужно.
Appium Java Client и Appium server — разные компоненты; сервер ставится через npm.
Python-скрипты основного локального запуска используют стандартную библиотеку Python.

### 3.2. Java, Maven, Node.js

Если Git ещё не установлен на Mac, выполнить `xcode-select --install` и завершить
установку Command Line Tools в системном окне. Для iOS далее всё равно нужен полный Xcode.

1. Установить JDK 8 подходящей архитектуры, например из [Temurin 8](https://adoptium.net/temurin/releases/?version=8).
2. Скачать Binary archive Maven 3.9.x со [страницы Maven](https://maven.apache.org/download.cgi), распаковать, например в `$HOME/tools/apache-maven-3.9.1`. Для точного повторения версии автора использовать архив 3.9.1, доступный через раздел архивов Maven.
3. Установить Node.js ветки 22 с npm через [официальный установщик](https://nodejs.org/en/download) или используемый командой менеджер версий.
4. Добавить в `~/.zshrc` следующие строки, указав фактический каталог Maven:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 1.8)"
export PATH="$JAVA_HOME/bin:$HOME/tools/apache-maven-3.9.1/bin:$PATH"
```

Открыть новый Terminal и проверить:

```bash
git --version
java -version
javac -version
mvn -version
node --version
npm --version
```

`mvn -version` должен показывать Java 8. Не переключайте Maven на JDK 17/21
только потому, что такую версию предлагает IDE: текущий AspectJ не обновлён для
этой конфигурации. Для современных Android SDK установите **дополнительно JDK 17**:
его можно выбрать на той же странице Temurin, сменив Version на 17.
Используйте его в терминале Appium/Android tools, а Maven оставьте на JDK 8.
Требования SDK зависят от его версии; см. [UiAutomator2 4.2.9](https://github.com/appium/appium-uiautomator2-driver/blob/v4.2.9/README.md).

### 3.3. Appium и драйверы

На новом окружении установить сервер и только нужный драйвер:

```bash
npm install -g appium@2.19.0

# iOS, только на Mac:
appium driver install xcuitest@9.10.5

# Android:
appium driver install uiautomator2@4.2.9

appium --version
appium driver list --installed
```

Версии закреплены для воспроизведения текущего окружения проекта.
Команды без номера версии могут установить другой major серверов/драйверов.
Если драйвер уже установлен, сначала проверить его версию; повторная команда
`install` не служит обновлением. Не менять общее окружение других проектов вслепую.
Требования сервера описаны в [Appium 2.19](https://appium.io/docs/en/2.19/quickstart/requirements/).

Для test-model установить Python 3 и ffmpeg. Если на Mac уже есть Homebrew:

```bash
brew install python@3.13 ffmpeg
python3 --version
ffmpeg -version
```

Если `brew` отсутствует, сначала установить его по [официальной инструкции Homebrew](https://brew.sh/).
Проверить, что именно эти команды доступны в терминале запуска Maven.

## 4. Скачать проект и проверить компиляцию

```bash
git clone git@github.com:Deedles218/The-Coach.git
cd The-Coach
git branch --show-current
mvn -DskipTests test-compile
```

Для SSH нужны настроенный GitHub SSH key и доступ к репозиторию. Ветка должна
совпадать с согласованной в команде; переключить её при необходимости через
`git switch <branch>`. Первый Maven-запуск скачивает зависимости и требует сети.
Ожидаемый результат — `BUILD SUCCESS`; устройство и Appium для компиляции не нужны.

В IntelliJ открыть корневой `pom.xml` как Maven project, выбрать JDK 8 в Project SDK
и в Maven Runner JRE, затем выполнить Reload Maven Projects. Терминальный запуск
ниже является основным: он явно задаёт устройство, набор и окружение.

## 5. Настроить устройство

### 5.1. iOS Simulator

1. Установить полный Xcode, открыть его, принять лицензию и дождаться установки компонентов.
2. В Xcode Settings установить iOS Simulator runtime под требуемую версию iOS.
3. В Window → Devices and Simulators создать Simulator нужной модели и версии.
4. Выбрать Xcode для командной строки и проверить доступные устройства:

```bash
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
xcodebuild -version
xcrun simctl list devices available
appium driver doctor xcuitest
```

Если Xcode лежит в другом каталоге, заменить путь. Исправить обязательные
ошибки doctor; дополнительные возможности нужны только использующим их тестам.

Настроить **все четыре** переменные в терминале тестов:

```bash
export IOS_SIMULATOR_APP="$HOME/Downloads/The Coach Simulator.app"
export IOS_SIMULATOR_UDID="<UDID из simctl>"
export IOS_SIMULATOR_DEVICE_NAME="<имя выбранного Simulator>"
export IOS_SIMULATOR_PLATFORM_VERSION="<версия iOS выбранного Simulator>"

plutil -extract DTPlatformName raw "$IOS_SIMULATOR_APP/Info.plist"
plutil -extract CFBundleIdentifier raw "$IOS_SIMULATOR_APP/Info.plist"
```

Первая команда должна вывести `iphonesimulator`. Расширение `.app` само по себе
этого не гарантирует. `.ipa` для телефона нельзя превратить в Simulator-сборку
переименованием или распаковкой. Попросите разработчика сборку под архитектуру
вашего Mac и выбранный runtime.

Launcher `ci-scripts/run-ios-simulator.sh` сам загрузит Simulator, проверит
сборку и передаст её bundle ID в Maven. Он использует переменные
`IOS_SIMULATOR_*`; обычный `mvn test` их не читает. Для прямого Maven нужны
`IOS_APP`, `IOS_UDID`, `IOS_DEVICE_NAME`, `IOS_PLATFORM_VERSION` и `IOS_BUNDLE_ID`.
Наблюдённые прогоны test-model описаны в [готовом iOS-поднаборе](ready-ios-autotests.md).

### 5.2. Настоящий iPhone

1. Установить Xcode и XCUITest по предыдущим разделам.
2. Подключить iPhone по USB, разблокировать и подтвердить доверие компьютеру.
3. Включить Developer Mode на телефоне, если требуется; дождаться pairing в Xcode → Devices and Simulators.
4. Скопировать оттуда UDID, имя и точную версию iOS.
5. Добавить Apple Developer account в Xcode. Настроить Team/сертификат для WebDriverAgent по [инструкции XCUITest](https://appium.github.io/appium-xcuitest-driver/9.10/preparation/real-device-config/). Подпись WDA и подпись приложения — отдельные требования.
6. Получить IPA, разрешённый к установке на этот телефон, либо установить приложение через TestFlight.

Для установки IPA:

```bash
export IOS_APP="$HOME/Downloads/The Coach.ipa"
export IOS_UDID="<UDID телефона>"
export IOS_DEVICE_NAME="<имя телефона>"
export IOS_PLATFORM_VERSION="<версия iOS телефона>"
export IOS_XCODE_ORG_ID="<Apple Developer Team ID>"
export IOS_XCODE_SIGNING_ID="Apple Development"
```

Значение signing ID должно соответствовать сертификату команды.
`run-ios-ipa.sh` прочитает bundle ID из IPA. Если ранее установлен
`IOS_BUNDLE_ID`, он должен совпадать с IPA; иначе выполнить `unset IOS_BUNDLE_ID`.
App Store IPA может не устанавливаться через Appium: нужен подходящий подписанный
артефакт либо запуск уже установленной сборки по bundle ID.

### 5.3. Android Emulator или телефон

1. Установить [Android Studio](https://developer.android.com/studio).
2. В SDK Manager установить Android SDK Platform, Platform-Tools, Build-Tools,
   Command-line Tools и Android Emulator. Выбрать версию API под тестируемую сборку.
3. Для эмулятора создать AVD в Device Manager с подходящей архитектурой и запустить его.
   Для тестовых покупок нужен образ Google Play и аккаунт license tester.
4. Для телефона включить USB debugging, подключить USB и подтвердить RSA-запрос.
5. Настроить SDK в `~/.zshrc` и открыть новый терминал:

```bash
# Стандартный путь macOS; сверить с Android SDK Location в SDK Manager.
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

`ANDROID_HOME` указывает на SDK, а не Android Studio; см. [переменные Android SDK](https://developer.android.com/tools/variables).
Если в окружении уже есть `ANDROID_SDK_ROOT`, он не должен указывать на другой SDK.

В терминале для Android tools и Appium:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
export PATH="$JAVA_HOME/bin:$PATH"
sdkmanager --licenses
adb devices -l
appium driver doctor uiautomator2
```

Ожидается устройство со статусом `device`; при `unauthorized` подтвердить доступ
на телефоне. Исправить обязательные ошибки doctor до запуска сервера.
В отдельном терминале **тестов**, где Maven использует Java 8:

```bash
export ANDROID_UDID="<serial из adb devices, например emulator-5554>"
export ANDROID_DEVICE_NAME="<имя AVD или телефона>"
export ANDROID_PLATFORM_VERSION="$(adb -s "$ANDROID_UDID" shell getprop ro.build.version.release | tr -d '\r')"
export ANDROID_APP="$HOME/Downloads/The-Coach.apk"
export ANDROID_APP_PACKAGE="com.vamapps.thecoach"
export ANDROID_APP_ACTIVITY="com.vamapps.thecoach.MainActivity"
```

Последние два значения — defaults проекта. Для другой сборки сверить с командой
приложения. APK должен соответствовать выбранному устройству и его архитектуре.

## 6. Запустить Appium

В отдельном терминале из корня проекта (для Android — с SDK и JDK 17 из шага 5.3):

```bash
mkdir -p target/appium-logs
appium --address 127.0.0.1 --port 4723 --base-path / --log "$PWD/target/appium-logs/server.log"
```

Оставить терминал работающим. В терминале тестов проверить:

```bash
curl --fail http://127.0.0.1:4723/status
```

Ожидается JSON с `value.ready: true`. Проект использует путь `/`, а не `/wd/hub`.
Другой адрес задаётся через `APPIUM_URL` или `-Dappium.url=...`.
Остановить сервер после работы можно через Ctrl+C. Не запускать одновременно
Inspector и тесты на одном устройстве.

## 7. Первый тест без аккаунта

Выбрать **один** вариант. Тест открывает Login и проверяет, что Continue выключена
при пустом email. Он может выйти из ранее авторизованной сессии приложения.

**iOS Simulator**, после настройки `IOS_SIMULATOR_*`:

```bash
./ci-scripts/run-ios-simulator.sh \
  '-Dtest=tests.SmokeTests#testLoginWithEmptyEmailKeepsContinueDisabled' \
  -Dios.noReset=true -Dios.fullReset=false
```

**iPhone с IPA**, после настройки `IOS_*`:

```bash
./ci-scripts/run-ios-ipa.sh \
  '-Dtest=tests.SmokeTests#testLoginWithEmptyEmailKeepsContinueDisabled' \
  -Dios.noReset=true -Dios.fullReset=false
```

**iPhone с уже установленным приложением**: задать bundle ID установленной
сборки и убрать `IOS_APP`, чтобы Appium не пытался устанавливать IPA:

```bash
unset IOS_APP
export IOS_BUNDLE_ID="<bundle ID установленной сборки>"
mvn test -Dplatform=ios \
  '-Dtest=tests.SmokeTests#testLoginWithEmptyEmailKeepsContinueDisabled' \
  -Dios.noReset=true -Dios.fullReset=false
```

**Android**, после настройки `ANDROID_*`:

```bash
mvn test -Dplatform=android \
  '-Dtest=tests.AndroidSmokeTests#test01EmptyEmailKeepsContinueDisabled' \
  -Dandroid.noReset=true -Dandroid.fullReset=false
```

Ожидается `Tests run: 1`, `Failures: 0`, `Errors: 0`, `Skipped: 0` и `BUILD SUCCESS`.
Успешная компиляция или пропущенный тест не подтверждают работу UI-окружения.
Не начинайте с голого `mvn test`: в проекте есть разные наборы, старые учебные
тесты и сценарии с особыми требованиями к данным.

## 8. Настроить данные для авторизации и Smoke

Переменные должны быть экспортированы в **том же терминале**, где запускается
Maven. `.env` автоматически не читается. Maven-свойство `-D...` имеет приоритет
над соответствующей переменной окружения; секреты передавайте через окружение.

Создать собственный файл вне репозитория:

```bash
mkdir -p "$HOME/.config/the-coach"
touch "$HOME/.config/the-coach/test-env.sh"
chmod 600 "$HOME/.config/the-coach/test-env.sh"
```

Открыть его в редакторе и заполнить выданными командой значениями:

```bash
export COACH_EXISTING_PROGRESS_EMAIL='<email разрешённого тестового аккаунта>'
export COACH_EXISTING_PROGRESS_OTP='<актуальный или фиксированный тестовый OTP>'
export COACH_DAILY_PLAN_DAY='<фактический ожидаемый день, например 1>'
export COACH_FIXTURE_RESET_MODE='prebuilt'
export TEST_ISOLATION_MODE='logout'
```

`prebuilt` означает, что аккаунт **уже подготовлен**: у него доступны Today,
Daily Lessons, Daily Practice и нужная Kegel practice. Это значение ничего
не создаёт и не сбрасывает. OTP должен работать на выбранном backend; репозиторий
не получает автоматически новый почтовый код для каждого обычного Smoke-теста.
Не записывайте реальные email/OTP в Git, JSON capabilities или Maven-команды.

Загрузить данные и проверить заполнение:

```bash
source "$HOME/.config/the-coach/test-env.sh"
./scripts/validate_smoke_test_data.sh
./scripts/prepare_smoke_fixture.sh
```

В режиме `prebuilt` последняя команда не меняет backend. Проверка переменных
не подтверждает правильность OTP и реальное состояние аккаунта.
Проверить авторизацию на Simulator:

```bash
./ci-scripts/run-ios-simulator.sh \
  '-Dtest=tests.SmokeTests#testEmailAndOtpOpenAuthorizedDailyPlan' \
  -Dios.noReset=true -Dios.fullReset=false
```

Дополнительные данные зависят от набора:

| Набор | Что подготовить сверх базовых переменных |
| --- | --- |
| Авторизация с email без прогресса | `COACH_VALID_EMAIL_WITHOUT_PROGRESS`, отдельный от existing-progress email |
| Smoke Kegel | Может использовать existing-progress аккаунт; отдельные `COACH_KEGEL_PLAYER_EMAIL` и `COACH_KEGEL_PLAYER_OTP` задаются вместе |
| Полный `KegelExerciseTests` | Обязателен отдельный Kegel аккаунт, разрешённый для изменения состояния |
| P1 PDF paywall | Отдельный аккаунт без PDF entitlement и `COACH_NO_PDF_ENTITLEMENT_EMAIL`, `COACH_NO_PDF_ENTITLEMENT_OTP`, `COACH_NO_PDF_ENTITLEMENT_FIXTURE_ID` |
| Backend reset | `COACH_FIXTURE_RESET_MODE=backend_api`, fixture IDs, environment и исполняемый `COACH_FIXTURE_RESET_SCRIPT`, предоставленный владельцем стенда |

Полная таблица данных — [smoke-environment.md](smoke-environment.md).
Для P1 дополнительно выполнить `SMOKE_DATA_SCOPE=p1 ./scripts/validate_smoke_test_data.sh`.
Переустановка приложения не сбрасывает backend-прогресс и подписку аккаунта.

## 9. Запустить нужный набор

### 9.1. iOS Smoke

Полный `suites.SmokeSuite` включает clean-install onboarding. Для него нужны
сборка и актуальные локаторы шагов, которые надо получить у команды или проверить
через Inspector. Не копировать условные `id:<step1>` как рабочие значения.

```bash
export IOS_ONBOARDING_STEPS='id:<реальный ID первого шага>,id:<реальный ID следующего шага>'
./scripts/validate_smoke_test_data.sh
./ci-scripts/run-ios-simulator.sh \
  -Dtest=suites.SmokeSuite \
  -Dios.fullReset=true -Dios.noReset=false
```

Это запуск с удалением/переустановкой приложения. Если нужна проверка на уже
установленной сборке без clean install, выбирайте отдельный метод из разделов 7–8.

### 9.2. Android Smoke

После подготовки existing-progress аккаунта и `ANDROID_*`:

```bash
mvn test -Dplatform=android -Dtest=tests.AndroidSmokeTests \
  -Dandroid.noReset=true -Dandroid.fullReset=false
```

Android Explore запускается заменой `-Dtest` на `tests.AndroidExploreTests`;
его ожидания должны соответствовать версии/конфигурации Explore в сборке.

### 9.3. Другие наборы

| `-Dtest` | Платформа | Предусловия |
| --- | --- | --- |
| `suites.ReleaseSmokeSuite` | iOS | Старая сборка в `ios.app`, новая в `ios.update.app`; одинаковый bundle ID; `noReset=true`, `fullReset=false` |
| `suites.PushPermissionSuite` | iOS | Clean install, onboarding locators и нужное исходное состояние разрешений ОС |
| `suites.StoreKitPurchaseSuite` | iOS | Sandbox и `storekit.sandbox=true`, `storekit.allowPurchases=true` |
| `tests.OnboardingPaywallTests` | iOS / Android | Тестовый магазин и явный `purchase.allow=true`; iOS: `storekit.sandbox=true`, Android: `googleplay.licenseTester=true` |
| `suites.TestModelAutomationSuite` | iOS Simulator для полного покрытия | Все case-specific аккаунты/UID, Python, ffmpeg, preprod fixture-доступ; см. следующий раздел |

Покупки и Restore настраиваются отдельно по [onboarding-paywall-e2e.md](onboarding-paywall-e2e.md).
Флаги покупки выставляются только после фактической настройки Sandbox/license tester.
Не использовать production-аккаунт для таких запусков.

## 10. Jira/test-model через macOS Keychain

Для начала достаточно Smoke. Этот раздел нужен для кейсов COA-* и скрипта
`scripts/run_test_model_local.py`. Он запускает явно выбранные кейсы последовательно.

1. Настроить Simulator, Appium, Python и ffmpeg по предыдущим шагам.
2. Получить у владельца тестовых данных базовый email и тестовый OTP.
   Аккаунт должен соответствовать preprod-фикстуре; для provisioning нужны
   разрешённые email aliases и работающий тестовый механизм OTP.
3. Открыть macOS Keychain Access («Связка ключей») и в login keychain создать
   две записи типа Password / generic password:

| Keychain Item Name / service | Account | Password |
| --- | --- | --- |
| `the-coach-test-model-email` | Вывод `whoami` | Базовый тестовый email |
| `the-coach-test-model-otp` | Вывод `whoami` | Тестовый OTP |

При запросе системы разрешить runner доступ к этим записям. Email/OTP не нужно
вставлять в командную строку `security ... -w`, где они попадут в историю.
`the-coach-test-model-accounts` — JSON-реестр: runner создаёт/обновляет его сам.
На новом компьютере `--status` может вывести `{}`; это не ошибка установки.

```bash
python3 scripts/run_test_model_local.py --status

# Первый кейс; --app обязателен для переносимого примера:
python3 scripts/run_test_model_local.py COA-7947 \
  --app "$IOS_SIMULATOR_APP" --maven=-q

# Проверки без смены программы, с подготовленным returning-user аккаунтом:
python3 scripts/run_test_model_local.py COA-8235 COA-8228 COA-8230 COA-8227 \
  --app "$IOS_SIMULATOR_APP" --maven=-q
```

Runner по умолчанию содержит путь с компьютера автора, поэтому всегда передавайте
`--app`. Он использует заданные ранее `IOS_SIMULATOR_UDID`,
`IOS_SIMULATOR_DEVICE_NAME` и `IOS_SIMULATOR_PLATFORM_VERSION`.

Для восстановления упражнения и смены программы нужны отдельные аккаунты.
Следующая команда **создаёт/подготавливает** их на тестовом стенде; применять её
к выданной для этого базе email. Подтверждённые записи runner сохраняет:

```bash
python3 scripts/run_test_model_local.py COA-8517 COA-8232 COA-8231 \
  --provision --app "$IOS_SIMULATOR_APP" --maven=-q

python3 scripts/run_test_model_local.py COA-8517 COA-8232 COA-8231 \
  --app "$IOS_SIMULATOR_APP" --maven=-q
```

COA-8511/8512 требуют отдельной согласованной Premium-фикстуры в Keychain
`the-coach-test-model-premium`, с правильными UID, scope и `kegelMutationScope`.
Получить подготовленную запись и разрешённый объём изменений у владельца данных;
не создавать произвольный JSON, чтобы обойти проверки. Настройка описана в
[отчёте Kegel](test-model-kegel-2026-09-07.md).

Для прямого запуска полного `suites.TestModelAutomationSuite` нужен отдельный
набор переменных из `scripts/validate_test_model_data.sh` (в том числе
`COACH_COA8231_*`, `COACH_COA8232_*`, `COACH_COA8511_*`, `COACH_COA8512_*`,
`COACH_COA8517_*` с EMAIL/OTP/UID). Секреты Keychain не экспортируются в родительский
терминал автоматически, поэтому внешний validator после runner может сообщить
об отсутствующих переменных. Для обычного explicit-case запуска он не нужен.

Не запускать несколько кейсов одновременно на одном Simulator/общей фикстуре.
COA-7949/7950 сохранены с известными расхождениями ранее проверенной сборки;
COA-8518 отложен и runner его отклоняет. Полный набор не гарантированно зелёный.
Исторические результаты и ограничения — [ready-ios-autotests.md](ready-ios-autotests.md).

## 11. Посмотреть отчёт

После запуска:

```bash
mvn allure:serve
```

Maven скачает настроенную в `pom.xml` Allure CLI (reportVersion 2.39.0) и откроет
отчёт; отдельная установка команды `allure` не требуется. Остановить сервер — Ctrl+C.
Для генерации статического отчёта выполнить `mvn allure:report`;
каталог готового отчёта будет указан в выводе Maven.

| Артефакт | Где искать |
| --- | --- |
| Итог JUnit, stack trace | `target/surefire-reports/` |
| Данные Allure | `target/allure-results/` |
| Скриншоты падений | `target/screenshots/` |
| XML дерева экрана | `target/page-source/` |
| Диагностика драйвера | `target/appium-logs/` |
| Полный лог сервера, если запущен как выше | `target/appium-logs/server.log` |
| Test-model evidence/fixture metadata | `target/test-model-evidence/`, `target/test-model-fixtures/` |
| Записи селектора | `target/*-motion-*/` |

Некоторые артефакты появляются только при падении и только после создания сессии.
Скриншоты экранов с учётными данными намеренно могут отсутствовать.
Отчёты предыдущих прогонов могут накапливаться. Для чистого нового прогона сначала
сохранить нужные артефакты, остановить Appium, выполнить `mvn clean`, затем заново
создать каталог логов и запустить сервер. `clean` удаляет весь `target/`.
Не коммитить отчёты/сборки/секреты; перед передачей логов проверить их содержимое.

## 12. Если запуск не работает

| Симптом | Что проверить |
| --- | --- |
| `command not found` | Установку инструмента и PATH в текущем терминале; после изменения `~/.zshrc` открыть новый Terminal |
| Maven использует другую Java / ошибка AspectJ | `mvn -version`, `JAVA_HOME`, JRE в Maven Runner; для проекта использовать JDK 8 |
| Ошибка Android SDK tools о версии Java | В терминале Appium/SDK выбрать JDK 17 или требуемую SDK версию; Maven оставить на JDK 8 |
| `Connection refused` | Appium работает, порт совпадает, `/status` отвечает |
| HTTP 404 при создании сессии | Совпадение `--base-path /` и `APPIUM_URL`; не добавлять `/wd/hub` к текущей настройке |
| Драйвер не найден | `appium driver list --installed`; нужный драйвер установлен тем же пользователем и в том же Appium окружении |
| Выбирается чужой iPhone / UDID недоступен | Переопределены все параметры устройства; Simulator запускается через его launcher |
| `.app was not found` / `DTPlatformName=iphoneos` | Абсолютный путь в кавычках и настоящая `iphonesimulator` сборка |
| WDA / `xcodebuild` exit 65 | Xcode, совместимость с iOS, pairing/Developer Mode, подпись и Team WDA; смотреть server.log |
| IPA не устанавливается | Provisioning profile и разрешение установки на телефон; попробовать выданную подходящую сборку или установленное TestFlight-приложение |
| Android `unauthorized` / `offline` | Разблокировка телефона, подтверждение USB debugging, состояние AVD и serial |
| Android activity не запускается | Package/activity текущей сборки и `ANDROID_PLATFORM_VERSION`; default проекта Android 11 может не соответствовать вашему устройству |
| `Missing ... variables` | Файл загружен через `source` в терминале Maven, переменные экспортированы, выбран правильный набор |
| OTP не подходит | Аккаунт и backend совпадают, код действителен, у стенда есть согласованный механизм тестового OTP |
| `Keychain item unavailable` | Точные service names, Account равен `whoami`, login keychain разблокирован и доступ разрешён |
| Motion/cache evidence не получено | Это локальный Simulator, передан UDID через launcher, доступны Python/ffmpeg/xcrun, сборка поддерживает нужный cache/analytics |
| `BUILD SUCCESS`, но `Skipped > 0` | Выбран ли класс для нужной платформы; проверить Surefire/Allure, а не только exit code |
| Падает full Smoke на clean-install проверке | Переданы `fullReset=true`, `noReset=false`, сборка и реальные onboarding steps |
| UI отличается от ожидаемого | Сверить язык (локаторы часто английские), версию сборки, feature flags, аккаунт и программу |

Appium Inspector подключается к `127.0.0.1:4723`, path `/`. Файлы в `appium/`
— примеры с локальными путями автора: в собственной копии заменить app, UDID,
deviceName, platformVersion и bundleId. Java-тесты эти JSON не читают; их
настройки находятся в `Platform.java` и переменных окружения.

Окружение для выбранного набора готово, когда компиляция проходит, `/status`
отвечает, первый UI-тест выполнен без skips, а необходимые аккаунты и состояние
данных подтверждены. При передаче ошибки приложить команду без секретов,
версии инструментов, сборку/устройство, Surefire и лог Appium.
