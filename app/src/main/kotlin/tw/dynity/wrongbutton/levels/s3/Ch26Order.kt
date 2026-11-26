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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

/**
 * 排順序（第 26 章）。[colors] 是正確答案：由左到右每一格是哪一顆按鈕；[drinks] 不為 null 時，每一顆還買了一種飲料。
 * [spoken] 的線索要聽 The Button 說，[lies] 是假的線索。[hiddenLies] > 0：看不到臉，只知道剛好幾條是假的。
 */
data class OrderQuiz(
    val n: Int,
    val clues: List<Int>,
    val colors: List<Int>,
    val drinks: List<Int>? = null,
    val spoken: Set<Int> = emptySet(),
    val lies: Set<Int> = emptySet(),
    val hiddenLies: Int = 0,
)

@Serializable
data class OrderState(val colors: List<Int> = emptyList(), val drinks: List<Int> = emptyList(), val heard: Set<Int> = emptySet())

abstract class OrderLevel(meta: S2Meta, val quiz: OrderQuiz, private val cover: FaceCover = FaceCover.None, private val subtle: Boolean = false) : S2Level(meta) {
    override val chrome: LevelChrome get() = LevelChrome(speechBubble = false)
    override val ambientChatter: Boolean get() = quiz.spoken.isEmpty()

    @Composable
    override fun Content(scope: LevelScope) = OrderField(scope, quiz, cover, subtle)
}

