package tw.dynity.wrongbutton.core.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.ui.WbDialog

/** 客服信箱（網站、條款、App 都用這一個）。 */
object Contact {
    const val SUPPORT_EMAIL = "support@luarvex.com"

    /**
     * 打開 email App 寫信給客服。依序嘗試：
     * 1. mailto: 網址（收件人、主旨、內文都放在網址裡，大部分 email App 都看得懂）；
     * 2. 一般的分享（讓玩家選要用哪個 App 寄）。
     * 都沒有 App 能處理時回傳 false，畫面要改成顯示信箱讓玩家自己寄。
     */
    fun sendEmail(context: Context, subject: String, body: String): Boolean {
        val uri = Uri.parse("mailto:$SUPPORT_EMAIL?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
        val mailto = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        if (runCatching { context.startActivity(mailto) }.isSuccess) return true
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        return runCatching { context.startActivity(Intent.createChooser(share, subject)) }.isSuccess
    }

    fun copy(context: Context, text: String): Boolean = runCatching {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("Wrong Button", text))
        true
    }.getOrDefault(false)
}

/**
 * 上一次閃退時，開啟遊戲後問玩家要不要把錯誤紀錄寄給我們。
 * 只有玩家按「寄給我們」才會打開 email App，由玩家自己決定要不要送出。
 */
@Composable
fun CrashReportPrompt() {
    var report by remember { mutableStateOf(CrashReport.pending()) }
    val text = report ?: return
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    fun done() {
        CrashReport.clear()
        report = null
    }
    WbDialog(face = Expression(Mood.Sad, Tell.Honest)) {
        Column(Modifier.testTag("crash_report"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.crash_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.crash_body), style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = {
                    if (sendEmail(context, text)) done() else copied = copyToClipboard(context, text)
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("crash_send"),
            ) { Text(stringResource(R.string.crash_send)) }
            OutlinedButton(
                onClick = { copied = copyToClipboard(context, text) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("crash_copy"),
            ) { Text(stringResource(if (copied) R.string.crash_copied else R.string.crash_copy, Contact.SUPPORT_EMAIL)) }
            TextButton(onClick = ::done, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("crash_dismiss")) {
                Text(stringResource(R.string.crash_dismiss))
            }
        }
    }
}

private fun sendEmail(context: Context, text: String): Boolean =
    Contact.sendEmail(context, context.getString(R.string.crash_email_subject), text)

private fun copyToClipboard(context: Context, text: String): Boolean = Contact.copy(context, text)
