# Today selector — отчёт 7 сентября 2026

Первый этап завершён: COA-8228, COA-8230, COA-8227 реализованы для iOS и имеют успешные повторные прогоны. COA-8229 усилен внутри COA-8227. Существующий COA-8235 повторно прошёл.

Окружение: iPhone 17 Pro, iOS 26.5 Simulator; `/Users/deedles/Downloads/The Coach 3.app`, `com.vamapps.preprod.The-Coach`, версия 1.13.29 (`CFBundleVersion=BITRISE_BUILD_NUMBER`). Appium — локальный сервер, фикстура — returning-user COA-8235 из Keychain.

## Последние результаты

| ТК | Результат | Время сценария по Allure | Артефакт |
| --- | --- | --- | --- |
| COA-8228 | PASS | 63.108 с | [Allure JSON](/Users/deedles/IdeaProjects/The-Coach/target/allure-results/a36acd6d-865c-4ce2-95e5-5e73015c40ca-result.json) |
| COA-8230 | PASS | 63.452 с | [Allure JSON](/Users/deedles/IdeaProjects/The-Coach/target/allure-results/5ae209b9-12a4-48f9-bc06-ed59b8e3e905-result.json) |
| COA-8227 | PASS | 111.191 с | [Allure JSON](/Users/deedles/IdeaProjects/The-Coach/target/allure-results/d0c9645c-5d41-4fc3-8f54-70c0f6bd78bc-result.json) |
| COA-8235 | PASS | 97.873 с | [Allure JSON](/Users/deedles/IdeaProjects/The-Coach/target/allure-results/bdda67d8-4e3d-4a35-9ff1-f9231c2fa5c7-result.json) |

COA-8229: PASS в составе последнего COA-8227; отдельный E2E не создавался.

## Проверки и команды

- `mvn -q -DskipTests test-compile` — успешно после исправления начального конфликта импортов.
- `mvn -q -Dtest=tests.SelectorMotionUnitTests,tests.TestModelLifecycleUnitTests test` — **13/13 PASS**.
- `python3 -m unittest discover -s scripts -p test_test_model_helpers.py -v` — **21/21 PASS**.
- `python3 scripts/run_test_model_local.py COA-8230 COA-8228 COA-8227 --maven=-q` — первый пакет; затем адресные исправления и повторные прогоны.
- `python3 scripts/run_test_model_local.py COA-8227 COA-8230 COA-8228 COA-8235 --maven=-q` — **все четыре PASS**.
- `python3 scripts/run_test_model_local.py COA-8227 COA-8227 --maven=-q` — **оба PASS** после изменения интервала наблюдения очереди.
- `python3 scripts/run_test_model_local.py COA-8227 --maven=-q` — **финальный PASS** с проверкой уникального ID SDK.
- `git diff --check -- src scripts docs README.md` — без ошибок. Ранее существующий whitespace в tracked-отчёте Android не исправлялся.

## Исправленные проблемы тестов

1. Холодный запуск: optional subscription error появлялся после первоначальной проверки popup; setup ошибочно ждал start screen. Добавлены активация и ожидание доступного стартового экрана перед существующим закрытием optional paywall.
2. Анимация открытия: карточки ещё не отрисованы во время выезда пустой модалки. Детектор перенесён с оранжевой рамки карточки на серый индикатор модалки; исходное видео подтвердило движение. Рамка проверяется отдельно по четырём сторонам карточки с учётом отступов её accessibility-контейнера.
3. Аналитика: один повтор не увидел событие, а очередь при последующем чтении была пуста. Сам этот факт не доказывает отсутствие события в приложении. Добавлен детерминированный тест короткого времени жизни события; интервал опроса уменьшен с 100 до 10 мс. При обнаруженном сбросе номера строк между прогонами добавлена проверка свежего UUID SDK вместо условия SQL id > baseline. Отдельные тесты запрещают повторное использование старого UUID даже с новой строкой/временем и разрешают новое событие после сброса SQL ID.

Эти неуспешные прогоны сохранены в Allure и логах; они не выдаются за дефекты приложения. UI-retry, skip при отсутствии аналитики, изменение feature flags или подмена анимации конечным screenshot не добавлялись.

## Доказательства последнего COA-8227

- [Selector motion](/Users/deedles/IdeaProjects/The-Coach/target/allure-results/d1bfbd6e-a09d-4d7b-b4a2-783b47060e65-attachment.mp4)
- [Sheet handle positions in video (pixels)](/Users/deedles/IdeaProjects/The-Coach/target/allure-results/424cc8e0-b7d1-41af-8cd7-a57b0d90272f-attachment.txt)
- [Fresh simulator analytics evidence](/Users/deedles/IdeaProjects/The-Coach/target/allure-results/d51fef96-cb0c-47d4-9631-dc77ae83e751-attachment.json)
- [Active program orange outline](/Users/deedles/IdeaProjects/The-Coach/target/allure-results/e0947a2a-d242-4281-8485-6a41a44b2c9f-attachment.png)
- [Selector vertical movement](/Users/deedles/IdeaProjects/The-Coach/target/allure-results/6017c5ac-e19f-4c64-afe8-1b22fb787338-attachment.txt)
- [Selector motion](/Users/deedles/IdeaProjects/The-Coach/target/allure-results/0dbd9bba-7257-413c-be1f-770ccbcb6fa7-attachment.mp4)
- [Sheet handle positions in video (pixels)](/Users/deedles/IdeaProjects/The-Coach/target/allure-results/dbc799fc-f602-4253-b10b-a7c60cd7eb1c-attachment.txt)

Логи прогонов: [финальная проверка](../target/today-selector-final-verification.log), [два повторных открытия](../target/today-selector-analytics-repeat-run.log), [связанный regression](../target/today-selector-verification-run.log), [Java](../target/today-selector-unit-run.log), [Python](../target/today-selector-python-tests.log).

## Границы первого этапа

Оставшиеся COA-8232 и COA-8231 реализованы следующим этапом;
см. [отдельный отчёт продолжения](today-program-selector-phase2-report-2026-09-07.md).
Ниже сохранено состояние на момент окончания первого этапа.

COA-8232 не реализован: нужна изолированная фикстура смены программы, фиксированная пара program_id, воспроизводимая загрузка с loader и проверенный rollback. COA-8231 не реализован как отдельная регрессия после смены: исходный порядок/рамка проверены в COA-8227, но COA-8253 после смены на сборке не перепроверялся. Его статус в Jira 7 сентября — Backlog, fixVersions пусты. Android этим этапом не покрыт.

Аналитика проверяет появление события в локальной очереди SDK; доставку в Amplitude она не доказывает. Ограничение наблюдения краткоживущих событий описано в [документации реализации](today-program-selector-automation.md). Jira/Xray не изменялись. Прогресс, выбранная программа, подписка и глобальные flags не менялись; существующие пользовательские изменения в репозитории сохранены.
