package app.inkling.ui.magic

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.inkling.BuildConfig
import app.inkling.container
import app.inkling.data.Material
import app.inkling.domain.Words
import app.inkling.domain.tr
import app.inkling.notify.Notes
import app.inkling.ui.Data
import app.inkling.ui.Nav
import app.inkling.ui.art.Creature
import app.inkling.ui.art.PetView
import app.inkling.ui.art.Pose
import app.inkling.ui.components.Btn
import app.inkling.ui.components.Chip
import app.inkling.ui.components.Field
import app.inkling.ui.components.Icon
import app.inkling.ui.components.Icons
import app.inkling.ui.components.Page
import app.inkling.ui.components.SubjectPicker
import app.inkling.ui.components.subjectColor
import app.inkling.ui.theme.C
import app.inkling.ui.theme.T
import kotlinx.coroutines.delay
import kotlin.math.sin

/** The candle: choose what to study, light it, put the phone face down; when it has burnt down – what was done, then a break. */
@Composable
fun CandlePage(d: Data, nav: Nav) {
    val cd = d.s.candle
    when {
        cd.start > 0 -> Burning(d, nav)
        cd.restUntil > 0 -> Rest(d, nav)
        else -> Setup(d, nav)
    }
}

@Composable
private fun Setup(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val ctx = LocalContext.current
    var sub by remember { mutableLongStateOf(d.studies.firstOrNull()?.subjectId?.takeIf { id -> d.active.any { it.id == id } } ?: d.active.firstOrNull()?.id ?: 0) }
    androidx.compose.runtime.LaunchedEffect(d.active.size) { if (sub == 0L) sub = d.active.firstOrNull()?.id ?: 0L }
    var mat by remember { mutableLongStateOf(0) }
    var task by remember { mutableLongStateOf(0) }
    var exam by remember { mutableLongStateOf(0) }
    var plan by remember { mutableIntStateOf(d.s.focus) }
    Page(tr("Свеча", "The candle"), nav::pop) {
        Row(verticalAlignment = Alignment.Bottom) {
            d.leader?.let { PetView(it, 110.dp, Pose.WORK) }
            Text(tr("Положите телефон экраном вниз – круг замкнётся.", "Put the phone face down – the circle closes."),
                style = T.small, modifier = Modifier.weight(1f).padding(start = 8.dp, bottom = 10.dp))
        }
        Text(tr("Предмет", "Subject"), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
        SubjectPicker(d.active, sub) { sub = it; mat = 0; task = 0; exam = 0 }
        val mats = d.materials.filter { it.subjectId == sub && it.finished == 0L && !it.archived }
        val tasks = d.openTasks.filter { it.subjectId == sub }
        val exams = d.upcoming.filter { it.subjectId == sub }
        if (mats.isNotEmpty() || tasks.isNotEmpty() || exams.isNotEmpty()) {
            Text(tr("Над чем", "Working on"), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                mats.forEach { m -> Pick("books", m.title, "${m.done}/${m.total.takeIf { it > 0 } ?: "?"} · ${Words.unitName(m.unit).lowercase()}", mat == m.id) { mat = if (mat == m.id) 0 else m.id; task = 0; exam = 0 } }
                tasks.forEach { t -> Pick("scroll", t.title, Words.taskKind(t.kind), task == t.id) { task = if (task == t.id) 0 else t.id; mat = 0; exam = 0 } }
                exams.forEach { e -> Pick("medal", e.title.ifBlank { Words.examKind(e.kind) }, Words.examKind(e.kind), exam == e.id) { exam = if (exam == e.id) 0 else e.id; mat = 0; task = 0 } }
            }
        }
        Text(tr("Сколько горит, мин", "How long, min"), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(10, 15, 25, 45, 60, 90).forEach { m -> Chip("$m", plan == m, Modifier.weight(1f)) { plan = m } }
        }
        Spacer(Modifier.height(20.dp))
        Btn(tr("Зажечь свечу", "Light the candle"), Modifier.fillMaxWidth(), enabled = sub != 0L) {
            c.write { light(sub, mat, task, exam, plan) }
            Notes.candle(ctx, d.subById[sub]?.name.orEmpty(), System.currentTimeMillis() + plan * 60_000L)
        }
        if (d.active.isEmpty()) Text(tr("Сначала добавьте предмет – на вкладке «Академия».", "Add a subject first – on the «Academy» tab."), style = T.small.copy(color = C.bad), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun Pick(prop: String, title: String, sub: String, on: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (on) C.accSoft else C.card).clickable(onClick = onClick).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        app.inkling.ui.art.Prop(prop, 28.dp); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) { Text(title, style = T.bodyB, maxLines = 1); Text(sub, style = T.tiny) }
        if (on) Icon(Icons.CHECK, C.acc, 18.dp)
    }
}

/** The candle burning: dark room, the flame, the time left, the creature at work. */
@Composable
private fun Burning(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val ctx = LocalContext.current
    val cd = d.s.candle
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val total = cd.plan * 60_000L
    val left = (cd.start + total - now).coerceAtLeast(0)
    var finish by remember { mutableStateOf(false) }
    LaunchedEffect(left == 0L) { if (left == 0L) finish = true }
    val down = cd.downSince > 0
    // keep the screen on while it burns
    val view = LocalView.current
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }
    // face down closes the circle
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var was = false
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) { val isDown = e.values[2] < -7f
                if (isDown != was) { was = isDown; c.write { candleDown(isDown) } } }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (acc != null) sm.registerListener(l, acc, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sm.unregisterListener(l) }
    }
    // leaving the app while it burns (not face down) counts as looking away
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP && System.currentTimeMillis() < cd.start + total) c.write { candleAway() } }
        owner.lifecycle.addObserver(o); onDispose { owner.lifecycle.removeObserver(o) }
    }
    BackHandler { nav.pop() }
    val sub = d.subById[cd.subjectId]
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0E0B22), Color(0xFF1E1840), Color(0xFF2A1E2E))))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).clickable { nav.pop() }, contentAlignment = Alignment.Center) { Icon(Icons.BACK, Color(0xFFE8E0FF), 22.dp) }
                Text(sub?.name.orEmpty(), style = T.h2.copy(color = Color(0xFFF1EAF9)), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.size(40.dp))
            }
            Text(listOfNotNull(d.materials.firstOrNull { it.id == cd.materialId }?.title, d.tasks.firstOrNull { it.id == cd.taskId }?.title,
                d.exams.firstOrNull { it.id == cd.examId }?.let { it.title.ifBlank { Words.examKind(it.kind) } }).joinToString(), style = T.small.copy(color = Color(0xFFA79FBD)))
            Spacer(Modifier.weight(.4f))
            CandleArt((left.toFloat() / total).coerceIn(0f, 1f), subjectColor(sub), Modifier.size(150.dp, 230.dp))
            Text("%d:%02d".format(left / 60_000, left / 1000 % 60), style = T.big.copy(color = Color(0xFFFFF1C2), fontSize = 54.sp), modifier = Modifier.padding(top = 10.dp))
            Text(when {
                down -> tr("Круг замкнут. Ничто не отвлекает", "The circle is closed. Nothing distracts")
                cd.away > 0 -> tr("Пламя дрогнуло ${cd.away} раз – ничего, продолжаем", "The flame flickered ${cd.away} times – never mind, carry on")
                else -> tr("Положите телефон экраном вниз", "Put the phone face down")
            }, style = T.body.copy(color = Color(0xFFCFC4F0)), textAlign = TextAlign.Center)
            Spacer(Modifier.weight(.6f))
            d.leader?.let { PetView(it, 120.dp, Pose.WORK) }
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0x22FFFFFF)).clickable { c.write { snuff() }; Notes.stopCandle(ctx); nav.pop() }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Text(tr("Задуть", "Blow out"), style = T.btn.copy(color = Color(0xFFCFC4F0))) }
                Box(Modifier.weight(1.4f).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF0C060)).clickable { finish = true }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Text(if (left == 0L) tr("Записать урок", "Write it down") else tr("Закончить раньше", "Finish early"), style = T.btn.copy(color = Color(0xFF231D33))) }
            }
        }
    }
    if (finish) FinishSheet(d, onCancel = { if (left > 0) finish = false }) { Notes.stopCandle(ctx) }
}

