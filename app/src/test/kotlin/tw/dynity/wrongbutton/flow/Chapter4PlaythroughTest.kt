package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowSystemClock
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.levels.Level56Storage
import tw.dynity.wrongbutton.levels.Level59Diary
import tw.dynity.wrongbutton.levels.Levels
import java.time.Duration

/** 第 4 章連續玩完：需要查紀錄的地方，照紀錄頁（記憶）作答。 */
@RunWith(AndroidJUnit4::class)
class Chapter4PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun chapter4CanBeCleared() = with(Player(rule)) {
        clearChapters(3)
        openLevel(8)

        // 46〈妳錯了幾次〉：照紀錄的錯誤總數（0）送出
        waitFor("l08_submit")
        repeat(memory.totalMistakes) { tap("l08_plus") }
        tap("l08_submit")
        finished(8)

        // 47〈忘掉一件事〉：到紀錄頁滑掉那一列
        waitFor("the_button"); advance(1_500)
        press("host_pause"); tap("pause_records")
        waitFor("forget_l49_grudge")
        rule.onNodeWithTag("forget_l49_grudge").performScrollTo().performTouchInput { swipeLeft() }
        advance(500)
        press("records_back")
        rule.onNodeWithText("繼續").performScrollTo().performClick()
        advance(500)
        press("the_button")
        finished(49)

        // 48〈貼上〉
        waitFor("l50_message")
        rule.onNodeWithTag("l50_message").performTouchInput { longClick() }
        press("l50_copy"); tap("l50_paste"); tap("l50_submit")
        finished(50)

        // 49〈幫我保管〉
        waitFor("l51_shape_1"); press("l51_shape_1")
        finished(51)

        // 50〈它記錯了〉：長按說謊臉的那一列
        waitFor("l52_entry_0")
        val fake = (0..5).filter { exists("l52_entry_$it") }.first { lying(desc(node("l52_entry_$it"))) }
        rule.onNodeWithTag("l52_entry_$fake").performScrollTo().performTouchInput { longClick() }
        press("l52_report")
        finished(52)

        // 51〈復原〉
        waitFor("l53_undo"); repeat(3) { press("l53_undo") }; tap("the_button")
        finished(53)

        // 52〈成就〉：故意按錯三次
        waitFor("l54_colleague"); repeat(3) { tap("l54_colleague") }
        finished(54)

        // 53〈草稿〉：離開再回來
        waitFor("the_button"); advance(2_000)
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor("select_grid")
        rule.onNodeWithTag("select_grid").performScrollToKey(55)
        press("select_level_55")
        waitFor("the_button"); advance(4_000)
        press("the_button")
        finished(55)

        // 54〈裝不下了〉：點每一項聽它說，誠實說「可以」的才刪
        waitFor("l56_usage")
        val ok = Level56Storage.items.indices.filter { i ->
            tap("l56_item_$i"); advance(200)
            !lying(desc(node("speech_face")))
        }
        rule.onNodeWithTag("l56_item_${ok.first()}").performScrollTo().performTouchInput { longClick() }
        ok.drop(1).forEach { tap("l56_item_$it") }
        tap("l56_trash")
        finished(56)

        // 55〈第一關〉：等得比第 1 關久
        waitFor("the_button")
        ShadowSystemClock.advanceBy(Duration.ofSeconds(4))
        advance(4_000)
        press("the_button")
        finished(57)

        // 56〈我很了解妳〉：沒有紀錄 → 它預測向右、左側；第三題選跟第二題同一邊
        waitFor("the_button"); advance(500)
        rule.onNodeWithTag("the_button").performTouchInput { swipeLeft() }
        advance(3_000)
        rule.waitUntil(5_000) { enabled("l09_right") }; tap("l09_right"); advance(3_000)
        rule.waitUntil(5_000) { enabled("l09_right") }; tap("l09_right"); advance(3_000)
        finished(9)

        // 57〈打開膠囊〉：第 49 關選的是 ●
        waitFor("l58_shape_1"); press("l58_shape_1")
        finished(58)

        // 58〈它的日記〉
        waitFor("l59_save")
        val blanks = Level59Diary.Rules.blanks(memory, "不記得", "左", "右", "勾「不要再問我」", "按 20 次確定")
        blanks.forEachIndexed { i, b -> tap("l59_opt_${i}_${b.options.indexOf(b.answer)}") }
        tap("l59_save")
        finished(59)

        // 59〈訪客〉
        waitFor("l60_avatar"); press("l60_avatar"); press("l60_as_guest"); press("the_button")
        finished(60)

        // 60〈妳是誰〉：四項都對得上紀錄的那張
        waitFor("l61_profile_0")
        val truths = listOf("第 1 關：— 秒", "錯誤總數：${memory.totalMistakes} 次", "滑掉方向：右", "成就：故意按錯")
        val right = (0..2).first { i -> truths.all { t -> texts(node("l61_profile_$i")).contains(t) } }
        tap("l61_profile_$right")
        waitFor("msg_0")
        val honest = (0..2).first { !lying(desc(node("msg_$it"))) }
        tap("msg_$honest")
        finished(61)

        assertTrue("第 4 章全部通關", Levels.chapters[3].levels.all { memory.isCleared(it.id) })
    }
}
