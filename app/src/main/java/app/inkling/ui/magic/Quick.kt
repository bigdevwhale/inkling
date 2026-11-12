package app.inkling.ui.magic

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.inkling.container
import app.inkling.data.Grade
import app.inkling.domain.Marks
import app.inkling.domain.tr
import app.inkling.ui.Data
import app.inkling.ui.art.Mini
import app.inkling.ui.components.Btn
import app.inkling.ui.components.Chip
import app.inkling.ui.components.DayPicker
import app.inkling.ui.components.Field
import app.inkling.ui.components.Sheet
import app.inkling.ui.components.SubjectPicker
import app.inkling.ui.theme.T
import java.io.File

/** «What did you learn?»: a spell for the grimoire – a name to be asked by, and the gist in one's own words. */
@Composable
fun SpellSheet(d: Data, subjectId: Long = 0, onDone: () -> Unit) {
    val c = LocalContext.current.container
    var sub by remember { mutableLongStateOf(subjectId.takeIf { it != 0L } ?: d.active.firstOrNull()?.id ?: 0) }
    androidx.compose.runtime.LaunchedEffect(d.active.size) { if (sub == 0L) sub = d.active.firstOrNull()?.id ?: 0L }
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    Sheet(onDone) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Mini("inkling", 44.dp); Spacer(Modifier.width(8.dp))
            Column0 { Text(tr("Что запомнили?", "What did you learn?"), style = T.h2)
                Text(tr("Кляксик запишет это в гримуар, а сфинкс спросит через 1, 3, 7 и 21 день", "The inkling writes it in the grimoire; the sphinx asks after 1, 3, 7 and 21 days"), style = T.small) }
        }
        Spacer(Modifier.height(10.dp))
        SubjectPicker(d.active, sub) { sub = it }
        Spacer(Modifier.height(10.dp))
        Field(title, { title = it }, tr("Тема или вопрос: «Теорема Виета»", "Topic or question: «Vieta's formulas»"))
        Spacer(Modifier.height(8.dp))
        Field(text, { text = it }, tr("Суть своими словами (по желанию)", "The gist in your own words (optional)"), single = false)
        Spacer(Modifier.height(14.dp))
        Btn(tr("В гримуар", "Into the grimoire"), Modifier.fillMaxWidth(), enabled = title.isNotBlank()) { c.write { addSpell(sub, title, text); say(tr("Новое заклинание в гримуаре", "A new spell in the grimoire")) }; onDone() }
    }
}

/** A question for the mimic's chest. */
@Composable
fun QuestionSheet(d: Data, subjectId: Long = 0, onDone: () -> Unit) {
    val c = LocalContext.current.container
    var sub by remember { mutableLongStateOf(subjectId) }
    var text by remember { mutableStateOf("") }
    Sheet(onDone) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Mini("mimic", 44.dp); Spacer(Modifier.width(8.dp))
            Column0 { Text(tr("Вопрос в сундук", "A question for the chest"), style = T.h2)
                Text(tr("Что осталось непонятным? Мимик сохранит, а ответ найдёте потом", "What's still unclear? The mimic keeps it; find the answer later"), style = T.small) }
        }
        Spacer(Modifier.height(10.dp))
        SubjectPicker(d.active, sub, allowNone = true) { sub = it }
        Spacer(Modifier.height(10.dp))
        Field(text, { text = it }, tr("Почему…? Как…? Зачем…?", "Why…? How…? What for…?"), single = false)
        Spacer(Modifier.height(14.dp))
        Btn(tr("Положить в сундук", "Into the chest"), Modifier.fillMaxWidth(), enabled = text.isNotBlank()) { c.write { addQuestion(sub, text) }; onDone() }
    }
}

/** A mark: picked from the scale (or points typed), a weight and a label. */
@Composable
fun GradeSheet(d: Data, subjectId: Long = 0, examId: Long = 0, onDone: () -> Unit) {
    val c = LocalContext.current.container
    val scale = if (examId != 0L) "100" else d.s.scale
    var sub by remember { mutableLongStateOf(subjectId.takeIf { it != 0L } ?: d.active.firstOrNull()?.id ?: 0) }
    androidx.compose.runtime.LaunchedEffect(d.active.size) { if (sub == 0L) sub = d.active.firstOrNull()?.id ?: 0L }
    var v by remember { mutableStateOf<Double?>(null) }
    var typed by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf(1.0) }
    var label by remember { mutableStateOf("") }
    var day by remember { mutableLongStateOf(d.today) }
    Sheet(onDone) {
        Text(if (examId != 0L) tr("Пробный вариант", "A practice paper") else tr("Оценка", "A mark"), style = T.h2)
        Spacer(Modifier.height(10.dp))
        if (examId == 0L) { SubjectPicker(d.active, sub) { sub = it }; Spacer(Modifier.height(10.dp)) }
        val choices = Marks.choices(scale)
        if (choices.isEmpty()) Field(typed, { typed = it }, if (examId != 0L) tr("Баллы", "Points") else tr("Баллы, например 8", "Points, e.g. 8"), number = true)
        else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { choices.take(6).forEach { (l, x) -> Chip(l, v == x, Modifier.weight(1f)) { v = x } } }
        if (choices.size > 6) Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { choices.drop(6).forEach { (l, x) -> Chip(l, v == x, Modifier.weight(1f)) { v = x } } }
        if (examId == 0L && scale != "100") {
            Text(tr("Вес", "Weight"), style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(1.0, 1.5, 2.0, 3.0).forEach { w -> Chip("×${app.inkling.domain.fmt1(w)}", weight == w) { weight = w } } }
        }
        Spacer(Modifier.height(10.dp))
        Field(label, { label = it }, tr("За что: контрольная, ответ у доски…", "What for: a test, an oral answer…"))
        Spacer(Modifier.height(10.dp))
        DayPicker(day, d.today) { day = it }
        Spacer(Modifier.height(14.dp))
        val value = v ?: typed.replace(',', '.').toDoubleOrNull()
        Btn(tr("Записать", "Save"), Modifier.fillMaxWidth(), enabled = value != null && (sub != 0L || examId != 0L)) {
            c.write { addGrade(Grade(subjectId = if (examId != 0L) d.exams.firstOrNull { it.id == examId }?.subjectId ?: sub else sub, value = value!!, weight = weight, day = day, label = label.trim(), examId = examId)) }; onDone() }
    }
}

/** Takes a picture with the phone's own camera into the app's photos; gives back its uri. */
@Composable
fun rememberPhoto(onTaken: (String) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    var pending by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) pending?.let { onTaken(it.toString()) } }
    return {
        val dir = File(ctx.filesDir, "photos").apply { mkdirs() }
        val f = File(dir, "board_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        pending = uri
        runCatching { launcher.launch(uri) }
    }
}

@Composable
private fun Column0(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) = androidx.compose.foundation.layout.Column(content = content)
