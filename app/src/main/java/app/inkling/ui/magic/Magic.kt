package app.inkling.ui.magic

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.inkling.container
import app.inkling.data.Pet
import app.inkling.data.Question
import app.inkling.data.Spell
import app.inkling.data.Study
import app.inkling.domain.Engine
import app.inkling.domain.Lang
import app.inkling.domain.Memory
import app.inkling.domain.Pets
import app.inkling.domain.Schools
import app.inkling.domain.Words
import app.inkling.domain.count
import app.inkling.domain.dayOf
import app.inkling.domain.dur
import app.inkling.domain.fmtDayShort
import app.inkling.domain.hm
import app.inkling.domain.relDay
import app.inkling.domain.tr
import app.inkling.ui.Data
import app.inkling.ui.Nav
import app.inkling.ui.Screen
import app.inkling.ui.art.Mini
import app.inkling.ui.art.PetView
import app.inkling.ui.art.Pose
import app.inkling.ui.art.Prop
import app.inkling.ui.components.Btn
import app.inkling.ui.components.Chip
import app.inkling.ui.components.DayPicker
import app.inkling.ui.components.Empty
import app.inkling.ui.components.Field
import app.inkling.ui.components.Ghost
import app.inkling.ui.components.Icon
import app.inkling.ui.components.Icons
import app.inkling.ui.components.Page
import app.inkling.ui.components.RecallRow
import app.inkling.ui.components.Row2
import app.inkling.ui.components.Section
import app.inkling.ui.components.Sheet
import app.inkling.ui.components.SubjectPicker
import app.inkling.ui.components.TileGrid
import app.inkling.ui.components.TimeChip
import app.inkling.ui.components.card
import app.inkling.ui.components.schoolColor
import app.inkling.ui.components.subjectColor
import app.inkling.ui.theme.C
import app.inkling.ui.theme.T
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// ---------------- a study written by hand ----------------

