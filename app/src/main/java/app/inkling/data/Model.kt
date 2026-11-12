package app.inkling.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * A subject of any kind – «Матанализ», «Английский», «Сопромат». [school] is the school of magic it belongs to
 * (guessed from the name, changeable). [credits] counts for GPA at university; [rules] keeps what the teacher wants.
 */
@Entity
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val school: String,
    val teacher: String = "",
    val contact: String = "",
    val rules: String = "",
    val credits: Double = 0.0,
    val archived: Boolean = false,
    val order: Int = 0,
)

/**
 * A class in the week's timetable. [weekday] 1 Monday … 7 Sunday, [start]/[end] minutes of the day.
 * [parity] 0 every week, 1 odd («числитель»), 2 even («знаменатель»).
 */
@Entity
data class Slot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val weekday: Int,
    val start: Int,
    val end: Int,
    val kind: Int = LESSON,
    val room: String = "",
    val parity: Int = 0,
) {
    companion object { const val LESSON = 0; const val LECTURE = 1; const val SEMINAR = 2; const val PRACTICE = 3; const val LAB = 4; const val CLUB = 5; const val TUTOR = 6 }
}

/** Whether a class was attended: [state] 1 was there, 2 missed, 3 cancelled. */
@Entity(indices = [Index("day")])
data class Attend(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val slotId: Long,
    val subjectId: Long,
    val day: Long,
    val state: Int,
    val at: Long = System.currentTimeMillis(),
) { companion object { const val WAS = 1; const val MISSED = 2; const val CANCELLED = 3 } }

/**
 * A stretch of study. [kind] 0 a candle (the timer), 1 written down afterwards, 2 «teach the familiar»,
 * 3 exam tickets. [focus] minutes the phone lay face down, [away] times the student left or picked it up.
 * [units] pages/lessons/tasks done of [materialId].
 */
@Entity(indices = [Index("day"), Index("subjectId")])
data class Study(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val day: Long,
    val start: Long,
    val minutes: Int,
    val kind: Int = MANUAL,
    val materialId: Long = 0,
    val taskId: Long = 0,
    val examId: Long = 0,
    val focus: Int = 0,
    val away: Int = 0,
    val units: Int = 0,
    val rested: Boolean = false,
    val note: String = "",
) { companion object { const val CANDLE = 0; const val MANUAL = 1; const val TEACH = 2; const val TICKETS = 3 } }

/** A course, book, problem book or video course, counted in its own [unit]: lessons, pages, tasks, videos, chapters, minutes. */
@Entity
data class Material(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val unit: String = LESSONS,
    val total: Int = 0,
    val done: Int = 0,
    val created: Long,
    val finished: Long = 0,
    val archived: Boolean = false,
) { companion object { const val LESSONS = "lessons"; const val PAGES = "pages"; const val TASKS = "tasks"; const val VIDEOS = "videos"; const val CHAPTERS = "chapters"; const val MINUTES = "minutes" } }

/**
 * Homework, a lab, an essay, a project – or a big work (coursework, thesis) split into [stages] (one per line).
 * [state]: 0 open, 1 done, 2 handed in, 3 defended. [photo] is a picture of the board or the task.
 */
@Entity(indices = [Index("due")])
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val kind: String = HW,
    val due: Long = 0,
    val state: Int = 0,
    val created: Long,
    val doneDay: Long = 0,
    val note: String = "",
    val photo: String = "",
    val stages: String = "",
    val stage: Int = 0,
) {
    val stageList get() = stages.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val big get() = kind == COURSEWORK || kind == THESIS
    /** The last state this kind of task goes through. */
    val last get() = when (kind) { LAB -> 3; ESSAY, PROJECT -> 2; COURSEWORK, THESIS -> 3; else -> 1 }
    /** Still has a step to go (a lab done but not defended is still open). */
    val open get() = state < last
    companion object {
        const val HW = "hw"; const val LAB = "lab"; const val ESSAY = "essay"; const val TEST = "test"; const val PROJECT = "project"
        const val COURSEWORK = "coursework"; const val THESIS = "thesis"; const val READING = "reading"; const val OTHER = "other"
    }
}

/**
 * A trial: an exam, a credit, a test, ОГЭ/ЕГЭ, an olympiad. [parts] numbered tasks for state exams, [score] the result
 * (grade in the user's scale, or points), [goal] the points wanted. Tickets hang off it.
 */
@Entity
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val kind: String = EXAM,
    val day: Long,
    val done: Boolean = false,
    val score: Double = -1.0,
    val maxScore: Double = 0.0,
    val goal: Double = 0.0,
    val parts: Int = 0,
    val note: String = "",
) {
    companion object {
        const val EXAM = "exam"; const val CREDIT = "credit"; const val DIFF = "diff"; const val TEST = "test"; const val VPR = "vpr"
        const val OGE = "oge"; const val EGE = "ege"; const val OLYMP = "olymp"; const val CERT = "cert"; const val OTHER = "other"
    }
}

