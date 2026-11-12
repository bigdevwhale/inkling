package app.inkling.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.inkling.ui.art.Art
import app.inkling.ui.theme.C
import app.inkling.ui.theme.Lora
import app.inkling.ui.theme.T
import kotlin.math.roundToInt

/** Parchment: a warm card with a thin gilded edge, for the chronicle and the academy's records. */
fun Modifier.parchment(r: Dp = 18.dp) = this.shadow(5.dp, RoundedCornerShape(r), ambientColor = C.shadow, spotColor = C.shadow).clip(RoundedCornerShape(r))
    .background(Brush.verticalGradient(if (C.dark) listOf(Color(0xFF2A2342), Color(0xFF231D38)) else listOf(Color(0xFFFFF8E8), Color(0xFFF8EBCF))))
    .border(1.dp, C.gold.copy(alpha = if (C.dark) .28f else .45f), RoundedCornerShape(r))

/**
 * A painted scene at the top of a tab: the picture cropped to the card, darkening towards the bottom,
 * the title written over it. [content] may add things over the picture (top end).
 */
@Composable
fun SceneHeader(art: String, title: String, sub: String, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val ctx = LocalContext.current
    val img = Art.get(ctx, art)
    Box(modifier.fillMaxWidth().aspectRatio(2.4f).shadow(8.dp, RoundedCornerShape(22.dp), ambientColor = C.shadow, spotColor = C.shadow).clip(RoundedCornerShape(22.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            if (img == null) { drawRect(Brush.verticalGradient(listOf(Color(0xFF2A2350), Color(0xFF15122A)))); return@Canvas }
            // cover: fill the card, crop the sides
            val s = maxOf(size.width / img.width, size.height / img.height)
            val w = img.width * s; val h = img.height * s
            drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(((size.width - w) / 2).roundToInt(), ((size.height - h) / 2).roundToInt()),
                IntSize(w.roundToInt(), h.roundToInt()), filterQuality = FilterQuality.Medium)
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x22100A20), Color(0xCC100A20)), 0f, size.height), Offset.Zero, Size(size.width, size.height))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 12.dp)) {
            val glow = Shadow(Color(0xAA000000), Offset(0f, 2f), 8f)
            Text(title, style = T.title.copy(color = Color(0xFFFFF4DC), shadow = glow))
            if (sub.isNotBlank()) Text(sub, style = T.small.copy(color = Color(0xE6F3E6CC), shadow = glow))
        }
        content()
    }
}

/** A section title in the academy's hand: a star, serif words and a gilded rule running out to the edge. */
@Composable
fun Ornate(text: String, modifier: Modifier = Modifier, action: String? = null, icon: Icons? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("✦", style = T.small.copy(color = C.gold, fontSize = 12.sp))
        Spacer(Modifier.width(7.dp))
        Text(text, style = T.h3.copy(fontFamily = Lora, color = C.ink, fontSize = 17.sp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(C.gold.copy(alpha = .55f), Color.Transparent))))
        if (action != null && onAction != null) Text(action, style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable(onClick = onAction).padding(start = 8.dp, top = 4.dp, bottom = 4.dp))
    }
}
