package tw.dynity.wrongbutton.levels

import androidx.annotation.StringRes
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.memory.MemoryKey
import tw.dynity.wrongbutton.core.memory.PlayerMemory

/**
 * 一章。章內關卡依序排列。
 *
 * [standalone] 的章（第二、三季）各自獨立：第一季全部通關後，每一章的第一關都直接開放，章內再依序解鎖。
 */
data class Chapter(
    @StringRes val titleRes: Int,
    val levels: List<Level>,
    val standalone: Boolean = false,
    /** 第幾季（選關畫面的分段標題）。第二、三季的章都是 [standalone]。 */
    val season: Int = if (standalone) 2 else 1,
)

/**
 * 關卡註冊表。
 *
 * - 關卡的 [Level.id] 是內部、永久的存檔鍵，一旦發布就不能改。
 * - 玩家看到的「第 N 關」是關卡在這份清單裡的位置，可以隨時重新排序。
 *
 * 新增一關：寫一個 Level 檔案，放進某一章的清單。
 */
object Levels {
    val chapters: List<Chapter> = listOf(
        Chapter(
            R.string.chapter_1,
            listOf(
                Level01Press,
                Level02Empty,
                Level03Ellipsis,
                Level11Tabs,
                Level04DontAsk,
                Level12Terms,
                Level05TouchTarget,
                Level13Reorder,
                Level14Onboarding,
                Level15OffScreen,
                Level16Filter,
                Level17BottomSheet,
                Level18AskAgain,
                Level19Overflow,
                Level20AllTogether,
            ),
        ),
        Chapter(
            R.string.chapter_2,
            listOf(
                Level06Advisor,
                Level21Switches,
                Level22Sunglasses,
                Level23Banner,
                Level24Avatar,
                Level25TwoLines,
                Level26Edited,
                Level27Quote,
                Level28Colleague,
                Level29Preview,
                Level30Story,
                Level31Sticker,
                Level32Wait,
                Level33AboutYou,
                Level34ThreeOfMe,
            ),
        ),
        Chapter(
            R.string.chapter_3,
            listOf(
                Level35Link,
                Level36NoLabel,
                Level37Hold,
                Level38AllCount,
                Level39SearchKey,
                Level40SlideConfirm,
                Level41Cards,
                Level42Fab,
                Level43Recipients,
                Level44ListGrid,
                Level45Release,
                Level46Reveal,
                Level47Leaderboard,
                Level48GroupPhoto,
                Level07LeaveNone,
            ),
        ),
        Chapter(
            R.string.chapter_4,
            listOf(
                Level08Mistakes,
                Level49Grudge,
                Level50Paste,
                Level51Keep,
                Level52Misremember,
                Level53Undo,
                Level54Achievement,
                Level55Draft,
                Level56Storage,
                Level57FirstAgain,
                Level09Predict,
                Level58Capsule,
                Level59Diary,
                Level60Guest,
                Level61WhoAreYou,
            ),
        ),
        Chapter(
            R.string.chapter_5,
            listOf(
                Level62Required,
                Level63RedText,
                Level64Booking,
                Level65Other,
                Level66ClearChoice,
                Level67SelectAll,
                Level68Keyboard,
                Level69Wizard,
                Level70Autocomplete,
                Level71LastTime,
                Level72Typo,
                Level73OnesDigit,
                Level74ThreePm,
                Level75Rating,
                Level76Dictation,
            ),
        ),
        Chapter(
            R.string.chapter_6,
            listOf(
                Level77Version2, Level78ChangeLog, Level79Smile, Level80Feedback, Level81Cache,
                Level82ABTest, Level83CoachMarks, Level84ReadAloud, Level85Versions, Level86Import,
                Level87Assemble, Level88Rename, Level89TwoAdvisors, Level90Default, Level91Disable,
            ),
        ),
        Chapter(
            R.string.chapter_7,
            listOf(
                Level92Knock, Level93Bounds, Level94Layers, Level95Properties, Level96Counts,
                Level97Direction, Level98SmallPrint, Level99WhiteOnWhite, Level100Clip, Level101TooFast,
                Level102KeepState, Level103ZeroWidth, Level104Rules, Level105Trust, Level106Rebuild,
            ),
        ),
        Chapter(
            R.string.chapter_8,
            listOf(
                Level107YourTurn, Level108FewerLies, Level109HonestMistake, Level110YouAdvise, Level111LastAdvice,
                Level112KeepOne, Level113OnlyYou, Level114AboutYouForm, Level115LetMeChoose, Level116DoYouRemember,
                Level117LieSwitch, Level118LastHint, Level119TryLater, Level120ReadAloud,
                // 〈相信我〉：最後一關。內部 id 是 10（早期試玩版就有的存檔鍵）。
                Level10TrustMe,
            ),
        ),
        // ---- 第二季：每章獨立，第一季通關後全部開放 ----
        Chapter(R.string.chapter_9, tw.dynity.wrongbutton.levels.s2.Chapter09, standalone = true),
        Chapter(R.string.chapter_10, tw.dynity.wrongbutton.levels.s2.Chapter10, standalone = true),
        Chapter(R.string.chapter_11, tw.dynity.wrongbutton.levels.s2.Chapter11, standalone = true),
        Chapter(R.string.chapter_12, tw.dynity.wrongbutton.levels.s2.Chapter12, standalone = true),
        Chapter(R.string.chapter_13, tw.dynity.wrongbutton.levels.s2.Chapter13, standalone = true),
        Chapter(R.string.chapter_14, tw.dynity.wrongbutton.levels.s2.Chapter14, standalone = true),
        Chapter(R.string.chapter_15, tw.dynity.wrongbutton.levels.s2.Chapter15, standalone = true),
        Chapter(R.string.chapter_16, tw.dynity.wrongbutton.levels.s2.Chapter16, standalone = true),
        Chapter(R.string.chapter_17, tw.dynity.wrongbutton.levels.s2.Chapter17, standalone = true),
        Chapter(R.string.chapter_18, tw.dynity.wrongbutton.levels.s2.Chapter18, standalone = true),
        Chapter(R.string.chapter_19, tw.dynity.wrongbutton.levels.s2.Chapter19, standalone = true),
        Chapter(R.string.chapter_20, tw.dynity.wrongbutton.levels.s2.Chapter20, standalone = true),
        // ---- 第三季：每章 20 關，一樣在第一季通關後開放 ----
        Chapter(R.string.chapter_21, tw.dynity.wrongbutton.levels.s3.Chapter21, standalone = true, season = 3),
        Chapter(R.string.chapter_22, tw.dynity.wrongbutton.levels.s3.Chapter22, standalone = true, season = 3),
        Chapter(R.string.chapter_23, tw.dynity.wrongbutton.levels.s3.Chapter23, standalone = true, season = 3),
        Chapter(R.string.chapter_24, tw.dynity.wrongbutton.levels.s3.Chapter24, standalone = true, season = 3),
        Chapter(R.string.chapter_25, tw.dynity.wrongbutton.levels.s3.Chapter25, standalone = true, season = 3),
        Chapter(R.string.chapter_26, tw.dynity.wrongbutton.levels.s3.Chapter26, standalone = true, season = 3),
        Chapter(R.string.chapter_27, tw.dynity.wrongbutton.levels.s3.Chapter27, standalone = true, season = 3),
        Chapter(R.string.chapter_28, tw.dynity.wrongbutton.levels.s3.Chapter28, standalone = true, season = 3),
        Chapter(R.string.chapter_29, tw.dynity.wrongbutton.levels.s3.Chapter29, standalone = true, season = 3),
    )