/** An exam ticket or a numbered task of a state exam. [level] 0 don't know … 3 sure. */
@Entity(indices = [Index("examId")])
data class Ticket(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val n: Int,
    val text: String = "",
    val level: Int = 0,
    val asked: Long = 0,
)

/**
 * A spell in the grimoire: something learned, recalled after 1, 3, 7, 21 and 60 days. [step] is how far it got,
 * [due] the day the sphinx asks next, [last] the last answer (1 remembered, 2 vaguely, 3 forgot), [asked] times asked.
 */
@Entity(indices = [Index("due")])
data class Spell(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val text: String = "",
    val created: Long,
    val due: Long,
    val step: Int = 0,
    val last: Int = 0,
    val asked: Int = 0,
    val good: Int = 0,
)

/** A question thrown into the mimic's chest; [answered] the day it found an answer. */
@Entity
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val text: String,
    val created: Long,
    val answered: Long = 0,
    val answer: String = "",
)

/**
 * A mark: a grade in the user's scale, points of a points system, or (with [examId]) the score of a practice paper.
 * [weight] as electronic school diaries weigh them.
 */
@Entity(indices = [Index("subjectId")])
data class Grade(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val value: Double,
    val weight: Double = 1.0,
    val day: Long,
    val label: String = "",
    val examId: Long = 0,
)

/**
 * A magical creature. [slot] 0 leads, 1–2 sit at the desks, -1 rests in the collection. [growth] counts towards
 * hatching while it's an egg ([hatched] 0), then [xp] grows it. Study fills [belly]; it turns into xp at night.
 * Six stats: wisdom, focus, wit, eloquence, stamina, curiosity. [schools] «num:120,rune:40» – what it was raised on.
 */
@Entity
data class Pet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val species: String,
    val name: String,
    val xp: Int = 0,
    val growth: Int = 0,
    val found: Long,
    val hatched: Long = 0,
    val slot: Int = -1,
    val shiny: Boolean = false,
    val belly: Int = 0,
    val wis: Int = 0, val foc: Int = 0, val wit: Int = 0, val elo: Int = 0, val sta: Int = 0, val cur: Int = 0,
    val schools: String = "",
) {
    fun schoolMap(): Map<String, Int> = schools.split(',').mapNotNull { p -> p.split(':').takeIf { it.size == 2 }?.let { it[0] to (it[1].toIntOrNull() ?: 0) } }.toMap()
    fun withSchool(school: String, n: Int): Pet { val m = schoolMap().toMutableMap(); m[school] = (m[school] ?: 0) + n; return copy(schools = m.entries.joinToString(",") { "${it.key}:${it.value}" }) }
    val stats get() = listOf(wis, foc, wit, elo, sta, cur)
}

@Dao
interface Dao0 {
    @Query("SELECT * FROM Subject ORDER BY `order`, id") fun subjects(): Flow<List<Subject>>
    @Query("SELECT * FROM Subject ORDER BY `order`, id") suspend fun subjectList(): List<Subject>
    @Insert suspend fun insert(s: Subject): Long
    @Update suspend fun update(s: Subject)
    @Query("DELETE FROM Subject WHERE id = :id") suspend fun deleteSubject(id: Long)

    @Query("SELECT * FROM Slot ORDER BY weekday, start") fun slots(): Flow<List<Slot>>
    @Query("SELECT * FROM Slot") suspend fun slotList(): List<Slot>
    @Insert suspend fun insert(s: Slot): Long
    @Update suspend fun update(s: Slot)
    @Query("DELETE FROM Slot WHERE id = :id") suspend fun deleteSlot(id: Long)
    @Query("DELETE FROM Slot WHERE subjectId = :id") suspend fun deleteSlotsOf(id: Long)

    @Query("SELECT * FROM Attend ORDER BY day DESC") fun attends(): Flow<List<Attend>>
    @Query("SELECT * FROM Attend") suspend fun attendList(): List<Attend>
    @Insert suspend fun insert(a: Attend): Long
    @Update suspend fun update(a: Attend)
    @Query("DELETE FROM Attend WHERE id = :id") suspend fun deleteAttend(id: Long)

    @Query("SELECT * FROM Study ORDER BY start DESC") fun studies(): Flow<List<Study>>
    @Query("SELECT * FROM Study") suspend fun studyList(): List<Study>
    @Insert suspend fun insert(s: Study): Long
    @Update suspend fun update(s: Study)
    @Query("DELETE FROM Study WHERE id = :id") suspend fun deleteStudy(id: Long)

