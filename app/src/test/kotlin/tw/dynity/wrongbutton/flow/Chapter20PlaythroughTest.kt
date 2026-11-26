package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
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
import tw.dynity.wrongbutton.levels.s2.Level299WhichTab

/** 第 20 章〈迷路〉：用每一種導覽方式找到 The Button；第 300 關之後沒有下一關。 */
@RunWith(AndroidJUnit4::class)
class Chapter20PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun everyNavigationPatternLeadsHome() = with(Player(rule)) {
        clearChapters(8)
        container.memory.update { m -> (121..285).fold(m) { acc, id -> MemoryOps.completeLevel(acc, id, RunResult(0, 0, 1, 0, false)) } }
        openLevel(286)
        val start = memory.totalMistakes
        fun t(vararg tags: String) = tags.forEach { waitFor(it); tap(it); rule.waitForIdle() }

        waitFor("tab_0")
        for (i in 0..2) { t("tab_$i"); if (exists("the_button")) break }
        t("the_button"); finished(286)
        t("menu", "drawer_fav", "the_button"); finished(287)
        t("nav_back", "the_button"); finished(288)
        t("crumb_1", "the_button"); finished(289)
        t("article_link", "the_button"); finished(290)
        waitFor("address"); rule.onNodeWithTag("address").performTextInput("wb://button"); t("address_go", "the_button"); finished(291)
        t("link_history", "link_bhome", "the_button"); finished(292)
        t("tab_1", "the_button"); finished(293)
        t("modal_login") // 這裡沒有帳號：算一次錯
        assertEquals(start + 1, memory.totalMistakes)
        t("modal_guest", "tab_2", "the_button"); finished(294)
        t("top_14", "the_button"); finished(295)
        t("menu", "group_more", "drawer_adv", "the_button"); finished(296)
        t("close_x", "tab_2", "the_button"); finished(297)
        t("link_r1", "link_r3", "link_r5", "the_button"); finished(298)
        t("opt_${Level299WhichTab.answer(memory)}"); finished(299)
        t("modal_guest", "menu", "group_more", "drawer_adv")
        assertFalse("去連結頁之前，The Button 不在「進階」", exists("the_button"))
        t("article_link", "nav_back", "the_button")
        rule.waitUntil(15_000) { exists("result_title") }
        assertTrue(memory.isCleared(300))
        assertEquals("第 300 關是第二季最後一關", 300, Levels.chapters.filter { it.season == 2 }.last().levels.last().id)
        assertEquals("只有故意按登入那一次算錯", start + 1, memory.totalMistakes)
    }
}