@Composable
fun StudyPage(scr: Screen.StudyEdit, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val old = d.studies.firstOrNull { it.id == scr.id }
    if (scr.id != 0L && old == null && d.studies.isNotEmpty()) { nav.pop(); return }
    var sub by remember { mutableLongStateOf(old?.subjectId ?: scr.subjectId.takeIf { it != 0L } ?: d.active.firstOrNull()?.id ?: 0) }
    androidx.compose.runtime.LaunchedEffect(d.active.size) { if (sub == 0L) sub = d.active.firstOrNull()?.id ?: 0L }
    var day by remember { mutableLongStateOf(old?.day ?: d.today) }
    var minutes by remember { mutableIntStateOf(old?.minutes ?: 45) }
    var at by remember { mutableIntStateOf(old?.let { Instant.ofEpochMilli(it.start).atZone(ZoneId.systemDefault()).let { z -> z.hour * 60 + z.minute } } ?: 18 * 60) }
    var mat by remember { mutableLongStateOf(old?.materialId ?: 0) }
    var units by remember { mutableIntStateOf(old?.units ?: 0) }
    var note by remember { mutableStateOf(old?.note ?: "") }
    var spell by remember { mutableStateOf("") }
    Page(if (old == null) tr("Занятие", "Study") else tr("Занятие", "Study"), nav::pop) {
        Text(tr("Учились без свечи – на паре, в библиотеке, с репетитором? Запишите, и существа получат своё.",
            "Studied without the candle – in class, in the library, with a tutor? Write it down and your creatures get their share."), style = T.small)
        Text(tr("Предмет", "Subject"), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
        SubjectPicker(d.active, sub) { sub = it; mat = 0 }
        Text(tr("Сколько минут", "Minutes"), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(15, 30, 45, 60, 90, 120).forEach { m -> Chip("$m", minutes == m, Modifier.weight(1f)) { minutes = m } } }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) { Stepper(minutes, 5) { minutes = it.coerceAtLeast(1) } }
        Text(tr("Когда", "When"), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
        DayPicker(day, d.today) { day = it }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) { Text(tr("начало в ", "started at "), style = T.small); TimeChip(at) { at = it } }
        val mats = d.materials.filter { it.subjectId == sub && (it.finished == 0L || it.id == mat) }
        if (mats.isNotEmpty()) {
            Text(tr("Материал", "Material"), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Chip(tr("нет", "none"), mat == 0L) { mat = 0 }
                mats.take(3).forEach { m -> Chip(m.title, mat == m.id) { mat = m.id } }
            }
            if (mat != 0L) { val m = mats.first { it.id == mat }
                Text(Words.unitName(m.unit), style = T.tiny, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                Stepper(units, if (m.unit == "pages") 5 else 1) { units = it } }
        }
        Field(note, { note = it }, tr("Что делали (по желанию)", "What you did (optional)"), modifier = Modifier.padding(top = 14.dp), single = false)
        if (old == null) Field(spell, { spell = it }, tr("Что запомнили – для гримуара", "What stuck – for the grimoire"), modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(16.dp))
        Btn(tr("Записать", "Save"), Modifier.fillMaxWidth(), enabled = sub != 0L) {
            val start = LocalDate.ofEpochDay(day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + at * 60_000L
            val st = Study(id = old?.id ?: 0, subjectId = sub, day = day, start = start, minutes = minutes, kind = old?.kind ?: Study.MANUAL, materialId = mat, units = if (mat != 0L) units else 0, note = note.trim(),
                focus = old?.focus ?: 0, away = old?.away ?: 0)
            c.write { if (old == null) { addStudy(st); if (spell.isNotBlank()) addSpell(sub, spell, "") } else updateStudy(st) }
            nav.pop()
        }
        if (old != null) { Spacer(Modifier.height(8.dp)); Ghost(tr("Удалить", "Delete"), Modifier.fillMaxWidth(), color = C.bad) { c.write { deleteStudy(old.id) }; nav.pop() } }
    }
}

// ---------------- teach the familiar ----------------

private val PROMPTS = listOf(
    "О чём это – в одной фразе?" to "What is it about – in one sentence?",
    "Почему это так? Откуда это берётся?" to "Why is it so? Where does it come from?",
    "Приведите пример – лучше свой, не из учебника." to "Give an example – your own, not the textbook's.",
    "С чем это легко спутать? Чем отличается?" to "What is it easily confused with? How is it different?",
    "А теперь так, чтобы понял пятиклассник." to "Now so that a ten-year-old would get it.",
)

/** The Feynman way: explain a topic to a creature that keeps asking; where words fail is what to learn next. */
@Composable
fun TeachPage(subjectId: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val started = remember { System.currentTimeMillis() }
    var sub by remember { mutableLongStateOf(subjectId.takeIf { it != 0L } ?: d.active.firstOrNull()?.id ?: 0) }
    androidx.compose.runtime.LaunchedEffect(d.active.size) { if (sub == 0L) sub = d.active.firstOrNull()?.id ?: 0L }
    var topic by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(-1) }
    val answers = remember { mutableStateListOf("", "", "", "", "") }
    var gaps by remember { mutableStateOf(false) }
    val teacher: Pet? = d.pets.firstOrNull { it.species == "raven" && it.hatched > 0 } ?: d.leader
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK && step in 0..4) r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { said ->
            answers[step] = (answers[step] + " " + said).trim() } }
    Page(tr("Научи фамильяра", "Teach the familiar"), nav::pop) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (teacher != null) PetView(teacher, 120.dp, if (step >= 5) Pose.CHEER else Pose.IDLE) else Mini("raven", 120.dp)
            Column(Modifier.weight(1f).padding(start = 6.dp, bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(C.card).padding(12.dp)) {
                Text(when {
                    step < 0 -> tr("Расскажите мне что-нибудь из учёбы. Я буду переспрашивать – так лучше всего понимают сами объясняющие.", "Tell me something you study. I'll keep asking – explaining is the best way to understand.")
                    step < 5 -> tr(PROMPTS[step].first, PROMPTS[step].second)
                    else -> tr("Спасибо, теперь и я понимаю! Где было трудно объяснить?", "Thank you, now I understand too! Where was it hard to explain?")
                }, style = T.body)
            }
        }
        if (step < 0) {
            Text(tr("Предмет", "Subject"), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
            SubjectPicker(d.active, sub) { sub = it }
            Field(topic, { topic = it }, tr("Тема: «Закон Ома», «Present Perfect»…", "Topic: «Ohm's law», «Present Perfect»…"), modifier = Modifier.padding(top = 12.dp))
            Spacer(Modifier.height(14.dp))
            Btn(tr("Начать объяснять", "Start explaining"), Modifier.fillMaxWidth(), enabled = topic.isNotBlank() && sub != 0L) { step = 0 }
        } else if (step < 5) {
            Text(topic, style = T.h3, modifier = Modifier.padding(top = 10.dp))
            Text(tr("Вопрос ${step + 1} из 5", "Question ${step + 1} of 5"), style = T.tiny, modifier = Modifier.padding(bottom = 8.dp))
            Field(answers[step], { answers[step] = it }, tr("Своими словами…", "In your own words…"), single = false)
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.clip(RoundedCornerShape(50)).background(C.card2).clickable {
                    runCatching { voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (Lang.ru) "ru-RU" else "en-US")) }
                }.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.MIC, C.ink, 16.dp); Spacer(Modifier.width(6.dp)); Text(tr("Сказать вслух", "Say it aloud"), style = T.smallB)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Ghost(tr("Не знаю", "Don't know"), Modifier.weight(1f)) { answers[step] = answers[step].ifBlank { "?" }; gaps = true; step++ }
                Btn(tr("Дальше", "Next"), Modifier.weight(1.4f)) { step++ }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(tr("Всё гладко", "All smooth"), !gaps, Modifier.weight(1f)) { gaps = false }
                Chip(tr("Были пробелы", "There were gaps"), gaps, Modifier.weight(1f)) { gaps = true }
            }
            if (gaps) Text(tr("Мимик положит тему в сундук как вопрос – вернётесь к ней.", "The mimic puts the topic in the chest as a question – you'll come back to it."), style = T.small, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(14.dp))
            Btn(tr("Готово", "Done"), Modifier.fillMaxWidth()) {
                val min = ((System.currentTimeMillis() - started) / 60_000).toInt().coerceAtLeast(5)
                val note = PROMPTS.indices.filter { answers[it].isNotBlank() }.joinToString("\n") { "${tr(PROMPTS[it].first, PROMPTS[it].second)} ${answers[it]}" }
                c.write {
                    addStudy(Study(subjectId = sub, day = clock(), start = now() - min * 60_000L, minutes = min, kind = Study.TEACH, note = "$topic\n$note"))
                    addSpell(sub, topic, answers[0].takeIf { it != "?" }.orEmpty())
                    if (gaps) addQuestion(sub, tr("Разобраться до конца: $topic", "Get to the bottom of: $topic"))
                    say(tr("Ворон-эхо запомнил объяснение", "The echo raven remembered your explanation"))
                }
                nav.pop()
            }
        }
        val past = d.studies.filter { it.kind == Study.TEACH }.take(5)
        if (step < 0 && past.isNotEmpty()) {
            Section(tr("Уже объясняли", "Explained before"))
            past.forEach { st -> Row2(st.note.lineSequence().firstOrNull().orEmpty(), "${d.subById[st.subjectId]?.name.orEmpty()} · ${relDay(st.day, d.today)}") { nav.push(Screen.StudyEdit(st.id)) } }
        }
    }
}

