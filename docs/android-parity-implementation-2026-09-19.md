# Перенос регрессии на Android — 19 сентября 2026

Работа продолжает [исходный аудит](android-test-parity-audit-2026-09-19.md).
Наличие реализации и прохождение UI-прогона учитываются отдельно. Исходная
пометодная CSV-инвентаризация сохраняется как снимок до переноса.

## Уточнения владельца продукта

- Удаления аккаунта на Android нет. COA-8502 неприменим; обращение в Support
  с запросом удаления не считается эквивалентом.
- Courses, Body Practices и Mind Practices отсутствуют **на обеих платформах**.
  Старые положительные проверки разделов, каруселей, размеров course-карточек
  и входа в Custom Kegel из Explore сняты. Вместо них общий ExploreTests
  проверяет отсутствие всех трёх разделов, просматривая страницу до конца.
  Kegel из Today остаётся отдельной областью тестирования.
- Переноса на завтра, удаления упражнений и восстановления из Removed exercises
  на Android нет. COA-8506/8513/8514 и COA-8511/8512/8517 неприменимы на Android;
  соответствующие iOS-сценарии сохранены.
- COA-8518 ранее отложен для Coach for Her; male Android его не дублирует.

## Карта переноса

| Исходный набор | Android-реализация / решение |
|---|---|
| ModulesTests, 7 | Общий набор, фабрика ModulesPageObject; ранее 7 PASS |
| CoachAuthorizationTests, 5 | Общий набор с AndroidCoachFlowPageObject; ранние return удалены |
| CoachProfileTests, 8 | 7 общих сценариев; удаление аккаунта — явный N/A |
| DailyPlanTests, 5 | 4 общих сценария; отмена удаления упражнения — N/A |
| ExploreTests, 12 исторических | Теперь 5 актуальных общих сценариев; 8 устаревших проверок Courses/практик заменены одной проверкой отсутствия |
| KegelExerciseTests, 5 | AndroidKegelPageObject; требуется отдельная изменяемая Kegel-фикстура с UID |
| MaleBuildStartScreenTests, 2 | AndroidWelcomePageObject и Android OTP-flow; ветви Android содержат проверки |
| SmokeTests, 7 | Общие сценарии; Android clean-install использует системный POST_NOTIFICATIONS |
| TodayProgramSelectorTests, 6 | AndroidProgramSelectorTests: 6 эквивалентов плюс проверка совпадения программ с Explore |
| TestModelAutomationTests | AndroidTestModelTests: 3 mail-сценария; список программ — selector; customization/Her — N/A |
| ReleaseSmokeTests, 5 | AndroidReleaseTests: 4 сценария; AndroidUpdateTests: обновление старой APK без удаления данных при upgrade |
| PushPermissionTests, 2 | AndroidPushPermissionTests: новая установка и повторный запрос после переустановки; отдельный третий тест полного онбординга |
| StoreKitPurchaseTests, 2 | AndroidPurchaseTests: Google Play с обязательным подтверждением license tester/test purchase |
| PdfGuideTests | Общий Page Object; отдельный исполняемый Android release-сценарий требует no-PDF фикстуру |
| Setup/inspection | AndroidModulesSetupTests; identity/catalog читаются AndroidModuleEvidence/AndroidProgramContent |
| Unit/offline | Не дублируются по платформам; алгоритмы общие |
| Игнорируемые Chat/Search/Profile/Feed и HW/Wikipedia | Устаревшие/учебные, не продуктовая регрессия The Coach |

## Запуск

```sh
python3 scripts/run_android_parity.py --serial emulator-5554 --suite safe
python3 scripts/run_android_parity.py --serial emulator-5554 --suite profile
python3 scripts/run_android_parity.py --serial emulator-5554 --suite explore
python3 scripts/run_android_parity.py --serial emulator-5554 --suite selector
python3 scripts/run_android_parity.py --serial emulator-5554 --suite update \
  --old-apk /Users/deedles/Downloads/app-1.40.19-manProd-release.apk \
  --new-apk /Users/deedles/Downloads/app-1.40.21-manProd-release.apk
```

Runner читает одобренную запись Keychain без вывода email/OTP и архивирует
только свежие отчёты, screenshots и page-source в `target/android-parity-<timestamp>`.
`safe` содержит modules, selector и актуальный Explore. Auth/logout, fresh install,
billing и изменяемые Kegel-фикстуры запускаются отдельно. Android noReset сохраняет
сессию между сценариями; selector восстанавливает исходную программу, этап и процент
также при ошибке. Это не гарантия полностью независимого запуска любой старой suite.

Для `push`, `billing`, полного `test-model` требуется `--new-apk`; они переустанавливают
приложение на явно выбранном тестовом эмуляторе. `kegel` требует отдельный
`--fixture-service` с UID/email/OTP; основной аккаунт модулей не назначается
изменяемой фикстурой автоматически. Google Play sandbox ещё не предоставлен.

