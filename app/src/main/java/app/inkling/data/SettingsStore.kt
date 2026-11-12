package app.inkling.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** A candle burning right now: kept here so it survives the app being closed. */
data class Candle(
    val start: Long = 0,            // 0 = no candle
    val subjectId: Long = 0,
    val materialId: Long = 0,
    val taskId: Long = 0,
    val examId: Long = 0,
    val plan: Int = 25,             // minutes planned
    val away: Int = 0,              // times the student left or picked the phone up
    val downMs: Long = 0,           // time the phone lay face down
    val downSince: Long = 0,        // face down since (0 = not now)
    val restUntil: Long = 0,        // a break running until
) {
    fun json(): JSONObject = JSONObject().put("start", start).put("subjectId", subjectId).put("materialId", materialId).put("taskId", taskId).put("examId", examId)
        .put("plan", plan).put("away", away).put("downMs", downMs).put("downSince", downSince).put("restUntil", restUntil)
    companion object {
        fun of(j: JSONObject?): Candle = if (j == null) Candle() else Candle(j.optLong("start"), j.optLong("subjectId"), j.optLong("materialId"), j.optLong("taskId"), j.optLong("examId"),
            j.optInt("plan", 25), j.optInt("away"), j.optLong("downMs"), j.optLong("downSince"), j.optLong("restUntil"))
    }
}

/**
 * Everything that isn't a record or a creature: who studies where, the bells, terms, the scale of marks,
 * mana, the tower's things, quests and the candle burning now.
 */
