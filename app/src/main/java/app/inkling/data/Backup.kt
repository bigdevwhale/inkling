package app.inkling.data

import androidx.room.withTransaction
import app.inkling.AppContainer
import org.json.JSONArray
import org.json.JSONObject

/** The whole tower as one JSON file: every table and the settings. Import replaces everything. */
object Backup {
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

    /** Reads a backup; false when the file isn't one. */
    suspend fun import(c: AppContainer, text: String): Boolean {
        val j = runCatching { JSONObject(text) }.getOrNull() ?: return false
        if (j.optString("app") != "inkling") return false
        val d = c.dao
        c.db.withTransaction {
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
                it.optLong("doneDay"), it.optString("note"), it.optString("photo"), it.optString("stages"), it.optInt("stage"))) }
            j.objs("exams").forEach { d.insert(Exam(it.getLong("id"), it.getLong("subjectId"), it.optString("title"), it.optString("kind", Exam.EXAM), it.getLong("day"), it.optBoolean("done"),
                it.optDouble("score", -1.0), it.optDouble("maxScore", 0.0), it.optDouble("goal", 0.0), it.optInt("parts"), it.optString("note"))) }
            j.objs("tickets").forEach { d.insert(Ticket(it.getLong("id"), it.getLong("examId"), it.getInt("n"), it.optString("text"), it.optInt("level"), it.optLong("asked"))) }
            j.objs("spells").forEach { d.insert(Spell(it.getLong("id"), it.getLong("subjectId"), it.getString("title"), it.optString("text"), it.getLong("created"), it.getLong("due"), it.optInt("step"),
                it.optInt("last"), it.optInt("asked"), it.optInt("good"))) }
            j.objs("questions").forEach { d.insert(Question(it.getLong("id"), it.getLong("subjectId"), it.getString("text"), it.getLong("created"), it.optLong("answered"), it.optString("answer"))) }
            j.objs("grades").forEach { d.insert(Grade(it.getLong("id"), it.getLong("subjectId"), it.getDouble("value"), it.optDouble("weight", 1.0), it.getLong("day"), it.optString("label"), it.optLong("examId"))) }
            j.objs("pets").forEach { d.insert(Pet(it.getLong("id"), it.getString("species"), it.getString("name"), it.optInt("xp"), it.optInt("growth"), it.optLong("found"), it.optLong("hatched"),
                it.optInt("slot", -1), it.optBoolean("shiny"), it.optInt("belly"), it.optInt("wis"), it.optInt("foc"), it.optInt("wit"), it.optInt("elo"), it.optInt("sta"), it.optInt("cur"), it.optString("schools"))) }
        }
        j.optJSONObject("settings")?.let { s -> val st = Settings.of(s); c.settings.update { st.copy(onboarded = true, candle = Candle()) } }
        return true
    }
}
