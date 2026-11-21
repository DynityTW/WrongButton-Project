package tw.dynity.wrongbutton.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tw.dynity.wrongbutton.core.audio.Feedback
import tw.dynity.wrongbutton.core.audio.MusicPlayer
import tw.dynity.wrongbutton.core.audio.SoundPlayer
import tw.dynity.wrongbutton.core.audio.AssetMusicPlayer
import tw.dynity.wrongbutton.core.audio.AssetSoundPlayer
import tw.dynity.wrongbutton.core.dialogue.DialogueDirector
import tw.dynity.wrongbutton.core.engine.Clock
import tw.dynity.wrongbutton.core.engine.ResourceTextSource
import tw.dynity.wrongbutton.core.engine.TextSource
import tw.dynity.wrongbutton.core.memory.JsonDataStores
import tw.dynity.wrongbutton.core.memory.MemoryMigrations
import tw.dynity.wrongbutton.core.memory.MemoryRepository
import tw.dynity.wrongbutton.core.memory.PlayerMemory
import tw.dynity.wrongbutton.core.settings.GameSettings
import tw.dynity.wrongbutton.core.settings.SettingsRepository
import java.io.File

/** 手寫的依賴容器：整個 App 只有一份。 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    /** 背景工作（存檔、結帳查詢）出錯時記錄下來，但不讓遊戲閃退。 */
    val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO +
            kotlinx.coroutines.CoroutineExceptionHandler { _, e -> tw.dynity.wrongbutton.core.crash.CrashReport.recordNonFatal(e) },
    )

    val memory = MemoryRepository(
        JsonDataStores.get(File(appContext.filesDir, "datastore/player_memory.json"), PlayerMemory(), PlayerMemory.serializer(), scope, MemoryMigrations),
        scope,
    )

    val settings = SettingsRepository(
        JsonDataStores.get(File(appContext.filesDir, "datastore/settings.json"), GameSettings(), GameSettings.serializer(), scope),
        scope,
    )

    val monetization = tw.dynity.wrongbutton.core.monetization.MonetizationRepository(
        JsonDataStores.get(
            File(appContext.filesDir, "datastore/monetization.json"),
            tw.dynity.wrongbutton.core.monetization.MonetizationState(),
            tw.dynity.wrongbutton.core.monetization.MonetizationState.serializer(),
            scope,
        ),
        scope,
    )
    /** true：Google Play 結帳與 AdMob（正式版）。false：模擬（debug 版與自動測試）。 */
    val realMonetization = tw.dynity.wrongbutton.BuildConfig.REAL_MONETIZATION

    /** 模擬廣告。畫面最上層的覆蓋層讀它；正式版不會用到（current 永遠是 null）。 */
    val mockAds = tw.dynity.wrongbutton.core.monetization.MockAdService()

    /** AdMob。只有正式版有。 */
    val adMob: tw.dynity.wrongbutton.core.monetization.AdMobService? =
        if (realMonetization) {
            tw.dynity.wrongbutton.core.monetization.AdMobService(
                appContext,
                tw.dynity.wrongbutton.BuildConfig.ADMOB_REWARDED_ID,
                tw.dynity.wrongbutton.BuildConfig.ADMOB_INTERSTITIAL_ID,
            )
        } else {
            null
        }

    val ads: tw.dynity.wrongbutton.core.monetization.AdService = adMob ?: mockAds

    val store: tw.dynity.wrongbutton.core.monetization.Store =
        if (realMonetization) {
            runCatching { tw.dynity.wrongbutton.core.monetization.PlayBillingStore(appContext, monetization, scope) }
                .getOrElse { e ->
                    tw.dynity.wrongbutton.core.crash.CrashReport.recordNonFatal(e)
                    tw.dynity.wrongbutton.core.monetization.UnavailableStore()
                }
        } else {
            tw.dynity.wrongbutton.core.monetization.MockStore(monetization)
        }

    val hintGate = tw.dynity.wrongbutton.core.monetization.AdHintGate(monetization, ads)

    val signals = tw.dynity.wrongbutton.core.system.SystemSignals()

    /** 從桌面捷徑或小工具按下、還沒處理的來源。導覽層讀到後會清成 null，並決定要不要直接打開對應的關卡。 */
    val externalOpen = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    /** 記下一次「從外面按下」。記憶還沒讀進來時先等它讀完，免得被初始讀取蓋掉。 */
    fun recordExternalPress(from: String) {
        scope.launch {
            memory.isReady.first { it }
            val key = tw.dynity.wrongbutton.core.system.ExternalPress.key(from)
            memory.update { m -> tw.dynity.wrongbutton.core.memory.MemoryOps.remember(m, key, tw.dynity.wrongbutton.core.memory.MemoryValue.Num((m.long(key) ?: 0) + 1)) }
            externalOpen.value = from
        }
    }

    val director = DialogueDirector()
    val text: TextSource = ResourceTextSource(appContext.resources)
    val clock = Clock { android.os.SystemClock.elapsedRealtime() }

    val sound: SoundPlayer = AssetSoundPlayer(appContext)
    val music: MusicPlayer = AssetMusicPlayer(appContext)
    val feedback = Feedback(sound) { settings.settings.value.vibration }
}
