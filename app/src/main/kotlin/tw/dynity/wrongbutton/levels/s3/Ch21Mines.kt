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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
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

/** 數字算哪些鄰格：周圍八格，或只算上下左右（第 308、311、316 關）。 */
enum class MineRule { Eight, Cross }

/** 翻開所有安全格，或只能插旗、把每一顆雷都標出來。 */
enum class MineMode { Reveal, Flag }

/**
 * 一張踩地雷的盤面（第 21 章）。格子編號 = 列 × 欄數 + 欄。
 *
 * - [open]：一開始就翻開的格子，數字一定是真的（盤面不會說謊）。
 * - [reported]：The Button 說的數字（格子 → 它說的數字）。[lies] 裡的是謊話：真正的數字一定不是它說的那個。
 * - [hiddenLies] > 0：看不到它的臉，數字寫在紙條上，只知道剛好幾張是假的。
 */
data class MineBoard(
    val rows: Int,
    val cols: Int,
    val mines: Set<Int>,
    val open: Set<Int>,
    val reported: Map<Int, Int>,
    val lies: Set<Int>,
    val rule: MineRule = MineRule.Eight,
    val mode: MineMode = MineMode.Reveal,
    val counter: Boolean = false,
    val hiddenLies: Int = 0,
    val cover: FaceCover = FaceCover.None,
) {
    val size get() = rows * cols

    fun name(i: Int) = "${'A' + i % cols}${i / cols + 1}"

    fun neighbors(i: Int): List<Int> {
        val r = i / cols
        val c = i % cols
        val d = when (rule) {
            MineRule.Eight -> listOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)
            MineRule.Cross -> listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        }
        return d.mapNotNull { (a, b) -> if (r + a in 0 until rows && c + b in 0 until cols) (r + a) * cols + (c + b) else null }
    }

    fun number(i: Int) = neighbors(i).count { it in mines }

    /** 翻開一格；是 0 的話像真的踩地雷一樣，自動翻開周圍。 */
    fun flood(start: Int, known: Set<Int>): Set<Int> {
        val out = mutableSetOf<Int>()
        val stack = ArrayDeque(listOf(start))
        while (stack.isNotEmpty()) {
            val x = stack.removeLast()
            if (x in out || x in mines || (x != start && x in known)) continue
            out += x
            if (number(x) == 0) stack.addAll(neighbors(x))
        }
        return out
    }

    /** 安全、而且需要玩家翻開的格子。 */
    val safeToReveal: Set<Int> get() = (0 until size).filter { it !in mines && it !in open && it !in reported }.toSet()

    companion object {
        /** 由 tools/s3/mines.py 的圖例字串建立：* 雷、. 沒翻開、o 翻開、R 真話、L 謊話。 */
        fun parse(
            vararg rows: String,
            reported: Map<Int, Int>,
            rule: MineRule = MineRule.Eight,
            mode: MineMode = MineMode.Reveal,
            counter: Boolean = false,
            hiddenLies: Int = 0,
            cover: FaceCover = FaceCover.None,
        ): MineBoard {
            val cols = rows.first().length
            val flat = rows.joinToString("")
            require(rows.all { it.length == cols })
            return MineBoard(
                rows = rows.size,
                cols = cols,
                mines = flat.indices.filter { flat[it] == '*' }.toSet(),
                open = flat.indices.filter { flat[it] == 'o' }.toSet(),
                reported = reported,
                lies = flat.indices.filter { flat[it] == 'L' }.toSet(),
                rule = rule,
                mode = mode,
                counter = counter,
                hiddenLies = hiddenLies,
                cover = cover,
            ).also { b -> require(b.reported.keys == flat.indices.filter { flat[it] == 'R' || flat[it] == 'L' }.toSet()) }
        }
    }
}

@Serializable
data class MineState(
    val revealed: Set<Int> = emptySet(),
    val flags: Set<Int> = emptySet(),
    val exploded: Set<Int> = emptySet(),
    val asked: Set<Int> = emptySet(),
    val flagging: Boolean = false,
    val readySaid: Boolean = false,
)

object Mines {
    fun ready(b: MineBoard, s: MineState): Boolean = when (b.mode) {
        MineMode.Reveal -> b.safeToReveal.all { it in s.revealed }
        MineMode.Flag -> s.flags == b.mines
    }
}

/** 第 21 章的關卡：一張盤面。 */
abstract class MineLevel(meta: S2Meta, val board: MineBoard, private val subtle: Boolean = false) : S2Level(meta) {
    override val chrome: LevelChrome get() = LevelChrome(speechBubble = false)

    // 讀臉的關卡：不要讓閒聊蓋掉它剛剛說數字時的表情。
    override val ambientChatter: Boolean get() = false

    @Composable
    override fun Content(scope: LevelScope) = MineField(scope, board, subtle)
}

