package app.inkling.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.inkling.data.Study
import app.inkling.domain.Duty
import app.inkling.domain.Marks
import app.inkling.domain.Memory
import app.inkling.domain.Schools
import app.inkling.domain.SpanKind
import app.inkling.domain.Spans
import app.inkling.domain.Stats
import app.inkling.domain.Timetable
import app.inkling.domain.Words
import app.inkling.domain.dayShort
import app.inkling.domain.dur
import app.inkling.domain.fmt1
import app.inkling.domain.fmtDayShort
import app.inkling.domain.tr
import app.inkling.ui.Data
import app.inkling.ui.Nav
import app.inkling.ui.Screen
import app.inkling.ui.art.Prop
import app.inkling.ui.components.Bars
import app.inkling.ui.components.ChipRow
import app.inkling.ui.components.Empty
import app.inkling.ui.components.Heat
import app.inkling.ui.components.Meter
import app.inkling.ui.components.Ornate
import app.inkling.ui.components.parchment
import app.inkling.ui.components.SceneHeader
import app.inkling.ui.components.Segments
import app.inkling.ui.components.Split
import app.inkling.ui.components.TileGrid
import app.inkling.ui.components.card
import app.inkling.ui.components.schoolColor
import app.inkling.ui.components.subjectColor
import app.inkling.ui.theme.C
import app.inkling.ui.theme.T
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The chronicle: a span to look at and five views of it. */
@Composable
fun StatsScreen(d: Data, nav: Nav) {
    val kinds = listOf(SpanKind.WEEK, SpanKind.MONTH, SpanKind.TERM, SpanKind.YEAR, SpanKind.ALL)
    var k by rememberSaveable { mutableIntStateOf(1) }
    var back by rememberSaveable { mutableIntStateOf(0) }
    var view by rememberSaveable { mutableIntStateOf(0) }
    val span = Spans.of(kinds[k], back, d.first, d.today, Timetable.terms(d.s))
    val st = Stats(d.studies, span, d.subById, d.today)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        SceneHeader("banner_chronicle", tr("Летопись", "The chronicle"), tr("${dur(d.studies.sumOf { it.minutes } * 60)} учёбы с ${fmtDayShort(d.first)}", "${dur(d.studies.sumOf { it.minutes } * 60)} of study since ${fmtDayShort(d.first)}"), Modifier.padding(top = 10.dp)) {
            Box(Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(50)).background(Color(0x55100A20)).clickable { nav.push(Screen.Wrapped(java.time.LocalDate.ofEpochDay(d.today).year)) }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Prop("scroll", 24.dp); Spacer(Modifier.width(4.dp)); Text(tr("Год", "Year"), style = T.smallB.copy(color = Color(0xFFFFF4DC))) } }
        }
        ChipRow(listOf(tr("Неделя", "Week"), tr("Месяц", "Month"), Words.term(d.s).replaceFirstChar { it.uppercase() }, tr("Год", "Year"), tr("Всё", "All")), k, Modifier.padding(top = 10.dp)) { k = it; back = 0 }
        if (kinds[k] != SpanKind.ALL) Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", style = T.h2.copy(color = C.mute), modifier = Modifier.clickable { back++ }.padding(horizontal = 10.dp))
            Text(span.label, style = T.bodyB, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("›", style = T.h2.copy(color = if (back > 0) C.mute else C.line), modifier = Modifier.clickable { if (back > 0) back-- }.padding(horizontal = 10.dp))
        }
        val views = if (d.s.self) listOf(tr("Обзор", "Overview"), tr("Предметы", "Subjects"), tr("Память", "Memory"), tr("Привычки", "Habits"))
            else listOf(tr("Обзор", "Overview"), tr("Предметы", "Subjects"), tr("Память", "Memory"), tr("Успехи", "Progress"), tr("Привычки", "Habits"))
        Segments(views, view.coerceAtMost(views.lastIndex), Modifier.padding(top = 10.dp)) { view = it }
        val which = if (d.s.self && view >= 3) view + 1 else view
        when (which) {
            0 -> Overview(st, d, nav)
            1 -> Subjects(st, d, nav)
            2 -> MemoryView(d)
            3 -> Progress(st, d)
            else -> Habits(st)
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun Overview(st: Stats, d: Data, nav: Nav) {
    if (st.count == 0) { Empty(tr("В этот срок занятий нет. Зажгите свечу – и летопись начнётся.", "No study in this span. Light a candle and the chronicle begins.")); return }
    Spacer(Modifier.height(12.dp))
    val delta = if (st.minutesBefore > 0) (st.minutes - st.minutesBefore) * 100 / st.minutesBefore else null
    TileGrid(parch = true, items = listOf(
        Triple(dur(st.minutes * 60), tr("учёбы", "of study"), delta?.let { (if (it >= 0) "+" else "") + "$it%" }),
        Triple("${st.days}", tr("дней с учёбой", "days with study"), null),
        Triple("${(st.clean * 100).toInt()}%", tr("без отвлечений", "undisturbed"), null),
        Triple("${st.streakWeeks()}", tr("недель подряд", "weeks in a row"), null),
    ))
    Ornate(tr("Часы по неделям", "Hours by week"))
    val wk = st.weeks(12)
    Bars(wk, color = C.acc, hi = wk.lastIndex)
    Ornate(tr("Календарь", "Calendar"))
    Box(Modifier.fillMaxWidth().parchment(16.dp).padding(12.dp)) { minOf(st.span.to, d.today).let { to -> Heat(st.all.groupBy { it.day }.mapValues { e -> e.value.sumOf { it.minutes } }, minOf(st.span.from, to - 7 * 20).coerceAtLeast(d.first - 7), to) } }
    st.bestDay?.let { (day, m) -> Text(tr("Лучший день – ${fmtDayShort(day)}, ${dur(m * 60)}", "Best day: ${fmtDayShort(day)}, ${dur(m * 60)}"), style = T.small, modifier = Modifier.padding(top = 8.dp)) }
}

@Composable
private fun Subjects(st: Stats, d: Data, nav: Nav) {
    val list = st.bySubject()
    if (list.isEmpty()) { Empty(tr("Пока пусто", "Nothing yet")); return }
    Ornate(tr("Звезда школ", "The star of schools"))
    val bySchool = st.bySchool()
    Box(Modifier.fillMaxWidth().parchment(18.dp).padding(12.dp), contentAlignment = Alignment.Center) { SchoolStar(bySchool, Modifier.size(240.dp)) }
        Ornate(tr("Предметы", "Subjects"))
    list.forEach { s ->
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).parchment(14.dp).clickable { s.subject?.let { nav.push(Screen.SubjectInfo(it.id)) } }.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.subject?.name ?: "–", style = T.bodyB, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(dur(s.minutes * 60), style = T.numS)
            }
            Meter(s.share, Modifier.padding(top = 6.dp), color = subjectColor(s.subject), h = 6.dp)
        }
    }
    // hours vs credits: where the work goes against the weight
    val withCredits = d.active.filter { it.credits > 0 }
    if (withCredits.size >= 2) {
        Ornate(tr("Часы и кредиты", "Hours and credits"))
        val totC = withCredits.sumOf { it.credits }; val totM = list.filter { it.subject in withCredits }.sumOf { it.minutes }.coerceAtLeast(1)
        withCredits.forEach { s -> val m = list.firstOrNull { it.subject?.id == s.id }?.minutes ?: 0
            val ratio = (m.toDouble() / totM) / (s.credits / totC)
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(s.name, style = T.small.copy(color = C.ink), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(when { ratio > 1.4 -> tr("тонете", "drowning"); ratio < .6 -> tr("недорабатываете", "under-worked"); else -> tr("в меру", "balanced") },
                    style = T.smallB.copy(color = when { ratio > 1.4 -> C.gold; ratio < .6 -> C.bad; else -> C.ok }))
            }
        }
    }
}