/** What was done during the candle: pages or lessons of the material, a note and what stuck. */
@Composable
private fun FinishSheet(d: Data, onCancel: () -> Unit, onSaved: () -> Unit) {
    val c = LocalContext.current.container
    val cd = d.s.candle
    val mat = d.materials.firstOrNull { it.id == cd.materialId }
    var units by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }
    var spell by remember { mutableStateOf("") }
    app.inkling.ui.components.Sheet(onCancel) {
        Text(tr("Урок окончен", "The lesson is over"), style = T.h2)
        Spacer(Modifier.height(10.dp))
        if (mat != null) {
            Text(tr("Сколько прошли: ", "How far: ") + Words.unitName(mat.unit).lowercase(), style = T.sec, modifier = Modifier.padding(bottom = 6.dp))
            Stepper(units, if (mat.unit == Material.PAGES) 5 else 1) { units = it }
        }
        Field(note, { note = it }, tr("Что делали (по желанию)", "What you did (optional)"), modifier = Modifier.padding(top = 10.dp))
        Text(tr("Что запомнили?", "What stuck?"), style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        Field(spell, { spell = it }, tr("Тема для гримуара – сфинкс спросит завтра", "A topic for the grimoire – the sphinx asks tomorrow"))
        Spacer(Modifier.height(14.dp))
        Btn(tr("Записать и отдохнуть", "Save and rest"), Modifier.fillMaxWidth()) {
            c.write { burn(units, note.trim()); if (spell.isNotBlank()) addSpell(cd.subjectId, spell, "") }
            onSaved()
        }
    }
}

