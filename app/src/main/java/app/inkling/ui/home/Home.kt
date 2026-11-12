package app.inkling.ui.home

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.inkling.container
import app.inkling.data.Attend
import app.inkling.data.Slot
import app.inkling.domain.Feast
import app.inkling.domain.Quests
import app.inkling.domain.Timetable
import app.inkling.domain.Words
import app.inkling.domain.dayShort
import app.inkling.domain.dur
import app.inkling.domain.hm
import app.inkling.domain.inDays
import app.inkling.domain.nowMin
import app.inkling.domain.plural
import app.inkling.domain.tr
import app.inkling.domain.weekStart
import app.inkling.ui.Data
import app.inkling.ui.Nav
import app.inkling.ui.Screen
import app.inkling.ui.art.Mini
import app.inkling.ui.art.Pose
import app.inkling.ui.art.Prop
import app.inkling.ui.art.Sky
import app.inkling.ui.art.TowerScene
import app.inkling.ui.components.Icon
import app.inkling.ui.components.Icons
import app.inkling.ui.components.ManaPill
import app.inkling.ui.components.Sheet
import app.inkling.ui.components.card
import app.inkling.ui.components.subjectColor
import app.inkling.ui.theme.C
import app.inkling.ui.theme.T

/**
 * The tower on one screen: the room, what's on now in the timetable, the candle, the one thing waiting,
 * the week's quests, the doors to the grimoire, the chest and the wand, and the week in bars.
 */
@Composable
fun HomeScreen(d: Data, nav: Nav) {
    val s = d.s
    var quests by remember { mutableStateOf(false) }
    var after by remember { mutableStateOf<Slot?>(null) }   // the class just marked «was there»: «what was set?» opens for it
    val todayMin = d.studies.filter { it.day == d.today }.sumOf { it.minutes }
    val lastDay = d.studies.maxOfOrNull { it.day } ?: d.today
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Башня", "The tower"), style = T.title)
                val term = Timetable.term(s, d.today)
                Text(when {
                    d.today in s.sick -> tr("Лазарет – отдыхайте", "The infirmary – rest")
                    s.terms.isNotEmpty() && term == null -> "${Words.year(s)} · ${tr("каникулы", "holidays")}"
                    todayMin > 0 -> "${Words.year(s)} · ${tr("сегодня", "today")} ${dur(todayMin * 60)}"
                    else -> Words.year(s)
                }, style = T.small)
            }
            ManaPill(s.mana) { nav.push(Screen.Shop) }
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(40.dp).clip(CircleShape).clickable { nav.push(Screen.Settings) }, contentAlignment = Alignment.Center) { Icon(Icons.GEAR, C.ink, 22.dp) }
        }
        Box(Modifier.fillMaxWidth().card(22.dp)) {
            val pose = when {
                s.candle.start > 0 -> Pose.WORK
                d.today in s.sick || d.today - lastDay >= 4 -> Pose.SLEEP
                todayMin > 0 -> Pose.CHEER
                else -> Pose.IDLE
            }
            TowerScene(d.team, s.items - s.hidden, pose, sky = Sky.of(s), spines = d.finished.map { subjectColor(d.subById[it.subjectId]) },
                candle = s.candle.start > 0) { nav.push(Screen.PetInfo(it.id)) }
        }
        Feast.on(d.today)?.let { f ->
            Row(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.goldSoft).padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Prop("star", 24.dp); Spacer(Modifier.width(8.dp))
                Column { Text(f.title, style = T.smallB); Text(f.text, style = T.tiny.copy(color = C.ink)) }
            }
        }
        NowLine(d, nav)

        // the candle: the main thing to do here
        Row(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.acc).clickable { nav.push(Screen.Candle) }.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Prop(if (s.candle.start > 0) "candle" else "stub", 36.dp); Spacer(Modifier.width(10.dp))
            Text(if (s.candle.start > 0) tr("Свеча горит – вернуться", "The candle is burning – go back") else tr("Зажечь свечу", "Light a candle"), style = T.btn.copy(color = C.accInk), modifier = Modifier.weight(1f))
            Icon(Icons.NEXT, C.accInk, 20.dp)
        }

        Waiting(d, nav) { after = it }

        val qs = s.quests.mapNotNull { Quests.byId[it] }
        if (qs.isNotEmpty()) {
            val next = qs.firstOrNull { it.id !in s.questsDone }
            Row(Modifier.padding(top = 10.dp).fillMaxWidth().card(18.dp).clickable { quests = true }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Prop("medal", 32.dp); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(next?.title ?: tr("Задания недели выполнены", "This week's quests are done"), style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
                    qs.forEach { q -> Box(Modifier.size(9.dp).clip(CircleShape).background(if (q.id in s.questsDone) C.ok else C.track)) }
                }
                Icon(Icons.NEXT, C.mute, 20.dp)
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Door("book", tr("Гримуар", "Grimoire"), Modifier.weight(1f), d.due.size.takeIf { it > 0 }?.toString(), hot = d.due.isNotEmpty()) { nav.push(Screen.Grimoire()) }
            Door("orb", tr("Сундук", "Chest"), Modifier.weight(1f)) { nav.push(Screen.Chest) }
            Door("", tr("Научи", "Teach"), Modifier.weight(1f), species = "raven") { nav.push(Screen.Teach()) }
            Door("wand", tr("Палочка", "Wand"), Modifier.weight(1f)) { nav.push(Screen.Wand) }
            Door("books", tr("Предметы", "Subjects"), Modifier.weight(1f)) { nav.push(Screen.Subjects) }
        }

        Text(tr("Эта неделя", "This week") + (d.studies.filter { it.day >= weekStart(d.today) }.sumOf { it.minutes }.takeIf { it > 0 }?.let { " · " + dur(it * 60) } ?: ""), style = T.sec, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
        Week(d)
        Spacer(Modifier.height(24.dp))
    }

    // the sheets fill the screen, so they live outside the scrolling column (inside it they get an endless height and crash)
    after?.let { sl -> AfterClassSheet(sl, d, nav) { after = null } }
    if (satchel) SatchelSheet(d) { satchel = false }
    if (quests) Sheet({ quests = false }) {
        Text(tr("Задания недели", "This week's quests"), style = T.h2)
        Text(tr("Новые – каждый понедельник. За них дают ману.", "New ones every Monday. They bring mana."), style = T.small, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        s.quests.mapNotNull { Quests.byId[it] }.forEach { q ->
            val done = q.id in s.questsDone
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(if (done) C.ok else C.card2), contentAlignment = Alignment.Center) { if (done) Icon(Icons.CHECK, C.accInk, 16.dp) }
                Spacer(Modifier.width(10.dp))
                Text(q.title, style = if (done) T.body.copy(color = C.mute) else T.body, modifier = Modifier.weight(1f))
                Text("+${q.mana}", style = T.smallB.copy(color = C.mana))
            }
        }
    }
}