/** A radar of the schools of magic studied (the ones with any time), up to eight. */
@Composable
fun SchoolStar(by: Map<String, Int>, modifier: Modifier = Modifier) {
    val keys = (by.keys.sortedByDescending { by[it] } + Schools.all.map { it.id }).distinct().take(maxOf(5, minOf(8, by.size)))
    val max = (by.values.maxOrNull() ?: 1).coerceAtLeast(1)
    val ink = C.line; val acc = C.acc
    val colors = keys.map { schoolColor(it) }
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2); val r = size.minDimension / 2 * .78f
        fun pt(i: Int, f: Float): Offset { val a = -PI / 2 + 2 * PI * i / keys.size; return Offset(c.x + (r * f * cos(a)).toFloat(), c.y + (r * f * sin(a)).toFloat()) }
        for (ring in 1..3) { val p = Path(); keys.indices.forEach { i -> val q = pt(i, ring / 3f); if (i == 0) p.moveTo(q.x, q.y) else p.lineTo(q.x, q.y) }; p.close(); drawPath(p, ink, style = Stroke(1.dp.toPx())) }
        keys.indices.forEach { i -> drawLine(ink, c, pt(i, 1f), 1.dp.toPx()) }
        val p = Path(); keys.forEachIndexed { i, k -> val q = pt(i, .08f + .92f * (by[k] ?: 0) / max.toFloat()); if (i == 0) p.moveTo(q.x, q.y) else p.lineTo(q.x, q.y) }; p.close()
        drawPath(p, acc.copy(alpha = .25f)); drawPath(p, acc, style = Stroke(2.dp.toPx()))
        keys.forEachIndexed { i, _ -> drawCircle(colors[i], 6.dp.toPx(), pt(i, 1.12f)) }
    }
}

