# Today selector — COA-8232 и COA-8231, 7 сентября 2026

Оставшиеся два кейса реализованы для iOS в `TodayProgramSelectorTests`.
Оба прошли повторно, возврат после намеренной ошибки подтверждён отдельным UI-тестом.
COA-8232 проверяет переключение на Keep It Hard, закрытие модалки, вкладку Today,
заголовок, модуль, этап, три карточки из свежего ответа текущего аккаунта,
`ProgramModalSelect` с точным `program_id=keep_it_hard` и появление/исчезновение
skeleton-лоадера. COA-8231 проверяет первую позицию и оранжевую рамку активной
программы до смены и после повторного открытия селектора с новой программой.

Окружение: iPhone 17 Pro, iOS 26.5 Simulator; `/Users/deedles/Downloads/The Coach 3.app`,
bundle `com.vamapps.preprod.The-Coach`, версия 1.13.29 (`CFBundleVersion=BITRISE_BUILD_NUMBER`),
локальный Appium. У каждого кейса отдельный аккаунт из Keychain; onboarding и
повторный вход подтверждены до прогонов. Общая фикстура COA-8235 не переключалась.

## Подтверждённые прогоны

| Проверка | Результат | Время по Allure | Артефакт |
| --- | --- | --- | --- |
| COA-8232, первый прогон | PASS | 78.594 с | [Allure JSON](../target/allure-results/aca4c5c2-c4f8-46d4-9f5f-15727d04ddbf-result.json) |
| COA-8231, первый прогон | PASS | 83.625 с | [Allure JSON](../target/allure-results/60fbc268-93a1-4039-93cf-be4681e46207-result.json) |
| COA-8232, повтор | PASS | 79.151 с | [Allure JSON](../target/allure-results/50fd88d0-2f9f-4fb9-8ab0-69eff57cf1ff-result.json) |
| COA-8232, финальный с обновлённым recorder | PASS | 78.954 с | [Allure JSON](../target/allure-results/338ed9f0-51b3-447f-b8a4-aa08959fc9a6-result.json) |
| COA-8231, повтор | PASS | 83.070 с | [Allure JSON](../target/allure-results/91e81b9a-cdb9-4385-97b9-9c8966313966-result.json) |
| COA-8227, связанная регрессия | PASS | 111.808 с | [Allure JSON](../target/allure-results/de46f3e2-f1fd-4d5b-a1b2-e012e7407784-result.json) |
| Восстановление после намеренной ошибки | PASS | 73.655 с | [Allure JSON](../target/allure-results/05e61063-0a36-463b-9bdf-733269768ac5-result.json) |

В тесте восстановления ошибка намеренно возникает после переключения на Keep It Hard.
Проверяется, что исходная AssertionError не потеряна, восстановление не добавило
собственную ошибку, а UI снова показывает Last Longer, исходный этап и прогресс.
Это отдельная инфраструктурная проверка, а не ещё один Jira-кейс.

COA-8253 не воспроизвёлся на этой сборке и новой фикстуре: после выбора Keep It Hard
он был первым и выделенным при повторном открытии. Этот результат не доказывает
исправление во всех окружениях и не меняет статус бага в Jira.

## Проверки вспомогательного кода

- Java: **33/33 PASS** — motion (10), loading (12), lifecycle (3), обязательное восстановление (8).
- Python: **58/58 PASS** — существующие helpers (21), точный program_id в аналитике (12), свежий контент и UID (18), аварийное завершение recorder (7).
- `git diff --check -- src scripts docs README.md` — без ошибок.
- `bash -n scripts/validate_test_model_data.sh` — успешно; full-suite data contract дополнен email/OTP/UID двух новых фикстур.

