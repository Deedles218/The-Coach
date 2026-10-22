# iOS: управляемые regression-прогоны

ci-scripts/run-ios-suite.sh выбирает существующие JUnit suites, проверяет
конфигурацию до Appium-сессий и сохраняет каждый профиль отдельно. Профиль full
означает оркестрацию выбранных профилей; без --profiles выбран только regression.
Это не discovery всех Java-классов и не заявление о проверке всего продукта.

Для управляемого запуска одного профиля используйте `run-ios-suite.sh profiles
<имя>`. Существующие команды `run-ios-suite.sh <suite> [Maven options]`
сохраняют прежнюю передачу аргументов и проверки данных. Новые reset policies,
UI gates и раздельные results включаются через `profiles`, `regression`
или `full`.

## 1. Подготовка терминалов

В каждом новом терминале сначала перейдите в проект. Переменные, экспортированные
в другом окне Terminal, не переносятся автоматически.

    cd /path/to/The-Coach
    export JAVA_HOME="$(/usr/libexec/java_home -v 1.8)"
    export PATH="$JAVA_HOME/bin:$PATH"

В отдельном окне запустите Appium и оставьте его работающим:

    cd /path/to/The-Coach
    mkdir -p target/appium-logs
    appium --address 127.0.0.1 --port 4726 --base-path / \
      --log "$PWD/target/appium-logs/ios-full.log"

В терминале тестов:

    export IOS_SIMULATOR_APP="/path/to/The Coach.app"
    export IOS_SIMULATOR_UDID="A6D99EE1-3D10-4360-9D3E-147F49856735"
    export IOS_SIMULATOR_DEVICE_NAME="Coach Critical Smoke"
    export IOS_SIMULATOR_PLATFORM_VERSION="26.5"
    export IOS_APP_VARIANT="male"
    export TEST_ISOLATION_MODE="logout"
    export COACH_FIXTURE_RESET_MODE="prebuilt"
    export COACH_DAILY_PLAN_DAY="Stage 1"

Stage 1 — ожидаемое состояние подготовленной fixture, а не команда сброса.
Если аккаунт находится на другом дне/этапе, UI-preflight остановит regression.
Нужно подготовить fixture согласно [контракту данных](smoke-environment.md),
включая видимые Lesson и Practice. Не заменяйте ожидаемый день фактическим
только ради зелёного результата. Переустановка приложения не сбрасывает
прогресс серверного аккаунта.

Введите реквизиты в этом же терминале, если они ещё не экспортированы:

    read -r "COACH_MALE_EXISTING_PROGRESS_EMAIL?Male email: "
    read -r -s "COACH_MALE_EXISTING_PROGRESS_OTP?Male OTP: "
    echo
    export COACH_MALE_EXISTING_PROGRESS_EMAIL
    export COACH_MALE_EXISTING_PROGRESS_OTP

Launcher выбирает male/female role по IOS_APP_VARIANT и проверяет вариант по
bundle ID. Если variant-specific переменные не заданы, можно использовать
COACH_EXISTING_PROGRESS_EMAIL / COACH_EXISTING_PROGRESS_OTP.
COACH_ACCOUNT_VARIANT, если задан, должен совпадать. OTP/email не записываются
в manifest; вывод дочернего процесса очищается перед сохранением в лог.

Для `regression` и `daily-plan` дополнительно нужны отдельные
`COACH_KEGEL_PLAYER_EMAIL` / `COACH_KEGEL_PLAYER_OTP` и подготовленная обязательная
практика для `DailyPlanTests`. Это другой аккаунт, отличный от existing-progress.
Параметры программы, дня и карточки берутся из согласованной
[Daily Plan fixture](ios-daily-plan-card-fixtures.md), а не подбираются для обхода
падения. Пример для ранее подготовленной male fixture:

    regression_options=(
      "-Dcoach.kegelPlayer.program=Kegel Challenge"
      "-Dcoach.dailyPlan.corePracticeProgram=Kegel Challenge"
      "-Dcoach.dailyPlan.corePracticeDay=Stage 6 of 9"
      "-Dcoach.dailyPlan.corePracticeTitle=Morning Kegel Workout"
    )

Для другой fixture замените значения заранее. `ios-parity-smoke` требует
`coach.kegelPlayer.program`, но может использовать existing-progress по контракту
Smoke. При отсутствии обязательных параметров launcher завершится до обращения
к устройству. `--plan` показывает имена этих параметров без доступа к секретам.

