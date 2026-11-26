package tw.dynity.wrongbutton.levels.s3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * 密碼規則（第 28 章）。[digits] 是唯一符合的密碼。[spoken] 的規則要聽 The Button 說，[lies] 是假的規則；
 * [hiddenLies] > 0：看不到臉，只知道剛好幾條是假的。規則旁邊不會即時打勾。
 */
data class PinQuiz(
    val digits: List<Int>,
    val rules: List<Int>,
    val spoken: Set<Int> = emptySet(),
    val lies: Set<Int> = emptySet(),
    val hiddenLies: Int = 0,
)

@Serializable
data class PinState(val digits: List<Int> = emptyList(), val heard: Set<Int> = emptySet())

abstract class PinLevel(meta: S2Meta, val quiz: PinQuiz, private val cover: FaceCover = FaceCover.None, private val subtle: Boolean = false) : S2Level(meta) {
    override val chrome: LevelChrome get() = LevelChrome(speechBubble = false)
    override val ambientChatter: Boolean get() = quiz.spoken.isEmpty()

    @Composable
    override fun Content(scope: LevelScope) = PinField(scope, quiz, cover, subtle)
}

@Composable
fun PinField(scope: LevelScope, q: PinQuiz, cover: FaceCover, subtle: Boolean) {
    val res = rememberResources()
    val length = q.digits.size
    var state by rememberLevelState(PinState.serializer()) { PinState(List(length) { 0 }) }

    fun bump(k: Int, by: Int) {
        scope.cue(Cue.Tap)
        state = state.copy(digits = state.digits.toMutableList().also { it[k] = (it[k] + by + 10) % 10 })
    }

    LevelColumn(spacing = 10.dp) { compact ->
        S3Face(scope, if (compact) 56.dp else 64.dp, cover)
        RuleCard(
            res.getString(R.string.ch28_rule_intro, length),
            res.getString(R.string.ch28_rule_no_check),
            *(if (q.spoken.isNotEmpty()) arrayOf(res.getString(R.string.ch28_rule_spoken)) else emptyArray()),
            *(if (q.hiddenLies > 0) arrayOf(res.getString(R.string.ch28_rule_hidden, q.hiddenLies)) else emptyArray()),
        )
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.ch28_requirements), style = MaterialTheme.typography.titleSmall)
                q.rules.forEachIndexed { j, text ->
                    if (j in q.spoken) {
                        val heard = j in state.heard
                        Surface(
                            onClick = {
                                state = state.copy(heard = state.heard + j)
                                scope.claim(res.getString(text), truth = j !in q.lies, subtle = subtle)
                            },
                            shape = RoundedCornerShape(8.dp), color = AskColor,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("pw_rule_${j + 1}"),
                        ) {
                            Text(
                                if (heard) res.getString(R.string.ch28_heard, j + 1, res.getString(text)) else res.getString(R.string.ch28_ask, j + 1),
                                color = AskTextColor, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(12.dp),
                            )
                        }
                    } else {
                        Text("・" + res.getString(text), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("pw_rule_${j + 1}"))
                    }
                }
            }
        }
        Text(stringResource(R.string.ch28_password), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(length) { k ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalIconButton(onClick = { bump(k, 1) }, modifier = Modifier.size(48.dp).testTag("pw_up_${k + 1}")) {
                        Text("▲", modifier = Modifier.semantics { contentDescription = res.getString(R.string.ch28_up_cd, k + 1) })
                    }
                    val desc = res.getString(R.string.ch28_digit_cd, k + 1, state.digits[k])
                    Box(
                        Modifier
                            .size(48.dp, 56.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .semantics { contentDescription = desc }
                            .testTag("pw_d_${k + 1}"),
                        contentAlignment = Alignment.Center,
                    ) { Text("${state.digits[k]}", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                    FilledTonalIconButton(onClick = { bump(k, -1) }, modifier = Modifier.size(48.dp).testTag("pw_down_${k + 1}")) {
                        Text("▼", modifier = Modifier.semantics { contentDescription = res.getString(R.string.ch28_down_cd, k + 1) })
                    }
                }
            }
        }
        Button(
            onClick = {
                when {
                    q.spoken.any { it !in state.heard } -> scope.say(Line.Res(R.string.ch28_listen_first), Mood.Normal)
                    state.digits == q.digits -> { scope.cue(Cue.Success); scope.complete() }
                    else -> scope.mistake("pw_wrong", Line.Res(R.string.ch28_wrong), Mood.Smug)
                }
            },
            modifier = Modifier.heightIn(min = 48.dp).testTag("pw_confirm"),
        ) { Text(stringResource(R.string.ch28_confirm)) }
    }
}

// ---- 關卡 ----

object Level441Password : PinLevel(Ch28Meta.l441, Ch28Puzzles.l441)
object Level442Ascending : PinLevel(Ch28Meta.l442, Ch28Puzzles.l442)
object Level443Odd : PinLevel(Ch28Meta.l443, Ch28Puzzles.l443)
object Level444ItSays : PinLevel(Ch28Meta.l444, Ch28Puzzles.l444)
object Level445ThreeDigits : PinLevel(Ch28Meta.l445, Ch28Puzzles.l445)
object Level446FourDigits : PinLevel(Ch28Meta.l446, Ch28Puzzles.l446)
object Level447Biggest : PinLevel(Ch28Meta.l447, Ch28Puzzles.l447)
object Level448Spoken : PinLevel(Ch28Meta.l448, Ch28Puzzles.l448)
object Level449Product : PinLevel(Ch28Meta.l449, Ch28Puzzles.l449)
object Level450OneFalse : PinLevel(Ch28Meta.l450, Ch28Puzzles.l450)
object Level451Prime : PinLevel(Ch28Meta.l451, Ch28Puzzles.l451, cover = FaceCover.Eyes, subtle = true)
object Level452Multiple : PinLevel(Ch28Meta.l452, Ch28Puzzles.l452)
object Level453Eleven : PinLevel(Ch28Meta.l453, Ch28Puzzles.l453)
object Level454Palindrome : PinLevel(Ch28Meta.l454, Ch28Puzzles.l454)
object Level455Mask : PinLevel(Ch28Meta.l455, Ch28Puzzles.l455, cover = FaceCover.Mouth, subtle = true)
object Level456FiveDigits : PinLevel(Ch28Meta.l456, Ch28Puzzles.l456)
object Level457FiveLie : PinLevel(Ch28Meta.l457, Ch28Puzzles.l457)
object Level458FiveFalse : PinLevel(Ch28Meta.l458, Ch28Puzzles.l458)
object Level459Strong : PinLevel(Ch28Meta.l459, Ch28Puzzles.l459, subtle = true)
object Level460Vault : PinLevel(Ch28Meta.l460, Ch28Puzzles.l460, subtle = true)

val Chapter28 = listOf(
    Level441Password, Level442Ascending, Level443Odd, Level444ItSays, Level445ThreeDigits,
    Level446FourDigits, Level447Biggest, Level448Spoken, Level449Product, Level450OneFalse,
    Level451Prime, Level452Multiple, Level453Eleven, Level454Palindrome, Level455Mask,
    Level456FiveDigits, Level457FiveLie, Level458FiveFalse, Level459Strong, Level460Vault,
)
