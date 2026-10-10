# Запуск кейсов Kegel и SIAS(M)

`tests.KegelOctoberCasesTests` содержит пять независимых мобильных UI-сценариев
для iOS и Android. Используются существующие Appium, JUnit и Page Objects проекта.

| Кейс | Метод | Ожидаемая проверка |
| --- | --- | --- |
| COA-8096 | `testCoa8096GuideReturnsToSameWorkout` | Крестик закрывает Guide и возвращает к той же приостановленной тренировке и упражнению. |
| COA-8094 | `testCoa8094InfoDescribesCurrentExercise` | Информация соответствует текущему упражнению; закрытие сохраняет состояние плеера. |
| COA-8170 | `testCoa8170CancelKeepsExercise` | Cancel сохраняет количество, порядок и настройки упражнений Custom Kegel. |
| COA-8171 | `testCoa8171EmptyWorkoutDisablesStart` | Пустой список блокирует Start; Add exercise доступна, после добавления Start активна. |
| COA-7937 | `testCoa7937MarketingLinkAutoLogin` | SIAS(M)-ссылка из состояния без авторизации открывает авторизованный Today; email профиля соответствует ссылке. |

## Подготовка

Настройте Appium и устройство по [общей инструкции](local-setup.md).
Установите нужную сборку с английским интерфейсом и выберите соответствующий
файл через `-Dcoach.kegelUi.fixture=...`:

- Android prod 1.42.1: `src/test/resources/kegel/android-prod-1.42.1.properties`.
- iOS prod 1.13.33: `src/test/resources/kegel/ios-prod-1.13.33.properties`.
- iOS preprod 1.13.29: `src/test/resources/kegel/ios-preprod-1.13.29.properties`.

Файл задаёт локаторы и ожидаемый контент конкретной сборки. Метаданные версии
в нём не проверяют версию установленного приложения: это ответственность запускающего.
Для другой сборки сначала проверьте локаторы и ожидаемые тексты.

Передайте через окружение секреты каждого запускаемого кейса:

- `COACH_COA8094_EMAIL` / `COACH_COA8094_OTP` и
  `COACH_COA8096_EMAIL` / `COACH_COA8096_OTP`: аккаунт с доступной обычной тренировкой Kegel.
- `COACH_COA8170_EMAIL` / `COACH_COA8170_OTP` и
  `COACH_COA8171_EMAIL` / `COACH_COA8171_OTP`: выделенный аккаунт с Custom Kegel,
  на котором разрешено менять упражнения.
- `COACH_COA7937_LINK` / `COACH_COA7937_EMAIL`: SIAS(M)-ссылка и соответствующий email.
  OTP в этом сценарии не используется.

Четыре Kegel-кейса открывают тренировки через переключатель программ:
`Kegel Challenge` либо `Last Longer` → `Last Longer: Retain`.
Список Custom Kegel должен быть небольшим и целиком помещаться в доступной
области списка. Исходные упражнения, включая дубликаты, порядок и настройки,
восстанавливаются через UI в блоке очистки; также проверяется восстановление
программы, дня, прогресса и настройки Stretching.
На одном аккаунте запускайте эти тесты последовательно. Для параллельных
запусков и разных устройств используйте отдельные аккаунты.

Android-файл с `auth.flow=android-session` требует заранее войти через UI
в аккаунт кейса. Дополнительно передайте `COACH_KEGEL_SESSION_EMAIL`, совпадающий
с email кейса, и сохраняйте сессию через `android.noReset=true`, `android.fullReset=false`.
Это декларация выполненного входа: тест проверяет наличие авторизованного Today,
но не открывает Profile для проверки email. Четыре Kegel-кейса оставляют сессию
авторизованной после очистки. На каждом устройстве её нужно подготовить отдельно.

COA-7937 требует состояния без авторизации и открывает Profile для проверки email.
Ручной вход после открытия ссылки считается отклонением. Запускайте его отдельно
от четырёх Kegel-кейсов на подготовленной Android-сессии; перед следующим Kegel-тестом
снова подготовьте нужный аккаунт. Для Android предварительно завершите первый
запуск браузера. Appium-драйвер должен поддерживать `mobile: deepLink`.

## Одиночные запуски

В примерах настройки устройства (`ANDROID_UDID`, `ANDROID_DEVICE_NAME`,
`ANDROID_PLATFORM_VERSION` либо `IOS_UDID`, `IOS_DEVICE_NAME`, `IOS_PLATFORM_VERSION`)
и секреты уже переданы через окружение. APK и приложение iOS устанавливаются заранее.
Для другого кейса подставьте метод из таблицы.

```bash
mvn test -Dplatform=android \
  -Dandroid.appPackage=com.vamapps.thecoach \
  -Dandroid.noReset=true -Dandroid.fullReset=false \
  -Dcoach.kegelUi.fixture=src/test/resources/kegel/android-prod-1.42.1.properties \
  -Dtest=tests.KegelOctoberCasesTests#testCoa8094InfoDescribesCurrentExercise
```

```bash
mvn test -Dplatform=ios -Dios.bundleId=com.vamapps.The-Coach \
  -Dcoach.kegelUi.fixture=src/test/resources/kegel/ios-prod-1.13.33.properties \
  -Dtest=tests.KegelOctoberCasesTests#testCoa8096GuideReturnsToSameWorkout
```

Для COA-7937 используйте метод `testCoa7937MarketingLinkAutoLogin`.
iOS prod-файл допускает Quiz как исходный экран без авторизации;
маркетинговый тест при этом не проходит анкету и не выполняет ручной вход.
Общая точка входа пяти кейсов — `suites.KegelOctoberCasesSuite`.
Для Android соблюдайте описанное выше разделение сессий и одиночные запуски.

## Проверки без устройства

```bash
mvn -q -DskipTests test-compile
mvn -q -Dtest=tests.KegelOctoberDataUnitTests,tests.RestoringTestActionUnitTests test
```

Эти команды проверяют компиляцию, маскирование учётных данных и ссылки,
а также сохранение исходной ошибки при сбое восстановления. Работу UI и CI
проверяют отдельно на соответствующих устройствах.
