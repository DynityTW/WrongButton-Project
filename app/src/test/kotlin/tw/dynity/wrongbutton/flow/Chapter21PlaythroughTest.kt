package tw.dynity.wrongbutton.flow

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.levels.s3.Chapter21
import tw.dynity.wrongbutton.levels.s3.MineBoard
import tw.dynity.wrongbutton.levels.s3.MineLevel
import tw.dynity.wrongbutton.levels.s3.MineMode
import tw.dynity.wrongbutton.levels.s3.MineRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import tw.dynity.wrongbutton.app.MainActivity

/** 第 21 章〈踩地雷〉：每一關都照玩家的方式玩完（問數字、讀臉、翻開或插旗、按 The Button）。 */
@RunWith(AndroidJUnit4::class)
class Chapter21PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun boardsAreConsistent() {
        Chapter21.forEach { level ->
            val b = (level as MineLevel).board
            assertEquals("第 ${level.id} 關的謊話都要在它說的格子裡", b.lies, b.lies.intersect(b.reported.keys))
            b.reported.forEach { (i, v) ->
                assertFalse("第 ${level.id} 關 ${b.name(i)} 不能是雷", i in b.mines)
                assertEquals("第 ${level.id} 關 ${b.name(i)}：說謊 ⇔ 數字不對", i in b.lies, v != b.number(i))
            }
            if (b.hiddenLies > 0) assertEquals(b.hiddenLies, b.lies.size)
            assertTrue("第 ${level.id} 關至少要翻開或插幾格", b.safeToReveal.size >= 3 || b.mode == MineMode.Flag)
        }
        assertEquals(MineRule.Cross, (Chapter21[7] as MineLevel).board.rule)
    }

    @Test
    fun everyBoardCanBeSweptWithoutMistakes() = with(Player(rule)) {
        clearChapters(8)
        openLevel(Chapter21.first().id)
        val start = memory.totalMistakes
        Chapter21.forEach { level ->
            val b = (level as MineLevel).board
            waitFor("the_button")
            // 還沒掃完就按：算錯。（只在第一關試一次）
            if (level.id == 301) {
                tap("the_button")
                rule.waitForIdle()
                assertEquals(start + 1, memory.totalMistakes)
            }
            // 問每一個「?」，讀臉。
            if (b.hiddenLies == 0) {
                b.reported.keys.sorted().forEach { i ->
                    tap("mf_${b.name(i)}")
                    rule.waitForIdle()
                    assertEquals("第 ${level.id} 關 ${b.name(i)} 的臉", i in b.lies, lying(desc(node("the_button"))))
                }
            }
            when (b.mode) {
                MineMode.Reveal -> b.safeToReveal.sorted().forEach { i ->
                    val tag = "mf_${b.name(i)}"
                    if (desc(node(tag)).endsWith("，沒翻開")) tap(tag)
                }
                MineMode.Flag -> b.mines.sorted().forEach { tap("mf_${b.name(it)}") }
            }
            rule.waitForIdle()
            tap("the_button")
            if (level != Chapter21.last()) finished(level.id)
            else rule.waitUntil(15_000) { exists("result_title") }
            assertTrue(memory.isCleared(level.id))
        }
        assertEquals("只有第 301 關故意早按那一次算錯", start + 1, memory.totalMistakes)
    }

    @Test
    fun steppingOnAMineOrAWrongFlagCountsAsAMistake() = with(Player(rule)) {
        clearChapters(8)
        openLevel(Chapter21.first().id)
        waitFor("the_button")
        val b: MineBoard = (Chapter21.first() as MineLevel).board
        val start = memory.totalMistakes
        val mine = b.mines.first()
        tap("mf_${b.name(mine)}")
        rule.waitForIdle()
        assertEquals(start + 1, memory.totalMistakes)
        assertTrue(desc(node("mf_${b.name(mine)}")).endsWith("地雷"))
        // 插旗模式下插在雷上不算錯，也不會爆。
        tap("mf_mode_flag")
        val other = b.mines.last()
        tap("mf_${b.name(other)}")
        rule.waitForIdle()
        assertEquals(start + 1, memory.totalMistakes)
        assertTrue(desc(node("mf_${b.name(other)}")).contains("插了旗"))
    }
}
