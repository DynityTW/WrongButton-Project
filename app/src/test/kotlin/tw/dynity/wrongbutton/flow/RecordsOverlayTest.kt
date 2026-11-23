package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity

/** 關卡裡的「紀錄」：返回箭頭固定在最上面，紀錄捲到最下面也看得到、按得到。 */
@RunWith(AndroidJUnit4::class)
class RecordsOverlayTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun backArrowStaysOnTopWhileScrolled() = with(Player(rule)) {
        clearChapters(4)
        openLevel(1)
        waitFor("host_pause")
        press("host_pause")
        tap("pause_records")
        waitFor("records_back")
        // 捲到各關紀錄最下面，返回箭頭還是在畫面上。
        rule.onNodeWithTag("records_levels").performScrollTo()
        rule.onNodeWithTag("records_back").assertIsDisplayed()
        press("records_back")
        waitFor("pause_records")
    }
}
