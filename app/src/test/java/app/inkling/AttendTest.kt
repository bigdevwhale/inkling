package app.inkling

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import app.inkling.data.Attend
import app.inkling.data.Settings
import app.inkling.data.Slot
import app.inkling.domain.Lang
import app.inkling.domain.Timetable
import app.inkling.domain.today
import app.inkling.ui.Root
import app.inkling.ui.theme.InkTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Issue #1: answering «were you at the lesson?» on the home screen must not crash. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ru-w393dp-h2400dp-xxhdpi")
class AttendTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun answer(label: String, state: Int) {
        Lang.ru = true
        val c = ApplicationProvider.getApplicationContext<InklingApp>().container
        val t = today()
        runBlocking {
            c.begin(Settings.SCHOOL, 7, "5", "", listOf("Алгебра"))
            // no terms, so today is never a holiday; every introduction already seen
            c.settings.update { it.copy(terms = emptyList(), seen = it.seen + listOf("i.home", "i.academy", "i.pets", "i.stats", "i.candle", "i.grimoire", "i.chest", "i.teach",
                "i.wand", "i.timetable", "i.exam", "i.pet", "i.shop", "i.settings", "i.wrapped")) }
            val sub = c.dao.subjectList().first()
            c.dao.insert(Slot(subjectId = sub.id, weekday = Timetable.weekday(t), start = 0, end = 1))
        }
        rule.setContent { InkTheme(false) { val s by c.settings.settings.collectAsState(null); s?.let { Root(it) } } }
        rule.waitForIdle()
        rule.waitUntil(5000) { rule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(label).performClick()
        // the mark is written off the main thread: wait for it, drawing the screen meanwhile (that's where it crashed)
        repeat(100) { if (runBlocking { c.dao.attendList() }.isEmpty()) { Thread.sleep(50); org.robolectric.shadows.ShadowLooper.idleMainLooper(); rule.waitForIdle() } }
        assertEquals(listOf(state), runBlocking { c.dao.attendList() }.map { it.state })
        // «was there» opens «what was set?»
        if (state == Attend.WAS) rule.onNodeWithText("Что задали?").assertExists()
    }

    @Test fun was() = answer("Был(а)", Attend.WAS)
    @Test fun missed() = answer("Пропуск", Attend.MISSED)
    @Test fun cancelled() = answer("Отменили", Attend.CANCELLED)
}