## 2. План, проверка и запуск

Посмотреть состав без доступа к аккаунтам и устройствам:

    ./ci-scripts/run-ios-suite.sh full --plan
    ./ci-scripts/run-ios-suite.sh profiles --help

Проверить наличие данных, сборки, Simulator и готовность Appium без создания
сессии:

    ./ci-scripts/run-ios-suite.sh full --check \
      "${regression_options[@]}" \
      -Dappium.url=http://127.0.0.1:4726/

Запустить regression и собрать общий отчёт:

    export ALLURE_BIN="/path/to/allure/bin/allure"
    export RUN_ROOT="$PWD/target/ios-profiles-$(date +%Y%m%d-%H%M%S)"
    ./ci-scripts/run-ios-suite.sh full --profiles regression \
      "${regression_options[@]}" \
      --results-root "$RUN_ROOT" --report \
      -Dappium.url=http://127.0.0.1:4726/

Regression включает IOSSmoke, DailyPlan, CoachProfile и ConfiguredExplore.
Перед ним запускается один отдельный UI-preflight: авторизация, ожидаемые
Day/Stage, Lesson/Practice и Explore. При провале зависимые тесты не запускаются;
manifest получает blocked-by-precondition, процесс возвращает ненулевой код.
Если задан COACH_EXPECTED_ACCOUNT_UID, preflight сверяет UID через существующий
read-only Simulator helper. COACH_DAILY_PLAN_PROGRAM задаёт дополнительное
ожидание выбранной программы.

Для проверки только Explore, без требований к Lesson/Practice:

    ./ci-scripts/run-ios-suite.sh profiles explore \
      --results-root "$PWD/target/ios-explore-$(date +%Y%m%d-%H%M%S)" \
      -Dappium.url=http://127.0.0.1:4726/

У Explore свой UI-preflight. Проверка одной функции доступна через
run-ios-simulator.sh -Dtest=tests.ClassName#methodName.

