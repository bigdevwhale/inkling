package app.inkling.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.inkling.data.Subject
import app.inkling.domain.fmtDayShort
import app.inkling.domain.hm
import app.inkling.domain.relDay
import app.inkling.domain.tr
import app.inkling.ui.theme.C
import app.inkling.ui.theme.T
import java.time.LocalDate

/** Subjects as chips with their school's glyph; [sel] 0 = none chosen. */
@Composable
fun SubjectPicker(subjects: List<Subject>, sel: Long, modifier: Modifier = Modifier, allowNone: Boolean = false, onSel: (Long) -> Unit) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (allowNone) Chip(tr("Без предмета", "No subject"), sel == 0L) { onSel(0) }
        subjects.forEach { s ->
            val on = s.id == sel; val col = subjectColor(s)
            Row(Modifier.clip(RoundedCornerShape(50)).background(if (on) col else C.card2).clickable { onSel(s.id) }.padding(start = 8.dp, end = 13.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(schoolIcon(s.school), if (on) C.accInk else col, 16.dp); Spacer(Modifier.width(5.dp))
                Text(s.name, style = T.smallB.copy(color = if (on) C.accInk else C.ink), maxLines = 1)
            }
        }
    }
}

/** A day: a few quick chips and a calendar for any other. [none] adds «no date». */
@Composable
fun DayPicker(day: Long, today: Long, modifier: Modifier = Modifier, none: Boolean = false, future: Boolean = false, onSel: (Long) -> Unit) {
    val ctx = LocalContext.current
    val quick = if (future) listOf(today, today + 1, today + 2, today + 7) else listOf(today, today - 1, today - 2)
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (none) Chip(tr("Без срока", "No date"), day == 0L) { onSel(0) }
        quick.forEach { d -> Chip(if (d == today + 7) tr("через неделю", "in a week") else relDay(d, today), day == d) { onSel(d) } }
        val other = day != 0L && day !in quick
        Chip(if (other) fmtDayShort(day) else tr("другой день…", "another day…"), other) {
            val d = LocalDate.ofEpochDay(if (day == 0L) today else day)
            DatePickerDialog(ctx, { _, y, m, dd -> onSel(LocalDate.of(y, m + 1, dd).toEpochDay()) }, d.year, d.monthValue - 1, d.dayOfMonth).show()
        }
    }
}

/** Opens the calendar straight away. */
fun pickDate(ctx: android.content.Context, day: Long, onSel: (Long) -> Unit) {
    val d = LocalDate.ofEpochDay(day)
    DatePickerDialog(ctx, { _, y, m, dd -> onSel(LocalDate.of(y, m + 1, dd).toEpochDay()) }, d.year, d.monthValue - 1, d.dayOfMonth).show()
}

/** Opens the clock for minutes of the day. */
fun pickTime(ctx: android.content.Context, min: Int, onSel: (Int) -> Unit) {
    TimePickerDialog(ctx, { _, h, m -> onSel(h * 60 + m) }, min / 60, min % 60, true).show()
}

/** «9:00» as a chip that opens the clock. */
@Composable
fun TimeChip(min: Int, onSel: (Int) -> Unit) {
    val ctx = LocalContext.current
    Chip(hm(min), false) { pickTime(ctx, min, onSel) }
}
