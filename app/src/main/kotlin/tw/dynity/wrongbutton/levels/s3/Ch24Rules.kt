package tw.dynity.wrongbutton.levels.s3

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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

// ---------------------------------------------------------------- 找規則

/** 一張範例圖片與它真正的標籤（符合規則與否）。 */
data class Example(val tile: Tile, val fits: Boolean)

/**
 * 找規則（第 361–370 關）。[spoken] 的範例要點一下聽 The Button 說它符不符合；[lies] 裡的它會說反。
 * 六張 [candidates] 要一張一張判斷符不符合；[answers] 是符合的那幾張（可能不只一張），全部判斷對才算過。
 */
data class RuleQuiz(val examples: List<Example>, val candidates: List<Tile>, val answers: Set<Int>, val spoken: Set<Int> = emptySet(), val lies: Set<Int> = emptySet())

@Serializable
data class RuleState(val heard: Set<Int> = emptySet(), val fits: Set<Int> = emptySet())

abstract class RuleLevel(meta: S2Meta, val quiz: RuleQuiz, private val cover: FaceCover = FaceCover.None, private val subtle: Boolean = false) : S2Level(meta) {
    override val chrome: LevelChrome get() = LevelChrome(speechBubble = false)
    override val ambientChatter: Boolean get() = quiz.spoken.isEmpty()

    @Composable
    override fun Content(scope: LevelScope) = RuleField(scope, quiz, cover, subtle)
}

