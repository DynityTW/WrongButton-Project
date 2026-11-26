package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.RunResult
import tw.dynity.wrongbutton.levels.Levels

/** 選關畫面：「待破關」篩選與章節選單。 */
@RunWith(AndroidJUnit4::class)
class LevelSelectTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun todoFilterShowsOnlyUnlockedButUnclearedLevels() = with(Player(rule)) {
        waitFor("menu_levels")
        val ch1 = Levels.chapters.first().levels
        container.memory.update { m ->
            ch1.take(3).fold(m) { acc, l -> MemoryOps.completeLevel(acc, l.id, RunResult(0, 0, 1, 0, false)) }
        }
        press("menu_levels")
        waitFor("select_grid")
        tap("select_filter_todo")
        rule.waitForIdle()
        assertFalse("通關的不會出現在待破關", exists("select_level_${ch1[0].id}"))
        rule.onNodeWithTag("select_grid").performScrollToKey(ch1[3].id)
        assertTrue("下一關已解鎖、還沒過", exists("select_level_${ch1[3].id}"))
        val locked = Levels.all.first { !Levels.isUnlocked(memory, it.id) }
        assertFalse("沒解鎖的不算待破關", exists("select_level_${locked.id}"))
        tap("select_filter_all")
        rule.waitForIdle()
        rule.onNodeWithTag("select_grid").performScrollToKey(ch1[0].id)
        assertTrue(exists("select_level_${ch1[0].id}"))
    }

    @Test
    fun chapterPickerJumpsToChapter() = with(Player(rule)) {
        clearChapters(Levels.chapters.size)
        press("menu_levels")
        waitFor("select_grid")
        tap("select_chapter_picker")
        waitFor("select_picker")
        val target = Levels.chapters.lastIndex
        rule.onNodeWithTag("select_pick_$target").performScrollTo()
        tap("select_pick_$target")
        rule.waitUntil(5_000) { !exists("select_picker") }
        rule.waitForIdle()
        assertTrue("跳到最後一章", exists("select_level_${Levels.chapters[target].levels.first().id}"))
    }
}
