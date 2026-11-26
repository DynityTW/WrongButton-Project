package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.levels.s3.Chapter27
import tw.dynity.wrongbutton.levels.s3.MazeLevel

/** 第 27 章〈迷宮〉：照玩家的方式走完（聽它說、讀臉、一格一格跳）。 */
@RunWith(AndroidJUnit4::class)
class Chapter27PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun pathsAreLegalAndUseTheLies() {
        Chapter27.forEach { level ->
            val q = (level as MazeLevel).quiz
            assertEquals(0, q.path.first())
            assertEquals(q.goal, q.path.last())
            assertEquals(q.steps, q.path.size - 1)
            q.path.zipWithNext().forEach { (a, b) -> assertTrue("第 ${level.id} 關 ${q.name(a)}→${q.name(b)}", b in q.moves(a)) }
            assertTrue(q.lies.all { it in q.spoken && it in q.path })
        }
    }

    @Test
    fun everyMazeCanBeWalked() = with(Player(rule)) {
        clearChapters(8)
        openLevel(Chapter27.first().id)
        val start = memory.totalMistakes
        Chapter27.forEach { level ->
            val q = (level as MazeLevel).quiz
            waitFor("mz_reset")
            q.spoken.sorted().forEach { i ->
                tap("mz_ask_${q.name(i)}")
                rule.waitForIdle()
                assertEquals("第 ${level.id} 關 ${q.name(i)} 的臉", i in q.lies, lying(desc(node("the_button"))))
            }
            if (level.id == 421) {
                val bad = (0 until q.size * q.size).first { it != 0 && it !in q.moves(0) }
                tap("mz_${q.name(bad)}") // 跳不到：算一次錯
                rule.waitForIdle()
                assertEquals(start + 1, memory.totalMistakes)
            }
            q.path.drop(1).forEach { tap("mz_${q.name(it)}") }
            if (level != Chapter27.last()) finished(level.id) else rule.waitUntil(15_000) { exists("result_title") }
            assertTrue(memory.isCleared(level.id))
        }
        assertEquals(start + 1, memory.totalMistakes)
    }
}
