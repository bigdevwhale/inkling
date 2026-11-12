package app.inkling.ui.academy

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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.inkling.container
import app.inkling.data.Attend
import app.inkling.data.Exam
import app.inkling.data.Slot
import app.inkling.data.Subject
import app.inkling.data.Task
import app.inkling.domain.Duty
import app.inkling.domain.Marks
import app.inkling.domain.Schools
import app.inkling.domain.Span
import app.inkling.domain.Timetable
import app.inkling.domain.Words
import app.inkling.domain.dayShort
import app.inkling.domain.dur
import app.inkling.domain.fmt1
import app.inkling.domain.fmtDayShort
import app.inkling.domain.hm
import app.inkling.domain.inDays
import app.inkling.domain.parseHm
import app.inkling.domain.relDay
import app.inkling.domain.tr
import app.inkling.domain.weekStart
import app.inkling.domain.weekday
import app.inkling.ui.Data
import app.inkling.ui.Nav
import app.inkling.ui.Screen
import app.inkling.ui.art.Prop
import app.inkling.ui.components.Btn
import app.inkling.ui.components.Chip
import app.inkling.ui.components.Empty
import app.inkling.ui.components.Field
import app.inkling.ui.components.Ghost
import app.inkling.ui.components.Icon
import app.inkling.ui.components.Icons
import app.inkling.ui.components.Meter
import app.inkling.ui.components.Page
import app.inkling.ui.components.Row2
import app.inkling.ui.components.SchoolMark
import app.inkling.ui.components.Ornate
import app.inkling.ui.components.parchment
import app.inkling.ui.components.SceneHeader
import app.inkling.ui.components.Segments
import app.inkling.ui.components.Sheet
import app.inkling.ui.components.SubjectPicker
import app.inkling.ui.components.TimeChip
import app.inkling.ui.components.card
import app.inkling.ui.components.levelColor
import app.inkling.ui.components.schoolColor
import app.inkling.ui.components.subjectColor
import app.inkling.ui.magic.GradeSheet
import app.inkling.ui.theme.C
import app.inkling.ui.theme.T