Google Play требует `GOOGLE_PLAY_LICENSE_TESTER=true` и `PURCHASE_ALLOW=true`
на устройстве с настроенным тестовым Play-аккаунтом. Флаг сам по себе не создаёт
sandbox: перед подтверждением проверяется видимая метка тестовой покупки.
Для PDF нужны `COACH_NO_PDF_ENTITLEMENT_EMAIL`, `COACH_NO_PDF_ENTITLEMENT_OTP`
и `COACH_NO_PDF_ENTITLEMENT_FIXTURE_ID`. Эти значения передаются только через
секреты окружения; реальные реквизиты не хранятся в репозитории.
Kegel completion-тест требует подготовленного экрана обратной связи после
тренировки; остальные Kegel-тесты — карточки Custom Kegel в Today. APK-локаторы
реализованы, но соответствие текущему live-player ещё не подтверждено.
Проверки выполнялись локально; удалённый CI и матрица устройств не запускались.

## Подтверждённые результаты

- 7/7 Modules ранее прошли на Android 1.40.21; контроль iOS — 1/1.
- Profile: основной экран, Account settings, Support, FAQ, Terms — 5 применимых
  проверок прошли (`target/android-parity-20260919-164431`). Тот архив также содержит
  прежнюю проверку Support, ошибочно предложенную как аналог удаления; после
  уточнения пользователя она удалена и не засчитывается в parity.
- Selector: вертикальный scroll — PASS (`target/android-parity-20260919-165235`);
  выбор программы с активной рамкой и восстановление после искусственной ошибки —
  PASS (`target/android-parity-20260919-165959`).
- Update 1.40.19 (338) → 1.40.21 (339): повторный **1/1 PASS**, 52.170 с
  (`target/android-parity-20260919-175707`); проверены рост versionCode и сохранение UID, программы, этапа и процента.
- Explore: 5/5 PASS, 58.858 с (`target/android-parity-20260919-172404`), включая полную проверку отсутствия снятых разделов.
- Selector, полный прогон: 5 PASS / 2 FAIL, 274.045 с (`target/android-parity-20260919-171306`). Аналитика ProgramModalSelect и сравнение destination-контента прошли. Одна ошибка связана с отключёнными системными анимациями эмулятора; добавлено их временное включение с восстановлением. Вторая: Last Longer отсутствует в Browse Programs, но присутствует в Today selector; расхождение подтверждено ручным просмотром всей карусели.
- Today: 4 применимых сценария прошли суммарно в исходном и целевых повторах. Карточки/граница модуля — `target/android-parity-20260919-172536`; урок — `target/android-parity-20260919-172841`; возврат из практики — `target/android-parity-20260919-173016` (18.847 с). Удаление упражнения — N/A.
- Explore открывает программу с изменением активной программы Android Today. Добавлено восстановление исходной программы/этапа/процента после каждого общего Explore-теста. Повтор с этим восстановлением: **5/5 PASS**, 162.841 с (`target/android-parity-20260919-173333`).
- Selector motion после настройки анимаций: 2/2 PASS, 86.918 с (`target/android-parity-20260919-173114`). Системные window/transition/animator scales восстановлены в исходное 0. Из семи selector-проверок шесть подтверждены успешными запусками, сравнение membership остаётся FAIL.
- Welcome: Terms, Privacy, Start, Login и возвраты — **1/1 PASS**, 39.936 с (`target/android-parity-20260919-173658`).
- Auth: 4 из 5 сценариев подтверждены PASS в полном/целевых запусках. Empty email и OTP cancel — `target/android-parity-20260919-174015`; invalid email и resend — 2/2 PASS, 20.718 с (`target/android-parity-20260919-174307`). COA-7935 остаётся FAIL: после OTP показана анкета вместо Today; скриншот: `target/android-parity-20260919-174015/screenshots/test05ExistingProgressUserLoginWithEmailAndOtp.png`.
- Push permissions: **2/2 PASS**, 17.001 с (`target/android-parity-20260919-175357`): выдача системного разрешения после установки, удаление/переустановка, сброс grant и повторная выдача. Проверка полного онбординга выделена отдельно и остаётся красной: выдача разрешения сама по себе не доказывает переход в Today.
- Test model: **1 PASS / 2 FAIL**, 128.498 с (`target/android-parity-20260919-174926`). Валидный email включает Continue; оба анонимных CONNECT не достигнуты из-за незакрываемого пустого paywall после анкеты.
- Полный clean-install onboarding: FAIL на пустом paywall. Скриншот фактического состояния: `target/android-parity-20260919-174622/screenshots/testReinstallRequestsNotificationPermissionAgain.png` (снят до разделения permission и onboarding проверок). Кнопка закрытия не меняет экран; причина на стороне приложения/сервиса пока не установлена.
- Profile, оставшиеся два сценария: logout PASS, открытие Profile из Explore ERROR — кнопка отсутствует (`target/android-parity-20260919-180024`, 46.384 с). Итого 6 из 7 применимых Profile-сценариев подтверждены; удаление аккаунта N/A.
- Release: **2 PASS / 1 ERROR**, 60.005 с (`target/android-parity-20260919-175905`). Три вкладки и актуальный каталог/отсутствие снятых разделов прошли. Private coaching не найден; состав этой конфигурации требует уточнения, проверка не удалена.
- **74/74 Java unit-проверки — PASS** (`mvn -q '-Dtest=*UnitTests' test`); архив `target/android-parity-offline-20260919`.
- **28 Python offline-проверок — PASS**, включая защиту от пропуска modules в `safe` runner и сохранение ненулевого exit code тестов.