@Composable
fun OrderField(scope: LevelScope, q: OrderQuiz, cover: FaceCover, subtle: Boolean) {
    val res = rememberResources()
    val names = res.getStringArray(R.array.ch26_names)
    val drinkNames = res.getStringArray(R.array.ch26_drinks)
    var state by rememberLevelState(OrderState.serializer()) { OrderState(List(q.n) { -1 }, List(q.n) { -1 }) }

    LevelColumn(spacing = 10.dp) { compact ->
        S3Face(scope, if (compact) 56.dp else 72.dp, cover)
        RuleCard(
            res.getString(if (q.drinks != null) R.string.ch26_rule_drinks else R.string.ch26_rule_intro, q.n),
            *(if (q.spoken.isNotEmpty()) arrayOf(res.getString(R.string.ch26_rule_spoken)) else emptyArray()),
            *(if (q.hiddenLies > 0) arrayOf(res.getString(R.string.ch26_rule_hidden, q.hiddenLies)) else emptyArray()),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
            q.clues.forEachIndexed { j, text ->
                val spoken = j in q.spoken
                val heard = j in state.heard
                if (spoken) {
                    Surface(
                        onClick = {
                            state = state.copy(heard = state.heard + j)
                            scope.claim(res.getString(text), truth = j !in q.lies, subtle = subtle)
                        },
                        shape = RoundedCornerShape(8.dp), color = AskColor,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("os_clue_${j + 1}"),
                    ) {
                        Text(
                            if (heard) res.getString(R.string.ch26_heard, j + 1, res.getString(text)) else res.getString(R.string.ch26_ask, j + 1),
                            color = AskTextColor, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(12.dp),
                        )
                    }
                } else {
                    Text(res.getString(R.string.ch26_line, j + 1, res.getString(text)), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("os_clue_${j + 1}"))
                }
            }
        }
        Text(stringResource(R.string.ch26_your_order), style = MaterialTheme.typography.titleSmall)
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.widthIn(max = 460.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
        // 六顆的時候，小手機也要放得下。
        val slot = androidx.compose.ui.unit.min(52.dp, (maxWidth - 6.dp * (q.n - 1)) / q.n)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(q.n) { k ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${k + 1}", style = MaterialTheme.typography.labelLarge)
                    val c = state.colors[k]
                    val cDesc = res.getString(R.string.ch26_slot_cd, k + 1, if (c < 0) res.getString(R.string.ch26_empty) else names[c])
                    Box(
                        Modifier
                            .size(slot)
                            .background(if (c < 0) Color.Transparent else S3Colors[c], RoundedCornerShape(12.dp))
                            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) {
                                scope.cue(Cue.Tap)
                                state = state.copy(colors = state.colors.toMutableList().also { it[k] = (c + 1) % q.n })
                            }
                            .semantics { contentDescription = cDesc }
                            .testTag("os_c_${k + 1}"),
                        contentAlignment = Alignment.Center,
                    ) { if (c >= 0) Text(names[c].take(1), color = Color.White, fontWeight = FontWeight.Bold) }
                    if (q.drinks != null) {
                        val d = state.drinks[k]
                        val dDesc = res.getString(R.string.ch26_drink_cd, k + 1, if (d < 0) res.getString(R.string.ch26_empty) else drinkNames[d])
                        Box(
                            Modifier
                                .size(slot, 40.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button) {
                                    scope.cue(Cue.Tap)
                                    state = state.copy(drinks = state.drinks.toMutableList().also { it[k] = (d + 1) % q.n })
                                }
                                .semantics { contentDescription = dDesc }
                                .testTag("os_d_${k + 1}"),
                            contentAlignment = Alignment.Center,
                        ) { Text(if (d < 0) "—" else drinkNames[d], style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        }
        }
        Text(stringResource(if (q.drinks != null) R.string.ch26_help_drinks else R.string.ch26_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = {
                when {
                    q.spoken.any { it !in state.heard } -> scope.say(Line.Res(R.string.ch26_listen_first), Mood.Normal)
                    state.colors == q.colors && (q.drinks == null || state.drinks == q.drinks) -> { scope.cue(Cue.Success); scope.complete() }
                    else -> scope.mistake("os_wrong", Line.Res(R.string.ch26_wrong), Mood.Smug)
                }
            },
            modifier = Modifier.heightIn(min = 48.dp).testTag("os_confirm"),
        ) { Text(stringResource(R.string.ch26_confirm)) }
    }
}

// ---- 關卡 ----

object Level401Line : OrderLevel(Ch26Meta.l401, Ch26Puzzles.l401)
object Level402Again : OrderLevel(Ch26Meta.l402, Ch26Puzzles.l402)
object Level403Five : OrderLevel(Ch26Meta.l403, Ch26Puzzles.l403)
object Level404Between : OrderLevel(Ch26Meta.l404, Ch26Puzzles.l404)
object Level405ItSays : OrderLevel(Ch26Meta.l405, Ch26Puzzles.l405)
object Level406Six : OrderLevel(Ch26Meta.l406, Ch26Puzzles.l406)
object Level407TwoLies : OrderLevel(Ch26Meta.l407, Ch26Puzzles.l407)
object Level408Drinks : OrderLevel(Ch26Meta.l408, Ch26Puzzles.l408)
object Level409FourDrinks : OrderLevel(Ch26Meta.l409, Ch26Puzzles.l409)
object Level410Cafe : OrderLevel(Ch26Meta.l410, Ch26Puzzles.l410)
object Level411OneFalse : OrderLevel(Ch26Meta.l411, Ch26Puzzles.l411)
object Level412NoDirect : OrderLevel(Ch26Meta.l412, Ch26Puzzles.l412)
object Level413DrinkLie : OrderLevel(Ch26Meta.l413, Ch26Puzzles.l413)
object Level414Sunglasses : OrderLevel(Ch26Meta.l414, Ch26Puzzles.l414, cover = FaceCover.Eyes, subtle = true)
object Level415FalseOrder : OrderLevel(Ch26Meta.l415, Ch26Puzzles.l415)
object Level416SixFalse : OrderLevel(Ch26Meta.l416, Ch26Puzzles.l416)
object Level417Mask : OrderLevel(Ch26Meta.l417, Ch26Puzzles.l417, cover = FaceCover.Mouth, subtle = true)
object Level418Rush : OrderLevel(Ch26Meta.l418, Ch26Puzzles.l418)
object Level419SixLies : OrderLevel(Ch26Meta.l419, Ch26Puzzles.l419, subtle = true)
object Level420Queue : OrderLevel(Ch26Meta.l420, Ch26Puzzles.l420, subtle = true)

val Chapter26 = listOf(
    Level401Line, Level402Again, Level403Five, Level404Between, Level405ItSays,
    Level406Six, Level407TwoLies, Level408Drinks, Level409FourDrinks, Level410Cafe,
    Level411OneFalse, Level412NoDirect, Level413DrinkLie, Level414Sunglasses, Level415FalseOrder,
    Level416SixFalse, Level417Mask, Level418Rush, Level419SixLies, Level420Queue,
)