/** The academy tab: the day's classes, tasks, trials and subjects. */
@Composable
fun AcademyScreen(d: Data, nav: Nav) {
    val tabs = if (d.s.self) listOf(tr("Задания", "Tasks"), tr("Испытания", "Trials"), tr("Предметы", "Subjects"))
        else listOf(tr("Неделя", "Week"), tr("Задания", "Tasks"), tr("Испытания", "Trials"), tr("Предметы", "Subjects"))
    var sel by rememberSaveable { mutableIntStateOf(0) }
    val which = if (d.s.self) sel + 1 else sel
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        SceneHeader("banner_academy", if (d.s.self) tr("Учёба", "Study") else tr("Академия", "Academy"),
            Words.year(d.s) + (Timetable.term(d.s, d.today)?.let { " · ${Words.term(d.s)} ${tr("до", "until")} ${fmtDayShort(it.last)}" } ?: if (d.s.terms.isNotEmpty()) " · " + tr("каникулы", "holidays") else ""),
            Modifier.padding(top = 10.dp))
        Segments(tabs, sel, Modifier.padding(top = 12.dp, bottom = 6.dp)) { sel = it }
        when (which) {
            0 -> WeekTab(d, nav)
            1 -> TasksTab(d, nav)
            2 -> TrialsTab(d, nav)
            else -> SubjectsTab(d, nav)
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ---------------- the week ----------------

@Composable
private fun WeekTab(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var day by rememberSaveable { mutableLongStateOf(d.today) }
    val monday = weekStart(day)
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until 7) { val dd = monday + i; val on = dd == day; val has = Timetable.on(d.s, d.slots, dd).isNotEmpty()
            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) C.acc else C.card).clickable { day = dd }.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(dayShort(i), style = T.tiny.copy(color = if (on) C.accInk else C.mute))
                Text("${java.time.LocalDate.ofEpochDay(dd).dayOfMonth}", style = T.bodyB.copy(color = if (on) C.accInk else if (dd == d.today) C.acc else C.ink))
                Box(Modifier.size(4.dp).clip(CircleShape).background(if (has) (if (on) C.accInk else C.acc) else androidx.compose.ui.graphics.Color.Transparent))
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(relDay(day, d.today).replaceFirstChar { it.uppercase() } + (Timetable.parity(d.s, day).takeIf { it > 0 }?.let { " · " + if (it == 1) tr("числитель", "odd week") else tr("знаменатель", "even week") } ?: ""),
            style = T.bodyB, modifier = Modifier.weight(1f))
        Text("‹", style = T.h2.copy(color = C.mute), modifier = Modifier.clickable { day -= 7 }.padding(horizontal = 10.dp))
        Text("›", style = T.h2.copy(color = C.mute), modifier = Modifier.clickable { day += 7 }.padding(horizontal = 10.dp))
    }
    val list = Timetable.on(d.s, d.slots, day)
    if (d.slots.isEmpty()) {
        Empty(tr("Расписания пока нет. Заведите неделю один раз – и башня будет знать, где вы и что задали.", "No timetable yet. Set up the week once and the tower will know where you are and what was set."))
        Btn(tr("Заполнить расписание", "Fill in the timetable"), Modifier.fillMaxWidth()) { nav.push(Screen.Timetable) }
        return
    }
    if (Timetable.holiday(d.s, day)) Empty(tr("Каникулы – отдыхайте, ничего не сгорает.", "Holidays – rest, nothing is lost."))
    else if (list.isEmpty()) Empty(tr("Свободный день", "A free day"))
    val nowMin = app.inkling.domain.nowMin()
    list.forEach { sl ->
        val sub = d.subById[sl.subjectId]
        val att = d.attends.firstOrNull { it.slotId == sl.id && it.day == day }
        val live = day == d.today && nowMin in sl.start until sl.end
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).then(if (live) Modifier.card(16.dp, C.accSoft) else Modifier.parchment(16.dp)).clickable { if (sub != null) nav.push(Screen.SubjectInfo(sub.id)) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(50.dp)) { Text(hm(sl.start), style = T.smallB); Text(hm(sl.end), style = T.tiny) }
            Box(Modifier.size(4.dp, 36.dp).clip(RoundedCornerShape(50)).background(subjectColor(sub)))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(sub?.name ?: "–", style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(Words.slotKind(sl.kind).takeIf { sl.kind != Slot.LESSON }, sl.room.takeIf { it.isNotBlank() }?.let { tr("ауд. $it", "room $it") }, sub?.teacher?.takeIf { it.isNotBlank() }).joinToString(" · "), style = T.tiny)
                val hw = d.openTasks.filter { it.subjectId == sl.subjectId && it.due == day }
                if (hw.isNotEmpty()) Text(tr("задано: ", "set: ") + hw.joinToString("; ") { it.title }, style = T.tiny.copy(color = C.acc), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (day <= d.today && (day < d.today || sl.end <= nowMin) && sl.kind != Slot.TUTOR) {
                val (txt, col) = when (att?.state) { Attend.WAS -> "✓" to C.ok; Attend.MISSED -> "✗" to C.bad; Attend.CANCELLED -> "–" to C.mute; else -> "?" to C.gold }
                Box(Modifier.size(32.dp).clip(CircleShape).background(col.copy(alpha = .15f)).clickable {
                    val next = when (att?.state) { null -> Attend.WAS; Attend.WAS -> Attend.MISSED; Attend.MISSED -> Attend.CANCELLED; else -> Attend.WAS }
                    c.write { attend(sl, day, next) } }, contentAlignment = Alignment.Center) { Text(txt, style = T.bodyB.copy(color = col)) }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Ghost(tr("Изменить расписание", "Edit the timetable"), Modifier.fillMaxWidth(), small = true) { nav.push(Screen.Timetable) }
    // attendance this term
    val term = Timetable.term(d.s, d.today) ?: (d.today - 30)..d.today
    val duty = Duty(d.tasks, d.attends, Span(term.first, term.last, ""), d.today)
    duty.attendance?.let { a ->
        Ornate(tr("Посещаемость за ${Words.term(d.s)}", "Attendance this ${Words.term(d.s)}"))
        Row(verticalAlignment = Alignment.CenterVertically) { Text("${(a * 100).toInt()}%", style = T.num); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { Meter(a, color = if (a >= .9f) C.ok else if (a >= .7f) C.gold else C.bad); Text(tr("был(а) ${duty.was} · пропусков ${duty.missed}", "${duty.was} attended · ${duty.missed} missed"), style = T.tiny, modifier = Modifier.padding(top = 4.dp)) } }
    }
}

// ---------------- tasks ----------------

@Composable
private fun TasksTab(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val open = d.openTasks
    val big = open.filter { it.big }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Btn(tr("Задание", "Task"), Modifier.weight(1f), small = true) { nav.push(Screen.TaskInfo(0)) }
        if (!d.s.school) Ghost(if (d.s.self) tr("Проект", "Project") else tr("Курсовая, диплом", "Coursework, thesis"), Modifier.weight(1f), small = true) { nav.push(Screen.TaskInfo(0, kind = if (d.s.self) Task.PROJECT else Task.COURSEWORK)) }
    }
    if (big.isNotEmpty()) { Ornate(tr("Великое делание", "The great work"))
        big.forEach { t -> BigRow(t, d) { nav.push(Screen.TaskInfo(t.id)) } } }
    val small = open.filter { !it.big }
    val groups = listOf(
        tr("Просрочено", "Overdue") to small.filter { it.due in 1 until d.today },
        tr("Сегодня и завтра", "Today and tomorrow") to small.filter { it.due in d.today..(d.today + 1) },
        tr("На неделе", "This week") to small.filter { it.due in (d.today + 2)..(d.today + 7) },
        tr("Позже", "Later") to small.filter { it.due > d.today + 7 },
        tr("Без срока", "No date") to small.filter { it.due == 0L },
    )
    if (small.isEmpty() && big.isEmpty()) Empty(tr("Всё сделано. Сова отдыхает.", "All done. The owl is resting."))
    groups.forEach { (title, l) -> if (l.isNotEmpty()) { Ornate(title); l.forEach { t -> TaskRow(t, d, { nav.push(Screen.TaskInfo(t.id)) }) { c.write { setTaskState(t, (t.state + 1).coerceAtMost(t.last)) } } } } }
    val done = d.tasks.filter { !it.open }.sortedByDescending { it.doneDay }.take(8)
    if (done.isNotEmpty()) { Ornate(tr("Недавно сделано", "Recently done"))
        done.forEach { t -> TaskRow(t, d, { nav.push(Screen.TaskInfo(t.id)) }) { c.write { setTaskState(t, 0) } } } }
}