@Composable
fun RuleField(scope: LevelScope, q: RuleQuiz, cover: FaceCover, subtle: Boolean) {
    val res = rememberResources()
    var state by rememberLevelState(RuleState.serializer()) { RuleState() }
    LevelColumn(spacing = 10.dp) { compact ->
        S3Face(scope, if (compact) 56.dp else 72.dp, cover)
        RuleCard(res.getString(R.string.ch24_rule_intro), if (q.spoken.isNotEmpty()) res.getString(R.string.ch24_rule_spoken) else res.getString(R.string.ch24_rule_plain))
        Text(stringResource(R.string.ch24_examples), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
            q.examples.forEachIndexed { j, ex ->
                val spoken = j in q.spoken
                val heard = j in state.heard
                val said = if (j in q.lies) !ex.fits else ex.fits
                val label = when {
                    spoken && !heard -> "?"
                    said -> "✓"
                    else -> "✗"
                }
                val desc = tileDescription(res, ex.tile) + "，" + when {
                    spoken && !heard -> res.getString(R.string.ch24_cd_ask)
                    spoken -> res.getString(if (said) R.string.ch24_cd_said_fits else R.string.ch24_cd_said_not)
                    said -> res.getString(R.string.ch24_cd_fits)
                    else -> res.getString(R.string.ch24_cd_not)
                }
                Column(
                    Modifier
                        .width(72.dp)
                        .background(if (spoken) AskColor else MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                        .clickable(enabled = spoken, role = Role.Button) {
                            state = state.copy(heard = state.heard + j)
                            // 它說的是 said：謊話時和真正的標籤相反，臉會露餡。
                            scope.claim(res.getString(if (said) R.string.ch24_says_fits else R.string.ch24_says_not), truth = j !in q.lies, subtle = subtle)
                        }
                        .semantics(mergeDescendants = true) { contentDescription = desc }
                        .testTag("rq_ex_${j + 1}")
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Canvas(Modifier.size(56.dp)) { drawTile(ex.tile) }
                    Text(
                        label, fontWeight = FontWeight.Bold,
                        color = when (label) { "✓" -> Color(0xFF2E7D32); "✗" -> Color(0xFFC62828); else -> AskTextColor },
                    )
                }
            }
        }
        Text(stringResource(R.string.ch24_candidates), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
            q.candidates.forEachIndexed { i, t ->
                val fits = i in state.fits
                val letter = "ABCDEF"[i].toString()
                val desc = res.getString(R.string.ch24_cand_cd, letter, tileDescription(res, t)) + "，" + res.getString(if (fits) R.string.ch24_judged_fits else R.string.ch24_judged_not)
                Column(
                    Modifier
                        .width(92.dp)
                        .background(if (fits) Color(0xFFC8E6C9) else MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(10.dp))
                        .border(2.dp, if (fits) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable(role = Role.Switch) {
                            scope.cue(Cue.Tap)
                            state = state.copy(fits = if (fits) state.fits - i else state.fits + i)
                        }
                        .semantics(mergeDescendants = true) { contentDescription = desc }
                        .testTag("rq_cand_${i + 1}")
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(letter, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Canvas(Modifier.size(60.dp)) { drawTile(t) }
                    Text(
                        if (fits) "✓ " + stringResource(R.string.ch24_judged_fits) else "✗ " + stringResource(R.string.ch24_judged_not),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (fits) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(stringResource(R.string.ch24_judge_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = {
                when {
                    q.spoken.any { it !in state.heard } -> scope.say(Line.Res(R.string.ch24_listen_first), Mood.Normal)
                    state.fits == q.answers -> { scope.cue(Cue.Success); scope.complete() }
                    else -> scope.mistake("rq_wrong", Line.Res(R.string.ch24_wrong), Mood.Smug)
                }
            },
            modifier = Modifier.heightIn(min = 48.dp).testTag("rq_confirm"),
        ) { Text(stringResource(R.string.ch24_confirm)) }
    }
}

fun tileDescription(res: android.content.res.Resources, t: Tile): String =
    res.getString(R.string.ch24_tile_cd, t.count, res.getStringArray(R.array.ch22_colors)[t.color], res.getStringArray(R.array.ch22_shapes)[t.shape])

// ---------------------------------------------------------------- 猜密碼

/** 一次之前的猜測與它的回饋（● 位置也對、○ 顏色對位置不對）。 */
data class Guess(val code: List<Int>, val black: Int, val white: Int)

/**
 * 猜密碼（第 371–380 關）。[spoken] 時，[lies] 裡的回饋是 The Button 說的謊話（真正的回饋一定不是那個）。
 * [hiddenLies] > 0：看不到臉，只知道剛好幾行是假的。
 */
data class CodeQuiz(
    val colors: Int,
    val secret: List<Int>,
    val rows: List<Guess>,
    val lies: Set<Int> = emptySet(),
    val hiddenLies: Int = 0,
    val spoken: Boolean = false,
    val dup: Boolean = false,
)

@Serializable
data class CodeState(val input: List<Int> = emptyList(), val heard: Set<Int> = emptySet())

private val CodeColors = listOf(Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFDD835), Color(0xFF8E24AA), Color(0xFFFB8C00))

abstract class CodeLevel(meta: S2Meta, val quiz: CodeQuiz, private val cover: FaceCover = FaceCover.None, private val subtle: Boolean = false) : S2Level(meta) {
    override val chrome: LevelChrome get() = LevelChrome(speechBubble = false)
    override val ambientChatter: Boolean get() = !quiz.spoken

    @Composable
    override fun Content(scope: LevelScope) = CodeField(scope, quiz, cover, subtle)
}

@Composable
fun CodeField(scope: LevelScope, q: CodeQuiz, cover: FaceCover, subtle: Boolean) {
    val res = rememberResources()
    val names = res.getStringArray(R.array.ch24_code_colors)
    val length = q.secret.size
    var state by rememberLevelState(CodeState.serializer()) { CodeState(List(length) { -1 }) }

    fun fb(b: Int, w: Int) = "●".repeat(b) + "○".repeat(w) + if (b + w == 0) res.getString(R.string.ch24_none) else ""

    LevelColumn(spacing = 10.dp) { compact ->
        S3Face(scope, if (compact) 56.dp else 72.dp, cover)
        RuleCard(
            res.getString(R.string.ch24_code_intro, length, q.colors),
            res.getString(if (q.dup) R.string.ch24_code_dup else R.string.ch24_code_nodup),
            res.getString(R.string.ch24_code_feedback),
            *(if (q.spoken) arrayOf(res.getString(R.string.ch24_code_spoken)) else emptyArray()),
            *(if (q.hiddenLies > 0) arrayOf(res.getString(R.string.ch24_code_hidden, q.hiddenLies)) else emptyArray()),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
            q.rows.forEachIndexed { j, g ->
                val spoken = j in q.spokenRows()
                val heard = j in state.heard
                val codeText = g.code.joinToString("") { names[it] }
                val desc = res.getString(R.string.ch24_row_cd, j + 1, codeText) + when {
                    spoken && !heard -> res.getString(R.string.ch24_row_ask)
                    else -> res.getString(R.string.ch24_row_fb, g.black, g.white) + if (spoken) res.getString(R.string.ch24_row_said) else ""
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .background(if (spoken) AskColor else MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                        .clickable(enabled = spoken, role = Role.Button) {
                            state = state.copy(heard = state.heard + j)
                            scope.claim(res.getString(R.string.ch24_says_fb, g.black, g.white), truth = j !in q.lies, subtle = subtle)
                        }
                        .semantics(mergeDescendants = true) { contentDescription = desc }
                        .testTag("cq_row_${j + 1}")
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("${j + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(20.dp))
                    g.code.forEach { c -> Peg(c, names) }
                    Box(Modifier.weight(1f))
                    Text(if (spoken && !heard) "?" else fb(g.black, g.white), style = MaterialTheme.typography.titleMedium, color = if (spoken) AskTextColor else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Text(stringResource(R.string.ch24_code_answer), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            state.input.forEachIndexed { k, c ->
                val desc = res.getString(R.string.ch24_slot_cd, k + 1, if (c < 0) res.getString(R.string.ch24_empty) else names[c])
                Box(
                    Modifier
                        .size(52.dp)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .background(if (c < 0) Color.Transparent else CodeColors[c], CircleShape)
                        .clickable(role = Role.Button) {
                            scope.cue(Cue.Tap)
                            state = state.copy(input = state.input.toMutableList().also { it[k] = (c + 1) % q.colors })
                        }
                        .semantics { contentDescription = desc }
                        .testTag("cq_slot_${k + 1}"),
                    contentAlignment = Alignment.Center,
                ) { if (c >= 0) Text(names[c], color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
        Text(stringResource(R.string.ch24_slot_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = {
                val spokenRows = q.spokenRows()
                when {
                    spokenRows.any { it !in state.heard } -> scope.say(Line.Res(R.string.ch24_listen_first), Mood.Normal)
                    state.input == q.secret -> { scope.cue(Cue.Success); scope.complete() }
                    else -> scope.mistake("cq_wrong", Line.Res(R.string.ch24_wrong), Mood.Smug)
                }
            },
            modifier = Modifier.heightIn(min = 48.dp).testTag("cq_confirm"),
        ) { Text(stringResource(R.string.ch24_confirm)) }
    }
}

@Composable
private fun Peg(c: Int, names: Array<String>) {
    Box(Modifier.size(30.dp).background(CodeColors[c], CircleShape), contentAlignment = Alignment.Center) {
        Text(names[c], color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

/** 哪幾行的回饋要聽 The Button 說：謊話的那幾行，加上偶數行（讓謊話藏在真話之間）。 */
fun CodeQuiz.spokenRows(): Set<Int> = if (!spoken) emptySet() else lies + rows.indices.filter { it % 2 == 1 }

// ---- 關卡 ----

object Level361WhatRule : RuleLevel(Ch24Meta.l361, Ch24Puzzles.l361)
object Level362Another : RuleLevel(Ch24Meta.l362, Ch24Puzzles.l362)
object Level363Not : RuleLevel(Ch24Meta.l363, Ch24Puzzles.l363)
object Level364And : RuleLevel(Ch24Meta.l364, Ch24Puzzles.l364)
object Level365But : RuleLevel(Ch24Meta.l365, Ch24Puzzles.l365)
object Level366ItLabels : RuleLevel(Ch24Meta.l366, Ch24Puzzles.l366)
object Level367Or : RuleLevel(Ch24Meta.l367, Ch24Puzzles.l367)
object Level368Sunglasses : RuleLevel(Ch24Meta.l368, Ch24Puzzles.l368, cover = FaceCover.Eyes, subtle = true)
object Level369TwoWrong : RuleLevel(Ch24Meta.l369, Ch24Puzzles.l369, subtle = true)
object Level370RuleMaster : RuleLevel(Ch24Meta.l370, Ch24Puzzles.l370, cover = FaceCover.Mouth, subtle = true)
object Level371Code : CodeLevel(Ch24Meta.l371, Ch24Puzzles.l371)
object Level372FiveColors : CodeLevel(Ch24Meta.l372, Ch24Puzzles.l372)
object Level373FourSlots : CodeLevel(Ch24Meta.l373, Ch24Puzzles.l373)
object Level374SixColors : CodeLevel(Ch24Meta.l374, Ch24Puzzles.l374)
object Level375Repeat : CodeLevel(Ch24Meta.l375, Ch24Puzzles.l375)
object Level376ItSays : CodeLevel(Ch24Meta.l376, Ch24Puzzles.l376)
object Level377Sunglasses : CodeLevel(Ch24Meta.l377, Ch24Puzzles.l377, cover = FaceCover.Eyes, subtle = true)
object Level378OneFalse : CodeLevel(Ch24Meta.l378, Ch24Puzzles.l378)
object Level379RepeatLie : CodeLevel(Ch24Meta.l379, Ch24Puzzles.l379, subtle = true)
object Level380Master : CodeLevel(Ch24Meta.l380, Ch24Puzzles.l380, cover = FaceCover.Mouth, subtle = true)

val Chapter24 = listOf(
    Level361WhatRule, Level362Another, Level363Not, Level364And, Level365But,
    Level366ItLabels, Level367Or, Level368Sunglasses, Level369TwoWrong, Level370RuleMaster,
    Level371Code, Level372FiveColors, Level373FourSlots, Level374SixColors, Level375Repeat,
    Level376ItSays, Level377Sunglasses, Level378OneFalse, Level379RepeatLie, Level380Master,
)