/** What's on in the timetable now, or next, in one line. */
@Composable
private fun NowLine(d: Data, nav: Nav) {
    val s = d.s
    if (s.self || d.slots.isEmpty()) return
    val min = nowMin()
    val (now, next) = Timetable.nowNext(s, d.slots, d.today, min)
    val (sl, line) = when {
        now != null -> now to tr("Сейчас · до конца ${now.end - min} мин", "Now · ${now.end - min} min left")
        next != null -> next to tr("Дальше в ${hm(next.start)}", "Next at ${hm(next.start)}")
        else -> {
            val tomorrow = Timetable.on(s, d.slots, d.today + 1).firstOrNull()
            tomorrow to if (tomorrow != null) tr("Завтра первым в ${hm(tomorrow.start)}", "Tomorrow first at ${hm(tomorrow.start)}") else tr("Сегодня больше ничего нет", "Nothing more today")
        }
    }
    Row(Modifier.padding(top = 10.dp).fillMaxWidth().card(18.dp).clickable { nav.push(Screen.Timetable) }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        val sub = sl?.let { d.subById[it.subjectId] }
        Box(Modifier.size(10.dp, 34.dp).clip(RoundedCornerShape(50)).background(if (sub != null) subjectColor(sub) else C.track))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(line, style = T.tiny)
            Text(if (sl == null || sub == null) tr("Свободное время – можно зажечь свечу", "Free time – light a candle") else
                listOfNotNull(sub.name, Words.slotKind(sl.kind).takeIf { sl.kind != Slot.LESSON }, sl.room.takeIf { it.isNotBlank() }?.let { tr("ауд. $it", "room $it") }).joinToString(" · "),
                style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.CAL, C.mute, 20.dp)
    }
}

