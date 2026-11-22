package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.core.legal.LegalKind

/** 服務條款與隱私權政策在 Luarvex 官網：主畫面、設定、Pro 頁的連結都用瀏覽器打開正確的網址。 */
@RunWith(AndroidJUnit4::class)
class LegalFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun lastOpenedUrl(): String? {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        var url: String? = null
        while (true) {
            val intent = org.robolectric.Shadows.shadowOf(app).nextStartedActivity ?: break
            if (intent.action == android.content.Intent.ACTION_VIEW) url = intent.dataString
        }
        return url
    }

    @Test
    fun urlsPointAtLuarvex() {
        assertEquals("https://luarvex.com/products/wrongbutton/terms/", LegalKind.Terms.url)
        assertEquals("https://luarvex.com/products/wrongbutton/privacy/", LegalKind.Privacy.url)
    }

    @Test
    fun contactRowOpensAnEmailToSupportWithTheVersion() = with(Player(rule)) {
        waitFor("menu_levels")
        tap("menu_settings")
        tap("settings_contact")
        rule.waitForIdle()
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val intent = org.robolectric.Shadows.shadowOf(app).nextStartedActivity
        assertEquals(android.content.Intent.ACTION_SENDTO, intent.action)
        assertEquals(listOf("support@luarvex.com"), intent.getStringArrayExtra(android.content.Intent.EXTRA_EMAIL)!!.toList())
        assertTrue(intent.getStringExtra(android.content.Intent.EXTRA_TEXT)!!.contains(tw.dynity.wrongbutton.BuildConfig.VERSION_NAME))
    }

    @Test
    fun withoutAnEmailAppTheAddressIsShownInstead() = with(Player(rule)) {
        waitFor("menu_levels")
        // 模擬「手機上沒有能寄信的 App」：沒有對應的 App 時 startActivity 會丟例外。
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        org.robolectric.Shadows.shadowOf(app).checkActivities(true)
        tap("menu_settings")
        tap("settings_contact")
        waitFor("contact_fallback")
        assertTrue(exists("contact_address"))
        tap("contact_copy_address")
        rule.waitForIdle()
        val clip = app.getSystemService(android.content.ClipboardManager::class.java).primaryClip!!.getItemAt(0).text.toString()
        assertEquals("support@luarvex.com", clip)
    }

    @Test
    fun titleSettingsAndProOpenTheWebsites() = with(Player(rule)) {
        waitFor("menu_levels")
        assertTrue(exists("legal_consent"))
        rule.onNodeWithTag("menu_terms").performScrollTo()
        tap("menu_terms")
        rule.waitForIdle()
        assertEquals(LegalKind.Terms.url, lastOpenedUrl())
        rule.onNodeWithTag("menu_privacy").performScrollTo()
        tap("menu_privacy")
        rule.waitForIdle()
        assertEquals(LegalKind.Privacy.url, lastOpenedUrl())
        press("menu_settings")
        tap("settings_terms")
        rule.waitForIdle()
        assertEquals(LegalKind.Terms.url, lastOpenedUrl())
        tap("settings_privacy")
        rule.waitForIdle()
        assertEquals(LegalKind.Privacy.url, lastOpenedUrl())
        tap("settings_pro")
        waitFor("pro_screen")
        tap("pro_terms")
        rule.waitForIdle()
        assertEquals(LegalKind.Terms.url, lastOpenedUrl())
    }
}

/** 閃退紀錄：寫在手機上，下次開啟時詢問；按「不用了」就刪除。 */
@RunWith(AndroidJUnit4::class)
class CrashReportTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun nonFatalErrorIsKeptLocallyAndOfferedOnTheNextScreen() = with(Player(rule)) {
        waitFor("menu_levels")
        tw.dynity.wrongbutton.core.crash.CrashReport.recordNonFatal(IllegalStateException("測試用的錯誤"))
        val text = tw.dynity.wrongbutton.core.crash.CrashReport.pending()
        assertTrue(text!!.contains("測試用的錯誤"))
        assertTrue("不含遊戲紀錄", !text.contains("records"))
        // 這次啟動時才寫入的紀錄，下次開啟才會問；這裡直接重建畫面模擬「下次開啟」。
        rule.activityRule.scenario.recreate()
        waitFor("crash_report")
        tap("crash_dismiss")
        rule.waitUntil(5_000) { !exists("crash_report") }
        assertEquals(null, tw.dynity.wrongbutton.core.crash.CrashReport.pending())
    }
}