Java negative controls запрещают статичный экран, затемнение без skeleton,
незавершённую загрузку и потерю исходной ошибки при сбое cleanup. Python controls
запрещают отсутствующий/другой/неправильного типа program_id, старые события,
чужой UID, устаревший cache, неизвестные метаданные и небезопасные пути cache.
Ревью также выявило возможный оставшийся процесс записи при timeout/ошибке READY.
Теперь Java посылает SIGTERM и ограниченно ждёт завершения Python; Python завершает
только собственные simctl/ffmpeg process groups с SIGKILL fallback. Первичная ошибка
сохраняется, ошибка cleanup не заменяет её.
Обычная запись и извлечение кадров после этого изменения проверены финальным
успешным COA-8232; аналитика, skeleton, контент и восстановление также прошли.

Команды:

```bash
python3 scripts/run_test_model_local.py COA-8232 COA-8231 --provision --maven=-q
python3 scripts/run_test_model_local.py COA-8232 --maven=-q
python3 scripts/run_test_model_local.py COA-8231 --maven=-q
python3 scripts/run_test_model_local.py COA-8232 COA-8231 COA-8227 --maven=-q
python3 scripts/run_test_model_local.py COA-8232 --maven=-q \
  '--maven=-Dtest=tests.TodayProgramSelectorTests#testProgramChangeFixtureRestoresAfterFailure'
mvn -q -Dtest=tests.SelectorMotionUnitTests,tests.TestModelLifecycleUnitTests,tests.RestoringTestActionUnitTests,tests.ProgramLoadingEvidenceUnitTests test
python3 -m unittest discover -s scripts -p 'test_test_model_helpers.py' -v
python3 -m unittest discover -s scripts -p 'test_program_select_analytics.py' -v
python3 -m unittest discover -s scripts -p 'test_read_simulator_program_content.py' -v
python3 -m unittest discover -s scripts -p 'test_record_simulator_motion.py' -v
```

Логи: [provisioning](../target/today-selector-provision-phase2.log),
[COA-8232](../target/today-selector-select-phase2.log),
[COA-8231](../target/today-selector-order-phase2.log),
[сбой и восстановление](../target/today-selector-recovery-phase2.log),
[повтор и связанная регрессия](../target/today-selector-phase2-final.log),
[финальный recorder/COA-8232](../target/today-selector-phase2-recorder-verification.log),
[Java](../target/today-selector-phase2-unit.log), [Python](../target/today-selector-phase2-python.log).

## Доказательства COA-8232

- [Видео переключения](../target/allure-results/a4ecc6f4-e1e2-4b67-ad81-2c8aa1e29268-attachment.mp4)
- [ProgramModalSelect](../target/allure-results/eae29c18-be75-45b5-918f-62a4a78e1385-attachment.json)
- [Свежий контент текущей сессии](../target/allure-results/a4ffec80-ecb2-4f80-8bab-fe5affe0693d-attachment.json)
- [Skeleton](../target/allure-results/8701152f-d75d-4d5d-962f-aebb03ebb81f-attachment.png)
- [Контент после загрузки](../target/allure-results/fb14d19c-1bca-41db-8e07-d7a5f6f53428-attachment.png)
- [Восстановленное состояние](../target/allure-results/4e0d50b5-30b0-4038-8a17-6c5c2a8e3667-attachment.txt)

## Ограничения и сохранность данных

Проверяется iOS Simulator с наблюдавшимся layout. Skeleton определяется по паре
серых областей заголовка и модуля; на текущей сборке загрузка может завершиться,
пока модалка ещё закрывается. После закрытия отдельно подтверждается именно
выбранная программа и содержимое Today. Ненаблюдаемый лоадер вызывает FAIL.
Численного требования к длительности загрузки в ТК нет.

Аналитика подтверждает появление в локальной SDK-очереди, не доставку в Amplitude.
Контент читается из ответа, полученного приложением после текущего действия;
HTTP-запросов из теста нет. Reader зависит от проверенного формата CFURL cache
request v9 / response v1; неизвестный формат вызывает ошибку. Подробнее в
[описании реализации](today-program-selector-automation.md).

После каждого сценария в `finally` проверяется восстановление Last Longer,
исходного этапа и прогресса 0%. Подписки, feature flags и тестовые API не менялись.
Jira/Xray не изменялись. Android не запускался. Прежние изменения пользователя
в рабочем каталоге сохранены; коммиты не создавались.