@Composable
fun MineField(scope: LevelScope, board: MineBoard, subtle: Boolean) {
    val res = rememberResources()
    var state by rememberLevelState(MineState.serializer()) { MineState(flagging = board.mode == MineMode.Flag) }
    val ready = Mines.ready(board, state)
    LaunchedEffect(ready) {
        if (ready && !state.readySaid) {
            state = state.copy(readySaid = true)
            scope.say(Line.Res(R.string.ch21_ready), Mood.Normal, Tell.Honest)
        }
    }

    fun tap(i: Int) {
        when {
            i in board.reported -> {
                if (board.hiddenLies > 0) { scope.cue(Cue.Tap); return }
                val lie = i in board.lies
                state = state.copy(asked = state.asked + i)
                scope.say(
                    Line.Text(res.getString(R.string.ch21_said, board.name(i), board.reported.getValue(i))),
                    if (lie) Mood.Smug else Mood.Normal,
                    if (!lie) Tell.Honest else if (subtle) Tell.LieSubtle else Tell.LieObvious,
                )
            }
            i in board.open || i in state.revealed || i in state.exploded -> scope.cue(Cue.Tap)
            state.flagging || board.mode == MineMode.Flag -> {
                when {
                    i in state.flags -> state = state.copy(flags = state.flags - i)
                    board.mode == MineMode.Flag && i !in board.mines -> scope.mistake("mf_flag", Line.Res(R.string.ch21_wrong_flag), Mood.Smug)
                    else -> { scope.cue(Cue.Tap); state = state.copy(flags = state.flags + i) }
                }
            }
            i in state.flags -> scope.cue(Cue.Tap)
            i in board.mines -> {
                state = state.copy(exploded = state.exploded + i)
                scope.mistake("mf_mine")
            }
            else -> {
                scope.cue(Cue.Tap)
                val known = board.open + board.reported.keys + state.revealed + state.flags
                state = state.copy(revealed = state.revealed + board.flood(i, known))
            }
        }
    }

    LevelColumn(spacing = 10.dp) { compact ->
        // The Button（可以按；掃完才算數）。
        val faceSize = if (compact || board.rows >= 7) 72.dp else 96.dp
        Box(contentAlignment = Alignment.TopCenter) {
            ButtonCharacter(
                scope.expression.mood,
                scope.expression.tell,
                faceSize,
                cover = board.cover,
                modifier = Modifier
                    .testTag("the_button")
                    .clickable(role = Role.Button) {
                        if (ready) { scope.cue(Cue.Success); scope.complete() }
                        else scope.mistake("mf_early", Line.Res(R.string.ch21_not_ready), Mood.Guilty)
                    },
            )
            if (board.cover == FaceCover.Mouth) {
                // 口罩：只看得到眼睛。
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE3F2FD),
                    modifier = Modifier.offset(y = faceSize * 0.52f).size(faceSize * 0.62f, faceSize * 0.3f),
                ) {}
            }
        }
        scope.SpeechBubble(Modifier.widthIn(max = 420.dp), showFace = false)
        if (board.rule == MineRule.Cross) {
            Text(stringResource(R.string.ch21_rule_cross), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.testTag("mf_rule"))
        }
        if (board.hiddenLies > 0) {
            Text(stringResource(R.string.ch21_notes, board.hiddenLies), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.testTag("mf_notes"))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (board.mode == MineMode.Reveal) {
                FilterChip(!state.flagging, { state = state.copy(flagging = false) }, { Text(stringResource(R.string.ch21_mode_reveal)) }, Modifier.heightIn(min = 48.dp).testTag("mf_mode_reveal"))
                FilterChip(state.flagging, { state = state.copy(flagging = true) }, { Text(stringResource(R.string.ch21_mode_flag)) }, Modifier.heightIn(min = 48.dp).testTag("mf_mode_flag"))
            }
            if (board.counter) {
                Text(
                    stringResource(R.string.ch21_counter, board.mines.size, state.flags.size),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.testTag("mf_counter"),
                )
            }
        }
        Grid(board, state, ::tap)
    }
}

@Composable
private fun Grid(board: MineBoard, state: MineState, onTap: (Int) -> Unit) {
    val res = rememberResources()
    BoxWithConstraints(Modifier.widthIn(max = 460.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
        val label = 20.dp
        val gap = 2.dp
        val cell: Dp = min(48.dp, (maxWidth - label - gap * board.cols) / board.cols)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Box(Modifier.size(label))
                repeat(board.cols) { c -> Box(Modifier.size(cell, label), contentAlignment = Alignment.Center) { Text("${'A' + c}", style = MaterialTheme.typography.labelSmall) } }
            }
            repeat(board.rows) { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(label, cell), contentAlignment = Alignment.Center) { Text("${r + 1}", style = MaterialTheme.typography.labelSmall) }
                    repeat(board.cols) { c ->
                        val i = r * board.cols + c
                        Cell(board, state, i, cell, res, onTap)
                    }
                }
            }
        }
    }
}

