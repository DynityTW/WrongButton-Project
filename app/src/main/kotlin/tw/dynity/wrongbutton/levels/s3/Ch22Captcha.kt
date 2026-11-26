package tw.dynity.wrongbutton.levels.s3

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
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

/** 一格圖片：顏色（紅藍綠黃）、形狀（圓方三角星）、數量（1–3）。 */
data class Tile(val color: Int, val shape: Int, val count: Int)

/**
 * 一題驗證（第 22 章）。[statements] 是關於正確答案的幾句話，[answer] 是正確答案（格子編號從 0 開始）。
 *
 * - [spoken]：每一句都要點一下聽 The Button 說；[lies] 裡的是謊話（說的時候會露出說謊的臉）。
 * - [exactlyTrue]：看不到臉，只知道剛好幾句是真的。
 * - [complementRule]：只有一句「選出所有……」；它說謊時，要選的剛好是相反的那些格子（第 325 關）。
 */
data class Captcha(
    val size: Int,
    val tiles: List<Tile>,
    val statements: List<Int>,
    val answer: Set<Int>,
    val spoken: Boolean = false,
    val lies: Set<Int> = emptySet(),
    val exactlyTrue: Int? = null,
    val cover: FaceCover = FaceCover.None,
    val subtle: Boolean = false,
    val complementRule: Boolean = false,
)

@Serializable
data class CaptchaState(
    val started: Boolean = false,
    val round: Int = 0,
    val selected: Set<Int> = emptySet(),
    val heard: Set<Int> = emptySet(),
    val passed: Boolean = false,
)

abstract class CaptchaLevel(meta: S2Meta, val rounds: List<Captcha>) : S2Level(meta) {
    override val chrome: LevelChrome get() = LevelChrome(speechBubble = false)
    override val ambientChatter: Boolean get() = rounds.none { it.spoken }

    @Composable
    override fun Content(scope: LevelScope) = CaptchaField(scope, rounds)
}

private val TileColors = listOf(Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFDD835))

@Composable
fun CaptchaField(scope: LevelScope, rounds: List<Captcha>) {
    val res = rememberResources()
    var state by rememberLevelState(CaptchaState.serializer()) { CaptchaState() }
    val round = rounds[state.round.coerceAtMost(rounds.lastIndex)]
    val cover = rounds.firstOrNull { it.cover != FaceCover.None }?.cover ?: FaceCover.None
    LaunchedEffect(state.passed) {
        if (state.passed) scope.say(Line.Res(R.string.ch22_ready), Mood.Normal, Tell.Honest)
    }

    // 注意：這兩個函式要在被呼叫的當下讀目前的題目（Compose 會記住函式參照，不能用外面的 round）。
    fun current() = rounds[state.round.coerceAtMost(rounds.lastIndex)]

    fun hear(j: Int) {
        val round = current()
        val lie = j in round.lies
        state = state.copy(heard = state.heard + j)
        scope.say(
            Line.Text(res.getString(round.statements[j])),
            if (lie) Mood.Smug else Mood.Normal,
            if (!lie) Tell.Honest else if (round.subtle) Tell.LieSubtle else Tell.LieObvious,
        )
    }

    fun verify() {
        val round = current()
        if (round.spoken && state.heard.size < round.statements.size) {
            scope.say(Line.Res(R.string.ch22_listen_first), Mood.Normal, Tell.Honest)
            return
        }
        if (state.selected == round.answer) {
            scope.cue(Cue.Success)
            state = if (state.round == rounds.lastIndex) state.copy(passed = true, selected = emptySet())
            else state.copy(round = state.round + 1, selected = emptySet(), heard = emptySet())
            if (!state.passed) scope.say(Line.Res(R.string.ch22_next), Mood.Normal, Tell.Honest)
        } else {
            scope.mistake("cap_wrong", Line.Res(R.string.ch22_wrong), Mood.Smug)
        }
    }

    LevelColumn(spacing = 10.dp) { compact ->
        val faceSize = if (compact || round.size >= 4) 64.dp else 88.dp
        Box(contentAlignment = Alignment.TopCenter) {
            ButtonCharacter(
                scope.expression.mood, scope.expression.tell, faceSize, cover = cover,
                modifier = Modifier.testTag("the_button").clickable(role = Role.Button) {
                    if (state.passed) { scope.cue(Cue.Success); scope.complete() }
                    else scope.mistake("cap_early", Line.Res(R.string.ch22_not_ready), Mood.Guilty)
                },
            )
            if (cover == FaceCover.Mouth) {
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFE3F2FD), modifier = Modifier.offset(y = faceSize * 0.52f).size(faceSize * 0.62f, faceSize * 0.3f)) {}
            }
        }
        scope.SpeechBubble(Modifier.widthIn(max = 420.dp), showFace = false)
        // 「我不是機器人」勾選框
        Surface(
            onClick = { if (!state.started) { scope.cue(Cue.Tap); state = state.copy(started = true) } },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().heightIn(min = 56.dp).testTag("cap_check")
                .semantics { contentDescription = res.getString(if (state.passed) R.string.ch22_checked_cd else R.string.ch22_check_cd) },
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(28.dp).border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                        .background(if (state.passed) Color(0xFF43A047) else Color.Transparent, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center,
                ) { if (state.passed) Text("✓", color = Color.White, fontWeight = FontWeight.Bold) }
                Text(stringResource(R.string.ch22_not_robot), style = MaterialTheme.typography.titleMedium)
            }
        }
        if (state.started && !state.passed) Challenge(round, state, rounds.size, onToggle = { i ->
            scope.cue(Cue.Tap)
            state = state.copy(selected = if (i in state.selected) state.selected - i else state.selected + i)
        }, onHear = ::hear, onVerify = ::verify)
    }
}

