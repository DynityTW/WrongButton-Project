package tw.dynity.wrongbutton.levels.s3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
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

/**
 * 迷宮（第 27 章）。[cells]：數字＝剛好跳幾格；U/D/L/R＝往那個方向走幾格都可以；G＝The Button。
 * 這是畫面上顯示的（它說的）箭頭；[lies] 裡的箭頭，真正的方向是相反的。從左上角出發，[steps] 步內走到 [goal]。
 */
data class MazeQuiz(
    val size: Int,
    val cells: List<String>,
    val goal: Int,
    val steps: Int,
    val spoken: Set<Int> = emptySet(),
    val lies: Set<Int> = emptySet(),
    val path: List<Int> = emptyList(),
) {
    fun name(i: Int) = "${'A' + i % size}${i / size + 1}"

    /** 真正的格子（謊話箭頭反過來）。 */
    fun real(i: Int): String = if (i in lies) when (cells[i]) { "U" -> "D"; "D" -> "U"; "L" -> "R"; else -> "L" } else cells[i]

    fun moves(i: Int): List<Int> {
        if (i == goal) return emptyList()
        val r = i / size
        val c = i % size
        val v = real(i)
        val dirs = listOf("U" to (-1 to 0), "D" to (1 to 0), "L" to (0 to -1), "R" to (0 to 1))
        return v.toIntOrNull()?.let { k ->
            dirs.mapNotNull { (_, d) -> val rr = r + d.first * k; val cc = c + d.second * k; if (rr in 0 until size && cc in 0 until size) rr * size + cc else null }
        } ?: run {
            val (dr, dc) = dirs.first { it.first == v }.second
            generateSequence(1) { it + 1 }.map { (r + dr * it) to (c + dc * it) }.takeWhile { (rr, cc) -> rr in 0 until size && cc in 0 until size }.map { (rr, cc) -> rr * size + cc }.toList()
        }
    }
}

@Serializable
data class MazeState(val trail: List<Int> = listOf(0), val heard: Set<Int> = emptySet())

abstract class MazeLevel(meta: S2Meta, val quiz: MazeQuiz, private val cover: FaceCover = FaceCover.None, private val subtle: Boolean = false) : S2Level(meta) {
    override val chrome: LevelChrome get() = LevelChrome(speechBubble = false)
    override val ambientChatter: Boolean get() = quiz.spoken.isEmpty()

    @Composable
    override fun Content(scope: LevelScope) = MazeField(scope, quiz, cover, subtle)
}

private fun arrow(v: String) = when (v) { "U" -> "↑"; "D" -> "↓"; "L" -> "←"; "R" -> "→"; else -> v }

