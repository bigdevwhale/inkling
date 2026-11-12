package app.inkling.ui.pets

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.inkling.container
import app.inkling.data.Pet
import app.inkling.domain.Pets
import app.inkling.domain.Stage
import app.inkling.domain.Species
import app.inkling.domain.fmtDayYear
import app.inkling.domain.stage
import app.inkling.domain.title
import app.inkling.domain.toNext
import app.inkling.domain.tr
import app.inkling.domain.Perk
import app.inkling.ui.Data
import app.inkling.ui.Nav
import app.inkling.ui.Screen
import app.inkling.ui.art.Creature
import app.inkling.ui.art.Mini
import app.inkling.ui.art.PetView
import app.inkling.ui.art.Pose
import app.inkling.ui.components.Btn
import app.inkling.ui.components.Card
import app.inkling.ui.components.Field
import app.inkling.ui.components.Ghost
import app.inkling.ui.components.Hint
import app.inkling.ui.components.Icons
import app.inkling.ui.components.Meter
import app.inkling.ui.components.Page
import app.inkling.ui.components.Popup
import app.inkling.ui.components.Row2
import app.inkling.ui.components.Section
import app.inkling.ui.components.card
import app.inkling.ui.share.CardKind
import app.inkling.ui.share.ShareReq
import app.inkling.ui.theme.C
import app.inkling.ui.theme.T

@Composable
fun PetsScreen(d: Data, nav: Nav) {
    var ask by remember { mutableStateOf<Species?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Bottom) {
            Text(tr("Существа", "Creatures"), style = T.title, modifier = Modifier.weight(1f))
            Text("${d.pets.count { it.hatched > 0 }} / ${Pets.all.size}", style = T.h3.copy(color = C.mute))
        }
        Section(tr("Живут в башне", "In the tower"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            (0..2).forEach { slot ->
                val p = d.pets.firstOrNull { it.slot == slot }
                Column(Modifier.weight(1f).card(18.dp).clickable(enabled = p != null) { p?.let { nav.push(Screen.PetInfo(it.id)) } }.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (slot == 0) tr("Вожак", "Leader") else tr("За партой", "At a desk"), style = T.tiny)
                    if (p != null) { PetView(p, 84.dp, Pose.IDLE); Text(p.name, style = T.smallB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(perkShort(Pets.of(p.species)), style = T.tiny, maxLines = 2, textAlign = TextAlign.Center, minLines = 2) }
                    else { Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) { Text("?", style = T.title.copy(color = C.line)) }; Text(tr("свободно", "free"), style = T.small) }
                }
            }
        }
        Section(tr("Коллекция", "Collection"))
        Pets.all.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { sp ->
                    val p = d.pets.firstOrNull { it.species == sp.id }
                    Column(Modifier.weight(1f).card(16.dp, if (sp.rare && p == null) C.goldSoft else null).clickable { if (p != null) nav.push(Screen.PetInfo(p.id)) else ask = sp }.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        when {
                            p == null -> { Mini(sp.id, 58.dp, dim = true); Text(sp.title, style = T.tiny, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            p.hatched == 0L -> { PetView(p, 58.dp); Meter(p.growth.toFloat() / Pets.HATCH, Modifier.padding(top = 4.dp, bottom = 3.dp), h = 5.dp) }
                            else -> { PetView(p, 58.dp); Text(p.name, style = T.tiny.copy(color = C.ink), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
    ask?.let { sp -> Popup({ ask = null }) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Mini(sp.id, 120.dp, dim = true) }
        Text(sp.title, style = T.h2, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center)
        Text(sp.perkText, style = T.body.copy(color = C.mute), modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
        Text(tr("Как найти", "How to find"), style = T.sec, modifier = Modifier.padding(top = 14.dp))
        Text(sp.find, style = T.bodyB, modifier = Modifier.padding(top = 4.dp))
        if (!sp.rare) Text(tr("А ещё яйцо можно купить на ярмарке академии за ману.", "An egg can also be bought with mana at the academy fair."), style = T.small, modifier = Modifier.padding(top = 6.dp))
        Btn(tr("Понятно", "Got it"), Modifier.fillMaxWidth().padding(top = 14.dp)) { ask = null }
    } }
}

fun perkShort(sp: Species): String = when (sp.perk) {
    Perk.SPELL -> tr("×2 за заклинания", "×2 for spells"); Perk.RECALL -> tr("×2 за загадки и билеты", "×2 for riddles and tickets")
    Perk.FOCUS -> tr("×2 за свечи без отвлечений", "×2 for undisturbed candles"); Perk.DAYS -> tr("×2 за первое занятие дня", "×2 for the day's first study")
    Perk.SHORT -> tr("×2 за короткие занятия", "×2 for short study"); Perk.MIX -> tr("×2 за второй предмет дня", "×2 for a day's second subject")
    Perk.UNITS -> tr("×2 за пройденное в материалах", "×2 for progress in materials"); Perk.EVENING -> tr("×2 вечером", "×2 in the evening")
    Perk.MORNING -> tr("×2 утром", "×2 in the morning"); Perk.FINISH -> tr("×2 за законченное", "×2 for finishing")
    Perk.QUESTION -> tr("×2 за вопросы", "×2 for questions"); Perk.READ -> tr("×2 за страницы", "×2 for pages")
    Perk.TEACH -> tr("×2 за объяснения", "×2 for explaining"); Perk.TASK -> tr("×2 за сданные задания", "×2 for tasks handed in")
    Perk.QUEST -> tr("×2 за задания недели", "×2 for weekly quests"); Perk.COMEBACK -> tr("×3 за возвращение", "×3 for coming back")
    Perk.DEADLINE -> tr("×2 за сделанное заранее", "×2 for tasks done early"); Perk.ATTEND -> tr("×2 за посещённые занятия", "×2 for classes attended")
    Perk.PLAN -> tr("×2 за порядок и портфель", "×2 for planning and the satchel"); Perk.GRADE -> tr("×2 за оценки", "×2 for marks")
    Perk.REST -> tr("мана за перемены", "mana for breaks"); Perk.ALL -> tr("+25% ко всему", "+25% to everything")
}
