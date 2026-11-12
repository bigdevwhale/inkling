package app.inkling.domain

import app.inkling.data.Attend
import app.inkling.data.Question
import app.inkling.data.Settings
import app.inkling.data.Spell
import app.inkling.data.Study
import app.inkling.data.Task
import java.time.LocalDate
import java.time.MonthDay

/** What happened this week, for the quests. */
class WeekFacts(val monday: Long, studies: List<Study>, spells: List<Spell>, questions: List<Question>, tasks: List<Task>, attends: List<Attend>, s: Settings, schoolOf: (Long) -> String) {
    private val week = monday..(monday + 6)
    private val mine = studies.filter { it.day in week }
    val days = mine.map { it.day }.toSet().size
    val minutes = mine.sumOf { it.minutes }
    val candles = mine.count { it.kind == Study.CANDLE && it.minutes >= 20 }
    val clean = mine.count { it.kind == Study.CANDLE && it.away == 0 && it.minutes >= 20 }
    val schools = mine.map { schoolOf(it.subjectId) }.toSet().size
    val subjects = mine.map { it.subjectId }.toSet().size
    val teach = mine.count { it.kind == Study.TEACH }
    val newSpells = spells.count { it.created in week }
    val recalls = 0
    val questions = questions.count { it.created in week }
    val answers = questions.count { it.answered in week }
    val tasksDone = tasks.count { it.doneDay in week && it.state >= 1 }
    val early = tasks.count { it.doneDay in week && it.due > it.doneDay }
    val attended = attends.count { it.day in week && it.state == Attend.WAS }
    val long = mine.count { it.minutes >= 60 }
    val morning = mine.count { hourOf(it.start) < 12 }
    val packed = s.packed.count { it in week }
    var recalled = 0
}

data class Quest(val id: String, val ru: String, val en: String, val need: Int, val mana: Int, val only: String = "", val progress: (WeekFacts) -> Int) {
    val title get() = tr(ru, en)
}

object Quests {
    val all = listOf(
        Quest("days5", "Учиться 5 дней из 7", "Study on 5 days of 7", 5, 30) { it.days },
        Quest("hours5", "Набрать 5 часов за неделю", "Reach 5 hours this week", 300, 30) { it.minutes },
        Quest("candles5", "Дожечь 5 свечей", "Burn down 5 candles", 5, 25) { it.candles },
        Quest("clean3", "3 свечи без отвлечений", "3 candles without looking away", 3, 25) { it.clean },
        Quest("schools3", "Три разные школы магии", "Three different schools of magic", 3, 20) { it.schools },
        Quest("teach1", "Объяснить тему фамильяру", "Explain a topic to your familiar", 1, 20) { it.teach },
        Quest("spells5", "5 новых заклинаний в гримуаре", "5 new spells in the grimoire", 5, 20) { it.newSpells },
        Quest("recall10", "Вспомнить 10 заклинаний", "Recall 10 spells", 10, 25) { it.recalled },
        Quest("questions3", "3 вопроса в сундук мимика", "3 questions for the mimic's chest", 3, 15) { it.questions },
        Quest("answer1", "Найти ответ на вопрос из сундука", "Find the answer to a question in the chest", 1, 15) { it.answers },
        Quest("tasks3", "Сделать 3 задания", "Finish 3 tasks", 3, 25) { it.tasksDone },
        Quest("early2", "2 задания заранее, до срока", "2 tasks done before they're due", 2, 25) { it.early },
        Quest("attend8", "Отметить 8 занятий по расписанию", "Mark 8 classes you attended", 8, 20, "class") { it.attended },
        Quest("long2", "Два долгих занятия, от часа", "Two long sessions, an hour or more", 2, 20) { it.long },
        Quest("morning3", "3 занятия до полудня", "3 sessions before noon", 3, 15) { it.morning },
        Quest("satchel4", "Собрать портфель 4 вечера", "Pack the satchel on 4 evenings", 4, 20, "school") { it.packed },
    )
    val byId = all.associateBy { it.id }

