COA-9585 — конфиги Explore для женского приложения (iOS)
Ключ: abtest_explore_config_v7_for_her
Билды с UI-фиксами: Dev #3499 / Preprod #3500 (комментарий Jira от 2026-10-06).
Каждый файл 00–10 — отдельный полный JSON: открыть, выделить всё и вставить как значение ключа.
Женские поля root сохраняются: programsTitle="", toolsSubtitle="For you", metricsId="explore_for_her", tools=[].
Начать с 02, затем 03. 00 сохранить для отката.
coverage.json содержит условия и 24 ожидаемых targets; это НЕ remote config.
review-report.txt содержит evidence, ограничения и план проверки.

Статическая проверка из корня репозитория:
  node qa-fixtures/COA-9585/for-her/validate.mjs
Нужен только Node.js со стандартными модулями. Исходные пользовательские JSON
включены в sources/for-her.json и sources/for-him.json без изменения содержимого.
Пути в coverage.json разрешаются относительно папки валидатора: его можно
запускать из любого рабочего каталога без доступа к локальным вложениям Codex.
Вывод проверки печатается в stdout; результаты прогонов в набор не входят.
Валидация не публикует конфиги и не подтверждает прохождение iOS/UI-проверок.

00_original_for_her.json
Исходный женский конфиг для baseline и отката
  - Без изменений: 4 секции, 30 карточек; vertical и square. Сравнить UI до и после обновления сборки.

01_combined_source_configs.json
Объединение рабочих конфигов М и Ж под женским корнем
  - 8 секций, 51 карточка, все 6 layout. Исходные карточки, redirect, startScreen, product и required_subscription сохранены.
  - Мужские lesson_id, product_id и workout_id в женском приложении не подтверждены. Проверять отображение отдельно от доступности контента и покупки.

02_all_six_layouts_female_content.json
Все 6 шаблонов с женским контентом
  - 6 секций, 12 карточек. Шаблон определяется layout, а не названием секции.
  - challenge_card отображает description; coaching_session_card — subtitle и buttonTitle.
  - Переходы и startScreen берутся из рабочего женского конфига. Сравнить изображения, шрифты, размеры, скругления и отступы с макетом.

03_layout_redirect_matrix.json
Матрица: 6 layout × 4 потока контента
  - 24 комбинации: статья coach_lesson; практика coach_lesson + startScreen; coach_video + startScreen/rating_page; web_view.
  - Проверять точные lesson_id, video_url и url; для startScreen — name, goal, description, picture_url, затем запуск контента.
  - Для coaching проверить CTA и область карточки согласно макету. После возврата нажать другую карточку и убедиться в правильном переходе.
  - URL WebView взят из мужского рабочего конфига; его доступность и поведение в женском билде требуют проверки.

04_reordered_sections_and_cards.json
Перестановка секций и карточек
  - Порядок: QA Video Card → QA Coaching Session → QA Challenge → QA Square Ordinary → QA Square → QA Vertical.
  - В каждой секции карточки из 02 идут в обратном порядке. Контент и переходы не изменены.
  - Массив и order согласованы; тест не предполагает правило обработки конфликта сортировки.

05_text_and_image_changes.json
Изменение текстов и изображений
  - 6 layout: короткие/длинные title, Unicode, description, subtitle и CTA; imageURL заменены на другие URL из исходных конфигов.
  - Проверить на небольшом iPhone и при увеличенном системном шрифте. Переносы и обрезку оценивать по макету: лимит длины в COA-9585 не указан.
  - После 02 → 05 должны появиться новые изображения и тексты, без старого кеша.
  - startScreen.name оставлен исходным: название карточки отличается от названия контента. Проверять их отдельно.

06_ten_cards_per_layout.json
По 10 карточек каждого layout
  - 6 секций × 10 карточек = 60. Прокрутить каждую секцию; проверить первую, пятую и последнюю карточки, переход и возврат.
  - Нет пропавших/дублированных карточек, перепутанных изображений, CTA и переходов после повторного использования ячеек.
  - 10 — объём для проверки прокрутки, а не подтверждённый максимальный размер конфигурации.

07_single_section_single_card.json
Одна новая секция и одна карточка
  - От exerciseSections остаются только QA One Section и QA One Coaching Lesson.
  - coaching_session_card открывает женский урок lesson_why_some_women_dont_orgasm_explore; CTA Open.
  - Исходные женские поля верхнего уровня сохранены. Поведение пустого programsTitle не подменяется мужским Browse Programs.

08_no_exercise_sections.json
Ноль секций
  - Нет crash/зависания; после подтверждённого применения конфига нет старых exerciseSections.
  - Вид empty state и fallback в COA-9585 не описан: согласовать UI-ожидание с разработчиком.

09_array_order_vs_order_field.json
Конфликт порядка массива и order — исследовательская проверка
  - Массив: Vertical → Square → Square Ordinary → Challenge → Coaching Session → Video Card.
  - Сортировка по возрастанию order даст обратный порядок.
  - Приоритет array/order не описан в COA-9585. Зафиксировать факт и согласовать правило, не считать один из вариантов дефектом без контракта.

10_video_without_start_screen.json
Видео без startScreen — исследовательская проверка
  - 6 layout с исходным женским coach_video/rating_page, но без startScreen.
  - Согласовать необязательность startScreen и ожидаемый прямой запуск плеера. В рабочем женском конфиге выбранное видео имеет startScreen.
  - Нет crash и зависшего loading; отсутствие startScreen не должно приводить к открытию другого контента.