// ---------------- the grimoire ----------------

/** The grimoire: spells by subject, glowing by how well they're known; the sphinx asks the due ones. */
@Composable
fun GrimoirePage(quiz0: Boolean, d: Data, nav: Nav) {
    var quiz by remember { mutableStateOf(quiz0 && d.due.isNotEmpty()) }
    var add by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf<Spell?>(null) }
    var filter by remember { mutableLongStateOf(0) }
    if (quiz) { Quiz(d) { quiz = false }; return }
    val m = Memory(d.spells, d.today)
    Page(tr("Гримуар", "The grimoire"), nav::pop, action = { Box(Modifier.size(40.dp).clip(CircleShape).clickable { add = true }, contentAlignment = Alignment.Center) { Icon(Icons.PLUS, C.ink, 22.dp) } }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Mini("sphinx", 64.dp); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(count(m.total, "заклинание", "заклинания", "заклинаний", "spell", "spells"), style = T.h3)
                Text(tr("выучено ${m.learned} · вспомнили ${(m.rate * 100).toInt()}% раз", "${m.learned} learned · recalled ${(m.rate * 100).toInt()}% of the time"), style = T.small)
            }
        }
        if (d.due.isNotEmpty()) Btn(tr("Сфинкс спрашивает: ${d.due.size}", "The sphinx asks: ${d.due.size}"), Modifier.fillMaxWidth().padding(top = 12.dp)) { quiz = true }
        else Text(tr("Сегодня сфинкс спокоен – все заклинания свежи.", "The sphinx is calm today – every spell is fresh."), style = T.small, modifier = Modifier.padding(top = 10.dp))
        // the steps: how many spells stand where on the way to learned
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val labels = listOf(tr("новые", "new"), "3", "7", "21", "60", "✦")
            m.steps.forEachIndexed { i, n -> Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(C.accSoft.copy(alpha = .3f + .7f * i / m.steps.size)).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$n", style = T.smallB); Text(labels.getOrElse(i) { "" }, style = T.tiny) } }
        }
        Text(tr("Ступени: через сколько дней сфинкс спросит снова", "Steps: in how many days the sphinx asks again"), style = T.tiny, modifier = Modifier.padding(top = 4.dp))
        val subs = d.spells.map { it.subjectId }.distinct().mapNotNull { d.subById[it] }
        if (subs.size > 1) { Spacer(Modifier.height(12.dp)); SubjectPicker(subs, filter, allowNone = false) { filter = if (filter == it) 0 else it } }
        val list = d.spells.filter { filter == 0L || it.subjectId == filter }.sortedByDescending { it.created }
        if (list.isEmpty()) Empty(tr("Пусто. После урока запишите, что запомнили, – это и есть заклинание.", "Empty. After a lesson write down what stuck – that's a spell."))
        list.groupBy { it.subjectId }.forEach { (sid, l) ->
            Section(d.subById[sid]?.name ?: tr("Без предмета", "No subject"))
            l.forEach { sp -> SpellRow(sp, d) { edit = sp } }
        }
        Spacer(Modifier.height(20.dp))
    }
    if (add) SpellSheet(d, filter) { add = false }
    edit?.let { sp -> EditSpell(sp) { edit = null } }
}

