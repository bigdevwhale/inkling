package app.inkling.ui.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.inkling.R
import app.inkling.data.Pet
import app.inkling.domain.Pets
import app.inkling.domain.Shop
import app.inkling.domain.Span
import app.inkling.domain.Stage
import app.inkling.domain.Stats
import app.inkling.domain.fmtDayShort
import app.inkling.domain.stage
import app.inkling.domain.title
import app.inkling.domain.tr
import app.inkling.ui.Data
import app.inkling.ui.art.Art
import app.inkling.ui.art.Pose
import app.inkling.ui.art.frameOf
import java.io.File
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

import app.inkling.domain.Marks
import app.inkling.domain.Schools
import app.inkling.domain.Stat
import app.inkling.domain.Timetable
import app.inkling.domain.Words
import app.inkling.domain.count
import app.inkling.domain.dur
import app.inkling.domain.fmt1
import app.inkling.domain.fmtDay
import app.inkling.domain.monthName
import app.inkling.domain.speciality
import app.inkling.domain.weekStart
import app.inkling.data.Exam
import app.inkling.ui.magic.WandSpec
import app.inkling.ui.magic.coreName

enum class CardKind(val ru: String, val en: String) {
    WEEK("Неделя", "Week"), MONTH("Месяц", "Month"), YEAR("Год", "Year"), TERM("Табель", "Report"), MATERIAL("Материал", "Material"), TRIAL("Свиток", "Scroll"),
    PET("Существо", "Creature"), TOWER("Башня", "Tower"), COLLECTION("Коллекция", "Collection"), WAND("Палочка", "Wand"), BOOK("Зачётка", "Record book");
    val label get() = tr(ru, en)
}

/** What to draw: a card kind and what it is about (a week's Monday, a material, a trial, a creature, a year, a term's start). */
data class ShareReq(val kind: CardKind, val id: Long = 0)

/** How a card looks: the format, the background, whether marks are hidden. */
data class Look(val story: Boolean = false, val bg: Int = 0, val hide: Boolean = false)

/** Backgrounds: two colours top to bottom and whether the text is light; -1 is the tower picture. */
val BACKS = listOf(
    Triple(0xFF3A2F80.toInt(), 0xFF120E30.toInt(), true), Triple(0xFF6A2E5A.toInt(), 0xFF21102A.toInt(), true), Triple(0xFF1E4A5A.toInt(), 0xFF0C1A24.toInt(), true),
    Triple(0xFFF3ECDD.toInt(), 0xFFE3D3B4.toInt(), false), Triple(0xFFF2D27A.toInt(), 0xFFC98A3A.toInt(), false), Triple(-1, -1, true),
)

class Painter(private val ctx: Context, private val d: Data, private val look: Look) {
    private val W = 1080; private val H = if (look.story) 1920 else 1350
    private val bold: Typeface = ResourcesCompat.getFont(ctx, R.font.lora_medium) ?: Typeface.DEFAULT_BOLD
    private val sans: Typeface = ResourcesCompat.getFont(ctx, R.font.nunito_bold) ?: Typeface.DEFAULT_BOLD
    private val reg: Typeface = ResourcesCompat.getFont(ctx, R.font.nunito_regular) ?: Typeface.DEFAULT
    private val num: Typeface = ResourcesCompat.getFont(ctx, R.font.inter_semibold) ?: Typeface.DEFAULT_BOLD
    private val back = BACKS[look.bg.coerceIn(0, BACKS.lastIndex)]
    private val light = back.third
    private val ink = if (light) 0xFFFFFFFF.toInt() else 0xFF231D33.toInt()
    private val sub = if (light) 0xC8FFFFFF.toInt() else 0xA0231D33.toInt()
    private val accent = if (light) 0xFFFFD36B.toInt() else 0xFF4B3FA0.toInt()
    private val panel = if (light) 0x24FFFFFF else 0x14231D33
    private val top = if (look.story) 300f else 200f
    private val bottom get() = H - 150f

