package app.inkling

import android.app.Application
import android.content.Context
import app.inkling.data.Attend
import app.inkling.data.Candle
import app.inkling.data.Db
import app.inkling.data.Exam
import app.inkling.data.Grade
import app.inkling.data.Material
import app.inkling.data.Outside
import app.inkling.data.OutsideSource
import app.inkling.data.Pet
import app.inkling.data.Question
import app.inkling.data.Settings
import app.inkling.data.SettingsStore
import app.inkling.data.Slot
import app.inkling.data.Spell
import app.inkling.data.Study
import app.inkling.data.Subject
import app.inkling.data.Task
import app.inkling.data.Ticket
import app.inkling.domain.Engine
import app.inkling.domain.Feast
import app.inkling.domain.Lang
import app.inkling.domain.Perk
import app.inkling.domain.Pets
import app.inkling.domain.Quests
import app.inkling.domain.Schools
import app.inkling.domain.Shop
import app.inkling.domain.Stat
import app.inkling.domain.Timetable
import app.inkling.domain.WeekFacts
import app.inkling.domain.dayOf
import app.inkling.domain.hourOf
import app.inkling.domain.today
import app.inkling.domain.tr
import app.inkling.domain.weekStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import kotlin.random.Random

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = Db.create(app)
    val dao = db.dao()
    val settings = SettingsStore(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val news = MutableSharedFlow<String>(extraBufferCapacity = 8)
    private val lock = Mutex()
    /** The day the tower lives by; the demo moves it while seeding. */
    @Volatile var clock: () -> Long = { today() }
    /** The moment «now» (ms); the demo moves it too. */
    @Volatile var now: () -> Long = { System.currentTimeMillis() }

    fun write(block: suspend AppContainer.() -> Unit) { scope.launch { block() } }
    fun say(s: String) { news.tryEmit(s) }

    // ---------- the start ----------

    /** First launch: the subjects named, the inkling and the settings from the questions. */
    suspend fun begin(mode: String, year: Int, scale: String, student: String, subjects: List<String>) = lock.withLock {
        if (dao.subjectList().isEmpty()) subjects.map { it.trim() }.filter { it.isNotEmpty() }.distinct().forEachIndexed { i, n ->
            dao.insert(Subject(name = n.replaceFirstChar { it.uppercase() }, school = Schools.guess(n), order = i)) }
        if (dao.petList().none { it.species == "inkling" }) dao.insert(newPet("inkling", 0).copy(hatched = clock(), growth = Pets.HATCH))
        val t = clock()
        settings.update { it.copy(onboarded = true, mode = mode, year = year, scale = scale, student = student,
            bells = it.bells.ifEmpty { if (mode == Settings.SELF) emptyList() else Timetable.defaultBells(mode) },
            terms = it.terms.ifEmpty { if (mode == Settings.SELF) emptyList() else Timetable.defaultTerms(mode, t) },
            satchel = mode == Settings.SCHOOL, digested = t,
            questWeek = weekStart(t), quests = Quests.deal(weekStart(t), it.copy(mode = mode), false)) }
    }

    fun newPet(species: String, slot: Int): Pet {
        val sp = Pets.of(species); val names = if (Lang.ru) sp.names else sp.namesEn
        return Pet(species = species, name = names.random(), found = clock(), slot = slot, shiny = Random.nextInt(20) == 0)
    }

    // ---------- mana ----------

    /** Adds mana; [study] mana is capped per day. Festivals double it. Returns what was given. */
    private suspend fun mana(n: Int, study: Boolean = false): Int {
        if (n <= 0) return 0
        val t = clock(); var given = n
        if (Feast.on(t) == Feast.CANDLES || Feast.on(t) == Feast.BALL) given *= 2
        settings.update { s ->
            val todayGot = if (s.manaDay == t) s.manaToday else 0
            if (study) given = given.coerceAtMost((Engine.DAILY_CAP - todayGot).coerceAtLeast(0))
            s.copy(mana = s.mana + given, manaDay = t, manaToday = todayGot + if (study) given else 0)
        }
        return given
    }

    private suspend fun news(items: List<String>) { if (items.isNotEmpty()) settings.update { it.copy(inbox = it.inbox + items) } }

    /** Lays eggs (into free desks first) and writes the news. */
    private suspend fun lay(species: List<String>, why: String = "") {
        if (species.isEmpty()) return
        val free = (0..2).filter { sl -> dao.petList().none { it.slot == sl } }.toMutableList()
        val laid = ArrayList<String>()
        for (sp in species.distinct()) { if (dao.petList().any { it.species == sp }) continue
            dao.insert(newPet(sp, free.removeFirstOrNull() ?: -1)); laid += sp }
        news(laid.map { "egg:$it$why" })
    }

    private suspend fun feedTeam(minutes: Int, subjectId: Long, stat: Stat?, warm: Int = 1, statBy: Int = 1) {
        val school = dao.subjectList().firstOrNull { it.id == subjectId }?.school.orEmpty()
        val fed = Engine.feed(dao.petList(), minutes, school, stat, clock(), statBy, warm)
        dao.updatePets(fed.pets)
        news(fed.hatched.map { "hatch:${it.id}" })
    }

    private suspend fun cheer(xp: Int, stat: Stat?, also: String = "") = dao.updatePets(Engine.cheer(dao.petList(), xp, stat, also))

    // ---------- subjects and the timetable ----------

    suspend fun saveSubject(s: Subject): Long = lock.withLock {
        if (s.id == 0L) dao.insert(s.copy(order = dao.subjectList().size)) else { dao.update(s); s.id }
    }
    suspend fun deleteSubject(id: Long) = lock.withLock { dao.deleteSlotsOf(id); dao.update(dao.subjectList().first { it.id == id }.copy(archived = true)) }
    suspend fun saveSlot(s: Slot) = lock.withLock { if (s.id == 0L) dao.insert(s) else dao.update(s); afterAct() }
    suspend fun deleteSlot(id: Long) = lock.withLock { dao.deleteSlot(id) }

    /** A class marked: was there, missed or cancelled. Being there feeds the team a little and gives mana. */
    suspend fun attend(slot: Slot, day: Long, state: Int) = lock.withLock {
        dao.attendList().firstOrNull { it.slotId == slot.id && it.day == day }?.let { dao.deleteAttend(it.id) }
        dao.insert(Attend(slotId = slot.id, subjectId = slot.subjectId, day = day, state = state))
        if (state == Attend.WAS) {
            mana(Engine.bonus(Engine.ATTEND, Perk.ATTEND, dao.petList()))
            feedTeam(((slot.end - slot.start) / 3).coerceIn(5, 40), slot.subjectId, Stat.STA)
        }
        afterAct()
    }

    // ---------- study ----------

    /** Lights a candle. */
    suspend fun light(subjectId: Long, materialId: Long, taskId: Long, examId: Long, plan: Int) = lock.withLock {
        settings.update { it.copy(candle = Candle(start = now(), subjectId = subjectId, materialId = materialId, taskId = taskId, examId = examId, plan = plan)) }
    }
    suspend fun candleDown(down: Boolean) = lock.withLock {
        settings.update { s -> val c = s.candle; if (c.start == 0L) s else when {
            down && c.downSince == 0L -> s.copy(candle = c.copy(downSince = now()))
            !down && c.downSince > 0L -> s.copy(candle = c.copy(downMs = c.downMs + (now() - c.downSince), downSince = 0, away = c.away + 1))
            else -> s } }
    }
    suspend fun candleAway() = lock.withLock { settings.update { s -> if (s.candle.start == 0L || s.candle.downSince > 0) s else s.copy(candle = s.candle.copy(away = s.candle.away + 1)) } }
    suspend fun snuff() = lock.withLock { settings.update { it.copy(candle = Candle()) } }

    /** The candle burnt down (or put out early): a study is written. Returns its id. */
    suspend fun burn(units: Int, note: String): Long {
        val s = settings.now(); val c = s.candle
        if (c.start == 0L) return 0
        val end = now()
        val down = c.downMs + if (c.downSince > 0) end - c.downSince else 0
        val min = ((end - c.start) / 60_000).toInt().coerceAtLeast(1)
        // the picking up that ends a face-down candle is not a distraction
        val away = (c.away - if (c.downSince == 0L && down > 0) 1 else 0).coerceAtLeast(0)
        settings.update { it.copy(candle = Candle(restUntil = end + it.rest * 60_000L)) }
        return addStudy(Study(subjectId = c.subjectId, day = dayOf(c.start).let { d -> if (clock() != today()) clock() else d }, start = c.start, minutes = min, kind = Study.CANDLE,
            materialId = c.materialId, taskId = c.taskId, examId = c.examId, focus = (down / 60_000).toInt(), away = away, units = units, note = note))
    }

    /** The break after a candle: [rested] when it was actually taken. */
    suspend fun endRest(rested: Boolean) = lock.withLock {
        settings.update { it.copy(candle = Candle(), rests = it.rests + if (rested) 1 else 0) }
        if (rested) { mana(Engine.bonus(Engine.REST, Perk.REST, dao.petList())); cheer(1, null, "cat") }
        afterAct()
    }

    /** Any study written: mana by minutes and perks, food for the team, material progress. */
    suspend fun addStudy(st: Study): Long = lock.withLock {
        val before = dao.studyList()
        val id = dao.insert(st)
        val pets = dao.petList()
        val school = dao.subjectList().firstOrNull { it.id == st.subjectId }?.school.orEmpty()
        val hour = hourOf(st.start)
        val perks = buildSet {
            if (st.kind == Study.CANDLE && st.away == 0 && st.minutes >= 20) add(Perk.FOCUS)
            if (st.minutes <= 15) add(Perk.SHORT)
            if (hour >= 18) add(Perk.EVENING)
            if (hour < 12) add(Perk.MORNING)
            if (st.units > 0) add(Perk.UNITS)
            if (st.kind == Study.TEACH) add(Perk.TEACH)
            if (st.kind == Study.TICKETS) add(Perk.RECALL)
            if (before.any { it.day == st.day && it.subjectId != st.subjectId }) add(Perk.MIX)
            val mat = dao.materialList().firstOrNull { it.id == st.materialId }
            if (mat?.unit == Material.PAGES) add(Perk.READ)
        }
        val team = Engine.team(pets).map { Pets.of(it.species).perk }.toSet()
        var m = (st.minutes / 5).coerceAtLeast(1)
        if (perks.any { it in team }) m *= 2
        if (st.kind == Study.TEACH) m += Engine.TEACH
        mana(Engine.bonus(m, null, pets), study = true)
        if (before.none { it.day == st.day }) mana(Engine.bonus(Engine.FIRST_OF_DAY, Perk.DAYS, pets))
        val last = before.filter { it.day < st.day }.maxOfOrNull { it.day }
        if (last != null && st.day - last >= 3) mana(Engine.bonus(5, Perk.COMEBACK, pets))
        val stat = Engine.statOf(st.kind, school, st.away, st.minutes)
        feedTeam(st.minutes, st.subjectId, stat, warm = if (st.minutes >= 25) 2 else 1, statBy = if (st.minutes >= 45) 2 else 1)
        if (st.materialId != 0L && st.units > 0) progressMaterial(st.materialId, st.units)
        afterAct()
        id
    }

    // an edited study (say, 45 minutes made 120) can finish a quest or bring a find too
    suspend fun updateStudy(st: Study) = lock.withLock { dao.update(st); afterAct() }
    suspend fun deleteStudy(id: Long) = lock.withLock { dao.deleteStudy(id); afterAct() }

    // ---------- materials ----------

    suspend fun saveMaterial(m: Material): Long = lock.withLock { if (m.id == 0L) dao.insert(m) else { dao.update(m); m.id } }
    suspend fun deleteMaterial(id: Long) = lock.withLock { dao.deleteMaterial(id) }
    suspend fun addUnits(id: Long, n: Int) = lock.withLock { progressMaterial(id, n); afterAct() }

    private suspend fun progressMaterial(id: Long, n: Int) {
        val m = dao.materialList().firstOrNull { it.id == id } ?: return
        val done = (m.done + n).coerceAtLeast(0).let { if (m.total > 0) it.coerceAtMost(m.total) else it }
        val fin = m.finished == 0L && m.total > 0 && done >= m.total
        dao.update(m.copy(done = done, finished = if (fin) clock() else m.finished))
        if (fin) { mana(Engine.bonus(Engine.FINISH, Perk.FINISH, dao.petList())); cheer(20, Stat.STA, "kitsune"); news(listOf("material:$id")) }
    }

    // ---------- the grimoire, the chest, teaching ----------

    suspend fun addSpell(subjectId: Long, title: String, text: String) = lock.withLock {
        if (title.isBlank()) return@withLock
        dao.insert(Spell(subjectId = subjectId, title = title.trim(), text = text.trim(), created = clock(), due = clock() + Engine.STEPS[0]))
        mana(Engine.bonus(Engine.SPELL, Perk.SPELL, dao.petList())); cheer(2, Stat.WIS, "inkling")
        afterAct()
    }
    suspend fun saveSpell(s: Spell) = lock.withLock { dao.update(s) }
    suspend fun deleteSpell(id: Long) = lock.withLock { dao.deleteSpell(id) }

    /** The sphinx asked: 1 remembered, 2 vaguely, 3 forgot. */
    suspend fun answerSpell(s: Spell, a: Int) = lock.withLock {
        val t = clock()
        dao.update(Engine.recall(s, a, t))
        val wk = weekStart(t)
        settings.update { it.copy(recallWeek = wk, recalled = (if (it.recallWeek == wk) it.recalled else 0) + 1) }
        if (a == 1) mana(Engine.bonus(Engine.RECALL, Perk.RECALL, dao.petList())) else mana(1)
        cheer(if (a == 1) 3 else 1, Stat.WIS, "sphinx")
        if (a == 1 && Engine.learned(Engine.recall(s, a, t))) news(listOf("learned:${s.id}"))
        afterAct()
    }

    suspend fun addQuestion(subjectId: Long, text: String) = lock.withLock {
        if (text.isBlank()) return@withLock
        dao.insert(Question(subjectId = subjectId, text = text.trim(), created = clock()))
        mana(Engine.bonus(Engine.QUESTION, Perk.QUESTION, dao.petList())); cheer(1, Stat.CUR, "mimic")
        afterAct()
    }
    suspend fun answerQuestion(q: Question, answer: String) = lock.withLock {
        val first = q.answered == 0L
        dao.update(q.copy(answered = if (first) clock() else q.answered, answer = answer.trim()))
        if (first) { mana(Engine.bonus(Engine.ANSWER, Perk.QUESTION, dao.petList())); cheer(3, Stat.CUR, "mimic") }
        afterAct()
    }
    suspend fun deleteQuestion(id: Long) = lock.withLock { dao.deleteQuestion(id) }

    // ---------- tasks ----------

    suspend fun saveTask(t: Task): Long = lock.withLock {
        val id = if (t.id == 0L) dao.insert(t.copy(created = if (t.created == 0L) clock() else t.created)).also { mana(Engine.bonus(1, Perk.PLAN, dao.petList())) } else { dao.update(t); t.id }
        afterAct(); id
    }
    suspend fun deleteTask(id: Long) = lock.withLock { dao.deleteTask(id) }

    /** Moves a task along: done, handed in, defended. Each step forward is worth mana; early ones more with the owl. */
    suspend fun setTaskState(t: Task, state: Int) = lock.withLock {
        val day = clock()
        val up = state > t.state
        dao.update(t.copy(state = state, doneDay = if (state >= 1 && t.doneDay == 0L) day else if (state == 0) 0 else t.doneDay))
        if (up) {
            val early = t.due > 0 && day < t.due
            mana(Engine.bonus(Engine.TASK, if (early) Perk.DEADLINE else Perk.TASK, dao.petList()))
            cheer(4, Stat.WIT, "mandrake")
            if (t.big && state >= 3) { mana(Engine.FINISH * 2); news(listOf("defended:${t.id}")) }
        }
        afterAct()
    }

    /** A stage of a big work ticked; the last one is the defence. */
    suspend fun setStage(t: Task, stage: Int) = lock.withLock {
        val n = t.stageList.size
        val done = n > 0 && stage >= n
        dao.update(t.copy(stage = stage, state = if (done) 3 else 0, doneDay = if (done) clock() else 0))
        if (stage > t.stage) { mana(Engine.bonus(Engine.TASK * 2, Perk.TASK, dao.petList())); cheer(8, Stat.STA, "dragon") }
        if (done && t.state < 3) { mana(Engine.FINISH * 2); news(listOf("defended:${t.id}")) }
        afterAct()
    }

    // ---------- trials, tickets, marks ----------

    suspend fun saveExam(e: Exam): Long = lock.withLock {
        val id = if (e.id == 0L) dao.insert(e) else { dao.update(e); e.id }
        // numbered tasks of a state exam become tickets by themselves
        if (e.parts > 0) { val have = dao.ticketList().filter { it.examId == id }.map { it.n }.toSet()
            dao.insertTickets((1..e.parts).filter { it !in have }.map { Ticket(examId = id, n = it) }) }
        id
    }
    suspend fun deleteExam(id: Long) = lock.withLock { dao.deleteTicketsOf(id); dao.deleteExam(id) }

    /** Tickets pasted as text, one per line («1. Пределы…» or just the text). Replaces the list. */
    suspend fun setTickets(examId: Long, text: String) = lock.withLock {
        val old = dao.ticketList().filter { it.examId == examId }.associateBy { it.n }
        dao.deleteTicketsOf(examId)
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        dao.insertTickets(lines.mapIndexed { i, l ->
            val m = Regex("^(\\d+)[.):\\s]+(.*)$").find(l)
            val n = m?.groupValues?.get(1)?.toIntOrNull() ?: (i + 1); val body = m?.groupValues?.get(2) ?: l
            Ticket(examId = examId, n = n, text = body, level = old[n]?.level ?: 0, asked = old[n]?.asked ?: 0) })
    }
    suspend fun setTicketLevel(t: Ticket, level: Int) = lock.withLock {
        dao.update(t.copy(level = level, asked = clock()))
        if (level > t.level) { mana(Engine.bonus(Engine.TICKET * (level - t.level), Perk.RECALL, dao.petList())); cheer(1, Stat.WIS, "sphinx") }
        afterAct()
    }
    suspend fun saveTicket(t: Ticket) = lock.withLock { if (t.id == 0L) dao.insert(t) else dao.update(t) }
    suspend fun deleteTicket(id: Long) = lock.withLock { dao.deleteTicket(id) }

    /** A trial passed: its result written. */
    suspend fun finishExam(e: Exam, score: Double) = lock.withLock {
        val first = !e.done
        dao.update(e.copy(done = true, score = score))
        if (first) { mana(Engine.TRIAL); cheer(30, Stat.STA); news(listOf("trial:${e.id}")) }
        afterAct()
    }

    suspend fun addGrade(g: Grade) = lock.withLock {
        if (g.id == 0L) { dao.insert(g); mana(Engine.bonus(Engine.GRADE, Perk.GRADE, dao.petList())); cheer(1, null, "magpie") } else dao.update(g)
        afterAct()
    }
    suspend fun deleteGrade(id: Long) = lock.withLock { dao.deleteGrade(id) }

    /** The satchel packed for [day]. */
    suspend fun pack(day: Long) = lock.withLock {
        val s = settings.now(); if (day in s.packed) return@withLock
        settings.update { it.copy(packed = it.packed + day) }
        mana(Engine.bonus(Engine.SATCHEL, Perk.PLAN, dao.petList())); cheer(1, null, "hob")
        afterAct()
    }

    suspend fun setSick(day: Long, on: Boolean) = lock.withLock { settings.update { it.copy(sick = if (on) it.sick + day else it.sick - day) } }

    // ---------- the tower ----------

    suspend fun buy(id: String): Boolean = lock.withLock {
        val it = Shop.byId[id] ?: return@withLock false; val s = settings.now()
        if (s.mana < it.price || id in s.items) return@withLock false
        settings.update { st -> st.copy(mana = st.mana - it.price, items = st.items + id) }
        true
    }

    fun eggPrice(day: Long) = if (Feast.on(day) == Feast.SOLSTICE) Shop.EGG / 2 else Shop.EGG

    suspend fun buyEgg(): Boolean = lock.withLock {
        val s = settings.now(); val price = eggPrice(clock()); if (s.mana < price) return@withLock false
        val sp = Engine.anyEgg(dao.petList().map { it.species }.toSet(), clock().toInt() + s.mana) ?: return@withLock false
        settings.update { it.copy(mana = it.mana - price) }
        lay(listOf(sp), ":fair"); true
    }

    suspend fun setSlot(p: Pet, slot: Int) = lock.withLock {
        val pets = dao.petList()
        if (slot >= 0) pets.firstOrNull { it.slot == slot && it.id != p.id }?.let { dao.update(it.copy(slot = p.slot)) }
        dao.update(dao.petList().first { it.id == p.id }.copy(slot = slot))
    }
    suspend fun rename(p: Pet, name: String) = lock.withLock { if (name.isNotBlank()) dao.update(dao.petList().first { it.id == p.id }.copy(name = name.trim())) }

    // ---------- the day ----------

    private suspend fun schoolOf(): (Long) -> String { val m = dao.subjectList().associate { it.id to it.school }; return { m[it].orEmpty() } }

    /** After any act: finds, then quests. */
    private suspend fun afterAct() {
        val s = settings.now()
        val studies = dao.studyList(); val spells = dao.spellList(); val questions = dao.questionList(); val tasks = dao.taskList(); val attends = dao.attendList()
        val owned = dao.petList().map { it.species }.toSet()
        val so = schoolOf()
        lay(Engine.finds(Engine.Life(s, studies, dao.subjectList(), spells, questions, tasks, attends, dao.materialList(), dao.examList(), dao.ticketList(), dao.gradeList(), s.rests), owned, so))
        val t = clock(); val monday = weekStart(t)
        val facts = WeekFacts(monday, studies, spells, questions, tasks, attends, s, so)
        facts.recalled = if (s.recallWeek == monday) s.recalled else 0
        val ids = if (s.questWeek == monday) s.quests else Quests.deal(monday, s, dao.slotList().isNotEmpty())
        val doneBefore = if (s.questWeek == monday) s.questsDone else emptySet()
        val newly = ids.mapNotNull { Quests.byId[it] }.filter { it.id !in doneBefore && it.progress(facts) >= it.need }
        if (s.questWeek != monday || newly.isNotEmpty()) {
            settings.update { it.copy(questWeek = monday, quests = ids, questsDone = doneBefore + newly.map { q -> q.id }, inbox = it.inbox + newly.map { q -> "quest:${q.id}" }) }
            val pets = dao.petList(); newly.forEach { mana(Engine.bonus(it.mana, Perk.QUEST, pets)) }
        }
    }

    /** Daily housekeeping: the night's growth, festivals, the end of a term, quests. */
    suspend fun tick() = lock.withLock {
        val s = settings.now(); val t = clock()
        if (s.digested < t) {
            val (pets, grown) = Engine.digest(dao.petList())
            dao.updatePets(pets)
            settings.update { it.copy(digested = t) }
            news(grown.map { "stage:${it.first.id}:${it.second.ordinal}" })
        }
        Feast.on(t)?.let { f -> val key = "${f.name}:${LocalDate.ofEpochDay(t).year}"
            if (key !in s.feasts) settings.update { it.copy(feasts = it.feasts + key, inbox = it.inbox + "feast:${f.name}") } }
        // a term that ended since: its report
        Timetable.terms(s).lastOrNull { it.last < t && it.last >= t - 20 }?.let { term ->
            val key = "term:${term.first}"
            if (key !in s.feasts && dao.studyList().any { it.day in term }) settings.update { it.copy(feasts = it.feasts + key, inbox = it.inbox + key) }
        }
        afterAct()
    }

    /** Fetches the weather for the window when it is older than half an hour; guesses the town from the time zone once. */
    suspend fun refreshOutside(force: Boolean = false) = kotlinx.coroutines.withContext(Dispatchers.IO) { runCatching {
        var s = settings.now()
        if (s.lat.isNaN() && !s.guessed) {
            val tw = OutsideSource.guess(Lang.ru)
            settings.update { it.copy(guessed = true, town = tw?.name ?: it.town, lat = tw?.lat ?: it.lat, lon = tw?.lon ?: it.lon) }
            s = settings.now()
        }
        if (s.lat.isNaN()) return@runCatching
        val old = Outside.of(s.outside)
        if (!force && old != null && System.currentTimeMillis() - old.at < 30 * 60_000L) return@runCatching
        val o = OutsideSource.fetch(s.lat, s.lon)
        settings.update { it.copy(outside = o.json()) }
    } }
}

class InklingApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Lang.ru = resources.configuration.locales[0].language == "ru"
        container = AppContainer(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as InklingApp).container
