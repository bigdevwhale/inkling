package app.inkling.ui.academy

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.inkling.container
import app.inkling.data.Exam
import app.inkling.data.Material
import app.inkling.data.Task
import app.inkling.data.Ticket
import app.inkling.domain.Marks
import app.inkling.domain.Timetable
import app.inkling.domain.Words
import app.inkling.domain.dur
import app.inkling.domain.fmt1
import app.inkling.domain.fmtDay
import app.inkling.domain.fmtDayShort
import app.inkling.domain.forecast
import app.inkling.domain.inDays
import app.inkling.domain.relDay
import app.inkling.domain.tr
import app.inkling.ui.Data
import app.inkling.ui.Nav
import app.inkling.ui.Screen
import app.inkling.ui.art.Mini
import app.inkling.ui.art.Prop
import app.inkling.ui.components.Btn
import app.inkling.ui.components.Chip
import app.inkling.ui.components.DayPicker
import app.inkling.ui.components.Empty
import app.inkling.ui.components.Field
import app.inkling.ui.components.Ghost
import app.inkling.ui.components.Icon
import app.inkling.ui.components.Icons
import app.inkling.ui.components.LevelRow
import app.inkling.ui.components.Line
import app.inkling.ui.components.Meter
import app.inkling.ui.components.Page
import app.inkling.ui.components.Row2
import app.inkling.ui.components.Section
import app.inkling.ui.components.Sheet
import app.inkling.ui.components.SubjectPicker
import app.inkling.ui.components.card
import app.inkling.ui.components.levelColor
import app.inkling.ui.components.subjectColor
import app.inkling.ui.magic.GradeSheet
import app.inkling.ui.magic.Stepper
import app.inkling.ui.magic.rememberPhoto
import app.inkling.ui.theme.C
import app.inkling.ui.theme.T

// ---------------- materials ----------------

@Composable
fun MaterialRow(m: Material, d: Data, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp)).background(C.card).clickable(onClick = onClick).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Prop(if (m.finished > 0) "books" else if (m.unit == Material.PAGES) "book" else "scroll", 30.dp); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(m.title, style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (m.total > 0) Meter(m.done.toFloat() / m.total, Modifier.padding(vertical = 4.dp), color = subjectColor(d.subById[m.subjectId]), h = 5.dp)
            Text(if (m.finished > 0) tr("закончено ${fmtDayShort(m.finished)}", "finished ${fmtDayShort(m.finished)}") else "${m.done}${if (m.total > 0) " / ${m.total}" else ""} · ${Words.unitName(m.unit).lowercase()}" +
                (m.forecast(d.studies, d.today)?.let { tr(" · к ${fmtDayShort(it)}", " · by ${fmtDayShort(it)}") } ?: ""), style = T.tiny)
        }
    }
}

