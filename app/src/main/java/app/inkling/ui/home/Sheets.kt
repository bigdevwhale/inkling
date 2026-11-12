package app.inkling.ui.home

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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.inkling.container
import app.inkling.data.Slot
import app.inkling.data.Task
import app.inkling.domain.Timetable
import app.inkling.domain.Words
import app.inkling.domain.relDay
import app.inkling.domain.tr
import app.inkling.ui.Data
import app.inkling.ui.Nav
import app.inkling.ui.Screen
import app.inkling.ui.art.Mini
import app.inkling.ui.art.Prop
import app.inkling.ui.components.Btn
import app.inkling.ui.components.Field
import app.inkling.ui.components.Icon
import app.inkling.ui.components.Icons
import app.inkling.ui.components.Sheet
import app.inkling.ui.components.card
import app.inkling.ui.magic.GradeSheet
import app.inkling.ui.magic.QuestionSheet
import app.inkling.ui.magic.SpellSheet
import app.inkling.ui.magic.rememberPhoto
import app.inkling.ui.theme.C
import app.inkling.ui.theme.T

/** The wand button: everything that can be added, as a grid of pictures. */
@Composable
fun AddSheet(d: Data, nav: Nav, onDone: () -> Unit) {
    var quick by remember { mutableStateOf("") }
    when (quick) {
        "spell" -> { SpellSheet(d) { quick = ""; onDone() }; return }
        "question" -> { QuestionSheet(d) { quick = ""; onDone() }; return }
        "grade" -> { GradeSheet(d) { quick = ""; onDone() }; return }
    }
    fun go(s: Screen) { onDone(); nav.push(s) }
    Sheet(onDone) {
        Text(tr("Что колдуем?", "What shall we conjure?"), style = T.h2)
        Spacer(Modifier.height(12.dp))
        val items = buildList {
            add(Triple("candle", tr("Свеча", "Candle"), tr("урок с таймером", "a timed lesson")) to { go(Screen.Candle) })
            add(Triple("quill", tr("Занятие", "Study"), tr("записать, что было", "write down what you did")) to { go(Screen.StudyEdit()) })
            add(Triple("book", tr("Заклинание", "Spell"), tr("что запомнили", "what you learned")) to { quick = "spell" })
            add(Triple("scroll", tr("Задание", "Task"), if (d.s.school) tr("домашка, проект", "homework, project") else tr("домашка, лаба, курсовая", "homework, lab, coursework")) to { go(Screen.TaskInfo(0)) })
            add(Triple("orb", tr("Вопрос", "Question"), tr("в сундук мимика", "for the mimic's chest")) to { quick = "question" })
            add(Triple("raven", tr("Научи", "Teach"), tr("объяснить тему", "explain a topic")) to { go(Screen.Teach()) })
            if (!d.s.self) add(Triple("star", tr("Оценка", "Mark"), tr("или баллы", "or points")) to { quick = "grade" })
            add(Triple("medal", tr("Испытание", "Trial"), if (d.s.school) tr("контрольная, ОГЭ, ЕГЭ", "test, state exam") else tr("экзамен, зачёт", "exam, credit")) to { go(Screen.ExamInfo(0)) })
            add(Triple("books", tr("Материал", "Material"), tr("курс, книга, задачник", "course, book, problems")) to { go(Screen.MaterialInfo(0)) })
        }
        items.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (t, f) ->
                    Column(Modifier.weight(1f).card(16.dp).clickable { f() }.padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (t.first == "raven") Mini("raven", 40.dp) else Prop(t.first, 40.dp)
                        Text(t.second, style = T.bodyB, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                        Text(t.third, style = T.tiny, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Right after a class: what was set (due at the next class of the subject, with a photo of the board) and what stuck. */
@Composable
fun AfterClassSheet(sl: Slot, d: Data, nav: Nav, onDone: () -> Unit) {
    val c = LocalContext.current.container
    val sub = d.subById[sl.subjectId]
    var hw by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf("") }
    var spell by remember { mutableStateOf("") }
    val take = rememberPhoto { photo = it }
    val due = Timetable.nextClass(d.s, d.slots, sl.subjectId, d.today) ?: (d.today + 7)
    Sheet(onDone) {
        Text(sub?.name ?: "", style = T.h2)
        Text(tr("Гаргулья отметила. Пока свежо – две строчки:", "The gargoyle marked it. While it's fresh – two lines:"), style = T.small, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
        Text(tr("Что задали?", "What was set?"), style = T.sec, modifier = Modifier.padding(bottom = 6.dp))
        Field(hw, { hw = it }, tr("Например: № 214, 216; параграф 12", "E.g. ex. 214, 216; chapter 12"), single = false)
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.clip(RoundedCornerShape(50)).background(C.card2).clickable { take() }.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.CAMERA, C.ink, 16.dp); Spacer(Modifier.width(6.dp)); Text(if (photo.isEmpty()) tr("Сфотографировать доску", "Photo of the board") else tr("Фото есть", "Photo taken"), style = T.smallB)
            }
            Spacer(Modifier.width(8.dp))
            Text(tr("к ${relDay(due, d.today)}", "due ${relDay(due, d.today)}"), style = T.tiny)
        }
        Text(tr("Что запомнили?", "What stuck?"), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
        Field(spell, { spell = it }, tr("Главная мысль пары – для гримуара", "The main idea – for the grimoire"))
        Spacer(Modifier.height(14.dp))
        Btn(tr("Готово", "Done"), Modifier.fillMaxWidth()) {
            c.write {
                if (hw.isNotBlank() || photo.isNotEmpty()) saveTask(Task(subjectId = sl.subjectId, title = hw.trim().ifBlank { tr("Задание с доски", "Task from the board") }, due = due, created = d.today, photo = photo))
                if (spell.isNotBlank()) addSpell(sl.subjectId, spell, "")
            }
            onDone()
        }
    }
}

/** The evening satchel: tomorrow's lessons with a textbook and a notebook each, PE kit for PE, and the homework still open. */
@Composable
fun SatchelSheet(d: Data, onDone: () -> Unit) {
    val c = LocalContext.current.container
    val tomorrow = d.today + 1
    val lessons = Timetable.on(d.s, d.slots, tomorrow).mapNotNull { d.subById[it.subjectId] }.distinctBy { it.id }
    val things = buildList {
        lessons.forEach { add(tr("${it.name}: учебник и тетрадь", "${it.name}: textbook and notebook")) }
        if (lessons.any { it.school == "body" }) add(tr("Спортивная форма", "PE kit"))
        if (lessons.any { it.school == "illus" }) add(tr("Альбом и краски", "Sketchbook and paints"))
        add(tr("Пенал, ручки, карандаш", "Pencil case, pens"))
        add(tr("Сменная обувь", "Indoor shoes"))
        d.openTasks.filter { it.due == tomorrow }.forEach { add(tr("Сделано: ", "Done: ") + it.title) }
    }
    val checked = remember { mutableStateListOf<Int>() }
    Sheet(onDone) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Mini("hob", 48.dp); Spacer(Modifier.width(8.dp))
            Column { Text(tr("Портфель на завтра", "Tomorrow's satchel"), style = T.h2); Text(lessons.joinToString(", ") { it.name }, style = T.small) }
        }
        Spacer(Modifier.height(10.dp))
        things.forEachIndexed { i, t ->
            val on = i in checked
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { if (on) checked.remove(i) else checked.add(i) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(if (on) C.ok else C.card2), contentAlignment = Alignment.Center) { if (on) Icon(Icons.CHECK, C.accInk, 15.dp) }
                Spacer(Modifier.width(10.dp))
                Text(t, style = if (on) T.body.copy(color = C.mute) else T.body)
            }
        }
        Spacer(Modifier.height(12.dp))
        Btn(tr("Портфель собран", "Satchel packed"), Modifier.fillMaxWidth(), enabled = checked.size >= things.size - 1) { c.write { pack(tomorrow); say(tr("Домовой застегнул портфель", "The hob buckled the satchel")) }; onDone() }
    }
}
