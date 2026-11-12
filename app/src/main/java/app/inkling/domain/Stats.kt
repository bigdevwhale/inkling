package app.inkling.domain

import app.inkling.data.Attend
import app.inkling.data.Material
import app.inkling.data.Spell
import app.inkling.data.Study
import app.inkling.data.Subject
import app.inkling.data.Task
import java.time.LocalDate
import kotlin.math.max

/** A stretch of days to look at. */
data class Span(val from: Long, val to: Long, val label: String) {
    val days get() = (to - from + 1).toInt().coerceAtLeast(1)
    fun has(d: Long) = d in from..to
}

enum class SpanKind { WEEK, MONTH, TERM, YEAR, ALL }

object Spans {
    fun of(kind: SpanKind, back: Int, first: Long, today: Long, terms: List<LongRange> = emptyList()): Span = when (kind) {
        SpanKind.WEEK -> { val m = weekStart(today) - 7L * back; Span(m, m + 6, "${fmtDayShort(m)} – ${fmtDayShort(m + 6)}") }
        SpanKind.MONTH -> { val m = LocalDate.ofEpochDay(today).withDayOfMonth(1).minusMonths(back.toLong())
            Span(m.toEpochDay(), m.plusMonths(1).toEpochDay() - 1, "${monthName(m.monthValue).cap()} ${m.year}") }
        SpanKind.TERM -> { val past = terms.filter { it.first <= today }.sortedByDescending { it.first }
            val t = past.getOrNull(back) ?: (today - 90)..today
            Span(t.first, t.last, "${fmtDayShort(t.first)} – ${fmtDayShort(t.last)}") }
        SpanKind.YEAR -> { val y = LocalDate.ofEpochDay(today).year - back
            Span(LocalDate.of(y, 1, 1).toEpochDay(), LocalDate.of(y, 12, 31).toEpochDay(), "$y") }
        SpanKind.ALL -> Span(minOf(first, today), today, tr("За всё время", "All time"))
    }
}

data class SubjectSum(val subject: Subject?, val minutes: Int, val count: Int, val share: Float, val before: Int)

/** Numbers about study in a span. [all] is every study, for comparisons and history. */
class Stats(val all: List<Study>, val span: Span, val subjects: Map<Long, Subject>, val today: Long) {
    val studies = all.filter { span.has(it.day) }
    val minutes = studies.sumOf { it.minutes }
    val hours get() = minutes / 60.0
    val count = studies.size
    val days = studies.map { it.day }.toSet().size
    /** Days of the span that have passed. */
    val lived = (minOf(span.to, today) - span.from + 1).toInt().coerceAtLeast(1)
    val perDay = minutes / lived
    val avg = if (count > 0) minutes / count else 0
    val longest = studies.maxOfOrNull { it.minutes } ?: 0
    /** The span before, as far into it as this one has got. */
    val prev = Span(span.from - span.days, span.from - 1 - (span.days - lived).coerceAtLeast(0), "")
    val minutesBefore = all.filter { prev.has(it.day) }.sumOf { it.minutes }

    val candles = studies.filter { it.kind == Study.CANDLE }
    /** Share of candle minutes undisturbed: the focus quality. */
    val clean: Float = candles.let { c -> if (c.isEmpty()) 0f else c.count { it.away == 0 }.toFloat() / c.size }
    val distractions = candles.sumOf { it.away }
    val deep = studies.filter { it.minutes >= 50 }.sumOf { it.minutes }
    val short = studies.filter { it.minutes < 25 }.sumOf { it.minutes }

    fun bySubject(): List<SubjectSum> {
        val tot = max(1, minutes)
        val before = all.filter { prev.has(it.day) }.groupBy { it.subjectId }.mapValues { e -> e.value.sumOf { it.minutes } }
        return studies.groupBy { it.subjectId }.map { (id, g) -> val m = g.sumOf { it.minutes }; SubjectSum(subjects[id], m, g.size, m.toFloat() / tot, before[id] ?: 0) }
            .sortedByDescending { it.minutes }
    }
    /** Minutes by school of magic. */
    fun bySchool(): Map<String, Int> = studies.groupBy { subjects[it.subjectId]?.school ?: "own" }.mapValues { e -> e.value.sumOf { it.minutes } }

