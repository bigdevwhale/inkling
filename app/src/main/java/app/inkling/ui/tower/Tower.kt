package app.inkling.ui.tower

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.inkling.container
import app.inkling.currentLanguage
import app.inkling.data.Backup
import app.inkling.data.Settings
import app.inkling.domain.Feast
import app.inkling.domain.Pets
import app.inkling.domain.Shop
import app.inkling.domain.Timetable
import app.inkling.domain.Words
import app.inkling.domain.fmtDayShort
import app.inkling.domain.hm
import app.inkling.domain.parseHm
import app.inkling.domain.tr
import app.inkling.domain.weekStart
import app.inkling.setLanguage
import app.inkling.ui.Data
import app.inkling.ui.Nav
import app.inkling.ui.art.Art
import app.inkling.ui.art.EggView
import app.inkling.ui.art.Pose
import app.inkling.ui.art.Sky
import app.inkling.ui.art.TowerScene
import app.inkling.ui.components.Badge
import app.inkling.ui.components.Btn
import app.inkling.ui.components.Card
import app.inkling.ui.components.Chip
import app.inkling.ui.components.Field
import app.inkling.ui.components.Fold
import app.inkling.ui.components.Ghost
import app.inkling.ui.components.Icon
import app.inkling.ui.components.Icons
import app.inkling.ui.components.ManaPill
import app.inkling.ui.components.Page
import app.inkling.ui.components.Section
import app.inkling.ui.components.Segments
import app.inkling.ui.components.TimeChip
import app.inkling.ui.components.card
import app.inkling.ui.components.pickDate
import app.inkling.ui.components.subjectColor
import app.inkling.ui.theme.C
import app.inkling.ui.theme.T
import kotlinx.coroutines.launch

/** The academy fair: things for the tower and surprise eggs, for mana. */
@Composable
fun ShopPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var preview by remember { mutableStateOf<String?>(null) }
    val eggPrice = c.eggPrice(d.today)
    Page(tr("Ярмарка академии", "The academy fair"), { nav.pop() }, action = { ManaPill(d.s.mana, Modifier.padding(end = 10.dp)) }) {
        Box(Modifier.fillMaxWidth().card(20.dp)) { TowerScene(d.team, d.s.items - d.s.hidden, Pose.IDLE, preview = preview, sky = Sky.of(d.s), spines = d.finished.map { subjectColor(d.subById[it.subjectId]) }) }
        Text(tr("Мана даётся за учёбу: свечи, заклинания, объяснения, вопросы, задания. Настоящие деньги здесь не нужны.",
            "Mana comes from study: candles, spells, explanations, questions, tasks. No real money here."), style = T.small, modifier = Modifier.padding(top = 10.dp))
        Section(tr("Яйцо с ярмарки", "An egg from the fair"))
        val left = Pets.all.count { sp -> !sp.rare && sp.id != "inkling" && d.pets.none { it.species == sp.id } }
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EggView("kraken", 64.dp, .3f); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Яйцо-сюрприз", "A surprise egg"), style = T.bodyB)
                    Text(if (left == 0) tr("Все уже живут в башне", "Everyone already lives here") else tr("Кто-то, кого ещё нет в башне", "Someone not yet in the tower"), style = T.small)
                    if (Feast.on(d.today) == Feast.SOLSTICE) Text(tr("Солнцеворот – за полцены!", "Solstice – half price!"), style = T.smallB.copy(color = C.gold))
                }
                Btn("$eggPrice", small = true, enabled = d.s.mana >= eggPrice && left > 0) { c.write { if (buyEgg()) say(tr("Яйцо в башне!", "An egg in the tower!")) } }
            }
        }
        Section(tr("Для башни", "For the tower"))
        Shop.all.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { it0 ->
                val have = it0.id in d.s.items; val hidden = it0.id in d.s.hidden
                Column(Modifier.weight(1f).card(16.dp).clickable { preview = if (preview == it0.id) null else it0.id }.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ItemPic(it0.id)
                    Text(it0.title, style = T.smallB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    when {
                        have -> Text(if (hidden) tr("Достать", "Bring out") else tr("Убрать", "Put away"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable {
                            c.write { settings.update { s -> s.copy(hidden = if (hidden) s.hidden - it0.id else s.hidden + it0.id) } } }.padding(4.dp))
                        else -> Btn("${it0.price}", Modifier.padding(top = 4.dp), small = true, enabled = d.s.mana >= it0.price) { c.write { if (buy(it0.id)) say(tr("Куплено: ${it0.title}", "Bought: ${it0.title}")) }; preview = null }
                    }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        } }
    }
}