/** At most one card asks for attention: a class to mark, the sphinx, the satchel, a deadline, a trial. */
@Composable
private fun Waiting(d: Data, nav: Nav, onWas: (Slot) -> Unit) {
    val c = LocalContext.current.container
    val s = d.s
    val min = nowMin()
    val unmarked = if (s.self) emptyList() else Timetable.unmarked(s, d.slots, d.attends, d.today, min)
    val tomorrow = Timetable.on(s, d.slots, d.today + 1)
    val soonTask = d.openTasks.firstOrNull { it.due in d.today..(d.today + 2) }
    val soonExam = d.upcoming.firstOrNull { it.day - d.today <= 14 }
    when {
        unmarked.isNotEmpty() -> { val sl = unmarked.last(); val sub = d.subById[sl.subjectId]
            Column(Modifier.padding(top = 10.dp).fillMaxWidth().card(18.dp).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Mini("gargoyle", 40.dp); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (s.school) tr("Были на уроке?", "Were you at the lesson?") else tr("Были на паре?", "Were you at the class?"), style = T.tiny)
                        Text("${sub?.name ?: "–"} · ${hm(sl.start)}", style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Attend.WAS to tr("Был(а)", "Yes"), Attend.MISSED to tr("Пропуск", "Missed"), Attend.CANCELLED to tr("Отменили", "Cancelled")).forEach { (st, l) ->
                        val col = when (st) { Attend.WAS -> C.ok; Attend.MISSED -> C.bad; else -> C.mute }
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(col.copy(alpha = .15f)).clickable {
                            c.write { attend(sl, d.today, st) }; if (st == Attend.WAS && s.afterClass) onWas(sl)
                        }.padding(vertical = 11.dp), contentAlignment = Alignment.Center) { Text(l, style = T.bodyB.copy(color = col)) }
                    }
                }
            }
        }
        d.due.isNotEmpty() -> Card1("sphinx", tr("Сфинкс ждёт с загадками", "The sphinx is waiting with riddles"),
            tr("${d.due.size} ${plural(d.due.size.toLong(), "заклинание пора вспомнить", "заклинания пора вспомнить", "заклинаний пора вспомнить")}", "${d.due.size} spells to recall")) { nav.push(Screen.Grimoire(true)) }
        s.school && s.satchel && min >= 17 * 60 && tomorrow.isNotEmpty() && (d.today + 1) !in s.packed -> Card1("hob", tr("Домовой собирает портфель", "The hob is packing the satchel"),
            tr("Завтра ${tomorrow.size} ${plural(tomorrow.size.toLong(), "урок", "урока", "уроков")} – проверим, всё ли взяли?", "${tomorrow.size} lessons tomorrow – got everything?")) { satchel = true }
        soonTask != null -> Card1("owl", tr("Письмо от совы", "A letter from the owl"),
            "${soonTask.title.ifBlank { Words.taskKind(soonTask.kind) }} · ${d.subById[soonTask.subjectId]?.name.orEmpty()} · ${inDays(soonTask.due - d.today)}") { nav.push(Screen.TaskInfo(soonTask.id)) }
        soonExam != null -> Card1("phoenix", "${Words.examKind(soonExam.kind)}: ${soonExam.title.ifBlank { d.subById[soonExam.subjectId]?.name.orEmpty() }}",
            inDays(soonExam.day - d.today).let { it.replaceFirstChar { ch -> ch.uppercase() } } + d.tickets.filter { it.examId == soonExam.id }.let { t -> if (t.isEmpty()) "" else tr(" · выучено ${t.count { it.level >= 2 }} из ${t.size}", " · ${t.count { it.level >= 2 }} of ${t.size} learned") }) { nav.push(Screen.ExamInfo(soonExam.id)) }
    }
}

private var satchel by mutableStateOf(false)

@Composable
private fun Card1(species: String, title: String, line: String, onClick: () -> Unit) {
    Row(Modifier.padding(top = 10.dp).fillMaxWidth().card(18.dp).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Mini(species, 40.dp); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(line, style = T.small, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.NEXT, C.mute, 20.dp)
    }
}

/** A door: a picture over a word and a number. */
@Composable
private fun Door(prop: String, label: String, modifier: Modifier = Modifier, value: String? = null, species: String? = null, hot: Boolean = false, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(if (hot) C.accSoft else C.card).clickable(onClick = onClick).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (species != null) Mini(species, 34.dp) else Prop(prop, 34.dp)
        Text(label, style = T.tiny.copy(color = C.ink), maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
        if (value != null) Text(value, style = T.tiny.copy(color = C.acc), maxLines = 1, textAlign = TextAlign.Center)
    }
}

/** Monday to Sunday: minutes studied, coloured by the day's main school. */
@Composable
private fun Week(d: Data) {
    val monday = weekStart(d.today)
    val byDay = d.studies.filter { it.day in monday..(monday + 6) }.groupBy { it.day }
    val max = (byDay.values.maxOfOrNull { l -> l.sumOf { it.minutes } } ?: 60).coerceAtLeast(60)
    Row(Modifier.fillMaxWidth().card(18.dp).padding(horizontal = 10.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until 7) {
            val day = monday + i; val l = byDay[day].orEmpty(); val v = l.sumOf { it.minutes }
            val main = l.groupBy { it.subjectId }.maxByOrNull { e -> e.value.sumOf { it.minutes } }?.key
            Column(Modifier.weight(1f).alpha(if (day > d.today) .35f else 1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.height(52.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    if (day in d.s.sick) Icon(Icons.CROSS, C.bad, 16.dp)
                    else Box(Modifier.width(14.dp).height((4 + 48f * v / max).dp).clip(RoundedCornerShape(50)).background(if (v == 0) C.track else subjectColor(d.subById[main])))
                }
                Text(dayShort(i), style = T.tiny.copy(color = if (day == d.today) C.acc else C.mute), modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

