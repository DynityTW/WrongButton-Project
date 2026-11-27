package tw.dynity.wrongbutton.flow

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
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
 * 第三季在小手機（寬 360dp）、深色模式下：每一章最大的那一關，所有可以點的東西都在畫面裡，不會被擠出右邊。
 * 設定 WB_SCREENSHOTS=s3 時也存成圖片（build/screenshots/s3_<關卡>.png）方便看深色模式的對比。
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h740dp-night-xxhdpi")
class Season3LayoutTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val biggest = listOf(317, 336, 359, 380, 400, 418, 440, 460, 480, 491)

    @Test
    fun everythingFitsOnANarrowPhone() = with(Player(rule)) {
        clearChapters(8)
        val out = File(System.getProperty("user.dir"), "build/screenshots").apply { mkdirs() }
        biggest.forEach { id ->
            container.memory.update { m -> Levels.all.takeWhile { it.id != id }.fold(m) { acc, l -> MemoryOps.completeLevel(acc, l.id, RunResult(0, 0, 1, 0, false)) } }
            openLevel(id)
            advance(2_500)
            rule.waitForIdle()
            if (exists("cap_check")) { tap("cap_check"); rule.waitForIdle() }
            val root = rule.onRoot().fetchSemanticsNode()
            val width = root.boundsInRoot.right
            val clickable = rule.onRoot().fetchSemanticsNode().let { n -> generateSequence(listOf(n)) { level -> level.flatMap { it.children }.takeIf { it.isNotEmpty() } }.flatten() }
                .filter { it.config.contains(SemanticsActions.OnClick) }
                .toList()
            val outside = clickable.filter { it.boundsInRoot.right > width + 1f || it.boundsInRoot.left < -1f }
            assertTrue("第 $id 關有 ${outside.size} 個可以點的東西超出畫面：${outside.take(3).map { it.boundsInRoot }}", outside.isEmpty())
            if (System.getenv("WB_SCREENSHOTS") == "s3") {
                val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
                File(out, "s3_$id.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            repeat(2) { rule.activityRule.scenario.onActivity { a -> a.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }
            waitFor("menu_levels")
        }
    }
}
