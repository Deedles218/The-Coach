# iOS: карточки Daily Plan

Проверки находятся в `tests.DailyPlanTests` и используют существующие Appium,
JUnit, Page Objects и `RestoringTestAction`. Общая настройка устройства и
capabilities описана в [local-setup.md](local-setup.md).

## Ожидания

| Карточка | Результат удержания |
| --- | --- |
| Обычная незавершённая практика в DAILY PRACTICE | Меню Move to tomorrow / Remove from Daily Plan. Подтверждение удаления можно отменить; та же практика остаётся незавершённой. |
| Незавершённый обязательный Kegel | Уведомление, что обязательную практику нельзя перенести или удалить; GOT IT возвращает к той же карточке и дню. |
| Урок в DAILY LESSONS | Действия переноса и удаления отсутствуют. |

Завершённая карточка не подходит для положительного теста меню. Локатор
проверяет раздел и отсутствие видимого `dailyCompletedCheckmark*`, поэтому
одноимённый урок или карточка из TO CATCH-UP не заменяют нужную практику.
Обычное удержание остаётся 0,8 секунды.

## Аккаунты и состояние

Секреты передаются через существующий механизм `TestData`:

- `COACH_COA8506_EMAIL` / `COACH_COA8506_OTP`: аккаунт с доступной удаляемой
  практикой для `test05DailyPlanRemoveActionCanBeCancelled`.
- `COACH_KEGEL_PLAYER_EMAIL` / `COACH_KEGEL_PLAYER_OTP`: аккаунт с доступным
  незавершённым Kegel для `test06MandatoryKegelCannotBeCustomized`.
- `COACH_EXISTING_PROGRESS_EMAIL` / `COACH_EXISTING_PROGRESS_OTP`: аккаунт,
  чей текущий Daily Plan содержит урок, для `test07DailyLessonCannotBeCustomized`.

Укажите фактически проверенные программу, полный текст дня и название карточки:

| Параметр JVM | Содержание |
| --- | --- |
| `coach.dailyPlan.removablePracticeProgram` | Программа обычной практики. |
| `coach.dailyPlan.removablePracticeDay` | Полный текст дня, например `Day 4 of 60`. |
| `coach.dailyPlan.removablePracticeTitle` | Точное название доступной незавершённой обычной практики. |
| `coach.dailyPlan.corePracticeProgram` | Kegel-программа данной сборки. |
| `coach.dailyPlan.corePracticeDay` | Полный текст дня с незавершённым Kegel. |
| `coach.dailyPlan.corePracticeTitle` | Точное название обязательного Kegel. |

Номер дня и доступность карточки проверяются для конкретной фикстуры; пример
номера дня не является универсальным предусловием. Тесты 05 и 06 возвращают
исходные программу и день через `RestoringTestAction`. Они не подтверждают
удаление, не переносят практику и не завершают занятия.

Без `coach.dailyPlan.removablePracticeTitle` тест 05 сохраняет прежнюю
проверку первой практики на основном existing-progress аккаунте. Этот путь
по-прежнему требует фикстуру с удаляемой первой карточкой. Новые проверки
ограничения Kegel и урока не подменяют положительную проверку удаления.

## Запуск

После настройки секретов, устройства и параметров фикстур:

В проекте используется Surefire 2.12.4: селектор `Class#method1+method2`
не поддерживается и может завершиться ошибкой до открытия Appium-сессии.
Запускайте точные методы последовательно; ненулевой результат останавливает
цикл и сохраняет его ненулевой код, чтобы сначала разобрать причину падения.
Подоболочка не завершает интерактивный терминал. Значения аккаунтов передаются
через environment, а не через аргументы Maven.

```bash
(
for test_method in \
  test05DailyPlanRemoveActionCanBeCancelled \
  test06MandatoryKegelCannotBeCustomized \
  test07DailyLessonCannotBeCustomized
do
  mvn test -Dplatform=ios \
    "-Dtest=tests.DailyPlanTests#$test_method" \
    "-Dcoach.dailyPlan.removablePracticeProgram=$REMOVABLE_PROGRAM" \
    "-Dcoach.dailyPlan.removablePracticeDay=$REMOVABLE_DAY" \
    "-Dcoach.dailyPlan.removablePracticeTitle=$REMOVABLE_TITLE" \
    "-Dcoach.dailyPlan.corePracticeProgram=$KEGEL_PROGRAM" \
    "-Dcoach.dailyPlan.corePracticeDay=$KEGEL_DAY" \
    "-Dcoach.dailyPlan.corePracticeTitle=$KEGEL_TITLE" || exit "$?"
done
)
```

Проверка локаторов без устройства:

```bash
mvn test -Dtest=tests.KegelCardLocatorUnitTests
```

Изменения состояния после удержания прикладываются в Allure как
`Daily Practice hold result` и `Daily Lesson hold result` до восстановления UI.