    @Query("SELECT * FROM Material ORDER BY id") fun materials(): Flow<List<Material>>
    @Query("SELECT * FROM Material") suspend fun materialList(): List<Material>
    @Insert suspend fun insert(m: Material): Long
    @Update suspend fun update(m: Material)
    @Query("DELETE FROM Material WHERE id = :id") suspend fun deleteMaterial(id: Long)

    @Query("SELECT * FROM Task ORDER BY due, id") fun tasks(): Flow<List<Task>>
    @Query("SELECT * FROM Task") suspend fun taskList(): List<Task>
    @Insert suspend fun insert(t: Task): Long
    @Update suspend fun update(t: Task)
    @Query("DELETE FROM Task WHERE id = :id") suspend fun deleteTask(id: Long)

    @Query("SELECT * FROM Exam ORDER BY day") fun exams(): Flow<List<Exam>>
    @Query("SELECT * FROM Exam") suspend fun examList(): List<Exam>
    @Insert suspend fun insert(e: Exam): Long
    @Update suspend fun update(e: Exam)
    @Query("DELETE FROM Exam WHERE id = :id") suspend fun deleteExam(id: Long)

    @Query("SELECT * FROM Ticket ORDER BY examId, n") fun tickets(): Flow<List<Ticket>>
    @Query("SELECT * FROM Ticket") suspend fun ticketList(): List<Ticket>
    @Insert suspend fun insert(t: Ticket): Long
    @Insert suspend fun insertTickets(t: List<Ticket>)
    @Update suspend fun update(t: Ticket)
    @Query("DELETE FROM Ticket WHERE id = :id") suspend fun deleteTicket(id: Long)
    @Query("DELETE FROM Ticket WHERE examId = :id") suspend fun deleteTicketsOf(id: Long)

    @Query("SELECT * FROM Spell ORDER BY due, id") fun spells(): Flow<List<Spell>>
    @Query("SELECT * FROM Spell") suspend fun spellList(): List<Spell>
    @Insert suspend fun insert(s: Spell): Long
    @Update suspend fun update(s: Spell)
    @Query("DELETE FROM Spell WHERE id = :id") suspend fun deleteSpell(id: Long)

    @Query("SELECT * FROM Question ORDER BY created DESC, id DESC") fun questions(): Flow<List<Question>>
    @Query("SELECT * FROM Question") suspend fun questionList(): List<Question>
    @Insert suspend fun insert(q: Question): Long
    @Update suspend fun update(q: Question)
    @Query("DELETE FROM Question WHERE id = :id") suspend fun deleteQuestion(id: Long)

    @Query("SELECT * FROM Grade ORDER BY day DESC, id DESC") fun grades(): Flow<List<Grade>>
    @Query("SELECT * FROM Grade") suspend fun gradeList(): List<Grade>
    @Insert suspend fun insert(g: Grade): Long
    @Update suspend fun update(g: Grade)
    @Query("DELETE FROM Grade WHERE id = :id") suspend fun deleteGrade(id: Long)

    @Query("SELECT * FROM Pet ORDER BY id") fun pets(): Flow<List<Pet>>
    @Query("SELECT * FROM Pet ORDER BY id") suspend fun petList(): List<Pet>
    @Insert suspend fun insert(p: Pet): Long
    @Update suspend fun update(p: Pet)
    @Update suspend fun updatePets(p: List<Pet>)

    @Query("DELETE FROM Subject") suspend fun wipeSubjects()
    @Query("DELETE FROM Slot") suspend fun wipeSlots()
    @Query("DELETE FROM Attend") suspend fun wipeAttends()
    @Query("DELETE FROM Study") suspend fun wipeStudies()
    @Query("DELETE FROM Material") suspend fun wipeMaterials()
    @Query("DELETE FROM Task") suspend fun wipeTasks()
    @Query("DELETE FROM Exam") suspend fun wipeExams()
    @Query("DELETE FROM Ticket") suspend fun wipeTickets()
    @Query("DELETE FROM Spell") suspend fun wipeSpells()
    @Query("DELETE FROM Question") suspend fun wipeQuestions()
    @Query("DELETE FROM Grade") suspend fun wipeGrades()
    @Query("DELETE FROM Pet") suspend fun wipePets()
}

@Database(entities = [Subject::class, Slot::class, Attend::class, Study::class, Material::class, Task::class, Exam::class, Ticket::class,
    Spell::class, Question::class, Grade::class, Pet::class], version = 1)
abstract class Db : RoomDatabase() {
    abstract fun dao(): Dao0

    companion object {
        fun create(ctx: Context): Db = Room.databaseBuilder(ctx, Db::class.java, "inkling.db").build()
    }
}