@Composable
private fun Cell(board: MineBoard, state: MineState, i: Int, size: Dp, res: android.content.res.Resources, onTap: (Int) -> Unit) {
    val name = board.name(i)
    val scheme = MaterialTheme.colorScheme
    val revealed = i in board.open || i in state.revealed
    val (bg, text, color, cd) = when {
        i in state.exploded -> Quad(scheme.errorContainer, "●", scheme.onErrorContainer, res.getString(R.string.ch21_cd_mine))
        i in board.reported && board.hiddenLies > 0 -> {
            val v = board.reported.getValue(i)
            Quad(Color(0xFFFFF3C4), "$v", Color(0xFF5D4300), res.getString(R.string.ch21_cd_note, v))
        }
        i in board.reported && i in state.asked -> {
            val v = board.reported.getValue(i)
            Quad(Color(0xFFFFE0B2), "$v", Color(0xFF6D3B00), res.getString(R.string.ch21_cd_said, v))
        }
        i in board.reported -> Quad(Color(0xFFFFE0B2), "?", Color(0xFF6D3B00), res.getString(R.string.ch21_cd_ask))
        revealed -> {
            val n = board.number(i)
            Quad(scheme.surface, if (n == 0) "" else "$n", numberColor(n), if (n == 0) res.getString(R.string.ch21_cd_zero) else "$n")
        }
        i in state.flags -> Quad(scheme.surfaceVariant, "▲", Color(0xFFC62828), res.getString(R.string.ch21_cd_flag))
        else -> Quad(scheme.surfaceVariant, "", scheme.onSurfaceVariant, res.getString(R.string.ch21_cd_hidden))
    }
    val description = res.getString(R.string.ch21_cd, name, cd)
    Box(
        Modifier
            .size(size)
            .background(bg, RoundedCornerShape(6.dp))
            .border(0.5.dp, scheme.outlineVariant, RoundedCornerShape(6.dp))
            .clickable(role = Role.Button) { onTap(i) }
            .semantics { contentDescription = description }
            .testTag("mf_$name"),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
        if (i in board.reported && i in state.asked) {
            // 小標記：這是它說的，不是盤面上的。
            Text("”", color = color, fontSize = (size.value * 0.3f).sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = (-3).dp))
        }
    }
}

private data class Quad(val bg: Color, val text: String, val color: Color, val cd: String)

private fun numberColor(n: Int) = when (n) {
    1 -> Color(0xFF1565C0)
    2 -> Color(0xFF2E7D32)
    3 -> Color(0xFFC62828)
    4 -> Color(0xFF283593)
    5 -> Color(0xFF6D4C41)
    else -> Color(0xFF424242)
}

// ---- 關卡 ----

object Level301Mines : MineLevel(Ch21Meta.l301, Ch21Boards.l301)
object Level302Numbers : MineLevel(Ch21Meta.l302, Ch21Boards.l302)
object Level303Flags : MineLevel(Ch21Meta.l303, Ch21Boards.l303)
object Level304Corner : MineLevel(Ch21Meta.l304, Ch21Boards.l304)
object Level305Count : MineLevel(Ch21Meta.l305, Ch21Boards.l305)
object Level306ItSays : MineLevel(Ch21Meta.l306, Ch21Boards.l306)
object Level307ThreeSays : MineLevel(Ch21Meta.l307, Ch21Boards.l307)
object Level308Cross : MineLevel(Ch21Meta.l308, Ch21Boards.l308)
object Level309FlagsOnly : MineLevel(Ch21Meta.l309, Ch21Boards.l309)
object Level310Sunglasses : MineLevel(Ch21Meta.l310, Ch21Boards.l310, subtle = true)
object Level311CrossFlags : MineLevel(Ch21Meta.l311, Ch21Boards.l311)
object Level312AllSaid : MineLevel(Ch21Meta.l312, Ch21Boards.l312, subtle = true)
object Level313Mask : MineLevel(Ch21Meta.l313, Ch21Boards.l313, subtle = true)
object Level314Notes : MineLevel(Ch21Meta.l314, Ch21Boards.l314)
object Level315LastMines : MineLevel(Ch21Meta.l315, Ch21Boards.l315)
object Level316CrossLies : MineLevel(Ch21Meta.l316, Ch21Boards.l316, subtle = true)
object Level317BigMap : MineLevel(Ch21Meta.l317, Ch21Boards.l317)
object Level318FlagExam : MineLevel(Ch21Meta.l318, Ch21Boards.l318, subtle = true)
object Level319TwoNotes : MineLevel(Ch21Meta.l319, Ch21Boards.l319)
object Level320Field : MineLevel(Ch21Meta.l320, Ch21Boards.l320, subtle = true)

val Chapter21 = listOf(
    Level301Mines, Level302Numbers, Level303Flags, Level304Corner, Level305Count,
    Level306ItSays, Level307ThreeSays, Level308Cross, Level309FlagsOnly, Level310Sunglasses,
    Level311CrossFlags, Level312AllSaid, Level313Mask, Level314Notes, Level315LastMines,
    Level316CrossLies, Level317BigMap, Level318FlagExam, Level319TwoNotes, Level320Field,
)
