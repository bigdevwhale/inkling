package app.inkling.domain

import app.inkling.data.Pet

/**
 * A kind of magical creature. All but the inkling are found by studying a certain way ([find]);
 * [perk] is what it doubles while it lives in the tower (the leader or at a desk).
 */
data class Species(
    val id: String, val ru: String, val en: String, val color: Long,
    val names: List<String>, val namesEn: List<String>,
    val perkRu: String, val perkEn: String,
    val findRu: String, val findEn: String,
    val perk: Perk,
    val rare: Boolean = false,
) {
    val title get() = tr(ru, en)
    val perkText get() = tr(perkRu, perkEn)
    val find get() = tr(findRu, findEn)
}

/** The stages: an egg, then four steps of study. */
enum class Stage { EGG, BABY, TEEN, ADULT, MASTER }

/** What a creature doubles while it's in the tower. */
enum class Perk { SPELL, RECALL, FOCUS, DAYS, SHORT, MIX, UNITS, EVENING, MORNING, FINISH, QUESTION, READ, TEACH, TASK, QUEST, COMEBACK, DEADLINE, ATTEND, PLAN, GRADE, REST, ALL }

/** The six stats every creature has. */
enum class Stat(val ru: String, val en: String, val whyRu: String, val whyEn: String, val color: Long) {
    WIS("Мудрость", "Wisdom", "растёт, когда вы вспоминаете заклинания гримуара и учите билеты", "grows when you recall the grimoire's spells and learn tickets", 0xFF6A7AE0),
    FOC("Сосредоточенность", "Focus", "растёт от свечей, догоревших без отвлечений", "grows on candles that burn down undisturbed", 0xFFE0A040),
    WIT("Смекалка", "Wit", "растёт от задач, лаб и домашних заданий", "grows on problems, labs and homework", 0xFF4AA88A),
    ELO("Красноречие", "Eloquence", "растёт, когда вы объясняете темы и учите языки", "grows when you explain topics and study languages", 0xFFD0609A),
    STA("Стойкость", "Stamina", "растёт от долгих занятий и пар, где вы были", "grows on long study and classes attended", 0xFFC0704A),
    CUR("Любопытство", "Curiosity", "растёт от вопросов в сундуке мимика", "grows on questions in the mimic's chest", 0xFF9A6AD0);
    val title get() = tr(ru, en)
    val why get() = tr(whyRu, whyEn)
}

object Pets {
    const val HATCH = 6
    const val TEEN = 600; const val ADULT = 3000; const val MASTER = 12000

    private fun sp(id: String, ru: String, en: String, color: Long, names: String, namesEn: String, perk: Perk, perkRu: String, perkEn: String,
                   findRu: String, findEn: String, rare: Boolean = false) =
        Species(id, ru, en, color, names.split(","), namesEn.split(","), perkRu, perkEn, findRu, findEn, perk, rare)

