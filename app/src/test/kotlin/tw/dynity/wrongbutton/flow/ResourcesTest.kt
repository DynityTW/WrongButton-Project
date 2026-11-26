package tw.dynity.wrongbutton.flow

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.dialogue.DialogueEvent
import tw.dynity.wrongbutton.core.dialogue.LineMarkup
import tw.dynity.wrongbutton.core.engine.GlobalDialogue
import tw.dynity.wrongbutton.levels.Levels

/** 台詞表與提示的完整性：每關每種事件至少 2 句、標記都能解析、提示固定三階。 */
@RunWith(AndroidJUnit4::class)
class ResourcesTest {
    private val res = ApplicationProvider.getApplicationContext<android.app.Application>().resources

    @Test
    fun packageNameIsFinalAndShortcutsPointAtIt() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        assertEquals("com.wrongbutton.app", context.packageName)
        // 桌面捷徑的 targetPackage 寫死在 XML 裡，換套件名稱時很容易漏改。
        val targets = mutableListOf<String>()
        val xml = res.getXml(R.xml.shortcuts)
        while (xml.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (xml.eventType == org.xmlpull.v1.XmlPullParser.START_TAG && xml.name == "intent") {
                targets += xml.getAttributeValue("http://schemas.android.com/apk/res/android", "targetPackage")
            }
        }
        assertTrue(targets.isNotEmpty())
        targets.forEach { assertEquals(context.packageName, it) }
    }

    @Test
    fun everyLevelHasAtLeastTwoLinesPerEvent() {
        (Levels.all.map { "第 ${it.id} 關" to it.dialogue } + ("全域" to GlobalDialogue)).forEach { (name, set) ->
            DialogueEvent.entries.forEach { event ->
                val lines = res.getStringArray(set[event])
                assertTrue("$name / $event 只有 ${lines.size} 句", lines.size >= 2)
                assertEquals("$name / $event 有重複台詞", lines.size, lines.toSet().size)
                lines.forEach { LineMarkup.parse(it) } // 標記錯誤會丟例外
            }
        }
    }

    @Test
    fun levelsHaveThreeHintsAndUniqueIds() {
        // 〈最後的提示〉按提示鈕就是解法，刻意沒有三階提示。
        Levels.all.filter { it.id != tw.dynity.wrongbutton.levels.Level118LastHint.id }.forEach { assertEquals("第 ${it.id} 關提示數", 3, it.hintRes.size) }
        val seasonOne = Levels.chapters.filter { !it.standalone }.flatMap { it.levels }
        assertEquals("第一季 120 關", 120, seasonOne.size)
        assertEquals("第一季最後一關是〈相信我〉", 10, seasonOne.last().id)
        Levels.chapters.filter { it.season == 2 }.forEach { assertEquals("第二季每章 15 關", 15, it.levels.size) }
        Levels.chapters.filter { it.season == 3 }.forEach { assertEquals("第三季每章 20 關", 20, it.levels.size) }
        assertEquals("第一、二季 300 關", 300, Levels.chapters.filter { it.season < 3 }.sumOf { it.levels.size })
        assertEquals("免費關卡以外都需要 Pro", Levels.all.size - Levels.FREE_LEVELS, Levels.all.count { Levels.requiresPro(it.id) })
        // 第二季的 id 和顯示編號相同（121 起）。
        Levels.all.drop(120).forEach { assertEquals("第二季 id 應等於顯示編號", Levels.numberOf(it.id), it.id) }
        assertEquals("關卡 id 不可重複", Levels.all.size, Levels.all.map { it.id }.toSet().size)
    }

    @Test
    fun level8NeverSaysTheAnswerOutLoud() {
        val set = Levels.byId(8).dialogue
        val all = DialogueEvent.entries.flatMap { res.getStringArray(set[it]).toList() } +
            listOf(res.getString(R.string.l08_wrong_a), res.getString(R.string.l08_wrong_b))
        all.forEach { assertFalse("第 8 關台詞不可帶入錯誤數：$it", it.contains("{total_mistakes}") || it.contains("{run_mistakes}")) }
    }

    @Test
    fun level4HasTwentyDistinctQuestions() {
        val questions = res.getStringArray(R.array.l04_questions).toList() +
            listOf(res.getString(R.string.l04_q1), res.getString(R.string.l04_q2))
        assertTrue(questions.toSet().size >= 20)
    }

    @Test
    fun lyingMarkupNeverAppearsBeforeLevel2() {
        val set = Levels.byId(1).dialogue
        DialogueEvent.entries.flatMap { res.getStringArray(set[it]).toList() }.forEach {
            assertFalse("第 1 關不應說謊：$it", LineMarkup.parse(it).tell?.isLie == true)
        }
    }
}
