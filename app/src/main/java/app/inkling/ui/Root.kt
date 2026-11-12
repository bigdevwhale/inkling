package app.inkling.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.inkling.BuildConfig
import app.inkling.container
import app.inkling.data.Attend
import app.inkling.data.Exam
import app.inkling.data.Grade
import app.inkling.data.Material
import app.inkling.data.Pet
import app.inkling.data.Question
import app.inkling.data.Settings
import app.inkling.data.Slot
import app.inkling.data.Spell
import app.inkling.data.Study
import app.inkling.data.Subject
import app.inkling.data.Task
import app.inkling.data.Ticket
import app.inkling.domain.Engine
import app.inkling.domain.today
import app.inkling.domain.tr
import app.inkling.ui.academy.AcademyScreen
import app.inkling.ui.academy.ExamPage
import app.inkling.ui.academy.GradesPage
import app.inkling.ui.academy.MaterialPage
import app.inkling.ui.academy.SubjectPage
import app.inkling.ui.academy.SubjectsPage
import app.inkling.ui.academy.TaskPage
import app.inkling.ui.academy.TimetablePage
import app.inkling.ui.components.Icon
import app.inkling.ui.components.Icons
import app.inkling.ui.home.AddSheet
import app.inkling.ui.home.HomeScreen
import app.inkling.ui.home.Inbox
import app.inkling.ui.magic.CandlePage
import app.inkling.ui.magic.ChestPage
import app.inkling.ui.magic.GrimoirePage
import app.inkling.ui.magic.StudyPage
import app.inkling.ui.magic.TeachPage
import app.inkling.ui.magic.WandPage
import app.inkling.ui.pets.PetPage
import app.inkling.ui.pets.PetsScreen
import app.inkling.ui.share.ShareReq
import app.inkling.ui.share.ShareSheet
import app.inkling.ui.stats.StatsScreen
import app.inkling.ui.stats.WrappedPage
import app.inkling.ui.tower.SettingsPage
import app.inkling.ui.tower.ShopPage
import app.inkling.ui.theme.C
import app.inkling.ui.theme.T
import kotlinx.coroutines.delay

/** Where a demo/screenshot launch opens (set from the intent in demo builds only). */
object Start { var tab = 0; var open: String? = null }

/** A page over the tabs. */
sealed class Screen {
    data class PetInfo(val id: Long) : Screen()
    data object Shop : Screen()
    data object Settings : Screen()
    data object Candle : Screen()
    /** A study written by hand ([id] 0) or an existing one. */
    data class StudyEdit(val id: Long = 0, val subjectId: Long = 0) : Screen()
    data class Teach(val subjectId: Long = 0) : Screen()
    data class Grimoire(val quiz: Boolean = false) : Screen()
    data object Chest : Screen()
    data object Wand : Screen()
    data object Subjects : Screen()
    data class SubjectInfo(val id: Long) : Screen()
    data class MaterialInfo(val id: Long, val subjectId: Long = 0) : Screen()
    data class TaskInfo(val id: Long, val subjectId: Long = 0, val kind: String = Task.HW) : Screen()
    data class ExamInfo(val id: Long, val subjectId: Long = 0) : Screen()
    data object Timetable : Screen()
    data object Grades : Screen()
    data class Wrapped(val year: Int) : Screen()
}

