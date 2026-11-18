package app.inkling

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.inkling.data.Backup
import app.inkling.data.Settings
import app.inkling.data.Task
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/** Issue #7: the tower moves to a new phone as one file, with its photos. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupTest {
    private val app get() = ApplicationProvider.getApplicationContext<InklingApp>()
    private val photos get() = File(app.filesDir, "photos")

    private fun file(name: String, bytes: ByteArray): Uri = Uri.parse("content://test/$name").also { shadowOf(app.contentResolver).registerInputStream(it, ByteArrayInputStream(bytes)) }

    /** A tower with a subject, a task with a photo of the board and some mana. */
    private fun build() = runBlocking {
        val c = app.container
        c.begin(Settings.SCHOOL, 7, "5", "Аня", listOf("Алгебра"))
        photos.mkdirs(); File(photos, "board_1.jpg").writeBytes(byteArrayOf(1, 2, 3))
        c.dao.insert(Task(subjectId = c.dao.subjectList().first().id, title = "№ 5", created = 1, photo = "content://some.other.install.files/photos/board_1.jpg"))
        c.settings.update { it.copy(mana = 77) }
    }

    @Test fun theTowerMovesWithItsPhotos() = runBlocking {
        val c = app.container
        build()
        val zip = ByteArrayOutputStream().also { Backup.write(c, it) }.toByteArray()
        // the new phone: nothing yet, the welcome screen
        c.dao.wipeTasks(); c.dao.wipeSubjects(); c.dao.wipePets(); photos.deleteRecursively(); c.settings.update { Settings() }

        assertTrue(Backup.read(c, file("inkling.zip", zip)))
        val s = c.settings.now()
        assertTrue(s.onboarded); assertEquals(77, s.mana); assertEquals("Аня", s.student)
        assertEquals(listOf("Алгебра"), c.dao.subjectList().map { it.name })
        assertEquals(listOf("inkling"), c.dao.petList().map { it.species })
        // the photo came along and is found under this install's name
        assertEquals("content://${app.packageName}.files/photos/board_1.jpg", c.dao.taskList().single().photo)
        assertArrayEquals(byteArrayOf(1, 2, 3), File(photos, "board_1.jpg").readBytes())
    }

    @Test fun aBareJsonOfTheFirstVersionsIsReadAndKeepsThePhotos() = runBlocking {
        val c = app.container
        build()
        val json = Backup.export(c).toByteArray()
        c.dao.wipeTasks(); c.settings.update { it.copy(mana = 0) }

        assertTrue(Backup.read(c, file("inkling.json", json)))
        assertEquals(77, c.settings.now().mana)
        assertEquals(1, c.dao.taskList().size)
        assertTrue(File(photos, "board_1.jpg").exists())
    }

    @Test fun aStrangeOrBrokenFileChangesNothing() = runBlocking {
        val c = app.container
        build()
        assertFalse(Backup.read(c, file("notes.txt", "hello".toByteArray())))
        // ours by the name, but a record in it is broken
        assertFalse(Backup.read(c, file("broken.json", """{"app":"inkling","subjects":[{"name":"x"}],"tasks":[]}""".toByteArray())))
        assertEquals(listOf("Алгебра"), c.dao.subjectList().map { it.name })
        assertEquals(1, c.dao.taskList().size)
        assertEquals(77, c.settings.now().mana)
        assertTrue(File(photos, "board_1.jpg").exists())
    }
}