    val all: List<Species> = listOf(
        sp("inkling", "Кляксик", "Inkling", 0xFF3E3A8A, "Клякса,Чернилка,Капа,Перышко,Кляксик", "Blot,Inky,Drip,Quill,Splotch", Perk.SPELL,
            "Записывает в гримуар всё, что вы запомнили: за новые заклинания – вдвое больше маны", "Writes down everything you learn: new spells bring double mana",
            "Живёт с вами с первого дня", "Lives with you from day one"),
        sp("sphinx", "Сфинкс", "Sphinx", 0xFFD9B060, "Загадка,Песчинка,Аменхотик,Сфинкси,Ребус", "Riddle,Sandy,Amun,Sphinxie,Puzzle", Perk.RECALL,
            "Загадывает заклинания из гримуара и билеты: каждый верный ответ – вдвое больше маны", "Asks the grimoire's spells and tickets: every right answer brings double mana",
            "Вспомните 40 заклинаний гримуара или выучите 15 билетов", "Recall 40 spells from the grimoire or learn 15 tickets"),
        sp("salamander", "Саламандра", "Salamander", 0xFFE0602A, "Уголёк,Искра,Огневка,Жарик,Фитиль", "Ember,Spark,Blaze,Cinder,Wick", Perk.FOCUS,
            "Живёт в пламени свечи: свечи без отвлечений приносят вдвое больше", "Lives in the candle flame: undisturbed candles bring double",
            "Дожгите свечу в 50 минут ни разу не отвлёкшись", "Burn a 50-minute candle without once looking away"),
        sp("golem", "Голем", "Golem", 0xFF7A8A7A, "Валун,Мох,Камешек,Глыба,Руник", "Boulder,Moss,Pebble,Rocky,Rune", Perk.DAYS,
            "Медленно, но каждый день: первое занятие дня даёт вдвое больше", "Slow but every day: the first study of a day brings double",
            "Занимайтесь в 30 разных дней", "Study on 30 different days"),
        sp("pixie", "Пикси", "Pixie", 0xFF7AC07A, "Стрекоза,Листик,Минутка,Искорка,Пыльца", "Dragonfly,Leaf,Minute,Twinkle,Pollen", Perk.SHORT,
            "Обожает короткие занятия: десять минут считаются за двадцать", "Loves short study: ten minutes count as twenty",
            "15 коротких занятий – до 15 минут", "15 short sessions – up to 15 minutes"),
        sp("chimera", "Химера", "Chimera", 0xFFC09040, "Трёхглав,Рык,Мешанка,Коза,Химка", "Patch,Roar,Mixer,Goaty,Chimmy", Perk.MIX,
            "Любит чередование: второй предмет за день приносит вдвое больше", "Loves mixing: the second subject of a day brings double",
            "10 дней, когда вы учили больше одного предмета", "10 days on which you studied more than one subject"),
        sp("hydra", "Гидра", "Hydra", 0xFF2E9A9A, "Трилистник,Волна,Тройка,Плеск,Гидрочка", "Trefoil,Wave,Triple,Splash,Hydri", Perk.UNITS,
            "Каждой голове по книге: страницы, уроки и задачи материалов дают вдвое больше", "A book for each head: pages, lessons and tasks of materials bring double",
            "Четыре разные школы магии за один день", "Four different schools of magic in one day"),
        sp("moonhare", "Лунный заяц", "Moon hare", 0xFFC8C8E0, "Серп,Полночь,Лунник,Пушок,Звездоух", "Crescent,Midnight,Luna,Fluff,Starear", Perk.EVENING,
            "Вечерние занятия, после шести, дают вдвое больше", "Evening study, after six, brings double",
            "15 занятий после восьми вечера", "15 sessions after 8 pm"),
        sp("griffin", "Грифон", "Griffin", 0xFFE0B040, "Рассвет,Солнышко,Клювик,Перо,Гриф", "Dawn,Sunny,Beak,Plume,Griff", Perk.MORNING,
            "Утренние занятия, до полудня, дают вдвое больше", "Morning study, before noon, brings double",
            "15 занятий до девяти утра", "15 sessions before 9 am"),
        sp("kitsune", "Кицунэ", "Kitsune", 0xFFE8E0F0, "Огонёк,Хвостик,Лисичка,Туман,Юки", "Foxfire,Tails,Kit,Mist,Yuki", Perk.FINISH,
            "За каждый законченный материал у неё вырастает хвост, а законченные материалы дают вдвое больше", "Grows a tail for each finished material; finishing brings double",
            "Закончите первый материал – курс, книгу или задачник", "Finish your first material – a course, a book or a problem book"),
        sp("mimic", "Мимик", "Mimic", 0xFFA0703A, "Сундучок,Щёлк,Ам,Ларчик,Замочек", "Chesty,Snap,Chomp,Coffer,Latch", Perk.QUESTION,
            "Глотает вопросы и выплёвывает ответы: вопросы в сундуке приносят вдвое больше", "Swallows questions and spits out answers: questions bring double",
            "Положите в сундук десять вопросов", "Put ten questions in the chest"),
        sp("bookwyrm", "Книжный змей", "Bookwyrm", 0xFF4A9A4A, "Корешок,Закладка,Переплёт,Буквоед,Страничка", "Spine,Bookmark,Binder,Glutton,Page", Perk.READ,
            "Живёт среди книг: страницы приносят вдвое больше", "Lives among books: pages bring double",
            "Прочитайте 500 страниц по материалам", "Read 500 pages of your materials"),
        sp("raven", "Ворон-эхо", "Echo raven", 0xFF2A2A3A, "Эхо,Мудрец,Лектор,Карлуша,Вороний", "Echo,Sage,Lecturer,Corvin,Quoth", Perk.TEACH,
            "Повторяет за вами: «Научи фамильяра» приносит вдвое больше", "Repeats after you: «teach the familiar» brings double",
            "Первый раз объясните тему фамильяру", "Explain a topic to your familiar for the first time"),
        sp("mandrake", "Мандрагора", "Mandrake", 0xFFB0A070, "Корешок,Росток,Пискля,Ботва,Мандра", "Rooty,Sprout,Squeak,Leafy,Drake", Perk.TASK,
            "Любит практику: сданные задания и лабы приносят вдвое больше", "Loves practice: tasks and labs handed in bring double",
            "Сдайте 25 заданий или лаб", "Hand in 25 tasks or labs"),
        sp("kraken", "Кракенёнок", "Kraken", 0xFF8A4AA0, "Щупальце,Пучина,Чернилыч,Осьминожка,Кракуша", "Tentacle,Deep,Inkpot,Octo,Krakie", Perk.QUEST,
            "Держит сразу всё: задания недели приносят вдвое больше маны", "Holds everything at once: weekly quests bring double mana",
            "Пять разных школ магии за одну неделю", "Five different schools of magic in one week"),
        sp("shade", "Тень", "Shade", 0xFF3A3A4A, "Сумрак,Дымок,Тенька,Мгла,Уголок", "Dusk,Smoky,Shady,Gloom,Nook", Perk.COMEBACK,
            "Встречает после перерыва: первое занятие после паузы в три дня – втрое больше", "Welcomes you back: the first study after three days off brings triple",
            "Вернитесь к учёбе после перерыва в десять дней", "Come back to study after ten days off"),
        sp("owl", "Сова-вестница", "Messenger owl", 0xFFA07A50, "Почта,Сургуч,Ухтыш,Совушка,Конверт", "Post,Seal,Hoot,Owlet,Letter", Perk.DEADLINE,
            "Носит письма о сроках: задания, сданные заранее, приносят вдвое больше", "Carries deadline letters: tasks done early bring double",
            "20 заданий, сделанных хотя бы за день до срока", "20 tasks done at least a day before they're due"),
        sp("gargoyle", "Гаргулья", "Gargoyle", 0xFF8A8A9A, "Карниз,Звонарь,Горгуля,Камнеглаз,Страж", "Ledge,Bell,Gargy,Stoneeye,Warden", Perk.ATTEND,
            "Сторожит расписание: каждая пара или урок, где вы были, приносит вдвое больше", "Guards the timetable: every class attended brings double",
            "Отметьте 60 занятий по расписанию, где вы были", "Mark 60 timetable classes you attended"),
        sp("hob", "Домовой", "Hob", 0xFFC0503A, "Кузя,Нафаня,Портфелич,Бородач,Шуршун", "Hobby,Satchel,Beardy,Rustle,Bramble", Perk.PLAN,
            "Следит за порядком: собранный портфель и задания, записанные вовремя, дают вдвое больше", "Keeps things tidy: a packed satchel and tasks written down bring double",
            "Соберите портфель 20 раз или запишите 30 заданий", "Pack the satchel 20 times or write down 30 tasks"),
        sp("magpie", "Сорока-отличница", "Star magpie", 0xFF3A4A6A, "Пятёрка,Звёздочка,Трещотка,Отличница,Белобока", "Ace,Starry,Chatter,Topper,Glint", Perk.GRADE,
            "Собирает звёзды: каждая записанная оценка приносит вдвое больше", "Collects stars: every mark written down brings double",
            "25 высших оценок", "25 top marks"),
        sp("cat", "Кот-фамильяр", "Familiar cat", 0xFF2A2430, "Уголь,Мурлин,Колпак,Тыква,Бархат", "Coal,Merlin,Hatty,Pumpkin,Velvet", Perk.REST,
            "Напоминает отдыхать: перемена после свечи приносит ману", "Reminds you to rest: a break after a candle brings mana",
            "25 раз отдохните на перемене после свечи", "Take a break after a candle 25 times"),
        sp("dragon", "Дракон", "Dragon", 0xFFC03030, "Рубин,Печать,Пламень,Дракоша,Горыч", "Ruby,Seal,Blaze,Draco,Smoulder", Perk.ALL,
            "Легенда академии. С ним вся мана приходит с прибавкой", "A legend of the academy. With it, all mana comes with a bonus",
            "Доведите до конца большую работу – курсовую, диплом или проект – или закончите курс из 20 уроков", "Finish a big work – coursework, thesis or project – or a course of 20 lessons", rare = true),
        sp("phoenix", "Феникс", "Phoenix", 0xFFE08020, "Пепел,Жар,Финист,Заря,Огнекрыл", "Ash,Flare,Fenix,Dawnfire,Pyre", Perk.ALL,
            "Возрождается из пепла. С ним вся мана приходит с прибавкой", "Rises from the ashes. With it, all mana comes with a bonus",
            "Сдайте испытание – экзамен, зачёт или ОГЭ/ЕГЭ – и запишите результат", "Pass a trial – an exam, a credit or a state exam – and write down the result", rare = true),
        sp("unicorn", "Единорог", "Unicorn", 0xFFD8B8E8, "Сияние,Рог,Лунный свет,Перламутр,Звездопад", "Glow,Horn,Moonlight,Pearl,Starfall", Perk.ALL,
            "Приходит к тем, кто учится долго. С ним вся мана приходит с прибавкой", "Comes to those who study long. With it, all mana comes with a bonus",
            "Наберите 100 часов учёбы", "Reach 100 hours of study", rare = true),
    )
    val byId = all.associateBy { it.id }
    fun of(id: String) = byId[id] ?: all[0]
}