@Composable
private fun SpellRow(sp: Spell, d: Data, onClick: () -> Unit) {
    val learned = Engine.learned(sp)
    val glow = (sp.step.toFloat() / Engine.STEPS.size).coerceIn(0f, 1f)
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp)).background(if (learned) C.goldSoft else C.card).clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(Brush.radialGradient(listOf((if (learned) C.gold else schoolColor(d.school(sp.subjectId))).copy(alpha = .25f + .7f * glow), Color.Transparent))),
            contentAlignment = Alignment.Center) { Icon(Icons.SPARK, if (learned) C.gold else schoolColor(d.school(sp.subjectId)).copy(alpha = .4f + .6f * glow), 18.dp) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(sp.title, style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(when { learned -> tr("выучено", "learned"); sp.due <= d.today -> tr("пора вспомнить", "time to recall"); else -> tr("сфинкс спросит ${relDay(sp.due, d.today)}", "the sphinx asks ${relDay(sp.due, d.today)}") },
                style = T.tiny.copy(color = if (sp.due <= d.today && !learned) C.acc else C.mute))
        }
        if (sp.last == 3) Text("↺", style = T.smallB.copy(color = C.bad))
    }
}

@Composable
private fun EditSpell(sp: Spell, onDone: () -> Unit) {
    val c = LocalContext.current.container
    var title by remember { mutableStateOf(sp.title) }
    var text by remember { mutableStateOf(sp.text) }
    Sheet(onDone) {
        Text(tr("Заклинание", "Spell"), style = T.h2)
        Text(tr("Записано ${fmtDayShort(sp.created)} · спрашивали ${sp.asked} · вспомнили ${sp.good}", "Written ${fmtDayShort(sp.created)} · asked ${sp.asked} · recalled ${sp.good}"), style = T.small, modifier = Modifier.padding(bottom = 10.dp))
        Field(title, { title = it }, tr("Тема или вопрос", "Topic or question"))
        Spacer(Modifier.height(8.dp))
        Field(text, { text = it }, tr("Суть своими словами", "The gist in your own words"), single = false)
        Spacer(Modifier.height(14.dp))
        Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth(), enabled = title.isNotBlank()) { c.write { saveSpell(sp.copy(title = title.trim(), text = text.trim())) }; onDone() }
        Spacer(Modifier.height(8.dp))
        Ghost(tr("Удалить", "Delete"), Modifier.fillMaxWidth(), color = C.bad) { c.write { deleteSpell(sp.id) }; onDone() }
    }
}