@Composable
fun TaskRow(t: Task, d: Data, onOpen: () -> Unit, onTick: () -> Unit) {
    val sub = d.subById[t.subjectId]
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).parchment(14.dp).clickable(onClick = onOpen).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        val doneAll = !t.open
        Box(Modifier.size(28.dp).clip(CircleShape).background(if (doneAll) C.ok else if (t.state > 0) C.okSoft else C.card2).clickable(onClick = onTick), contentAlignment = Alignment.Center) {
            if (doneAll) Icon(Icons.CHECK, C.accInk, 16.dp) else if (t.state > 0) Text("${t.state}", style = T.smallB.copy(color = C.ok)) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title.ifBlank { Words.taskKind(t.kind) }, style = if (doneAll) T.body.copy(color = C.mute) else T.bodyB, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(sub?.name, Words.taskKind(t.kind).takeIf { t.kind != Task.HW }, if (t.kind == Task.LAB || t.kind == Task.ESSAY || t.kind == Task.PROJECT) Words.taskState(t) else null,
                t.due.takeIf { it > 0 && t.open }?.let { relDay(it, d.today) }).joinToString(" · "), style = T.tiny.copy(color = if (t.open && t.due in 1 until d.today) C.bad else C.mute))
        }
        if (t.photo.isNotEmpty()) Icon(Icons.CAMERA, C.mute, 16.dp)
        Box(Modifier.size(6.dp, 30.dp).clip(RoundedCornerShape(50)).background(subjectColor(sub)))
    }
}

