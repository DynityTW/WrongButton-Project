package tw.dynity.wrongbutton.levels.s3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.FaceCover
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.LevelChrome
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.levels.LevelColumn
import tw.dynity.wrongbutton.levels.rememberResources
import tw.dynity.wrongbutton.levels.s2.S2Level
import tw.dynity.wrongbutton.levels.s2.S2Meta

/** 要找出什麼：所有說謊的、The Button 的分身、間諜。 */
enum class LiarGoal { Liars, Button, Spy }

/** 某一顆按鈕說的一句話。 */
data class Said(val who: Int, val text: Int)

/**
 * 一題「誰在說謊」（第 23 章）。[roles] 是每一顆的真實身分（T 說真話、L 說謊、S 間諜），只給測試與設計文件用。
 * [narrator] 是 The Button 本人說的一句話：有臉可以讀（[narratorTrue] 決定它是不是謊話）。
 */
data class LiarPuzzle(
    val n: Int,
    val said: List<Said>,
    val answer: Set<Int>,
    val goal: LiarGoal,
    val roles: String,
    val button: Int? = null,
    val spy: Boolean = false,
    val oneHonest: Boolean = false,
    val narrator: Int? = null,
    val narratorTrue: Boolean = true,
)

@Serializable
data class LiarState(val marked: Set<Int> = emptySet(), val heard: Boolean = false, val special: Int? = null)

abstract class LiarLevel(
    meta: S2Meta,
    val puzzle: LiarPuzzle,
    private val cover: FaceCover = FaceCover.None,
    private val subtle: Boolean = false,
) : S2Level(meta) {
    override val chrome: LevelChrome get() = LevelChrome(speechBubble = false)
    override val ambientChatter: Boolean get() = puzzle.narrator == null

    @Composable
    override fun Content(scope: LevelScope) = LiarField(scope, puzzle, cover, subtle)
}

private val SpeakerColors = listOf(Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFF9A825), Color(0xFF8E24AA), Color(0xFF757575))

