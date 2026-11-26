package tw.dynity.wrongbutton.levels.s3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.FaceCover
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.LevelChrome
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.levels.LevelColumn
import tw.dynity.wrongbutton.levels.rememberResources
import tw.dynity.wrongbutton.levels.s2.S2Level
import tw.dynity.wrongbutton.levels.s2.S2Meta

/** 一次實驗：哪些開關打開（位元），哪些燈亮（位元；畫面上顯示的，可能是 The Button 說的謊話）。 */
data class Trial(val switches: Int, val lamps: Int)

/**
 * 開關與線路（第 25 章）。格子編號 = 實驗 × 燈數 + 燈。
 * [spoken] 的格子要聽 The Button 說，[lies] 是說錯（或看不到臉時寫錯）的格子。[wiring] 是真正的接線，只給測試用。
 */
data class WireQuiz(
    val switches: Int,
    val lamps: Int,
    val trials: List<Trial>,
    val target: Int,
    val answer: Int,
    val xor: Boolean,
    val spoken: Set<Int> = emptySet(),
    val lies: Set<Int> = emptySet(),
    val hiddenLies: Int = 0,
    val wiring: List<Int> = emptyList(),
) {
    fun lit(trial: Int, lamp: Int) = trials[trial].lamps shr lamp and 1 == 1
}

@Serializable
data class WireState(val on: Int = 0, val heard: Set<Int> = emptySet())

abstract class WireLevel(meta: S2Meta, val quiz: WireQuiz, private val cover: FaceCover = FaceCover.None, private val subtle: Boolean = false) : S2Level(meta) {
    override val chrome: LevelChrome get() = LevelChrome(speechBubble = false)
    override val ambientChatter: Boolean get() = quiz.spoken.isEmpty()

    @Composable
    override fun Content(scope: LevelScope) = WireField(scope, quiz, cover, subtle)
}

private const val SWITCH_NAMES = "ABCDE"