/** − n + with a step. */
@Composable
fun Stepper(v: Int, step: Int, onSet: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(C.card2).clickable { onSet((v - step).coerceAtLeast(0)) }, contentAlignment = Alignment.Center) { Icon(Icons.MINUS, C.ink, 20.dp) }
        Text("$v", style = T.num, modifier = Modifier.width(70.dp), textAlign = TextAlign.Center)
        Box(Modifier.size(44.dp).clip(CircleShape).background(C.card2).clickable { onSet(v + step) }, contentAlignment = Alignment.Center) { Icon(Icons.PLUS, C.ink, 20.dp) }
        Spacer(Modifier.width(8.dp))
        if (step > 1) Box(Modifier.size(44.dp).clip(CircleShape).background(C.card2).clickable { onSet(v + 1) }, contentAlignment = Alignment.Center) { Text("+1", style = T.smallB) }
    }
}

/** The break: the cat stretches; going back too soon is fine, but a real break is worth mana. */
@Composable
private fun Rest(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val left = (d.s.candle.restUntil - now).coerceAtLeast(0)
    val rested = left < d.s.rest * 60_000L * .2f
    Page(tr("Перемена", "Break"), nav::pop) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Creature("cat", app.inkling.domain.Stage.ADULT, Pose.WORK, 180.dp) }
        Text("%d:%02d".format(left / 60_000, left / 1000 % 60), style = T.big, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(tr("Встаньте, потянитесь, посмотрите в окно.", "Stand up, stretch, look out of the window."),
            style = T.body.copy(color = C.mute), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        Spacer(Modifier.height(20.dp))
        Btn(if (rested) tr("Перемена окончена", "Break is over") else tr("Пропустить перемену", "Skip the break"), Modifier.fillMaxWidth()) { c.write { endRest(rested) }; nav.pop() }
        Spacer(Modifier.height(8.dp))
        app.inkling.ui.components.Ghost(tr("Ещё одну свечу", "Another candle"), Modifier.fillMaxWidth()) { c.write { endRest(rested) } }
    }
}

/** A candle burning down: [left] 1 full … 0 gone; the flame flickers, wax pools in the school's colour. */
@Composable
fun CandleArt(left: Float, wax: Color, modifier: Modifier = Modifier) {
    val t by rememberInfiniteTransition(label = "flame").animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val time = if (BuildConfig.SHOTS) .3f else t
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val base = h * .95f
        val bodyH = h * (.12f + .5f * left)
        val bw = w * .26f
        val top = base - bodyH
        // the glow
        drawCircle(Brush.radialGradient(listOf(Color(0x66FFC060), Color.Transparent), Offset(w / 2, top - h * .1f), w * .7f), w * .7f, Offset(w / 2, top - h * .1f))
        // the holder
        drawOval(Color(0xFFB08A3A), Offset(w * .15f, base - h * .03f), Size(w * .7f, h * .07f))
        // the candle and its drips
        drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFFE8DCC8), Color(0xFFFFF8EA), Color(0xFFD8CCB4)), w / 2 - bw / 2, w / 2 + bw / 2), Offset(w / 2 - bw / 2, top), Size(bw, bodyH),
            androidx.compose.ui.geometry.CornerRadius(w * .03f))
        drawRoundRect(wax.copy(alpha = .55f), Offset(w / 2 - bw / 2, top), Size(bw, h * .03f), androidx.compose.ui.geometry.CornerRadius(w * .02f))
        drawRoundRect(wax.copy(alpha = .45f), Offset(w / 2 - bw / 2 + bw * .1f, top), Size(bw * .14f, h * .09f), androidx.compose.ui.geometry.CornerRadius(w * .04f))
        drawRoundRect(wax.copy(alpha = .4f), Offset(w / 2 + bw * .2f, top), Size(bw * .12f, h * .05f), androidx.compose.ui.geometry.CornerRadius(w * .04f))
        // the wick and the flame
        drawLine(Color(0xFF3A2A20), Offset(w / 2, top), Offset(w / 2, top - h * .03f), w * .015f)
        val fl = sin(time * 6.283f) * .06f + sin(time * 18.8f) * .03f
        val fh = h * (.16f + fl * .5f); val fw = w * (.11f - fl * .2f)
        val fy = top - h * .025f
        val p = Path().apply {
            moveTo(w / 2 + fl * w * .2f, fy - fh)
            cubicTo(w / 2 + fw, fy - fh * .45f, w / 2 + fw, fy, w / 2, fy)
            cubicTo(w / 2 - fw, fy, w / 2 - fw, fy - fh * .45f, w / 2 + fl * w * .2f, fy - fh)
            close()
        }
        drawPath(p, Brush.verticalGradient(listOf(Color(0xFFFFF6D0), Color(0xFFFFC040), Color(0xFFE06020)), fy - fh, fy))
        drawCircle(Color(0xFF6A8AFF).copy(alpha = .6f), w * .025f, Offset(w / 2, fy - h * .015f))
    }
}