@Composable
private fun BigRow(t: Task, d: Data, onClick: () -> Unit) {
    val n = t.stageList.size.coerceAtLeast(1)
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).parchment(18.dp).clickable(onClick = onClick).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Prop("egg", 40.dp); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(t.title, style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${Words.taskKind(t.kind)} · ${t.stageList.getOrNull(t.stage)?.let { tr("сейчас: $it", "now: $it") } ?: tr("готово", "done")}${if (t.due > 0) " · " + inDays(t.due - d.today) else ""}", style = T.tiny)
            }
        }
        Meter(t.stage.toFloat() / n, Modifier.padding(top = 8.dp), color = C.gold)
        Text(tr("Драконье яйцо теплеет: ${t.stage} из $n этапов", "The dragon egg warms: ${t.stage} of $n stages"), style = T.tiny, modifier = Modifier.padding(top = 4.dp))
    }
}

// ---------------- trials ----------------

@Composable
private fun TrialsTab(d: Data, nav: Nav) {
    Btn(tr("Новое испытание", "New trial"), Modifier.fillMaxWidth().padding(top = 8.dp), small = true) { nav.push(Screen.ExamInfo(0)) }
    if (d.upcoming.isEmpty()) Empty(if (d.s.school) tr("Контрольные, ВПР, ОГЭ и ЕГЭ – с отсчётом дней и билетами.", "Tests and state exams – with a countdown and tickets.")
        else tr("Экзамены и зачёты сессии – с отсчётом дней и билетами.", "The exams and credits of the session – with a countdown and tickets."))
    else Ornate(tr("Впереди", "Ahead"))
    d.upcoming.forEach { e -> ExamRow(e, d) { nav.push(Screen.ExamInfo(e.id)) } }
    val past = d.exams.filter { it.done || it.day < d.today }.sortedByDescending { it.day }
    if (past.isNotEmpty()) { Ornate(tr("Позади", "Behind")); past.forEach { e -> ExamRow(e, d) { nav.push(Screen.ExamInfo(e.id)) } } }
    if (!d.s.self) { Spacer(Modifier.height(10.dp)); Ghost(if (d.s.school) tr("Оценки и четверть", "Marks and the term") else tr("Зачётка и оценки", "Record book and marks"), Modifier.fillMaxWidth(), small = true) { nav.push(Screen.Grades) } }
}

@Composable
fun ExamRow(e: Exam, d: Data, onClick: () -> Unit) {
    val sub = d.subById[e.subjectId]
    val tk = d.tickets.filter { it.examId == e.id }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).parchment(16.dp).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(54.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (e.done) Prop("scroll", 36.dp) else { Text("${(e.day - d.today).coerceAtLeast(0)}", style = T.num.copy(color = if (e.day - d.today <= 3) C.bad else C.acc)); Text(tr("дн.", "days"), style = T.tiny) }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(e.title.ifBlank { sub?.name.orEmpty() }, style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${Words.examKind(e.kind)} · ${sub?.name.orEmpty()} · ${fmtDayShort(e.day)}", style = T.tiny, maxLines = 1)
            if (!e.done && tk.isNotEmpty()) Row(Modifier.padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                tk.sortedBy { it.n }.take(40).forEach { t -> Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(50)).background(levelColor(t.level).copy(alpha = if (t.level == 0) .3f else 1f))) } }
        }
        if (e.done && e.score >= 0) Text(if (e.maxScore > 0 || e.kind == Exam.EGE || e.kind == Exam.OGE) fmt1(e.score) else Marks.show(d.s.scale, e.score), style = T.num.copy(color = C.ok))
    }
}

// ---------------- subjects ----------------

@Composable
private fun SubjectsTab(d: Data, nav: Nav) {
    var add by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Btn(tr("Предмет", "Subject"), Modifier.weight(1f), small = true) { add = true }
        Ghost(tr("Материал", "Material"), Modifier.weight(1f), small = true) { nav.push(Screen.MaterialInfo(0)) }
    }
    SubjectList(d, nav)
    if (add) SubjectSheet(null, d) { add = false }
}

