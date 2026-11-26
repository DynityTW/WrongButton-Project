package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.levels.s3.Chapter26
import tw.dynity.wrongbutton.levels.s3.OrderLevel

/** 第 26 章〈排順序〉：照玩家的方式排完（聽它說、讀臉、點格子換按鈕與飲料）。 */
@RunWith(AndroidJUnit4::class)
class Chapter26PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun answersArePermutations() {
        Chapter26.forEach { level ->
            val q = (level as OrderLevel).quiz
            assertEquals((0 until q.n).toList(), q.colors.sorted())
            q.drinks?.let { assertEquals((0 until q.n).toList(), it.sorted()) }
            assertTrue(q.hiddenLies > 0 || q.lies.all { it in q.spoken })
        }
    }

    @Test
    fun everyQueueCanBeArranged() = with(Player(rule)) {
        clearChapters(8)
        openLevel(Chapter26.first().id)
        val start = memory.totalMistakes
        Chapter26.forEach { level ->
            val q = (level as OrderLevel).quiz
            waitFor("os_confirm")
            q.spoken.sorted().forEach { j ->
                tap("os_clue_${j + 1}")
                rule.waitForIdle()
                assertEquals("第 ${level.id} 關線索 ${j + 1} 的臉", j in q.lies, lying(desc(node("the_button"))))
            }
            if (level.id == 401) {
                tap("os_confirm")
                rule.waitForIdle()
                assertEquals(start + 1, memory.totalMistakes)
            }
            q.colors.forEachIndexed { k, c -> repeat(c + 1) { tap("os_c_${k + 1}") } }
            q.drinks?.forEachIndexed { k, d -> repeat(d + 1) { tap("os_d_${k + 1}") } }
            tap("os_confirm")
            rule.waitForIdle()
            if (level != Chapter26.last()) finished(level.id) else rule.waitUntil(15_000) { exists("result_title") }
            assertTrue(memory.isCleared(level.id))
        }
        assertEquals(start + 1, memory.totalMistakes)
    }
}