/** The sphinx's riddles: the name of a spell; recall it, reveal the gist, answer honestly. */
@Composable
private fun Quiz(d: Data, onDone: () -> Unit) {
    val c = LocalContext.current.container
    val queue = remember { d.due.shuffled().toMutableList() }
    var i by remember { mutableIntStateOf(0) }
    var shown by remember { mutableStateOf(false) }
    var good by remember { mutableIntStateOf(0) }
    Page(tr("Загадки сфинкса", "The sphinx's riddles"), onDone) {
        if (i >= queue.size) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Mini("sphinx", 160.dp) }
            Text(tr("Всё на сегодня!", "That's all for today!"), style = T.h2, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text(tr("Вспомнили $good из ${queue.size}. Забытое вернётся завтра – так и запоминают.", "Recalled $good of ${queue.size}. What slipped comes back tomorrow – that's how memory works."),
                style = T.body.copy(color = C.mute), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            Spacer(Modifier.height(18.dp))
            Btn(tr("Закрыть гримуар", "Close the grimoire"), Modifier.fillMaxWidth(), onClick = onDone)
            return@Page
        }
        val sp = queue[i]
        Text("${i + 1} / ${queue.size}", style = T.tiny)
        Column(Modifier.fillMaxWidth().padding(top = 8.dp).card(22.dp).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(d.subById[sp.subjectId]?.name.orEmpty(), style = T.small.copy(color = subjectColor(d.subById[sp.subjectId])))
            Text(sp.title, style = T.h2, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
            if (!shown) Text(tr("Вспомните суть. Скажите про себя или вслух – потом откройте.", "Recall the gist. Say it to yourself or aloud – then reveal."), style = T.small, textAlign = TextAlign.Center)
            else Text(sp.text.ifBlank { tr("Суть не записана – сверьтесь с конспектом.", "No gist written – check your notes.") }, style = T.body, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        if (!shown) Btn(tr("Открыть", "Reveal"), Modifier.fillMaxWidth()) { shown = true }
        else RecallRow { a -> c.write { answerSpell(sp, a) }; if (a == 1) good++; shown = false; i++ }
    }
}

// ---------------- the mimic's chest ----------------

@Composable
fun ChestPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var add by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<Question?>(null) }
    Page(tr("Сундук мимика", "The mimic's chest"), nav::pop, action = { Box(Modifier.size(40.dp).clip(CircleShape).clickable { add = true }, contentAlignment = Alignment.Center) { Icon(Icons.PLUS, C.ink, 22.dp) } }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Mini("mimic", 64.dp); Spacer(Modifier.width(10.dp))
            Text(tr("Сюда – всё, что непонятно. Вопрос без ответа – не беда, а начало. Нашли ответ – мимик выплюнет его кристаллом.",
                "Everything unclear goes here. An open question is a beginning, not a failure. Find the answer and the mimic spits out a crystal."), style = T.small, modifier = Modifier.weight(1f))
        }
        val openQ = d.questions.filter { it.answered == 0L }; val done = d.questions.filter { it.answered > 0L }
        Section(tr("Ждут ответа · ${openQ.size}", "Waiting · ${openQ.size}"))
        if (openQ.isEmpty()) Empty(tr("Вопросов нет. Любопытство – тоже стат!", "No questions. Curiosity is a stat too!"))
        openQ.forEach { q -> QRow(q, d) { open = q } }
        if (done.isNotEmpty()) {
            Section(tr("Кристаллы ответов · ${done.size}", "Answer crystals · ${done.size}"))
            done.forEach { q -> QRow(q, d) { open = q } }
        }
        Spacer(Modifier.height(20.dp))
    }
    if (add) QuestionSheet(d) { add = false }
    open?.let { q ->
        var ans by remember(q.id) { mutableStateOf(q.answer) }
        Sheet({ open = null }) {
            Text(q.text, style = T.h3)
            Text("${d.subById[q.subjectId]?.name ?: ""} · ${fmtDayShort(q.created)}", style = T.tiny, modifier = Modifier.padding(bottom = 10.dp))
            Field(ans, { ans = it }, tr("Ответ, который нашли", "The answer you found"), single = false)
            Spacer(Modifier.height(14.dp))
            Btn(if (q.answered == 0L) tr("Ответ найден", "Answer found") else tr("Сохранить", "Save"), Modifier.fillMaxWidth(), enabled = ans.isNotBlank()) { c.write { answerQuestion(q, ans) }; open = null }
            Spacer(Modifier.height(8.dp))
            Ghost(tr("Удалить", "Delete"), Modifier.fillMaxWidth(), color = C.bad) { c.write { deleteQuestion(q.id) }; open = null }
        }
    }
}