@Composable
private fun SubjectList(d: Data, nav: Nav) {
    val week = weekStart(d.today)
    d.active.forEach { s ->
        val min = d.studies.filter { it.subjectId == s.id }.sumOf { it.minutes }
        val wk = d.studies.filter { it.subjectId == s.id && it.day >= week }.sumOf { it.minutes }
        val gs = d.grades.filter { it.subjectId == s.id && it.examId == 0L }
        val term = Timetable.term(d.s, d.today)
        val termGs = if (term != null) gs.filter { it.day in term } else gs
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).parchment(16.dp).clickable { nav.push(Screen.SubjectInfo(s.id)) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SchoolMark(s.school, 42.dp); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(s.name, style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(dur(min * 60) + if (wk > 0) tr(" · на неделе ${dur(wk * 60)}", " · ${dur(wk * 60)} this week") else "", style = T.tiny, maxLines = 1)
            }
            Marks.term(d.s.scale, termGs, d.s.passes)?.let { Text(if (d.s.scale == "100") fmt1(it) else fmt1(it), style = T.numS.copy(color = subjectColor(s))) }
        }
    }
    if (d.active.isEmpty()) Empty(tr("Добавьте предметы – любые, как они называются у вас.", "Add your subjects – any, named as you call them."))
}

/** A list of all subjects as a page (from the home door). */
@Composable
fun SubjectsPage(d: Data, nav: Nav) {
    var add by remember { mutableStateOf(false) }
    Page(tr("Предметы", "Subjects"), nav::pop, action = { Box(Modifier.size(40.dp).clip(CircleShape).clickable { add = true }, contentAlignment = Alignment.Center) { Icon(Icons.PLUS, C.ink, 22.dp) } }) {
                SubjectList(d, nav)
        val arch = d.subjects.filter { it.archived }
        if (arch.isNotEmpty()) { Ornate(tr("В архиве", "Archived")); arch.forEach { s -> Row2(s.name, Schools.of(s.school).title) { nav.push(Screen.SubjectInfo(s.id)) } } }
    }
    if (add) SubjectSheet(null, d) { add = false }
}

/** A subject: name, school of magic, teacher and rules, credits. */
@Composable
fun SubjectSheet(old: Subject?, d: Data, onDone: () -> Unit) {
    val c = LocalContext.current.container
    var name by remember { mutableStateOf(old?.name ?: "") }
    var school by remember { mutableStateOf(old?.school) }
    var teacher by remember { mutableStateOf(old?.teacher ?: "") }
    var contact by remember { mutableStateOf(old?.contact ?: "") }
    var rules by remember { mutableStateOf(old?.rules ?: "") }
    var credits by remember { mutableDoubleStateOf(old?.credits ?: 0.0) }
    val guessed = Schools.guess(name)
    val sc = school ?: guessed
    Sheet(onDone) {
        Text(if (old == null) tr("Новый предмет", "A new subject") else tr("Предмет", "Subject"), style = T.h2)
        Spacer(Modifier.height(10.dp))
        Field(name, { name = it }, tr("Как называется: «Сопромат», «Английский»…", "Its name: «Calculus», «French»…"))
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            SchoolMark(sc, 36.dp); Spacer(Modifier.width(8.dp))
            Column { Text(Schools.of(sc).title, style = T.bodyB); Text(Schools.of(sc).about, style = T.tiny) }
        }
        Text(tr("Школа магии", "School of magic"), style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Schools.all.forEach { s -> Chip(s.title, sc == s.id, color = schoolColor(s.id)) { school = s.id } }
        }
        Text(if (d.s.school) tr("Учитель", "Teacher") else tr("Преподаватель – магистр", "Lecturer – the master"), style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        Field(teacher, { teacher = it }, tr("Имя", "Name"))
        Spacer(Modifier.height(6.dp))
        Field(contact, { contact = it }, tr("Почта, кабинет, консультации", "Email, office, office hours"))
        Spacer(Modifier.height(6.dp))
        Field(rules, { rules = it }, if (d.s.school) tr("Что требует, как ставит оценки", "What they expect, how they mark") else tr("Требования, условия автомата", "Requirements, how to get a pass without the exam"), single = false)
        if (d.s.uni) { Text(tr("Кредиты (ECTS / зачётные единицы)", "Credits"), style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(0.0, 2.0, 3.0, 4.0, 5.0, 6.0).forEach { v -> Chip(if (v == 0.0) "–" else fmt1(v), credits == v) { credits = v } } } }
        Spacer(Modifier.height(14.dp))
        Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth(), enabled = name.isNotBlank()) {
            c.write { saveSubject((old ?: Subject(name = "", school = sc)).copy(name = name.trim(), school = sc, teacher = teacher.trim(), contact = contact.trim(), rules = rules.trim(), credits = credits)) }; onDone() }
        if (old != null) { Spacer(Modifier.height(8.dp))
            Ghost(if (old.archived) tr("Вернуть из архива", "Restore") else tr("В архив", "Archive"), Modifier.fillMaxWidth(), color = C.bad) {
                c.write { if (old.archived) saveSubject(old.copy(archived = false)) else deleteSubject(old.id) }; onDone() } }
    }
}