## Ограничения проверки

Неподготовленные Google Play, no-PDF и Kegel-фикстуры не считаются пройденными UI-тестами.
Анонимный CONNECT требует отдельной чистой установки. Покупка проверяет `subscription.active: false → true` для того же Firebase UID; Today не используется как самостоятельное доказательство Premium. Подтверждение Google Play требует видимой метки тестовой покупки. Android login может привести
к анкете вместо Today; обычный тест авторизации должен фиксировать это как ошибку,
а специальная подготовка upgrade-фикстуры явно завершает разрешённую анкету.

iOS-проверка отсутствия Courses/практик выполнена: **1 FAIL**, 99.431 с
(`target/ios-explore-parity-20260919-175501`). В установленной preprod-сборке
`com.vamapps.preprod.The-Coach`, версия 1.13.29, видны BODY PRACTICES и MIND PRACTICES. Это расхождение
текущей сборки/конфигурации с уточнённым требованием; ожидание отсутствия сохранено.
Скриншот: `target/ios-explore-parity-20260919-175501/screenshots/testRemovedCoursesAndPracticesStayAbsent.png`.
Первая попытка не создала сессию из-за отсутствующего Simulator.app в Xcode;
повтор с `IOS_IS_HEADLESS=true` прошёл до проверки UI. Добавлена опциональная
capability `ios.isHeadless` / `IOS_IS_HEADLESS`, обычный запуск не изменён. Карточки Private coaching не объявлены неприменимыми:
они не наблюдались в данной Android-конфигурации и остаются отдельным вопросом
старого AndroidExploreTests. Новый общий ExploreTests их наличие не предполагает.

Снимки неудачных попыток сохраняются до восстановления исходного состояния.
Аналитика проверяет свежий timestamp, UID, insert_id и program_id, не принимает
старую очередь событий за результат текущего действия. Каталог сравнивается со
свежим read-only GET и явно не называется перехваченным сетевым трафиком приложения.
По завершении остановлены только запущенные для этой задачи Appium и Android-эмулятор.
Последний Profile-тест штатно вышел из Android-аккаунта; прогресс тренировок не сбрасывался.

## Открытые риски

| Приоритет | Наблюдение | Последствие / следующий шаг |
|---|---|---|
| P1 | Last Longer есть в Today selector, но отсутствует во всей карусели Browse Programs | Проверка COA-8235 остаётся красной; требуется решение по каталогу/требованию, ожидание не ослаблено |
| P1 | Нет Google Play sandbox и отдельной изменяемой Kegel-фикстуры | Покупки/полный player-flow не подтверждены реальным прогоном |
| P2 | Android Profile недоступен напрямую из Explore | COA-8503 падает; подтвердить платформенное отличие или вернуть кнопку |
| P2 | Private coaching не найден в Android Explore | Существующая проверка состава разделов падает; применимость ещё не подтверждена |
| P2 | Не подготовлена no-PDF entitlement fixture | Отдельный PDF UI-тест не засчитывается как PASS |
| P2 | Welcome legal links — ClickableSpans без отдельных accessibility nodes | Android использует проверенную геометрию английской строки с защитными проверками; другие раскладки требуют accessibility IDs |
| P1 | После чистого онбординга пустой paywall не закрывается | Полный onboarding и anonymous CONNECT блокируются до проверяемых шагов; скриншот сохранён |
| P1 | Вход существующего Android-аккаунта после OTP открывает анкету | Успешная обычная авторизация пока FAIL; upgrade setup обрабатывает анкету явно |
| P1 | В iOS preprod ещё видны BODY/MIND PRACTICES | Новый тест отсутствия падает; сверить установленную сборку/конфигурацию с актуальными требованиями |

Скриншоты/иерархии реальных неудач находятся в соответствующих архивах; никаких
искусственно созданных картинок ошибок не используется. Например, расхождение
каталогов: `target/android-parity-20260919-171306/screenshots/androidSelectorBeforeRestore.png`.