@Composable
private fun ItemPic(id: String) {
    val ctx = LocalContext.current
    val img = Art.item(ctx, id)
    if (img != null) Image(img, null, Modifier.size(72.dp), filterQuality = FilterQuality.Medium) else Box(Modifier.size(72.dp))
}

// ---------------- settings ----------------

object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "inkling"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
    const val SUPPORT = "https://bigdevwhale.github.io/stillcraft/support/"
    const val SITE = "https://bigdevwhale.github.io/stillcraft/"
    const val CODE = "https://github.com/bigdevwhale/inkling"
}

@Composable
private fun LinkRow(icon: Icons, tint: Color, title: String, sub: String?, url: String) {
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Badge(icon, tint, 36.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, style = T.bodyB); if (sub != null) Text(sub, style = T.small) }
        Icon(Icons.NEXT, C.mute, 18.dp)
    }
}

@Composable
private fun Step(value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Ghost("−", small = true, onClick = onMinus)
        Text(value, style = T.bodyB, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Ghost("+", small = true, onClick = onPlus)
    }
}

@Composable
private fun Toggle(title: String, sub: String?, on: Boolean, onSet: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onSet(!on) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = T.bodyB); if (sub != null) Text(sub, style = T.small) }
        Chip(if (on) tr("вкл", "on") else tr("выкл", "off"), on) { onSet(!on) }
    }
}