class Nav {
    var tab by mutableIntStateOf(0)
    val stack = mutableStateListOf<Screen>()
    var share by mutableStateOf<ShareReq?>(null)
    var add by mutableStateOf(false)
    fun push(s: Screen) { stack.add(s) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
}

/** Everything the screens read, collected once. */
class Data(val s: Settings, val subjects: List<Subject>, val slots: List<Slot>, val attends: List<Attend>, val studies: List<Study>, val materials: List<Material>,
           val tasks: List<Task>, val exams: List<Exam>, val tickets: List<Ticket>, val spells: List<Spell>, val questions: List<Question>, val grades: List<Grade>,
           val pets: List<Pet>, val today: Long = today()) {
    val subById = subjects.associateBy { it.id }
    val active = subjects.filter { !it.archived }
    val team = Engine.team(pets)
    val leader = team.firstOrNull { it.slot == 0 } ?: pets.firstOrNull { it.hatched > 0 }
    val first = studies.minOfOrNull { it.day } ?: today
    fun school(subjectId: Long) = subById[subjectId]?.school ?: "own"
    val due = spells.filter { it.due <= today && !Engine.learned(it) }
    val openTasks = tasks.filter { it.open }.sortedWith(compareBy({ if (it.due == 0L) Long.MAX_VALUE else it.due }, { it.id }))
    val upcoming = exams.filter { !it.done && it.day >= today }.sortedBy { it.day }
    val finished = materials.filter { it.finished > 0 }.sortedBy { it.finished }
}

@Composable
fun Root(s: Settings) {
    val c = LocalContext.current.container
    val subjects by c.dao.subjects().collectAsState(initial = emptyList())
    val slots by c.dao.slots().collectAsState(initial = emptyList())
    val attends by c.dao.attends().collectAsState(initial = emptyList())
    val studies by c.dao.studies().collectAsState(initial = emptyList())
    val materials by c.dao.materials().collectAsState(initial = emptyList())
    val tasks by c.dao.tasks().collectAsState(initial = emptyList())
    val exams by c.dao.exams().collectAsState(initial = emptyList())
    val tickets by c.dao.tickets().collectAsState(initial = emptyList())
    val spells by c.dao.spells().collectAsState(initial = emptyList())
    val questions by c.dao.questions().collectAsState(initial = emptyList())
    val grades by c.dao.grades().collectAsState(initial = emptyList())
    val pets by c.dao.pets().collectAsState(initial = emptyList())
    val data = remember(s, subjects, slots, attends, studies, materials, tasks, exams, tickets, spells, questions, grades, pets) {
        Data(s, subjects, slots, attends, studies, materials, tasks, exams, tickets, spells, questions, grades, pets, c.clock()) }
    val nav = remember { Nav().apply { tab = Start.tab } }
    var note by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { c.news.collect { note = it; delay(2600); note = null } }
    LaunchedEffect(Unit) { c.tick() }
    LaunchedEffect(Unit) { if (!BuildConfig.SHOTS) while (true) { c.refreshOutside(); delay(30 * 60_000L) } }
    // a candle burning when the app opens: straight to it
    LaunchedEffect(Unit) { if (s.candle.start > 0 || s.candle.restUntil > 0) nav.push(Screen.Candle) }
    LaunchedEffect(Unit) {
        when (val o = Start.open) {
            null -> {}
            "shop" -> nav.push(Screen.Shop); "settings" -> nav.push(Screen.Settings); "candle" -> nav.push(Screen.Candle); "teach" -> nav.push(Screen.Teach())
            "grimoire" -> nav.push(Screen.Grimoire()); "quiz" -> nav.push(Screen.Grimoire(true)); "chest" -> nav.push(Screen.Chest); "wand" -> nav.push(Screen.Wand)
            "subjects" -> nav.push(Screen.Subjects); "timetable" -> nav.push(Screen.Timetable); "grades" -> nav.push(Screen.Grades); "add" -> nav.add = true
            "study" -> nav.push(Screen.StudyEdit())
            "wrapped" -> nav.push(Screen.Wrapped(java.time.LocalDate.now().year))
            else -> {
                val id = o.substringAfter(':').toLongOrNull() ?: -1
                when {
                    o.startsWith("pet:") -> nav.push(Screen.PetInfo(id))
                    o.startsWith("subject:") -> nav.push(Screen.SubjectInfo(id))
                    o.startsWith("material:") -> nav.push(Screen.MaterialInfo(id))
                    o.startsWith("task:") -> nav.push(Screen.TaskInfo(id))
                    o.startsWith("exam:") -> nav.push(Screen.ExamInfo(id))
                    o.startsWith("share:") -> nav.share = ShareReq(app.inkling.ui.share.CardKind.valueOf(o.drop(6).substringBefore(':')), o.drop(6).substringAfter(':', "0").toLong())
                }
            }
        }
        Start.open = null
    }

    Box(Modifier.fillMaxSize().background(C.bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).statusBarsPadding()) {
                when (nav.tab) {
                    0 -> HomeScreen(data, nav)
                    1 -> AcademyScreen(data, nav)
                    2 -> PetsScreen(data, nav)
                    else -> StatsScreen(data, nav)
                }
            }
            Dock(s, nav.tab, { nav.tab = it }) { nav.add = true }
        }
        for (scr in nav.stack) Box(Modifier.fillMaxSize()) {
            when (scr) {
                is Screen.PetInfo -> PetPage(scr.id, data, nav)
                Screen.Shop -> ShopPage(data, nav)
                Screen.Settings -> SettingsPage(data, nav)
                Screen.Candle -> CandlePage(data, nav)
                is Screen.StudyEdit -> StudyPage(scr, data, nav)
                is Screen.Teach -> TeachPage(scr.subjectId, data, nav)
                is Screen.Grimoire -> GrimoirePage(scr.quiz, data, nav)
                Screen.Chest -> ChestPage(data, nav)
                Screen.Wand -> WandPage(data, nav)
                Screen.Subjects -> SubjectsPage(data, nav)
                is Screen.SubjectInfo -> SubjectPage(scr.id, data, nav)
                is Screen.MaterialInfo -> MaterialPage(scr, data, nav)
                is Screen.TaskInfo -> TaskPage(scr, data, nav)
                is Screen.ExamInfo -> ExamPage(scr, data, nav)
                Screen.Timetable -> TimetablePage(data, nav)
                Screen.Grades -> GradesPage(data, nav)
                is Screen.Wrapped -> WrappedPage(scr.year, data, nav)
            }
        }
        if (nav.add) AddSheet(data, nav) { nav.add = false }
        if (nav.stack.isEmpty() && !nav.add && !BuildConfig.SHOTS) Inbox(data, nav)
        // a first-visit introduction to whatever is on top (after any news)
        if (!BuildConfig.SHOTS && s.inbox.isEmpty() && nav.share == null && !nav.add) introFor(nav.stack.lastOrNull(), nav.tab, s)?.takeIf { it.id !in s.seen }?.let { IntroSheet(it) }
        nav.share?.let { ShareSheet(it, data) { nav.share = null } }
        AnimatedVisibility(note != null, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp, start = 18.dp, end = 18.dp), enter = fadeIn(), exit = fadeOut()) {
            Text(note ?: "", style = T.bodyB.copy(color = C.bg), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.ink).padding(horizontal = 16.dp, vertical = 11.dp))
        }
    }
}

@Composable
private fun Dock(s: Settings, sel: Int, onSel: (Int) -> Unit, onAdd: () -> Unit) {
    val tabs = listOf(tr("Башня", "Tower") to Icons.TOWER, (if (s.self) tr("Учёба", "Study") else tr("Академия", "Academy")) to Icons.BOOK,
        tr("Существа", "Creatures") to Icons.PAW, tr("Летопись", "Chronicle") to Icons.STATS)
    Row(Modifier.fillMaxWidth().shadow(12.dp, ambientColor = C.shadow, spotColor = C.shadow).background(C.nav).navigationBarsPadding().padding(top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        tabs.forEachIndexed { i, (label, ic) ->
            if (i == 2) Box(Modifier.weight(1.1f), contentAlignment = Alignment.Center) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(C.acc).clickable(onClick = onAdd), contentAlignment = Alignment.Center) { Icon(Icons.WAND, C.accInk, 28.dp) }
            }
            val col = if (i == sel) C.acc else C.mute
            Column(Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) { onSel(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(ic, col, 24.dp)
                Text(label, style = T.tiny.copy(color = col), modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
