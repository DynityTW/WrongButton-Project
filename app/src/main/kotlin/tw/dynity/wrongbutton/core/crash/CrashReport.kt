package tw.dynity.wrongbutton.core.crash

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 閃退紀錄：只存在手機上，不會自動上傳。
 *
 * 遊戲意外關閉時，把錯誤訊息寫進 App 的私有檔案；下次開啟時問玩家要不要用 email 寄給我們。
 * 內容只有：時間、遊戲版本、手機型號、Android 版本、錯誤訊息。**不含遊戲紀錄、稱呼或任何玩家輸入的文字。**
 * 玩家按「不用了」或寄出之後就刪除。
 */
object CrashReport {
    private const val FILE = "last_crash.txt"
    private const val MAX_CHARS = 12_000
    private var file: File? = null
    private var version: String = "?"

    /** 在 Application.onCreate 最前面呼叫。 */
    fun install(context: Context, versionName: String, versionCode: Int) {
        file = File(context.filesDir, FILE)
        version = "$versionName ($versionCode)"
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(error, fatal = true) }
            previous?.uncaughtException(thread, error)
        }
    }

    /** 沒有讓遊戲關閉、但不該發生的錯誤（例如廣告或結帳 SDK 丟出的例外）。 */
    fun recordNonFatal(error: Throwable) {
        runCatching { write(error, fatal = false) }
    }

    private fun write(error: Throwable, fatal: Boolean) {
        val target = file ?: return
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val text = buildString {
            appendLine(if (fatal) "類型：閃退" else "類型：錯誤（遊戲沒有關閉）")
            appendLine("時間：" + SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date()))
            appendLine("遊戲版本：$version")
            appendLine("手機：${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android：${Build.VERSION.RELEASE}（API ${Build.VERSION.SDK_INT}）")
            appendLine()
            append(trace)
        }
        // 已經有一份還沒處理的閃退紀錄時，不要用比較不嚴重的錯誤蓋掉它。
        if (!fatal && target.exists() && target.readText().startsWith("類型：閃退")) return
        target.writeText(text.take(MAX_CHARS))
    }

    /** 上一次留下、還沒處理的紀錄。 */
    fun pending(): String? = file?.takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }

    fun clear() {
        file?.delete()
    }
}
