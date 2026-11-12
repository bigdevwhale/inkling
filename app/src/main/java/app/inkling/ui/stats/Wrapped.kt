package app.inkling.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.inkling.data.Study
import app.inkling.domain.Engine
import app.inkling.domain.Schools
import app.inkling.domain.Span
import app.inkling.domain.Stage
import app.inkling.domain.Stats
import app.inkling.domain.cap
import app.inkling.domain.count
import app.inkling.domain.dur
import app.inkling.domain.monthName
import app.inkling.domain.stage
import app.inkling.domain.tr
import app.inkling.ui.Data
import app.inkling.ui.Nav
import app.inkling.ui.art.Creature
import app.inkling.ui.art.Pose
import app.inkling.ui.art.Prop
import app.inkling.ui.components.Btn
import app.inkling.ui.components.Empty
import app.inkling.ui.components.Page
import app.inkling.ui.components.SchoolMark
import app.inkling.ui.share.CardKind
import app.inkling.ui.share.ShareReq
import app.inkling.ui.theme.C
import app.inkling.ui.theme.T
import java.time.LocalDate

/** «A year in the academy»: the year's story told card by card. */
@Composable
fun WrappedPage(year: Int, d: Data, nav: Nav) {
    val from = LocalDate.of(year, 1, 1).toEpochDay(); val to = LocalDate.of(year, 12, 31).toEpochDay()
    val st = Stats(d.studies, Span(from, to, "$year"), d.subById, d.today)
    Page(tr("Год в академии · $year", "A year in the academy · $year"), { nav.pop() }) {
        if (st.count == 0) { Empty(tr("В этом году занятий нет", "No study this year")); return@Page }
        @Composable fun Leaf(bg: Color, content: @Composable () -> Unit) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(24.dp)).background(bg).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) { content() }
        }
        Leaf(C.accSoft) {
            Prop("candle", 90.dp)
            Text(dur(st.minutes * 60), style = T.big, modifier = Modifier.padding(top = 8.dp))
            Text(tr("учёбы за год – ${count(st.days, "день", "дня", "дней", "day", "days")}, ${count(st.count, "занятие", "занятия", "занятий", "session", "sessions")}",
                "of study this year – ${count(st.days, "day", "days", "days", "day", "days")}, ${count(st.count, "session", "sessions", "sessions", "session", "sessions")}"), style = T.body, textAlign = TextAlign.Center)
        }
        st.bySubject().firstOrNull()?.let { top -> Leaf(C.goldSoft) {
            SchoolMark(top.subject?.school, 80.dp)
            Text(tr("Главный предмет года", "The subject of the year"), style = T.sec, modifier = Modifier.padding(top = 8.dp))
            Text(top.subject?.name ?: "–", style = T.title, textAlign = TextAlign.Center)
            Text(tr("${dur(top.minutes * 60)} – ${(top.share * 100).toInt()}% всей учёбы", "${dur(top.minutes * 60)} – ${(top.share * 100).toInt()}% of all study"), style = T.body)
            top.subject?.let { Text(Schools.of(it.school).title, style = T.small) }
        } }
        Leaf(C.card) {
            Text(tr("Звезда школ", "The star of schools"), style = T.sec)
            SchoolStar(st.bySchool(), Modifier.padding(top = 8.dp).height(200.dp).fillMaxWidth())
            Text(count(st.bySchool().size, "школа магии", "школы магии", "школ магии", "school of magic", "schools of magic"), style = T.h3)
        }
        val ms = (1..12).map { m -> val a = LocalDate.of(year, m, 1).toEpochDay(); val z = LocalDate.of(year, m, 1).plusMonths(1).toEpochDay() - 1
            m to st.studies.filter { it.day in a..z }.sumOf { it.minutes } }.filter { it.second > 0 }
        if (ms.size >= 2) Leaf(C.card) { val hi = ms.maxBy { it.second }
            Text(tr("Самый учёный месяц", "The most studious month"), style = T.sec)
            Text(monthName(hi.first).cap(), style = T.title); Text(dur(hi.second * 60), style = T.body) }
        val spells = d.spells.filter { it.created in from..to }
        if (spells.isNotEmpty()) Leaf(C.accSoft) {
            Creature("sphinx", Stage.ADULT, Pose.WORK, 130.dp)
            Text(count(spells.size, "заклинание", "заклинания", "заклинаний", "spell", "spells"), style = T.title)
            Text(tr("записано в гримуар, выучено ${spells.count { Engine.learned(it) }}", "written in the grimoire, ${spells.count { Engine.learned(it) }} learned"), style = T.body)
        }
        val teach = st.studies.count { it.kind == Study.TEACH }
        if (teach > 0) Leaf(C.okSoft) {
            Creature("raven", Stage.ADULT, Pose.WORK, 120.dp)
            Text(count(teach, "объяснение", "объяснения", "объяснений", "explanation", "explanations"), style = T.title)
            Text(tr("ворону-эху – а значит, и себе", "to the echo raven – and so to yourself"), style = T.body)
        }
        val fin = d.materials.filter { it.finished in from..to }
        if (fin.isNotEmpty()) Leaf(C.goldSoft) {
            Prop("books", 80.dp)
            Text(count(fin.size, "материал закончен", "материала закончено", "материалов закончено", "material finished", "materials finished"), style = T.title)
            Text(fin.joinToString(", ") { it.title }, style = T.body, textAlign = TextAlign.Center)
        }
        val trials = d.exams.filter { it.done && it.day in from..to }
        if (trials.isNotEmpty()) Leaf(C.card) {
            Prop("scroll", 80.dp)
            Text(count(trials.size, "испытание пройдено", "испытания пройдено", "испытаний пройдено", "trial passed", "trials passed"), style = T.title)
        }
        val born = d.pets.filter { it.hatched in from..to }
        if (born.isNotEmpty()) Leaf(C.card) {
            Text(tr("Вылупились в этом году", "Hatched this year"), style = T.sec)
            Row(Modifier.padding(top = 8.dp)) { born.take(5).forEach { p -> Creature(p.species, p.stage(), Pose.IDLE, 58.dp, shiny = p.shiny); Spacer(Modifier.width(2.dp)) } }
            Text(count(born.size, "существо", "существа", "существ", "creature", "creatures"), style = T.title)
        }
        Btn(tr("Поделиться годом", "Share the year"), Modifier.fillMaxWidth().padding(top = 16.dp)) { nav.share = ShareReq(CardKind.YEAR, year.toLong()) }
    }
}