@Composable
private fun MemoryView(d: Data) {
    val m = Memory(d.spells, d.today)
    Spacer(Modifier.height(12.dp))
    TileGrid(parch = true, items = listOf(
        Triple("${m.total}", tr("заклинаний", "spells"), null),
        Triple("${m.learned}", tr("выучено", "learned"), null),
        Triple("${(m.rate * 100).toInt()}%", tr("вспоминаете", "recalled"), null),
        Triple("${m.due}", tr("ждут сфинкса", "await the sphinx"), null),
    ))
    Ornate(tr("Кривая памяти", "The memory curve"))
    Text(tr("от новых – к выученным", "from new to learned"), style = T.tiny, modifier = Modifier.padding(bottom = 8.dp))
    Bars(m.steps.mapIndexed { i, n -> (if (i == 0) tr("нов", "new") else if (i == m.steps.lastIndex) "✦" else "${app.inkling.domain.Engine.STEPS[i]}") to n.toDouble() }, color = C.acc, fmtv = { "${it.toInt()}" })
    val teach = d.studies.filter { it.kind == Study.TEACH }
    Ornate(tr("Объяснения фамильяру", "Explanations to the familiar"))
    Text(if (teach.isEmpty()) tr("Ещё не объясняли. Попробуйте – это лучший способ понять тему.", "None yet. Try it – it's the best way to understand a topic.")
        else tr("${teach.size} раз · ${dur(teach.sumOf { it.minutes } * 60)}", "${teach.size} times · ${dur(teach.sumOf { it.minutes } * 60)}"), style = T.body)
    val ahead = d.upcoming.filter { e -> d.tickets.any { it.examId == e.id } }
    if (ahead.isNotEmpty()) { Ornate(tr("Готовность к испытаниям", "Readiness for trials"))
        ahead.forEach { e -> val tk = d.tickets.filter { it.examId == e.id }; val f = tk.count { it.level >= 2 }.toFloat() / tk.size
            Column(Modifier.padding(vertical = 5.dp)) {
                Row { Text(e.title.ifBlank { d.subById[e.subjectId]?.name.orEmpty() }, style = T.smallB, modifier = Modifier.weight(1f)); Text("${(f * 100).toInt()}% · ${fmtDayShort(e.day)}", style = T.tiny) }
                Meter(f, Modifier.padding(top = 4.dp), color = if (f > .7f) C.ok else if (f > .4f) C.gold else C.bad) } } }
}

