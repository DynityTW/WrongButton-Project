package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.levels.Levels
import tw.dynity.wrongbutton.levels.s3.CaptchaLevel
import tw.dynity.wrongbutton.levels.s3.Chapter30
import tw.dynity.wrongbutton.levels.s3.CodeLevel
import tw.dynity.wrongbutton.levels.s3.LiarLevel
import tw.dynity.wrongbutton.levels.s3.MazeLevel
import tw.dynity.wrongbutton.levels.s3.MineLevel
import tw.dynity.wrongbutton.levels.s3.MineMode
import tw.dynity.wrongbutton.levels.s3.MirrorLevel
import tw.dynity.wrongbutton.levels.s3.OrderLevel
import tw.dynity.wrongbutton.levels.s3.PinLevel
import tw.dynity.wrongbutton.levels.s3.RuleLevel
import tw.dynity.wrongbutton.levels.s3.WireLevel
import tw.dynity.wrongbutton.levels.s3.spokenRows

/** 第 30 章〈終章〉：九種謎題各兩題，照玩家的方式解完，一路到第 500 關。 */
@RunWith(AndroidJUnit4::class)
class Chapter30PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun theGameEndsAtLevel500() {
        assertEquals(500, Levels.all.size)
        assertEquals(500, Levels.all.last().id)
        assertEquals(3, Levels.chapters.last().season)
    }

    private fun Player.face(level: Level, what: String, lie: Boolean) {
        rule.waitForIdle()
        assertEquals("第 ${level.id} 關 $what 的臉", lie, lying(desc(node("the_button"))))
    }

    @Test
    fun everyFinalePuzzleCanBeSolved() = with(Player(rule)) {
        clearChapters(8)
        openLevel(Chapter30.first().id)
        val start = memory.totalMistakes
        Chapter30.forEach { level ->
            when (level) {
                is MineLevel -> {
                    val b = level.board
                    waitFor("the_button")
                    b.reported.keys.sorted().forEach { i -> tap("mf_${b.name(i)}"); face(level, b.name(i), i in b.lies) }
                    if (b.mode == MineMode.Reveal) b.safeToReveal.sorted().forEach { i -> if (desc(node("mf_${b.name(i)}")).endsWith("，沒翻開")) tap("mf_${b.name(i)}") }
                    else b.mines.sorted().forEach { tap("mf_${b.name(it)}") }
                    rule.waitForIdle()
                    tap("the_button")
                }
                is CaptchaLevel -> {
                    waitFor("cap_check"); tap("cap_check")
                    level.rounds.forEach { r ->
                        waitFor("cap_tile_1")
                        if (r.spoken) r.statements.indices.forEach { j -> tap("cap_say_${j + 1}"); face(level, "第 ${j + 1} 句", j in r.lies) }
                        r.answer.sorted().forEach { tap("cap_tile_${it + 1}") }
                        tap("cap_verify"); rule.waitForIdle()
                    }
                    waitFor("the_button"); tap("the_button")
                }
                is LiarLevel -> {
                    val p = level.puzzle
                    waitFor("kk_confirm")
                    if (p.narrator != null) { tap("kk_narrator"); face(level, "The Button 那句", !p.narratorTrue) }
                    markLiarPuzzle(p)
                    tap("kk_confirm")
                }
                is RuleLevel -> {
                    val q = level.quiz
                    waitFor("rq_cand_1")
                    q.spoken.sorted().forEach { j -> tap("rq_ex_${j + 1}"); face(level, "例子 ${j + 1}", j in q.lies) }
                    q.answers.sorted().forEach { tap("rq_cand_${it + 1}") }
                    tap("rq_confirm")
                }
                is CodeLevel -> {
                    val q = level.quiz
                    waitFor("cq_confirm")
                    q.spokenRows().sorted().forEach { j -> tap("cq_row_${j + 1}"); face(level, "第 ${j + 1} 行", j in q.lies) }
                    q.secret.forEachIndexed { k, c -> repeat(c + 1) { tap("cq_slot_${k + 1}") } }
                    tap("cq_confirm")
                }
                is WireLevel -> {
                    val q = level.quiz
                    waitFor("wq_power")
                    q.spoken.sorted().forEach { c -> tap("wq_cell_${c / q.lamps + 1}_${c % q.lamps + 1}"); face(level, "格子 $c", c in q.lies) }
                    repeat(q.switches) { i -> if (q.answer shr i and 1 == 1) tap("wq_sw_${"ABCDE"[i]}") }
                    tap("wq_power")
                }
                is OrderLevel -> {
                    val q = level.quiz
                    waitFor("os_confirm")
                    q.spoken.sorted().forEach { j -> tap("os_clue_${j + 1}"); face(level, "線索 ${j + 1}", j in q.lies) }
                    q.colors.forEachIndexed { k, c -> repeat(c + 1) { tap("os_c_${k + 1}") } }
                    q.drinks?.forEachIndexed { k, d -> repeat(d + 1) { tap("os_d_${k + 1}") } }
                    tap("os_confirm")
                }
                is MazeLevel -> {
                    val q = level.quiz
                    waitFor("mz_reset")
                    q.spoken.sorted().forEach { i -> tap("mz_ask_${q.name(i)}"); face(level, q.name(i), i in q.lies) }
                    q.path.drop(1).forEach { tap("mz_${q.name(it)}") }
                }
                is PinLevel -> {
                    val q = level.quiz
                    waitFor("pw_confirm")
                    q.spoken.sorted().forEach { j -> tap("pw_rule_${j + 1}"); face(level, "規則 ${j + 1}", j in q.lies) }
                    q.digits.forEachIndexed { k, d -> if (d <= 5) repeat(d) { tap("pw_up_${k + 1}") } else repeat(10 - d) { tap("pw_down_${k + 1}") } }
                    tap("pw_confirm")
                }
                is MirrorLevel -> {
                    val q = level.quiz
                    waitFor("mr_fire")
                    q.spoken.sorted().forEach { i -> tap("mr_ask_${q.name(i)}"); face(level, q.name(i), i in q.lies) }
                    q.rotatable.forEachIndexed { k, i -> if (q.answer[k] == "\\") tap("mr_${q.name(i)}") }
                    tap("mr_fire")
                }
                else -> error("第 ${level.id} 關的種類沒有處理")
            }
            if (level != Chapter30.last()) finished(level.id) else rule.waitUntil(15_000) { exists("result_title") }
            assertTrue("第 ${level.id} 關", memory.isCleared(level.id))
        }
        assertEquals("終章一次都沒錯", start, memory.totalMistakes)
        assertTrue("第 500 關結算時沒有下一關", !exists("result_next"))
    }
}