@Composable
private fun QRow(q: Question, d: Data, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp)).background(C.card).clickable(onClick = onClick).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (q.answered > 0) Prop("orb", 30.dp) else Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) { Icon(Icons.QUESTION, schoolColor(d.school(q.subjectId)), 22.dp) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(q.text, style = T.bodyB, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(if (q.answered > 0) q.answer else d.subById[q.subjectId]?.name ?: "", style = T.tiny, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ---------------- the wand ----------------

/** What the wand is made of, read off how one studies. */
class WandSpec(d: Data) {
    private val bySchool = d.studies.groupBy { d.school(it.subjectId) }.mapValues { e -> e.value.sumOf { it.minutes } }
    val school = Schools.of(bySchool.maxByOrNull { it.value }?.key ?: (d.active.firstOrNull()?.school ?: "own"))
    val second = bySchool.entries.sortedByDescending { it.value }.getOrNull(1)?.key?.let { Schools.of(it) }
    val core = d.leader?.species ?: "inkling"
    val hours = d.studies.sumOf { it.minutes } / 60
    val length = (18 + hours / 8).coerceAtMost(38)
    val carvings = d.finished.size
    val runes = d.spells.count { Engine.learned(it) }
    private val candles = d.studies.filter { it.kind == Study.CANDLE }
    val clean = if (candles.isEmpty()) 0f else candles.count { it.away == 0 }.toFloat() / candles.size
    val flex get() = when { clean >= .8f -> tr("несгибаемая", "unyielding"); clean >= .5f -> tr("упругая", "springy"); candles.isEmpty() -> tr("ещё не испытана", "not yet tested"); else -> tr("гибкая", "supple") }
    val coreName get() = coreName(core)
}

fun coreName(sp: String) = when (sp) {
    "inkling" -> tr("капля живых чернил", "a drop of living ink"); "sphinx" -> tr("песчинка сфинкса", "a sphinx's grain of sand"); "salamander" -> tr("уголёк саламандры", "a salamander's ember")
    "golem" -> tr("руна голема", "a golem's rune"); "pixie" -> tr("пыльца пикси", "pixie dust"); "chimera" -> tr("волос химеры", "a chimera's hair"); "hydra" -> tr("чешуйка гидры", "a hydra's scale")
    "moonhare" -> tr("лунный пух", "moon down"); "griffin" -> tr("перо грифона", "a griffin's feather"); "kitsune" -> tr("лисий огонёк", "a foxfire spark"); "mimic" -> tr("язычок мимика", "a mimic's tongue")
    "bookwyrm" -> tr("закладка книжного змея", "a bookwyrm's bookmark"); "raven" -> tr("перо ворона-эха", "an echo raven's feather"); "mandrake" -> tr("корень мандрагоры", "a mandrake root")
    "kraken" -> tr("чернила кракена", "kraken ink"); "shade" -> tr("лоскут тени", "a scrap of shadow"); "owl" -> tr("сургуч совы", "the owl's sealing wax"); "gargoyle" -> tr("крошка гаргульи", "a gargoyle's chip")
    "hob" -> tr("нить домового", "a hob's thread"); "magpie" -> tr("звёздочка сороки", "a magpie's star"); "cat" -> tr("ус кота-фамильяра", "a familiar's whisker")
    "dragon" -> tr("жила дракона", "dragon heartstring"); "phoenix" -> tr("перо феникса", "a phoenix feather"); else -> tr("волос единорога", "unicorn hair")
}

@Composable
fun WandPage(d: Data, nav: Nav) {
    val w = WandSpec(d)
    Page(tr("Ваша палочка", "Your wand"), nav::pop, action = { Box(Modifier.size(40.dp).clip(CircleShape).clickable { nav.share = app.inkling.ui.share.ShareReq(app.inkling.ui.share.CardKind.WAND, 0) }, contentAlignment = Alignment.Center) { Icon(Icons.SHARE, C.ink, 22.dp) } }) {
        Text(tr("Палочка складывается из того, как вы учитесь, и меняется вместе с вами. Двух одинаковых не бывает.",
            "The wand is made of the way you study and changes with you. No two are alike."), style = T.small)
        Box(Modifier.fillMaxWidth().padding(vertical = 14.dp).card(22.dp, Color(0xFF1B1733)), contentAlignment = Alignment.Center) { WandArt(w, Modifier.fillMaxWidth().height(170.dp)) }
        Row2(tr("Древесина", "Wood"), tr("от главной школы: ${w.school.title}", "from your main school: ${w.school.title}"), w.school.wood.replaceFirstChar { it.uppercase() })
        Row2(tr("Сердцевина", "Core"), tr("от вожака башни", "from the tower's leader"), w.coreName.replaceFirstChar { it.uppercase() })
        Row2(tr("Длина", "Length"), tr("растёт с часами учёбы: ${w.hours} ч", "grows with hours of study: ${w.hours} h"), tr("${w.length} см", "${w.length} cm"))
        Row2(tr("Гибкость", "Flexibility"), tr("от свечей без отвлечений: ${(w.clean * 100).toInt()}%", "from undisturbed candles: ${(w.clean * 100).toInt()}%"), w.flex)
        Row2(tr("Резьба", "Carvings"), tr("кольцо за каждый законченный материал", "a ring for every finished material"), "${w.carvings}")
        Row2(tr("Руны", "Runes"), tr("искра за каждое выученное заклинание", "a spark for every spell learned"), "${w.runes}")
        w.second?.let { Row2(tr("Инкрустация", "Inlay"), tr("от второй школы", "from your second school"), it.title) }
        Spacer(Modifier.height(20.dp))
    }
}

/** The wand drawn: tapering wood of the school's wood colour, carved rings, a handle with the second school's inlay, runes and a glowing tip. */
@Composable
fun WandArt(w: WandSpec, modifier: Modifier = Modifier) {
    val wood = Color(w.school.woodColor); val glow = Color(w.school.color)
    val inlay = w.second?.let { Color(it.color) } ?: Color(0xFFD9B060)
    Canvas(modifier) {
        val W = size.width; val H = size.height
        val len = W * (.55f + .4f * ((w.length - 18) / 20f).coerceIn(0f, 1f))
        val x0 = (W - len) / 2; val cy = H / 2
        rotate(-14f, Offset(W / 2, cy)) {
            drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = .8f), Color.Transparent), Offset(x0 + len + 4, cy), H * .3f), H * .3f, Offset(x0 + len + 4, cy))
            val shaft = Path().apply { moveTo(x0, cy - H * .055f); lineTo(x0 + len, cy - H * .016f); lineTo(x0 + len, cy + H * .016f); lineTo(x0, cy + H * .055f); close() }
            drawPath(shaft, Brush.verticalGradient(listOf(wood.copy(alpha = 1f), lerpC(wood, Color.Black, .35f)), cy - H * .06f, cy + H * .06f))
            // handle
            drawRoundRect(lerpC(wood, Color.Black, .45f), Offset(x0 - W * .01f, cy - H * .07f), Size(len * .26f, H * .14f), CornerRadius(H * .05f))
            drawRoundRect(inlay, Offset(x0 + len * .04f, cy - H * .07f), Size(len * .03f, H * .14f))
            drawRoundRect(inlay, Offset(x0 + len * .19f, cy - H * .07f), Size(len * .03f, H * .14f))
            // carved rings
            for (k in 0 until w.carvings.coerceAtMost(12)) { val x = x0 + len * (.3f + .05f * k); val hh = H * (.05f - .03f * (k / 12f))
                drawLine(lerpC(wood, Color.Black, .5f), Offset(x, cy - hh), Offset(x, cy + hh), W * .004f) }
            // runes: sparks along the wand
            for (k in 0 until w.runes.coerceAtMost(20)) { val x = x0 + len * (.32f + .033f * k); drawCircle(Color(0xFFFFE6A0).copy(alpha = .9f), W * .0035f, Offset(x, cy - H * .004f)) }
            drawCircle(Color.White, H * .02f, Offset(x0 + len + 2, cy))
        }
    }
}

private fun lerpC(a: Color, b: Color, t: Float) = Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, 1f)

