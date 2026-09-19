# Автотесты модулей Daily Plan

Источник сценариев — `modules-automation-handoff.json`, revision 6, и
`modules-automation-review.md` от 19 сентября 2026. Это спецификация проверок;
её `ready_for_automation` не означает готовую фикстуру или успешный прогон.

## Реализованный набор

`suites.ModulesSuite` → `tests.ModulesTests`, существующий Java 8 / JUnit 4 /
Appium стек. Семь независимых методов; последовательный запуск на одном
изолированном аккаунте. Ключ COA-9044 служит именем аккаунта для всего набора,
каждый метод сохраняет собственные Jira-ключи через `@Issue`.

| Кейсы | Метод / состояние реализации |
|---|---|
| COA-9015, COA-9016 | `testModuleTitleAndStageAreVisible`: непустой Module, Stage X of Y, отсутствие прежнего Day |
| COA-9014 | `testHeaderBoundsAndPeerOverlap`: **частично**, границы контейнера/экрана и пересечения названия, Stage и стрелок |
| COA-9044 | `testNextStageWithoutCompletingActivities`: один шаг вперёд |
| COA-9045 | `testPreviousStagesBackToFirst`: назад по каждому этапу до первого |
| COA-9046 | `testLastStageIsAccessibleWithoutCompletion`: все переходы до последнего этапа |
| COA-9047 | `testPreviousFromLastStage`: последний → предпоследний этап |
| COA-9048, COA-9049, COA-9050, COA-9051 | `testNextModuleBlockedAndGotItRetainsStage`: блокировка, видимый поп-ап, GOT IT, сохранение этапа и повторная попытка |
| COA-9017 | Не реализован: нужна вторая модульная программа с отличающимся прогрессом |
| COA-9018 | Не реализована матрица устройств; геометрический тест нельзя считать покрытием всей матрицы |
| COA-9052, COA-9054 | Не реализованы: нужны независимые lesson-only / practice-only completion-потоки последнего этапа |
| COA-9055, COA-9056, COA-9057, COA-9061 | Не реализованы: 18 календарных комбинаций, управление реальной датой устройства и завершённым циклом обновления |
| COA-9058, COA-9062 | Не реализованы: фикстура завершения активности в A и просмотра B, затем восстановление A |
| COA-9059 | Не реализован: нужен доступ минимум к двум модулям |
| COA-9060 | Не реализован: предыдущие этапы завершены, последний не выполнен |

Названия модулей с Airtable не сравниваются. Номер модуля входит в идентичность
позиции: одинаковый Stage в следующем модуле не считается прежним этапом.
Текст поп-апа используется существующим локатором для распознавания диалога,
его полный контент, GIF и Remote Config не проверяются.

Границы accessibility-элементов не доказывают отсутствие обрезания глифов,
перекрытия произвольными слоями или соблюдение safe area. COA-9014 остаётся
частичным. Исходная матрица COA-9018: iPhone 17 Pro Max, iPhone 13,
iPhone SE 2020, Samsung S25 Ultra, Samsung S25. Android Page Object для этого
набора пока отсутствует. Локаль English, portrait, штатный размер текста.

## Изоляция и данные

По запросу пользователя используется новый аккаунт. Runner резервирует отдельный
alias `+coa9044` в существующем Keychain registry и вызывает уже имеющийся UI
provisioning с подтверждением почты и повторным входом. Повторный запуск
сохраняет подтверждённую учётную запись. Секреты передаются окружением,
COA9044 добавлен в существующую маскировку диагностических артефактов.

Перед каждой проверкой нужны:

- preprod Simulator build `com.vamapps.preprod.The-Coach`;
- подтверждённый UID этого аккаунта, доступ к нужной программе и включённые модули;
- первый этап первого модуля, минимум два этапа и следующий модуль;
- все активности просматриваемых этапов незавершены и не перенесены.

Наличие новой учётной записи само по себе не доказывает эти предусловия.
Тест сверяет UID текущей сессии и читает свежий ответ Today из URL cache после
UI-действия. Ответ должен принадлежать ожидаемым UID/программе, иметь свежую
дату и непросроченную авторизацию. Неизвестные completion-флаги, завершённые
или перенесённые активности, отсутствие модульных границ и конец программы
отклоняются. Подготовка через подмену ответа приложения не используется.

Навигация ограничена текущим модулем, активности не завершаются. В `finally`
восстанавливается исходный просмотренный этап; при падении диагностика
сохраняется **до** восстановления. Ошибка восстановления не затирает исходную.
Не запускать этот аккаунт параллельно в другой сессии.

