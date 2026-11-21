package tw.dynity.wrongbutton.core.legal

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * 服務條款與隱私權政策放在 Luarvex 官網，App 不內建副本，直接用瀏覽器打開，
 * 所以玩家看到的永遠是最新版本。
 */
enum class LegalKind(val url: String) {
    Terms("https://luarvex.com/products/wrongbutton/terms/"),
    Privacy("https://luarvex.com/products/wrongbutton/privacy/"),
}

object LegalLinks {
    /** 用瀏覽器打開。手機上沒有瀏覽器時回傳 false（不會閃退）。 */
    fun open(context: Context, kind: LegalKind): Boolean = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(kind.url))
        if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
