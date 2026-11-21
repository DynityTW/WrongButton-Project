package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.core.monetization.ReviewAccess

/**
 * 審查用解鎖碼。測試用假碼，真的解鎖碼不會出現在這個公開的 repo 裡。
 */
@RunWith(AndroidJUnit4::class)
class ReviewAccessTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val original = ReviewAccess.expectedHash
    private val fakeCode = "test-only-Review-Code-1"

    @After
    fun restore() {
        ReviewAccess.expectedHash = original
    }

    @Test
    fun realHashIsNotTheFakeCodeAndWhitespaceIsIgnored() {
        assertEquals(64, original.length)
        // 和產生正式雜湊用的同一套算法（SHA-256、加鹽、小寫十六進位）。
        assertEquals("db075d490b684cf99617b17b2df1c1c7a981ac4bab1e7ce5fb4585b63d522ad2", ReviewAccess.hash(fakeCode))
        assertFalse(ReviewAccess.matches(fakeCode))
        ReviewAccess.expectedHash = ReviewAccess.hash(fakeCode)
        assertTrue(ReviewAccess.matches("  $fakeCode "))
        assertFalse(ReviewAccess.matches(fakeCode.lowercase()))
    }

    @Test
    fun sevenTapsOnTheVersionOpenTheCodeBoxAndTheRightCodeUnlocksPro() = with(Player(rule)) {
        ReviewAccess.expectedHash = ReviewAccess.hash(fakeCode)
        waitFor("menu_levels")
        container.monetization.revokeProForTesting()
        tap("menu_settings")
        waitFor("settings_version")
        val version = rule.onNodeWithTag("settings_version").performScrollTo()
        repeat(6) { version.performClick() }
        assertFalse("點 6 下還不會出現", exists("review_code"))
        version.performClick()
        waitFor("review_code")

        rule.onNodeWithTag("review_code").performTextInput("wrong-code")
        press("review_submit")
        rule.waitForIdle()
        assertFalse(container.monetization.state.value.pro)
        assertFalse(exists("review_done"))

        rule.onNodeWithTag("review_code").performTextReplacement(fakeCode)
        press("review_submit")
        waitFor("review_done")
        val state = container.monetization.state.value
        assertTrue(state.pro && state.reviewAccess)

        // Google Play 查不到購買紀錄時（restore 會呼叫 setPro(false)），審查用的開通不會被收回。
        container.monetization.setPro(false)
        assertTrue(container.monetization.state.value.pro)
    }
}
