package app.inkling.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.inkling.container
import app.inkling.domain.Stage
import app.inkling.domain.tr
import app.inkling.domain.Words
import app.inkling.ui.art.Creature
import app.inkling.ui.art.Pose
import app.inkling.ui.components.Btn
import app.inkling.ui.components.tap
import app.inkling.ui.theme.C
import app.inkling.ui.theme.T

/** A first-visit introduction to one screen: who explains it, the title and a few short points. */
class Intro(val id: String, val species: String, val title: String, val points: List<String>)

/** The introduction for what is on top now: the page over the tabs, or the tab itself. */
fun introFor(top: Screen?, tab: Int, s: app.inkling.data.Settings): Intro? = when (top) {
    null -> when (tab) {
        0 -> Intro("i.home", "inkling", tr("Это ваша башня", "This is your tower"), listOf(
            tr("Пока вы учитесь, существа едят знания, а ночью растут.", "While you study, the creatures feed; at night they grow."),
            tr("«Зажечь свечу» – урок с таймером.", "«Light a candle» starts a timed lesson."),
            tr("Палочка внизу добавляет всё остальное.", "The wand below adds everything else.")))
        1 -> Intro("i.academy", "owl", if (s.self) tr("Учёба", "Study") else tr("Академия", "The academy"), listOfNotNull(
            if (s.self) null else tr("«Неделя» – расписание и отметки, где были.", "«Week» – the timetable and attendance."),
            tr("«Задания» и «Испытания» – по срокам.", "«Tasks» and «Trials» – by due date."),
            tr("«Предметы» – материалы и оценки.", "«Subjects» – materials and marks.")))
        2 -> Intro("i.pets", "kitsune", tr("Существа", "Creatures"), listOf(
            tr("Каждое находится за то, как вы учитесь. Нажмите на силуэт – узнаете за что.", "Each is found by the way you study. Tap a shadow to see how."),
            tr("Трое в башне удваивают ману за своё дело.", "The three in the tower double mana for their thing.")))
        else -> Intro("i.stats", "sphinx", tr("Летопись", "The chronicle"), listOf(
            tr("Вверху – срок, ниже – разделы.", "Pick the span at the top, the view below."),
            tr("Свиток справа – год в академии.", "The scroll at the top right is your year.")))
    }
    Screen.Candle -> Intro("i.candle", "salamander", tr("Свеча", "The candle"), listOf(
        tr("Положите телефон экраном вниз – круг замкнётся.", "Put the phone face down – the circle closes."),
        tr("Взяли телефон – пламя дрогнет. Это не штраф.", "Pick it up and the flame flickers. No penalty."),
        tr("После свечи – перемена.", "A break follows the candle.")))
    is Screen.Grimoire -> Intro("i.grimoire", "sphinx", tr("Гримуар", "The grimoire"), listOf(
        tr("Заклинание – то, что вы запомнили.", "A spell is something you learned."),
        tr("Сфинкс спрашивает его через 1, 3, 7, 21 и 60 дней.", "The sphinx asks it after 1, 3, 7, 21 and 60 days."),
        tr("«Не помню» – не беда: спросит завтра.", "«Forgot» is fine – it comes back tomorrow.")))
    Screen.Chest -> null
    is Screen.Teach -> Intro("i.teach", "raven", tr("Научи фамильяра", "Teach the familiar"), listOf(
        tr("Объясните тему своими словами.", "Explain a topic in your own words."),
        tr("Где слов не хватило – там пробел.", "Where words fail, there's a gap.")))
    Screen.Wand -> null
    Screen.Timetable -> Intro("i.timetable", "gargoyle", tr("Расписание", "The timetable"), listOf(
        tr("Заводится один раз. Звонки и числитель – в настройках.", "Set it up once. Bells and odd/even weeks are in the settings."),
        tr("После занятия башня спросит: были? что задали?", "After a class the tower asks: were you there? what was set?")))
    is Screen.ExamInfo -> if (top.id == 0L) null else Intro("i.exam", "phoenix", tr("Испытание", "A trial"), listOf(
        tr("Отмечайте билеты по уровню – полоска покажет готовность.", "Mark tickets by level – the bar shows readiness."),
        tr("Сфинкс тянет случайный билет, слабые – чаще.", "The sphinx draws a ticket, weak ones more often.")))
    is Screen.PetInfo -> null
    Screen.Shop -> null
    Screen.Settings -> null
    is Screen.Wrapped -> null
    else -> null
}

/** The introduction as a card sliding up over the screen; «Got it» stores its id in Settings.seen. */
@Composable
fun IntroSheet(intro: Intro) {
    val c = LocalContext.current.container
    fun done() = c.write { settings.update { it.copy(seen = it.seen + intro.id) } }
    BackHandler { done() }
    val a = remember(intro.id) { Animatable(0f) }
    LaunchedEffect(intro.id) { a.animateTo(1f, tween(320)) }
    Box(Modifier.fillMaxSize().background(Color(0x99000000).copy(alpha = .6f * a.value)).tap {}, contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth().offset(y = ((1 - a.value) * 220).dp).alpha(a.value).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(C.bg)
            .navigationBarsPadding().padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Creature(intro.species, Stage.ADULT, Pose.CHEER, 96.dp)
                Column(Modifier.weight(1f).padding(start = 6.dp, bottom = 14.dp)) {
                    Text(intro.title, style = T.h2)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                intro.points.forEachIndexed { i, s ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(C.accSoft), contentAlignment = Alignment.Center) { Text("${i + 1}", style = T.smallB.copy(color = C.acc)) }
                        Spacer(Modifier.width(10.dp))
                        Text(s, style = T.body, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Btn(tr("Понятно", "Got it"), Modifier.fillMaxWidth()) { done() }
        }
    }
}
