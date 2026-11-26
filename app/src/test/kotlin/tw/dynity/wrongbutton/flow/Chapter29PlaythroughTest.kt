package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.levels.s3.Chapter29
import tw.dynity.wrongbutton.levels.s3.MirrorLevel

/** 第 29 章〈鏡子〉：照玩家的方式轉鏡子、發射（聽它說、讀臉）。 */
@RunWith(AndroidJUnit4::class)
class Chapter29PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun onlyTheAnswerHitsTheButton() {
        Chapter29.forEach { level ->
            val q = (level as MirrorLevel).quiz
            val rot = q.rotatable
            assertEquals(rot.size, q.answer.size)
            val hits = (0 until (1 shl rot.size)).filter { bits ->
                val setting = rot.withIndex().associate { (k, i) -> i to if (bits shr k and 1 == 1) "\\" else "/" }
                val (side, pos, _) = q.trace(setting)
                side == q.exitSide && pos == q.exitPos
            }
            val expected = rot.indices.sumOf { k -> if (q.answer[k] == "\\") 1 shl k else 0 }
            assertEquals("第 ${level.id} 關只有一種轉法", listOf(expected), hits)
        }
    }

    @Test
    fun everyLaserCanHitTheButton() = with(Player(rule)) {
        clearChapters(8)
        openLevel(Chapter29.first().id)
        val start = memory.totalMistakes
        Chapter29.forEach { level ->
            val q = (level as MirrorLevel).quiz
            waitFor("mr_fire")
            q.spoken.sorted().forEach { i ->
                tap("mr_ask_${q.name(i)}")
                rule.waitForIdle()
                assertEquals("第 ${level.id} 關 ${q.name(i)} 的臉", i in q.lies, lying(desc(node("the_button"))))
            }
            val rot = q.rotatable
            if (level.id == 461 && q.answer.any { it == "\\" }) {
                tap("mr_fire") // 全部都是 ／：打不到，算一次錯
                rule.waitForIdle()
                assertEquals(start + 1, memory.totalMistakes)
            }
            rot.forEachIndexed { k, i -> if (q.answer[k] == "\\") tap("mr_${q.name(i)}") }
            tap("mr_fire")
            if (level != Chapter29.last()) finished(level.id) else rule.waitUntil(15_000) { exists("result_title") }
            assertTrue(memory.isCleared(level.id))
        }
        assertEquals(start + 1, memory.totalMistakes)
    }
}
