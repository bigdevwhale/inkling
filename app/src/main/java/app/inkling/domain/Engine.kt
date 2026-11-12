package app.inkling.domain

import app.inkling.data.Attend
import app.inkling.data.Exam
import app.inkling.data.Grade
import app.inkling.data.Material
import app.inkling.data.Pet
import app.inkling.data.Question
import app.inkling.data.Settings
import app.inkling.data.Spell
import app.inkling.data.Study
import app.inkling.data.Subject
import app.inkling.data.Task
import app.inkling.data.Ticket

/**
 * The rules of the tower. Mana and growth come from honest study – minutes, recalls, explanations, questions,
 * classes attended, tasks handed in – never from marks being good or bad.
 */
object Engine {
    const val DAILY_CAP = 60            // mana for study minutes a day
    const val FIRST_OF_DAY = 3
    const val SPELL = 2
    const val RECALL = 2
    const val TEACH = 8
    const val QUESTION = 1
    const val ANSWER = 3
    const val ATTEND = 2
    const val TASK = 4
    const val GRADE = 1
    const val SATCHEL = 3
    const val REST = 2
    const val FINISH = 25
    const val TRIAL = 30
    const val TICKET = 1

    /** Recall steps of the grimoire, in days. */
    val STEPS = listOf(1, 3, 7, 21, 60)

    fun team(pets: List<Pet>) = pets.filter { it.slot in 0..2 && it.hatched > 0 }.sortedBy { it.slot }

    /** Mana for an act, doubled when a creature with the matching perk is in the tower; the legendary ones add a quarter. */
    fun bonus(base: Int, perk: Perk?, pets: List<Pet>): Int {
        val t = team(pets)
        var v = base
        if (perk != null && t.any { Pets.of(it.species).perk == perk }) v *= if (perk == Perk.COMEBACK) 3 else 2
        if (t.any { Pets.of(it.species).perk == Perk.ALL }) v = (v * 1.25).toInt().coerceAtLeast(v + 1)
        return v
    }

    data class Fed(val pets: List<Pet>, val hatched: List<Pet>)

    /**
     * Study feeds the tower: the leader's belly gets the minutes, the two at the desks half; every egg gets warmer.
     * The bellies turn into growth at night ([digest]). [stat] rises for the whole team; [school] is remembered.
     */
    fun feed(pets: List<Pet>, minutes: Int, school: String, stat: Stat?, today: Long, statBy: Int = 1, warm: Int = 1): Fed {
        val hatched = ArrayList<Pet>()
        val out = pets.map { p ->
            if (p.hatched == 0L) {
                val g = p.growth + warm
                if (g >= Pets.HATCH) p.copy(growth = Pets.HATCH, hatched = today).also { hatched += it } else p.copy(growth = g)
            } else if (p.slot in 0..2) {
                val share = if (p.slot == 0) minutes else (minutes + 1) / 2
                var q = p.copy(belly = p.belly + share)
                if (share > 0 && school.isNotEmpty()) q = q.withSchool(school, share)
                if (stat != null) q = q.bump(stat, statBy)
                q
            } else p
        }
        return Fed(out, hatched)
    }

    fun Pet.bump(s: Stat, n: Int): Pet = when (s) {
        Stat.WIS -> copy(wis = wis + n); Stat.FOC -> copy(foc = foc + n); Stat.WIT -> copy(wit = wit + n)
        Stat.ELO -> copy(elo = elo + n); Stat.STA -> copy(sta = sta + n); Stat.CUR -> copy(cur = cur + n)
    }

    /** XP straight away for the team (an answer, a question, a class) and its stat. */
    fun cheer(pets: List<Pet>, xp: Int, stat: Stat? = null, also: String = ""): List<Pet> {
        val team = team(pets).map { it.id }.toSet()
        return pets.map { p -> if (p.hatched > 0 && (p.id in team || p.species == also)) (if (stat != null) p.bump(stat, 1) else p).copy(xp = p.xp + xp) else p }
    }

    /** Night: what was eaten becomes growth. Returns the pets and who moved to a new stage. */
    fun digest(pets: List<Pet>): Pair<List<Pet>, List<Pair<Pet, Stage>>> {
        val grown = ArrayList<Pair<Pet, Stage>>()
        val out = pets.map { p ->
            if (p.belly <= 0 || p.hatched == 0L) p else {
                val before = p.stage(); val q = p.copy(xp = p.xp + p.belly, belly = 0)
                if (q.stage() != before) grown += q to q.stage()
                q
            }
        }
        return out to grown
    }

