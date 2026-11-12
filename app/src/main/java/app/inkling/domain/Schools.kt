package app.inkling.domain

/**
 * The schools of magic every real subject falls into. Together they cover every faculty; a subject's school
 * is guessed from its name ([guess]) and can be changed by hand. [wood] is the wand wood this school gives.
 */
data class School(val id: String, val ru: String, val en: String, val color: Long, val aboutRu: String, val aboutEn: String,
                  val woodRu: String, val woodEn: String, val woodColor: Long, val stems: List<String>) {
    val title get() = tr(ru, en)
    val about get() = tr(aboutRu, aboutEn)
    val wood get() = tr(woodRu, woodEn)
}

object Schools {
    private fun s(id: String, ru: String, en: String, color: Long, aboutRu: String, aboutEn: String, woodRu: String, woodEn: String, wood: Long, stems: String) =
        School(id, ru, en, color, aboutRu, aboutEn, woodRu, woodEn, wood, stems.split('|').map { it.trim() }.filter { it.isNotEmpty() })

    // the order matters: the first school whose stem fits wins («физиология» is healing before «физ…» is the stars)
    val all: List<School> = listOf(
        s("body", "Закалка", "Hardening", 0xFFD0603A, "физкультура, спорт, ОБЖ", "PE, sport, safety",
            "граб", "hornbeam", 0xFFB59A72,
            "физкульт|физическ культур|физическая культура|физ-ра|физра|спорт|обж|бжд|безопасность жизнедеятельности|основы безопасности|лфк|плаван|лёгкая атлетика|легкая атлетика|=pe|physical education|sport|athletic|swimming|fitness|gym"),
        s("heal", "Целительство", "Healing", 0xFFD65A7A, "медицина, анатомия, физиология", "medicine, anatomy, physiology",
            "бузина", "elder", 0xFFC9A98A,
            "анатом|физиолог|медицин|патолог|гистолог|цитолог|хирург|терапи|педиатр|стоматолог|сестрин|гигиен|фармаколог|микробиолог|иммунолог|эпидемиолог|кардиолог|неврол|акушер|гинеколог|офтальмолог|психиатр|первая помощь|пропедевтик|клиническ|anatom|physiolog|medic|nurs|patholog|histolog|surger|pharmacolog|microbiolog|immunolog|clinical|first aid|dentist"),
        s("num", "Нумерология", "Numerology", 0xFF4A6FD8, "математика, статистика, эконометрика", "maths, statistics, econometrics",
            "ясень", "ash", 0xFFD9C29A,
            "математ|алгебр|геометр|матан|матанализ|анализ|статист|вероятн|дискрет|линейн|эконометр|исследование операций|численн|теория чисел|арифметик|тригонометр|дифференц|интеграл|комбинатор|топологи|теория граф|матлог|высшая математ|=мат|math|algebra|geometr|calculus|statistic|probab|arithmetic|trigonometr|discrete|linear|numerical|econometric|topolog"),
        s("star", "Звездочётство", "Starcraft", 0xFF5B4BB0, "физика, астрономия, механика", "physics, astronomy, mechanics",
            "тис", "yew", 0xFF9C5A3A,
            "физик|физ |=физ|астроном|термодинам|оптик|квант|электродинам|механик|ядерн|атомн|астрофиз|геофиз|космо|теплотех|гидравлик|аэродинам|physic|astronom|thermodynam|optic|quantum|mechanic|nuclear|astrophys|cosmolog"),
        s("alch", "Алхимия", "Alchemy", 0xFF3E9A6A, "химия, материаловедение, фармация", "chemistry, materials, pharmacy",
            "кипарис", "cypress", 0xFFA88250,
            "хими|биохим|материаловед|фармац|фармацевт|металлург|нефтехим|полимер|токсиколог|chemi|biochem|material|pharmac|metallurg|polymer|toxicolog"),
        s("herb", "Травничество", "Herbalism", 0xFF5E9A3A, "биология, экология, география", "biology, ecology, geography",
            "ива", "willow", 0xFFC2B27A,
            "биолог|эколог|ботаник|зоолог|генетик|агро|почв|лесн|лесо|географ|окружающий мир|природовед|естествозн|естествен|геолог|океанолог|метеоролог|ветеринар|растен|животн|микологи|biolog|ecolog|botan|zoolog|genetic|agro|soil|forest|geograph|geolog|natural science|environment|veterinar"),
        s("art", "Артефакторика", "Artificing", 0xFFC07A2E, "информатика, программирование, инженерия", "computing, programming, engineering",
            "дуб", "oak", 0xFF8A5A2E,
            "информат|программ|алгоритм|баз данных|базы данных|сет|сопромат|сопротивление материалов|электро|схемотех|черчен|чертёж|инженер|технолог|робот|труд|=ит|=it|=ии|=ai|=ui|=ux|компьютер|веб|микропроцесс|операционн|машинн|детали машин|теоретическая механика|термех|метролог|автоматиз|радиотех|телеком|кибер|криптограф|тестирован|python|java|kotlin|javascript|c++|c#|=sql|=go|rust|swift|computer|programming|coding|software|engineering|electronic|circuit|network|database|algorithm|robot|machine learning|data science|web|devops|android|ios"),
        s("rune", "Руны", "Runes", 0xFF2E8FA6, "языки, литература, лингвистика", "languages, literature, linguistics",
            "вишня", "cherry", 0xFFB0603E,
            "правопис|язык|английск|немецк|французск|испанск|итальянск|китайск|японск|корейск|арабск|турецк|латин|греческ|польск|португальск|лингвист|литератур|чтени|русский|родной|грамматик|перевод|журналист|риторик|письм|орфограф|словесн|стилистик|филолог|=ielts|=toefl|english|german|french|spanish|italian|chinese|japanese|korean|arabic|latin|greek|language|literature|linguist|reading|writing|grammar|translation|journalism|rhetoric|vocabulary|phonetic|spelling"),
        s("chron", "Хронология", "Chronology", 0xFF9A6A3A, "история, философия, археология", "history, philosophy, archaeology",
            "орех", "walnut", 0xFF6A4A30,
            "истори|археолог|философ|религи|культуролог|обществознан|обществ|этик|логик|антрополог|этнограф|краевед|искусствовед|теолог|богослов|history|philosoph|archaeolog|religio|culture|ethic|logic|anthropolog|social stud|civics|theolog"),
        s("seal", "Печати", "Seals", 0xFF8A3A3A, "право, госуправление, политика", "law, government, politics",
            "красное дерево", "mahogany", 0xFF7A3A2A,
            "право|правов|юрисп|юридич|конституц|гражданск|уголовн|административн|госуправ|государствен|международн|политолог|политик|дипломат|таможен|нотариат|адвокат|криминалист|law|legal|jurisprud|constitution|criminal|civil|government|politic|diplomac|international"),
        s("trade", "Торговая магия", "Trade magic", 0xFFC9A02A, "экономика, менеджмент, финансы", "economics, management, finance",
            "клён", "maple", 0xFFD2A060,
            "эконом|менеджм|маркет|финанс|бухгалт|учёт|учет|аудит|бизнес|налог|логистик|банк|предприним|коммерц|торгов|инвест|страхов|стартап|продаж|управлен|=hr|econom|manag|market|financ|account|audit|business|tax|logistic|bank|entrepreneur|commerce|invest|sales"),
        s("mind", "Чары разума", "Mind charms", 0xFF9A5AB0, "психология, педагогика, социология", "psychology, education, sociology",
            "осина", "aspen", 0xFFC8C0A8,
            "психолог|педагог|социолог|социальн|дефектолог|конфликтолог|логопед|нейро|когнитив|воспитан|дидактик|андрагог|psycholog|pedagog|sociolog|social|education|teaching|cognitive|neuro|counsel"),
        s("illus", "Иллюзии", "Illusions", 0xFFD07AA0, "дизайн, искусство, музыка, архитектура", "design, art, music, architecture",
            "груша", "pear", 0xFFD8B890,
            "дизайн|архитект|музык|рисован|=изо|изобразит|живопис|кино|театр|хореограф|танц|фотограф|композиц|сольфеджио|искусств|=мхк|скульптур|графи|анимаци|актёрск|актерск|вокал|пени|гитар|фортепиано|скрипк|режиссур|мода|fashion|design|architect|music|drawing|painting|film|cinema|theatre|theater|dance|photograph|composition|sculpt|graphic|animation|acting|vocal|singing|guitar|piano|violin|art"),
        s("own", "Своя школа", "Own school", 0xFF6A7A8A, "всё, что не нашло школы", "everything else",
            "можжевельник", "juniper", 0xFFB09070, ""),
    )
    val byId = all.associateBy { it.id }
    fun of(id: String) = byId[id] ?: all.last()

    /**
     * The school a subject's name points to. A stem matches the start of any word of the name («матан» in
     * «Матанализ»), a stem with spaces matches anywhere, and «=ит» only the whole word.
     */
    fun guess(name: String): String {
        val n = name.lowercase().replace('ё', 'е').replace(Regex("[^\\p{L}\\p{N}+# ]"), " ").replace(Regex("\\s+"), " ").trim()
        if (n.isEmpty()) return "own"
        val words = n.split(' ')
        for (s in all) for (raw in s.stems) {
            val st = raw.replace('ё', 'е')
            val hit = when {
                st.startsWith("=") -> words.any { it == st.drop(1) }
                st.contains(' ') -> " $n ".contains(if (st.endsWith(' ')) " $st" else st)
                else -> words.any { it.startsWith(st) }
            }
            if (hit) return s.id
        }
        return "own"
    }
}
