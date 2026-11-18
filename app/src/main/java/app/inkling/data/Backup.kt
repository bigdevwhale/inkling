package app.inkling.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import app.inkling.AppContainer
import app.inkling.domain.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The whole tower as one file, to keep or to carry to another phone: a zip with `inkling.json` (every table and
 * the settings) and the photos of the board. A bare JSON file, as the first versions saved, is read too.
 * Import replaces everything.
 */
object Backup {
    private const val JSON = "inkling.json"
    private const val PHOTOS = "photos"
    fun fileName() = "inkling-${LocalDate.now()}.zip"

    /** Writes the tower into [out] and closes it. */
    suspend fun write(c: AppContainer, out: OutputStream) {
        val text = export(c)
        withContext(Dispatchers.IO) {
            ZipOutputStream(out).use { z ->
                z.putNextEntry(ZipEntry(JSON)); z.write(text.toByteArray()); z.closeEntry()
                File(c.app.filesDir, PHOTOS).listFiles()?.filter { it.isFile }?.forEach { f ->
                    z.putNextEntry(ZipEntry("$PHOTOS/${f.name}")); f.inputStream().use { it.copyTo(z) }; z.closeEntry() }
            }
        }
    }

    /** Reads a tower from [uri] – a zip or a bare JSON; false when the file isn't one of ours. Nothing changes then. */
    suspend fun read(c: AppContainer, uri: Uri): Boolean {
        val fresh = File(c.app.filesDir, "$PHOTOS.new")
        val text = withContext(Dispatchers.IO) {
            runCatching { c.app.contentResolver.openInputStream(uri)?.use { unpack(it, fresh) } }.getOrNull()
        }
        val ok = text != null && import(c, text)
        withContext(Dispatchers.IO) {
            // a zip's photos take the place of the old ones; a bare JSON brings none, and those here stay for it
            if (ok && fresh.exists()) { val dir = File(c.app.filesDir, PHOTOS); dir.deleteRecursively(); fresh.renameTo(dir) }
            fresh.deleteRecursively()
        }
        return ok
    }

