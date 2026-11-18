# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

## [1.3.0] - 2026-11-18

### Added
- **Moving to a new phone through Lingvale:** Lingvale can now gather the backups of all our apps into one file and load them back on another phone. Inkling hands it the same backup file the settings save, with the photos of the board, and takes one back the same way. Only Lingvale signed with our key is answered, loading happens only after you confirm it in Lingvale, and nothing leaves the phone.

<!-- tg
Переезд на новый телефон через Lingvale: деревня собирает копии всех наших приложений в один файл и возвращает их на новом телефоне.
Inkling отдаёт туда ту же копию, что сохраняют настройки, вместе с фото с доски.
-->

## [1.2.1] - 2026-11-18

- The bottom bar is now a floating shelf with drawn icons – the tower, the book, the paw and the chronicle – and the wand in a gold ring in the middle. A lit patch slides under the chosen place, and its picture grows and comes into colour.

<!-- tg
Новое нижнее меню: нарисованные иконки – башня, книга, лапа и свиток летописи, а посередине палочка в золотом кольце.
-->

## [1.2.0] - 2026-11-18

### Added
- **Moving to a new phone (#7):** the backup is now one zip with the whole tower and the photos of the board. «Send a backup» in the settings hands it to a messenger, the mail or a cloud drive, and on the new phone the welcome screen has «I have a backup of my tower» – the tower opens as it was, without the questions. Backups saved by earlier versions (a bare JSON) are still read.
- **A bell when the candle has burnt down (#5):** it rings on the candle's screen and with the notification when the app is closed. It follows the phone's notification volume, so a silent phone stays silent; it can be turned off in the settings, «Candle and break».

### Changed
- The home screen's row of quests is titled «This week's quests», with the next quest under it, instead of only a cut-off quest (#4).

### Fixed
- A broken or foreign backup file could crash the app or leave the tower half-replaced; now it changes nothing.

<!-- tg
Переезд на новый телефон: в настройках «Копия и новый телефон» → «Отправить копию», а на новом телефоне при первом запуске – «У меня есть копия башни». Фото с доски переезжают тоже.
Когда свеча догорела, звенит колокольчик. Он слушается громкости уведомлений и выключается в настройках свечи.
На главном экране у заданий недели появился заголовок.
-->

## [1.1.0] - 2026-11-16

### Added
- **Lingvale, the village of the pets from all our apps:** Inkling now has a small read-only bridge, so in Lingvale you can build the academy tower and its residents move in with their days. The bridge answers only Lingvale signed with our key; it changes nothing and nothing leaves the phone.

<!-- tg
Питомцы Inkling теперь могут жить в Lingvale – общей деревне наших приложений.
В Lingvale для них можно построить башню академии, и жильцы переедут туда сами вместе со своими днями.
Данные остаются в Inkling: деревня их только читает.
-->

## [1.0.2] - 2026-11-13

- Fixed: editing or deleting a study didn't recheck the week's quests and finds, so a study made longer (45 → 120 minutes) didn't finish a quest until the next study (#2).
- The week's quests show how far each one got: «1 из 2», «2 ч 10 мин из 5 ч».
- Tap a day in the home screen's week to see what was studied that day; tap a study to edit it (#2).

<!-- tg
Задания недели теперь показывают прогресс – видно, сколько уже засчитано.
Если исправить или удалить занятие, задания пересчитываются сразу.
Нажмите на столбик дня на главном экране – увидите, какие занятия в этот день были.
-->

## [1.0.1] - 2026-11-12

- Fixed: answering «were you at the lesson?» with «yes» crashed the app (#1). The «what was set?» sheet and the evening satchel no longer open inside the scrolling home screen.

<!-- tg
Исправлен вылет приложения, когда отвечаешь «Был(а)» на вопрос «Были на уроке?».
Спасибо, что сообщили!
-->

## [1.0.0] - 2026-11-12

The first release of Inkling.

- A study tracker set in an academy of magic, for school, college, university or studying on your own. Any subject goes into one of 14 schools of magic, guessed from its name.
- The candle: a timed lesson; put the phone face down and the circle closes. Distractions are counted, never punished; a break follows.
- The timetable with bells, odd and even weeks, terms and holidays; «were you there? what was set?» after each class, with a photo of the board; the evening satchel for school.
- Tasks with due dates, labs that are done, handed in and defended, coursework and theses in stages. Trials: exams, credits, tests, state exams with tickets, the sphinx drawing a ticket, practice papers and a predicted score.
- Marks in any scale – 5, 10, 12, points, A–F, pass/fail – with weights, the term mark and «two more 5s for a 5»; the record book, GPA and the grant.
- The grimoire: what you learned, recalled after 1, 3, 7, 21 and 60 days. «Teach the familiar» – the Feynman way, by text or voice. The mimic's chest of questions.
- 24 magical creatures drawn as pixel art, found by the way you study; six stats and a speciality each; they eat while you study and grow at night. Mana, weekly quests, festivals, the academy fair, your own wand.
- The chronicle: hours, the calendar, the star of schools, memory, progress, habits, a year in the academy; eleven kinds of share cards.

<!-- tg
Inkling – учёба в академии магии. Подходит для школы, колледжа, вуза и для самостоятельных занятий: любой ваш предмет попадёт в одну из школ магии.
Зажгите свечу, положите телефон экраном вниз и занимайтесь. Расписание, домашние задания, лабы и билеты к экзаменам тоже здесь.
Пока вы учитесь, в башне растут волшебные существа. Их 24, и каждое появляется за свою привычку.
-->