/** A subject's page: its numbers, marks, materials, tasks, trials, spells and teacher. */
@Composable
fun SubjectPage(id: Long, d: Data, nav: Nav) {
    val s = d.subById[id] ?: run { if (d.subjects.isNotEmpty()) nav.pop(); return }
    var edit by remember { mutableStateOf(false) }
    var grade by remember { mutableStateOf(false) }
    val mine = d.studies.filter { it.subjectId == id }
    val week = weekStart(d.today)
    Page(s.name, nav::pop, action = { Box(Modifier.size(40.dp).clip(CircleShape).clickable { edit = true }, contentAlignment = Alignment.Center) { Icon(Icons.EDIT, C.ink, 20.dp) } }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SchoolMark(s.school, 52.dp); Spacer(Modifier.width(10.dp))
            Text(Schools.of(s.school).title, style = T.h3.copy(color = schoolColor(s.school)), modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat1(dur(mine.sumOf { it.minutes } * 60), tr("всего", "in all"), Modifier.weight(1f))
            Stat1(dur(mine.filter { it.day >= week }.sumOf { it.minutes } * 60), tr("на неделе", "this week"), Modifier.weight(1f))
            Stat1("${d.spells.count { it.subjectId == id }}", tr("заклинаний", "spells"), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(tr("Свеча", "Candle"), Modifier.weight(1f), small = true) { nav.push(app.inkling.ui.Screen.Candle) }
            Ghost(tr("Научи", "Teach"), Modifier.weight(1f), small = true) { nav.push(Screen.Teach(id)) }
            Ghost(tr("Записать", "Log"), Modifier.weight(1f), small = true) { nav.push(Screen.StudyEdit(subjectId = id)) }
        }
        // marks
        if (!d.s.self) {
            val gs = d.grades.filter { it.subjectId == id && it.examId == 0L }
            val term = Timetable.term(d.s, d.today); val termGs = if (term != null) gs.filter { it.day in term } else gs
            Ornate(if (d.s.scale == "100") tr("Баллы", "Points") else tr("Оценки", "Marks"), action = tr("Добавить", "Add")) { grade = true }
            if (gs.isEmpty()) Empty(tr("Оценок пока нет", "No marks yet"))
            else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Marks.term(d.s.scale, termGs, d.s.passes)?.let { v -> Text(fmt1(v), style = T.big.copy(color = subjectColor(s))); Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { Text(if (d.s.scale == "100") tr("баллов за ${Words.term(d.s)}", "points this ${Words.term(d.s)}") else tr("средний за ${Words.term(d.s)}", "${Words.term(d.s)} average"), style = T.small)
                            Text(tr("выходит «${Marks.termMark(d.s.scale, v, d.s.passes)}»", "comes to «${Marks.termMark(d.s.scale, v, d.s.passes)}»"), style = T.smallB) } }
                }
                Marks.nudge(d.s.scale, termGs, d.s.passes)?.let { Text(it, style = T.bodyB.copy(color = C.acc), modifier = Modifier.padding(top = 6.dp)) }
                androidx.compose.foundation.layout.FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    gs.sortedBy { it.day }.takeLast(30).forEach { g -> val col = C.ok.copy(alpha = .2f + .8f * Marks.share(d.s.scale, g.value).toFloat())
                        Box(Modifier.clip(RoundedCornerShape(10.dp)).background(col.copy(alpha = .25f)).padding(horizontal = 9.dp, vertical = 5.dp)) {
                            Text(Marks.show(d.s.scale, g.value) + if (g.weight != 1.0) "×${fmt1(g.weight)}" else "", style = T.smallB) } }
                }
            }
        }
        val mats = d.materials.filter { it.subjectId == id && !it.archived }
        Ornate(tr("Материалы", "Materials"), action = tr("Добавить", "Add")) { nav.push(Screen.MaterialInfo(0, id)) }
        if (mats.isEmpty()) Empty(tr("Курс, учебник, задачник – чтобы видеть, сколько пройдено", "A course, textbook or problem book – to see how far you've got"))
        mats.forEach { m -> MaterialRow(m, d) { nav.push(Screen.MaterialInfo(m.id)) } }
        val tasks = d.tasks.filter { it.subjectId == id && it.open }
        Ornate(tr("Задания", "Tasks"), action = tr("Добавить", "Add")) { nav.push(Screen.TaskInfo(0, id)) }
        if (tasks.isEmpty()) Empty(tr("Ничего не задано", "Nothing set"))
        val c = LocalContext.current.container
        tasks.forEach { t -> TaskRow(t, d, { nav.push(Screen.TaskInfo(t.id)) }) { c.write { setTaskState(t, (t.state + 1).coerceAtMost(t.last)) } } }
        val exams = d.exams.filter { it.subjectId == id }
        if (exams.isNotEmpty()) { Ornate(tr("Испытания", "Trials")); exams.forEach { e -> ExamRow(e, d) { nav.push(Screen.ExamInfo(e.id)) } } }
        val slots = d.slots.filter { it.subjectId == id }
        if (slots.isNotEmpty()) { Ornate(tr("В расписании", "In the timetable"))
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                slots.sortedWith(compareBy({ it.weekday }, { it.start })).forEach { sl -> Text("${dayShort(sl.weekday - 1)} ${hm(sl.start)} · ${Words.slotKind(sl.kind)}" + when (sl.parity) { 1 -> tr(" · числ.", " · odd"); 2 -> tr(" · знам.", " · even"); else -> "" },
                    style = T.smallB, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(C.card).padding(horizontal = 10.dp, vertical = 6.dp)) } } }
        if (s.teacher.isNotBlank() || s.rules.isNotBlank() || s.contact.isNotBlank()) {
            Ornate(if (d.s.school) tr("Учитель", "Teacher") else tr("Магистр", "The master"))
            if (s.teacher.isNotBlank()) Text(s.teacher, style = T.bodyB)
            if (s.contact.isNotBlank()) Text(s.contact, style = T.small)
            if (s.rules.isNotBlank()) Text(s.rules, style = T.body, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
    if (edit) SubjectSheet(s, d) { edit = false }
    if (grade) GradeSheet(d, id) { grade = false }
}

@Composable
fun Stat1(v: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.card(14.dp).padding(horizontal = 10.dp, vertical = 9.dp)) { Text(v, style = T.numS, maxLines = 1); Text(label, style = T.tiny, maxLines = 1) }
}