fun Pet.stage(): Stage = when {
    hatched == 0L -> Stage.EGG
    xp < Pets.TEEN -> Stage.BABY
    xp < Pets.ADULT -> Stage.TEEN
    xp < Pets.MASTER -> Stage.ADULT
    else -> Stage.MASTER
}

fun Stage.title() = when (this) {
    Stage.EGG -> tr("Яйцо", "Egg"); Stage.BABY -> tr("Детёныш", "Hatchling"); Stage.TEEN -> tr("Ученик", "Pupil")
    Stage.ADULT -> tr("Подмастерье", "Journeyman"); Stage.MASTER -> tr("Маг", "Mage")
}

/** XP to the next stage, as (have, need) within the stage. */
fun Pet.toNext(): Pair<Int, Int> = when (stage()) {
    Stage.EGG -> growth to Pets.HATCH
    Stage.BABY -> xp to Pets.TEEN
    Stage.TEEN -> (xp - Pets.TEEN) to (Pets.ADULT - Pets.TEEN)
    Stage.ADULT -> (xp - Pets.ADULT) to (Pets.MASTER - Pets.ADULT)
    Stage.MASTER -> 1 to 1
}

/** The school it was raised on most, once it has grown up – its speciality («Рунный грифон»). */
fun Pet.speciality(): School? = if (stage() < Stage.ADULT) null else schoolMap().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.let { Schools.of(it.key) }
