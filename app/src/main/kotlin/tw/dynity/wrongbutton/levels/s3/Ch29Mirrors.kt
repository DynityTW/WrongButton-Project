package tw.dynity.wrongbutton.levels.s3

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
 * 鏡子（第 29 章）。[cells]：. 空、/ 或 \ 固定鏡子（畫面上顯示、它說的方向）、R 可以轉的鏡子。
 * 雷射從第 [row] 列的左邊射進來；The Button 在出口（[exitSide] 0 右、1 下、2 左、3 上；[exitPos] 第幾列／行）。
 * [answer] 是可轉鏡子（由上到下、由左到右）唯一正確的方向。[lies] 的固定鏡子，真正的方向和它說的相反。
 */
data class MirrorQuiz(
    val size: Int,
    val cells: List<String>,
    val row: Int,
    val exitSide: Int,
    val exitPos: Int,
    val answer: List<String>,
    val spoken: Set<Int> = emptySet(),
    val lies: Set<Int> = emptySet(),
) {
    val rotatable: List<Int> get() = cells.indices.filter { cells[it] == "R" }

    fun name(i: Int) = "${'A' + i % size}${i / size + 1}"

    /** 依目前可轉鏡子的方向射出雷射，回傳 (出口邊, 位置, 經過的格子)；繞圈時出口是 null。 */
    fun trace(setting: Map<Int, String>): Triple<Int?, Int, List<Int>> {
        var r = row
        var c = 0
        var d = 0
        val dr = intArrayOf(0, 1, 0, -1)
        val dc = intArrayOf(1, 0, -1, 0)
        val seen = HashSet<Int>()
        val path = mutableListOf<Int>()
        while (r in 0 until size && c in 0 until size) {
            val i = r * size + c
            if (!seen.add(i * 4 + d)) return Triple(null, 0, path)
            path += i
            val m = when (val v = cells[i]) {
                "R" -> setting.getValue(i)
                "/", "\\" -> if (i in lies) (if (v == "/") "\\" else "/") else v
                else -> "."
            }
            d = when (m) {
                "/" -> intArrayOf(3, 2, 1, 0)[d]
                "\\" -> intArrayOf(1, 0, 3, 2)[d]
                else -> d
            }
            r += dr[d]; c += dc[d]
        }
        return when {
            c >= size -> Triple(0, r, path)
            r >= size -> Triple(1, c, path)
            c < 0 -> Triple(2, r, path)
            else -> Triple(3, c, path)
        }
    }
}

@Serializable
data class MirrorState(val setting: Map<Int, String> = emptyMap(), val heard: Set<Int> = emptySet(), val beam: List<Int> = emptyList())

abstract class MirrorLevel(meta: S2Meta, val quiz: MirrorQuiz, private val cover: FaceCover = FaceCover.None, private val subtle: Boolean = false) : S2Level(meta) {
    override val chrome: LevelChrome get() = LevelChrome(speechBubble = false)
    override val ambientChatter: Boolean get() = quiz.spoken.isEmpty()

    @Composable
    override fun Content(scope: LevelScope) = MirrorField(scope, quiz, cover, subtle)
}

