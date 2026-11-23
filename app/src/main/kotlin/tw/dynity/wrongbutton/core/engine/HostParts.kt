package tw.dynity.wrongbutton.core.engine

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.settings.LocalReduceMotion
import tw.dynity.wrongbutton.core.ui.WbDialog
import tw.dynity.wrongbutton.core.ui.WbIcons
import tw.dynity.wrongbutton.core.ui.WbOverlay
import tw.dynity.wrongbutton.ui.theme.TaskTextStyle

@Composable
fun HostTopBar(title: String, onPause: () -> Unit, onHint: () -> Unit, modifier: Modifier = Modifier, hintFace: Boolean = false) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPause, modifier = Modifier.testTag("host_pause")) {
            Icon(WbIcons.Pause, contentDescription = stringResource(R.string.host_pause_cd))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onHint, modifier = Modifier.testTag("host_hint")) {
            if (hintFace) {
                val cd = stringResource(R.string.host_hint_face_cd)
                tw.dynity.wrongbutton.core.character.ButtonCharacter(
                    tw.dynity.wrongbutton.core.character.Mood.Normal, tw.dynity.wrongbutton.core.character.Tell.Honest, 28.dp,
                    modifier = Modifier.semantics { contentDescription = cd }, describe = false,
                )
            } else {
                Icon(WbIcons.Hint, contentDescription = stringResource(R.string.host_hint_cd))
            }
        }
    }
}

@Composable
fun TaskCard(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(stringResource(R.string.host_task_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = TaskTextStyle)
        }
    }
}

/** The Button 的對話泡泡：左邊的小臉和本體表情一致。 */
@Composable
fun SpeechBubbleView(speech: Speech?, modifier: Modifier = Modifier, showFace: Boolean = true) {
    val reduceMotion = LocalReduceMotion.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val face = speech?.expression ?: Expression.Default
        // 遮擋關卡不顯示小臉，否則泡泡會洩漏被遮住的表情。
        // 小臉也提供外觀描述：只有泡泡在畫面上時，TalkBack 使用者才讀得到這句話的表情。
        if (showFace) ButtonCharacter(face.mood, face.tell, 40.dp, modifier = Modifier.testTag("speech_face"))
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.weight(1f),
        ) {
            AnimatedContent(
                targetState = speech,
                transitionSpec = {
                    if (reduceMotion) fadeIn(initialAlpha = 1f) togetherWith fadeOut(targetAlpha = 1f)
                    else fadeIn() togetherWith fadeOut()
                },
                label = "speech",
            ) { s ->
                Text(
                    s?.text ?: "……",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("speech")
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

@Composable
fun PauseOverlay(
    recordsBadge: Boolean,
    onResume: () -> Unit,
    onRecords: () -> Unit,
    onExit: () -> Unit,
) {
    WbOverlay {
        Text(stringResource(R.string.pause_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Button(onClick = onResume, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.pause_resume)) }
        val badgeCd = stringResource(R.string.pause_records_badge_cd)
        FilledTonalButton(
            onClick = onRecords,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("pause_records")
                .then(if (recordsBadge) Modifier.semantics { contentDescription = badgeCd } else Modifier),
        ) {
            if (recordsBadge) {
                BadgedBox(badge = { Badge(Modifier.size(8.dp)) }) { Text(stringResource(R.string.pause_records)) }
            } else {
                Text(stringResource(R.string.pause_records))
            }
        }
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.pause_exit)) }
    }
}

@Composable
fun RecordsOverlay(onBack: () -> Unit, content: @Composable () -> Unit) {
    // 返回放在最上面、固定不動（和主選單的紀錄頁一樣是箭頭），紀錄很長時不用捲到底。
    WbOverlay(
        maxWidth = 560.dp,
        header = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("records_back")) {
                    Icon(WbIcons.Back, contentDescription = stringResource(R.string.records_back))
                }
                Text(stringResource(R.string.records_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            }
        },
    ) {
        content()
    }
}

@Composable
fun HintOverlay(
    hints: List<String>,
    tier: Int,
    face: Expression,
    onReveal: () -> Unit,
    needsAd: Boolean = false,
    onClose: () -> Unit,
) {
    WbDialog(face = face) {
        Text(stringResource(R.string.hint_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        if (tier == 0) {
            Text(stringResource(R.string.hint_none_yet), style = MaterialTheme.typography.bodyMedium)
        }
        hints.take(tier).forEachIndexed { i, hint ->
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(stringResource(R.string.hint_tier_label, i + 1), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(hint, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (tier < hints.size) {
            Button(onClick = onReveal, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("hint_reveal")) {
                Text(stringResource(if (needsAd) R.string.hint_reveal_ad else R.string.hint_reveal, tier + 1))
            }
            if (needsAd) {
                Text(stringResource(R.string.hint_ad_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.hint_close)) }
    }
}

@Composable
fun ResultScreen(
    result: LevelResult,
    hasNext: Boolean,
    onNext: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    nextNeedsPro: Boolean = false,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val compact = maxHeight < 560.dp
        Column(
            Modifier
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = if (compact) 8.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp),
        ) {
            Text(
                stringResource(R.string.result_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }.testTag("result_title"),
            )
            ButtonCharacter(result.expression.mood, result.expression.tell, if (compact) 72.dp else 120.dp)
            if (result.line.isNotBlank()) {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        result.line,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat(stringResource(R.string.result_time), stringResource(R.string.result_seconds, formatSeconds(result.timeMs)))
                Stat(stringResource(R.string.result_mistakes), stringResource(R.string.result_count, result.mistakes))
            }
            if (result.special) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiary) {
                    Text(
                        stringResource(R.string.result_special),
                        color = MaterialTheme.colorScheme.onTertiary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
            if (!hasNext) {
                Text(stringResource(R.string.result_thanks), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.size(4.dp))
            if (hasNext) {
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("result_next")) {
                    Text(stringResource(if (nextNeedsPro) R.string.result_next_pro else R.string.result_next))
                }
            }
            OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("result_exit")) {
                Text(stringResource(R.string.result_select))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
