package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.levels.s3.CaptchaLevel
import tw.dynity.wrongbutton.levels.s3.Chapter22

/** 第 22 章〈我不是機器人〉：每一題都照玩家的方式通過（聽它說、讀臉、選格子、驗證）。 */
@RunWith(AndroidJUnit4::class)
class Chapter22PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun puzzlesAreWellFormed() {
        Chapter22.forEach { level ->
            (level as CaptchaLevel).rounds.forEach { r ->
                assertEquals(r.size * r.size, r.tiles.size)
                assertTrue("第 ${level.id} 關答案不能是空的", r.answer.isNotEmpty())
                assertTrue(r.answer.all { it in r.tiles.indices })
                assertTrue(r.lies.all { it in r.statements.indices })
                if (r.lies.isNotEmpty()) assertTrue("有謊話就要用說的", r.spoken)
            }
        }
    }

    @Test
    fun everyChallengeCanBePassed() = with(Player(rule)) {
        clearChapters(8)
        openLevel(Chapter22.first().id)
        val start = memory.totalMistakes
        Chapter22.forEach { level ->
            waitFor("cap_check")
            tap("cap_check")
            (level as CaptchaLevel).rounds.forEachIndexed { n, r ->
                waitFor("cap_tile_1")
                if (r.spoken) r.statements.indices.forEach { j ->
                    tap("cap_say_${j + 1}")
                    rule.waitForIdle()
                    assertEquals("第 ${level.id} 關第 ${n + 1} 題第 ${j + 1} 句的臉", j in r.lies, lying(desc(node("the_button"))))
                }
                if (level.id == 321) {
                    tap("cap_verify") // 什麼都沒選：驗證失敗，算一次錯
                    rule.waitForIdle()
                    assertEquals(start + 1, memory.totalMistakes)
                }
                r.answer.sorted().forEach { tap("cap_tile_${it + 1}") }
                tap("cap_verify")
                rule.waitForIdle()
            }
            waitFor("the_button")
            tap("the_button")
            if (level != Chapter22.last()) finished(level.id) else rule.waitUntil(15_000) { exists("result_title") }
            assertTrue(memory.isCleared(level.id))
        }
        assertEquals("只有故意驗證失敗那一次算錯", start + 1, memory.totalMistakes)
    }
}
