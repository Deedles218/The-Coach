# Android parity audit — 19 сентября 2026

> Это исходный аудит до переноса. Актуальные исключения, реализации и результаты:
> [отчёт переноса](android-parity-implementation-2026-09-19.md).

## Executive summary

**Нет: все прежние тесты не продублированы для Android.** До текущей задачи
явная Android-реализация была у 5 smoke, 7 Explore и 6 общих onboarding/paywall
сценариев. Это наличие кода, а не подтверждение их прохождения на версии 1.40.21.
Семь модульных тестов теперь работают общим набором для iOS/Android.
Полный Android-прогон на manProd 1.40.21: **7 PASS, 0 failures/errors/skipped**.
Контрольный iOS-переход после общего рефакторинга: **1 PASS**.

## Scope and evidence

Проверены все Java-файлы `src/test/java/tests`, их base classes, Page Objects,
factories, suites, Platform, CoreTestCase, Maven и локальные команды запуска.
Полная пометодная инвентаризация с номерами строк: [CSV](android-test-parity-inventory.csv).
До изменений было 44 файла / 199 методов `@Test`; добавленный Android setup —
отдельный служебный сценарий. Числа включают ignored, unit и legacy HW, поэтому
не являются размером выполняемой продуктовой регрессии.

## Assumptions and limitations

Аудит прежних наборов статический: покупки, удаление, reset программ и весь
исторический regression не запускались. Запуск текущих module/setup сценариев
описан отдельно в [отчёте модулей](modules-automation.md).
StoreKit — iOS-специфичен; Android-эквивалент должен проверять Google Play.
Общему тесту с двумя Page Objects не нужна дублирующая копия Java-метода.
HW/Wikipedia и offline unit checks не считаются Android-покрытием The Coach.

## Overall assessment

Оценки относятся к полноте Android parity, не к качеству продукта.

| Область | 0–5 | Основание |
|---|---:|---|
| Requirements quality | N/A | Полный набор Android требований не предоставлен |
| Risk coverage | 2 | Базовые smoke есть, player/selector/customization/lifecycle отсутствуют |
| Test design | 3 | Сценарии разделены, но часть guards даёт ложный PASS |
| Functional coverage | 2 | Только часть продуктовых потоков перенесена |
| Non-functional coverage | 1 | Нет Android-эквивалента selector motion/loading/device matrix |
| Automation architecture | 3 | Factories и Android base есть, но много iOS-specific зависимостей |
| Reliability/flakiness | 2 | Early return и неполные Android locators; нет полного свежего прогона |
| Diagnostics | 3 | Скриншоты/XML/logs есть; платформенная маркировка старых файлов неоднозначна |
| Maintainability | 2 | Смешаны legacy, ignored, setup и активная регрессия |
| CI/CD | 2 | SmokeSuite включает только iOS SmokeTests; AndroidSmokeTests отдельно |
| Release readiness | N/A | Это аудит parity, нет Android release-run evidence |

## Coverage map

