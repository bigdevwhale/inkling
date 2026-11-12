package app.inkling.ui.art

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import app.inkling.data.Outside
import app.inkling.data.Settings
import app.inkling.data.Sun
import app.inkling.data.Weather
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * What the tower window shows: [day] 0 night .. 1 full day, [glow] 0..1 how much dawn or dusk colours it,
 * and the weather (CLEAR when unknown).
 */
data class Sky(val day: Float, val glow: Float, val weather: Weather) {
    val night get() = day < .25f

    companion object {
        /** The sky now in the user's town: the real weather and the sun by its rise and set there. */
        fun of(s: Settings, now: Long = System.currentTimeMillis()): Sky {
            val o = Outside.of(s.outside)?.takeIf { now - it.at < 6 * 3600_000L }
            val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val (rise, set) = when {
                o != null && o.sunrise in today..(today + 86400_000) -> o.sunrise to o.sunset
                !s.lat.isNaN() -> Sun.riseSet(s.lat, s.lon, now)
                else -> (today + 7 * 3600_000L) to (today + 19 * 3600_000L)
            }
            val h = 3600_000f
            // light rises over the hour around sunrise and falls over the hour around sunset
            val day = min(((now - rise) / h + .5f).coerceIn(0f, 1f), ((set - now) / h + .5f).coerceIn(0f, 1f))
            val glow = max(1f - abs(now - rise) / (1.2f * h), 1f - abs(now - set) / (1.2f * h)).coerceIn(0f, 1f)
            return Sky(day, glow, o?.weather ?: Weather.CLEAR)
        }
    }
}

/** The arched window of the tower picture, as parts of its width and height. */
private const val WX0 = .0443f; private const val WX1 = .1702f; private const val WY0 = .194f; private const val WY1 = .6055f

private fun mix(a: Int, b: Int, t: Float): Int {
    fun ch(c: Int, s: Int) = (c shr s) and 0xFF
    fun m(s: Int) = (ch(a, s) + (ch(b, s) - ch(a, s)) * t.coerceIn(0f, 1f)).toInt()
    return (0xFF shl 24) or (m(16) shl 16) or (m(8) shl 8) or m(0)
}

/**
 * Paints the view out of the arched window of a tower picture drawn in [r]: the real sky over the academy's
 * other towers, lit windows at night. [t] 0..1 loops for rain, snow and twinkling.
 */
