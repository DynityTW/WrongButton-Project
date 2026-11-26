package tw.dynity.wrongbutton.levels.s3

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.FaceCover
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.LevelScope

/**
 * 第三季共用：上方的 The Button（可遮住眼睛或嘴巴）與對話泡泡。
 * 泡泡不顯示小臉，免得遮住的部分從小臉洩漏出來。[onPress] 不為 null 時，The Button 可以按。
 */
@Composable
fun S3Face(scope: LevelScope, size: Dp, cover: FaceCover = FaceCover.None, onPress: (() -> Unit)? = null) {
    Box(contentAlignment = Alignment.TopCenter) {
        ButtonCharacter(
            scope.expression.mood, scope.expression.tell, size, cover = cover,
            modifier = Modifier.testTag("the_button").then(if (onPress != null) Modifier.clickable(role = Role.Button, onClick = onPress) else Modifier),
        )
        if (cover == FaceCover.Mouth) {
            // 口罩：只看得到眼睛。
            Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFE3F2FD), modifier = Modifier.offset(y = size * 0.52f).size(size * 0.62f, size * 0.3f)) {}
        }
    }
    scope.SpeechBubble(Modifier.widthIn(max = 420.dp), showFace = false)
}

/** 讓 The Button 說一句話：[truth] 為 false 時是謊話，臉會露餡（[subtle] 時幅度很小）。 */
fun LevelScope.claim(text: String, truth: Boolean, subtle: Boolean = false) =
    say(Line.Text(text), if (truth) Mood.Normal else Mood.Smug, if (truth) Tell.Honest else if (subtle) Tell.LieSubtle else Tell.LieObvious)

/** 一張說明卡。 */
@Composable
fun RuleCard(vararg lines: String, tag: String = "s3_rules") {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().testTag(tag)) {
        androidx.compose.foundation.layout.Column(Modifier.padding(12.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
            lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

/** 「點一下聽它說」的橘色條。 */
@Composable
fun AskBar(text: String, tag: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(8.dp), color = Color(0xFFFFE0B2),
        modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().heightIn(min = 48.dp).testTag(tag),
    ) { Text(text, color = Color(0xFF6D3B00), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(12.dp)) }
}

val AskColor = Color(0xFFFFE0B2)
val AskTextColor = Color(0xFF6D3B00)

/** 第三季的按鈕顏色：紅、藍、綠、黃、紫、橘。 */
val S3Colors = listOf(Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFDD835), Color(0xFF8E24AA), Color(0xFFFB8C00))

/** 比 [AskBar] 小的「聽它說」按鈕，可以好幾個排成一行。 */
@Composable
fun AskChip(text: String, tag: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = AskColor, modifier = Modifier.heightIn(min = 48.dp).testTag(tag)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp)) {
            Text(text, color = AskTextColor, style = MaterialTheme.typography.labelLarge)
        }
    }
}