data class Settings(
    val onboarded: Boolean = false,
    val mode: String = UNI,                      // school · college · uni · self
    val year: Int = 1,                           // form at school, year at college/university
    val student: String = "",                    // a name for the diploma cards
    val scale: String = "5",                     // 5 · 10 · 12 · 100 (points) · letter · pass
    val passes: List<Int> = listOf(61, 76, 91),  // points for 3/4/5 in a points system
    val bells: List<String> = emptyList(),       // «08:30-09:15»
    val parityStart: Long = 0,                   // a Monday of an odd («числитель») week; 0 = no odd/even weeks
    val terms: List<String> = emptyList(),       // «20330-20385» (epoch days) – quarters or semesters
    val mana: Int = 0,
    val items: Set<String> = emptySet(),
    val hidden: Set<String> = emptySet(),
    val theme: String = "system",
    val inbox: List<String> = emptyList(),       // news to show once: «egg:owl», «hatch:12», «stage:12:2», «quest:x», «term:20330»
    val seen: Set<String> = emptySet(),
    val questWeek: Long = 0,
    val quests: List<String> = emptyList(),
    val questsDone: Set<String> = emptySet(),
    val focus: Int = 25,                         // a candle's minutes
    val rest: Int = 5,                           // a break's minutes
    val afterClass: Boolean = true,              // ask «were you there? what was set?» after each class
    val evening: Int = 20,                       // hour of the evening reminder (satchel, spells), -1 off
    val satchel: Boolean = true,                 // the evening satchel for tomorrow (school)
    val packed: Set<Long> = emptySet(),          // days the satchel was packed for
    val sick: Set<Long> = emptySet(),            // days in the infirmary
    val digested: Long = 0,                      // the last day the creatures' bellies turned into growth
    val manaDay: Long = 0, val manaToday: Int = 0,
    val candle: Candle = Candle(),
    val town: String = "", val lat: Double = Double.NaN, val lon: Double = Double.NaN,
    val guessed: Boolean = false,
    val outside: String = "",
    val cardBg: Int = 0,
    val feasts: Set<String> = emptySet(),        // festivals and term ends already greeted, «CANDLES:2026», «term:20330»
    val recallWeek: Long = 0, val recalled: Int = 0,   // spells recalled this week, for the quests
    val rests: Int = 0,                          // breaks taken after a candle
) {
    val school get() = mode == SCHOOL
    val uni get() = mode == UNI || mode == COLLEGE
    val self get() = mode == SELF

    fun json(): JSONObject = JSONObject().apply {
        put("onboarded", onboarded); put("mode", mode); put("year", year); put("student", student); put("scale", scale); put("passes", JSONArray(passes))
        put("bells", JSONArray(bells)); put("parityStart", parityStart); put("terms", JSONArray(terms)); put("mana", mana)
        put("items", JSONArray(items.toList())); put("hidden", JSONArray(hidden.toList())); put("theme", theme); put("inbox", JSONArray(inbox))
        put("seen", JSONArray(seen.toList())); put("questWeek", questWeek); put("quests", JSONArray(quests)); put("questsDone", JSONArray(questsDone.toList()))
        put("focus", focus); put("rest", rest); put("afterClass", afterClass); put("evening", evening); put("satchel", satchel)
        put("packed", JSONArray(packed.toList())); put("sick", JSONArray(sick.toList())); put("digested", digested); put("manaDay", manaDay); put("manaToday", manaToday)
        put("candle", candle.json())
        put("town", town); if (!lat.isNaN()) { put("lat", lat); put("lon", lon) }; put("guessed", guessed); put("outside", outside)
        put("cardBg", cardBg); put("feasts", JSONArray(feasts.toList()))
        put("recallWeek", recallWeek); put("recalled", recalled); put("rests", rests)
    }

    companion object {
        const val SCHOOL = "school"; const val COLLEGE = "college"; const val UNI = "uni"; const val SELF = "self"
        fun of(j: JSONObject): Settings {
            fun list(k: String) = j.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
            fun set(k: String) = list(k).toSet()
            fun days(k: String) = list(k).mapNotNull { it.toLongOrNull() }.toSet()
            return Settings(
                onboarded = j.optBoolean("onboarded"), mode = j.optString("mode", UNI), year = j.optInt("year", 1), student = j.optString("student"),
                scale = j.optString("scale", "5"), passes = list("passes").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 3 } ?: listOf(61, 76, 91),
                bells = list("bells"), parityStart = j.optLong("parityStart"), terms = list("terms"), mana = j.optInt("mana"),
                items = set("items"), hidden = set("hidden"), theme = j.optString("theme", "system"), inbox = list("inbox"), seen = set("seen"),
                questWeek = j.optLong("questWeek"), quests = list("quests"), questsDone = set("questsDone"),
                focus = j.optInt("focus", 25), rest = j.optInt("rest", 5), afterClass = j.optBoolean("afterClass", true), evening = j.optInt("evening", 20),
                satchel = j.optBoolean("satchel", true), packed = days("packed"), sick = days("sick"), digested = j.optLong("digested"),
                manaDay = j.optLong("manaDay"), manaToday = j.optInt("manaToday"), candle = Candle.of(j.optJSONObject("candle")),
                town = j.optString("town"), lat = j.optDouble("lat", Double.NaN), lon = j.optDouble("lon", Double.NaN), guessed = j.optBoolean("guessed"),
                outside = j.optString("outside"), cardBg = j.optInt("cardBg"), feasts = set("feasts"),
                recallWeek = j.optLong("recallWeek"), recalled = j.optInt("recalled"), rests = j.optInt("rests"))
        }
    }
}

private val Context.store by preferencesDataStore("inkling")
private val KEY = stringPreferencesKey("settings")

class SettingsStore(private val ctx: Context) {
    val settings: Flow<Settings> = ctx.store.data.map { p -> p[KEY]?.let { runCatching { Settings.of(JSONObject(it)) }.getOrNull() } ?: Settings() }
    suspend fun now(): Settings = settings.first()
    suspend fun update(f: (Settings) -> Settings) { ctx.store.edit { p ->
        val cur = p[KEY]?.let { runCatching { Settings.of(JSONObject(it)) }.getOrNull() } ?: Settings()
        p[KEY] = f(cur).json().toString() } }
}
