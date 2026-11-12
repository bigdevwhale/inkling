package app.inkling.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.inkling.BuildConfig
import app.inkling.data.Pet
import app.inkling.domain.Shop
import kotlin.math.roundToInt

/**
 * The tower room: the generated room, the window onto the real sky over the academy, the things bought,
 * a shelf of spines – one per finished material, in its school's colour – and the creatures:
 * the leader on the rug, the two others by the walls.
 */
@Composable
fun TowerScene(team: List<Pet>, items: Set<String>, pose: Pose, modifier: Modifier = Modifier, preview: String? = null, sky: Sky? = null,
               spines: List<Color> = emptyList(), candle: Boolean = false, onPet: ((Pet) -> Unit)? = null) {
    val ctx = LocalContext.current
    val tt by rememberInfiniteTransition(label = "sky").animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "t")
    val t = if (BuildConfig.SHOTS) .3f else tt
    val bg = Art.get(ctx, "tower_bg")
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1.5f)) {
        val wDp = maxWidth; val hDp = maxHeight
        Canvas(Modifier.fillMaxSize()) {
            val r = android.graphics.RectF(0f, 0f, size.width, size.height)
            val sk = sky ?: Sky(1f, 0f, app.inkling.data.Weather.CLEAR)
            // the sky goes first, under the picture: the window glass is cut out of it
            drawIntoCanvas { drawSky(it.nativeCanvas, r, sk, t) }
            if (bg != null) drawImage(bg, IntOffset.Zero, IntSize(bg.width, bg.height), IntOffset.Zero, IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.Medium)
            else {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF6A4A6A), Color(0xFF4A3050))), size = size.copy(height = size.height * .66f))
                drawRect(Color(0xFF6A4026), Offset(0f, size.height * .66f), size.copy(height = size.height * .34f))
            }
            // the shelf of finished materials: a plank and a row of spines
            if (spines.isNotEmpty()) {
                val x0 = size.width * .67f; val x1 = size.width * .83f; val y = size.height * .3f
                val sw = (x1 - x0) / 10f
                spines.take(10).forEachIndexed { i, col ->
                    val hh = size.height * (.075f + ((i * 37) % 5) * .006f)
                    drawRect(col, Offset(x0 + i * sw + sw * .08f, y - hh), Size(sw * .84f, hh))
                    drawRect(Color(0x55FFE6A0), Offset(x0 + i * sw + sw * .08f, y - hh * .78f), Size(sw * .84f, hh * .06f))
                }
                drawRect(Color(0xFF5A3A22), Offset(x0 - sw * .4f, y), Size(x1 - x0 + sw * .8f, size.height * .014f))
            }
            val shown = Shop.all.filter { it.id in items || it.id == preview }.sortedBy { if (it.wall) 0 else 1 }
            for (it in shown) {
                val img = Art.item(ctx, it.id) ?: continue
                val w = size.width * it.w; val h = w * img.height / img.width
                val x = size.width * it.x - w / 2; val y = if (it.wall) size.height * it.y - h / 2 else size.height * it.y - h
                drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(x.roundToInt(), y.roundToInt()), IntSize(w.roundToInt(), h.roundToInt()),
                    alpha = if (it.id == preview && it.id !in items) .85f else 1f, filterQuality = FilterQuality.Medium)
            }
            if (candle) Art.prop(ctx, "candle")?.let { img ->
                val w = size.width * .045f; val h = w * img.height / img.width
                drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset((size.width * .62f).roundToInt(), (size.height * .985f - h).roundToInt()), IntSize(w.roundToInt(), h.roundToInt()), filterQuality = FilterQuality.Medium)
            }
            drawIntoCanvas { drawNight(it.nativeCanvas, r, sk, candle) }
        }
        val tap = remember { MutableInteractionSource() }
        team.filter { it.slot in 1..2 }.forEach { p ->
            key(p.id) {
                val s = hDp * .3f
                Box(Modifier.offset(x = wDp * (if (p.slot == 1) .2f else .66f), y = hDp * .97f - s)
                    .then(if (onPet != null) Modifier.clickable(tap, null) { onPet(p) } else Modifier)) { PetView(p, s, if (pose == Pose.WORK) Pose.IDLE else pose) }
            }
        }
        team.firstOrNull { it.slot == 0 }?.let { lead -> key(lead.id) { val s = hDp * .44f
            Box(Modifier.offset(x = (wDp - s) / 2, y = hDp * .99f - s).then(if (onPet != null) Modifier.clickable(tap, null) { onPet(lead) } else Modifier)) { PetView(lead, s, pose) } } }
    }
}
