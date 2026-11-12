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
            tr("Здесь живут существа. Пока вы учитесь, они едят знания, а ночью растут – как память во сне.", "Your creatures live here. While you study they feed on knowledge, and at night they grow – as memory does in sleep."),
            tr("«Зажечь свечу» – урок с таймером. Положите телефон экраном вниз, и ничто не отвлечёт.", "«Light a candle» – a timed lesson. Put the phone face down and nothing distracts you."),
            if (s.self) tr("Ниже – одно дело, которое ждёт: сфинкс с загадками или письмо совы о сроке.", "Below is the one thing waiting: the sphinx's riddles or the owl's letter about a deadline.")
            else tr("Ниже – что сейчас по расписанию и одно дело, которое ждёт: отметить занятие, вспомнить заклинания, собрать портфель.", "Below: what's on in the timetable and the one thing waiting – mark a class, recall spells, pack the satchel."),
            tr("Палочка внизу – всё, что можно добавить. Нажмите на существо, чтобы открыть его.", "The wand below adds anything. Tap a creature to open it.")))
        1 -> Intro("i.academy", "owl", if (s.self) tr("Учёба", "Study") else tr("Академия", "The academy"), listOfNotNull(
            if (s.self) null else tr("«Неделя» – расписание. Отмечайте, где были: гаргулья любит посещаемость.", "«Week» is the timetable. Mark the classes you attended – the gargoyle loves that."),
            tr("«Задания» – всё, что задали, по срокам. Большие работы делятся на этапы.", "«Tasks» – everything set, by due date. Big works split into stages."),
            tr("«Испытания» – экзамены и контрольные с отсчётом дней, билетами и пробниками.", "«Trials» – exams and tests with a countdown, tickets and practice papers."),
            tr("«Предметы» – у каждого своя школа магии, материалы и оценки.", "«Subjects» – each has its school of magic, materials and marks.")))
        2 -> Intro("i.pets", "kitsune", tr("Существа", "Creatures"), listOf(
            tr("24 существа. Кроме кляксика, все находятся за то, как вы учитесь. Нажмите на серый силуэт – узнаете за что.", "24 creatures. Apart from the inkling, each is found by the way you study. Tap a grey shadow to see how."),
            tr("Вожак и двое за партами живут в башне и удваивают ману за своё дело.", "The leader and the two at the desks live in the tower and double the mana for their thing."),
            tr("У каждого шесть характеристик и специальность – по школе, на которой его растили.", "Each has six stats and a speciality – from the school it was raised on."),
            tr("Существа растут от учёбы, а не от оценок.", "Creatures grow on study, not on marks.")))
        else -> Intro("i.stats", "sphinx", tr("Летопись", "The chronicle"), listOf(
            tr("Вверху – срок: неделя, месяц, ${Words.term(s)}, год или всё время.", "At the top pick the span: a week, a month, a ${Words.term(s)}, a year or all time."),
            tr("«Обзор» – часы, дни и календарь, «Предметы» – звезда школ магии.", "«Overview» – hours, days and the calendar; «Subjects» – the star of schools."),
            tr("«Память» – гримуар в цифрах, «Привычки» – когда и как долго вы учитесь лучше всего.", "«Memory» – the grimoire in numbers; «Habits» – when and how long you study best."),
            tr("Свиток справа вверху – год в академии.", "The scroll at the top right – a year in the academy.")))
    }
    Screen.Candle -> Intro("i.candle", "salamander", tr("Свеча", "The candle"), listOf(
        tr("Выберите предмет и, если хотите, над чем работаете – материал, задание или испытание.", "Pick a subject and, if you like, what you're working on – a material, a task or a trial."),
        tr("Пока горит свеча, положите телефон экраном вниз: круг замкнётся.", "While the candle burns, put the phone face down: the circle closes."),
        tr("Если выйти из приложения или взять телефон – пламя дрогнет. Это не штраф, просто честный счёт.", "Leaving the app or picking the phone up makes the flame flicker. Not a penalty – just an honest count."),
        tr("После свечи – перемена. Кот любит, когда вы отдыхаете по-настоящему.", "After the candle comes a break. The cat likes it when you really rest.")))
    is Screen.Grimoire -> Intro("i.grimoire", "sphinx", tr("Гримуар", "The grimoire"), listOf(
        tr("Заклинание – то, что вы запомнили: тема и суть своими словами.", "A spell is something you learned: a topic and the gist in your own words."),
        tr("Сфинкс спрашивает через 1, 3, 7, 21 и 60 дней – так знание переходит в долгую память.", "The sphinx asks after 1, 3, 7, 21 and 60 days – that's how knowledge moves into long-term memory."),
        tr("Отвечайте честно: «не помню» – не беда, заклинание просто вернётся завтра.", "Answer honestly: «forgot» is fine, the spell just comes back tomorrow.")))
    Screen.Chest -> Intro("i.chest", "mimic", tr("Сундук мимика", "The mimic's chest"), listOf(
        tr("Сюда – всё, что непонятно: вопросы по ходу учёбы.", "Everything unclear goes here: questions that come up as you study."),
        tr("Нашли ответ – запишите его, и мимик выплюнет кристалл.", "Found an answer? Write it and the mimic spits out a crystal."),
        tr("Вопросы растят любопытство существ.", "Questions grow your creatures' curiosity.")))
    is Screen.Teach -> Intro("i.teach", "raven", tr("Научи фамильяра", "Teach the familiar"), listOf(
        tr("Объясните тему своими словами – как будто учите кого-то.", "Explain a topic in your own words – as if teaching someone."),
        tr("Ворон-эхо будет переспрашивать: почему, пример, чем отличается, а проще?", "The echo raven keeps asking: why, an example, how is it different, simpler?"),
        tr("Где слова не находятся – там пробел. Его стоит доучить.", "Where words fail, there's a gap. That's what to learn next.")))
    Screen.Wand -> Intro("i.wand", "inkling", tr("Палочка", "The wand"), listOf(
        tr("Палочка складывается из вашей учёбы: дерево – от главной школы, сердцевина – от вожака.", "The wand is made of your study: the wood from your main school, the core from your leader."),
        tr("Длина растёт с часами, резьба – с законченными материалами, руны – с выученными заклинаниями.", "Its length grows with hours, the carvings with finished materials, the runes with spells learned.")))
    Screen.Timetable -> Intro("i.timetable", "gargoyle", tr("Расписание", "The timetable"), listOf(
        tr("Неделя заводится один раз. Время берётся из звонков – их можно поправить в настройках.", "Set the week up once. Times come from the bells – adjust them in the settings."),
        tr("Если недели чередуются, включите числитель и знаменатель в настройках.", "If weeks alternate, switch on odd and even weeks in the settings."),
        tr("После каждого занятия башня спросит: были? что задали?", "After each class the tower asks: were you there? what was set?")))
    is Screen.ExamInfo -> if (top.id == 0L) null else Intro("i.exam", "phoenix", tr("Испытание", "A trial"), listOf(
        tr("Билеты отмечайте по уровню: от «не знаю» до «отлично». Полоска покажет готовность.", "Mark tickets by level, from «no idea» to «sure». The bar shows how ready you are."),
        tr("«Сфинкс тянет билет» – случайный, слабые чаще. Отвечайте как на экзамене.", "«The sphinx draws a ticket» – a random one, weak ones more often. Answer as in the exam."),
        tr("Для ОГЭ и ЕГЭ записывайте пробные варианты – появится прогноз балла.", "For state exams, log practice papers – a predicted score appears.")))
    is Screen.TaskInfo -> null
    is Screen.PetInfo -> Intro("i.pet", "inkling", tr("Страница существа", "A creature's page"), listOf(
        tr("Кнопки под существом меняют его позу.", "The buttons under the creature show its poses."),
        tr("«Место»: вожак и двое за партами живут в башне и дают бонус к мане.", "«Place»: the leader and the two at desks live in the tower and add mana."),
        tr("Шесть характеристик растут от разной учёбы, а ночью накопленное превращается в опыт.", "Six stats grow on different kinds of study, and at night what was eaten turns into growth.")))
    Screen.Shop -> Intro("i.shop", "hob", tr("Ярмарка академии", "The academy fair"), listOf(
        tr("Мана тратится здесь: на вещи для башни и яйца-сюрпризы.", "Mana is spent here: on things for the tower and surprise eggs."),
        tr("Нажмите на вещь, чтобы примерить её в башне до покупки.", "Tap a thing to try it in the tower before buying."),
        tr("На Солнцеворот яйца стоят вдвое дешевле.", "At the Solstice eggs are half price.")))
    Screen.Settings -> Intro("i.settings", "inkling", tr("Настройки", "Settings"), listOf(
        tr("Где учитесь, шкала оценок, звонки, четверти или семестры.", "Where you study, the marking scale, bells, terms or semesters."),
        tr("Здесь же лазарет: на больничном ничего не сгорает.", "The infirmary is here too: nothing is lost on sick leave."),
        tr("«Подсказки» вернут эти знакомства на всех экранах.", "«Hints» bring these introductions back on every screen.")))
    is Screen.Wrapped -> Intro("i.wrapped", "inkling", tr("Год в академии", "A year in the academy"), listOf(
        tr("Итоги года собраны в карточки – листайте вниз.", "The year's story scrolls card by card."),
        tr("Внизу – открытка, чтобы поделиться годом.", "At the bottom is a card to share the year.")))
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
