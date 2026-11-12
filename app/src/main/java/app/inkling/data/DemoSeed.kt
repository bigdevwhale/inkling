package app.inkling.data

import app.inkling.AppContainer
import app.inkling.domain.Engine
import app.inkling.domain.Lang
import app.inkling.domain.Shop
import app.inkling.domain.Timetable
import app.inkling.domain.today
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * Four months of a second-year student, lived through the real engine day by day (the clock is moved),
 * so creatures, mana, quests and finds are what they would be. Demo builds only.
 */
object DemoSeed {
    suspend fun seedIfEmpty(c: AppContainer) {
        if (c.settings.now().onboarded || c.dao.studyList().isNotEmpty()) return
        val ru = Lang.ru
        fun n(r: String, e: String) = if (ru) r else e
        val t0 = today()
        val start = t0 - 120
        val r = Random(11)
        c.clock = { start }
        c.begin(Settings.UNI, 2, "5", n("Аня", "Anna"), listOf(
            n("Математический анализ", "Calculus"), n("Линейная алгебра", "Linear algebra"), n("Программирование на Python", "Python programming"),
            n("Английский язык", "Academic English"), n("Физика", "Physics"), n("История России", "Modern history"), n("Философия", "Philosophy")))
        // the screens wait until the whole history is in
        c.settings.update { it.copy(onboarded = false, parityStart = app.inkling.domain.weekStart(start), terms = Timetable.defaultTerms(Settings.UNI, t0)) }
        val subs = c.dao.subjectList()
        val (mat, lin, py, eng, phys, hist, phil) = subs.map { it.id }.let { listOf(it[0], it[1], it[2], it[3], it[4], it[5], it[6]) }
        c.saveSubject(subs[0].copy(teacher = n("Петров Игорь Семёнович", "Dr Peters"), contact = n("ауд. 512, консультации по средам в 15:00", "Room 512, office hours Wed 3 pm"),
            rules = n("Автомат при 85 баллах за семестр и всех сданных ДЗ", "Exempt from the exam with 85 points and all homework in"), credits = 6.0))
        c.saveSubject(subs[1].copy(credits = 4.0)); c.saveSubject(subs[2].copy(credits = 5.0, teacher = n("Ким Анна Викторовна", "Ms Kim"))); c.saveSubject(subs[3].copy(credits = 3.0))
        c.saveSubject(subs[4].copy(credits = 5.0)); c.saveSubject(subs[5].copy(credits = 2.0)); c.saveSubject(subs[6].copy(credits = 2.0))
        // the week
        suspend fun slot(sub: Long, wd: Int, from: String, to: String, kind: Int, room: String, parity: Int = 0) =
            c.saveSlot(Slot(subjectId = sub, weekday = wd, start = app.inkling.domain.parseHm(from)!!, end = app.inkling.domain.parseHm(to)!!, kind = kind, room = room, parity = parity))
        slot(mat, 1, "9:00", "10:30", Slot.LECTURE, "512"); slot(lin, 1, "10:40", "12:10", Slot.SEMINAR, "305"); slot(eng, 1, "12:40", "14:10", Slot.PRACTICE, "214")
        slot(py, 2, "10:40", "12:10", Slot.LECTURE, "101"); slot(py, 2, "12:40", "14:10", Slot.LAB, n("комп. класс 3", "Lab 3"))
        slot(phys, 3, "9:00", "10:30", Slot.LECTURE, "401"); slot(mat, 3, "10:40", "12:10", Slot.SEMINAR, "512"); slot(hist, 3, "12:40", "14:10", Slot.LECTURE, "Б-1", 1)
        slot(phil, 3, "12:40", "14:10", Slot.SEMINAR, "Б-2", 2)
        slot(lin, 4, "9:00", "10:30", Slot.LECTURE, "305"); slot(phys, 4, "10:40", "12:10", Slot.LAB, n("лаб. 2", "Lab 2")); slot(eng, 4, "12:40", "14:10", Slot.PRACTICE, "214")
        slot(mat, 5, "10:40", "12:10", Slot.SEMINAR, "512"); slot(py, 5, "12:40", "14:10", Slot.SEMINAR, "101")
        // materials
        val stepik = c.saveMaterial(Material(subjectId = py, title = n("Stepik: «Поколение Python»", "Python course online"), unit = Material.LESSONS, total = 48, created = start))
        val demid = c.saveMaterial(Material(subjectId = mat, title = n("Демидович, задачник", "Problem book"), unit = Material.TASKS, total = 220, created = start))
        val feyn = c.saveMaterial(Material(subjectId = phys, title = n("Фейнмановские лекции, т. 1", "Feynman Lectures, vol. 1"), unit = Material.PAGES, total = 380, created = start))
        val eng1 = c.saveMaterial(Material(subjectId = eng, title = n("English Grammar in Use", "Academic Vocabulary in Use"), unit = Material.CHAPTERS, total = 30, done = 6, created = start))
        val small = c.saveMaterial(Material(subjectId = lin, title = n("Видеокурс «Линал за 20 роликов»", "Linear algebra in 20 videos"), unit = Material.VIDEOS, total = 20, created = start))
        val thesis = c.saveTask(Task(subjectId = py, title = n("Курсовая: телеграм-бот для расписания", "Coursework: a timetable bot"), kind = Task.COURSEWORK, due = t0 + 40, created = start + 10,
            stages = n("Тема\nОбзор литературы\nТеоретическая часть\nПрактическая часть\nЧерновик\nЗащита", "Topic\nLiterature review\nTheory\nPractice\nDraft\nDefence")))
        val topics = mapOf(
            mat to n("Предел последовательности,Признак Коши,Непрерывность функции,Производная сложной функции,Правило Лопиталя,Ряд Тейлора,Интеграл по частям,Несобственный интеграл,Признак Даламбера,Теорема Ролля",
                "Limits of sequences,Cauchy criterion,Continuity,Chain rule,L'Hôpital's rule,Taylor series,Integration by parts,Improper integrals,Ratio test,Rolle's theorem"),
            lin to n("Ранг матрицы,Определитель,Собственные векторы,Базис пространства,Метод Гаусса,Обратная матрица", "Matrix rank,Determinant,Eigenvectors,Basis,Gaussian elimination,Inverse matrix"),
            py to n("Списковые включения,Декораторы,Генераторы,Словари и хеши,Исключения,Классы и наследование", "List comprehensions,Decorators,Generators,Dicts and hashing,Exceptions,Classes"),
            eng to n("Present Perfect vs Past Simple,Conditionals,Passive voice,Linking words,Phrasal verbs", "Hedging language,Linking words,Nominalisation,Passive voice,Citing sources"),
            phys to n("Законы Ньютона,Закон сохранения импульса,Работа и энергия,Момент инерции,Колебания", "Newton's laws,Momentum,Work and energy,Moment of inertia,Oscillations"),
            hist to n("Реформы Петра I,Отмена крепостного права,НЭП", "The Industrial Revolution,The Congress of Vienna,The Cold War"),
            phil to n("Категорический императив,Пещера Платона,Cogito Декарта", "The categorical imperative,Plato's cave,Descartes' cogito"))
            .mapValues { it.value.split(',') }
        val questions = listOf(mat to n("Почему ряд 1/n расходится, а 1/n² – нет?", "Why does 1/n diverge but 1/n² converge?"), py to n("Чем генератор отличается от итератора?", "How is a generator different from an iterator?"),
            phys to n("Откуда берётся момент инерции в формуле?", "Where does the moment of inertia come from?"), lin to n("Зачем нужны собственные векторы на практике?", "What are eigenvectors for in practice?"),
            phil to n("Можно ли сомневаться в самом сомнении?", "Can one doubt doubt itself?"), eng to n("Когда в академическом тексте уместен пассив?", "When is the passive right in academic writing?"))
        var qi = 0
        val morning = { day: Long, h: Int, m: Int -> LocalDate.ofEpochDay(day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + (h * 60 + m) * 60_000L }

        for (day in start..t0) {
            c.clock = { day }; c.now = { morning(day, 22, 0) }
            c.tick()
            val s = c.settings.now()
            val wd = LocalDate.ofEpochDay(day).dayOfWeek.value
            val classes = Timetable.on(s, c.dao.slotList(), day)
            val sick = day in (start + 47)..(start + 50)
            if (sick) { c.setSick(day, true); continue }
            // classes: attended mostly, homework set at some
            for (sl in classes) {
                if (day == t0 && sl.end > app.inkling.domain.nowMin()) continue
                c.attend(sl, day, if (r.nextInt(12) == 0) Attend.MISSED else if (r.nextInt(30) == 0) Attend.CANCELLED else Attend.WAS)
                if (sl.kind == Slot.SEMINAR || sl.kind == Slot.PRACTICE || sl.kind == Slot.LAB) if (r.nextInt(2) == 0) {
                    val due = Timetable.nextClass(s, c.dao.slotList(), sl.subjectId, day) ?: (day + 7)
                    val kind = if (sl.kind == Slot.LAB) Task.LAB else Task.HW
                    c.saveTask(Task(subjectId = sl.subjectId, title = if (kind == Task.LAB) n("Лабораторная № ${r.nextInt(1, 9)}", "Lab ${r.nextInt(1, 9)}") else n("№ ${100 + r.nextInt(300)}, ${100 + r.nextInt(300)}; конспект", "Ex. ${100 + r.nextInt(300)}, ${100 + r.nextInt(300)}; notes"),
                        kind = kind, due = due, created = day))
                }
                if (r.nextInt(5) == 0) c.addGrade(Grade(subjectId = sl.subjectId, value = listOf(5.0, 5.0, 4.0, 4.0, 4.0, 3.0, 5.0)[r.nextInt(7)], day = day, label = n("ответ на семинаре", "seminar answer")))
            }
            // tasks due tomorrow get done today, some earlier
            c.dao.taskList().filter { it.open && !it.big && it.due > 0 && it.due - day in 0..2 && r.nextInt(3) != 0 }.forEach { t -> c.setTaskState(t, if (t.kind == Task.LAB) minOf(t.state + 1 + r.nextInt(2), 3) else 1) }
            // own study in the evening (more at weekends), sometimes a morning one
            val sessions = when { wd >= 6 -> r.nextInt(1, 4); r.nextInt(5) == 0 -> 0; else -> r.nextInt(1, 3) }
            repeat(sessions) { k ->
                val sub = listOf(mat, mat, lin, py, py, eng, phys, phys, hist, phil)[r.nextInt(10)]
                val mins = listOf(25, 25, 25, 45, 50, 60, 15, 90)[r.nextInt(8)]
                val h = if (k == 0 && r.nextInt(4) == 0) 8 else 18 + k * 2
                val matId = when (sub) { py -> stepik; mat -> demid; phys -> feyn; eng -> eng1; lin -> small; else -> 0L }
                val units = when (sub) { py -> r.nextInt(0, 2); mat -> r.nextInt(2, 8); phys -> r.nextInt(5, 20); eng -> if (r.nextInt(3) == 0) 1 else 0; lin -> if (r.nextInt(2) == 0) 1 else 0; else -> 0 }
                c.addStudy(Study(subjectId = sub, day = day, start = morning(day, h, r.nextInt(0, 40)), minutes = mins, kind = if (r.nextInt(3) == 0) Study.MANUAL else Study.CANDLE,
                    materialId = if (units > 0) matId else 0, units = units, away = if (r.nextInt(3) == 0) r.nextInt(1, 3) else 0))
                if (r.nextInt(3) != 0) topics[sub]?.let { l -> val tp = l[r.nextInt(l.size)]; if (c.dao.spellList().none { it.title == tp }) c.addSpell(sub, tp, "") }
                if (r.nextInt(2) == 0) c.endRest(true)
            }
            if (wd == 6 && r.nextInt(2) == 0) { val sub = listOf(mat, phys, py, phil)[r.nextInt(4)]; val tp = topics[sub]!![r.nextInt(topics[sub]!!.size)]
                c.addStudy(Study(subjectId = sub, day = day, start = morning(day, 12, 0), minutes = 20, kind = Study.TEACH, note = tp)) }
            if (day % 9 == 0L && qi < questions.size) { val (sub, q) = questions[qi++]; c.addQuestion(sub, q) }
            if (day % 13 == 0L) c.dao.questionList().firstOrNull { it.answered == 0L }?.let { c.answerQuestion(it, n("Разобрались на консультации", "Sorted out at office hours")) }
            // the sphinx's riddles, answered most evenings
            if (r.nextInt(4) != 0) c.dao.spellList().filter { it.due <= day && !Engine.learned(it) }.forEach { sp -> c.answerSpell(sp, if (r.nextInt(5) == 0) 2 else if (r.nextInt(8) == 0) 3 else 1) }
            // the coursework moves on now and then
            if (day % 17 == 0L) c.dao.taskList().firstOrNull { it.id == thesis }?.let { t -> if (t.stage < 4) c.setStage(t, t.stage + 1) }
            // furniture, when there's enough mana
            val st = c.settings.now()
            Shop.all.firstOrNull { it.id !in st.items && st.mana >= it.price + 60 }?.let { c.buy(it.id) }
        }
        // a past test and the coming session
        c.saveExam(Exam(subjectId = lin, title = n("Контрольная по матрицам", "Matrices test"), kind = Exam.TEST, day = start + 60)).let { id -> c.finishExam(c.dao.examList().first { it.id == id }, 4.0) }
        c.saveExam(Exam(subjectId = hist, title = n("Зачёт по истории", "History credit"), kind = Exam.CREDIT, day = t0 - 3)).let { id -> c.finishExam(c.dao.examList().first { it.id == id }, 1.0) }
        val mex = c.saveExam(Exam(subjectId = mat, title = n("Экзамен по матанализу", "Calculus exam"), kind = Exam.EXAM, day = t0 + 24))
        c.setTickets(mex, (1..30).joinToString("\n") { i -> "$i. " + (topics[mat]!![(i - 1) % topics[mat]!!.size]) + if (i > 10) n(" (часть ${(i - 1) / 10 + 1})", " (part ${(i - 1) / 10 + 1})") else "" })
        c.dao.ticketList().filter { it.examId == mex }.forEach { t -> c.setTicketLevel(t, listOf(3, 3, 2, 2, 2, 1, 1, 0, 0, 0)[r.nextInt(10)]) }
        c.saveExam(Exam(subjectId = py, title = n("Зачёт по Python", "Python credit"), kind = Exam.CREDIT, day = t0 + 18))
        c.saveExam(Exam(subjectId = phys, title = n("Экзамен по физике", "Physics exam"), kind = Exam.EXAM, day = t0 + 29))
        c.clock = { today() }; c.now = { System.currentTimeMillis() }
        c.settings.update { it.copy(onboarded = true, inbox = emptyList()) }
        c.tick()
    }
}

private operator fun <T> List<T>.component6() = this[5]
private operator fun <T> List<T>.component7() = this[6]