    private fun mark(v: String) = if (look.hide) "•" else v
    private fun p(color: Int = ink) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    private fun base(kicker: String): Pair<Bitmap, Canvas> {
        val b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888); val c = Canvas(b)
        if (back.first == -1) {
            Art.get(ctx, "tower_bg")?.asAndroidBitmap()?.let { g -> val s = H.toFloat() / g.height; val w = g.width * s
                c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p(0xFF2A1E3A.toInt()))
                c.drawBitmap(g, Rect(0, 0, g.width, g.height), RectF((W - w) / 2, 0f, (W + w) / 2, H.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG)) }
            c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, 0f, H.toFloat(), 0xA0100A20.toInt(), 0xE6100A20.toInt(), Shader.TileMode.CLAMP) })
        } else {
            c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, W * .4f, H.toFloat(), back.first, back.second, Shader.TileMode.CLAMP) })
            // a sprinkle of stars
            val star = p(if (light) 0x55FFF1C2 else 0x334B3FA0)
            for (k in 0 until 40) { val x = (k * 397 % 1000) / 1000f * W; val y = (k * 613 % 1000) / 1000f * H; c.drawCircle(x, y, 2f + (k % 3) * 1.5f, star) }
            for ((x, y, r) in listOf(Triple(.92f, .08f, .42f), Triple(.05f, .62f, .36f), Triple(.8f, .95f, .3f)))
                c.drawCircle(W * x, H * y, W * r, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(W * x, H * y, W * r, if (light) 0x28FFFFFF else 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP) })
        }
        val hy = if (look.story) 130f else 70f
        c.drawRoundRect(RectF(60f, hy, 60f + 280f, hy + 64f), 32f, 32f, p(panel))
        Art.prop(ctx, "quill")?.asAndroidBitmap()?.let { img -> val s = 48f / max(img.width, img.height)
            c.drawBitmap(img, Rect(0, 0, img.width, img.height), RectF(76f, hy + 8f, 76f + img.width * s, hy + 8f + img.height * s), Paint(Paint.FILTER_BITMAP_FLAG)) }
        c.t("Inkling", 136f, hy + 45f, 34f, bold)
        c.t(kicker.uppercase(), W - 60f, hy + 44f, 30f, sans, sub, Paint.Align.RIGHT, maxW = W - 420f)
        return b to c
    }

    private fun Canvas.t(s: String, x: Float, y: Float, size: Float, face: Typeface = bold, color: Int = ink, align: Paint.Align = Paint.Align.LEFT, maxW: Float = when (align) { Paint.Align.CENTER -> W - 120f; Paint.Align.RIGHT -> x - 60f; else -> W - x - 60f }) {
        val pt = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; typeface = face; this.color = color; textAlign = align }
        var str = s; if (pt.measureText(str) > maxW) { while (pt.measureText("$str…") > maxW && str.length > 2) str = str.dropLast(1); str += "…" }
        drawText(str, x, y, pt)
    }
    private fun Canvas.panel(x: Float, y: Float, w: Float, h: Float, r: Float = 36f, color: Int = panel) = drawRoundRect(RectF(x, y, x + w, y + h), r, r, p(color))
    private fun Canvas.facts(items: List<Pair<String, String>>, y: Float, h: Float = 170f) {
        val n = items.size; val gap = 18f; val w = (W - 120f - gap * (n - 1)) / n
        items.forEachIndexed { i, (v, l) -> val x = 60f + i * (w + gap); panel(x, y, w, h)
            t(v, x + w / 2, y + h * .52f, if (v.length > 8) 40f else 52f, num, ink, Paint.Align.CENTER, w - 24); t(l, x + w / 2, y + h * .8f, 27f, reg, sub, Paint.Align.CENTER, w - 24) }
    }
    private fun Canvas.bitmap(img: Bitmap, cx: Float, foot: Float, size: Float, shadow: Boolean = true) {
        val s = min(size / img.width, size / img.height); val w = img.width * s; val h = img.height * s
        if (shadow) drawOval(RectF(cx - w * .36f, foot - h * .04f, cx + w * .36f, foot + h * .04f), p(0x33000000))
        drawBitmap(img, Rect(0, 0, img.width, img.height), RectF(cx - w / 2, foot - h, cx + w / 2, foot), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    }
    private fun Canvas.pet(p: Pet?, pose: Pose, cx: Float, foot: Float, size: Float) {
        p ?: return
        val st = p.stage()
        val img = (if (st == Stage.EGG) Art.egg(ctx, p.species) else Art.pet(ctx, p.species, frameOf(st, pose)))?.asAndroidBitmap() ?: return
        val scale = when (st) { Stage.EGG -> .7f; Stage.BABY -> .62f; Stage.TEEN -> .8f; else -> .96f }
        bitmap(img, cx, foot, size * scale)
    }
    private fun Canvas.prop(id: String, cx: Float, foot: Float, size: Float) { Art.prop(ctx, id)?.asAndroidBitmap()?.let { bitmap(it, cx, foot, size, false) } }
    private fun Canvas.footer() = t(tr("Inkling · академия, где учатся вместе с существами", "Inkling · an academy where you study with magical creatures"), W / 2f, H - 64f, 28f, reg, sub, Paint.Align.CENTER)
    private fun Canvas.bars(rows: List<Triple<String, Int, Int>>, y0: Float, maxRows: Int): Float {
        var y = y0; val max = (rows.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
        rows.take(maxRows).forEach { (name, v, col) ->
            panel(60f, y, W - 120f, 92f, 28f)
            drawRoundRect(RectF(60f, y, 60f + (W - 120f) * v / max, y + 92f), 28f, 28f, p((col and 0xFFFFFF) or 0x55000000))
            t(name, 96f, y + 58, 38f, sans, ink, maxW = 600f)
            t(dur(v * 60), W - 96f, y + 58, 36f, num, accent, Paint.Align.RIGHT)
            y += 108
        }
        return y
    }

    /** The tower in [r]: the room, the window, the things and the creatures. */
    private fun Canvas.tower(r: RectF) {
        save(); clipPath(Path().apply { addRoundRect(r, 40f, 40f, Path.Direction.CW) })
        app.inkling.ui.art.drawSky(this, r, app.inkling.ui.art.Sky.of(d.s), .3f)
        Art.get(ctx, "tower_bg")?.asAndroidBitmap()?.let { drawBitmap(it, Rect(0, 0, it.width, it.height), r, Paint(Paint.FILTER_BITMAP_FLAG)) }
        val items = d.s.items - d.s.hidden
        for (it in Shop.all.filter { it.id in items }.sortedBy { if (it.wall) 0 else 1 }) {
            val img = Art.item(ctx, it.id)?.asAndroidBitmap() ?: continue
            val w = r.width() * it.w; val h = w * img.height / img.width
            val x = r.left + r.width() * it.x - w / 2; val y = r.top + if (it.wall) r.height() * it.y - h / 2 else r.height() * it.y - h
            drawBitmap(img, Rect(0, 0, img.width, img.height), RectF(x, y, x + w, y + h), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        val h = r.height(); val w = r.width()
        d.team.filter { it.slot in 1..2 }.forEach { pt -> val s = h * .3f; pet(pt, Pose.IDLE, r.left + w * (if (pt.slot == 1) .2f else .66f) + s / 2, r.top + h * .97f, s) }
        d.leader?.let { pet(it, Pose.CHEER, r.centerX(), r.top + h * .99f, h * .44f) }
        app.inkling.ui.art.drawNight(this, r, app.inkling.ui.art.Sky.of(d.s), false)
        restore()
    }

    fun render(r: ShareReq): Pair<Bitmap, String> = when (r.kind) {
        CardKind.WEEK -> { val m = weekStart(if (r.id > 0) r.id else d.today); span(Span(m, m + 6, ""), tr("Неделя в академии", "A week in the academy"), "${fmtDayShort(m)} – ${fmtDayShort(m + 6)}") }
        CardKind.MONTH -> { val m = LocalDate.ofEpochDay(if (r.id > 0) r.id else d.today).withDayOfMonth(1)
            span(Span(m.toEpochDay(), m.plusMonths(1).toEpochDay() - 1, ""), monthName(m.monthValue).replaceFirstChar { it.uppercase() }, "${m.year}") }
        CardKind.TERM -> term(Timetable.terms(d.s).firstOrNull { it.first == r.id } ?: Timetable.terms(d.s).lastOrNull { it.first <= d.today } ?: (d.today - 90)..d.today)
        CardKind.YEAR -> year(if (r.id > 0) r.id.toInt() else LocalDate.now().year)
        CardKind.MATERIAL -> material(r.id); CardKind.TRIAL -> trial(r.id); CardKind.PET -> pet(r.id)
        CardKind.TOWER -> towerCard(); CardKind.COLLECTION -> collection(); CardKind.WAND -> wand(); CardKind.BOOK -> book()
    }

    private fun span(sp: Span, title: String, kicker: String): Pair<Bitmap, String> {
        val (b, c) = base(kicker)
        val st = Stats(d.studies, sp, d.subById, d.today)
        var y = top + 60
        c.t(title, 60f, y + 30, 80f); y += 90
        c.facts(listOf(dur(st.minutes * 60) to tr("учёбы", "of study"), "${st.days}" to tr("дней", "days"), "${(st.clean * 100).toInt()}%" to tr("без отвлечений", "undisturbed")), y, 160f)
        y += 200
        y = c.bars(st.bySubject().map { Triple(it.subject?.name ?: "–", it.minutes, Schools.of(it.subject?.school ?: "own").color.toInt()) }, y, if (look.story) 6 else 4)
        val spells = d.spells.count { it.created in sp.from..sp.to }
        if (spells > 0) { c.t(tr("+ ${count(spells, "заклинание", "заклинания", "заклинаний", "", "")} в гримуаре", "+ $spells spells in the grimoire"), 60f, y + 40, 36f, sans, sub); y += 70 }
        val room = bottom - y
        if (room > 220) c.pet(d.leader, Pose.CHEER, W / 2f, bottom, min(room, 440f))
        c.footer()
        return b to tr("Моя учёба в Inkling", "My study in Inkling")
    }

    private fun term(t: LongRange): Pair<Bitmap, String> {
        val (b, c) = base("${fmtDayShort(t.first)} – ${fmtDayShort(t.last)}")
        val st = Stats(d.studies, Span(t.first, t.last, ""), d.subById, d.today)
        var y = top + 60
        c.t(tr("Табель: ${Words.term(d.s)}", "Report: the ${Words.term(d.s)}"), 60f, y + 30, 76f); y += 90
        c.facts(listOf(dur(st.minutes * 60) to tr("учёбы", "of study"), "${d.spells.count { it.created in t }}" to tr("заклинаний", "spells"), "${d.tasks.count { it.doneDay in t }}" to tr("заданий", "tasks")), y, 160f)
        y += 200
        if (!d.s.self) for (s in d.active) {
            val gs = d.grades.filter { it.subjectId == s.id && it.examId == 0L && it.day in t }
            if (gs.isEmpty() || y > bottom - 120) continue
            val v = Marks.term(d.s.scale, gs, d.s.passes)!!
            c.panel(60f, y, W - 120f, 86f, 26f)
            c.t(s.name, 96f, y + 56, 36f, sans, ink, maxW = 650f)
            c.t(mark(Marks.termMark(d.s.scale, v, d.s.passes)), W - 96f, y + 58, 44f, num, accent, Paint.Align.RIGHT)
            y += 100
        }
        val room = bottom - y
        if (room > 220) { val row = d.team.ifEmpty { listOfNotNull(d.leader) }; val s = min(room, 360f)
            row.forEachIndexed { i, pt -> c.pet(pt, Pose.CHEER, W / 2f + (i - (row.size - 1) / 2f) * s * .8f, bottom, s) } }
        c.footer()
        return b to tr("Мой табель в Inkling", "My report in Inkling")
    }

    private fun year(year: Int): Pair<Bitmap, String> {
        val (b, c) = base("$year")
        val from = LocalDate.of(year, 1, 1).toEpochDay(); val to = LocalDate.of(year, 12, 31).toEpochDay()
        val st = Stats(d.studies, Span(from, to, ""), d.subById, d.today)
        var y = top + 60
        c.t(tr("Год в академии", "A year in the academy"), 60f, y + 30, 80f); y += 90
        c.facts(listOf(dur(st.minutes * 60) to tr("учёбы", "of study"), "${st.days}" to tr("дней", "days"), "${d.materials.count { it.finished in from..to }}" to tr("закончено", "finished")), y, 160f)
        y += 190
        c.facts(listOf("${d.spells.count { it.created in from..to }}" to tr("заклинаний", "spells"), "${d.pets.count { it.hatched in from..to }}" to tr("вылупилось", "hatched"),
            "${d.exams.count { it.done && it.day in from..to }}" to tr("испытаний", "trials")), y, 160f)
        y += 190
        st.bySubject().firstOrNull()?.let { cs -> c.t(tr("Главный предмет: ${cs.subject?.name ?: "–"}", "Subject of the year: ${cs.subject?.name ?: "–"}"), 60f, y + 40, 40f, sans, ink); y += 70 }
        val room = bottom - y
        if (room > 220) { val row = d.team.ifEmpty { listOfNotNull(d.leader) }; val s = min(room, 380f)
            row.forEachIndexed { i, pt -> c.pet(pt, Pose.CHEER, W / 2f + (i - (row.size - 1) / 2f) * s * .8f, bottom, s) } }
        c.footer()
        return b to tr("Мой $year в Inkling", "My $year in Inkling")
    }

    private fun material(id: Long): Pair<Bitmap, String> {
        val m = d.materials.firstOrNull { it.id == id } ?: d.finished.lastOrNull(); val (b, c) = base(tr("материал закончен", "material finished")); if (m == null) return b to ""
        c.prop("books", W / 2f, top + (if (look.story) 700 else 520), if (look.story) 560f else 420f)
        var y = top + (if (look.story) 800 else 610)
        c.t(m.title, W / 2f, y, if (m.title.length > 18) 60f else 80f, bold, ink, Paint.Align.CENTER)
        c.t(d.subById[m.subjectId]?.name.orEmpty(), W / 2f, y + 60, 38f, reg, sub, Paint.Align.CENTER)
        y += 110
        val mine = d.studies.filter { it.materialId == m.id }
        c.facts(listOf(Words.unit(m.unit, m.total.coerceAtLeast(m.done)) to tr("пройдено", "done"), dur(mine.sumOf { it.minutes } * 60) to tr("учёбы", "of study"),
            "${((if (m.finished > 0) m.finished else d.today) - m.created).coerceAtLeast(1)}" to tr("дней", "days")), y, 150f)
        c.footer()
        return b to tr("Закончен материал «${m.title}» – Inkling", "Finished «${m.title}» – Inkling")
    }

    private fun trial(id: Long): Pair<Bitmap, String> {
        val e = d.exams.firstOrNull { it.id == id } ?: d.exams.lastOrNull { it.done }; val (b, c) = base(tr("испытание пройдено", "trial passed")); if (e == null) return b to ""
        c.prop("scroll", W / 2f, top + (if (look.story) 700 else 520), if (look.story) 600f else 440f)
        var y = top + (if (look.story) 800 else 610)
        e.title.ifBlank { d.subById[e.subjectId]?.name.orEmpty() }.let { tt -> c.t(tt, W / 2f, y, if (tt.length > 18) 60f else 76f, bold, ink, Paint.Align.CENTER) }
        c.t("${Words.examKind(e.kind)} · ${fmtDay(e.day)}", W / 2f, y + 60, 38f, reg, sub, Paint.Align.CENTER)
        y += 110
        val result = if (e.score < 0) "–" else if (e.kind == Exam.OGE || e.kind == Exam.EGE || e.maxScore > 0) fmt1(e.score) else Marks.show(if (e.kind == Exam.CREDIT) "pass" else d.s.scale, e.score)
        val tk = d.tickets.filter { it.examId == e.id }
        c.facts(listOfNotNull(mark(result) to tr("результат", "result"), if (tk.isNotEmpty()) "${tk.count { it.level >= 2 }}/${tk.size}" to tr("билетов выучено", "tickets learned") else null,
            dur(d.studies.filter { it.examId == e.id || (it.subjectId == e.subjectId && it.day in (e.day - 30)..e.day) }.sumOf { it.minutes } * 60) to tr("подготовки", "of preparation")), y, 150f)
        c.footer()
        return b to tr("Испытание пройдено – Inkling", "Trial passed – Inkling")
    }

    private fun pet(id: Long): Pair<Bitmap, String> {
        val pt = d.pets.firstOrNull { it.id == id } ?: d.leader; val (b, c) = base(tr("моё существо", "my creature")); if (pt == null) return b to ""
        val sp = Pets.of(pt.species); val col = sp.color.toInt()
        val cy = top + (if (look.story) 450 else 290)
        c.drawCircle(W / 2f, cy, if (look.story) 400f else 320f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(W / 2f, cy, 420f, (col and 0xFFFFFF) or 0x88000000.toInt(), col and 0xFFFFFF, Shader.TileMode.CLAMP) })
        c.pet(pt, Pose.IDLE, W / 2f, cy + (if (look.story) 400 else 290), if (look.story) 820f else 560f)
        var y = cy + (if (look.story) 520 else 380)
        c.t(pt.name, W / 2f, y, 96f, bold, ink, Paint.Align.CENTER)
        c.t("${sp.title} · ${pt.stage().title()}" + (pt.speciality()?.let { " · " + it.title } ?: "") + if (pt.shiny) tr(" · редкий окрас", " · rare colours") else "", W / 2f, y + 64, 38f, reg, sub, Paint.Align.CENTER)
        y += 110
        val best = Stat.entries.zip(pt.stats).maxByOrNull { it.second }
        c.facts(listOf("${pt.xp}" to tr("опыта", "XP"), (best?.let { "${it.second}" } ?: "0") to (best?.first?.title?.lowercase() ?: ""), "${d.pets.count { it.hatched > 0 }}/${Pets.all.size}" to tr("в коллекции", "collected")), y, 150f)
        c.t(tr("В башне: ", "In the tower: ") + app.inkling.ui.pets.perkShort(sp), W / 2f, y + 200, 30f, reg, sub, Paint.Align.CENTER)
        c.footer()
        return b to "${pt.name} – ${sp.title}"
    }

    private fun towerCard(): Pair<Bitmap, String> {
        val (b, c) = base(tr("моя башня", "my tower"))
        val gh = (W - 120f) / 1.5f
        c.tower(RectF(60f, top + 20, W - 60f, top + 20 + gh))
        var y = top + 70 + gh
        c.t(tr("Башня · ${Words.year(d.s)}", "The tower · ${Words.year(d.s)}"), 60f, y + 40, 64f, maxW = W - 120f)
        c.t(d.team.joinToString(" · ") { "${it.name} (${Pets.of(it.species).title.lowercase()})" }, 60f, y + 100, 32f, reg, sub)
        y += 140
        c.facts(listOf("${(d.s.items - d.s.hidden).size}/${Shop.all.size}" to tr("вещей", "things"), "${d.pets.count { it.hatched > 0 }}" to tr("существ", "creatures"),
            "${d.finished.size}" to tr("корешков на полке", "spines on the shelf")), y, 150f)
        c.footer()
        return b to tr("Моя башня в Inkling", "My tower in Inkling")
    }

    private fun collection(): Pair<Bitmap, String> {
        val (b, c) = base(tr("коллекция", "collection"))
        val cols = if (look.story) 4 else 6; val cell = (W - 120f) / cols
        var y = top + 20
        c.t(tr("${d.pets.count { it.hatched > 0 }} из ${Pets.all.size}", "${d.pets.count { it.hatched > 0 }} of ${Pets.all.size}"), 60f, y + 60, 76f); y += 100
        Pets.all.forEachIndexed { i, sp ->
            val x = 60f + (i % cols) * cell; val yy = y + (i / cols) * cell * .82f
            if (yy + cell * .8f > bottom) return@forEachIndexed
            val pt = d.pets.firstOrNull { it.species == sp.id }
            val img = when { pt == null -> null; pt.hatched == 0L -> Art.egg(ctx, sp.id); else -> Art.pet(ctx, sp.id, 2) }?.asAndroidBitmap()
            c.panel(x + 6, yy + 6, cell - 12, cell * .82f - 12, 28f)
            if (img != null) c.bitmap(img, x + cell / 2, yy + cell * .74f, cell * .66f, false) else c.t("?", x + cell / 2, yy + cell * .5f, 64f, bold, sub, Paint.Align.CENTER)
        }
        c.footer()
        return b to tr("Мои существа в Inkling", "My creatures in Inkling")
    }

    private fun wand(): Pair<Bitmap, String> {
        val (b, c) = base(tr("моя палочка", "my wand"))
        val w = WandSpec(d)
        val cy = top + (if (look.story) 520 else 360); val len = (W - 200f) * (.6f + .4f * ((w.length - 18) / 20f).coerceIn(0f, 1f)); val x0 = (W - len) / 2
        val wood = w.school.woodColor.toInt(); val glow = w.school.color.toInt()
        c.save(); c.rotate(-14f, W / 2f, cy)
        c.drawCircle(x0 + len, cy, 150f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(x0 + len, cy, 150f, (glow and 0xFFFFFF) or 0xC0000000.toInt(), glow and 0xFFFFFF, Shader.TileMode.CLAMP) })
        c.drawPath(Path().apply { moveTo(x0, cy - 34f); lineTo(x0 + len, cy - 10f); lineTo(x0 + len, cy + 10f); lineTo(x0, cy + 34f); close() }, p(wood))
        c.drawRoundRect(RectF(x0 - 10f, cy - 42f, x0 + len * .26f, cy + 42f), 30f, 30f, p(wood).apply { colorFilter = android.graphics.LightingColorFilter(0xFF8A8A8A.toInt(), 0) })
        for (k in 0 until w.carvings.coerceAtMost(12)) { val x = x0 + len * (.3f + .05f * k); c.drawLine(x, cy - 26f, x, cy + 26f, p(0x66000000).apply { strokeWidth = 5f }) }
        for (k in 0 until w.runes.coerceAtMost(20)) c.drawCircle(x0 + len * (.32f + .033f * k), cy, 4f, p(0xFFFFE6A0.toInt()))
        c.drawCircle(x0 + len + 4, cy, 14f, p(0xFFFFFFFF.toInt()))
        c.restore()
        var y = cy + (if (look.story) 300 else 230)
        c.t("${w.school.wood.replaceFirstChar { it.uppercase() }}, ${coreName(w.core)}", W / 2f, y, 52f, bold, ink, Paint.Align.CENTER)
        c.t(tr("${w.length} см · ${w.flex}", "${w.length} cm · ${w.flex}"), W / 2f, y + 60, 38f, reg, sub, Paint.Align.CENTER)
        y += 110
        c.facts(listOf("${w.hours}" to tr("часов учёбы", "hours of study"), "${w.carvings}" to tr("колец резьбы", "carved rings"), "${w.runes}" to tr("рун", "runes")), y, 150f)
        c.footer()
        return b to tr("Моя палочка в Inkling", "My wand in Inkling")
    }

    private fun book(): Pair<Bitmap, String> {
        val (b, c) = base(if (d.s.school) tr("испытания", "trials") else tr("зачётка", "record book"))
        var y = top + 60
        c.t(if (d.s.school) tr("Мои испытания", "My trials") else tr("Зачётная книга мага", "A mage's record book"), 60f, y + 30, 70f); y += 90
        Marks.gpa(d.s.scale, d.exams) { id -> d.subById[id]?.credits ?: 0.0 }?.let { g -> c.t(tr("Средний балл: ${mark(fmt1(g))}", "Average: ${mark(fmt1(g))}"), 60f, y + 30, 44f, sans, accent); y += 70 }
        for (e in d.exams.filter { it.done && it.score >= 0 && it.kind !in setOf(Exam.TEST, Exam.VPR) }.sortedByDescending { it.day }) {
            if (y > bottom - 110) break
            c.panel(60f, y, W - 120f, 92f, 26f)
            c.prop("scroll", 112f, y + 82, 66f)
            c.t(e.title.ifBlank { d.subById[e.subjectId]?.name.orEmpty() }, 164f, y + 50, 36f, sans, ink, maxW = 620f)
            c.t("${Words.examKind(e.kind)} · ${fmtDayShort(e.day)}", 164f, y + 80, 24f, reg, sub)
            c.t(mark(if (e.kind == Exam.OGE || e.kind == Exam.EGE || e.maxScore > 0) fmt1(e.score) else Marks.show(if (e.kind == Exam.CREDIT) "pass" else d.s.scale, e.score)), W - 96f, y + 60, 42f, num, accent, Paint.Align.RIGHT)
            y += 106
        }
        c.footer()
        return b to tr("Моя зачётка в Inkling", "My record book in Inkling")
    }
}

object Share {
    fun send(ctx: Context, bmp: Bitmap, title: String) {
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }; val f = File(dir, "inkling.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val i = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_TEXT, title).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(i, tr("Поделиться открыткой", "Share the card")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Saves the card to Pictures/Inkling. */
    fun save(ctx: Context, bmp: Bitmap): Boolean = runCatching {
        val v = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "inkling-${System.currentTimeMillis()}.png"); put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Inkling") }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)!!
        ctx.contentResolver.openOutputStream(uri)!!.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }; true }.getOrDefault(false)
}