## Запуск

Appium должен слушать `http://127.0.0.1:4723/`; существующий
`ci-scripts/run-ios-simulator.sh` задаёт Simulator, сборку и UDID.

```bash
appium --base-path / --address 127.0.0.1 --log-level warn
python3 scripts/run_test_model_local.py COA-9044 --provision --maven=-q
python3 scripts/run_test_model_local.py COA-9044 --modules-program-id=last_longer --maven=-q
```

Последняя команда предполагает, что модульная фикстура подготовлена именно в
`last_longer`. Если build назначает другую программу, нужно передать её
фактический `program_id`; тест не переключает и не сбрасывает посторонние
программы автоматически. Запуск через Maven требует тех же секретов и UID:

```bash
bash ci-scripts/run-ios-simulator.sh -Dtest=suites.ModulesSuite \
  -Dcoach.modules.enabled=true -Dcoach.modules.programId=last_longer \
  -Dios.noReset=true -Dios.fullReset=false
```

Без `coach.modules.enabled` набор пропускается JUnit, чтобы общий запуск не
использовал случайную сессию. В явно включённом наборе отсутствие фикстуры —
ошибка, а не успешный тест. Команды без устройства:

```bash
mvn -q -Dtest=tests.ModuleStageUnitTests,tests.RestoringTestActionUnitTests test
python3 -m unittest discover -s scripts -p 'test*content.py' -v
python3 -m unittest discover -s scripts -p 'test_module_runner.py' -v
python3 -m unittest discover -s scripts -p 'test_premium_inspection_runner.py' -v
```

## Фактическая проверка 19 сентября 2026

- Компиляция проекта и 18 Java-проверок парсинга/геометрии/восстановления прошли.
- 27 Python-проверок cached-response reader, 3 проверки module runner и
  9 существующих проверок premium runner прошли. Всего 57 offline-проверок.
- Xcode сначала сообщал о непринятой лицензии, затем `simctl` стал доступен.
  При запуске Appium обнаружено отсутствие
  `/Applications/Xcode.app/Contents/Developer/Applications/Simulator.app`.
  Запуск сервера с `--default-capabilities '{"appium:isHeadless":true}'`
  позволил создать сеанс на iPhone 17 Pro / iOS 26.5, build 1.13.29.
- Provisioning COA-9044 дошёл до формы поиска почты. Приложение показало
  **`An error occured. Try again later.`**; OTP и `Email not found` отсутствовали.
  Причина общего ответа приложения не установлена. Alias зарезервирован в
  Keychain со статусом `planned`; создание/подтверждение аккаунта не доказано.
  Существующий provisioning теперь явно распознаёт этот ответ и останавливается
  с диагностикой вместо ожидания неизвестного исхода.
- Диагностика: `target/page-source/testProvisionAndVerifyDedicatedAccount.xml`,
  `target/appium-logs/testProvisionAndVerifyDedicatedAccount.log`,
  `target/surefire-reports/tests.TestModelAccountProvisioningTests.txt`.
  В первом прогоне скриншот формы с введённой почтой не сохранялся, XML маскировался.
- В первом проходе семь module UI-тестов **не выполнялись**: не было подтверждённого отдельного аккаунта.
  Offline-проверки не являются результатом прохождения этих сценариев в приложении.

После устранения ошибки поиска почты повторить `--provision`: runner возобновит
тот же alias, затем проверить фактическое назначение модулей и запустить набор.
Ни один из оставшихся 12 кейсов не представлен пустым проходящим тестом.

### Повтор по запросу пользователя

Ошибка поиска почты не повторилась. Следующий запуск выявил неучтённый промо-поп-ап
`Get an access for your partner`, закрывающий профиль и Today. Снимок этой ошибки:
`target/screenshots/modules-provisioning-promo-2026-09-19.png`.
В iOS Page Object добавлено закрытие именно этого поп-апа по наблюдаемому
контейнеру и единственной безымянной кнопке X; покупки не выполняются.
Возобновление provisioning теперь открывает профиль уже созданного анонимного
пользователя. После исправления provisioning прошёл: **1 test, 0 failures,
0 errors**. Аккаунт COA-9044 подтверждён, повторный вход и UID проверены.

Обработчик падений теперь сохраняет снимки login/CONNECT с закрытыми полем почты
и клавиатурой. Маскирование выполняется над настоящим PNG в памяти до записи,
сообщение приложения сохраняется. Четыре проверки маскирования прошли:
`mvn -q -Dtest=tests.LoginFailureScreenshotUnitTests test`.
OTP-экраны по-прежнему не сохраняются этим обработчиком.

