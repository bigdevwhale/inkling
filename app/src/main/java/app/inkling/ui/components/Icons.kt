package app.inkling.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Line icons drawn on a 24-unit grid. */
enum class Icons { BACK, PLUS, CHECK, CLOSE, GEAR, TIMER, SHARE, TRASH, DOTS, GYM, LOG, PAW, STATS, EDIT, SEARCH, PLATE, STAR, NEXT, UP, DOWN, CAMERA, SAVE, FLAME, TROPHY, SCALE, LIST, SWAP,
    CAL, HEART, ARM, ROUTE, BOLT, CLOCK, SPARK, SUN, EGG, HOME, TARGET, PALETTE, GLOBE,
    WALLET, CARD, COIN, GIFT, JAR, SNOW, ACORN, BARREL, ROCK, DAM, BELL, MINUS, BANK, ATTIC, PIGGY, FILTER,
    TOWER, BOOK, WAND, CANDLE, SCROLL, QUESTION, MOON, CAP, PEN, BAG, MIC, EYE, LEAF, FLASK, GEAR2, RUNE, HOURGLASS, SCALES, MASK, SHIELD, SIGMA, CROSS, CIRCLE }

@Composable
fun Icon(i: Icons, color: Color, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { icon(i, color) }
}

fun DrawScope.icon(i: Icons, c: Color) {
    val k = size.width / 24f
    val s = Stroke(2f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun o(x: Float, y: Float) = Offset(x * k, y * k)
    fun l(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(c, o(x1, y1), o(x2, y2), 2f * k, StrokeCap.Round)
    fun p(vararg pts: Float, close: Boolean = false, fill: Boolean = false) {
        val path = Path().apply { moveTo(pts[0] * k, pts[1] * k); var j = 2; while (j < pts.size) { lineTo(pts[j] * k, pts[j + 1] * k); j += 2 }; if (close) close() }
        if (fill) drawPath(path, c) else drawPath(path, c, style = s)
    }
    fun circle(x: Float, y: Float, r: Float, fill: Boolean = false) = if (fill) drawCircle(c, r * k, o(x, y)) else drawCircle(c, r * k, o(x, y), style = s)
    fun rr(x: Float, y: Float, w: Float, h: Float, r: Float, fill: Boolean = false) = if (fill) drawRoundRect(c, o(x, y), Size(w * k, h * k), CornerRadius(r * k))
        else drawRoundRect(c, o(x, y), Size(w * k, h * k), CornerRadius(r * k), style = s)
    when (i) {
        Icons.BACK -> p(15f, 5f, 8f, 12f, 15f, 19f)
        Icons.NEXT -> p(9f, 5f, 16f, 12f, 9f, 19f)
        Icons.UP -> p(6f, 15f, 12f, 9f, 18f, 15f)
        Icons.DOWN -> p(6f, 9f, 12f, 15f, 18f, 9f)
        Icons.PLUS -> { l(12f, 5f, 12f, 19f); l(5f, 12f, 19f, 12f) }
        Icons.CHECK -> p(5f, 12.5f, 10f, 17.5f, 19f, 7f)
        Icons.CLOSE -> { l(6f, 6f, 18f, 18f); l(18f, 6f, 6f, 18f) }
        Icons.GEAR -> { circle(12f, 12f, 3.2f); for (a in 0 until 8) { val r = Math.toRadians(a * 45.0)
            l(12 + 6f * Math.cos(r).toFloat(), 12 + 6f * Math.sin(r).toFloat(), 12 + 8.6f * Math.cos(r).toFloat(), 12 + 8.6f * Math.sin(r).toFloat()) }; circle(12f, 12f, 6.2f) }
        Icons.TIMER -> { circle(12f, 13.5f, 7.5f); l(12f, 13.5f, 12f, 9.5f); l(10f, 3.5f, 14f, 3.5f); l(18f, 7f, 19.5f, 5.5f) }
        Icons.SHARE -> { l(12f, 4f, 12f, 15f); p(8f, 8f, 12f, 4f, 16f, 8f); p(6f, 12f, 6f, 20f, 18f, 20f, 18f, 12f) }
        Icons.SAVE -> { l(12f, 4f, 12f, 15f); p(8f, 11f, 12f, 15f, 16f, 11f); l(5f, 20f, 19f, 20f) }
        Icons.TRASH -> { l(5f, 7f, 19f, 7f); p(7f, 7f, 8f, 20f, 16f, 20f, 17f, 7f); l(10f, 4f, 14f, 4f) }
        Icons.DOTS -> { circle(6f, 12f, 1.6f, true); circle(12f, 12f, 1.6f, true); circle(18f, 12f, 1.6f, true) }
        Icons.GYM -> { l(7f, 12f, 17f, 12f); rr(3.5f, 8f, 3.5f, 8f, 1.2f); rr(17f, 8f, 3.5f, 8f, 1.2f); l(2f, 12f, 3.5f, 12f); l(20.5f, 12f, 22f, 12f) }
        Icons.LOG -> { rr(5f, 3.5f, 14f, 17f, 2.5f); l(8.5f, 9f, 15.5f, 9f); l(8.5f, 13f, 15.5f, 13f); l(8.5f, 17f, 12.5f, 17f) }
        Icons.LIST -> { l(9f, 7f, 19f, 7f); l(9f, 12f, 19f, 12f); l(9f, 17f, 19f, 17f); circle(5f, 7f, 1.2f, true); circle(5f, 12f, 1.2f, true); circle(5f, 17f, 1.2f, true) }
        Icons.PAW -> { drawOval(c, o(7.5f, 12f), Size(9f * k, 8f * k)); circle(5.5f, 10f, 2f, true); circle(9.5f, 6.5f, 2f, true); circle(14.5f, 6.5f, 2f, true); circle(18.5f, 10f, 2f, true) }
        Icons.STATS -> { l(5f, 19f, 5f, 13f); l(10f, 19f, 10f, 8f); l(15f, 19f, 15f, 11f); l(20f, 19f, 20f, 5f) }
        Icons.EDIT -> { p(5f, 19f, 6f, 15f, 15.5f, 5.5f, 18.5f, 8.5f, 9f, 18f, close = true) }
        Icons.SEARCH -> { circle(10.5f, 10.5f, 6f); l(15f, 15f, 20f, 20f) }
        Icons.PLATE -> { circle(12f, 12f, 8.5f); circle(12f, 12f, 4f); circle(12f, 12f, 1.2f, true) }
        Icons.STAR -> p(12f, 3.5f, 14.6f, 9f, 20.5f, 9.6f, 16f, 13.6f, 17.3f, 19.5f, 12f, 16.5f, 6.7f, 19.5f, 8f, 13.6f, 3.5f, 9.6f, 9.4f, 9f, close = true, fill = true)
        Icons.CAMERA -> { rr(3.5f, 7f, 17f, 12.5f, 2.5f); circle(12f, 13f, 3.5f); p(8.5f, 7f, 10f, 4.5f, 14f, 4.5f, 15.5f, 7f) }
        Icons.FLAME -> p(12f, 3f, 16.5f, 9f, 17.5f, 14f, 15f, 19f, 12f, 20.5f, 9f, 19f, 6.5f, 14.5f, 8f, 10f, 10f, 12f, 12f, 3f, close = true)
        Icons.TROPHY -> { p(7f, 4f, 17f, 4f, 16.5f, 10f, 12f, 14f, 7.5f, 10f, close = true); l(12f, 14f, 12f, 18f); l(8f, 20f, 16f, 20f)
            drawArc(c, 90f, 180f, false, o(3.5f, 5f), Size(4f * k, 5f * k), style = s); drawArc(c, -90f, 180f, false, o(16.5f, 5f), Size(4f * k, 5f * k), style = s) }
        Icons.SCALE -> { rr(4f, 4f, 16f, 16f, 4f); drawArc(c, 200f, 140f, false, Offset(8f * k, 7f * k), Size(8f * k, 8f * k), style = s); l(12f, 11f, 13.5f, 8.5f) }
        Icons.CAL -> { rr(4f, 5.5f, 16f, 14.5f, 2.5f); l(4f, 10f, 20f, 10f); l(8.5f, 3.5f, 8.5f, 7f); l(15.5f, 3.5f, 15.5f, 7f); circle(9f, 14.5f, 1f, true); circle(15f, 14.5f, 1f, true) }
        Icons.HEART -> p(12f, 19.5f, 4.8f, 12.5f, 4f, 8.5f, 6.2f, 5.6f, 9.4f, 5.2f, 12f, 8f, 14.6f, 5.2f, 17.8f, 5.6f, 20f, 8.5f, 19.2f, 12.5f, close = true)
        Icons.ARM -> { p(5f, 19f, 5f, 12f, 8f, 7f, 11f, 5f, 13f, 7f, 11f, 9.5f, 12f, 11f, 15f, 9.5f, 18.5f, 11f, 19.5f, 15f, 17f, 19f, close = true) }
        Icons.ROUTE -> { circle(6f, 18f, 2f); circle(18f, 6f, 2f); p(8f, 18f, 15f, 18f, 15f, 12f, 9f, 12f, 9f, 6f, 16f, 6f) }
        Icons.BOLT -> p(13f, 3f, 5.5f, 13.5f, 11.5f, 13.5f, 10.5f, 21f, 18.5f, 10f, 12.5f, 10f, close = true)
        Icons.CLOCK -> { circle(12f, 12f, 8.5f); p(12f, 7f, 12f, 12f, 15.5f, 14f) }
        Icons.SPARK -> { l(12f, 3f, 12f, 8f); l(12f, 16f, 12f, 21f); l(3f, 12f, 8f, 12f); l(16f, 12f, 21f, 12f); l(6f, 6f, 8.5f, 8.5f); l(15.5f, 15.5f, 18f, 18f); l(18f, 6f, 15.5f, 8.5f); l(8.5f, 15.5f, 6f, 18f) }
        Icons.SUN -> { circle(12f, 12f, 4f); for (a in 0 until 8) { val r = Math.toRadians(a * 45.0)
            l(12 + 6.5f * Math.cos(r).toFloat(), 12 + 6.5f * Math.sin(r).toFloat(), 12 + 8.8f * Math.cos(r).toFloat(), 12 + 8.8f * Math.sin(r).toFloat()) } }
        Icons.EGG -> drawOval(c, o(6f, 3.5f), Size(12f * k, 17f * k), style = s)
        Icons.HOME -> { p(4f, 11f, 12f, 4f, 20f, 11f); p(6.5f, 9.5f, 6.5f, 20f, 17.5f, 20f, 17.5f, 9.5f); rr(10f, 14f, 4f, 6f, 1f) }
        Icons.TARGET -> { circle(12f, 12f, 8.5f); circle(12f, 12f, 4.5f); circle(12f, 12f, 1.3f, true) }
        Icons.PALETTE -> { circle(12f, 12f, 8.5f); circle(8.5f, 10f, 1.3f, true); circle(12f, 7.5f, 1.3f, true); circle(15.5f, 10f, 1.3f, true); circle(14f, 15.5f, 2f) }
        Icons.GLOBE -> { circle(12f, 12f, 8.5f); l(3.5f, 12f, 20.5f, 12f); drawOval(c, o(8f, 3.5f), Size(8f * k, 17f * k), style = s) }
        Icons.SWAP -> { p(7f, 4f, 4f, 7f, 7f, 10f); l(4f, 7f, 18f, 7f); p(17f, 14f, 20f, 17f, 17f, 20f); l(20f, 17f, 6f, 17f) }
        Icons.WALLET -> { rr(3.5f, 6.5f, 17f, 13f, 2.5f); p(5f, 6.5f, 15f, 3.5f, 16.5f, 6.5f); rr(14f, 11f, 6.5f, 4f, 1.5f); circle(16.5f, 13f, .8f, true) }
        Icons.CARD -> { rr(3f, 6f, 18f, 12f, 2.5f); l(3f, 10f, 21f, 10f); l(6.5f, 14.5f, 10f, 14.5f) }
        Icons.COIN -> { circle(12f, 12f, 8.5f); l(12f, 7.5f, 12f, 16.5f); p(14.5f, 9.5f, 11f, 9.5f, 9.5f, 11f, 11f, 12.3f, 13f, 12.3f, 14.5f, 13.6f, 13f, 15f, 9.5f, 15f) }
        Icons.GIFT -> { rr(4f, 9f, 16f, 4f, 1f); p(5.5f, 13f, 5.5f, 20f, 18.5f, 20f, 18.5f, 13f); l(12f, 9f, 12f, 20f)
            p(12f, 9f, 9f, 5f, 7f, 6.5f, 8.5f, 9f); p(12f, 9f, 15f, 5f, 17f, 6.5f, 15.5f, 9f) }
        Icons.JAR -> { p(7f, 7.5f, 6f, 10f, 6f, 18.5f, 7.5f, 20f, 16.5f, 20f, 18f, 18.5f, 18f, 10f, 17f, 7.5f); rr(6.5f, 4f, 11f, 3.5f, 1f); l(8f, 13f, 16f, 13f) }
        Icons.SNOW -> { l(12f, 3f, 12f, 21f); l(4.2f, 7.5f, 19.8f, 16.5f); l(4.2f, 16.5f, 19.8f, 7.5f); p(10f, 4.5f, 12f, 6.5f, 14f, 4.5f); p(10f, 19.5f, 12f, 17.5f, 14f, 19.5f) }
        Icons.ACORN -> { p(5f, 10f, 19f, 10f); drawArc(c, 180f, 180f, false, o(5f, 4.5f), Size(14f * k, 11f * k), style = s)
            p(6.5f, 10f, 7f, 14f, 9.5f, 18.5f, 12f, 20.5f, 14.5f, 18.5f, 17f, 14f, 17.5f, 10f); l(12f, 2.5f, 12f, 4.5f) }
        Icons.BARREL -> { p(7f, 4f, 17f, 4f, 19f, 12f, 17f, 20f, 7f, 20f, 5f, 12f, close = true); l(5.6f, 8.5f, 18.4f, 8.5f); l(5.6f, 15.5f, 18.4f, 15.5f) }
        Icons.ROCK -> p(3.5f, 18.5f, 5f, 12f, 9f, 7f, 14f, 6f, 18.5f, 9f, 20.5f, 14f, 20f, 18.5f, close = true)
        Icons.DAM -> { p(3f, 19f, 6f, 8f, 18f, 8f, 21f, 19f, close = true); l(4.6f, 13.5f, 19.4f, 13.5f); l(10f, 8f, 9f, 13.5f); l(14.5f, 13.5f, 15.5f, 19f); p(2f, 5f, 5f, 4f, 8f, 5f, 11f, 4f) }
        Icons.BELL -> { p(6f, 17f, 6f, 11f, 7.5f, 7f, 12f, 5f, 16.5f, 7f, 18f, 11f, 18f, 17f); l(4.5f, 17f, 19.5f, 17f); l(10.5f, 20f, 13.5f, 20f); l(12f, 3f, 12f, 5f) }
        Icons.MINUS -> l(5f, 12f, 19f, 12f)
        Icons.BANK -> { p(3.5f, 9.5f, 12f, 4f, 20.5f, 9.5f, close = true); l(6f, 11.5f, 6f, 17f); l(10f, 11.5f, 10f, 17f); l(14f, 11.5f, 14f, 17f); l(18f, 11.5f, 18f, 17f); l(3.5f, 20f, 20.5f, 20f) }
        Icons.ATTIC -> { p(3f, 13f, 12f, 4f, 21f, 13f); p(6f, 11f, 6f, 20f, 18f, 20f, 18f, 11f); circle(12f, 13.5f, 2.2f) }
        Icons.PIGGY -> { drawOval(c, o(3.5f, 7f), Size(15f * k, 11.5f * k), style = s); p(18.5f, 11f, 20.5f, 10.5f, 20.5f, 14f, 18.3f, 14f); l(7f, 18f, 7f, 20.5f); l(14f, 18f, 14f, 20.5f); l(9f, 7.3f, 12f, 4.5f); circle(15f, 10.5f, .9f, true) }
        Icons.TOWER -> { p(6f, 21f, 6f, 9f, 4.5f, 9f, 4.5f, 5f, 7f, 5f, 7f, 7f, 9.5f, 7f, 9.5f, 5f, 14.5f, 5f, 14.5f, 7f, 17f, 7f, 17f, 5f, 19.5f, 5f, 19.5f, 9f, 18f, 9f, 18f, 21f, close = true); p(10f, 21f, 10f, 16f, 12f, 14f, 14f, 16f, 14f, 21f) }
        Icons.BOOK -> { p(12f, 6.5f, 8f, 5f, 3.5f, 5.5f, 3.5f, 18.5f, 8f, 18f, 12f, 19.5f, 16f, 18f, 20.5f, 18.5f, 20.5f, 5.5f, 16f, 5f, 12f, 6.5f, 12f, 19.5f) }
        Icons.WAND -> { l(4f, 20f, 15f, 9f); p(17f, 3f, 17.6f, 5.4f, 20f, 6f, 17.6f, 6.6f, 17f, 9f, 16.4f, 6.6f, 14f, 6f, 16.4f, 5.4f, close = true); circle(20f, 11f, .8f, true); circle(12f, 4f, .8f, true) }
        Icons.CANDLE -> { rr(9f, 10f, 6f, 10.5f, 1.2f); l(12f, 10f, 12f, 8.5f); p(12f, 3f, 14f, 6f, 12f, 8f, 10f, 6f, close = true, fill = true); l(7f, 20.5f, 17f, 20.5f) }
        Icons.SCROLL -> { rr(5f, 5f, 12f, 14f, 1.5f); circle(17f, 7f, 2f); circle(7f, 19f, 2f); l(8.5f, 9.5f, 14f, 9.5f); l(8.5f, 13f, 14f, 13f) }
        Icons.QUESTION -> { drawArc(c, 200f, 230f, false, o(8f, 4f), Size(8f * k, 8f * k), style = s); p(14.8f, 10.5f, 12f, 12.8f, 12f, 15f); circle(12f, 19f, 1.1f, true) }
        Icons.MOON -> drawArc(c, 60f, 250f, false, o(4.5f, 4.5f), Size(15f * k, 15f * k), style = s).also { p(15.5f, 5.2f, 12f, 8f, 11f, 12f, 12.5f, 16f, 16.5f, 18.6f) }
        Icons.CAP -> { p(2.5f, 9.5f, 12f, 5f, 21.5f, 9.5f, 12f, 14f, close = true); p(6.5f, 11.5f, 6.5f, 16f, 12f, 18.5f, 17.5f, 16f, 17.5f, 11.5f); l(21.5f, 9.5f, 21.5f, 15f) }
        Icons.PEN -> { p(4f, 20f, 5f, 15.5f, 15.5f, 5f, 19f, 8.5f, 8.5f, 19f, close = true); l(13.5f, 7f, 17f, 10.5f) }
        Icons.BAG -> { rr(3.5f, 8f, 17f, 12f, 2.5f); p(9f, 8f, 9f, 5.5f, 15f, 5.5f, 15f, 8f); l(3.5f, 13f, 20.5f, 13f); rr(10.5f, 11.5f, 3f, 3f, .8f) }
        Icons.MIC -> { rr(9f, 3.5f, 6f, 11f, 3f); drawArc(c, 0f, 180f, false, o(5.5f, 7f), Size(13f * k, 11f * k), style = s); l(12f, 18f, 12f, 21f) }
        Icons.EYE -> { p(2.5f, 12f, 7f, 6.5f, 12f, 5f, 17f, 6.5f, 21.5f, 12f, 17f, 17.5f, 12f, 19f, 7f, 17.5f, close = true); circle(12f, 12f, 3f) }
        Icons.LEAF -> { p(5f, 19f, 5f, 12f, 8f, 7f, 13f, 5f, 19.5f, 4.5f, 19f, 11f, 16f, 16f, 11f, 19f, close = true); l(5f, 19f, 14f, 10f) }
        Icons.FLASK -> { p(9.5f, 3.5f, 9.5f, 9f, 4.5f, 18f, 5.5f, 20f, 18.5f, 20f, 19.5f, 18f, 14.5f, 9f, 14.5f, 3.5f); l(8f, 3.5f, 16f, 3.5f); l(7f, 14f, 17f, 14f) }
        Icons.GEAR2 -> { circle(12f, 12f, 3f); for (a in 0 until 6) { val r = Math.toRadians(a * 60.0); l(12 + 5f * Math.cos(r).toFloat(), 12 + 5f * Math.sin(r).toFloat(), 12 + 8.5f * Math.cos(r).toFloat(), 12 + 8.5f * Math.sin(r).toFloat()) }; circle(12f, 12f, 5.5f) }
        Icons.RUNE -> { l(8f, 3.5f, 8f, 20.5f); p(8f, 4f, 16f, 8f, 8f, 12f); l(8f, 12f, 16.5f, 20.5f) }
        Icons.HOURGLASS -> { l(6f, 3.5f, 18f, 3.5f); l(6f, 20.5f, 18f, 20.5f); p(7.5f, 3.5f, 7.5f, 7f, 12f, 12f, 16.5f, 7f, 16.5f, 3.5f); p(7.5f, 20.5f, 7.5f, 17f, 12f, 12f, 16.5f, 17f, 16.5f, 20.5f) }
        Icons.SCALES -> { l(12f, 3.5f, 12f, 20f); l(8f, 20.5f, 16f, 20.5f); l(4f, 7f, 20f, 7f); p(4f, 7f, 2f, 13f, 6f, 13f, close = true); p(20f, 7f, 18f, 13f, 22f, 13f, close = true) }
        Icons.MASK -> { p(3.5f, 6f, 12f, 8f, 20.5f, 6f, 20f, 13f, 16f, 18f, 12f, 19f, 8f, 18f, 4f, 13f, close = true); circle(8.5f, 11.5f, 1.3f, true); circle(15.5f, 11.5f, 1.3f, true); drawArc(c, 20f, 140f, false, o(9f, 12f), Size(6f * k, 4f * k), style = s) }
        Icons.SHIELD -> { p(12f, 3f, 19.5f, 6f, 19.5f, 11.5f, 17f, 17f, 12f, 21f, 7f, 17f, 4.5f, 11.5f, 4.5f, 6f, close = true); p(9f, 12f, 11.5f, 14.5f, 15.5f, 9.5f) }
        Icons.SIGMA -> p(17.5f, 6.5f, 17.5f, 4f, 6.5f, 4f, 12.5f, 12f, 6.5f, 20f, 17.5f, 20f, 17.5f, 17.5f)
        Icons.CROSS -> p(9.5f, 3.5f, 14.5f, 3.5f, 14.5f, 9.5f, 20.5f, 9.5f, 20.5f, 14.5f, 14.5f, 14.5f, 14.5f, 20.5f, 9.5f, 20.5f, 9.5f, 14.5f, 3.5f, 14.5f, 3.5f, 9.5f, 9.5f, 9.5f, close = true)
        Icons.CIRCLE -> { circle(12f, 12f, 8f); p(12f, 6.5f, 13.4f, 10.5f, 17.5f, 10.6f, 14.2f, 13.1f, 15.4f, 17.2f, 12f, 14.8f, 8.6f, 17.2f, 9.8f, 13.1f, 6.5f, 10.6f, 10.6f, 10.5f, close = true) }
        Icons.FILTER -> p(4f, 5f, 20f, 5f, 14f, 12.5f, 14f, 19f, 10f, 17f, 10f, 12.5f, close = true)
    }
}