@Composable
fun LiarField(scope: LevelScope, p: LiarPuzzle, cover: FaceCover, subtle: Boolean) {
    val res = rememberResources()
    var state by rememberLevelState(LiarState.serializer()) { LiarState() }
    val names = res.getStringArray(R.array.ch23_names)

    // 點一下換標記。找分身／找間諜的關卡，也要標出所有說謊的（不然一顆一顆試就過了）：
    // 只找說謊的：無 → 說謊 → 無
    // 找間諜：無 → 說謊 → 間諜 → 無（間諜不算說謊的）
    // 找分身：無 → 說謊 → 分身 → 分身而且說謊 → 無
    fun toggle(i: Int) {
        scope.cue(Cue.Tap)
        val liar = i in state.marked
        val special = state.special == i
        state = when (p.goal) {
            LiarGoal.Liars -> state.copy(marked = if (liar) state.marked - i else state.marked + i)
            LiarGoal.Spy -> when {
                !liar && !special -> state.copy(marked = state.marked + i)
                liar -> state.copy(marked = state.marked - i, special = i)
                else -> state.copy(special = null)
            }
            LiarGoal.Button -> when {
                !liar && !special -> state.copy(marked = state.marked + i)
                liar && !special -> state.copy(marked = state.marked - i, special = i)
                !liar -> state.copy(marked = state.marked + i)
                else -> state.copy(marked = state.marked - i, special = null)
            }
        }
    }

    fun solved(): Boolean {
        val liars = p.roles.indices.filter { p.roles[it] == 'L' }.toSet()
        return state.marked == liars && (p.goal == LiarGoal.Liars || state.special == p.answer.single())
    }

    fun confirm() {
        when {
            p.narrator != null && !state.heard -> scope.say(Line.Res(R.string.ch23_listen_first), Mood.Normal, Tell.Honest)
            solved() -> { scope.cue(Cue.Success); scope.complete() }
            else -> scope.mistake("kk_wrong", Line.Res(R.string.ch23_wrong), Mood.Smug)
        }
    }

    LevelColumn(spacing = 10.dp) { compact ->
        val faceSize = if (compact || p.n >= 5) 64.dp else 80.dp
        Box(contentAlignment = Alignment.TopCenter) {
            ButtonCharacter(scope.expression.mood, scope.expression.tell, faceSize, cover = cover, modifier = Modifier.testTag("the_button"))
            if (cover == FaceCover.Mouth) {
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFE3F2FD), modifier = Modifier.offset(y = faceSize * 0.52f).size(faceSize * 0.62f, faceSize * 0.3f)) {}
            }
        }
        scope.SpeechBubble(Modifier.widthIn(max = 420.dp), showFace = false)
        // 規則
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.ch23_rule_basic), style = MaterialTheme.typography.bodyMedium)
                if (p.spy) Text(stringResource(R.string.ch23_rule_spy), style = MaterialTheme.typography.bodyMedium)
                if (p.button != null) Text(stringResource(R.string.ch23_rule_double), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(when (p.goal) { LiarGoal.Liars -> R.string.ch23_goal_liars; LiarGoal.Button -> R.string.ch23_goal_double; LiarGoal.Spy -> R.string.ch23_goal_spy }),
                    style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag("kk_goal"),
                )
            }
        }
        if (p.narrator != null) {
            Surface(
                onClick = {
                    state = state.copy(heard = true)
                    scope.say(Line.Text(res.getString(p.narrator)), if (p.narratorTrue) Mood.Normal else Mood.Smug,
                        if (p.narratorTrue) Tell.Honest else if (subtle) Tell.LieSubtle else Tell.LieObvious)
                },
                shape = RoundedCornerShape(8.dp), color = Color(0xFFFFE0B2),
                modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().heightIn(min = 48.dp).testTag("kk_narrator"),
            ) {
                Text(
                    if (state.heard) stringResource(R.string.ch23_narrator_heard, stringResource(p.narrator)) else stringResource(R.string.ch23_narrator_ask),
                    color = Color(0xFF6D3B00), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(12.dp),
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
        ) {
            repeat(p.n) { i ->
                val lines = p.said.filter { it.who == i }.map { res.getString(it.text) }
                val liar = i in state.marked
                val special = state.special == i
                val marked = liar || special
                val mark = listOfNotNull(
                    if (special) res.getString(if (p.goal == LiarGoal.Spy) R.string.ch23_marked_spy else R.string.ch23_marked_double) else null,
                    if (liar) res.getString(R.string.ch23_marked_liar) else null,
                ).joinToString("・")
                val cd = res.getString(R.string.ch23_card_cd, names[i], lines.joinToString("」「")) + if (marked) "，$mark" else ""
                Surface(
                    onClick = { toggle(i) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth(0.48f)
                        .heightIn(min = 96.dp)
                        .border(if (marked) 3.dp else 0.dp, if (special) Color(0xFF6A1B9A) else if (marked) MaterialTheme.colorScheme.error else Color.Transparent, RoundedCornerShape(14.dp))
                        .semantics { contentDescription = cd; selected = marked }
                        .testTag("kk_$i"),
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(28.dp).background(SpeakerColors[i], RoundedCornerShape(8.dp)))
                            Text(names[i], style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (marked) Text(mark, style = MaterialTheme.typography.labelMedium, color = if (special) Color(0xFF6A1B9A) else MaterialTheme.colorScheme.error)
                        }
                        lines.forEach { Text("「$it」", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
        Button(onClick = ::confirm, modifier = Modifier.heightIn(min = 48.dp).testTag("kk_confirm")) {
            Text(stringResource(R.string.ch23_confirm), textAlign = TextAlign.Center)
        }
    }
}

// ---- 關卡 ----

object Level341TwoOfThem : LiarLevel(Ch23Meta.l341, Ch23Puzzles.l341)
object Level342Three : LiarLevel(Ch23Meta.l342, Ch23Puzzles.l342)
object Level343Who : LiarLevel(Ch23Meta.l343, Ch23Puzzles.l343)
object Level344Double : LiarLevel(Ch23Meta.l344, Ch23Puzzles.l344)
object Level345SameKind : LiarLevel(Ch23Meta.l345, Ch23Puzzles.l345)
object Level346Count : LiarLevel(Ch23Meta.l346, Ch23Puzzles.l346)
object Level347IfThen : LiarLevel(Ch23Meta.l347, Ch23Puzzles.l347)
object Level348TwoLines : LiarLevel(Ch23Meta.l348, Ch23Puzzles.l348)
object Level349Spy : LiarLevel(Ch23Meta.l349, Ch23Puzzles.l349)
object Level350ItSaysToo : LiarLevel(Ch23Meta.l350, Ch23Puzzles.l350)
object Level351InARow : LiarLevel(Ch23Meta.l351, Ch23Puzzles.l351)
object Level352WouldSay : LiarLevel(Ch23Meta.l352, Ch23Puzzles.l352)
object Level353FindDouble : LiarLevel(Ch23Meta.l353, Ch23Puzzles.l353)
object Level354SpyAndDouble : LiarLevel(Ch23Meta.l354, Ch23Puzzles.l354)
object Level355Five : LiarLevel(Ch23Meta.l355, Ch23Puzzles.l355)
object Level356TwoEach : LiarLevel(Ch23Meta.l356, Ch23Puzzles.l356)
object Level357Sunglasses : LiarLevel(Ch23Meta.l357, Ch23Puzzles.l357, cover = FaceCover.Eyes, subtle = true)
object Level358SpyTwoLines : LiarLevel(Ch23Meta.l358, Ch23Puzzles.l358)
object Level359Six : LiarLevel(Ch23Meta.l359, Ch23Puzzles.l359)
object Level360WhoLies : LiarLevel(Ch23Meta.l360, Ch23Puzzles.l360, cover = FaceCover.Mouth, subtle = true)

val Chapter23 = listOf(
    Level341TwoOfThem, Level342Three, Level343Who, Level344Double, Level345SameKind,
    Level346Count, Level347IfThen, Level348TwoLines, Level349Spy, Level350ItSaysToo,
    Level351InARow, Level352WouldSay, Level353FindDouble, Level354SpyAndDouble, Level355Five,
    Level356TwoEach, Level357Sunglasses, Level358SpyTwoLines, Level359Six, Level360WhoLies,
)
