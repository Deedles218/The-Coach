# Импорт ручных тестов COA-9549 и COA-9551 в Xray Cloud

Файл: `coa-9549-9551-manual-import.csv` — 11 тестов, 33 шага. Кейсы применимы к iOS и Android, если соответствующая функция есть в выбранной сборке. В файл не включены Issue key существующих тестов: импорт должен создать новые Test issues.

В Jira откройте **Apps → Xray → Test Case Importer**, выберите CSV, кодировку UTF-8 и разделитель `;`. Сопоставьте `Issue Id` → Issue Id, `Issue type` → Issue Type, `Test type` → Test Type, `Test Summary` → Summary, `Description` → Description, `Project key` → Project Key, `Action` → Action, `Data` → Data, `Result` → Result, `Links` → Links / требования. Проверьте предпросмотр импорта: строки с одинаковым `Issue Id` должны стать шагами одного теста, а `Links` — связать тест с COA-9549 или COA-9551. Названия полей в мастере могут отличаться от названий колонок. Импорт в Jira этим файлом ещё не выполнялся.

Сборки из комментариев Jira: tooltip — iOS TestFlight Preprod для мужской версии #3435, для женской #3437; плеер — последние указанные iOS Preprod #3455 и #3457. Для Android пользователь передал `app-1.41.2-manProd-release.apk` (tooltip) и `app-1.41.3-manProd-release.apk` (плеер). Android APK не обозначены как Preprod, поэтому перед прогоном нужно проверить нужное окружение.

Уточнения и открытые вопросы:

- Для скорости принят пользовательский набор x0.5, x0.75, x1, x1.25, x1.5 и x2; x3 исключена, хотя ещё указана в тексте COA-9551.
- Сброс скорости при переходе к следующему уроку и после перезапуска приложения в задаче помечен как неуверенное требование; он не используется как критерий PASS/FAIL.
- Не заданы точная погрешность позиции после seek/resume и названия/параметры аналитических событий. Сравнивать следует фактическую позицию с выбранной без придумывания числового допуска, а события — по смыслу и наличию.
- Для переустановки использован тот же тестовый аккаунт с уже показанным первым tooltip и незавершённым пунктом Daily Plan. Если состояние плана не позволяет выполнить пункт после установки, потребуется отдельная подготовка данных; сам кейс не считать пройденным.

Источники: [COA-9549](https://the-coach.atlassian.net/browse/COA-9549), [COA-9551](https://the-coach.atlassian.net/browse/COA-9551), [Xray Cloud Test Case Importer](https://getxraydocs.atlassian.net/wiki/spaces/XRAYCLOUD/pages/44565062), [Xray CSV examples](https://getxraydocs.atlassian.net/wiki/spaces/XRAYCLOUD/pages/44565495).
