package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.levels.s3.Chapter23
import tw.dynity.wrongbutton.levels.s3.LiarGoal
import tw.dynity.wrongbutton.levels.s3.LiarLevel

/** 第 23 章〈誰在說謊〉：每一題都照玩家的方式解完（聽 The Button 說、讀臉、標記、確定）。 */
@RunWith(AndroidJUnit4::class)
class Chapter23PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun answersMatchTheRoles() {
        Chapter23.forEach { level ->
            val p = (level as LiarLevel).puzzle
            assertEquals(p.n, p.roles.length)
            val expected = when (p.goal) {
                LiarGoal.Liars -> p.roles.indices.filter { p.roles[it] == 'L' }.toSet()
                LiarGoal.Spy -> setOf(p.roles.indexOf('S'))
                LiarGoal.Button -> setOf(p.button!!)
            }
            assertEquals("第 ${level.id} 關", expected, p.answer)
            assertTrue(p.said.all { it.who in 0 until p.n })
        }
    }

    @Test
    fun everyPuzzleCanBeSolved() = with(Player(rule)) {
        clearChapters(8)
        openLevel(Chapter23.first().id)
        val start = memory.totalMistakes
        Chapter23.forEach { level ->
            val p = (level as LiarLevel).puzzle
            waitFor("kk_confirm")
            if (p.narrator != null) {
                tap("kk_narrator")
                rule.waitForIdle()
                assertEquals("第 ${level.id} 關 The Button 的臉", !p.narratorTrue, lying(desc(node("the_button"))))
            }
            if (level.id == 341) {
                tap("kk_confirm") // 什麼都沒標：算一次錯
                rule.waitForIdle()
                assertEquals(start + 1, memory.totalMistakes)
            }
            p.answer.sorted().forEach { tap("kk_$it") }
            tap("kk_confirm")
            if (level != Chapter23.last()) finished(level.id) else rule.waitUntil(15_000) { exists("result_title") }
            assertTrue(memory.isCleared(level.id))
        }
        assertEquals(start + 1, memory.totalMistakes)
    }
}