    val all: List<Level> = chapters.flatMap { it.levels }

    /** 免費的關卡數（依顯示順序，第 1–5 章）。之後的關卡需要 Pro。 */
    const val FREE_LEVELS = 75

    /** id → 在 [all] 裡的位置。選關畫面每一格都會查，所以先建好表，不要每次從頭找。 */
    private val indexById: Map<Int, Int> = all.withIndex().associate { (i, level) -> level.id to i }
    private val byIdMap: Map<Int, Level> = all.associateBy { it.id }

    /** 每一關所屬的章，以及它在章內的位置。 */
    private val chapterOf: Map<Int, Pair<Chapter, Int>> =
        chapters.flatMap { c -> c.levels.mapIndexed { i, l -> l.id to (c to i) } }.toMap()

    /** 第一季的最後一關（〈相信我〉）。通關後第二季的每一章都開放。 */
    val seasonOneFinale: Level = chapters.last { !it.standalone }.levels.last()

    fun byId(id: Int): Level = byIdMap.getValue(id)

    fun indexOf(id: Int): Int = indexById[id] ?: -1

    /** 玩家看到的關卡編號（從 1 開始）。 */
    fun numberOf(id: Int): Int = indexOf(id) + 1

    fun next(id: Int): Level? = all.getOrNull(indexOf(id) + 1)

    /** 這一關是否需要 Pro。 */
    fun requiresPro(id: Int): Boolean = indexOf(id) >= FREE_LEVELS

    /**
     * 進度上是否解鎖（不管 Pro）：
     * - 第一季：第一關永遠開放；其餘在前一關通關後解鎖。
     * - 第二季：章的第一關在第一季通關後開放；章內在前一關通關後解鎖。
     */
    fun isUnlocked(memory: PlayerMemory, id: Int): Boolean {
        val index = indexOf(id)
        if (index < 0) return false
        if (index == 0) return true
        val (chapter, inChapter) = chapterOf.getValue(id)
        return if (chapter.standalone && inChapter == 0) memory.isCleared(seasonOneFinale.id)
        else memory.isCleared(all[index - 1].id)
    }