@Composable
fun MaterialPage(scr: Screen.MaterialInfo, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val old = d.materials.firstOrNull { it.id == scr.id }
    if (scr.id != 0L && old == null) { if (d.materials.isNotEmpty()) nav.pop(); return }
    var editing by remember { mutableStateOf(old == null) }
    if (editing) {
        var sub by remember { mutableLongStateOf(old?.subjectId ?: scr.subjectId.takeIf { it != 0L } ?: d.active.firstOrNull()?.id ?: 0) }
        androidx.compose.runtime.LaunchedEffect(d.active.size) { if (sub == 0L) sub = d.active.firstOrNull()?.id ?: 0L }
        var title by remember { mutableStateOf(old?.title ?: "") }
        var unit by remember { mutableStateOf(old?.unit ?: Material.LESSONS) }
        var total by remember { mutableStateOf(old?.total?.takeIf { it > 0 }?.toString() ?: "") }
        var done by remember { mutableStateOf(old?.done?.toString() ?: "0") }
        Page(if (old == null) tr("Новый материал", "A new material") else tr("Материал", "Material"), { if (old == null) nav.pop() else editing = false }) {
            Text(tr("Онлайн-курс, учебник, задачник, видеокурс – то, что проходится по частям. Кицунэ отрастит хвост за каждый законченный.",
                "An online course, a textbook, a problem book, a video course – something done in parts. The kitsune grows a tail for each one finished."), style = T.small)
            Text(tr("Предмет", "Subject"), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
            SubjectPicker(d.active, sub) { sub = it }
            Field(title, { title = it }, tr("Название", "Title"), modifier = Modifier.padding(top = 12.dp))
            Text(tr("Чем считается", "Counted in"), style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Material.LESSONS, Material.PAGES, Material.TASKS, Material.VIDEOS, Material.CHAPTERS, Material.MINUTES).forEach { u -> Chip(Words.unitName(u), unit == u) { unit = u } } }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(total, { total = it.filter(Char::isDigit) }, tr("Всего", "Total"), Modifier.weight(1f), number = true)
                Field(done, { done = it.filter(Char::isDigit) }, tr("Уже пройдено", "Done already"), Modifier.weight(1f), number = true)
            }
            Spacer(Modifier.height(16.dp))
            Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth(), enabled = title.isNotBlank() && sub != 0L) {
                val t = total.toIntOrNull() ?: 0; val dn = (done.toIntOrNull() ?: 0).let { if (t > 0) it.coerceAtMost(t) else it }
                c.write { saveMaterial((old ?: Material(subjectId = sub, title = "", created = clock())).copy(subjectId = sub, title = title.trim(), unit = unit, total = t, done = dn)) }
                if (old == null) nav.pop() else editing = false
            }
            if (old != null) { Spacer(Modifier.height(8.dp)); Ghost(tr("Удалить", "Delete"), Modifier.fillMaxWidth(), color = C.bad) { c.write { deleteMaterial(old.id) }; nav.pop() } }
        }
        return
    }
    val m = old!!
    var add by remember { mutableIntStateOf(if (m.unit == Material.PAGES) 10 else 1) }
    Page(m.title, nav::pop, action = { Box(Modifier.size(40.dp).clip(CircleShape).clickable { editing = true }, contentAlignment = Alignment.Center) { Icon(Icons.EDIT, C.ink, 20.dp) } }) {
        Text("${d.subById[m.subjectId]?.name.orEmpty()} · ${Words.unitName(m.unit).lowercase()}", style = T.small)
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Bottom) {
            Text("${m.done}", style = T.big); Text(if (m.total > 0) " / ${m.total}" else "", style = T.h2.copy(color = C.mute), modifier = Modifier.padding(bottom = 6.dp))
        }
        if (m.total > 0) Meter(m.done.toFloat() / m.total, Modifier.padding(top = 6.dp), color = subjectColor(d.subById[m.subjectId]), h = 10.dp)
        m.forecast(d.studies, d.today)?.let { Text(tr("В нынешнем темпе закончите ${fmtDay(it)}", "At this pace you'll finish on ${fmtDay(it)}"), style = T.bodyB.copy(color = C.acc), modifier = Modifier.padding(top = 8.dp)) }
        if (m.finished > 0) Text(tr("Закончено ${fmtDay(m.finished)} – корешок стоит на полке в башне", "Finished on ${fmtDay(m.finished)} – its spine stands on the tower shelf"), style = T.bodyB.copy(color = C.ok), modifier = Modifier.padding(top = 8.dp))
        else {
            Section(tr("Отметить, сколько прошли", "Mark how far you got"))
            Row(verticalAlignment = Alignment.CenterVertically) { Stepper(add, if (m.unit == Material.PAGES) 5 else 1) { add = it.coerceAtLeast(1) }; Spacer(Modifier.weight(1f))
                Btn("+$add", small = true) { c.write { addUnits(m.id, add) } } }
            Spacer(Modifier.height(10.dp))
            Btn(tr("Зажечь свечу над ним", "Light a candle for it"), Modifier.fillMaxWidth()) { nav.push(Screen.Candle) }
        }
        val mine = d.studies.filter { it.materialId == m.id }
        if (mine.isNotEmpty()) { Section(tr("Занятия · ${dur(mine.sumOf { it.minutes } * 60)}", "Sessions · ${dur(mine.sumOf { it.minutes } * 60)}"))
            mine.take(15).forEach { st -> Row2(relDay(st.day, d.today), st.note.takeIf { it.isNotBlank() }, "${dur(st.minutes * 60)}${if (st.units > 0) " · +${st.units}" else ""}") { nav.push(Screen.StudyEdit(st.id)) } } }
        Spacer(Modifier.height(24.dp))
    }
}

// ---------------- tasks ----------------

