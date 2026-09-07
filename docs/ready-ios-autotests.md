# Готовые автотесты iOS

В опубликованный набор входят пять ТК селектора Today и три готовых ТК из
предыдущего этапа. Все восемь имеют успешные UI-прогоны. Незавершённые
COA-7949/7950/8511/8512/8518 и отдельная работа над onboarding paywall в этот набор
не включены.

| ТК | Проверка | Подтверждённый результат |
| --- | --- | --- |
| COA-7947 | Корректный email активирует Continue | PASS, повтор в ветке публикации |
| COA-8235 | Полный список программ Today совпадает с Explore | PASS, повтор 7 сентября 2026 |
| COA-8517 | Восстановление удалённого упражнения из Settings | PASS: UI, все 69 предусмотренных дней и возврат фикстуры на день 2 |
| COA-8228 | Закрытие селектора свайпом вниз с промежуточными кадрами анимации | PASS, повтор |
| COA-8230 | Вертикальный скролл содержимого селектора | PASS, повтор |
| COA-8227 | Открытие, состав программ и свежее ProgramModalOpen | PASS, повтор после расширения аналитики |
| COA-8232 | Выбор программы, Today, лоадер и ProgramModalSelect с program_id | PASS, повторные прогоны и контроль в ветке публикации |
| COA-8231 | Активная программа первая и выделена до/после смены | PASS, два прогона |

COA-8229 проверяется внутри COA-8227: тап вне модалки, её исчезновение и анимация
вниз. COA-8253 не воспроизвёлся при переключении Last Longer → Keep It Hard на
проверенной сборке; статус дефекта в Jira не изменялся.

Окружение подтверждённых прогонов: iPhone 17 Pro, iOS 26.5 Simulator,
The Coach 1.13.29, bundle `com.vamapps.preprod.The-Coach`, локальный Appium.
Android этим набором не покрыт.

## Запуск и данные

Нужны Java/Maven и зависимости проекта, Python 3, Xcode Simulator tools,
`ffmpeg`, Appium с XCUITest и сборка для `iphonesimulator`.
Идентификаторы Simulator задаются существующими `IOS_SIMULATOR_UDID`,
`IOS_SIMULATOR_DEVICE_NAME`, `IOS_SIMULATOR_PLATFORM_VERSION`.

Runner `scripts/run_test_model_local.py` использует текущего пользователя macOS
и Keychain service names `the-coach-test-model-email`, `the-coach-test-model-otp`
и `the-coach-test-model-accounts`. Значения выдаются через принятый в команде
канал секретов. Код не содержит email, OTP или токены реальных аккаунтов.

```bash
# Явный setup отдельных аккаунтов; существующие подтверждённые записи сохраняются.
python3 scripts/run_test_model_local.py COA-8517 COA-8232 COA-8231 --provision --app "/path/to/The Coach.app" --maven=-q
# Последовательный запуск только явно выбранных кейсов.
python3 scripts/run_test_model_local.py COA-7947 COA-8235 COA-8517 COA-8228 COA-8230 COA-8227 COA-8232 COA-8231 --app "/path/to/The Coach.app" --maven=-q
```

Для прямого запуска suite заполните data contract из
`scripts/validate_test_model_data.sh`, проверьте его и используйте существующий
`ci-scripts/run-ios-simulator.sh -Dtest=suites.TestModelAutomationSuite`.
Набор содержит также отдельную проверку восстановления после намеренной ошибки.
Параллельный запуск на одном Simulator не поддерживается.

COA-8235 и сценарии без смены программы используют общий returning-user аккаунт.
COA-8232/8231 имеют собственные аккаунты с проверкой UID и исходным прогрессом 0%.
После переключения в `finally` восстанавливаются Last Longer, исходный этап и
прогресс. Это отдельно подтверждено успешным UI-тестом с намеренной ошибкой
после выбора Keep It Hard.

COA-8517 имеет отдельный alias/UID. Fixture bridge разрешает только подготовку,
проверку и reset восстановления. Перед изменениями через существующие формы
preprod проверяется соответствие UID/email. Проверка 69 дней учитывает, что
чтение daily endpoint меняет выбранный день, и возвращает фикстуру на день 2.
Подписки и глобальные feature flags не меняются; покупки не выполняются.

## Доказательства и ограничения

Allure, screenshots, XML, видео и минимальные JSON evidence создаются под
`target/` и не публикуются в этом коммите. Они могут содержать состояние локальных
фикстур. Секреты в диагностике маскируются; credential screens не прикладываются
как screenshot. Для воспроизведения используются собственные разрешённые
тестовые данные.

Аналитика проверяет свежий UUID SDK, UID, время и точный program_id для Select.
Это наблюдение локальной очереди SDK, а не подтверждение доставки в Amplitude.
Контент Today читается из свежего ответа, полученного приложением при текущем
действии: проверяются UID, expiry JWT, CFURL timestamp, HTTP Date и program_id.
Контент reader не отправляет HTTP-запросы и не меняет cache.

Skeleton проверяется по наблюдённым областям заголовка и модуля Today,
с последовательностью content → skeleton → content. Геометрия рассчитана на
проверенную portrait iOS-разметку; неизвестная разметка или отсутствие кадров
приводят к FAIL. Численного требования к длительности анимации/загрузки в ТК нет.

## Проверки вспомогательного кода

```bash
mvn -q -Dtest=tests.SelectorMotionUnitTests,tests.TestModelLifecycleUnitTests,tests.RestoringTestActionUnitTests,tests.ProgramLoadingEvidenceUnitTests test
python3 -m unittest discover -s scripts -p 'test_*.py' -v
bash -n scripts/validate_test_model_data.sh
```

Подтверждённый локальный результат: 33 Java и 58 Python проверок — PASS.
После изоляции готового набора в отдельную ветку эти проверки повторно прошли;
дополнительно выполнены успешные UI-прогоны COA-7947 и COA-8232 из этой ветки.
Инфраструктурные проверки запрещают stale/чужую аналитику и cache, ложный PASS
на статичных кадрах, потерю исходной ошибки при cleanup и оставшийся дочерний
процесс рекордера при timeout.

Подробности селектора: [описание реализации](today-program-selector-automation.md).