fun drawSky(c: Canvas, r: RectF, sky: Sky, t: Float) {
    val w = r.width(); val h = r.height()
    val pane = RectF(r.left + w * WX0 - 2, r.top + h * WY0 - 2, r.left + w * WX1 + 2, r.top + h * WY1 + 2)
    val x0 = pane.left; val y0 = pane.top; val pw = pane.width(); val ph = pane.height()
    val grey = sky.weather == Weather.OVERCAST || sky.weather == Weather.RAIN || sky.weather == Weather.STORM || sky.weather == Weather.FOG || sky.weather == Weather.SNOW
    var top = mix(0xFF0A0B26.toInt(), 0xFF4F8FE0.toInt(), sky.day); var bottom = mix(0xFF26205A.toInt(), 0xFFB5DAF5.toInt(), sky.day)
    if (sky.glow > 0) { top = mix(top, 0xFF5A4C9C.toInt(), sky.glow * .6f); bottom = mix(bottom, 0xFFF59A62.toInt(), sky.glow * .8f) }
    if (grey) { top = mix(top, mix(0xFF22263A.toInt(), 0xFF8C96A2.toInt(), sky.day), .75f); bottom = mix(bottom, mix(0xFF2E3242.toInt(), 0xFFB3BBC4.toInt(), sky.day), .75f) }
    val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    c.save()
    c.clipRect(pane)
    c.drawRect(pane, Paint().apply { shader = LinearGradient(0f, y0, 0f, y0 + ph, top, bottom, Shader.TileMode.CLAMP) })
    if (sky.night && !grey) {
        dot.color = 0xFFFFF6D8.toInt()
        for (k in 0 until 14) { val tw = .5f + .5f * sin((t * 2 * PI + k * 1.7).toFloat()); dot.alpha = (110 + 140 * tw).toInt()
            c.drawCircle(x0 + pw * ((k * 37) % 100) / 100f, y0 + ph * ((k * 53) % 60) / 100f + ph * .04f, w * .0016f, dot) }
        dot.color = 0xFFFFF1C2.toInt(); dot.alpha = 255; c.drawCircle(x0 + pw * .66f, y0 + ph * .22f, pw * .13f, dot)
        dot.color = top; c.drawCircle(x0 + pw * .66f + pw * .06f, y0 + ph * .2f, pw * .11f, dot)
    }
    if (!sky.night && sky.weather == Weather.CLEAR) { dot.color = 0xFFFFE89A.toInt(); dot.alpha = 255; c.drawCircle(x0 + pw * .32f, y0 + ph * .22f, pw * .13f, dot) }
    if (sky.weather == Weather.CLOUDY || sky.weather == Weather.CLEAR || grey && sky.weather != Weather.FOG) {
        dot.color = if (grey) mix(0xFF39404E.toInt(), 0xFFD5DAE0.toInt(), sky.day) else if (sky.night) 0xFF3A3F66.toInt() else 0xFFFFFFFF.toInt(); dot.alpha = 220
        for (k in 0 until (if (grey) 3 else 1)) { val cx = x0 + pw * ((.1f + t * .5f + k * .4f) % 1.3f - .15f); val cy = y0 + ph * (.3f + k * .1f)
            c.drawCircle(cx, cy, pw * .1f, dot); c.drawCircle(cx + pw * .12f, cy - pw * .05f, pw * .12f, dot); c.drawCircle(cx + pw * .24f, cy, pw * .09f, dot) }
    }
    // the academy across the valley: towers and roofs, windows lit at dusk and night
    val dim = 1f - (1f - sky.day) * .6f
    val far = mix(0xFF141436.toInt(), 0xFF6A6A9A.toInt(), dim); val near = mix(0xFF0E0E26.toInt(), 0xFF4A4A72.toInt(), dim)
    val pf = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = far }
    fun tower(cx: Float, top: Float, tw: Float, p: Paint) {
        val l = x0 + pw * (cx - tw / 2); val rr = x0 + pw * (cx + tw / 2); val tp = y0 + ph * top
        c.drawRect(l, tp, rr, y0 + ph, p)
        c.drawPath(android.graphics.Path().apply { moveTo(l - pw * .02f, tp); lineTo(x0 + pw * cx, tp - ph * .1f); lineTo(rr + pw * .02f, tp); close() }, p)
    }
    tower(.15f, .62f, .16f, pf); tower(.5f, .55f, .2f, pf); tower(.86f, .66f, .14f, pf)
    c.drawRect(x0, y0 + ph * .74f, x0 + pw, y0 + ph, pf)
    val pn = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = near }
    c.drawPath(android.graphics.Path().apply { moveTo(x0, y0 + ph * .86f); cubicTo(x0 + pw * .3f, y0 + ph * .8f, x0 + pw * .7f, y0 + ph * .84f, x0 + pw, y0 + ph * .82f); lineTo(x0 + pw, y0 + ph); lineTo(x0, y0 + ph); close() }, pn)
    if (sky.day < .7f) {
        dot.color = 0xFFFFC870.toInt(); dot.alpha = ((1 - sky.day) * 255).toInt().coerceIn(0, 255)
        val lit = listOf(.13f to .68f, .17f to .76f, .47f to .6f, .53f to .66f, .5f to .74f, .84f to .72f, .88f to .78f)
        for ((k, p) in lit.withIndex()) { if ((k + (t * 3).toInt()) % 7 == 0 && k % 3 == 0) continue
            c.drawRect(x0 + pw * p.first, y0 + ph * p.second, x0 + pw * (p.first + .025f), y0 + ph * (p.second + .035f), dot) }
    }
    if (sky.weather == Weather.SNOW) {
        dot.color = 0xFFFFFFFF.toInt(); dot.alpha = 230
        for (k in 0 until 16) { val fx2 = ((k * 0.173f) % 1f) + .03f * sin((t * 6 + k).toFloat()); val fy2 = ((t + k * .1f) % 1f)
            c.drawCircle(x0 + pw * fx2, y0 - ph * .05f + ph * 1.1f * fy2, w * .002f, dot) }
    }
    if (sky.weather == Weather.RAIN || sky.weather == Weather.STORM) {
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99C8D8EE.toInt(); strokeWidth = w * .0012f }
        for (k in 0 until 12) { val fx2 = ((k * 0.137f) % 1f); val fy2 = ((t * 3 + k * .23f) % 1f)
            val x = x0 + pw * fx2; val y = y0 - ph * .1f + ph * 1.2f * fy2; c.drawLine(x, y, x - pw * .04f, y + ph * .06f, line) }
        if (sky.weather == Weather.STORM && t % .5f < .025f) { dot.color = 0xFFFFFFFF.toInt(); dot.alpha = 160; c.drawRect(pane, dot) }
    }
    if (sky.weather == Weather.FOG) { dot.color = 0xFFE6E9EE.toInt(); dot.alpha = 150; c.drawRect(pane, dot) }
    c.restore()
}

/** The tower darkens at night (over the room and its things, under the creatures); the sconces and the candle keep warm pools. */
fun drawNight(c: Canvas, r: RectF, sky: Sky, candle: Boolean) {
    val w = r.width(); val h = r.height()
    val dark = ((1 - sky.day) * .32f * 255).toInt()
    if (dark > 4) c.drawRect(r, Paint().apply { color = (dark shl 24) or 0x0C0A26 })
    fun glow(x: Float, y: Float, rad: Float, a: Float) {
        val al = (a * 255).toInt().coerceIn(0, 255); if (al <= 2) return
        c.drawCircle(r.left + w * x, r.top + h * y, w * rad, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(r.left + w * x, r.top + h * y, w * rad, (al shl 24) or 0xFFC060, 0x00FFC060, Shader.TileMode.CLAMP) })
    }
    glow(.254f, .36f, .14f, .12f + (1 - sky.day) * .2f)
    glow(.81f, .36f, .14f, .12f + (1 - sky.day) * .2f)
    if (candle) glow(.5f, .78f, .3f, .32f)
}