private fun defaultStages(kind: String) = if (kind == Task.THESIS) tr("Тема и руководитель\nОбзор литературы\nПервая глава\nВторая глава\nПрактическая часть\nЧерновик целиком\nПредзащита\nЗащита", "Topic and supervisor\nLiterature review\nChapter one\nChapter two\nPractical part\nFull draft\nPre-defence\nDefence")
    else if (kind == Task.COURSEWORK) tr("Тема\nОбзор литературы\nТеоретическая часть\nПрактическая часть\nЧерновик\nЗащита", "Topic\nLiterature review\nTheory\nPractice\nDraft\nDefence")
    else tr("Идея\nПлан\nСделать\nПроверить\nСдать", "Idea\nPlan\nMake it\nCheck it\nHand it in")

@Composable
fun TaskPage(scr: Screen.TaskInfo, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val ctx = LocalContext.current
    val old = d.tasks.firstOrNull { it.id == scr.id }
    if (scr.id != 0L && old == null) { if (d.tasks.isNotEmpty()) nav.pop(); return }
    var editing by remember { mutableStateOf(old == null) }
    if (editing) {
        var sub by remember { mutableLongStateOf(old?.subjectId ?: scr.subjectId.takeIf { it != 0L } ?: d.active.firstOrNull()?.id ?: 0) }
        androidx.compose.runtime.LaunchedEffect(d.active.size) { if (sub == 0L) sub = d.active.firstOrNull()?.id ?: 0L }
        var kind by remember { mutableStateOf(old?.kind ?: scr.kind) }
        var title by remember { mutableStateOf(old?.title ?: "") }
        var due by remember { mutableLongStateOf(old?.due ?: 0L) }
        var note by remember { mutableStateOf(old?.note ?: "") }
        var photo by remember { mutableStateOf(old?.photo ?: "") }
        var stages by remember { mutableStateOf(old?.stages ?: "") }
        val take = rememberPhoto { photo = it }
        val big = kind == Task.COURSEWORK || kind == Task.THESIS || (kind == Task.PROJECT && stages.isNotBlank())
        Page(if (old == null) tr("Новое задание", "A new task") else tr("Задание", "Task"), { if (old == null) nav.pop() else editing = false }) {
            Text(tr("Предмет", "Subject"), style = T.sec, modifier = Modifier.padding(bottom = 8.dp))
            SubjectPicker(d.active, sub) { sub = it; if (due == 0L && old == null) Timetable.nextClass(d.s, d.slots, it, d.today)?.let { n -> due = n } }
            Text(tr("Что это", "What it is"), style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            val kinds = when { d.s.school -> listOf(Task.HW, Task.TEST, Task.PROJECT, Task.ESSAY, Task.READING, Task.OTHER)
                d.s.self -> listOf(Task.HW, Task.PROJECT, Task.READING, Task.OTHER)
                else -> listOf(Task.HW, Task.LAB, Task.ESSAY, Task.TEST, Task.PROJECT, Task.COURSEWORK, Task.THESIS, Task.READING, Task.OTHER) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                kinds.forEach { k -> Chip(Words.taskKind(k), kind == k) { kind = k; if ((k == Task.COURSEWORK || k == Task.THESIS) && stages.isBlank()) stages = defaultStages(k) } } }
            Field(title, { title = it }, if (big) tr("Тема работы", "The topic") else tr("Что сделать: № 214, 216; параграф 12", "What to do: ex. 214, 216; chapter 12"), modifier = Modifier.padding(top = 12.dp), single = false)
            Text(tr("Срок", "Due"), style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            DayPicker(due, d.today, none = true, future = true) { due = it }
            Timetable.nextClass(d.s, d.slots, sub, d.today)?.let { n -> if (n != due) Text(tr("Следующее занятие – ${relDay(n, d.today)}", "The next class is ${relDay(n, d.today)}"), style = T.tiny.copy(color = C.acc), modifier = Modifier.padding(top = 4.dp).clickable { due = n }) }
            if (big || kind == Task.PROJECT) {
                Text(tr("Этапы – по одному в строке", "Stages – one per line"), style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                Field(stages, { stages = it }, defaultStages(kind), single = false)
                if (stages.isBlank()) Text(tr("Заполнить по образцу", "Fill in a template"), style = T.smallB.copy(color = C.acc), modifier = Modifier.padding(top = 4.dp).clickable { stages = defaultStages(kind) })
            }
            Field(note, { note = it }, tr("Заметка", "Note"), modifier = Modifier.padding(top = 12.dp), single = false)
            Row(Modifier.padding(top = 8.dp).clip(RoundedCornerShape(50)).background(C.card2).clickable { take() }.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.CAMERA, C.ink, 16.dp); Spacer(Modifier.width(6.dp)); Text(if (photo.isEmpty()) tr("Фото доски или задания", "Photo of the board or task") else tr("Переснять", "Retake"), style = T.smallB) }
            Spacer(Modifier.height(16.dp))
            Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth(), enabled = sub != 0L && (title.isNotBlank() || photo.isNotEmpty())) {
                val t = (old ?: Task(subjectId = sub, title = "", created = d.today)).copy(subjectId = sub, kind = kind, title = title.trim(), due = due, note = note.trim(), photo = photo,
                    stages = if (big || kind == Task.PROJECT) stages.trim() else "")
                c.write { saveTask(t) }
                if (old == null) nav.pop() else editing = false
            }
            if (old != null) { Spacer(Modifier.height(8.dp)); Ghost(tr("Удалить", "Delete"), Modifier.fillMaxWidth(), color = C.bad) { c.write { deleteTask(old.id) }; nav.pop() } }
            Spacer(Modifier.height(24.dp))
        }
        return
    }
    val t = old!!
    Page(Words.taskKind(t.kind), nav::pop, action = { Box(Modifier.size(40.dp).clip(CircleShape).clickable { editing = true }, contentAlignment = Alignment.Center) { Icon(Icons.EDIT, C.ink, 20.dp) } }) {
        Text(d.subById[t.subjectId]?.name.orEmpty(), style = T.small.copy(color = subjectColor(d.subById[t.subjectId])))
        Text(t.title, style = T.h2, modifier = Modifier.padding(top = 4.dp))
        if (t.due > 0) Text(if (t.open) tr("Срок: ${relDay(t.due, d.today)} (${inDays(t.due - d.today)})", "Due ${relDay(t.due, d.today)} (${inDays(t.due - d.today)})") else tr("Срок был ${fmtDayShort(t.due)}", "Was due ${fmtDayShort(t.due)}"),
            style = T.small.copy(color = if (t.open && t.due < d.today) C.bad else C.mute), modifier = Modifier.padding(top = 4.dp))
        if (t.note.isNotBlank()) Text(t.note, style = T.body, modifier = Modifier.padding(top = 10.dp))
        if (t.photo.isNotEmpty()) {
            val bmp = remember(t.photo) { runCatching { ctx.contentResolver.openInputStream(Uri.parse(t.photo))?.use { android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }) } }.getOrNull() }
            if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.padding(top = 12.dp).fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)).clickable {
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(t.photo), "image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } }, contentScale = ContentScale.Crop)
        }
        if (t.stageList.isNotEmpty()) {
            Section(tr("Этапы", "Stages"))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Mini(if (t.state >= 3) "dragon" else "dragon", 54.dp, egg = t.state < 3); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Meter(t.stage.toFloat() / t.stageList.size, color = C.gold); Text(tr("${t.stage} из ${t.stageList.size}", "${t.stage} of ${t.stageList.size}"), style = T.tiny, modifier = Modifier.padding(top = 4.dp)) }
            }
            t.stageList.forEachIndexed { i, st -> val done = i < t.stage
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { c.write { setStage(t, if (done) i else i + 1) } }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(26.dp).clip(CircleShape).background(if (done) C.gold else C.card2), contentAlignment = Alignment.Center) { if (done) Icon(Icons.CHECK, C.accInk, 15.dp) else Text("${i + 1}", style = T.tiny) }
                    Spacer(Modifier.width(10.dp)); Text(st, style = if (done) T.body.copy(color = C.mute) else T.body) }
            }
        } else {
            Section(tr("Где сейчас", "Where it is"))
            val states = when (t.kind) { Task.LAB -> listOf(tr("не начата", "to do"), tr("сделана", "done"), tr("сдана", "handed in"), tr("защищена", "defended"))
                Task.ESSAY, Task.PROJECT -> listOf(tr("в работе", "to do"), tr("готово", "done"), tr("сдано", "handed in")); else -> listOf(tr("не сделано", "to do"), tr("сделано", "done")) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { states.forEachIndexed { i, l -> Chip(l, t.state == i, Modifier.weight(1f), color = if (i == 0) C.acc else C.ok) { c.write { setTaskState(t, i) } } } }
        }
        Spacer(Modifier.height(14.dp))
        Btn(tr("Зажечь свечу над заданием", "Light a candle for it"), Modifier.fillMaxWidth()) { nav.push(Screen.Candle) }
        val mine = d.studies.filter { it.taskId == t.id }
        if (mine.isNotEmpty()) Text(tr("Потрачено: ${dur(mine.sumOf { it.minutes } * 60)}", "Time spent: ${dur(mine.sumOf { it.minutes } * 60)}"), style = T.small, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.height(24.dp))
    }
}

