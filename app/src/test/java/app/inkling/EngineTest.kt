package app.inkling

import app.inkling.data.Grade
import app.inkling.data.Pet
import app.inkling.data.Spell
import app.inkling.domain.Engine
import app.inkling.domain.Marks
import app.inkling.domain.Pets
import app.inkling.domain.Schools
import app.inkling.domain.Stage
import app.inkling.domain.Stat
import app.inkling.domain.stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {
    @Test fun schoolsAreGuessedFromNames() {
        assertEquals("num", Schools.guess("Математический анализ"))
        assertEquals("num", Schools.guess("Матанализ"))
        assertEquals("num", Schools.guess("Эконометрика"))
        assertEquals("art", Schools.guess("Сопромат"))
        assertEquals("heal", Schools.guess("Нормальная физиология"))
        assertEquals("body", Schools.guess("Физкультура"))
        assertEquals("star", Schools.guess("Физика"))
        assertEquals("rune", Schools.guess("Русский язык"))
        assertEquals("rune", Schools.guess("Правописание"))
        assertEquals("seal", Schools.guess("Гражданское право"))
        assertEquals("trade", Schools.guess("Маркетинг"))
        assertEquals("herb", Schools.guess("Окружающий мир"))
        assertEquals("art", Schools.guess("Python programming"))
        assertEquals("art", Schools.guess("Computer Science"))
        assertEquals("illus", Schools.guess("ИЗО"))
        assertEquals("own", Schools.guess("Вязание крючком"))
    }

    @Test fun foodWaitsForTheNight() {
        val lead = Pet(id = 1, species = "inkling", name = "a", found = 0, hatched = 1, slot = 0)
        val desk = Pet(id = 2, species = "owl", name = "b", found = 0, hatched = 1, slot = 1)
        val fed = Engine.feed(listOf(lead, desk), 60, "num", Stat.FOC, 10)
        assertEquals(60, fed.pets[0].belly); assertEquals(30, fed.pets[1].belly)
        assertEquals(0, fed.pets[0].xp); assertEquals(1, fed.pets[0].foc)
        val (night, _) = Engine.digest(fed.pets)
        assertEquals(60, night[0].xp); assertEquals(0, night[0].belly)
        assertEquals(60, night[0].schoolMap()["num"])
    }

    @Test fun eggsHatchFromStudy() {
        var p = listOf(Pet(id = 1, species = "owl", name = "o", found = 0))
        repeat(Pets.HATCH) { p = Engine.feed(p, 25, "num", null, 5).pets }
        assertEquals(Stage.BABY, p[0].stage())
    }

    @Test fun spellsClimbTheSteps() {
        var s = Spell(subjectId = 1, title = "x", created = 0, due = 1)
        s = Engine.recall(s, 1, 1); assertEquals(1, s.step); assertEquals(4L, s.due)
        s = Engine.recall(s, 3, 4); assertEquals(0, s.step); assertEquals(5L, s.due)
        repeat(5) { s = Engine.recall(s, 1, s.due) }
        assertTrue(Engine.learned(s))
    }

    @Test fun theNudgeCountsTopMarks() {
        val gs = listOf(4.0, 4.0, 3.0).map { Grade(subjectId = 1, value = it, day = 0) }
        val n = Marks.nudge("5", gs, listOf(61, 76, 91))!!
        assertTrue(n, n.contains("5"))
        val top = listOf(5.0, 5.0, 5.0).map { Grade(subjectId = 1, value = it, day = 0) }
        assertTrue(Marks.nudge("5", top, listOf(61, 76, 91))!!.isNotEmpty())
        assertEquals("4", Marks.termMark("100", 80.0, listOf(61, 76, 91)))
    }
}
