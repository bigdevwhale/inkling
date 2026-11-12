package app.inkling.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.inkling.R

/** Colour tokens: parchment by day, indigo night by night; ink-blue for action, candle gold for mana, sage for things done. */
class Pal(
    val dark: Boolean, val bg: Color, val card: Color, val card2: Color, val ink: Color, val mute: Color, val line: Color, val track: Color,
    val acc: Color, val accInk: Color, val accSoft: Color, val ok: Color, val okSoft: Color, val gold: Color, val goldSoft: Color, val bad: Color,
    val heat: List<Color>, val nav: Color, val shadow: Color, val mana: Color,
)

private fun c(hex: Long) = Color(0xFF000000 or hex)

val Light = Pal(false, c(0xF3ECDD), c(0xFFFBF2), c(0xEDE3D0), c(0x231D33), c(0x7D7389), c(0xE3D8C4), c(0xE8DECB),
    c(0x4B3FA0), c(0xFFFFFF), c(0xE3DEF5), c(0x4F8A6B), c(0xDCEDE2), c(0xC8901E), c(0xF7E8C5), c(0xC4533A),
    listOf(c(0xE8DECB), c(0xD6CFF0), c(0xA99BE0), c(0x7563C8), c(0x4B3FA0)), c(0xFFF8EC), Color(0x22403070), c(0x3E8FE0))

val Dark = Pal(true, c(0x15122A), c(0x211C3A), c(0x2A2447), c(0xF1EAF9), c(0xA79FBD), c(0x352E55), c(0x2F2950),
    c(0xB4A2FF), c(0x1A1433), c(0x3A3170), c(0x7CC79A), c(0x22382F), c(0xF0C060), c(0x3D3220), c(0xEE7558),
    listOf(c(0x2F2950), c(0x433A7A), c(0x6250B0), c(0x8E78E8), c(0xC4B4FF)), c(0x1B1733), Color(0x66000000), c(0x7CC4FF))

/** The palette in use; switched by the theme setting before the tree is drawn. */
object C {
    @Volatile var p: Pal = Light
    val dark get() = p.dark
    val bg get() = p.bg; val card get() = p.card; val card2 get() = p.card2; val ink get() = p.ink; val mute get() = p.mute
    val line get() = p.line; val track get() = p.track; val acc get() = p.acc; val accInk get() = p.accInk; val accSoft get() = p.accSoft
    val ok get() = p.ok; val okSoft get() = p.okSoft; val gold get() = p.gold; val goldSoft get() = p.goldSoft; val bad get() = p.bad
    val heat get() = p.heat; val nav get() = p.nav; val shadow get() = p.shadow; val mana get() = p.mana
}

val Lora = FontFamily(Font(R.font.lora_regular, FontWeight.Normal), Font(R.font.lora_medium, FontWeight.Medium), Font(R.font.lora_medium, FontWeight.Bold))
val Nunito = FontFamily(Font(R.font.nunito_regular, FontWeight.Normal), Font(R.font.nunito_semibold, FontWeight.SemiBold), Font(R.font.nunito_bold, FontWeight.Bold), Font(R.font.nunito_bold, FontWeight.ExtraBold))
val Inter = FontFamily(Font(R.font.inter_regular, FontWeight.Normal), Font(R.font.inter_medium, FontWeight.Medium), Font(R.font.inter_semibold, FontWeight.SemiBold), Font(R.font.inter_semibold, FontWeight.Bold))

object T {
    val title get() = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Medium, fontSize = 27.sp, lineHeight = 32.sp, color = C.ink)
    val h2 get() = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 25.sp, color = C.ink)
    val h3 get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp, color = C.ink)
    val body get() = TextStyle(fontFamily = Nunito, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val bodyB get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val small get() = TextStyle(fontFamily = Nunito, fontSize = 13.sp, lineHeight = 17.sp, color = C.mute)
    val smallB get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 17.sp, color = C.ink)
    val tiny get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, color = C.mute)
    val sec get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.sp, color = C.mute)
    val num get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = C.ink)
    val numS get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = C.ink)
    val big get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp, color = C.ink)
    val btn get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
}

@Composable
fun InkTheme(dark: Boolean, content: @Composable () -> Unit) {
    C.p = if (dark) Dark else Light
    val p = C.p
    val scheme = if (dark) darkColorScheme(primary = p.acc, background = p.bg, surface = p.card, onSurface = p.ink, onBackground = p.ink)
    else lightColorScheme(primary = p.acc, background = p.bg, surface = p.card, onSurface = p.ink, onBackground = p.ink)
    MaterialTheme(colorScheme = scheme, content = content)
}