// ---------------- trials ----------------

@Composable
fun ExamPage(scr: Screen.ExamInfo, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val old = d.exams.firstOrNull { it.id == scr.id }
    if (scr.id != 0L && old == null) { if (d.exams.isNotEmpty()) nav.pop(); return }
    var editing by remember { mutableStateOf(old == null) }
    if (editing) {
        var sub by remember { mutableLongStateOf(old?.subjectId ?: scr.subjectId.takeIf { it != 0L } ?: d.active.firstOrNull()?.id ?: 0) }
        androidx.compose.runtime.LaunchedEffect(d.active.size) { if (sub == 0L) sub = d.active.firstOrNull()?.id ?: 0L }
        var kind by remember { mutableStateOf(old?.kind ?: if (d.s.school) Exam.TEST else Exam.EXAM) }
        var title by remember { mutableStateOf(old?.title ?: "") }
        var day by remember { mutableLongStateOf(old?.day ?: (d.today + 14)) }
        var parts by remember { mutableStateOf(old?.parts?.takeIf { it > 0 }?.toString() ?: "") }
        var goal by remember { mutableStateOf(old?.goal?.takeIf { it > 0 }?.let { fmt1(it) } ?: "") }
        var tickets by remember { mutableStateOf(d.tickets.filter { it.examId == old?.id }.sortedBy { it.n }.joinToString("\n") { "${it.n}. ${it.text}" }) }
        val state = kind == Exam.OGE || kind == Exam.EGE
        Page(if (old == null) tr("Новое испытание", "A new trial") else tr("Испытание", "Trial"), { if (old == null) nav.pop() else editing = false }) {
            SubjectPicker(d.active, sub) { sub = it }
            Text(tr("Что за испытание", "What kind"), style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            val kinds = when { d.s.school -> listOf(Exam.TEST, Exam.VPR, Exam.OGE, Exam.EGE, Exam.OLYMP, Exam.OTHER); d.s.self -> listOf(Exam.CERT, Exam.EXAM, Exam.TEST, Exam.OTHER)
                else -> listOf(Exam.EXAM, Exam.CREDIT, Exam.DIFF, Exam.TEST, Exam.OLYMP, Exam.CERT, Exam.OTHER) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { kinds.forEach { k -> Chip(Words.examKind(k), kind == k) { kind = k } } }
            Field(title, { title = it }, tr("Название (по желанию)", "Title (optional)"), modifier = Modifier.padding(top = 12.dp))
            Text(tr("Когда", "When"), style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            DayPicker(day, d.today, future = true) { day = it }
            if (state) Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(parts, { parts = it.filter(Char::isDigit) }, tr("Заданий в КИМ", "Tasks in the paper"), Modifier.weight(1f), number = true)
                Field(goal, { goal = it }, tr("Нужно баллов", "Points wanted"), Modifier.weight(1f), number = true)
            }
            Text(if (state) tr("Названия заданий – по строке (по желанию)", "Task names – one per line (optional)") else tr("Билеты – вставьте списком, по одному в строке", "Tickets – paste a list, one per line"),
                style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            Field(tickets, { tickets = it }, tr("1. Предел последовательности\n2. Непрерывность функции\n…", "1. Limits of sequences\n2. Continuity\n…"), single = false)
            Spacer(Modifier.height(16.dp))
            Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth(), enabled = sub != 0L) {
                val e = (old ?: Exam(subjectId = sub, title = "", day = day)).copy(subjectId = sub, kind = kind, title = title.trim(), day = day, parts = parts.toIntOrNull() ?: 0,
                    goal = goal.replace(',', '.').toDoubleOrNull() ?: 0.0, maxScore = if (state) 100.0 else old?.maxScore ?: 0.0)
                c.write { val id = saveExam(e); if (tickets.isNotBlank()) setTickets(id, tickets) }
                if (old == null) nav.pop() else editing = false
            }
            if (old != null) { Spacer(Modifier.height(8.dp)); Ghost(tr("Удалить", "Delete"), Modifier.fillMaxWidth(), color = C.bad) { c.write { deleteExam(old.id) }; nav.pop() } }
            Spacer(Modifier.height(24.dp))
        }
        return
    }
    val e = old!!
    val tk = d.tickets.filter { it.examId == e.id }.sortedBy { it.n }
    var open by remember { mutableStateOf<Ticket?>(null) }
    var draw by remember { mutableStateOf<Ticket?>(null) }
    var result by remember { mutableStateOf(false) }
    var paper by remember { mutableStateOf(false) }
    val state = e.kind == Exam.OGE || e.kind == Exam.EGE
    Page(e.title.ifBlank { d.subById[e.subjectId]?.name.orEmpty() }, nav::pop, action = { Box(Modifier.size(40.dp).clip(CircleShape).clickable { editing = true }, contentAlignment = Alignment.Center) { Icon(Icons.EDIT, C.ink, 20.dp) } }) {
        Text("${Words.examKind(e.kind)} · ${d.subById[e.subjectId]?.name.orEmpty()} · ${fmtDay(e.day)}", style = T.small)
        if (!e.done) Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Bottom) {
            Text("${(e.day - d.today).coerceAtLeast(0)}", style = T.big.copy(color = C.acc)); Text(tr(" дней до испытания", " days to go"), style = T.body, modifier = Modifier.padding(bottom = 8.dp)) }
        else Text(tr("Результат: ", "Result: ") + (if (state || e.maxScore > 0) fmt1(e.score) else Marks.show(d.s.scale, e.score)), style = T.h2.copy(color = C.ok), modifier = Modifier.padding(top = 10.dp))
        if (tk.isNotEmpty()) {
            val known = tk.count { it.level >= 2 }; val left = tk.count { it.level < 2 }; val days = (e.day - d.today).coerceAtLeast(1)
            Section(if (state) tr("Задания КИМ", "Tasks of the paper") else tr("Билеты", "Tickets"))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) { tk.forEach { t -> Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(50)).background(levelColor(t.level).copy(alpha = if (t.level == 0) .3f else 1f))) } }
            Text(tr("Выучено $known из ${tk.size}", "$known of ${tk.size} learned") + if (!e.done && left > 0) tr(" · в день нужно ${(left + days - 1) / days}", " · ${(left + days - 1) / days} a day needed") else "", style = T.bodyB, modifier = Modifier.padding(top = 6.dp))
            if (!e.done) Btn(tr("Сфинкс тянет билет", "The sphinx draws a ticket"), Modifier.fillMaxWidth().padding(top = 10.dp)) { draw = tk.filter { it.level < 3 }.ifEmpty { tk }.minByOrNull { it.level * 1000 + (it.asked % 997).toInt() + (Math.random() * 400).toInt() } }
            Spacer(Modifier.height(6.dp))
            tk.forEach { t -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { open = t }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(levelColor(t.level).copy(alpha = .18f)), contentAlignment = Alignment.Center) { Text("${t.n}", style = T.smallB.copy(color = levelColor(t.level))) }
                Spacer(Modifier.width(10.dp)); Text(t.text.ifBlank { if (state) tr("Задание ${t.n}", "Task ${t.n}") else tr("Билет ${t.n}", "Ticket ${t.n}") }, style = T.body, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis) } }
        } else if (!e.done) Empty(tr("Добавьте билеты – карандаш вверху. Сфинкс будет их тянуть.", "Add the tickets – the pencil above. The sphinx will draw them."))
        // practice papers and the predicted score
        if (state || e.maxScore > 0) {
            val papers = d.grades.filter { it.examId == e.id }.sortedBy { it.day }
            Section(tr("Пробные варианты", "Practice papers"), action = tr("Добавить", "Add")) { paper = true }
            if (papers.isEmpty()) Empty(tr("Решили вариант – запишите балл, и появится прогноз.", "Did a practice paper? Write the score and a forecast appears."))
            else {
                val last = papers.takeLast(3).map { it.value }; val pred = last.average()
                Text(tr("Прогноз: ~${fmt1(pred)} баллов", "Forecast: ~${fmt1(pred)} points"), style = T.h3.copy(color = C.acc))
                if (e.goal > 0) Text(if (pred >= e.goal) tr("Цель ${fmt1(e.goal)} в досягаемости", "The goal of ${fmt1(e.goal)} is in reach") else tr("До цели ${fmt1(e.goal)} не хватает ~${fmt1(e.goal - pred)}", "~${fmt1(e.goal - pred)} short of the goal of ${fmt1(e.goal)}"), style = T.small)
                if (papers.size >= 2) Line(papers.mapIndexed { i, g -> i.toDouble() to g.value }, Modifier.padding(top = 8.dp), h = 110.dp)
                if (state && tk.isNotEmpty()) { val weak = tk.filter { it.level <= 1 }.take(5)
                    if (weak.isNotEmpty()) Text(tr("Слабые задания: ", "Weak tasks: ") + weak.joinToString(", ") { "№${it.n}" } + tr(" – начните с них, это самые быстрые баллы", " – start with these, they're the quickest points"), style = T.small, modifier = Modifier.padding(top = 6.dp)) }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (!e.done) Btn(tr("Записать результат", "Write the result"), Modifier.fillMaxWidth()) { result = true }
        else Ghost(tr("Поделиться свитком", "Share the scroll"), Modifier.fillMaxWidth()) { nav.share = app.inkling.ui.share.ShareReq(app.inkling.ui.share.CardKind.TRIAL, e.id) }
        Spacer(Modifier.height(24.dp))
    }
    open?.let { t -> Sheet({ open = null }) {
        Text(if (state) tr("Задание ${t.n}", "Task ${t.n}") else tr("Билет ${t.n}", "Ticket ${t.n}"), style = T.h2)
        if (t.text.isNotBlank()) Text(t.text, style = T.body, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(12.dp))
        LevelRow(t.level) { l -> c.write { setTicketLevel(t, l) }; open = null }
    } }
    draw?.let { t -> Sheet({ draw = null }) {
        Row(verticalAlignment = Alignment.CenterVertically) { Mini("sphinx", 56.dp); Spacer(Modifier.width(8.dp)); Text(tr("Ваш билет…", "Your ticket…"), style = T.h3) }
        Text("№${t.n}", style = T.big.copy(color = C.acc), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(t.text, style = T.h3, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(tr("Расскажите его вслух или на бумаге, как на испытании. Потом честно отметьте:", "Answer it aloud or on paper, as in the trial. Then be honest:"), style = T.small, modifier = Modifier.padding(vertical = 12.dp))
        LevelRow(t.level) { l -> c.write { setTicketLevel(t, l) }; draw = null }
    } }
    if (result) {
        var v by remember { mutableStateOf<Double?>(null) }; var typed by remember { mutableStateOf("") }
        Sheet({ result = false }) {
            Text(tr("Результат", "The result"), style = T.h2)
            val ch = if (state || e.maxScore > 0) emptyList() else if (e.kind == Exam.CREDIT) Marks.choices("pass") else Marks.choices(d.s.scale)
            if (ch.isEmpty()) Field(typed, { typed = it }, tr("Баллы", "Points"), number = true, modifier = Modifier.padding(top = 10.dp))
            else FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { ch.forEach { (l, x) -> Chip(l, v == x) { v = x } } }
            Spacer(Modifier.height(14.dp))
            val score = v ?: typed.replace(',', '.').toDoubleOrNull()
            Btn(tr("Записать", "Save"), Modifier.fillMaxWidth(), enabled = score != null) { c.write { finishExam(e, score!!) }; result = false }
        }
    }
    if (paper) GradeSheet(d, e.subjectId, e.id) { paper = false }
}

// ---------------- marks and the record book ----------------

@Composable
fun GradesPage(d: Data, nav: Nav) {
    var add by remember { mutableStateOf(false) }
    val term = Timetable.term(d.s, d.today)
    Page(if (d.s.school) tr("Оценки", "Marks") else tr("Зачётка", "Record book"), nav::pop, action = { Box(Modifier.size(40.dp).clip(CircleShape).clickable { add = true }, contentAlignment = Alignment.Center) { Icon(Icons.PLUS, C.ink, 22.dp) } }) {
        Text(when (d.s.scale) { "100" -> tr("Баллы складываются: ${d.s.passes[0]} – «3», ${d.s.passes[1]} – «4», ${d.s.passes[2]} – «5».", "Points add up: ${d.s.passes[0]} for a 3, ${d.s.passes[1]} for a 4, ${d.s.passes[2]} for a 5.")
            else -> tr("Средний с весами, как в электронном дневнике. Округление – от половины.", "A weighted average, as in electronic diaries. Rounded from half up.") }, style = T.small)
        Section(if (term != null) tr("${Words.term(d.s).replaceFirstChar { it.uppercase() }} до ${fmtDayShort(term.last)}", "This ${Words.term(d.s)}, until ${fmtDayShort(term.last)}") else tr("Предметы", "Subjects"))
        d.active.forEach { s ->
            val gs = d.grades.filter { it.subjectId == s.id && it.examId == 0L && (term == null || it.day in term) }
            if (gs.isEmpty()) return@forEach
            val v = Marks.term(d.s.scale, gs, d.s.passes)!!
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).card(14.dp).clickable { nav.push(Screen.SubjectInfo(s.id)) }.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.name, style = T.bodyB, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(fmt1(v), style = T.numS.copy(color = subjectColor(s))); Text(" → ${Marks.termMark(d.s.scale, v, d.s.passes)}", style = T.smallB)
                }
                Text(gs.sortedBy { it.day }.joinToString(" ") { Marks.show(d.s.scale, it.value) }, style = T.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Marks.nudge(d.s.scale, gs, d.s.passes)?.let { Text(it, style = T.tiny.copy(color = C.acc)) }
            }
        }
        if (d.grades.none { it.examId == 0L }) Empty(tr("Записывайте оценки – сорока соберёт их и подскажет, сколько осталось до пятёрки.", "Write down marks – the magpie collects them and tells you how far you are from the top mark."))
        // the record book: trials passed
        val passed = d.exams.filter { it.done && it.score >= 0 && it.kind !in setOf(Exam.TEST, Exam.VPR) }.sortedByDescending { it.day }
        if (passed.isNotEmpty()) {
            Section(if (d.s.school) tr("Испытания", "Trials") else tr("Зачётка", "Record book"))
            Marks.gpa(d.s.scale, d.exams) { id -> d.subById[id]?.credits ?: 0.0 }?.let { g ->
                Row(verticalAlignment = Alignment.CenterVertically) { Text(fmt1(g), style = T.big.copy(color = C.acc)); Spacer(Modifier.width(10.dp))
                    Text(if (d.subjects.any { it.credits > 0 }) tr("средний балл с весом кредитов", "average weighted by credits") else tr("средний балл экзаменов", "exam average"), style = T.small) } }
            if (d.s.scale == "5" && d.s.uni) {
                val session = passed.filter { it.day >= d.today - 120 && it.kind in setOf(Exam.EXAM, Exam.DIFF) }
                if (session.isNotEmpty()) Text(when { session.all { it.score >= 5 } -> tr("Сессия на «отлично» – повышенная стипендия в кармане", "A straight-A session – the higher grant is yours")
                    session.all { it.score >= 4 } -> tr("Без троек – стипендия будет", "No 3s – the grant is safe"); else -> tr("Есть тройки – стипендии в этот раз, скорее всего, не будет", "There are 3s – probably no grant this time") },
                    style = T.bodyB.copy(color = if (session.all { it.score >= 4 }) C.ok else C.gold), modifier = Modifier.padding(top = 6.dp))
            }
            passed.forEach { e -> Row2(e.title.ifBlank { d.subById[e.subjectId]?.name.orEmpty() }, "${Words.examKind(e.kind)} · ${fmtDayShort(e.day)}",
                if (e.kind == Exam.OGE || e.kind == Exam.EGE || e.maxScore > 0) fmt1(e.score) else Marks.show(if (e.kind == Exam.CREDIT) "pass" else d.s.scale, e.score)) { nav.push(Screen.ExamInfo(e.id)) } }
            Ghost(tr("Поделиться зачёткой", "Share the record book"), Modifier.fillMaxWidth().padding(top = 10.dp)) { nav.share = app.inkling.ui.share.ShareReq(app.inkling.ui.share.CardKind.BOOK, 0) }
        }
        Spacer(Modifier.height(24.dp))
    }
    if (add) GradeSheet(d) { add = false }
}