| Класс / источник | Методов | Android-статус |
|---|---:|---|
| [AndroidExploreTests](../src/test/java/tests/AndroidExploreTests.java#L62) | 7 | Android: 7 отдельных сценариев; не полная копия iOS Explore |
| [AndroidModulesSetupTests](../src/test/java/tests/AndroidModulesSetupTests.java#L16) | 1 | Android setup, opt-in; не продуктовый тест модулей |
| [AndroidSmokeTests](../src/test/java/tests/AndroidSmokeTests.java#L41) | 5 | Android: 5 отдельных smoke-сценариев; частичное совпадение с iOS |
| [ChatTests](../src/test/java/tests/ChatTests.java#L20) | 11 | @Ignore: устаревший Chat |
| [CoachAuthorizationFlowTests](../src/test/java/tests/CoachAuthorizationFlowTests.java#L28) | 1 | @Ignore: устаревший Feed flow |
| [CoachAuthorizationTests](../src/test/java/tests/CoachAuthorizationTests.java#L27) | 5 | Нет полной Android-копии; 5 ранних return без проверок |
| [CoachProfileTests](../src/test/java/tests/CoachProfileTests.java#L27) | 8 | Нет полной Android-копии; 8 ранних return без проверок |
| [DailyPlanTests](../src/test/java/tests/DailyPlanTests.java#L44) | 5 | Нет Android-копии; 5 ранних return без проверок |
| [ExploreTests](../src/test/java/tests/ExploreTests.java#L49) | 12 | Нет полной Android-копии; 12 ранних return без проверок |
| [ArticleTests](../src/test/java/tests/HW/ArticleTests.java#L13) | 2 | Legacy HW/Wikipedia: не покрытие The Coach; Android parity не засчитывается |
| [ChangeAppCondition](../src/test/java/tests/HW/ChangeAppCondition.java#L13) | 2 | Legacy HW/Wikipedia: не покрытие The Coach; Android parity не засчитывается |
| [GetStartedTest](../src/test/java/tests/HW/GetStartedTest.java#L10) | 1 | Legacy HW/Wikipedia: не покрытие The Coach; Android parity не засчитывается |
| [HomeworkEx12](../src/test/java/tests/HW/HomeworkEx12.java#L10) | 1 | Legacy HW/Wikipedia: не покрытие The Coach; Android parity не засчитывается |
| [HomeworkEx17](../src/test/java/tests/HW/HomeworkEx17.java#L22) | 1 | Legacy HW/Wikipedia: не покрытие The Coach; Android parity не засчитывается |
| [HomeworkEx18](../src/test/java/tests/HW/HomeworkEx18.java#L11) | 1 | Legacy HW/Wikipedia: не покрытие The Coach; Android parity не засчитывается |
| [HomeworkEx8](../src/test/java/tests/HW/HomeworkEx8.java#L1) | 0 | Legacy HW/Wikipedia: не покрытие The Coach; Android parity не засчитывается |
| [MyListsTests](../src/test/java/tests/HW/MyListsTests.java#L23) | 1 | Legacy HW/Wikipedia: не покрытие The Coach; Android parity не засчитывается |
| [KegelCardLocatorUnitTests](../src/test/java/tests/KegelCardLocatorUnitTests.java#L28) | 4 | Unit/offline: не требует дублирования на мобильные платформы |
| [KegelExerciseTests](../src/test/java/tests/KegelExerciseTests.java#L33) | 5 | Нет Android player/completion parity: 1 ранний return, остальные Assert.fail iOS-only |
| [LoginFailureScreenshotUnitTests](../src/test/java/tests/LoginFailureScreenshotUnitTests.java#L23) | 4 | Unit/offline: не требует дублирования на мобильные платформы |
| [MaleBuildStartScreenTests](../src/test/java/tests/MaleBuildStartScreenTests.java#L55) | 2 | iOS-only; 2 ранних return на Android |
| [ModuleStageUnitTests](../src/test/java/tests/ModuleStageUnitTests.java#L11) | 10 | Unit/offline: не требует дублирования на мобильные платформы |
| [ModulesAccountInspectionTests](../src/test/java/tests/ModulesAccountInspectionTests.java#L23) | 1 | iOS inspection opt-in; подготовка отдельно от Android module suite |
| [ModulesTests](../src/test/java/tests/ModulesTests.java#L106) | 7 | 7 общих сценариев: 7 PASS на manProd 1.40.21; подробности в modules-automation.md |
| [OnboardingGoalProgramTests](../src/test/java/tests/OnboardingGoalProgramTests.java#L34) | 1 | Общий сценарий iOS/Android через factories |
| [OnboardingPaywallTests](../src/test/java/tests/OnboardingPaywallTests.java#L29) | 5 | Общие 5 сценариев; отдельные StoreKit/Google Play реализации |
| [PdfGuideTests](../src/test/java/tests/PdfGuideTests.java#L29) | 1 | @Ignore: нет подготовленной PDF-фикстуры; Android реализации нет |
| [ProfileTests](../src/test/java/tests/ProfileTests.java#L22) | 3 | @Ignore: устаревший Program tab |
| [ProgramLoadingEvidenceUnitTests](../src/test/java/tests/ProgramLoadingEvidenceUnitTests.java#L22) | 12 | Unit/offline: не требует дублирования на мобильные платформы |
| [PushNotificationTests](../src/test/java/tests/PushNotificationTests.java#L29) | 1 | @Ignore: устаревший сценарий |
| [PushPermissionTests](../src/test/java/tests/PushPermissionTests.java#L29) | 2 | iOS-only: requireIOSPlatform; Android permission-flow отсутствует |
| [ReleaseSmokeTests](../src/test/java/tests/ReleaseSmokeTests.java#L35) | 5 | iOS-only: requireIOSPlatform; update/PDF parity отсутствует |
| [RestoringTestActionUnitTests](../src/test/java/tests/RestoringTestActionUnitTests.java#L15) | 8 | Unit/offline: не требует дублирования на мобильные платформы |
| [SearchTests](../src/test/java/tests/SearchTests.java#L24) | 4 | @Ignore: устаревший Feed/search |
| [SelectorMotionUnitTests](../src/test/java/tests/SelectorMotionUnitTests.java#L16) | 10 | Unit/offline: не требует дублирования на мобильные платформы |
| [SmokeTests](../src/test/java/tests/SmokeTests.java#L31) | 7 | iOS-only: requireIOSPlatform; AndroidSmokeTests покрывает только часть |
| [StoreKitPurchaseTests](../src/test/java/tests/StoreKitPurchaseTests.java#L30) | 2 | iOS StoreKit; Android отдельная покупка/restore в OnboardingPaywallTests, не буквальный дубль |
| [TestModelAccountProvisioningTests](../src/test/java/tests/TestModelAccountProvisioningTests.java#L22) | 1 | iOS setup opt-in; Android provisioning не реализован |
| [TestModelAutomationTests](../src/test/java/tests/TestModelAutomationTests.java#L92) | 8 | iOS-only через Assume и Simulator API; COA-8518 отдельно deferred |
| [TestModelLifecycleUnitTests](../src/test/java/tests/TestModelLifecycleUnitTests.java#L12) | 3 | Unit/offline: не требует дублирования на мобильные платформы |
| [TestModelMailFixtureUnitTests](../src/test/java/tests/TestModelMailFixtureUnitTests.java#L10) | 5 | Unit/offline: не требует дублирования на мобильные платформы |
| [TestModelPremiumInspectionTests](../src/test/java/tests/TestModelPremiumInspectionTests.java#L27) | 1 | iOS inspection opt-in; не регрессионное покрытие Android |
| [TestModelProgressUnitTests](../src/test/java/tests/TestModelProgressUnitTests.java#L7) | 12 | Unit/offline: не требует дублирования на мобильные платформы |
| [TestModelScopeUnitTests](../src/test/java/tests/TestModelScopeUnitTests.java#L18) | 6 | Unit/offline: не требует дублирования на мобильные платформы |
| [TodayProgramSelectorTests](../src/test/java/tests/TodayProgramSelectorTests.java#L79) | 6 | iOS-only через Assume; UI/analytics/motion Android не реализованы |

## Findings

| ID | Category | Severity | Priority | Confidence | Evidence | Risk/impact | Recommendation | Owner | Effort |
|---|---|---|---|---|---|---|---|---|---|
| A1 | False positive | High | P1 | High | CoachAuthorizationTests:28; CoachProfileTests:28; DailyPlanTests:45; ExploreTests:50; MaleBuildStartScreenTests:56 | 32 метода возвращаются без assertions на Android; ещё 1 в KegelExerciseTests:34 (итого 33 теста). Дополнительно ранний return есть в Explore @Before | Перенести проверки либо маркировать unsupported через platform assumption до запуска драйвера | Automation | 0.5–1 день для guards |
| A2 | Coverage | High | P1 | High | TestModelAutomationTests:357; TodayProgramSelectorTests:36; KegelExerciseTests:160 | Нет Android selector, customization, recovery, player и completion parity | Реализовать платформенные UI/evidence/fixtures и сохранить Jira traceability | Automation + QA | Несколько дней после подготовки данных |
| A3 | Locators | High | P1 | High | AndroidDailyPlanPageObject:106–145 | Ряд player/customization полей указывает на btnNavigateUp/tvTitle, а не на соответствующий control | Не переиспользовать эти заглушки для новых тестов; заменить по фактическому UI | Automation | 1–3 дня |
| A4 | Suite routing | Medium | P1 | High | suites/SmokeSuite:9; SmokeTests:32; CoreTestCase:105 | SmokeSuite на Android не запускает AndroidSmokeTests; cleanup Android исключён из общего logout | Явная Android smoke suite/CI job и Android cleanup с проверкой аккаунта | Automation/CI | 0.5–1 день |
| A5 | Legacy | Medium | P2 | High | suites/TestSuite; suites/TestSuiteIOS; tests/HW; @Ignore в Chat/Profile/Search | Название suite не доказывает платформу; legacy HW вызывает неподходящие factories | Отделить legacy/HW от продуктовых наборов и CI discovery | Automation | 0.5 дня |

## Missing and weak tests

- Authorization: invalid email, OTP cancel, resend; Android smoke покрывает только пустую почту и успешный вход.
- Profile: открытие с разных tabs, отмена удаления, Account settings, Support, FAQ, Terms; smoke покрывает общий экран/logout.
- Daily Plan/Kegel: уроки, player controls, звук/вибрация, completion/intensity, customization и отмена удаления.
- Selector: открытие/закрытие/scroll, смена и порядок программ, content/loading, analytics и motion.
- Test-model: COA-7947/7949/7950/8235/8511/8512/8517; COA-8518 отдельно deferred и не включается автоматически.
- Release: обновление с сохранением авторизации/прогресса, push permission transitions, PDF entitlement.
- Modules: completion/date/device-matrix gaps из исходного handoff сохраняются на обеих платформах.

## Test architecture assessment

Сценарии модулей должны оставаться одним `ModulesTests` с платформенным factory.
iOS CFURL cache/Simulator helpers не работают на Android: требуется самостоятельное
доказательство UID и состояния активности, а не обход предусловий. Наличие Android
capabilities или Page Object само по себе не доказывает работоспособность сценария.

## Release recommendation

**Insufficient evidence.** Полная Android parity отсутствует; отдельный успешный
module run не заменяет регрессию всех перечисленных потоков.

## Prioritized action plan

- Completed — Automation: Android modules перенесены и проверены (7/7); диагностика и отчёты сохранены. Начальный setup не подтверждён чистым повторным прогоном: PDF upsell закрыт только после диагностического restart.
- Short-term — Automation/CI: устранить ложные PASS, выделить Android smoke entry point и cleanup.
- Medium-term — QA/Automation: перенести selector, customization, Kegel и недостающие auth/profile сценарии с отдельными фикстурами.
- Long-term — QA/CI: добавить Android device matrix, update, permission/lifecycle и единый отчёт parity по Jira cases.
