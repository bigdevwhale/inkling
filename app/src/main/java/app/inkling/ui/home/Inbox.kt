package app.inkling.ui.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.inkling.container
import app.inkling.domain.Pets
import app.inkling.domain.Quests
import app.inkling.domain.Stage
import app.inkling.domain.Feast
import app.inkling.domain.Timetable
import app.inkling.domain.Words
import app.inkling.domain.cap
import app.inkling.domain.dur
import app.inkling.domain.title
import app.inkling.domain.tr
import app.inkling.ui.Data
import app.inkling.ui.Nav
import app.inkling.ui.Screen
import app.inkling.ui.art.Creature
import app.inkling.ui.art.EggView
import app.inkling.ui.art.Pose
import app.inkling.ui.art.Prop
import app.inkling.ui.components.Btn
import app.inkling.ui.components.Centered
import app.inkling.ui.components.Popup
import app.inkling.ui.share.CardKind
import app.inkling.ui.share.ShareReq
import app.inkling.ui.theme.C
import app.inkling.ui.theme.T

/** News waiting in Settings.inbox, one at a time: eggs, hatchings, growth, quests, finished materials, trials, festivals. */
@Composable
fun Inbox(d: Data, nav: Nav) {
    val item = d.s.inbox.firstOrNull() ?: return
    val c = LocalContext.current.container
    fun done() = c.write { settings.update { it.copy(inbox = it.inbox.drop(1)) } }
    val parts = item.split(':')
    var title = ""; var text = ""; var pic: @Composable () -> Unit = {}; var action: Pair<String, () -> Unit>? = null
    when (parts[0]) {
        "egg" -> { val sp = Pets.of(parts[1]); val why = parts.getOrNull(2)
            title = tr("Новое яйцо!", "A new egg!")
            text = if (why == "fair") tr("Яйцо с ярмарки академии. Учёба его согреет, и скоро оно проклюнется.", "An egg from the academy fair. Study will warm it, and soon it cracks.")
                else tr("Это награда за то, как вы учитесь. Кто внутри? Позанимайтесь – и узнаете.", "A reward for the way you study. Who is inside? Study a little and see.")
            pic = { EggView(sp.id, 150.dp, .6f) } }
        "hatch" -> { val p = d.pets.firstOrNull { it.id == parts[1].toLongOrNull() } ?: run { done(); return }
            val sp = Pets.of(p.species)
            title = tr("В башне новый жилец!", "A new lodger in the tower!")
            text = tr("Знакомьтесь: ${p.name}, ${sp.ru.lowercase()}. ${sp.perkRu}.", "Meet ${p.name}, the ${sp.en.lowercase()}. ${sp.perkEn}.")
            pic = { Creature(p.species, Stage.BABY, Pose.CHEER, 160.dp, shiny = p.shiny) }
            if (p.slot !in 0..2) action = tr("Поселить в башне", "Bring into the tower") to { c.write { val free = (0..2).firstOrNull { sl -> d.pets.none { it.slot == sl } } ?: 1; setSlot(p, free) } } }
        "stage" -> { val p = d.pets.firstOrNull { it.id == parts[1].toLongOrNull() } ?: run { done(); return }
            val st = Stage.entries[parts[2].toInt()]
            title = tr("${p.name} подрос!", "${p.name} grew up!")
            text = tr("За ночь знания улеглись, и ${p.name} теперь «${st.title().lowercase()}».", "Overnight the learning settled, and ${p.name} is now a «${st.title().lowercase()}».")
            pic = { Creature(p.species, st, Pose.CHEER, 160.dp, shiny = p.shiny) } }
        "quest" -> { val q = Quests.byId[parts[1]] ?: run { done(); return }
            title = tr("Задание выполнено", "Quest done"); text = q.title + "\n+${q.mana} " + tr("маны", "mana")
            pic = { Prop("medal", 110.dp) } }
        "material" -> { val m = d.materials.firstOrNull { it.id == parts[1].toLongOrNull() } ?: run { done(); return }
            title = tr("Материал закончен!", "Material finished!"); text = tr("«${m.title}» встаёт корешком на полку в башне.", "«${m.title}» takes its place on the tower shelf.")
            pic = { Prop("books", 120.dp) }
            action = tr("Поделиться", "Share") to { nav.share = ShareReq(CardKind.MATERIAL, m.id) } }
        "learned" -> { val sp = d.spells.firstOrNull { it.id == parts[1].toLongOrNull() } ?: run { done(); return }
            title = tr("Заклинание выучено", "A spell learned"); text = tr("«${sp.title}» – пять раз вспомнили, и теперь оно светится золотом.", "«${sp.title}» – recalled five times; now it glows gold.")
            pic = { Prop("book", 120.dp) } }
        "defended" -> { val t = d.tasks.firstOrNull { it.id == parts[1].toLongOrNull() } ?: run { done(); return }
            title = tr("Великое делание завершено!", "The great work is done!"); text = tr("«${t.title}» – позади. Дракон гордится вами.", "«${t.title}» is behind you. The dragon is proud.")
            pic = { Creature("dragon", Stage.ADULT, Pose.CHEER, 160.dp) } }
        "trial" -> { val e = d.exams.firstOrNull { it.id == parts[1].toLongOrNull() } ?: run { done(); return }
            title = tr("Испытание пройдено!", "Trial passed!"); text = "${Words.examKind(e.kind)} · ${e.title.ifBlank { d.subById[e.subjectId]?.name.orEmpty() }}"
            pic = { Prop("scroll", 120.dp) }
            action = tr("Поделиться", "Share") to { nav.share = ShareReq(CardKind.TRIAL, e.id) } }
        "feast" -> { val f = runCatching { Feast.valueOf(parts[1]) }.getOrNull() ?: run { done(); return }
            title = f.title; text = f.text; pic = { Prop("star", 110.dp) } }
        "term" -> { val start = parts[1].toLong()
            title = tr("${Words.term(d.s).cap()} позади!", "The ${Words.term(d.s)} is over!")
            val term = Timetable.terms(d.s).firstOrNull { it.first == start }
            val min = d.studies.filter { term != null && it.day in term }.sumOf { it.minutes }
            text = tr("За ${Words.term(d.s)} – ${dur(min * 60)} учёбы. Существа получили табель.", "${dur(min * 60)} of study this ${Words.term(d.s)}. Your creatures got their report.")
            pic = { Prop("scroll", 120.dp) }
            action = tr("Поделиться табелем", "Share the report") to { nav.share = ShareReq(CardKind.TERM, start) } }
        else -> { done(); return }
    }
    Popup(::done) {
        Centered { pic() }
        Spacer(Modifier.height(10.dp))
        Text(title, style = T.h2, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(text, style = T.body.copy(color = C.mute), modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        action?.let { (l, f) -> Btn(l, Modifier.fillMaxWidth()) { f(); done() }; Spacer(Modifier.height(8.dp)) }
        Btn(tr("Чудесно", "Lovely"), Modifier.fillMaxWidth(), color = if (action != null) C.card2 else C.acc, ink = if (action != null) C.ink else C.accInk) { done() }
    }
}