    /** 進度解鎖，而且（需要的話）有 Pro。 */
    fun isPlayable(memory: PlayerMemory, pro: Boolean, id: Int): Boolean = isUnlocked(memory, id) && (pro || !requiresPro(id))

    /** 從桌面捷徑或小工具按下時，要直接打開哪一關（第 9 章）。 */
    fun externalTarget(from: String): Int? = tw.dynity.wrongbutton.levels.s2.externalTargetLevel(from)

    /** 「繼續」要去的關卡：第一個已解鎖但還沒通關的關卡。 */
    fun continueTarget(memory: PlayerMemory): Level? =
        all.firstOrNull { !memory.isCleared(it.id) && isUnlocked(memory, it.id) }
}

/**
 * 跨關卡共用的記憶鍵（後面的關卡會讀前面關卡留下的紀錄）。
 *
 * **鍵名是永久的存檔鍵，發布後不能改名。** 鍵名裡的數字是當初命名時的編號，和現在的顯示編號不一定一樣：
 *
 * | 鍵 | 寫入的關卡（顯示編號） |
 * |---|---|
 * | l1_press_delay_ms | 1 |
 * | l4_solution | 5〈不要再問我〉 |
 * | l5_solution | 7〈至少 48dp〉 |
 * | l6_first_side | 16〈顧問〉 |
 * | l7_swipe_direction、l7_hesitation_ms | 45〈一個都不留〉 |
 * | l49_grudge、l49_grudge_planted | 47〈忘掉一件事〉 |
 * | l51_kept_shape | 49〈幫我保管〉 |
 * | l54_achievement | 52〈成就〉 |
 * | l55_draft_stage | 53〈草稿〉 |
 * | l62_nickname | 61〈必填〉 |
 * | l75_first_stars | 74〈評分〉 |
 * | frag_* | 80–84（第 6 章的碎片） |
 * | l92_dev_mode | 91〈敲七下〉 |
 * | l107_annoying | 106〈換妳說〉 |
 * | l113_wish | 112〈只有妳記得〉 |
 * | l114_like | 113〈關於妳的表單〉 |
 * | l117_lying_switch | 116〈說謊開關〉 |
 * | l120_keep | 119〈念給妳聽〉 |
 * | l10_press_delay_ms | 120〈相信我〉 |
 *
 * 第二季（121 關以後）的鍵一律用「s2_<內部 id>_<名稱>」，內部 id 和顯示編號相同。
 */
object Keys {
    val L1PressDelay = MemoryKey("l1_press_delay_ms")
    val L4Solution = MemoryKey("l4_solution")
    val L5Solution = MemoryKey("l5_solution")
    val L6FirstSide = MemoryKey("l6_first_side")
    val L7SwipeDirection = MemoryKey("l7_swipe_direction")
    val L7Hesitation = MemoryKey("l7_hesitation_ms")
    val L10PressDelay = MemoryKey("l10_press_delay_ms")
    val Grudge = MemoryKey("l49_grudge")
    /** 〈忘掉一件事〉這一輪已經記過仇了（"1"）；通關後設回 "0"，重玩時才會再記一次。 */
    val GrudgePlanted = MemoryKey("l49_grudge_planted")
    val KeptShape = MemoryKey("l51_kept_shape")
    val Achievement = MemoryKey("l54_achievement")
    val DraftStage = MemoryKey("l55_draft_stage")
    /** 第 61 關玩家輸入的「稱呼」。全遊戲唯一存下的文字，只存本機，「忘記我」會清除。 */
    val Nickname = MemoryKey("l62_nickname")
    val FirstStars = MemoryKey("l75_first_stars")
    /** 第 6 章找回的碎片。 */
    val FragLeftEye = MemoryKey("frag_left_eye")
    val FragMouth = MemoryKey("frag_mouth")
    val FragRightEye = MemoryKey("frag_right_eye")
    val FragName = MemoryKey("frag_name")
    val FragMemory = MemoryKey("frag_memory")
    val DevMode = MemoryKey("l92_dev_mode")
    val LyingSwitch = MemoryKey("l117_lying_switch")
    val Annoying = MemoryKey("l107_annoying")
    /** 第 113 關的願望。刻意不顯示在紀錄頁（只有妳記得）。 */
    val Wish = MemoryKey("l113_wish")
    val FormLike = MemoryKey("l114_like")
    val KeepRemembering = MemoryKey("l120_keep")

    const val LEFT = "left"
    const val RIGHT = "right"
}