// ---------------- the timetable ----------------

@Composable
fun TimetablePage(d: Data, nav: Nav) {
    var wd by rememberSaveable { mutableIntStateOf(java.time.LocalDate.ofEpochDay(d.today).dayOfWeek.value) }
    var edit by remember { mutableStateOf<Slot?>(null) }
    Page(tr("Расписание", "Timetable"), nav::pop) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..7).forEach { i -> val n = d.slots.count { it.weekday == i }
                Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (wd == i) C.acc else C.card).clickable { wd = i }.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dayShort(i - 1), style = T.smallB.copy(color = if (wd == i) C.accInk else C.ink)); Text(if (n > 0) "$n" else "·", style = T.tiny.copy(color = if (wd == i) C.accInk else C.mute)) } }
        }
        val list = d.slots.filter { it.weekday == wd }.sortedBy { it.start }
        if (list.isEmpty()) Empty(tr("В этот день занятий нет", "No classes on this day"))
        list.forEach { sl -> val sub = d.subById[sl.subjectId]
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).card(14.dp).clickable { edit = sl }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${hm(sl.start)}\n${hm(sl.end)}", style = T.smallB, modifier = Modifier.width(48.dp))
                Box(Modifier.size(4.dp, 30.dp).clip(RoundedCornerShape(50)).background(subjectColor(sub))); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text(sub?.name ?: "–", style = T.bodyB)
                    Text(listOfNotNull(Words.slotKind(sl.kind), sl.room.takeIf { it.isNotBlank() }, when (sl.parity) { 1 -> tr("числитель", "odd weeks"); 2 -> tr("знаменатель", "even weeks"); else -> null }).joinToString(" · "), style = T.tiny) }
            }
        }
        Spacer(Modifier.height(10.dp))
        Btn(tr("Добавить занятие", "Add a class"), Modifier.fillMaxWidth(), enabled = d.active.isNotEmpty()) { edit = Slot(subjectId = 0, weekday = wd, start = 0, end = 0) }
        if (d.active.isEmpty()) Text(tr("Сначала добавьте предметы.", "Add subjects first."), style = T.small.copy(color = C.bad), modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(20.dp))
    }
    edit?.let { SlotSheet(it, d) { edit = null } }
}

