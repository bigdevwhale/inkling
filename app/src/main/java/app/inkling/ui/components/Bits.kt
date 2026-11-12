package app.inkling.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.inkling.data.Subject
import app.inkling.domain.Schools
import app.inkling.domain.fmt
import app.inkling.domain.tr
import app.inkling.ui.art.Prop
import app.inkling.ui.theme.C
import app.inkling.ui.theme.T

/** A school's colour, a touch lighter at night. */
fun schoolColor(id: String?): Color { val c = Color(Schools.of(id ?: "own").color); return if (C.dark) lerp(c, Color.White, .18f) else c }
fun subjectColor(s: Subject?): Color = schoolColor(s?.school)

/** The glyph of a school of magic. */
fun schoolIcon(id: String?): Icons = when (id) {
    "num" -> Icons.SIGMA; "art" -> Icons.GEAR2; "star" -> Icons.STAR; "alch" -> Icons.FLASK; "heal" -> Icons.CROSS; "herb" -> Icons.LEAF
    "rune" -> Icons.RUNE; "chron" -> Icons.HOURGLASS; "seal" -> Icons.SCALES; "trade" -> Icons.COIN; "mind" -> Icons.EYE; "illus" -> Icons.MASK
    "body" -> Icons.SHIELD; else -> Icons.CIRCLE
}

/** A subject's mark: its school's glyph in a soft circle of the school's colour. */
@Composable
fun SchoolMark(school: String?, size: Dp = 40.dp) {
    val col = schoolColor(school)
    Box(Modifier.size(size).clip(CircleShape).background(col.copy(alpha = .16f)), contentAlignment = Alignment.Center) { Icon(schoolIcon(school), col, size * .52f) }
}

/** Mana as a little pill: the crystal and the number. */
@Composable
fun ManaPill(n: Int, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(modifier.clip(RoundedCornerShape(50)).background(C.mana.copy(alpha = .14f)).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Prop("mana", 20.dp); Spacer(Modifier.width(4.dp))
        Text(fmt(n), style = T.smallB.copy(color = C.ink))
    }
}

/** A tile with a picture, a title and a line about it. */
@Composable
fun StoreTile(prop: String, title: String, line: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.card(18.dp).clickable(onClick = onClick).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Prop(prop, 36.dp); Spacer(Modifier.width(8.dp))
            Text(title, style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        Text(line, style = T.small, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Three honest answers to the sphinx. */
@Composable
fun RecallRow(onAnswer: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(1 to tr("Помню", "I remember"), 2 to tr("Смутно", "Vaguely"), 3 to tr("Не помню", "Forgot")).forEach { (a, l) ->
            val col = when (a) { 1 -> C.ok; 2 -> C.gold; else -> C.bad }
            Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(col.copy(alpha = .15f)).clickable { onAnswer(a) }.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                Text(l, style = T.bodyB.copy(color = col))
            }
        }
    }
}

/** Four levels of knowing a ticket. */
@Composable
fun LevelRow(level: Int, modifier: Modifier = Modifier, onSet: (Int) -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(tr("Не знаю", "No idea"), tr("Слабо", "Weak"), tr("Знаю", "Know"), tr("Отлично", "Sure")).forEachIndexed { i, l ->
            val col = levelColor(i)
            Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (level == i) col else col.copy(alpha = .13f)).clickable { onSet(i) }.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                Text(l, style = T.smallB.copy(color = if (level == i) Color.White else col), maxLines = 1)
            }
        }
    }
}
fun levelColor(l: Int) = when (l) { 0 -> C.bad; 1 -> C.gold; 2 -> C.acc; else -> C.ok }

@Composable
fun Dim(on: Boolean, content: @Composable () -> Unit) = Box(Modifier.alpha(if (on) .45f else 1f)) { content() }
