package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.levels.s3.Chapter25
import tw.dynity.wrongbutton.levels.s3.WireLevel
import tw.dynity.wrongbutton.levels.s3.WireQuiz

/** 第 25 章〈開關與線路〉：照玩家的方式解完（聽它說、讀臉、扳開關、通電）。 */
@RunWith(AndroidJUnit4::class)
class Chapter25PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun WireQuiz.result(on: Int): Int = (0 until lamps).sumOf { j ->
        val hits = Integer.bitCount(on and wiring[j])
        val lit = if (xor) hits % 2 == 1 else hits > 0
        if (lit) 1 shl j else 0
    }

    @Test
    fun trialsMatchTheHiddenWiring() {
        Chapter25.forEach { level ->
            val q = (level as WireLevel).quiz
            assertEquals("第 ${level.id} 關答案", q.target, q.result(q.answer))
            assertEquals("第 ${level.id} 關只有一組開關剛好成功", listOf(q.answer), (1 until (1 shl q.switches)).filter { q.result(it) == q.target })
            q.trials.forEachIndexed { k, t ->
                repeat(q.lamps) { j ->
                    val truth = q.result(t.switches) shr j and 1 == t.lamps shr j and 1
                    assertEquals("第 ${level.id} 關實驗 ${k + 1} 燈 ${j + 1}", k * q.lamps + j !in q.lies, truth)
                }
            }
            assertTrue(q.hiddenLies > 0 || q.lies.all { it in q.spoken })
        }
    }

    @Test
    fun everyCircuitCanBeSolved() = with(Player(rule)) {
        clearChapters(8)
        openLevel(Chapter25.first().id)
        val start = memory.totalMistakes
        Chapter25.forEach { level ->
            val q = (level as WireLevel).quiz
            waitFor("wq_power")
            q.spoken.sorted().forEach { cell ->
                tap("wq_cell_${cell / q.lamps + 1}_${cell % q.lamps + 1}")
                rule.waitForIdle()
                assertEquals("第 ${level.id} 關格子 $cell 的臉", cell in q.lies, lying(desc(node("the_button"))))
            }
            if (level.id == 381) {
                tap("wq_power") // 什麼都沒開：算一次錯
                rule.waitForIdle()
                assertEquals(start + 1, memory.totalMistakes)
            }
            repeat(q.switches) { i -> if (q.answer shr i and 1 == 1) tap("wq_sw_${"ABCDE"[i]}") }
            tap("wq_power")
            if (level != Chapter25.last()) finished(level.id) else rule.waitUntil(15_000) { exists("result_title") }
            assertTrue(memory.isCleared(level.id))
        }
        assertEquals(start + 1, memory.totalMistakes)
    }
}