    /** Three quests for a week, the same for the same week; those that need a timetable or a school only when they make sense. */
    fun deal(monday: Long, s: Settings, hasSlots: Boolean): List<String> {
        val r = Rand(monday.toInt() * 7919)
        val pool = all.filter { when (it.only) { "class" -> hasSlots; "school" -> s.school && s.satchel; else -> true } }.toMutableList()
        return (0 until 3).map { pool.removeAt(r.int(pool.size)).id }
    }
}

/** Furniture for the tower room, bought with mana. Placed by [x], [y] (parts of the picture) and drawn [w] wide. */
data class Item(val id: String, val ru: String, val en: String, val price: Int, val x: Float, val y: Float, val w: Float, val wall: Boolean) {
    val title get() = tr(ru, en)
}

object Shop {
    val all = listOf(
        Item("banner", "Знамя академии", "Academy banner", 200, .36f, .3f, .07f, true),
        Item("perch", "Жёрдочка", "Perch", 240, .27f, .93f, .07f, false),
        Item("painting", "Картина с замком", "Castle painting", 300, .52f, .33f, .1f, true),
        Item("chandelier", "Люстра со свечами", "Candle chandelier", 360, .58f, .1f, .13f, true),
        Item("grimoires", "Стопка гримуаров", "Stack of grimoires", 400, .955f, .995f, .06f, false),
        Item("crystal", "Хрустальный шар", "Crystal ball", 460, .74f, .9f, .055f, false),
        Item("hourglass", "Большие песочные часы", "Great hourglass", 520, .21f, .93f, .05f, false),
        Item("harp", "Золотая арфа", "Golden harp", 560, .33f, .93f, .06f, false),
        Item("cauldron", "Котёл", "Cauldron", 640, .64f, .97f, .08f, false),
        Item("shelf", "Книжный шкаф", "Bookcase", 720, .71f, .67f, .11f, false),
        Item("aquarium", "Аквариум с медузами", "Jellyfish aquarium", 800, .44f, .66f, .08f, false),
        Item("armillary", "Армиллярная сфера", "Armillary sphere", 880, .885f, .97f, .07f, false),
        Item("telescope", "Телескоп", "Telescope", 960, .14f, .97f, .09f, false),
        Item("skull", "Череп дракона", "Dragon skull", 1040, .79f, .5f, .085f, true),
        Item("alchemy", "Стол алхимика", "Alchemy table", 1120, .57f, .67f, .11f, false),
        Item("desk", "Письменный стол", "Writing desk", 1240, .3f, .67f, .12f, false),
    )
    val byId = all.associateBy { it.id }
    const val EGG = 600
}

/** The academy's festivals: a greeting, a mana bonus and, at the solstice, eggs at half price. */
enum class Feast(val from: MonthDay, val to: MonthDay, val ru: String, val en: String, val textRu: String, val textEn: String) {
    CANDLES(MonthDay.of(10, 30), MonthDay.of(11, 1), "Ночь Длинных Свечей", "Night of Long Candles",
        "Самая тёмная из осенних ночей: в академии зажигают все свечи. Сегодня мана за учёбу удваивается.", "The darkest of the autumn nights: every candle in the academy is lit. Mana for study is doubled today."),
    SOLSTICE(MonthDay.of(12, 20), MonthDay.of(12, 23), "Солнцеворот", "Solstice",
        "День начинает расти. На ярмарке академии яйца продают за полцены.", "The days start to grow. At the academy fair eggs are half price."),
    BALL(MonthDay.of(3, 20), MonthDay.of(3, 21), "Весенний бал", "Spring ball",
        "Равноденствие: в башнях танцуют, а мана за учёбу удваивается.", "The equinox: there is dancing in the towers and mana for study is doubled."),
    LAST(MonthDay.of(5, 24), MonthDay.of(5, 26), "Последний звонок", "Last bell",
        "Учебный год заканчивается. Загляните в табель – существа гордятся вами.", "The school year ends. Look at the report – your creatures are proud of you.");
    val title get() = tr(ru, en)
    val text get() = tr(textRu, textEn)

    companion object {
        fun on(day: Long): Feast? { val md = MonthDay.from(LocalDate.ofEpochDay(day)); return entries.firstOrNull { !md.isBefore(it.from) && !md.isAfter(it.to) } }
    }
}
