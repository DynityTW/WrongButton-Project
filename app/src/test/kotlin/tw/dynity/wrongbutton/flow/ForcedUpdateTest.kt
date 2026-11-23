package tw.dynity.wrongbutton.flow

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.BuildConfig
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.core.update.UpdateChecker
import tw.dynity.wrongbutton.core.update.VersionRules

/** 主版本或次版本變了才強制更新；必須更新時，整個遊戲被「需要更新」蓋住。 */
@RunWith(AndroidJUnit4::class)
class ForcedUpdateTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun onlyMajorOrMinorChangesForceAnUpdate() {
        assertEquals(810, BuildConfig.VERSION_CODE)
        assertEquals(BuildConfig.VERSION_NAME, VersionRules.name(BuildConfig.VERSION_CODE))
        assertFalse("0.8.0 → 0.8.1 不強制", VersionRules.mustUpdate(800, 801))
        assertFalse("0.8.0 → 0.8.99 不強制", VersionRules.mustUpdate(800, 899))
        assertTrue("0.8.0 → 0.9.0 強制", VersionRules.mustUpdate(800, 900))
        assertTrue("0.8.3 → 1.0.0 強制", VersionRules.mustUpdate(803, 10000))
        assertFalse("一樣的版本不強制", VersionRules.mustUpdate(800, 800))
    }

    private class FakeChecker(start: Boolean) : UpdateChecker {
        override val required = MutableStateFlow(start)
        var retries = 0
        override suspend fun check(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>) = Unit
        override suspend fun retry(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>) {
            retries++
        }
    }

    @Test
    fun requiredUpdateCoversTheGameUntilUpdated() = with(Player(rule)) {
        waitFor("menu_levels")
        val fake = FakeChecker(start = false)
        container.updates = fake
        rule.activityRule.scenario.recreate()
        waitFor("menu_levels")
        assertFalse(exists("forced_update"))
        fake.required.value = true
        waitFor("forced_update")
        press("forced_update_now")
        rule.waitUntil(5_000) { fake.retries == 1 }
        fake.required.value = false
        rule.waitUntil(5_000) { !exists("forced_update") }
    }
}