@Composable
fun MirrorField(scope: LevelScope, q: MirrorQuiz, cover: FaceCover, subtle: Boolean) {
    val res = rememberResources()
    var state by rememberLevelState(MirrorState.serializer()) { MirrorState(q.rotatable.associateWith { "/" }) }

    fun mirrorName(m: String) = res.getString(if (m == "/") R.string.ch29_slash else R.string.ch29_backslash)

    LevelColumn(spacing = 10.dp) { compact ->
        S3Face(scope, if (compact) 56.dp else 64.dp, cover)
        RuleCard(
            res.getString(R.string.ch29_rule_intro),
            res.getString(R.string.ch29_rule_mirrors),
            res.getString(R.string.ch29_rule_fire),
            *(if (q.spoken.isNotEmpty()) arrayOf(res.getString(R.string.ch29_rule_spoken)) else emptyArray()),
        )
        if (q.spoken.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
                q.spoken.sorted().forEach { i ->
                    val heard = i in state.heard
                    AskChip(
                        if (heard) res.getString(R.string.ch29_heard_short, q.name(i), q.cells[i]) else res.getString(R.string.ch29_ask_short, q.name(i)),
                        tag = "mr_ask_${q.name(i)}",
                    ) {
                        state = state.copy(heard = state.heard + i)
                        scope.claim(res.getString(R.string.ch29_says, q.name(i), mirrorName(q.cells[i])), truth = i !in q.lies, subtle = subtle)
                    }
                }
            }
        }
        BoxWithConstraints(Modifier.widthIn(max = 460.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val n = q.size + 2
            val cell: Dp = min(48.dp, maxWidth / n)
            Column {
                for (r in -1..q.size) {
                    Row {
                        for (c in -1..q.size) {
                            val inside = r in 0 until q.size && c in 0 until q.size
                            when {
                                inside -> MirrorCell(q, state, r * q.size + c, cell, res, ::mirrorName) { i ->
                                    if (q.cells[i] == "R" && state.beam.isEmpty()) {
                                        scope.cue(Cue.Tap)
                                        state = state.copy(setting = state.setting + (i to if (state.setting[i] == "/") "\\" else "/"))
                                    } else scope.cue(Cue.Tap)
                                }
                                r == q.row && c == -1 -> Box(Modifier.size(cell).semantics { contentDescription = res.getString(R.string.ch29_laser_cd, q.row + 1) }, contentAlignment = Alignment.Center) {
                                    Text("▶", color = Color(0xFFE53935), fontSize = (cell.value * 0.5f).sp)
                                }
                                isExit(q, r, c) -> Box(Modifier.size(cell).semantics { contentDescription = res.getString(R.string.ch29_target_cd) }.testTag("mr_target"), contentAlignment = Alignment.Center) {
                                    ButtonCharacter(Mood.Normal, Tell.Honest, cell * 0.8f, describe = false)
                                }
                                else -> Box(Modifier.size(cell))
                            }
                        }
                    }
                }
            }
        }
        Button(
            onClick = {
                when {
                    q.spoken.any { it !in state.heard } -> scope.say(Line.Res(R.string.ch29_listen_first), Mood.Normal)
                    else -> {
                        val (side, pos, path) = q.trace(state.setting)
                        if (side == q.exitSide && pos == q.exitPos) {
                            state = state.copy(beam = path)
                            scope.cue(Cue.Success)
                            scope.complete()
                        } else {
                            scope.mistake("mr_miss", Line.Res(R.string.ch29_miss), Mood.Smug)
                        }
                    }
                }
            },
            modifier = Modifier.heightIn(min = 48.dp).testTag("mr_fire"),
        ) { Text(stringResource(R.string.ch29_fire)) }
    }
}

private fun isExit(q: MirrorQuiz, r: Int, c: Int) = when (q.exitSide) {
    0 -> c == q.size && r == q.exitPos
    1 -> r == q.size && c == q.exitPos
    2 -> c == -1 && r == q.exitPos
    else -> r == -1 && c == q.exitPos
}