У For Her отдельный конфиг Explore по [COA-7069](https://the-coach.atlassian.net/browse/COA-7069).
В наблюдённой iOS 2.0.33 конфигурации `abtest_explore_config_v7_for_her`
поле `programsTitle` пустое: UI-preflight требует карусель с реальными
программными карточками, изображениями и непустыми названиями, а не мужской
`Browse Programs`. Мужской контракт заголовка сохранён. Успешный catalog gate
не подтверждает остальные сценарии `ConfiguredExploreTests`: их ожидания
секций и переходов нужно сверять отдельно с конфигом выбранного варианта.
Broad discovery tests.*Tests,tests.HW.* отклоняется до установки или старта
Simulator.

## 3. Изолированные профили

| Профиль | Основные предусловия |
| --- | --- |
| regression | Existing-progress + smoke fixture; отдельный Kegel account, coach.kegelPlayer.program и три corePractice-параметра; UI gate |
| daily-plan | Existing-progress + smoke fixture; отдельный Kegel account и три corePractice-параметра; UI gate |
| ios-parity-smoke | Existing-progress + smoke fixture; coach.kegelPlayer.program; UI gate |
| explore, profile | Existing-progress; Explore имеет собственный UI gate |
| authorization | Existing-progress + COACH_VALID_EMAIL_WITHOUT_PROGRESS |
| smoke | Smoke fixture, clean install и IOS_ONBOARDING_STEPS |
| push-permission | Clean install, IOS_ONBOARDING_STEPS, подготовленное permission state |
| release-smoke | Старая .app, IOS_UPDATE_APP новой сборки, одинаковый bundle ID, разные build numbers, approved UID и активная подписка |
| kegel | Smoke fixture и выделенные COACH_KEGEL_PLAYER_EMAIL / COACH_KEGEL_PLAYER_OTP |
| kegel-october | Case-specific COA-8094/8096/8170/8171 accounts, coach.kegelUi.fixture, маркетинговый link/account COA-7937 |
| modules | Preprod build, COA-9044 account/UID, -Dcoach.modules.programId=<prepared program_id>; launcher включает coach.modules.enabled=true |
| test-model | Все переменные validate_test_model_data.sh; подготовленные и разрешённые case-specific fixtures |
| localization | Установленное приложение, localization.fixture, вариант и языки; dashboard/switch также требует existing-progress |
| male-start-screen | Мужская сборка и existing-progress |
| onboarding-goal, onboarding-slides | Свежая вручную подготовленная установленная сборка, IOS_FRESH_START_PREPARED=true; slides также требует onboarding.slides.fixture |
| onboarding-paywall | Свежая установленная TestFlight-сборка, Sandbox, явные storekit.sandbox=true и purchase.allow=true |
| storekit-purchase | Установленная сборка, Sandbox, явные storekit.sandbox=true, storekit.allowPurchases=true, onboarding steps |
| video-player | Выделенный COA-9551 account, coach.video.lessonTitle и coach.video.secondLessonTitle |
| program-switch | Чистая локальная Simulator-сборка и coach.program.target |

Reset policy задаётся профилем. Противоречащие -Dios.noReset / -Dios.fullReset
отклоняются. Для clean-install выделяйте отдельный Simulator: эти профили
удаляют/переустанавливают тестовое приложение.

Installed-профили не передают ios.app: используйте IOS_BUNDLE_ID, IOS_UDID
и при необходимости -Dios.deviceName / -Dios.platformVersion. Не задавайте
IOS_APP или -Dios.app для TestFlight-профилей. IOS_SIMULATOR_APP можно оставить
для определения bundle/version у Simulator; он не становится app capability
installed-профиля. Для физического устройства без локальной сборки версия
в отчёте останется unknown.

Пример отдельной localization-проверки:

    ./ci-scripts/run-ios-suite.sh profiles localization --check \
      -Dappium.url=http://127.0.0.1:4726/ \
      -Dlocalization.variant=male \
      -Dlocalization.locales=en \
      -Dlocalization.scenarios=welcome \
      -Dlocalization.fixture=src/test/resources/localization/ios-male-prod-1.13.33.properties

--check проверяет конфигурацию и доступность файла; при запуске содержимое
frozen fixture один раз проверяет отдельный offline gate через существующий Java
loader, до запуска locale matrix и открытия localization-сессий.
Resource baseline остаётся provisional; наличие файла не подтверждает
лингвистическое качество.

Для нескольких уже подготовленных профилей укажите их явно:

    ./ci-scripts/run-ios-suite.sh full --profiles regression,authorization,kegel \
      "${regression_options[@]}" \
      --results-root "$PWD/target/ios-expanded-$(date +%Y%m%d-%H%M%S)" \
      --report -Dappium.url=http://127.0.0.1:4726/

Все конфигурации проверяются до первого UI gate. Не выбранные профили отражены
как not-requested, а не passed. Дублирующие профили regression и
ios-parity-smoke / profile / daily-plan / explore запускать вместе обычно
не требуется. Legacy tests.HW сохранены и запускаются явно для отдельного
разбора; в актуальный regression они не включены.

## 4. Артефакты и Allure

    RUN_ROOT/
      run-summary.json
      regression/
        preflight.log
        preflight/allure-results/
        tests.log
        tests/allure-results/
      allure-report/

В Allure записываются профиль, Git SHA, признак локальных изменений, версия/build
приложения, variant и reset policy. failureType и failureConfidence показывают диагностическое направление:
таймаут или несовпадение assertion требуют расследования и не объявляются
подтверждённым продуктовым дефектом автоматически.

Файл categories.json создаётся автоматически; существующий пользовательский файл
категорий сохраняется. UI-preflight failure остаётся красным тестом и не может
превратить неполный regression в зелёный release gate.

Для сохранения истории при --report добавьте
--history-from "/absolute/path/to/previous/allure-report". Launcher копирует
только history в новый run; предыдущий отчёт сохраняется. Сравнивайте прогоны
с одинаковым составом профилей, чтобы изменение scope не искажало trend.

Открывайте отчёт по HTTP, сохранив сервер работающим:

    python3 -m http.server 5051 --bind 127.0.0.1 \
      --directory "$RUN_ROOT/allure-report"

Откройте http://127.0.0.1:5051/ в браузере. Открытие index.html через file://
может дать Allure 404 [object Object].

Коды возврата: 0 — выбранные профили прошли; 1 — UI gate или тесты упали;
2 — неверная конфигурация/недоступное окружение или невозможность сгенерировать
запрошенный отчёт; 130 — прерывание пользователем.
Непустой results-root повторно не используется: старые прогоны сохраняются.
