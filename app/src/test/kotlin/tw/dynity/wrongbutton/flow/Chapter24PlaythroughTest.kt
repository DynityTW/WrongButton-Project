package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.levels.s3.Chapter24
import tw.dynity.wrongbutton.levels.s3.CodeLevel
import tw.dynity.wrongbutton.levels.s3.RuleLevel
import tw.dynity.wrongbutton.levels.s3.spokenRows

/** 第 24 章〈猜規則〉：找規則與猜密碼，照玩家的方式解完（聽它說、讀臉、選候選／填密碼）。 */
@RunWith(AndroidJUnit4::class)
class Chapter24PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun ruleQuizzesNeedEveryCandidateJudged() {
        Chapter24.filterIsInstance<RuleLevel>().forEach { level ->
            val q = level.quiz
            assertEquals("第 ${level.id} 關有六張候選", 6, q.candidates.size)
            assertTrue("第 ${level.id} 關至少一張符合、一張不符合", q.answers.isNotEmpty() && q.answers.size < 6)
        }
    }

    @Test
    fun codeFeedbackMatchesTheSecret() {
        Chapter24.filterIsInstance<CodeLevel>().forEach { level ->
            val q = level.quiz
            q.rows.forEachIndexed { j, g ->
                val black = g.code.indices.count { g.code[it] == q.secret[it] }
                val common = g.code.toSet().sumOf { c -> minOf(g.code.count { it == c }, q.secret.count { it == c }) }
                val truth = black == g.black && common - black == g.white
                assertEquals("第 ${level.id} 關第 ${j + 1} 行", j !in q.lies, truth)
            }
            assertTrue(q.lies.all { it in q.spokenRows() } || q.hiddenLies > 0)
        }
    }

    @Test
    fun everyQuizCanBeSolved() = with(Player(rule)) {
        clearChapters(8)
        openLevel(Chapter24.first().id)
        val start = memory.totalMistakes
        Chapter24.forEach { level ->
            when (level) {
                is RuleLevel -> {
                    val q = level.quiz
                    waitFor("rq_cand_1")
                    q.spoken.sorted().forEach { j ->
                        tap("rq_ex_${j + 1}")
                        rule.waitForIdle()
                        assertEquals("第 ${level.id} 關例子 ${j + 1} 的臉", j in q.lies, lying(desc(node("the_button"))))
                    }
                    if (level.id == 361) {
                        tap("rq_confirm") // 全部判斷成不符合：至少一張符合，所以算一次錯
                        rule.waitForIdle()
                        assertEquals(start + 1, memory.totalMistakes)
                    }
                    q.answers.sorted().forEach { tap("rq_cand_${it + 1}") }
                    tap("rq_confirm")
                }
                is CodeLevel -> {
                    val q = level.quiz
                    waitFor("cq_confirm")
                    q.spokenRows().sorted().forEach { j ->
                        tap("cq_row_${j + 1}")
                        rule.waitForIdle()
                        assertEquals("第 ${level.id} 關第 ${j + 1} 行的臉", j in q.lies, lying(desc(node("the_button"))))
                    }
                    q.secret.forEachIndexed { k, c -> repeat(c + 1) { tap("cq_slot_${k + 1}") } }
                    tap("cq_confirm")
                }
            }
            if (level != Chapter24.last()) finished(level.id) else rule.waitUntil(15_000) { exists("result_title") }
            assertTrue(memory.isCleared(level.id))
        }
        assertEquals(start + 1, memory.totalMistakes)
    }
}