@Composable
private fun Challenge(round: Captcha, state: CaptchaState, total: Int, onToggle: (Int) -> Unit, onHear: (Int) -> Unit, onVerify: () -> Unit) {
    val res = rememberResources()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).testTag("cap_card"),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = Color(0xFF1A73E8), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.ch22_round, state.round + 1, total), color = Color.White, style = MaterialTheme.typography.labelLarge)
                    val header = when {
                        round.complementRule -> R.string.ch22_header_complement
                        round.spoken -> R.string.ch22_header_spoken
                        round.exactlyTrue != null -> R.string.ch22_header_exactly
                        round.statements.size == 1 -> R.string.ch22_header_one
                        else -> R.string.ch22_header_all
                    }
                    Text(
                        if (round.exactlyTrue != null) stringResource(header, round.statements.size, round.exactlyTrue) else stringResource(header),
                        color = Color.White, style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag("cap_header"),
                    )
                }
            }
            round.statements.forEachIndexed { j, text ->
                if (round.spoken) {
                    val heard = j in state.heard
                    Surface(
                        onClick = { onHear(j) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFE0B2),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("cap_say_${j + 1}"),
                    ) {
                        Text(
                            if (heard) stringResource(R.string.ch22_heard, j + 1, stringResource(text)) else stringResource(R.string.ch22_ask, j + 1),
                            color = Color(0xFF6D3B00), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(12.dp),
                        )
                    }
                } else {
                    Text(stringResource(R.string.ch22_line, j + 1, stringResource(text)), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("cap_line_${j + 1}"))
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val gap = 4.dp
                val cell: Dp = min(if (round.size >= 4) 84.dp else 104.dp, (maxWidth - gap * (round.size - 1)) / round.size)
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    repeat(round.size) { r ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            repeat(round.size) { c ->
                                val i = r * round.size + c
                                TileView(round.tiles[i], i, i in state.selected, cell, res, onToggle)
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onVerify, modifier = Modifier.heightIn(min = 48.dp).testTag("cap_verify")) { Text(stringResource(R.string.ch22_verify)) }
            }
        }
    }
}

