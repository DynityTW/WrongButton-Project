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
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.RunResult
import tw.dynity.wrongbutton.levels.Levels
import java.io.File

/**
 * 開發用：WB_STORE=1 時產生 Google Play 商店截圖（1080×1920，9:16）到 app/build/store/。
 * WB_STORE_LEVELS 可指定要截的關卡 id（逗號分隔）。
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xxhdpi")
class StoreScreenshotTool {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun capture() {
        assumeTrue(System.getenv("WB_STORE") == "1")
        val out = File(System.getProperty("user.dir"), "build/store").apply { mkdirs() }
        fun shot(name: String) {
            rule.waitForIdle()
            val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
            File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        val ids = (System.getenv("WB_STORE_LEVELS") ?: "1,2,16,136,166,199,228").split(',').mapNotNull { it.trim().toIntOrNull() }
        with(Player(rule)) {
            waitFor("menu_levels")
            shot("title_new")
            clearChapters(8)
            container.monetization.revokeProForTesting()
            ids.forEach { id ->
                container.memory.update { m -> Levels.all.takeWhile { it.id != id }.fold(m) { acc, l -> MemoryOps.completeLevel(acc, l.id, RunResult(0, 0, 1, 0, false)) } }
                if (Levels.requiresPro(id)) grantPro()
                openLevel(id)
                advance(2_500)
                shot("level_$id")
                repeat(2) { rule.activityRule.scenario.onActivity { a -> a.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }
                waitFor("menu_levels")
                container.monetization.revokeProForTesting()
            }
            shot("title")
            tap("menu_records"); waitFor("records"); shot("records")
            rule.activityRule.scenario.onActivity { a -> a.onBackPressedDispatcher.onBackPressed() }
            waitFor("menu_levels")
            tap("menu_pro"); waitFor("pro_screen"); shot("pro")
        }
    }
}