При первом module smoke после перезапуска появилось `Cannot Load Subscription Options`.
Снимок сохранён отдельно: `target/screenshots/modules-subscription-error-2026-09-19.png`.
Из подготовки исключён ненужный этому сценарию restart: свежесть Today теперь
проверяется относительно обязательного нового входа, UID по-прежнему сверяется.
Нормализация запуска обрабатывает промо, появляющееся после закрытия ошибки подписки.

Проверка фикстуры уточнена по свежему ответу нового аккаунта: `is_moved_forward`
отсутствует у нетронутых карточек, включая переносимую практику. Отсутствие этого
опционального поля допускается, явные `true`, `null` и неверный тип отклоняются.
`completed=false` по-прежнему обязателен. 28 проверок reader прошли.

**Итог повторного полного UI-прогона:** `Tests run: 7, Failures: 0, Errors: 5,
Skipped: 0`, 525.271 s, Maven exit code 1.

| Тест | Результат |
|---|---|
| `testModuleTitleAndStageAreVisible` | PASS |
| `testHeaderBoundsAndPeerOverlap` | PASS |
| `testNextStageWithoutCompletingActivities` | ERROR — экран подписки после стрелки |
| `testPreviousStagesBackToFirst` | ERROR — экран подписки при подготовке последнего этапа |
| `testLastStageIsAccessibleWithoutCompletion` | ERROR — экран подписки после стрелки |
| `testPreviousFromLastStage` | ERROR — экран подписки при подготовке последнего этапа |
| `testNextModuleBlockedAndGotItRetainsStage` | ERROR — экран подписки при подготовке последнего этапа |

Все пять снимков/деревьев `*BeforeRestore` подтверждают
`subscription_error_illustration` / **Cannot Load Subscription Options**.
Техническое сообщение JUnit — `Module title is absent`: экран подписки скрывает
модуль, поэтому до ожидаемого перехода тест не доходит. В свежем cached `/user/`
ответе с проверенным UID новой учётной записи `subscription.active=false`.
Это блокер доступа тестовой фикстуры; дефект бизнес-правил модулей не доказан.
Покупки и выдача подписки не выполнялись. Для навигационного прогона нужно
предоставить новому аккаунту COA-9044 тестовый доступ к программе/Premium.

Артефакты текущего прогона:

- `target/screenshots/modules-navigation-subscription-error-2026-09-19.png` — снимок после стрелки;
- `target/page-source/previousStagesBeforeRestore.xml` — UI-дерево до восстановления;
- `target/surefire-reports/suites.ModulesSuite.txt` — результат всех семи тестов;
- `target/allure-results/` — шаги и вложения каждой проверки.

## Следующая часть handoff

Для COA-9052/9054 нужны именно UI-завершения урока/практики последнего этапа,
с доказанной блокировкой до действия, отсутствием второй completion-ветви и
невыполненными предыдущими этапами. Backend seed не должен заменять проверяемое
UI-действие. Для COA-9060 нужна отдельная противоположная фикстура.

Календарная матрица каждого lifecycle-варианта запускается заново:

| Кейс | Исходный этап | Completion | Lifecycle | Комбинаций |
|---|---|---|---|---|
| COA-9055 | Обычный | Только урок / только практика | foreground / resume / restart | 6 |
| COA-9056 | Обычный | Нет | foreground / resume / restart | 3 |
| COA-9057 | Последний | Только урок / только практика | foreground / resume / restart | 6 |
| COA-9061 | Последний | Нет | foreground / resume / restart | 3 |

Дата D → D+1 должна изменяться на устройстве в Europe/Samara.
`simctl status_bar override --time` изменяет отображение времени, а не часы
приложения, и не подходит. Изменение часов macOS-хоста тоже не реализовано.
Нужен контролируемый механизм смены/восстановления даты, timezone и auto-time;
для негативных ветвей — свидетельство завершённого цикла обновления.


### Проверка предоставленного аккаунта с подпиской

По следующему запросу пользователя добавлен отдельный режим для предоставленного
аккаунта. Данные хранятся в Keychain `the-coach-modules-premium`; registry
изолированного COA-9044 и ранее использованный shared Premium не заменяются.
Почта и OTP передаются только окружением, без записи в исходники.

```bash
python3 scripts/run_test_model_local.py COA-9044 --modules-premium --modules-inspect-account --maven=-q
# После успешной проверки подписки и предусловий:
python3 scripts/run_test_model_local.py COA-9044 --modules-premium --modules-program-id=last_longer --maven=-q
```

