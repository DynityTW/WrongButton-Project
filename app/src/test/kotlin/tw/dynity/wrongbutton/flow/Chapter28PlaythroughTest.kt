package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.levels.s3.Chapter28
import tw.dynity.wrongbutton.levels.s3.PinLevel

/** 第 28 章〈密碼規則〉：照玩家的方式設定密碼（聽它說、讀臉、轉數字）。 */
@RunWith(AndroidJUnit4::class)
class Chapter28PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun everyPasswordCanBeSet() = with(Player(rule)) {
        clearChapters(8)
        openLevel(Chapter28.first().id)
        val start = memory.totalMistakes
        Chapter28.forEach { level ->
            val q = (level as PinLevel).quiz
            assertTrue(q.hiddenLies > 0 || q.lies.all { it in q.spoken })
            waitFor("pw_confirm")
            q.spoken.sorted().forEach { j ->
                tap("pw_rule_${j + 1}")
                rule.waitForIdle()
                assertEquals("第 ${level.id} 關規則 ${j + 1} 的臉", j in q.lies, lying(desc(node("the_button"))))
            }
            if (level.id == 441) {
                tap("pw_confirm")
                rule.waitForIdle()
                assertEquals(start + 1, memory.totalMistakes)
            }
            q.digits.forEachIndexed { k, d ->
                if (d <= 5) repeat(d) { tap("pw_up_${k + 1}") } else repeat(10 - d) { tap("pw_down_${k + 1}") }
            }
            tap("pw_confirm")
            if (level != Chapter28.last()) finished(level.id) else rule.waitUntil(15_000) { exists("result_title") }
            assertTrue(memory.isCleared(level.id))
        }
        assertEquals(start + 1, memory.totalMistakes)
    }
}