    /** The JSON out of [input]; a zip's photos are put into [photos] on the way. */
    private fun unpack(input: InputStream, photos: File): String? {
        photos.deleteRecursively()
        val src = BufferedInputStream(input)
        src.mark(4); val head = ByteArray(2); val n = src.read(head); src.reset()
        if (n < 2 || head[0] != 'P'.code.toByte() || head[1] != 'K'.code.toByte()) return src.readBytes().decodeToString()
        var text: String? = null
        ZipInputStream(src).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                val name = e.name.substringAfterLast('/')   // only the file's own name: nothing is written outside the photos
                when {
                    e.isDirectory || name.isBlank() -> {}
                    e.name == JSON -> text = z.readBytes().decodeToString()
                    e.name.startsWith("$PHOTOS/") -> { photos.mkdirs(); File(photos, name).outputStream().use { z.copyTo(it) } }
                }
            }
        }
        return text
    }

    /** Hands the tower to another app – a messenger, the mail, a cloud drive – so it can be opened on the new phone. */
    suspend fun send(c: AppContainer, ctx: Context) {
        val f = withContext(Dispatchers.IO) { val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
            dir.listFiles()?.filter { it.name.startsWith("inkling-") && it.name.endsWith(".zip") }?.forEach { it.delete() }
            File(dir, fileName()) }
        write(c, withContext(Dispatchers.IO) { f.outputStream() })
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val i = Intent(Intent.ACTION_SEND).setType("application/zip").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { ctx.startActivity(Intent.createChooser(i, tr("Отправить копию башни", "Send the tower's backup")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /** A photo's address on this phone: the same file, whatever the install it was taken in was called. */
    private fun photoHere(c: AppContainer, uri: String) =
        if (uri.startsWith("content://") && "/$PHOTOS/" in uri) "content://${c.app.packageName}.files/$PHOTOS/${uri.substringAfterLast('/')}" else uri

    private fun arr(l: List<JSONObject>) = JSONArray().apply { l.forEach { put(it) } }
    private fun JSONObject.objs(k: String): List<JSONObject> = optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } } ?: emptyList()
    private fun o(vararg kv: Pair<String, Any?>) = JSONObject().apply { kv.forEach { (k, v) -> put(k, v) } }

    suspend fun export(c: AppContainer): String {
        val d = c.dao
        return JSONObject().apply {
            put("app", "inkling"); put("version", 1); put("at", System.currentTimeMillis())
            put("settings", c.settings.now().json())
            put("subjects", arr(d.subjectList().map { o("id" to it.id, "name" to it.name, "school" to it.school, "teacher" to it.teacher, "contact" to it.contact, "rules" to it.rules,
                "credits" to it.credits, "archived" to it.archived, "order" to it.order) }))
            put("slots", arr(d.slotList().map { o("id" to it.id, "subjectId" to it.subjectId, "weekday" to it.weekday, "start" to it.start, "end" to it.end, "kind" to it.kind, "room" to it.room, "parity" to it.parity) }))
            put("attends", arr(d.attendList().map { o("id" to it.id, "slotId" to it.slotId, "subjectId" to it.subjectId, "day" to it.day, "state" to it.state, "at" to it.at) }))
            put("studies", arr(d.studyList().map { o("id" to it.id, "subjectId" to it.subjectId, "day" to it.day, "start" to it.start, "minutes" to it.minutes, "kind" to it.kind,
                "materialId" to it.materialId, "taskId" to it.taskId, "examId" to it.examId, "focus" to it.focus, "away" to it.away, "units" to it.units, "rested" to it.rested, "note" to it.note) }))
            put("materials", arr(d.materialList().map { o("id" to it.id, "subjectId" to it.subjectId, "title" to it.title, "unit" to it.unit, "total" to it.total, "done" to it.done,
                "created" to it.created, "finished" to it.finished, "archived" to it.archived) }))
            put("tasks", arr(d.taskList().map { o("id" to it.id, "subjectId" to it.subjectId, "title" to it.title, "kind" to it.kind, "due" to it.due, "state" to it.state, "created" to it.created,
                "doneDay" to it.doneDay, "note" to it.note, "photo" to it.photo, "stages" to it.stages, "stage" to it.stage) }))
            put("exams", arr(d.examList().map { o("id" to it.id, "subjectId" to it.subjectId, "title" to it.title, "kind" to it.kind, "day" to it.day, "done" to it.done, "score" to it.score,
                "maxScore" to it.maxScore, "goal" to it.goal, "parts" to it.parts, "note" to it.note) }))
            put("tickets", arr(d.ticketList().map { o("id" to it.id, "examId" to it.examId, "n" to it.n, "text" to it.text, "level" to it.level, "asked" to it.asked) }))
            put("spells", arr(d.spellList().map { o("id" to it.id, "subjectId" to it.subjectId, "title" to it.title, "text" to it.text, "created" to it.created, "due" to it.due, "step" to it.step,
                "last" to it.last, "asked" to it.asked, "good" to it.good) }))
            put("questions", arr(d.questionList().map { o("id" to it.id, "subjectId" to it.subjectId, "text" to it.text, "created" to it.created, "answered" to it.answered, "answer" to it.answer) }))
            put("grades", arr(d.gradeList().map { o("id" to it.id, "subjectId" to it.subjectId, "value" to it.value, "weight" to it.weight, "day" to it.day, "label" to it.label, "examId" to it.examId) }))
            put("pets", arr(d.petList().map { o("id" to it.id, "species" to it.species, "name" to it.name, "xp" to it.xp, "growth" to it.growth, "found" to it.found, "hatched" to it.hatched,
                "slot" to it.slot, "shiny" to it.shiny, "belly" to it.belly, "wis" to it.wis, "foc" to it.foc, "wit" to it.wit, "elo" to it.elo, "sta" to it.sta, "cur" to it.cur, "schools" to it.schools) }))
        }.toString(1)
    }

    /** Reads a backup; false when the text isn't one or is broken – then nothing changes. */
    suspend fun import(c: AppContainer, text: String): Boolean {
        val j = runCatching { JSONObject(text) }.getOrNull() ?: return false
        if (j.optString("app") != "inkling") return false
        val st = runCatching { j.optJSONObject("settings")?.let { Settings.of(it) } }.getOrElse { return false }
        val d = c.dao
        // a broken record throws, and the transaction puts everything back
        runCatching { c.db.withTransaction {
            d.wipeSubjects(); d.wipeSlots(); d.wipeAttends(); d.wipeStudies(); d.wipeMaterials(); d.wipeTasks(); d.wipeExams(); d.wipeTickets(); d.wipeSpells(); d.wipeQuestions(); d.wipeGrades(); d.wipePets()
            j.objs("subjects").forEach { d.insert(Subject(it.getLong("id"), it.getString("name"), it.optString("school", "own"), it.optString("teacher"), it.optString("contact"), it.optString("rules"),
                it.optDouble("credits", 0.0), it.optBoolean("archived"), it.optInt("order"))) }
            j.objs("slots").forEach { d.insert(Slot(it.getLong("id"), it.getLong("subjectId"), it.getInt("weekday"), it.getInt("start"), it.getInt("end"), it.optInt("kind"), it.optString("room"), it.optInt("parity"))) }
            j.objs("attends").forEach { d.insert(Attend(it.getLong("id"), it.getLong("slotId"), it.getLong("subjectId"), it.getLong("day"), it.getInt("state"), it.optLong("at"))) }
            j.objs("studies").forEach { d.insert(Study(it.getLong("id"), it.getLong("subjectId"), it.getLong("day"), it.optLong("start"), it.getInt("minutes"), it.optInt("kind", Study.MANUAL),
                it.optLong("materialId"), it.optLong("taskId"), it.optLong("examId"), it.optInt("focus"), it.optInt("away"), it.optInt("units"), it.optBoolean("rested"), it.optString("note"))) }
            j.objs("materials").forEach { d.insert(Material(it.getLong("id"), it.getLong("subjectId"), it.getString("title"), it.optString("unit", Material.LESSONS), it.optInt("total"), it.optInt("done"),
                it.optLong("created"), it.optLong("finished"), it.optBoolean("archived"))) }
            j.objs("tasks").forEach { d.insert(Task(it.getLong("id"), it.getLong("subjectId"), it.optString("title"), it.optString("kind", Task.HW), it.optLong("due"), it.optInt("state"), it.optLong("created"),
                it.optLong("doneDay"), it.optString("note"), photoHere(c, it.optString("photo")), it.optString("stages"), it.optInt("stage"))) }
            j.objs("exams").forEach { d.insert(Exam(it.getLong("id"), it.getLong("subjectId"), it.optString("title"), it.optString("kind", Exam.EXAM), it.getLong("day"), it.optBoolean("done"),
                it.optDouble("score", -1.0), it.optDouble("maxScore", 0.0), it.optDouble("goal", 0.0), it.optInt("parts"), it.optString("note"))) }
            j.objs("tickets").forEach { d.insert(Ticket(it.getLong("id"), it.getLong("examId"), it.getInt("n"), it.optString("text"), it.optInt("level"), it.optLong("asked"))) }
            j.objs("spells").forEach { d.insert(Spell(it.getLong("id"), it.getLong("subjectId"), it.getString("title"), it.optString("text"), it.getLong("created"), it.getLong("due"), it.optInt("step"),
                it.optInt("last"), it.optInt("asked"), it.optInt("good"))) }
            j.objs("questions").forEach { d.insert(Question(it.getLong("id"), it.getLong("subjectId"), it.getString("text"), it.getLong("created"), it.optLong("answered"), it.optString("answer"))) }
            j.objs("grades").forEach { d.insert(Grade(it.getLong("id"), it.getLong("subjectId"), it.getDouble("value"), it.optDouble("weight", 1.0), it.getLong("day"), it.optString("label"), it.optLong("examId"))) }
            j.objs("pets").forEach { d.insert(Pet(it.getLong("id"), it.getString("species"), it.getString("name"), it.optInt("xp"), it.optInt("growth"), it.optLong("found"), it.optLong("hatched"),
                it.optInt("slot", -1), it.optBoolean("shiny"), it.optInt("belly"), it.optInt("wis"), it.optInt("foc"), it.optInt("wit"), it.optInt("elo"), it.optInt("sta"), it.optInt("cur"), it.optString("schools"))) }
        } }.onFailure { return false }
        // without settings in the file the ones here stay, but the tower is open either way
        c.settings.update { (st ?: it).copy(onboarded = true, candle = Candle()) }
        return true
    }
}