Inspection выполняет вход и сохраняет исходное состояние Today; после него
runner проверяет UID/email и активность подписки через read-only `/user/`.
Режим не создаёт аккаунт, не заполняет анкету и не завершает активности.
API-клиент inspection запрещает выбор дня и любые записи.

Фактический запуск inspection: **1 test, 1 error**, 60.924 s, Maven exit 1.
После ввода OTP открылся экран **QUIZ / GOALS / WHAT DO YOU WANT TO ACHIEVE?**;
ожидание Today завершилось `Authorized dashboard did not open`.
Отдельный read-only lookup подтвердил соответствие текущего UID предоставленной
почте. Это подтверждает идентичность аккаунта, но не активность подписки.
Premium и предусловия модулей пока не проверены; module suite на этом аккаунте
не запускался. Для продолжения требуется выбрать программу и завершить анкету.

Скриншот фактического состояния:
`target/screenshots/modules-premium-onboarding-blocker-2026-09-19.png`.
UI-дерево: `target/page-source/inspectSuppliedModuleAccount.xml`.
JUnit: `target/surefire-reports/tests.ModulesAccountInspectionTests.txt`.
Семь offline-проверок module runner и девять existing premium runner прошли.


### Завершение анкеты с согласия пользователя

Пользователь одобрил подготовку Last Longer. Перед изменениями выполнена новая
read-only проверка текущего UID/email; UID сохранён в отдельном Keychain registry.
Явный `-Dcoach.modules.completeOnboarding=true` в account inspection возобновляет
анкету только при совпадении UID, выбирает Beat Premature Ejaculation и использует
существующий onboarding Page Object. Обычный inspection анкету не заполняет.

```bash
python3 scripts/run_test_model_local.py COA-9044 --modules-premium --modules-inspect-account \
  --maven=-q --maven=-Dcoach.modules.completeOnboarding=true
```

Подготовка прошла: **1 test, 0 failures, 0 errors**, 47.871 s.
Read-only `/user/` подтвердил **subscriptionActive=true**. Исходное состояние:
Last Longer, Module 1, Stage 1 of 7, прогресс 0%. Активности не завершались.


Первый focused-переход на Premium-аккаунте прошёл: **1 test, 0 errors**,
68.336 s, включая возврат на первый этап.

Во время следующего полного прогона оба вызова проверки блокирующего поп-апа
и закрытия GOT IT прошли, но последующее восстановление этапа завершилось
ошибкой: Today показывал загрузочные заглушки. Свежий скриншот этого падения:
`target/screenshots/modules-premium-restoration-loading-2026-09-19.png`,
UI-дерево с тем же именем в `target/page-source/`.
Это другое состояние, чем старый экран ошибки подписки; старые файлы
`blockedModuleBeforeRestore.*` остались от предыдущего аккаунта и не являются
доказательством данного падения.

В `waitForPosition` устранён вложенный 10-секундный timeout `position()`,
который мог оборвать внешнее 15-секундное ожидание. Теперь одно явное ожидание
повторно читает видимые элементы заголовка и точную позицию при обновлении Today.
Общий timeout не увеличен; автоматический retry тестов не добавлен.


**Результат полного прогона на Premium:** 7 tests, 0 failures, 1 error,
0 skipped, 532.236 s: шесть PASS, один ERROR при восстановлении после
`testNextModuleBlockedAndGotItRetainsStage`. Отчёт сохранён без перезаписи:
`target/surefire-reports/suites.ModulesSuite.txt`.

**Повтор после исправления ожидания:**

```bash
python3 scripts/run_test_model_local.py COA-9044 --modules-premium --modules-program-id=last_longer \
  --maven=-q --maven=-Dtest=tests.ModulesTests#testNextModuleBlockedAndGotItRetainsStage
```

1 test, 0 failures, 0 errors, 0 skipped, 93.931 s, exit 0.
Обе проверки блокирующего поп-апа/GOT IT и восстановление Module 1 / Stage 1 of 7
имеют PASS в Allure. Отчёт повтора: `target/surefire-reports/tests.ModulesTests.txt`.
Таким образом, каждый из семи реализованных сценариев прошёл; это **6 PASS
в полном прогоне + 1 PASS в отдельном повторе**, а не новый чистый полный прогон.
Причина задержки загрузки приложения не доказана. Артефакт первоначального сбоя
сохранён. После восстановления стандартный teardown вышел из аккаунта.
Семь unit-проверок module runner и девять premium runner повторно прошли;
`git diff --check` для изменённых исходников и скриптов не выявил ошибок.
