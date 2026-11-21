package tw.dynity.wrongbutton.flow

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tw.dynity.wrongbutton.app.MainActivity
import java.io.File

/** 開發用：WB_SCREENSHOTS=menus 時，把選關、Pro 頁、模擬廣告存成圖片。 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class MenuScreenshotTool {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun capture() {
        assumeTrue(System.getenv("WB_SCREENSHOTS") == "menus")
        val out = File(System.getProperty("user.dir"), "build/screenshots").apply { mkdirs() }
        fun shot(name: String) {
            rule.waitForIdle()
            val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
            File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        with(Player(rule)) {
            clearChapters(8)
            container.monetization.revokeProForTesting()
            container.memory.update { m ->
                // 幾關沒通關、幾關用了提示和特殊解，讓紀錄頁有各種狀態可看。
                var x = m
                listOf(121, 122, 136).forEach { id -> x = tw.dynity.wrongbutton.core.memory.MemoryOps.startAttempt(x, id) }
                x
            }
            press("menu_records"); waitFor("records"); shot("records")
            runCatching { rule.onNodeWithTag("records_levels").performScrollTo(); shot("records_levels") }
            run { rule.onNodeWithTag("records_chapter_${tw.dynity.wrongbutton.R.string.chapter_1}").performScrollTo().performClick(); rule.onNodeWithTag("records_level_3").performScrollTo(); shot("records_cleared") }
            rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor("menu_levels")
            press("menu_levels"); waitFor("select_grid")
            val proId = tw.dynity.wrongbutton.levels.Levels.all[tw.dynity.wrongbutton.levels.Levels.FREE_LEVELS].id
            rule.onNodeWithTag("select_grid").performScrollToKey(proId)
            shot("select")
            tap("select_level_$proId"); waitFor("pro_screen"); shot("pro")
            rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor("select_grid")
            rule.onNodeWithTag("select_grid").performScrollToKey(1)
            tap("select_level_1"); tap("host_hint"); tap("hint_reveal"); tap("hint_reveal"); waitFor("mock_ad"); advance(2_000); shot("ad")
            tap("mock_ad_close"); rule.waitUntil(5_000) { !exists("mock_ad") }
            rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor("menu_levels"); shot("title")
        }
    }
}
