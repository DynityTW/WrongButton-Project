package tw.dynity.wrongbutton.core.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell

/**
 * 必須更新時蓋在整個遊戲上面：擋住所有點擊與返回鍵，只留「立即更新」。
 * 進度、紀錄和 Pro 都存在手機上（Pro 也在 Google 帳號），更新後都會保留。
 */
@Composable
fun ForcedUpdateScreen(onUpdate: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .safeDrawingPadding()
            .padding(24.dp)
            .testTag("forced_update"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        ButtonCharacter(Mood.Normal, Tell.Honest, 120.dp)
        Text(stringResource(R.string.update_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(
            stringResource(R.string.update_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp),
        )
        Button(onClick = onUpdate, modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = 52.dp).testTag("forced_update_now")) {
            Text(stringResource(R.string.update_now))
        }
    }
}