@Composable
private fun TileView(tile: Tile, i: Int, selected: Boolean, size: Dp, res: android.content.res.Resources, onToggle: (Int) -> Unit) {
    val colorName = res.getStringArray(R.array.ch22_colors)[tile.color]
    val shapeName = res.getStringArray(R.array.ch22_shapes)[tile.shape]
    val cd = res.getString(R.string.ch22_tile_cd, i + 1, tile.count, colorName, shapeName) + if (selected) res.getString(R.string.ch22_selected) else ""
    Box(
        Modifier
            .size(size)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(4.dp))
            .border(if (selected) 4.dp else 0.dp, if (selected) Color(0xFF1A73E8) else Color.Transparent, RoundedCornerShape(4.dp))
            .clickable(role = Role.Checkbox) { onToggle(i) }
            .semantics { contentDescription = cd; this.selected = selected }
            .testTag("cap_tile_${i + 1}"),
    ) {
        Canvas(Modifier.fillMaxSize().padding(if (selected) 14.dp else 10.dp)) { drawTile(tile) }
        Text("${i + 1}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.TopStart).padding(4.dp))
        Text(colorName.take(1), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.BottomStart).padding(4.dp))
        if (selected) {
            Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).background(Color(0xFF1A73E8), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Text("✓", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

internal fun DrawScope.drawTile(tile: Tile) {
    val color = TileColors[tile.color]
    val slots = when (tile.count) {
        1 -> listOf(Offset(0.5f, 0.5f))
        2 -> listOf(Offset(0.3f, 0.5f), Offset(0.7f, 0.5f))
        else -> listOf(Offset(0.5f, 0.28f), Offset(0.28f, 0.72f), Offset(0.72f, 0.72f))
    }
    val r = size.minDimension * (if (tile.count == 1) 0.3f else 0.18f)
    slots.forEach { s ->
        val c = Offset(size.width * s.x, size.height * s.y)
        when (tile.shape) {
            0 -> drawCircle(color, r, c)
            1 -> drawRect(color, Offset(c.x - r, c.y - r), androidx.compose.ui.geometry.Size(r * 2, r * 2))
            2 -> drawPath(Path().apply { moveTo(c.x, c.y - r); lineTo(c.x + r, c.y + r * 0.8f); lineTo(c.x - r, c.y + r * 0.8f); close() }, color)
            else -> drawPath(star(c, r), color)
        }
    }
}

private fun star(c: Offset, r: Float): Path = Path().apply {
    for (k in 0 until 10) {
        val rad = if (k % 2 == 0) r else r * 0.45f
        val a = Math.PI / 5 * k - Math.PI / 2
        val x = c.x + (rad * Math.cos(a)).toFloat()
        val y = c.y + (rad * Math.sin(a)).toFloat()
        if (k == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

// ---- 關卡 ----

object Level321NotRobot : CaptchaLevel(Ch22Meta.l321, Ch22Puzzles.l321)
object Level322TwoRounds : CaptchaLevel(Ch22Meta.l322, Ch22Puzzles.l322)
object Level323Every : CaptchaLevel(Ch22Meta.l323, Ch22Puzzles.l323)
object Level324Again : CaptchaLevel(Ch22Meta.l324, Ch22Puzzles.l324)
object Level325ReadAloud : CaptchaLevel(Ch22Meta.l325, Ch22Puzzles.l325)
object Level326ThreeLines : CaptchaLevel(Ch22Meta.l326, Ch22Puzzles.l326)
object Level327AddUp : CaptchaLevel(Ch22Meta.l327, Ch22Puzzles.l327)
object Level328RowsCols : CaptchaLevel(Ch22Meta.l328, Ch22Puzzles.l328)
object Level329Sunglasses : CaptchaLevel(Ch22Meta.l329, Ch22Puzzles.l329)
object Level330OneTrue : CaptchaLevel(Ch22Meta.l330, Ch22Puzzles.l330)
object Level331Apart : CaptchaLevel(Ch22Meta.l331, Ch22Puzzles.l331)
object Level332Bigger : CaptchaLevel(Ch22Meta.l332, Ch22Puzzles.l332)
object Level333TwoLies : CaptchaLevel(Ch22Meta.l333, Ch22Puzzles.l333)
object Level334TwoTrue : CaptchaLevel(Ch22Meta.l334, Ch22Puzzles.l334)
object Level335Numbers : CaptchaLevel(Ch22Meta.l335, Ch22Puzzles.l335)
object Level336Latin : CaptchaLevel(Ch22Meta.l336, Ch22Puzzles.l336)
object Level337Mask : CaptchaLevel(Ch22Meta.l337, Ch22Puzzles.l337)
object Level338NoFace : CaptchaLevel(Ch22Meta.l338, Ch22Puzzles.l338)
object Level339ThreeMore : CaptchaLevel(Ch22Meta.l339, Ch22Puzzles.l339)
object Level340Human : CaptchaLevel(Ch22Meta.l340, Ch22Puzzles.l340)

val Chapter22 = listOf(
    Level321NotRobot, Level322TwoRounds, Level323Every, Level324Again, Level325ReadAloud,
    Level326ThreeLines, Level327AddUp, Level328RowsCols, Level329Sunglasses, Level330OneTrue,
    Level331Apart, Level332Bigger, Level333TwoLies, Level334TwoTrue, Level335Numbers,
    Level336Latin, Level337Mask, Level338NoFace, Level339ThreeMore, Level340Human,
)
