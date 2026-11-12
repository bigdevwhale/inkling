package app.inkling.domain

import app.inkling.data.Attend
import app.inkling.data.Exam
import app.inkling.data.Grade
import app.inkling.data.Settings
import app.inkling.data.Slot
import app.inkling.data.Task
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.ceil
import kotlin.math.roundToInt

/** «08:30» from minutes of the day. */
fun hm(min: Int): String = "%d:%02d".format(min / 60, min % 60)
fun parseHm(s: String): Int? { val p = s.trim().split(':', '.'); val h = p.getOrNull(0)?.toIntOrNull() ?: return null; val m = p.getOrNull(1)?.toIntOrNull() ?: 0
    return if (h in 0..23 && m in 0..59) h * 60 + m else null }
fun nowMin(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

/** The words of the mode: a lesson or a class, a quarter or a semester, a form or a year. */
object Words {
    fun cls(s: Settings) = if (s.school) tr("урок", "lesson") else tr("пара", "class")
    fun clsCap(s: Settings) = cls(s).cap()
    fun term(s: Settings) = if (s.school) tr("четверть", "term") else tr("семестр", "semester")
    fun year(s: Settings) = when (s.mode) { Settings.SCHOOL -> tr("${s.year} класс", "Year ${s.year}"); Settings.SELF -> tr("${s.year} год", "Year ${s.year}"); else -> tr("${s.year} курс", "Year ${s.year}") }
    fun slotKind(k: Int) = when (k) {
        Slot.LECTURE -> tr("лекция", "lecture"); Slot.SEMINAR -> tr("семинар", "seminar"); Slot.PRACTICE -> tr("практика", "practice"); Slot.LAB -> tr("лаба", "lab")
        Slot.CLUB -> tr("кружок", "club"); Slot.TUTOR -> tr("репетитор", "tutor"); else -> tr("урок", "lesson")
    }
    fun taskKind(k: String) = when (k) {
        Task.HW -> tr("Домашка", "Homework"); Task.LAB -> tr("Лабораторная", "Lab"); Task.ESSAY -> tr("Реферат, эссе", "Essay"); Task.TEST -> tr("Подготовка к контрольной", "Test prep")
        Task.PROJECT -> tr("Проект", "Project"); Task.COURSEWORK -> tr("Курсовая", "Coursework"); Task.THESIS -> tr("Диплом", "Thesis"); Task.READING -> tr("Прочитать", "Reading"); else -> tr("Другое", "Other")
    }
    fun taskState(t: Task) = when {
        t.big -> if (t.state >= 3) tr("защищено", "defended") else "${t.stage}/${t.stageList.size.coerceAtLeast(1)}"
        t.kind == Task.LAB -> when (t.state) { 0 -> tr("не начата", "to do"); 1 -> tr("сделана", "done"); 2 -> tr("сдана", "handed in"); else -> tr("защищена", "defended") }
        t.kind == Task.ESSAY || t.kind == Task.PROJECT -> when (t.state) { 0 -> tr("в работе", "to do"); 1 -> tr("готово", "done"); else -> tr("сдано", "handed in") }
        else -> if (t.state > 0) tr("сделано", "done") else tr("не сделано", "to do")
    }
    fun examKind(k: String) = when (k) {
        Exam.EXAM -> tr("Экзамен", "Exam"); Exam.CREDIT -> tr("Зачёт", "Pass/fail credit"); Exam.DIFF -> tr("Дифзачёт", "Graded credit"); Exam.TEST -> tr("Контрольная", "Test")
        Exam.VPR -> tr("ВПР", "National test"); Exam.OGE -> tr("ОГЭ", "OGE"); Exam.EGE -> tr("ЕГЭ", "EGE"); Exam.OLYMP -> tr("Олимпиада", "Olympiad"); Exam.CERT -> tr("Сертификат", "Certificate"); else -> tr("Испытание", "Trial")
    }
    fun unit(u: String, n: Int) = when (u) {
        "pages" -> count(n, "страница", "страницы", "страниц", "page", "pages"); "tasks" -> count(n, "задача", "задачи", "задач", "task", "tasks")
        "videos" -> count(n, "видео", "видео", "видео", "video", "videos"); "chapters" -> count(n, "глава", "главы", "глав", "chapter", "chapters")
        "minutes" -> count(n, "минута", "минуты", "минут", "minute", "minutes"); else -> count(n, "урок", "урока", "уроков", "lesson", "lessons")
    }
    fun unitName(u: String) = when (u) { "pages" -> tr("Страницы", "Pages"); "tasks" -> tr("Задачи", "Tasks"); "videos" -> tr("Видео", "Videos"); "chapters" -> tr("Главы", "Chapters"); "minutes" -> tr("Минуты", "Minutes"); else -> tr("Уроки", "Lessons") }
}

/** The timetable as it falls on real days: odd and even weeks, terms and holidays. */
object Timetable {
    /** 1 odd («числитель»), 2 even, 0 when weeks don't alternate. */
    fun parity(s: Settings, day: Long): Int {
        if (s.parityStart == 0L) return 0
        val w = Math.floorDiv(weekStart(day) - weekStart(s.parityStart), 7L)
        return if (Math.floorMod(w, 2L) == 0L) 1 else 2
    }
    fun weekday(day: Long) = LocalDate.ofEpochDay(day).dayOfWeek.value

    fun term(s: Settings, day: Long): LongRange? = terms(s).firstOrNull { day in it }
    fun terms(s: Settings): List<LongRange> = s.terms.mapNotNull { t -> t.split('-').takeIf { it.size == 2 }?.let { val a = it[0].toLongOrNull(); val b = it[1].toLongOrNull(); if (a != null && b != null) a..b else null } }.sortedBy { it.first }
    /** Holidays: days between terms (and outside them, when terms are set at all). */
    fun holiday(s: Settings, day: Long): Boolean { val t = terms(s); return t.isNotEmpty() && t.none { day in it } }

    /** The classes on a day, by time. */
    fun on(s: Settings, slots: List<Slot>, day: Long): List<Slot> {
        if (holiday(s, day)) return emptyList()
        val wd = weekday(day); val p = parity(s, day)
        return slots.filter { it.weekday == wd && (it.parity == 0 || p == 0 || it.parity == p) }.sortedBy { it.start }
    }

    /** The class going on at [min] of [day], and the next one today. */
    fun nowNext(s: Settings, slots: List<Slot>, day: Long, min: Int): Pair<Slot?, Slot?> {
        val l = on(s, slots, day)
        return l.firstOrNull { min in it.start until it.end } to l.firstOrNull { it.start > min }
    }

    /** The next day (after [day]) a subject has a class – where homework set today is due. */
    fun nextClass(s: Settings, slots: List<Slot>, subjectId: Long, day: Long): Long? =
        (1..21).map { day + it }.firstOrNull { d -> on(s, slots, d).any { it.subjectId == subjectId } }

    /** Classes that have ended today and haven't been marked yet. */
    fun unmarked(s: Settings, slots: List<Slot>, attends: List<Attend>, day: Long, min: Int): List<Slot> =
        on(s, slots, day).filter { it.end <= min && it.kind != Slot.TUTOR && attends.none { a -> a.slotId == it.id && a.day == day } }

    /** Default school terms for the academic year around [day] (Russian quarters), or two semesters. */
    fun defaultTerms(mode: String, day: Long): List<String> {
        val d = LocalDate.ofEpochDay(day)
        val y = if (d.monthValue >= 8) d.year else d.year - 1
        fun r(a: LocalDate, b: LocalDate) = "${a.toEpochDay()}-${b.toEpochDay()}"
        return if (mode == Settings.SCHOOL) listOf(
            r(LocalDate.of(y, 9, 1), LocalDate.of(y, 10, 24)), r(LocalDate.of(y, 11, 3), LocalDate.of(y, 12, 28)),
            r(LocalDate.of(y + 1, 1, 9), LocalDate.of(y + 1, 3, 20)), r(LocalDate.of(y + 1, 3, 30), LocalDate.of(y + 1, 5, 26)))
        else listOf(r(LocalDate.of(y, 9, 1), LocalDate.of(y + 1, 1, 31)), r(LocalDate.of(y + 1, 2, 9), LocalDate.of(y + 1, 6, 30)))
    }

    /** Typical school bells, 45-minute lessons; and university pairs of 90 minutes. */
    fun defaultBells(mode: String): List<String> = if (mode == Settings.SCHOOL)
        listOf("8:30-9:15", "9:25-10:10", "10:25-11:10", "11:25-12:10", "12:25-13:10", "13:20-14:05", "14:15-15:00", "15:10-15:55")
    else listOf("9:00-10:30", "10:40-12:10", "12:40-14:10", "14:20-15:50", "16:00-17:30", "17:40-19:10", "19:20-20:50")
}

/** Marks in the user's scale: averages, what a term will come to and what it takes to get the top mark. */
object Marks {
    fun top(scale: String) = when (scale) { "10" -> 10.0; "12" -> 12.0; "100" -> 100.0; "letter" -> 4.0; "pass" -> 1.0; else -> 5.0 }
    fun choices(scale: String): List<Pair<String, Double>> = when (scale) {
        "10" -> (10 downTo 1).map { "$it" to it.toDouble() }
        "12" -> (12 downTo 1).map { "$it" to it.toDouble() }
        "letter" -> listOf("A" to 4.0, "B" to 3.0, "C" to 2.0, "D" to 1.0, "F" to 0.0)
        "pass" -> listOf(tr("зачёт", "pass") to 1.0, tr("незачёт", "fail") to 0.0)
        "100" -> emptyList()
        else -> listOf("5" to 5.0, "4" to 4.0, "3" to 3.0, "2" to 2.0)
    }
    fun show(scale: String, v: Double): String = when (scale) {
        "letter" -> when { v >= 3.5 -> "A"; v >= 2.5 -> "B"; v >= 1.5 -> "C"; v >= .5 -> "D"; else -> "F" }
        "pass" -> if (v >= .5) tr("зачёт", "pass") else tr("незачёт", "fail")
        "100" -> fmt1(v)
        else -> if (v == v.roundToInt().toDouble()) "${v.roundToInt()}" else fmt1(v)
    }
    fun isTop(scale: String, v: Double, passes: List<Int>) = when (scale) { "100" -> v >= passes[2]; "pass" -> v >= 1; "letter" -> v >= 4; else -> v >= top(scale) - 1e-9 }
    /** 0..1 of the scale, for colours. */
    fun share(scale: String, v: Double) = when (scale) { "5" -> ((v - 2) / 3).coerceIn(0.0, 1.0); else -> (v / top(scale)).coerceIn(0.0, 1.0) }

    /** Weighted average of marks (not points). */
    fun avg(gs: List<Grade>): Double? { val w = gs.sumOf { it.weight }; return if (w <= 0) null else gs.sumOf { it.value * it.weight } / w }

    /** What the term comes to: an average rounded at .5, or points summed against the pass marks. */
    fun term(scale: String, gs: List<Grade>, passes: List<Int>): Double? = when (scale) {
        "100" -> if (gs.isEmpty()) null else gs.sumOf { it.value }
        else -> avg(gs)
    }
    fun termMark(scale: String, v: Double, passes: List<Int>): String = when (scale) {
        "100" -> when { v >= passes[2] -> "5"; v >= passes[1] -> "4"; v >= passes[0] -> "3"; else -> "2" }
        "5", "10", "12" -> "${(v + 1e-9).let { kotlin.math.floor(it + .5).toInt() }}"
        else -> show(scale, v)
    }

    /**
     * The nudge for a subject: how many top marks (weight 1) bring the average to the top mark,
     * and whether one middling mark would drop it a step. Null when nothing useful can be said.
     */
    fun nudge(scale: String, gs: List<Grade>, passes: List<Int>): String? {
        if (gs.isEmpty()) return null
        if (scale == "100") {
            val sum = gs.sumOf { it.value }
            val next = passes.firstOrNull { it > sum } ?: return tr("Набрано на «отлично»", "Enough for the top mark")
            val mark = when (next) { passes[0] -> "3"; passes[1] -> "4"; else -> "5" }
            return tr("До «$mark» не хватает ${fmt1(next - sum)} ${plural(ceil(next - sum).toLong(), "балла", "баллов", "баллов")}", "${fmt1(next - sum)} points short of a «$mark»")
        }
        if (scale != "5" && scale != "10" && scale != "12") return null
        val top = top(scale); val sum = gs.sumOf { it.value * it.weight }; val w = gs.sumOf { it.weight }
        val avg = sum / w
        val goal = top - .5
        if (avg >= goal) {
            val low = if (scale == "5") 3.0 else top - 3
            val after = (sum + low) / (w + 1)
            return if (after < goal) tr("Сейчас выходит ${top.roundToInt()}. Одна ${Marks.show(scale, low)} – и выйдет ${(top - 1).roundToInt()}", "A ${top.roundToInt()} for now. One ${Marks.show(scale, low)} and it drops to ${(top - 1).roundToInt()}")
                else tr("Выходит ${top.roundToInt()} с запасом", "A ${top.roundToInt()}, comfortably")
        }
        var n = 0
        while (n < 30 && (sum + top * n) / (w + n) < goal) n++
        if (n >= 30) return null
        val mark = top.roundToInt()
        return if (Lang.ru) "Чтобы выйти на $mark, нужно ещё ${count(n, if (mark == 5) "пятёрка" else "оценка «$mark»", if (mark == 5) "пятёрки" else "оценки «$mark»", if (mark == 5) "пятёрок" else "оценок «$mark»", "", "")}"
            else "${count(n, "", "", "", "more $mark", "more ${mark}s")} for a $mark"
    }

    /** Grade point average over finished trials, weighted by credits when they're set. */
    fun gpa(scale: String, exams: List<Exam>, credits: (Long) -> Double): Double? {
        val done = exams.filter { it.done && it.score >= 0 && it.kind in setOf(Exam.EXAM, Exam.DIFF) }
        if (done.isEmpty()) return null
        val w = done.sumOf { credits(it.subjectId).takeIf { c -> c > 0 } ?: 1.0 }
        return done.sumOf { it.score * (credits(it.subjectId).takeIf { c -> c > 0 } ?: 1.0) } / w
    }
}
