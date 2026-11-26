package tw.dynity.wrongbutton.flow

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tw.dynity.wrongbutton.app.MainActivity
import java.io.File

/**
 * 開發用的截圖工具：只在設定環境變數 WB_SCREENSHOTS=<關卡 id，逗號分隔> 時執行，
 * 把每一關剛進關的畫面存成 app/build/screenshots/level_<id>.png。平常的測試會跳過。
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTool {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun capture() {
        val ids = System.getenv("WB_SCREENSHOTS")?.split(',')?.mapNotNull { it.trim().toIntOrNull() }.orEmpty()
        assumeTrue(ids.isNotEmpty())
        val out = File(System.getProperty("user.dir"), "build/screenshots").apply { mkdirs() }
        with(Player(rule)) {
            clearChapters(8)
            ids.forEach { id ->
                container.memory.update { m -> tw.dynity.wrongbutton.levels.Levels.all.takeWhile { it.id != id }.fold(m) { acc, l -> tw.dynity.wrongbutton.core.memory.MemoryOps.completeLevel(acc, l.id, tw.dynity.wrongbutton.core.memory.RunResult(0, 0, 1, 0, false)) } }
                openLevel(id)
                advance(2_500)
                // 第 22 章：先打開驗證框，截圖才看得到題目。
                if (exists("cap_check")) { tap("cap_check"); rule.waitForIdle(); if (exists("cap_say_1")) { tap("cap_say_1"); rule.waitForIdle() } }
                rule.waitForIdle()
                val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
                File(out, "level_$id.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                repeat(2) { rule.activityRule.scenario.onActivity { a -> a.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }
                waitFor("menu_levels")
            }
        }
    }
}