    val byDay: Map<Long, Int> = studies.groupBy { it.day }.mapValues { e -> e.value.sumOf { it.minutes } }
    val bestDay = byDay.maxByOrNull { it.value }

    /** Average minutes on each weekday (Mon..Sun). */
    fun weekdays(): List<Double> {
        val weeks = (span.days / 7.0).coerceAtLeast(1.0)
        val m = DoubleArray(7)
        studies.forEach { m[LocalDate.ofEpochDay(it.day).dayOfWeek.value - 1] += it.minutes.toDouble() }
        return m.map { it / weeks }
    }
    /** Minutes by the hour they started. */
    fun hours(): List<Int> { val h = IntArray(24); studies.forEach { h[hourOf(it.start)] += it.minutes }; return h.toList() }

    /** Lengths: up to 15, 15–30, 30–50, 50–90, longer. */
    fun lengths(): List<Int> { val b = IntArray(5); studies.forEach { val m = it.minutes; b[when { m <= 15 -> 0; m <= 30 -> 1; m <= 50 -> 2; m <= 90 -> 3; else -> 4 }]++ }; return b.toList() }

    /** Weeks in a row (up to this one) with any study. */
    fun streakWeeks(): Int {
        val weeks = all.map { weekStart(it.day) }.toSet()
        var w = weekStart(today); var n = 0
        if (w !in weeks) w -= 7
        while (w in weeks) { n++; w -= 7 }
        return n
    }

    /** Minutes per month for the last [n] months. */
    fun months(n: Int): List<Pair<String, Double>> {
        val m0 = LocalDate.ofEpochDay(today).withDayOfMonth(1)
        return (n - 1 downTo 0).map { k -> val m = m0.minusMonths(k.toLong()); val a = m.toEpochDay(); val b = m.plusMonths(1).toEpochDay()
            monthShort(m.monthValue) to all.filter { it.day in a until b }.sumOf { it.minutes } / 60.0 }
    }
    /** Hours per week for the last [n] weeks. */
    fun weeks(n: Int): List<Pair<String, Double>> {
        val w0 = weekStart(today)
        return (n - 1 downTo 0).map { k -> val a = w0 - 7L * k; fmtDayShort(a) to all.filter { it.day in a..(a + 6) }.sumOf { it.minutes } / 60.0 }
    }
}

/** The grimoire in numbers. */
class Memory(spells: List<Spell>, today: Long) {
    val total = spells.size
    val learned = spells.count { Engine.learned(it) }
    val due = spells.count { it.due <= today && !Engine.learned(it) }
    val asked = spells.sumOf { it.asked }
    val good = spells.sumOf { it.good }
    val rate: Float = if (asked > 0) good.toFloat() / asked else 0f
    /** How many spells stand on each recall step (0 new … 5 learned). */
    val steps = (0..Engine.STEPS.size).map { s -> spells.count { it.step == s } }
}

/** Tasks and attendance in a span. */
class Duty(tasks: List<Task>, attends: List<Attend>, span: Span, today: Long) {
    private val mine = tasks.filter { it.due in span.from..span.to || (it.due == 0L && it.created in span.from..span.to) }
    val done = mine.count { it.state >= 1 }
    val onTime = mine.count { it.state >= 1 && (it.due == 0L || it.doneDay in 1..it.due) }
    val late = mine.count { it.due in 1 until today && it.state == 0 }
    val open = tasks.count { it.open }
    private val att = attends.filter { span.has(it.day) && it.state != Attend.CANCELLED }
    val was = att.count { it.state == Attend.WAS }
    val missed = att.count { it.state == Attend.MISSED }
    val attendance: Float? = if (att.isEmpty()) null else was.toFloat() / att.size
}

/** A material's pace and the day it would end at that pace. */
fun Material.forecast(studies: List<Study>, today: Long): Long? {
    if (total <= 0 || done >= total) return null
    val mine = studies.filter { it.materialId == id && it.units > 0 }
    val from = mine.minOfOrNull { it.day } ?: created
    val days = (today - from + 1).coerceAtLeast(7)
    val units = mine.sumOf { it.units }.coerceAtLeast(done)
    if (units <= 0) return null
    val perDay = units.toDouble() / days
    return today + kotlin.math.ceil((total - done) / perDay).toLong()
}