@Composable
private fun SlotSheet(old: Slot, d: Data, onDone: () -> Unit) {
    val c = LocalContext.current.container
    val bells = d.s.bells.mapNotNull { b -> b.split('-').takeIf { it.size == 2 }?.let { p -> val a = parseHm(p[0]); val z = parseHm(p[1]); if (a != null && z != null) a to z else null } }
    // a new class takes the first free bell of the day
    val taken = d.slots.filter { it.weekday == old.weekday }.map { it.start }.toSet()
    val firstFree = bells.firstOrNull { it.first !in taken } ?: (9 * 60 to 10 * 60 + 30)
    var sub by remember { mutableLongStateOf(old.subjectId.takeIf { it != 0L } ?: d.active.firstOrNull()?.id ?: 0) }
    androidx.compose.runtime.LaunchedEffect(d.active.size) { if (sub == 0L) sub = d.active.firstOrNull()?.id ?: 0L }
    var start by remember { mutableIntStateOf(if (old.id == 0L) firstFree.first else old.start) }
    var end by remember { mutableIntStateOf(if (old.id == 0L) firstFree.second else old.end) }
    var kind by remember { mutableIntStateOf(if (old.id == 0L) (if (d.s.school) Slot.LESSON else Slot.LECTURE) else old.kind) }
    var room by remember { mutableStateOf(old.room) }
    var parity by remember { mutableIntStateOf(old.parity) }
    Sheet(onDone) {
        Text(tr("Занятие", "Class"), style = T.h2)
        Spacer(Modifier.height(10.dp))
        SubjectPicker(d.active, sub) { sub = it }
        if (bells.isNotEmpty()) {
            Text(if (d.s.school) tr("Урок", "Lesson") else tr("Пара", "Period"), style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                bells.forEachIndexed { i, (a, z) -> Chip("${i + 1} · ${hm(a)}", start == a && end == z) { start = a; end = z } } }
        }
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("с ", "from "), style = T.small); TimeChip(start) { start = it; if (end <= it) end = it + 45 }
            Text(tr(" до ", " to "), style = T.small); TimeChip(end) { end = it }
        }
        Text(tr("Вид", "Kind"), style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        val kinds = if (d.s.school) listOf(Slot.LESSON, Slot.CLUB, Slot.TUTOR) else listOf(Slot.LECTURE, Slot.SEMINAR, Slot.PRACTICE, Slot.LAB, Slot.TUTOR)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            kinds.forEach { k -> Chip(Words.slotKind(k).replaceFirstChar { it.uppercase() }, kind == k) { kind = k } } }
        if (d.s.parityStart != 0L) {
            Text(tr("Недели", "Weeks"), style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(tr("каждую", "every"), parity == 0) { parity = 0 }; Chip(tr("числитель", "odd"), parity == 1) { parity = 1 }; Chip(tr("знаменатель", "even"), parity == 2) { parity = 2 } }
        }
        Field(room, { room = it }, if (d.s.school) tr("Кабинет", "Room") else tr("Аудитория", "Room"), modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(14.dp))
        Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth(), enabled = sub != 0L && end > start) {
            c.write { saveSlot(old.copy(subjectId = sub, start = start, end = end, kind = kind, room = room.trim(), parity = parity)) }; onDone() }
        if (old.id != 0L) { Spacer(Modifier.height(8.dp)); Ghost(tr("Удалить", "Delete"), Modifier.fillMaxWidth(), color = C.bad) { c.write { deleteSlot(old.id) }; onDone() } }
    }
}