@Composable
fun WireField(scope: LevelScope, q: WireQuiz, cover: FaceCover, subtle: Boolean) {
    val res = rememberResources()
    var state by rememberLevelState(WireState.serializer()) { WireState() }
    fun names(bits: Int) = (0 until q.switches).filter { bits shr it and 1 == 1 }.joinToString("、") { SWITCH_NAMES[it].toString() }

    LevelColumn(spacing = 10.dp) { compact ->
        S3Face(scope, if (compact) 56.dp else 72.dp, cover)
        RuleCard(
            res.getString(R.string.ch25_rule_intro, q.switches, q.lamps),
            res.getString(if (q.xor) R.string.ch25_rule_xor else R.string.ch25_rule_or),
            *(if (q.spoken.isNotEmpty()) arrayOf(res.getString(R.string.ch25_rule_spoken)) else emptyArray()),
            *(if (q.hiddenLies > 0) arrayOf(res.getString(R.string.ch25_rule_hidden, q.hiddenLies)) else emptyArray()),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
            q.trials.forEachIndexed { k, t ->
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(res.getString(R.string.ch25_trial, k + 1, names(t.switches)), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).testTag("wq_trial_${k + 1}"))
                    repeat(q.lamps) { j ->
                        val cell = k * q.lamps + j
                        val spoken = cell in q.spoken
                        val heard = cell in state.heard
                        val on = q.lit(k, j)
                        val desc = res.getString(R.string.ch25_cell_cd, k + 1, j + 1) + when {
                            spoken && !heard -> res.getString(R.string.ch25_cell_ask)
                            else -> res.getString(if (on) R.string.ch25_on else R.string.ch25_off) + if (spoken) res.getString(R.string.ch25_said) else ""
                        }
                        Bulb(
                            label = "${j + 1}",
                            on = if (spoken && !heard) null else on,
                            spoken = spoken,
                            modifier = Modifier
                                .clickable(enabled = spoken, role = Role.Button) {
                                    state = state.copy(heard = state.heard + cell)
                                    scope.claim(res.getString(if (on) R.string.ch25_says_on else R.string.ch25_says_off, k + 1, j + 1), truth = cell !in q.lies, subtle = subtle)
                                }
                                .semantics { contentDescription = desc }
                                .testTag("wq_cell_${k + 1}_${j + 1}"),
                        )
                    }
                }
            }
        }
        // 目標
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
            val lit = (0 until q.lamps).filter { q.target shr it and 1 == 1 }.joinToString("、") { "${it + 1}" }
            Text(res.getString(R.string.ch25_target, lit), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).testTag("wq_target"))
            repeat(q.lamps) { j -> Bulb("${j + 1}", q.target shr j and 1 == 1, false, Modifier) }
        }
        Text(stringResource(R.string.ch25_your_switches), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(q.switches) { i ->
                val on = state.on shr i and 1 == 1
                val desc = res.getString(R.string.ch25_switch_cd, SWITCH_NAMES[i].toString(), res.getString(if (on) R.string.ch25_switch_on else R.string.ch25_switch_off))
                Box(
                    Modifier
                        .size(52.dp, 64.dp)
                        .background(if (on) Color(0xFF43A047) else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable(role = Role.Switch) { scope.cue(Cue.Tap); state = state.copy(on = state.on xor (1 shl i)) }
                        .semantics { contentDescription = desc; selected = on }
                        .testTag("wq_sw_${SWITCH_NAMES[i]}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(SWITCH_NAMES[i].toString(), fontWeight = FontWeight.Bold, color = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(res.getString(if (on) R.string.ch25_switch_on else R.string.ch25_switch_off), style = MaterialTheme.typography.labelSmall, color = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Button(
            onClick = {
                when {
                    q.spoken.any { it !in state.heard } -> scope.say(Line.Res(R.string.ch25_listen_first), Mood.Normal)
                    state.on == q.answer -> { scope.cue(Cue.Success); scope.complete() }
                    else -> scope.mistake("wq_wrong", Line.Res(R.string.ch25_wrong), Mood.Smug)
                }
            },
            modifier = Modifier.heightIn(min = 48.dp).testTag("wq_power"),
        ) { Text(stringResource(R.string.ch25_power)) }
    }
}

/** 一顆燈泡：亮（黃）、暗（灰）、還沒聽（橘色問號）。 */
@Composable
private fun Bulb(label: String, on: Boolean?, spoken: Boolean, modifier: Modifier) {
    Box(
        modifier
            .size(34.dp)
            .background(
                when (on) {
                    null -> AskColor
                    true -> Color(0xFFFFD54F)
                    false -> Color(0xFF9E9E9E)
                },
                CircleShape,
            )
            .border(if (spoken) 2.dp else 0.dp, if (spoken) AskTextColor else Color.Transparent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (on == null) "?" else label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (on == null) AskTextColor else Color(0xFF212121))
    }
}

// ---- 關卡 ----

object Level381Wires : WireLevel(Ch25Meta.l381, Ch25Puzzles.l381)
object Level382Another : WireLevel(Ch25Meta.l382, Ch25Puzzles.l382)
object Level383FourSwitches : WireLevel(Ch25Meta.l383, Ch25Puzzles.l383)
object Level384FourLamps : WireLevel(Ch25Meta.l384, Ch25Puzzles.l384)
object Level385ItSays : WireLevel(Ch25Meta.l385, Ch25Puzzles.l385)
object Level386Toggle : WireLevel(Ch25Meta.l386, Ch25Puzzles.l386)
object Level387ToggleMore : WireLevel(Ch25Meta.l387, Ch25Puzzles.l387)
object Level388ToggleFour : WireLevel(Ch25Meta.l388, Ch25Puzzles.l388)
object Level389FiveSwitches : WireLevel(Ch25Meta.l389, Ch25Puzzles.l389)
object Level390Sunglasses : WireLevel(Ch25Meta.l390, Ch25Puzzles.l390, cover = FaceCover.Eyes, subtle = true)
object Level391ToggleLie : WireLevel(Ch25Meta.l391, Ch25Puzzles.l391)
object Level392FiveByFive : WireLevel(Ch25Meta.l392, Ch25Puzzles.l392)
object Level393ToggleFive : WireLevel(Ch25Meta.l393, Ch25Puzzles.l393)
object Level394OneWrong : WireLevel(Ch25Meta.l394, Ch25Puzzles.l394)
object Level395ToggleWrong : WireLevel(Ch25Meta.l395, Ch25Puzzles.l395)
object Level396Mask : WireLevel(Ch25Meta.l396, Ch25Puzzles.l396, cover = FaceCover.Mouth, subtle = true)
object Level397BigToggle : WireLevel(Ch25Meta.l397, Ch25Puzzles.l397)
object Level398ToggleLies : WireLevel(Ch25Meta.l398, Ch25Puzzles.l398, subtle = true)
object Level399Blackout : WireLevel(Ch25Meta.l399, Ch25Puzzles.l399)
object Level400Electrician : WireLevel(Ch25Meta.l400, Ch25Puzzles.l400)

val Chapter25 = listOf(
    Level381Wires, Level382Another, Level383FourSwitches, Level384FourLamps, Level385ItSays,
    Level386Toggle, Level387ToggleMore, Level388ToggleFour, Level389FiveSwitches, Level390Sunglasses,
    Level391ToggleLie, Level392FiveByFive, Level393ToggleFive, Level394OneWrong, Level395ToggleWrong,
    Level396Mask, Level397BigToggle, Level398ToggleLies, Level399Blackout, Level400Electrician,
)
