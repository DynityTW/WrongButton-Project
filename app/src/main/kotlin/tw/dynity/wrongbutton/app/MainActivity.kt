package tw.dynity.wrongbutton.app

import android.content.res.Resources
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.compose.foundation.isSystemInDarkTheme
import tw.dynity.wrongbutton.core.settings.ThemeMode
import tw.dynity.wrongbutton.core.system.ExternalPress
import tw.dynity.wrongbutton.core.system.LocalSystemSignals
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tw.dynity.wrongbutton.core.audio.LocalFeedback
import tw.dynity.wrongbutton.core.settings.PronounResources
import tw.dynity.wrongbutton.core.settings.PronounText
import tw.dynity.wrongbutton.core.settings.SystemMotion
import tw.dynity.wrongbutton.ui.theme.WrongButtonTheme

class MainActivity : ComponentActivity() {
    private val container get() = (application as WrongButtonApp).container
    private var pronounResources: PronounResources? = null

    /** 所有文字都經過 [PronounResources]，依設定把「妳」換成「你」。 */
    override fun getResources(): Resources {
        val base = super.getResources()
        val cached = pronounResources
        return if (cached != null && cached.base === base) cached else PronounResources(base).also { pronounResources = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleExternal(intent)
        // 正式版：先取得廣告同意（需要時顯示 Google 的同意表單），之後才載入廣告。
        // 上一次閃退的話，這一次先不載入廣告，讓玩家可以把錯誤紀錄寄出來。
        if (!(application as WrongButtonApp).crashedLastTime) container.adMob?.gatherConsent(this)
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle()
            val systemReduce = remember { SystemMotion.animationsDisabled(this) }
            LaunchedEffect(settings.pronoun) {
                // 稱呼改了：重建 Activity，畫面上的文字全部重讀（關卡狀態與返回堆疊都會保留）。
                if (PronounText.mode != settings.pronoun) {
                    PronounText.mode = settings.pronoun
                    recreate()
                }
            }
            LaunchedEffect(settings.sound, settings.music) {
                container.sound.enabled = settings.sound
                container.music.enabled = settings.music
            }
            val dark = when (settings.theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            WrongButtonTheme(reduceMotion = settings.reduceMotion || systemReduce, darkTheme = dark) {
                CompositionLocalProvider(LocalFeedback provides container.feedback, LocalSystemSignals provides container.signals) {
                    WrongButtonNavigation(container)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        container.adMob?.attach(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleExternal(intent)
    }

    /** 桌面捷徑、桌面小工具送來的「按下 The Button」。 */
    private fun handleExternal(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == ExternalPress.SCHEME && data.host == ExternalPress.HOST) {
            container.recordExternalPress(data.getQueryParameter("from") ?: ExternalPress.FROM_SHORTCUT)
            intent.data = null
        }
    }

    /** 實體音量鍵：只有關卡要求時才交給遊戲（第 9 章）。 */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        container.signals.onKeyEvent(event) || super.dispatchKeyEvent(event)

    override fun onStart() {
        super.onStart()
        container.music.onForeground()
    }

    override fun onStop() {
        container.music.onBackground()
        super.onStop()
    }
}
