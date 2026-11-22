package tw.dynity.wrongbutton.core.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 版本號規則：versionCode ＝ 主 × 10000 ＋ 次 × 100 ＋ 修正（0.8.0 → 800，見 app/build.gradle.kts）。
 * Google Play 上的新版本，只要「主版本或次版本」比現在的大，就要求強制更新；只差修正版不強制。
 */
object VersionRules {
    fun majorMinor(versionCode: Int): Int = versionCode / 100

    fun mustUpdate(currentCode: Int, availableCode: Int): Boolean = majorMinor(availableCode) > majorMinor(currentCode)

    fun name(versionCode: Int): String = "${versionCode / 10000}.${versionCode / 100 % 100}.${versionCode % 100}"
}

/** 強制更新的檢查。只有從 Google Play 安裝的正式版才查得到；其他情況（側載、測試、沒網路）一律放行。 */
interface UpdateChecker {
    /** true：必須更新才能繼續玩，畫面最上層會蓋一張「需要更新」。 */
    val required: StateFlow<Boolean>

    /** 每次回到前景時檢查；需要時直接打開 Google Play 的全螢幕更新畫面。 */
    suspend fun check(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>)

    /** 「需要更新」畫面上的按鈕：再打開一次 Google Play 的更新畫面，失敗就開商店頁。 */
    suspend fun retry(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>)

    companion object {
        val None = object : UpdateChecker {
            override val required: StateFlow<Boolean> = MutableStateFlow(false)
            override suspend fun check(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>) = Unit
            override suspend fun retry(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>) = Unit
        }
    }
}

/** Google Play 的應用程式內更新（立即更新＝Google 的全螢幕更新畫面，更新完會自動重新開啟遊戲）。 */
class PlayUpdateChecker(context: Context, private val currentVersionCode: Int) : UpdateChecker {
    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(context.applicationContext)
    private val _required = MutableStateFlow(false)
    override val required: StateFlow<Boolean> = _required.asStateFlow()

    override suspend fun check(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val info = runCatching { manager.requestAppUpdateInfo() }.getOrNull() ?: return
        val availability = info.updateAvailability()
        val newer = availability == UpdateAvailability.UPDATE_AVAILABLE ||
            availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
        if (!newer || !VersionRules.mustUpdate(currentVersionCode, info.availableVersionCode())) {
            _required.value = false
            return
        }
        _required.value = true
        if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            runCatching { manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE)) }
        }
    }

    override suspend fun retry(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val info = runCatching { manager.requestAppUpdateInfo() }.getOrNull()
        val started = info != null && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) &&
            runCatching { manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE)) }.getOrDefault(false)
        if (!started) openStore(activity)
    }

    private fun openStore(activity: Activity) {
        val id = activity.packageName
        runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id"))) }
            .recoverCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id"))) }
    }
}
