package tw.dynity.wrongbutton.app

import android.app.Application
import tw.dynity.wrongbutton.BuildConfig
import tw.dynity.wrongbutton.core.crash.CrashReport

class WrongButtonApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    /** 這次啟動時，上一次是不是閃退了（閃退後的這一次不載入廣告，確保玩家能把紀錄寄出來）。 */
    var crashedLastTime = false
        private set

    /** 最早的時間點：在 Google 程式庫的自動初始化（ContentProvider）之前，就開始記錄閃退。 */
    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
        CrashReport.install(this, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        crashedLastTime = CrashReport.pending() != null
    }
}