@Composable
private fun Progress(st: Stats, d: Data) {
    val duty = Duty(d.tasks, d.attends, st.span, d.today)
    Spacer(Modifier.height(12.dp))
    TileGrid(parch = true, items = listOf(
        Triple("${duty.done}", tr("заданий сделано", "tasks done"), null),
        Triple("${duty.onTime}", tr("вовремя", "on time"), null),
        Triple("${duty.late}", tr("просрочено", "overdue"), null),
        Triple(duty.attendance?.let { "${(it * 100).toInt()}%" } ?: "–", tr("посещаемость", "attendance"), null),
    ))
    val gs = d.grades.filter { it.examId == 0L && st.span.has(it.day) }
    if (gs.isNotEmpty()) {
        Ornate(if (d.s.scale == "100") tr("Баллы по предметам", "Points by subject") else tr("Средний по предметам", "Average by subject"))
        val rows = gs.groupBy { it.subjectId }.mapNotNull { (id, l) -> d.subById[id]?.let { it to Marks.term(d.s.scale, l, d.s.passes)!! } }.sortedByDescending { it.second }
        rows.forEach { (s, v) -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(s.name, style = T.small.copy(color = C.ink), modifier = Modifier.width(120.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Meter(if (d.s.scale == "100") (v / 100).toFloat() else Marks.share(d.s.scale, v).toFloat(), Modifier.weight(1f), color = subjectColor(s), h = 8.dp)
            Text(fmt1(v), style = T.smallB, modifier = Modifier.width(44.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End) } }
        Text(tr("Высших оценок: ${gs.count { Marks.isTop(d.s.scale, it.value, d.s.passes) }} из ${gs.size}", "Top marks: ${gs.count { Marks.isTop(d.s.scale, it.value, d.s.passes) }} of ${gs.size}"), style = T.small, modifier = Modifier.padding(top = 6.dp))
    }
    Marks.gpa(d.s.scale, d.exams) { id -> d.subById[id]?.credits ?: 0.0 }?.let { g -> Ornate(tr("Средний балл испытаний", "Trials average")); Text(fmt1(g), style = T.big.copy(color = C.acc)) }
}

@Composable
private fun Habits(st: Stats) {
    if (st.count == 0) { Empty(tr("Пока пусто", "Nothing yet")); return }
    Ornate(tr("По дням недели", "By weekday"))
    Bars(st.weekdays().mapIndexed { i, v -> dayShort(i) to v / 60 }, color = C.acc, fmtv = { fmt1(it) })
    Ornate(tr("По часам", "By hour"))
    val h = st.hours()
    Bars((6..23).map { "$it" to h[it] / 60.0 } + listOf("0–5" to (0..5).sumOf { h[it] } / 60.0), color = C.gold)
    val morning = (5..11).sumOf { h[it] }.toDouble(); val evening = (18..23).sumOf { h[it] } + (0..4).sumOf { h[it] }.toDouble()
    Spacer(Modifier.height(10.dp))
    Split(morning, evening, tr("утро", "morning"), tr("вечер", "evening"), C.gold, C.acc)
    Ornate(tr("Длина занятий", "Session length"))
    val l = st.lengths()
    Bars(listOf(tr("≤15", "≤15") to l[0].toDouble(), "15–30" to l[1].toDouble(), "30–50" to l[2].toDouble(), "50–90" to l[3].toDouble(), "90+" to l[4].toDouble()), color = C.ok, fmtv = { "${it.toInt()}" })
    Spacer(Modifier.height(10.dp))
    Split(st.deep.toDouble(), st.short.toDouble(), tr("глубокие (50+ мин)", "deep (50+ min)"), tr("короткие", "short"), C.acc, C.mana)
    Ornate(tr("Отвлечения", "Distractions"))
    Text(tr("За срок пламя дрогнуло ${st.distractions} раз. Свечей без единого отвлечения – ${(st.clean * 100).toInt()}%.", "The flame flickered ${st.distractions} times. Candles with no distraction at all: ${(st.clean * 100).toInt()}%."), style = T.body)
}

