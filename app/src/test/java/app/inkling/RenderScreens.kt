package app.inkling

import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import app.inkling.data.DemoSeed
import app.inkling.domain.Lang
import app.inkling.ui.Onboarding
import app.inkling.ui.Root
import app.inkling.ui.Start
import app.inkling.ui.share.CardKind
import app.inkling.ui.share.Look
import app.inkling.ui.share.Painter
import app.inkling.ui.share.ShareReq
import app.inkling.ui.theme.InkTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Every screen of the demo drawn to PNGs in app/build/art/screens (run with `-Prender`).
 * The screen is very tall, so whole scrolling pages fit in one picture.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ru-w393dp-h2400dp-xxhdpi")
class RenderScreens {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val out = File(System.getProperty("render.out") ?: "build/art", "screens").apply { mkdirs() }
    private val dark = System.getProperty("render.dark") == "1"

    private fun shot(name: String) {
        repeat(6) { rule.mainClock.advanceTimeBy(400); Thread.sleep(150); org.robolectric.shadows.ShadowLooper.idleMainLooper() }
        val v = rule.activity.window.decorView
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(android.graphics.Canvas(b)) }
        File(out, "${if (dark) "d-" else ""}$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun render() {
        Lang.ru = true
        val c = ApplicationProvider.getApplicationContext<InklingApp>().container
        var i by mutableIntStateOf(-1)
        rule.mainClock.autoAdvance = false
        // onboarding first, on an empty burrow
        rule.setContent { InkTheme(dark) { key(i) { if (i == -2) Onboarding() else { val s by c.settings.settings.collectAsState(null); s?.let { st -> if (i >= 0 && st.onboarded) Root(st) } } } } }
        rule.runOnUiThread { i = -2 }; shot("0-onboarding")
        runBlocking { DemoSeed.seedIfEmpty(c) }
        // the first-visit introductions: one drawn on its own, the rest put away for the screenshots
        runBlocking { c.settings.update { it.copy(seen = it.seen + listOf("i.academy", "i.pets", "i.stats", "i.candle", "i.grimoire", "i.chest", "i.teach", "i.wand", "i.timetable",
            "i.exam", "i.pet", "i.shop", "i.settings", "i.wrapped")) } }
        val pets = runBlocking { c.dao.petList() }
        val subj = runBlocking { c.dao.subjectList().first() }
        val exam = runBlocking { c.dao.examList().first { !it.done } }
        val task = runBlocking { c.dao.taskList().first { it.big } }
        val mat = runBlocking { c.dao.materialList().first() }
        val shots = listOf(
            "1-home" to (0 to null), "2-academy" to (1 to null), "3-pets" to (2 to null), "4-stats" to (3 to null),
            "5-pet" to (2 to "pet:${pets.first { it.slot == 0 }.id}"), "6-add" to (0 to "add"), "7-candle" to (0 to "candle"), "8-study" to (0 to "study"),
            "9-grimoire" to (0 to "grimoire"), "10-quiz" to (0 to "quiz"), "11-chest" to (0 to "chest"), "12-teach" to (0 to "teach"), "13-wand" to (0 to "wand"),
            "14-subject" to (1 to "subject:${subj.id}"), "15-subjects" to (1 to "subjects"), "16-timetable" to (1 to "timetable"), "17-exam" to (1 to "exam:${exam.id}"),
            "18-task" to (1 to "task:${task.id}"), "19-material" to (1 to "material:${mat.id}"), "20-grades" to (1 to "grades"),
            "21-shop" to (0 to "shop"), "22-settings" to (0 to "settings"), "23-wrapped" to (3 to "wrapped"), "24-share" to (0 to "share:WEEK"),
        )
        for ((n, sh) in shots.withIndex()) {
            Start.tab = sh.second.first; Start.open = sh.second.second
            rule.runOnUiThread { i = n }
            shot(sh.first)
        }
        // every piece of furniture at once, to check the layout
        runBlocking { c.settings.update { it.copy(items = app.inkling.domain.Shop.all.map { i -> i.id }.toSet(), hidden = emptySet()) } }
        Start.tab = 0; Start.open = null; rule.runOnUiThread { i = 300 }; shot("1c-home-full")
        val data = runBlocking { app.inkling.ui.Data(c.settings.now(), c.dao.subjectList(), c.dao.slotList(), c.dao.attendList(), c.dao.studyList().sortedByDescending { it.start },
            c.dao.materialList(), c.dao.taskList(), c.dao.examList(), c.dao.ticketList(), c.dao.spellList(), c.dao.questionList(), c.dao.gradeList(), c.dao.petList()) }
        val cards = File(out.parentFile, "cards").apply { mkdirs() }
        val reqs = listOf(ShareReq(CardKind.WEEK), ShareReq(CardKind.MONTH), ShareReq(CardKind.YEAR), ShareReq(CardKind.TERM), ShareReq(CardKind.MATERIAL), ShareReq(CardKind.TRIAL),
            ShareReq(CardKind.PET, pets.first().id), ShareReq(CardKind.TOWER), ShareReq(CardKind.COLLECTION), ShareReq(CardKind.WAND), ShareReq(CardKind.BOOK))
        reqs.forEachIndexed { k, r -> listOf(false, true).forEach { story ->
            val bmp = Painter(rule.activity, data, Look(story, k % 6, false)).render(r).first
            File(cards, "${k}-${r.kind.name.lowercase()}${if (story) "-story" else ""}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) } } }
    }
}
