package app.inkling.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.inkling.container
import app.inkling.data.Settings
import app.inkling.domain.Schools
import app.inkling.domain.Stage
import app.inkling.domain.tr
import app.inkling.ui.art.Creature
import app.inkling.ui.art.Pose
import app.inkling.ui.components.Btn
import app.inkling.ui.components.Chip
import app.inkling.ui.components.Field
import app.inkling.ui.components.Ghost
import app.inkling.ui.components.SchoolMark
import app.inkling.ui.components.card
import app.inkling.ui.components.schoolColor
import app.inkling.ui.theme.C
import app.inkling.ui.theme.T
import app.inkling.ui.tower.scaleName

/** First launch: the inkling, where one studies, the subjects (each finds its school of magic) and the scale of marks. */
@Composable
fun Onboarding() {
    val c = LocalContext.current.container
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(Settings.UNI) }
    var year by remember { mutableIntStateOf(1) }
    var scale by remember { mutableStateOf("5") }
    val subjects = remember { mutableStateListOf<String>() }
    var typed by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(C.bg).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { i -> Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(50)).background(if (i <= step) C.acc else C.track)) } }
            when (step) {
                0 -> {
                    Box(Modifier.fillMaxWidth().padding(top = 30.dp), contentAlignment = Alignment.Center) { Creature("inkling", Stage.BABY, Pose.CHEER, 200.dp) }
                    Text(tr("Добро пожаловать в академию", "Welcome to the academy"), style = T.title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Text(tr("Это Кляксик – чернильный дух. Он будет жить с вами в башне, расти, пока вы учитесь, и записывать в гримуар всё, что вы запомнили. За учёбу находятся новые существа – их 24.",
                        "This is the Inkling, a spirit of ink. It will live with you in the tower, grow while you study and write down in the grimoire everything you learn. Study brings new creatures – there are 24."),
                        style = T.body.copy(color = C.mute), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    Field(name, { name = it.take(30) }, tr("Как вас записать в журнал академии? (по желанию)", "How shall the academy register you? (optional)"), modifier = Modifier.padding(top = 20.dp))
                }
                1 -> {
                    Text(tr("Где вы учитесь?", "Where do you study?"), style = T.title, modifier = Modifier.padding(top = 24.dp))
                    Text(tr("От этого зависят слова и вкладки: уроки или пары, четверти или семестры, ОГЭ/ЕГЭ или сессия.", "This sets the words and tabs: lessons or classes, terms or semesters, state exams or a session."), style = T.small, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    listOf(Triple(Settings.SCHOOL, tr("Школа", "School"), tr("уроки, домашка, портфель, четверти, ОГЭ и ЕГЭ", "lessons, homework, the satchel, terms, state exams")),
                        Triple(Settings.COLLEGE, tr("Колледж", "College"), tr("пары, практика, семестры, зачёты", "classes, practice, semesters, credits")),
                        Triple(Settings.UNI, tr("Университет", "University"), tr("пары, лабы, курсовые, сессия, билеты, БРС", "classes, labs, coursework, the session, tickets, points")),
                        Triple(Settings.SELF, tr("Учусь сам(а)", "On my own"), tr("курсы, книги, языки, цели – без расписания", "courses, books, languages, goals – no timetable"))).forEach { (m, t, sub) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).card(16.dp, if (mode == m) C.accSoft else null).clickable { mode = m; scale = if (m == Settings.UNI) "5" else scale }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(t, style = T.bodyB); Text(sub, style = T.small) }
                            Box(Modifier.size(22.dp).clip(CircleShape).background(if (mode == m) C.acc else C.track))
                        }
                    }
                    if (mode != Settings.SELF) {
                        Text(if (mode == Settings.SCHOOL) tr("Класс", "Year") else tr("Курс", "Year"), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            (if (mode == Settings.SCHOOL) 1..11 else 1..6).forEach { y -> Chip("$y", year == y) { year = y } } }
                    }
                }
                2 -> {
                    Text(tr("Что изучаете?", "What do you study?"), style = T.title, modifier = Modifier.padding(top = 24.dp))
                    Text(tr("Любые предметы – как они называются у вас. Каждый попадёт в свою школу магии, её можно потом сменить.", "Any subjects, named as you call them. Each goes to a school of magic, which you can change later."), style = T.small, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Field(typed, { typed = it }, tr("Например: «Сопромат»", "E.g. «Organic chemistry»"), Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Btn("+", small = true, enabled = typed.isNotBlank()) { if (typed.trim() !in subjects) subjects += typed.trim(); typed = "" }
                    }
                    if (typed.isNotBlank()) Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        SchoolMark(Schools.guess(typed), 24.dp); Spacer(Modifier.width(6.dp)); Text(Schools.of(Schools.guess(typed)).title, style = T.small.copy(color = schoolColor(Schools.guess(typed)))) }
                    subjects.forEach { s -> Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).card(14.dp).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        SchoolMark(Schools.guess(s), 34.dp); Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { Text(s, style = T.bodyB); Text(Schools.of(Schools.guess(s)).title, style = T.tiny.copy(color = schoolColor(Schools.guess(s)))) }
                        Text("×", style = T.h3.copy(color = C.mute), modifier = Modifier.clickable { subjects.remove(s) }.padding(8.dp)) } }
                    Text(tr("Часто бывают", "Common ones"), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        suggestions(mode).filter { it !in subjects }.forEach { s -> Chip(s, false) { subjects += s } } }
                }
                else -> {
                    Text(tr("Как ставят оценки?", "How are you marked?"), style = T.title, modifier = Modifier.padding(top = 24.dp))
                    Text(tr("Сорока будет считать средний и подсказывать, сколько осталось до высшей оценки. Шкалу можно сменить в настройках.", "The magpie will keep the average and tell you how far you are from the top mark. You can change it in the settings."), style = T.small, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("5", "10", "12", "100", "letter", "pass").forEach { sc -> Chip(scaleName(sc), scale == sc) { scale = sc } } }
                    Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) { Creature("magpie", Stage.ADULT, Pose.WORK, 150.dp) }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) Ghost(tr("Назад", "Back"), Modifier.weight(1f)) { step-- }
            val last = step == 3 || (step == 2 && mode == Settings.SELF)
            Btn(if (last) tr("Открыть башню", "Open the tower") else tr("Дальше", "Next"), Modifier.weight(1.6f), enabled = step != 2 || subjects.isNotEmpty() || typed.isNotBlank()) {
                if (step == 2 && typed.isNotBlank() && typed.trim() !in subjects) { subjects += typed.trim(); typed = "" }
                if (last) c.write { begin(mode, if (mode == Settings.SELF) 1 else year, scale, name.trim(), subjects.toList()) } else step++
            }
        }
    }
}

private fun suggestions(mode: String) = when (mode) {
    Settings.SCHOOL -> tr("Алгебра,Геометрия,Русский язык,Литература,Английский язык,История,Обществознание,Физика,Химия,Биология,География,Информатика,Физкультура,Окружающий мир,Математика,ИЗО,Музыка,Технология",
        "Maths,English,Literature,History,Geography,Physics,Chemistry,Biology,Computing,French,Spanish,Art,Music,PE,Science").split(',')
    Settings.SELF -> tr("Английский,Испанский,Программирование,Python,Рисование,Гитара,Фортепиано,Психология,Маркетинг,Дизайн,Статистика,Философия,История искусств",
        "Spanish,French,Japanese,Programming,Python,Drawing,Guitar,Piano,Psychology,Marketing,Design,Statistics,Philosophy,Art history").split(',')
    else -> tr("Математический анализ,Линейная алгебра,Английский язык,История России,Философия,Программирование,Физика,Экономика,Физкультура,Химия,Анатомия,Право,Психология,Сопромат,Теория вероятностей",
        "Calculus,Linear algebra,Academic English,Philosophy,Programming,Physics,Economics,Chemistry,Anatomy,Law,Psychology,Statistics,Probability").split(',')
}