    /** The next step of a spell after an answer: 1 remembered, 2 vaguely, 3 forgot. */
    fun recall(s: Spell, answer: Int, today: Long): Spell = when (answer) {
        1 -> { val step = (s.step + 1).coerceAtMost(STEPS.size); s.copy(step = step, due = if (step >= STEPS.size) today + 180 else today + STEPS[step], last = 1, asked = s.asked + 1, good = s.good + 1) }
        2 -> s.copy(due = today + (STEPS[s.step.coerceAtMost(STEPS.size - 1)] / 2).coerceAtLeast(1), last = 2, asked = s.asked + 1)
        else -> s.copy(step = 0, due = today + 1, last = 3, asked = s.asked + 1)
    }
    fun learned(s: Spell) = s.step >= STEPS.size

    /** What the finds are judged on. */
    class Life(val s: Settings, val studies: List<Study>, val subjects: List<Subject>, val spells: List<Spell>, val questions: List<Question>,
               val tasks: List<Task>, val attends: List<Attend>, val materials: List<Material>, val exams: List<Exam>, val tickets: List<Ticket>, val grades: List<Grade>, val rests: Int) {
        val days = studies.map { it.day }.toSet()
        val byDay = studies.groupBy { it.day }
        val hours = studies.sumOf { it.minutes } / 60.0
    }

    /** Creatures found by how one studies, not yet owned. */
    fun finds(l: Life, owned: Set<String>, schoolOf: (Long) -> String): List<String> {
        val out = ArrayList<String>()
        fun f(id: String, ok: Boolean) { if (ok && id !in owned) out += id }
        val st = l.studies
        f("sphinx", l.spells.sumOf { it.good } >= 40 || l.tickets.count { it.level >= 3 } >= 15)
        f("salamander", st.any { it.kind == Study.CANDLE && it.minutes >= 50 && it.away == 0 })
        f("golem", l.days.size >= 30)
        f("pixie", st.count { it.minutes in 1..15 } >= 15)
        f("chimera", l.byDay.values.count { d -> d.map { it.subjectId }.toSet().size >= 2 } >= 10)
        f("hydra", l.byDay.values.any { d -> d.map { schoolOf(it.subjectId) }.toSet().size >= 5 })
        f("moonhare", st.count { hourOf(it.start) >= 20 } >= 15)
        f("griffin", st.count { hourOf(it.start) in 4..8 } >= 15)
        f("kitsune", l.materials.any { it.finished > 0 })
        f("mimic", l.questions.size >= 10)
        f("bookwyrm", run { val pages = l.materials.filter { it.unit == Material.PAGES }.map { it.id }.toSet(); st.filter { it.materialId in pages }.sumOf { it.units } >= 500 })
        f("raven", st.any { it.kind == Study.TEACH })
        f("mandrake", l.tasks.count { it.state >= 2 || (it.state >= 1 && it.last == 1) } >= 25)
        f("kraken", st.groupBy { weekStart(it.day) }.values.any { w -> w.map { schoolOf(it.subjectId) }.toSet().size >= 5 })
        f("shade", l.days.sorted().zipWithNext().any { (a, b) -> b - a >= 10 })
        f("owl", l.tasks.count { it.state >= 1 && it.due > 0 && it.doneDay in 1 until it.due } >= 20)
        f("gargoyle", l.attends.count { it.state == Attend.WAS } >= 60)
        f("hob", l.s.packed.size >= 20 || l.tasks.size >= 30)
        f("magpie", l.grades.count { it.examId == 0L && Marks.isTop(l.s.scale, it.value, l.s.passes) } >= 25)
        f("cat", l.rests >= 25)
        f("dragon", l.tasks.any { it.big && it.state >= 3 } || l.materials.any { it.finished > 0 && it.unit == Material.LESSONS && it.total >= 20 })
        f("phoenix", l.exams.any { it.done && it.score >= 0 && it.kind != Exam.TEST })
        f("unicorn", l.hours >= 100)
        return out
    }

    /** A species for an egg from the fair: one not owned yet, never a legendary one. */
    fun anyEgg(owned: Set<String>, seed: Int): String? {
        val pool = Pets.all.filter { it.id !in owned && !it.rare && it.id != "inkling" }
        if (pool.isEmpty()) return null
        return pool[Rand(seed).int(pool.size)].id
    }

    /** The stat a study of this school and kind trains. */
    fun statOf(kind: Int, school: String, away: Int, minutes: Int): Stat = when {
        kind == Study.TEACH -> Stat.ELO
        kind == Study.TICKETS -> Stat.WIS
        school == "rune" -> Stat.ELO
        kind == Study.CANDLE && away == 0 -> Stat.FOC
        minutes >= 60 -> Stat.STA
        school == "num" || school == "art" || school == "star" -> Stat.WIT
        else -> Stat.STA
    }
}
