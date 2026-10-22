# Полный iOS-прогон

`scripts/ios_regression/run_regression.py` сохраняет последовательный запуск
мужской и женской сборок, отдельные результаты каждого задания, возобновление
очереди и сводку HTML/JSON. Это переносимая версия runner, использованного в
прогоне 21–22 октября 2026 года. Appium и выбранный Simulator запускаются заранее;
runner не создаёт фоновых расписаний и не управляет чужими Appium-процессами.

## Подготовка

Нужны Python 3, Maven, Java версии, совместимой с проектом, Appium/XCUITest,
Allure CLI и две Simulator `.app` сборки каждого варианта для update-проверки.
Для update нужны одинаковый bundle ID и настоящие различные числовые build
numbers; `BITRISE_BUILD_NUMBER` не подтверждает обновление. Правила тестов
TestFlight/StoreKit сохраняются: Simulator `.app` не заменяет TestFlight и
настроенный sandbox. Неподходящие входные данные отражаются как ошибки.

Скопируйте [пример конфига](ios-regression.example.json) в локальный `target/`,
замените пути, UDID, версии и названия записей Keychain. `runId` должен быть
уникальным для нового запуска и тем же для `--resume`. `existingAccountService`
содержит JSON с `email`, `otp` и необязательным `variant`. Используются
существующие `scripts/run_test_model_local.py` и `TestData`; значения аккаунтов
передаются дочернему Maven через environment. Пароли и OTP не задаются в
`properties`/`suiteProperties` и не коммитятся.

`registryPath` — приватный JSON-массив аккаунтов с полями `variant`, `role`,
`email`, `status="verified"` и `accessConfigured`. Один адрес на роль и вариант;
общий OTP читается из `otpService`. Для полного набора нужны роли
`premium_no_pdf`, `fresh_program_tooltip`, `kegel`, `video`, `masterclass_purchased`,
а также `basic_partner_buyer` для male и `masterclass_unpaid_isolated` для female.
Подтверждённая регистрация не подтверждает entitlement: доступ и состояние
каждой роли готовятся отдельно. Runner не покупает и не выдаёт подписки.

Auth/Profile используют `existingAccountService` с отключённой автоматической
анкетой после OTP. Kegel использует этот же выбранный аккаунт. Остальные
сценарии допускают подготовку анкеты новых аккаунтов через `postAuthGoal`.
Наблюдённые карточки/дни в примере нужно сверить с конкретной фикстурой по
[контракту Daily Plan](ios-daily-plan-card-fixtures.md).

`properties` задаёт общие JVM-параметры варианта, `suiteProperties` — параметры
конкретного job label. Например, старый Quiz/Profile-вход включается только
для нужного набора через `coach.ios.quizProfileLogin=true` и соответствующую
`coach.kegelUi.fixture`; прямой вход Auth/Profile не требует этого обхода.
Не переопределяйте параметры чистой установки общими свойствами.

## Запуск и возобновление

```bash
python3 scripts/ios_regression/run_regression.py --config target/ios-local.json --list
python3 scripts/ios_regression/run_regression.py --config target/ios-local.json \
  --output target/ios-full-local
python3 scripts/ios_regression/run_regression.py --config target/ios-local.json \
  --output target/ios-full-local --resume
```

Для точечного запуска используйте повторяемый `--only <job label>` и отдельный
output, например `--only SmokeTests --only SmokeCleanInstall`. Для одного
варианта оставьте в `apps` только его. Полная матрица двух вариантов содержит
70 заданий, включая два новых Bookmarks-задания. Семь Smoke-методов сохранены:
шесть выполняются с `noReset=true/fullReset=false`, чистая установка — отдельно
с `fullReset=true/noReset=false`. Test discovery проверяет, что методы не потеряны.

`--resume` продолжает незавершённые задания, включая случаи, когда отдельный
запуск Maven завершился с ошибкой до создания testcase. Уже завершённые задания
не повторяются. Для повторного запуска упавшего метода используйте отдельный
каталог Allure, например:

```bash
mvn test -Dplatform=ios \
  '-Dtest=tests.SmokeTests#testLogoutReturnsToWelcome' \
  -Dios.noReset=true -Dios.fullReset=false \
  -Dallure.results.directory=target/retry-logout/allure-results
```

Добавьте обычные параметры своего устройства и приложения. Первоначальные
артефакты остаются в каталоге полного прогона; повторный запуск не превращает
старый результат в успешный и не смешивается с ним автоматически.

## Bookmarks и результаты

Все 21 Bookmarks-сценарий и требования к восьми ролям описаны в
[bookmarks-automation.md](bookmarks-automation.md). Для двух вариантов можно
связать `COACH_BOOKMARKS_*` с отдельными секретными переменными через
`apps.<variant>.environmentBindings`, например
`"COACH_BOOKMARKS_BASELINE_EMAIL": "COACH_MALE_BOOKMARKS_BASELINE_EMAIL"` и
аналогично для OTP, остальных ролей и content-параметров. Нельзя подставлять
один аккаунт вместо ролей с разным состоянием. Suite включает изменение
закладок, завершение контента и update/reinstall: используйте выделенные
сбросимые фикстуры. `coach.bookmarks.useCurrentAccount=true` подходит только
для одного совместимого метода, не полного набора.

`status.json` хранит активное задание и exit codes; `summary.json` и `report.html`
показывают отдельно UI и offline-проверки. Allure собирается из изолированных
каталогов с вариантом/версией в history ID. Предыдущая агрегация архивируется.
Exit code runner ненулевой, если хотя бы одно задание упало. Фоновый supervisor
должен завершаться при любом exit code, без KeepAlive/restart-on-failure:
тестовые падения не требуют нового автоматического прогона.

Проверка без устройства:

```bash
python3 -m unittest discover -s scripts -p test_ios_regression.py
mvn test '-Dtest=tests.*UnitTests'
```
