package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.RunResult
import tw.dynity.wrongbutton.levels.Levels

/** 模擬的提示廣告與 Pro：從畫面上走一次。 */
@RunWith(AndroidJUnit4::class)
class MonetizationFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun firstHintIsFreeThenEachTierShowsOneMockAd() = with(Player(rule)) {
        waitFor("menu_levels")
        openLevel(Levels.all.first().id)
        tap("host_hint")
        tap("hint_reveal")
        rule.waitForIdle()
        assertFalse("第 1 階提示不用看廣告", exists("mock_ad"))
        tap("hint_reveal")
        waitFor("mock_ad")
        assertFalse("倒數結束前不能領取", enabled("mock_ad_claim"))
        advance(6_000)
        rule.waitUntil(5_000) { enabled("mock_ad_claim") }
        tap("mock_ad_claim")
        rule.waitUntil(5_000) { !exists("mock_ad") }
        assertEquals(2, container.monetization.state.value.adHints[Levels.all.first().id])
        // 中途關掉廣告：不給第 2 階提示，也不算錯。
        val mistakes = memory.totalMistakes
        tap("hint_reveal")
        waitFor("mock_ad")
        tap("mock_ad_close")
        rule.waitUntil(5_000) { !exists("mock_ad") }
        assertEquals(2, container.monetization.state.value.adHints[Levels.all.first().id])
        assertEquals(mistakes, memory.totalMistakes)
    }

    @Test
    fun proUnlocksLevel101AndRemovesAds() = with(Player(rule)) {
        waitFor("menu_levels")
        val fake = FakeStore(container.monetization)
        container.store = fake
        val first = Levels.all[Levels.FREE_LEVELS]
        container.memory.update { m -> Levels.all.take(Levels.FREE_LEVELS).fold(m) { acc, l -> MemoryOps.completeLevel(acc, l.id, RunResult(0, 0, 1, 0, false)) } }
        press("menu_levels")
        waitFor("select_grid")
        rule.onNodeWithTag("select_grid").performScrollToKey(first.id)
        rule.onNodeWithTag("select_level_${first.id}").performClick()
        // 沒買 Pro：打開的是 Pro 頁，不是關卡。價格來自 Google Play（這裡是測試用的假付費）。
        waitFor("pro_screen")
        assertTrue(exists("pro_price"))
        assertFalse("App 沒有模擬付款的按鈕", exists("pro_revoke"))
        tap("pro_buy")
        tap("pro_confirm_buy")
        rule.waitUntil(5_000) { container.monetization.state.value.pro }
        assertEquals(1, fake.purchases)
        waitFor("pro_owned")
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor("select_grid")
        rule.onNodeWithTag("select_level_${first.id}").performClick()
        waitFor("host_hint")
        tap("host_hint")
        tap("hint_reveal")
        tap("hint_reveal")
        rule.waitForIdle()
        assertFalse("Pro 不用看廣告", exists("mock_ad"))
    }

    @Test
    fun fullScreenAdPlaysBetweenLevelsAfterEveryFifthClear() = with(Player(rule)) {
        waitFor("menu_levels")
        container.monetization.update { it.copy(clearsSinceAd = 4) }
        openLevel(Levels.all.first().id)
        waitFor("the_button")
        assertFalse("關卡進行中不插播", exists("mock_ad"))
        press("the_button")
        waitFor("result_title")
        assertFalse("結算畫面本身不插播", exists("mock_ad"))
        assertTrue(container.monetization.adBreakDue())
        press("result_next")
        waitFor("mock_ad")
        assertFalse("倒數完才能關", enabled("mock_ad_break_close"))
        advance(6_000)
        rule.waitUntil(5_000) { enabled("mock_ad_break_close") }
        tap("mock_ad_break_close")
        rule.waitUntil(5_000) { !exists("mock_ad") }
        waitFor("l02_list")
        assertEquals(0, container.monetization.state.value.clearsSinceAd)
        assertFalse(container.monetization.adBreakDue())
    }

    @Test
    fun proNeverSeesFullScreenAds() = with(Player(rule)) {
        waitFor("menu_levels")
        grantPro()
        container.monetization.update { it.copy(clearsSinceAd = 4) }
        openLevel(Levels.all.first().id)
        waitFor("the_button")
        press("the_button")
        waitFor("result_title")
        press("result_next")
        waitFor("l02_list")
        assertFalse(exists("mock_ad"))
    }
}
