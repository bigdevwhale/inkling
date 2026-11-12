package app.inkling.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.inkling.container
import app.inkling.domain.Engine
import app.inkling.domain.Lang
import app.inkling.domain.Timetable
import app.inkling.domain.nowMin
import app.inkling.domain.plural
import app.inkling.domain.today
import app.inkling.domain.tr
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Every quarter of an hour: after a class ends, «were you there? what was set?»; in the evening hour one gentle
 * reminder – the satchel, the sphinx or tomorrow's deadline. Never during a class, never on holidays or in the infirmary.
 */
class Evening(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val s = c.settings.now()
        if (!s.onboarded) return Result.success()
        Lang.ru = applicationContext.resources.configuration.locales[0].language == "ru"
        val t = today(); val min = nowMin()
        if (t in s.sick) return Result.success()
        val slots = c.dao.slotList()
        if (Timetable.nowNext(s, slots, t, min).first != null) return Result.success()   // a class is on: quiet
        val prefs = applicationContext.getSharedPreferences("told", Context.MODE_PRIVATE)
        val told = prefs.getStringSet("told", emptySet())!!.filter { it.substringAfter('@').toLongOrNull() == t }.toMutableSet()
        fun tell(key: String, block: () -> Unit) { val k = "$key@$t"; if (k in told) return; block(); told += k; prefs.edit().putStringSet("told", told).apply() }

        if (s.afterClass) {
            val subjects = c.dao.subjectList().associateBy { it.id }
            Timetable.unmarked(s, slots, c.dao.attendList(), t, min).filter { min - it.end in 0..45 }.lastOrNull()?.let { sl ->
                tell("slot${sl.id}") { Notes.nudge(applicationContext, Notes.CLASS, tr("${subjects[sl.subjectId]?.name ?: ""}: были?", "${subjects[sl.subjectId]?.name ?: ""}: were you there?"),
                    tr("Отметьте и запишите, что задали, пока свежо.", "Mark it and note what was set while it's fresh."), "home") }
            }
        }
        if (s.evening >= 0 && LocalDateTime.now().hour == s.evening && !Timetable.holiday(s, t + 1)) {
            val tomorrow = Timetable.on(s, slots, t + 1)
            val due = c.dao.spellList().count { it.due <= t && !Engine.learned(it) }
            val deadline = c.dao.taskList().filter { it.open && it.due == t + 1 }
            tell("evening") {
                when {
                    s.school && s.satchel && tomorrow.isNotEmpty() && (t + 1) !in s.packed -> Notes.nudge(applicationContext, Notes.EVENING,
                        tr("Домовой ждёт у портфеля", "The hob is waiting by the satchel"), tr("Завтра ${tomorrow.size} ${plural(tomorrow.size.toLong(), "урок", "урока", "уроков")}. Соберём?", "${tomorrow.size} lessons tomorrow. Shall we pack?"), "home")
                    deadline.isNotEmpty() -> Notes.nudge(applicationContext, Notes.EVENING, tr("Сова принесла письмо", "The owl brought a letter"),
                        tr("Завтра срок: ${deadline.joinToString(", ") { it.title }}", "Due tomorrow: ${deadline.joinToString(", ") { it.title }}"), "home")
                    due > 0 -> Notes.nudge(applicationContext, Notes.EVENING, tr("Сфинкс приготовил загадки", "The sphinx has riddles ready"),
                        tr("$due ${plural(due.toLong(), "заклинание ждёт", "заклинания ждут", "заклинаний ждут")} – пара минут на вспомнить.", "$due spells waiting – a couple of minutes to recall."), "quiz")
                }
            }
        }
        return Result.success()
    }

    companion object {
        fun schedule(ctx: Context) {
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("bells", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<Evening>(15, TimeUnit.MINUTES).build())
        }
    }
}