@Composable
fun MazeField(scope: LevelScope, q: MazeQuiz, cover: FaceCover, subtle: Boolean) {
    val res = rememberResources()
    var state by rememberLevelState(MazeState.serializer()) { MazeState() }
    val dirNames = res.getStringArray(R.array.ch27_dirs)
    fun dirName(v: String) = dirNames["UDLR".indexOf(v)]

    fun tap(i: Int) {
        val pos = state.trail.last()
        when {
            q.spoken.any { it !in state.heard } -> scope.say(Line.Res(R.string.ch27_listen_first), Mood.Normal, Tell.Honest)
            i == pos -> scope.cue(Cue.Tap)
            i !in q.moves(pos) -> scope.mistake("mz_illegal", Line.Res(R.string.ch27_illegal), Mood.Smug)
            i == q.goal -> { state = state.copy(trail = state.trail + i); scope.cue(Cue.Success); scope.complete() }
            state.trail.size - 1 + 1 >= q.steps -> {
                // 步數用完了還沒到：算一次錯，回到起點。
                state = state.copy(trail = listOf(0))
                scope.mistake("mz_out", Line.Res(R.string.ch27_out_of_steps), Mood.Guilty)
            }
            else -> { scope.cue(Cue.Tap); state = state.copy(trail = state.trail + i) }
        }
    }

    LevelColumn(spacing = 10.dp) { compact ->
        S3Face(scope, if (compact) 56.dp else 64.dp, cover)
        RuleCard(
            res.getString(R.string.ch27_rule_intro),
            res.getString(R.string.ch27_rule_cells),
            res.getString(R.string.ch27_rule_steps, q.steps),
            *(if (q.spoken.isNotEmpty()) arrayOf(res.getString(R.string.ch27_rule_spoken)) else emptyArray()),
        )
        if (q.spoken.isNotEmpty()) {
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
            ) {
                q.spoken.sorted().forEach { i ->
                    val heard = i in state.heard
                    AskChip(
                        if (heard) res.getString(R.string.ch27_heard_short, q.name(i), arrow(q.cells[i])) else res.getString(R.string.ch27_ask_short, q.name(i)),
                        tag = "mz_ask_${q.name(i)}",
                    ) {
                        state = state.copy(heard = state.heard + i)
                        scope.claim(res.getString(R.string.ch27_says, q.name(i), dirName(q.cells[i])), truth = i !in q.lies, subtle = subtle)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.ch27_counter, state.trail.size - 1, q.steps), style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag("mz_counter"))
            OutlinedButton(onClick = { scope.cue(Cue.Tap); state = state.copy(trail = listOf(0)) }, modifier = Modifier.heightIn(min = 48.dp).testTag("mz_reset")) {
                Text(stringResource(R.string.ch27_reset))
            }
        }
        BoxWithConstraints(Modifier.widthIn(max = 460.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val gap = 3.dp
            val cell: Dp = min(56.dp, (maxWidth - 20.dp - gap * q.size) / q.size)
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Box(Modifier.size(20.dp))
                    repeat(q.size) { c -> Box(Modifier.size(cell, 20.dp), contentAlignment = Alignment.Center) { Text("${'A' + c}", style = MaterialTheme.typography.labelSmall) } }
                }
                repeat(q.size) { r ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(20.dp, cell), contentAlignment = Alignment.Center) { Text("${r + 1}", style = MaterialTheme.typography.labelSmall) }
                        repeat(q.size) { c ->
                            val i = r * q.size + c
                            MazeCell(q, state, i, cell, res, ::dirName) { tap(i) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MazeCell(q: MazeQuiz, state: MazeState, i: Int, size: Dp, res: android.content.res.Resources, dirName: (String) -> String, onTap: () -> Unit) {
    val v = q.cells[i]
    val here = state.trail.last() == i
    val step = state.trail.indexOf(i)
    val spoken = i in q.spoken
    val unheard = spoken && i !in state.heard
    val what = when {
        i == q.goal -> res.getString(R.string.ch27_cd_goal)
        unheard -> res.getString(R.string.ch27_cd_unheard)
        v.toIntOrNull() != null -> res.getString(R.string.ch27_cd_number, v.toInt())
        else -> res.getString(R.string.ch27_cd_arrow, dirName(v)) + if (spoken) res.getString(R.string.ch27_cd_said) else ""
    }
    val extra = when {
        here -> res.getString(R.string.ch27_cd_here)
        step >= 0 -> res.getString(R.string.ch27_cd_step, step)
        else -> ""
    }
    val desc = "${q.name(i)}：$what$extra"
    Box(
        Modifier
            .size(size)
            .background(
                when {
                    here -> MaterialTheme.colorScheme.primary
                    step >= 0 -> MaterialTheme.colorScheme.primaryContainer
                    spoken -> AskColor
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
                RoundedCornerShape(8.dp),
            )
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onTap)
            .semantics { contentDescription = desc }
            .testTag("mz_${q.name(i)}"),
        contentAlignment = Alignment.Center,
    ) {
        if (i == q.goal) {
            ButtonCharacter(Mood.Normal, Tell.Honest, size * 0.7f, describe = false)
        } else {
            Text(
                if (unheard) "?" else arrow(v),
                fontSize = (size.value * 0.45f).sp, fontWeight = FontWeight.Bold,
                color = when {
                    here -> MaterialTheme.colorScheme.onPrimary
                    spoken -> AskTextColor
                    v.toIntOrNull() != null -> Color(0xFF1565C0)
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
        if (step > 0 && !here) Text("$step", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.TopStart).padding(2.dp))
    }
}

// ---- 關卡 ----

object Level421Jump : MazeLevel(Ch27Meta.l421, Ch27Puzzles.l421)
object Level422Bigger : MazeLevel(Ch27Meta.l422, Ch27Puzzles.l422)
object Level423Longer : MazeLevel(Ch27Meta.l423, Ch27Puzzles.l423)
object Level424Arrows : MazeLevel(Ch27Meta.l424, Ch27Puzzles.l424)
object Level425MoreArrows : MazeLevel(Ch27Meta.l425, Ch27Puzzles.l425)
object Level426Mixed : MazeLevel(Ch27Meta.l426, Ch27Puzzles.l426)
object Level427SixJump : MazeLevel(Ch27Meta.l427, Ch27Puzzles.l427)
object Level428SixArrows : MazeLevel(Ch27Meta.l428, Ch27Puzzles.l428)
object Level429ItPoints : MazeLevel(Ch27Meta.l429, Ch27Puzzles.l429)
object Level430SixMixed : MazeLevel(Ch27Meta.l430, Ch27Puzzles.l430)
object Level431LongJump : MazeLevel(Ch27Meta.l431, Ch27Puzzles.l431)
object Level432Sunglasses : MazeLevel(Ch27Meta.l432, Ch27Puzzles.l432, cover = FaceCover.Eyes, subtle = true)
object Level433SevenMixed : MazeLevel(Ch27Meta.l433, Ch27Puzzles.l433)
object Level434SevenArrows : MazeLevel(Ch27Meta.l434, Ch27Puzzles.l434)
object Level435SevenJump : MazeLevel(Ch27Meta.l435, Ch27Puzzles.l435)
object Level436TwoLies : MazeLevel(Ch27Meta.l436, Ch27Puzzles.l436)
object Level437Mask : MazeLevel(Ch27Meta.l437, Ch27Puzzles.l437, cover = FaceCover.Mouth, subtle = true)
object Level438Marathon : MazeLevel(Ch27Meta.l438, Ch27Puzzles.l438)
object Level439Subtle : MazeLevel(Ch27Meta.l439, Ch27Puzzles.l439, subtle = true)
object Level440Labyrinth : MazeLevel(Ch27Meta.l440, Ch27Puzzles.l440, subtle = true)

val Chapter27 = listOf(
    Level421Jump, Level422Bigger, Level423Longer, Level424Arrows, Level425MoreArrows,
    Level426Mixed, Level427SixJump, Level428SixArrows, Level429ItPoints, Level430SixMixed,
    Level431LongJump, Level432Sunglasses, Level433SevenMixed, Level434SevenArrows, Level435SevenJump,
    Level436TwoLies, Level437Mask, Level438Marathon, Level439Subtle, Level440Labyrinth,
)