@Composable
fun SettingsPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = d.s
    fun up(f: (Settings) -> Settings) = c.write { settings.update(f) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch { val text = Backup.export(c); runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } }
            c.say(tr("Копия сохранена", "Backup saved")) } }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { val text = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull().orEmpty()
            c.say(if (Backup.import(c, text)) tr("Башня восстановлена", "The tower is restored") else tr("Это не копия Inkling", "That's not an Inkling backup")) } }
    Page(tr("Настройки", "Settings"), { nav.pop() }) {
        Fold(Icons.CAP, C.acc, tr("Где учитесь", "Where you study"), "${modeName(s.mode)} · ${Words.year(s)}") {
            Segments(listOf(tr("Школа", "School"), tr("Колледж", "College"), tr("Вуз", "University"), tr("Сам(а)", "Self")),
                listOf(Settings.SCHOOL, Settings.COLLEGE, Settings.UNI, Settings.SELF).indexOf(s.mode).coerceAtLeast(0)) { i ->
                val m = listOf(Settings.SCHOOL, Settings.COLLEGE, Settings.UNI, Settings.SELF)[i]; up { it.copy(mode = m, satchel = m == Settings.SCHOOL) } }
            Text(if (s.school) tr("Класс", "Year") else tr("Курс академии", "Academy year"), style = T.small, modifier = Modifier.padding(top = 10.dp))
            Step(Words.year(s), { up { it.copy(year = (it.year - 1).coerceAtLeast(1)) } }, { up { it.copy(year = (it.year + 1).coerceAtMost(11)) } })
            var name by remember { mutableStateOf(s.student) }
            Field(name, { name = it; up { st -> st.copy(student = it.trim()) } }, tr("Имя для открыток и зачётки", "A name for cards and the record book"), modifier = Modifier.padding(top = 10.dp))
        }
        if (!s.self) Fold(Icons.STAR, C.gold, tr("Оценки", "Marks"), scaleName(s.scale)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("5", "10", "12", "100", "letter", "pass").forEach { sc -> Chip(scaleName(sc), s.scale == sc) { up { it.copy(scale = sc) } } } }
            if (s.scale == "100") {
                Text(tr("Баллы на «3», «4» и «5»", "Points for a 3, a 4 and a 5"), style = T.small, modifier = Modifier.padding(top = 10.dp))
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip("61 · 76 · 91", s.passes == listOf(61, 76, 91)) { up { it.copy(passes = listOf(61, 76, 91)) } }
                    Chip("51 · 71 · 86", s.passes == listOf(51, 71, 86)) { up { it.copy(passes = listOf(51, 71, 86)) } }
                    Chip("50 · 70 · 85", s.passes == listOf(50, 70, 85)) { up { it.copy(passes = listOf(50, 70, 85)) } }
                }
            }
        }
        Fold(Icons.CANDLE, C.gold, tr("Свеча и перемена", "Candle and break"), tr("${s.focus} мин · перемена ${s.rest} мин", "${s.focus} min · break ${s.rest} min")) {
            Text(tr("Сколько горит свеча по умолчанию", "The candle's default length"), style = T.small)
            Step(tr("${s.focus} мин", "${s.focus} min"), { up { it.copy(focus = (it.focus - 5).coerceAtLeast(5)) } }, { up { it.copy(focus = (it.focus + 5).coerceAtMost(120)) } })
            Text(tr("Перемена после свечи", "The break after a candle"), style = T.small, modifier = Modifier.padding(top = 10.dp))
            Step(tr("${s.rest} мин", "${s.rest} min"), { up { it.copy(rest = (it.rest - 1).coerceAtLeast(1)) } }, { up { it.copy(rest = (it.rest + 1).coerceAtMost(30)) } })
        }
        if (!s.self) Fold(Icons.BELL, C.acc, if (s.school) tr("Звонки", "Bells") else tr("Пары", "Periods"), tr("${s.bells.size} в день", "${s.bells.size} a day")) {
            s.bells.forEachIndexed { i, b -> val p = b.split('-'); val a = parseHm(p[0]) ?: 0; val z = parseHm(p.getOrElse(1) { "0" }) ?: 0
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", style = T.bodyB, modifier = Modifier.width(28.dp))
                    TimeChip(a) { v -> up { st -> st.copy(bells = st.bells.toMutableList().also { l -> l[i] = "${hm(v)}-${hm(z)}" }) } }
                    Text(" – ", style = T.body)
                    TimeChip(z) { v -> up { st -> st.copy(bells = st.bells.toMutableList().also { l -> l[i] = "${hm(a)}-${hm(v)}" }) } }
                    Spacer(Modifier.weight(1f))
                    Text("×", style = T.h3.copy(color = C.mute), modifier = Modifier.clickable { up { st -> st.copy(bells = st.bells.filterIndexed { k, _ -> k != i }) } }.padding(8.dp))
                } }
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Ghost(tr("Добавить", "Add"), Modifier.weight(1f), small = true) { up { st -> val last = st.bells.lastOrNull()?.split('-')?.getOrNull(1)?.let { parseHm(it) } ?: (8 * 60)
                    val len = if (st.school) 45 else 90; st.copy(bells = st.bells + "${hm(last + 10)}-${hm(last + 10 + len)}") } }
                Ghost(tr("Как обычно", "Typical"), Modifier.weight(1f), small = true) { up { it.copy(bells = Timetable.defaultBells(it.mode)) } }
            }
            Spacer(Modifier.height(10.dp))
            Toggle(tr("Недели чередуются", "Weeks alternate"), tr("числитель и знаменатель", "odd and even weeks"), s.parityStart != 0L) { on -> up { it.copy(parityStart = if (on) weekStart(d.today) else 0L) } }
            if (s.parityStart != 0L) {
                val p = Timetable.parity(s, d.today)
                Text(tr("Эта неделя – ", "This week is ") + if (p == 1) tr("числитель", "odd") else tr("знаменатель", "even"), style = T.small)
                Ghost(tr("Нет, наоборот", "No, the other way"), Modifier.fillMaxWidth().padding(top = 6.dp), small = true) { up { it.copy(parityStart = it.parityStart + if (p == 1) 7 else -7) } }
            }
        }
        if (!s.self) Fold(Icons.CAL, C.ok, if (s.school) tr("Четверти и каникулы", "Terms and holidays") else tr("Семестры и каникулы", "Semesters and holidays"), tr("${s.terms.size} · между ними – каникулы", "${s.terms.size} · holidays between")) {
            Timetable.terms(s).forEachIndexed { i, t ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", style = T.bodyB, modifier = Modifier.width(28.dp))
                    Chip(fmtDayShort(t.first), false) { pickDate(ctx, t.first) { v -> up { st -> st.copy(terms = Timetable.terms(st).toMutableList().also { l -> l[i] = v..l[i].last }.map { "${it.first}-${it.last}" }) } } }
                    Text(" – ", style = T.body)
                    Chip(fmtDayShort(t.last), false) { pickDate(ctx, t.last) { v -> up { st -> st.copy(terms = Timetable.terms(st).toMutableList().also { l -> l[i] = l[i].first..v }.map { "${it.first}-${it.last}" }) } } }
                    Spacer(Modifier.weight(1f))
                    Text("×", style = T.h3.copy(color = C.mute), modifier = Modifier.clickable { up { st -> st.copy(terms = Timetable.terms(st).filterIndexed { k, _ -> k != i }.map { "${it.first}-${it.last}" }) } }.padding(8.dp))
                }
            }
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Ghost(tr("Добавить", "Add"), Modifier.weight(1f), small = true) { up { st -> val last = Timetable.terms(st).lastOrNull()?.last ?: d.today; st.copy(terms = st.terms + "${last + 10}-${last + 70}") } }
                Ghost(tr("На этот год", "This year's"), Modifier.weight(1f), small = true) { up { it.copy(terms = Timetable.defaultTerms(it.mode, d.today)) } }
            }
            Text(tr("Даты по умолчанию – примерные, поправьте под своё расписание.", "The default dates are approximate – adjust them to yours."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
        }
        Fold(Icons.BELL, C.gold, tr("Напоминания", "Reminders"), if (s.evening < 0) tr("вечером выключены", "evening off") else tr("вечером в ${s.evening}:00", "evening at ${s.evening}:00")) {
            if (!s.self) Toggle(tr("После занятия", "After a class"), tr("«были? что задали?» – пока свежо", "«were you there? what was set?» while it's fresh"), s.afterClass) { v -> up { it.copy(afterClass = v) } }
            if (s.school) Toggle(tr("Портфель на завтра", "Tomorrow's satchel"), tr("вечером домовой поможет собраться", "the hob helps you pack in the evening"), s.satchel) { v -> up { it.copy(satchel = v) } }
            Text(tr("Вечернее напоминание – портфель, сфинкс или завтрашний срок, одно за раз", "The evening reminder – the satchel, the sphinx or tomorrow's deadline, one at a time"), style = T.small, modifier = Modifier.padding(top = 8.dp))
            Step(if (s.evening < 0) tr("выключено", "off") else "${s.evening}:00", { up { it.copy(evening = if (it.evening <= 16) -1 else it.evening - 1) } }, { up { it.copy(evening = if (it.evening < 0) 17 else (it.evening + 1).coerceAtMost(22)) } })
            Text(tr("Во время уроков и на каникулах башня молчит.", "During classes and holidays the tower stays quiet."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
            if (android.os.Build.VERSION.SDK_INT >= 33) { val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
                Ghost(tr("Разрешить уведомления", "Allow notifications"), Modifier.fillMaxWidth().padding(top = 8.dp), small = true) { ask.launch(android.Manifest.permission.POST_NOTIFICATIONS) } }
        }
        Fold(Icons.CROSS, C.bad, tr("Лазарет", "Infirmary"), if (d.today in s.sick) tr("сегодня на больничном", "on sick leave today") else tr("если заболели", "if you're ill")) {
            Text(tr("В лазарете существа спят, а напоминания молчат. Ничего не сгорает.", "In the infirmary the creatures sleep and reminders are silent. Nothing is lost."), style = T.small)
            Toggle(tr("Сегодня болею", "I'm ill today"), null, d.today in s.sick) { v -> c.write { setSick(d.today, v) } }
        }
        Fold(Icons.PALETTE, C.acc, tr("Вид и язык", "Look and language"), null) {
            Segments(listOf(tr("Светлая", "Light"), tr("Тёмная", "Dark"), tr("Как в системе", "System")), when (s.theme) { "light" -> 0; "dark" -> 1; else -> 2 }) { i -> up { it.copy(theme = listOf("light", "dark", "system")[i]) } }
            Spacer(Modifier.height(10.dp))
            val lang = currentLanguage()
            Segments(listOf("Русский", "English", tr("Как в системе", "System")), when (lang) { "ru" -> 0; "en" -> 1; else -> 2 }) { i -> setLanguage(listOf("ru", "en", "")[i]) }
            Text(tr("За окном – погода в городе: ${s.town.ifBlank { "не определён" }}", "Out of the window – the weather in: ${s.town.ifBlank { "unknown" }}"), style = T.small, modifier = Modifier.padding(top = 10.dp))
        }
        Fold(Icons.SAVE, C.ok, tr("Резервная копия", "Backup"), tr("файл со всей башней", "one file with the whole tower")) {
            Btn(tr("Сохранить копию", "Save a backup"), Modifier.fillMaxWidth(), small = true) { save.launch("inkling-${java.time.LocalDate.now()}.json") }
            Ghost(tr("Восстановить из файла", "Restore from a file"), Modifier.fillMaxWidth().padding(top = 8.dp), small = true) { open.launch(arrayOf("application/json", "*/*")) }
            Text(tr("Восстановление заменит всё, что есть сейчас.", "Restoring replaces everything there is now."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
        }
        Fold(Icons.HEART, C.bad, tr("Поддержать", "Support"), tr("StillCraft держится на поддержке пользователей", "StillCraft lives on goodwill")) { LinkRow(Icons.HEART, C.bad, tr("Поддержать разработку", "Support development"), null, Community.SUPPORT) }
        Fold(Icons.GLOBE, C.ok, "StillCraft", tr("Inkling – одно из небольших приложений StillCraft", "Inkling is one of the small StillCraft apps")) {
            LinkRow(Icons.HEART, C.acc, tr("Сообщество в Telegram", "Telegram community"), tr("новости, обсуждения, другие приложения", "news, talk and the other apps"), Community.CHAT)
            LinkRow(Icons.CLOSE, C.bad, tr("Сообщить об ошибке", "Report a bug"), tr("через бота StillCraft", "through the StillCraft bot"), Community.BUG)
            LinkRow(Icons.SPARK, C.gold, tr("Предложить идею", "Suggest an idea"), null, Community.IDEA)
            LinkRow(Icons.STAR, C.ok, tr("Следить за обновлениями", "Follow updates"), tr("бот напишет, когда выйдет новая версия", "the bot writes when a new version is out"), Community.FOLLOW)
            LinkRow(Icons.HOME, C.acc, tr("Все приложения StillCraft", "All StillCraft apps"), null, Community.SITE)
            LinkRow(Icons.LIST, C.mute, tr("Исходный код", "Source code"), "GitHub", Community.CODE)
        }
        Fold(Icons.SPARK, C.acc, tr("Подсказки", "Hints"), tr("короткие пояснения на экранах", "short notes on the screens")) {
            Ghost(tr("Показать заново", "Show again"), Modifier.fillMaxWidth(), small = true) { up { it.copy(seen = emptySet()) }; c.say(tr("Подсказки покажутся снова", "Hints will show again")) } }
        Text(tr("Inkling работает без аккаунта. Всё хранится только на телефоне; в интернет уходят лишь координаты города для погоды за окном. Погода – Open-Meteo и MET Norway.",
            "Inkling needs no account. Everything stays on the phone; only the town's coordinates go online, for the weather outside the window. Weather: Open-Meteo and MET Norway."), style = T.tiny, modifier = Modifier.padding(top = 14.dp))
        Spacer(Modifier.height(20.dp))
    }
}

fun modeName(m: String) = when (m) { Settings.SCHOOL -> tr("Школа", "School"); Settings.COLLEGE -> tr("Колледж", "College"); Settings.UNI -> tr("Вуз", "University"); else -> tr("Сам(а)", "Self-study") }
fun scaleName(s: String) = when (s) { "5" -> tr("5-балльная", "1–5"); "10" -> tr("10-балльная", "1–10"); "12" -> tr("12-балльная", "1–12"); "100" -> tr("Баллы (БРС)", "Points (0–100)"); "letter" -> "A–F"; else -> tr("Зачёт", "Pass/fail") }
