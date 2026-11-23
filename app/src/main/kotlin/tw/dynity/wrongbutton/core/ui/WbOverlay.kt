package tw.dynity.wrongbutton.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Expression

/**
 * 遊戲內的覆蓋層（不是系統 Dialog 視窗）。
 * 系統返回手勢不會被它攔截，所以永遠能直接離開關卡。
 */
@Composable
fun WbOverlay(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 420.dp,
    /** 固定在最上面、不跟著內容捲動的列（例如返回箭頭與標題）。 */
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            // 吃掉點擊，避免穿透到底下的關卡。用手勢偵測而不是 clickable：
            // clickable 會把整個對話框合併成一個無障礙節點，TalkBack 就讀不出個別項目。
            .pointerInput(Unit) { detectTapGestures { } }
            .safeDrawingPadding()
            .semantics { isTraversalGroup = true },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = maxWidth)
                .fillMaxWidth(),
        ) {
            Column {
                if (header != null) {
                    Box(Modifier.fillMaxWidth().padding(start = 12.dp, end = 24.dp, top = 12.dp)) { header() }
                }
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = if (header != null) 8.dp else 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
            }
        }
    }
}

/** 遊戲風格的對話框：上方一定看得到 The Button 的臉。 */
@Composable
fun WbDialog(
    face: Expression,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    WbOverlay(modifier) {
        ButtonCharacter(
            mood = face.mood,
            tell = face.tell,
            size = 72.dp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        content()
    }
}