@Composable
private fun MirrorCell(q: MirrorQuiz, state: MirrorState, i: Int, size: Dp, res: android.content.res.Resources, mirrorName: (String) -> String, onTap: (Int) -> Unit) {
    val v = q.cells[i]
    val rot = v == "R"
    val spoken = i in q.spoken
    val unheard = spoken && i !in state.heard
    val shown = if (rot) state.setting[i] ?: "/" else v
    val lit = i in state.beam
    val desc = q.name(i) + "：" + when {
        rot -> res.getString(R.string.ch29_cd_rot, mirrorName(shown))
        unheard -> res.getString(R.string.ch29_cd_unheard)
        v == "." -> res.getString(R.string.ch29_cd_empty)
        else -> res.getString(R.string.ch29_cd_fixed, mirrorName(v)) + if (spoken) res.getString(R.string.ch29_cd_said) else ""
    }
    val line = MaterialTheme.colorScheme.onSurface
    Box(
        Modifier
            .size(size)
            .background(
                when {
                    lit -> Color(0xFFFFCDD2)
                    rot -> Color(0xFFCFD8DC)
                    spoken -> AskColor
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
                RoundedCornerShape(4.dp),
            )
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            .clickable(role = if (rot) Role.Button else Role.Image) { onTap(i) }
            .semantics { contentDescription = desc }
            .testTag("mr_${q.name(i)}"),
        contentAlignment = Alignment.Center,
    ) {
        if (unheard) {
            Text("?", color = AskTextColor, fontWeight = FontWeight.Bold)
        } else if (shown == "/" || shown == "\\") {
            Canvas(Modifier.fillMaxSize()) {
                val p = size.toPx() * 0.15f
                val w = this.size.width
                val h = this.size.height
                if (shown == "/") drawLine(if (rot) Color(0xFF37474F) else line, Offset(p, h - p), Offset(w - p, p), strokeWidth = if (rot) 6f else 4f)
                else drawLine(if (rot) Color(0xFF37474F) else line, Offset(p, p), Offset(w - p, h - p), strokeWidth = if (rot) 6f else 4f)
            }
        }
    }
}

// ---- 關卡 ----

object Level461Mirror : MirrorLevel(Ch29Meta.l461, Ch29Puzzles.l461)
object Level462Three : MirrorLevel(Ch29Meta.l462, Ch29Puzzles.l462)
object Level463Bounce : MirrorLevel(Ch29Meta.l463, Ch29Puzzles.l463)
object Level464Four : MirrorLevel(Ch29Meta.l464, Ch29Puzzles.l464)
object Level465ItSays : MirrorLevel(Ch29Meta.l465, Ch29Puzzles.l465)
object Level466Five : MirrorLevel(Ch29Meta.l466, Ch29Puzzles.l466)
object Level467SixGrid : MirrorLevel(Ch29Meta.l467, Ch29Puzzles.l467)
object Level468Sunglasses : MirrorLevel(Ch29Meta.l468, Ch29Puzzles.l468, cover = FaceCover.Eyes, subtle = true)
object Level469Zigzag : MirrorLevel(Ch29Meta.l469, Ch29Puzzles.l469)
object Level470Spoken : MirrorLevel(Ch29Meta.l470, Ch29Puzzles.l470)
object Level471TwoLies : MirrorLevel(Ch29Meta.l471, Ch29Puzzles.l471)
object Level472SevenGrid : MirrorLevel(Ch29Meta.l472, Ch29Puzzles.l472)
object Level473Seven : MirrorLevel(Ch29Meta.l473, Ch29Puzzles.l473)
object Level474Mask : MirrorLevel(Ch29Meta.l474, Ch29Puzzles.l474, cover = FaceCover.Mouth, subtle = true)
object Level475Funhouse : MirrorLevel(Ch29Meta.l475, Ch29Puzzles.l475)
object Level476Eight : MirrorLevel(Ch29Meta.l476, Ch29Puzzles.l476)
object Level477Maze : MirrorLevel(Ch29Meta.l477, Ch29Puzzles.l477, subtle = true)
object Level478Hall : MirrorLevel(Ch29Meta.l478, Ch29Puzzles.l478, subtle = true)
object Level479Nine : MirrorLevel(Ch29Meta.l479, Ch29Puzzles.l479, cover = FaceCover.Eyes, subtle = true)
object Level480MirrorMirror : MirrorLevel(Ch29Meta.l480, Ch29Puzzles.l480, subtle = true)

val Chapter29 = listOf(
    Level461Mirror, Level462Three, Level463Bounce, Level464Four, Level465ItSays,
    Level466Five, Level467SixGrid, Level468Sunglasses, Level469Zigzag, Level470Spoken,
    Level471TwoLies, Level472SevenGrid, Level473Seven, Level474Mask, Level475Funhouse,
    Level476Eight, Level477Maze, Level478Hall, Level479Nine, Level480MirrorMirror,
)
