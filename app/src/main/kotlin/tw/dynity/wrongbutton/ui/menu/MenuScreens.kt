package tw.dynity.wrongbutton.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.app.AppContainer
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.audio.rememberCuePlayer
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.legal.LegalKind
import tw.dynity.wrongbutton.ui.pro.findActivity
import tw.dynity.wrongbutton.core.engine.Speech
import tw.dynity.wrongbutton.core.engine.SpeechBubbleView
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.PlayerMemory
import tw.dynity.wrongbutton.core.ui.WbDialog
import tw.dynity.wrongbutton.core.ui.WbIcons

@Composable
fun ScreenScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(WbIcons.Back, contentDescription = stringResource(R.string.nav_back)) }
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        }
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

@Composable
fun TitleScreen(
    memory: PlayerMemory,
    /** （關卡 id, 顯示編號），全部通關時為 null。 */
    continueTarget: Pair<Int, Int>?,
    onContinue: (Int) -> Unit,
    onLevels: () -> Unit,
    onRecords: () -> Unit,
    onSettings: () -> Unit,
    pro: Boolean = false,
    onPro: () -> Unit = {},
    onLegal: (LegalKind) -> Unit = {},
) {
    val cue = rememberCuePlayer()
    val fresh = memory.levels.isEmpty()
    var poked by rememberSaveable { mutableStateOf(0) }
    val greetings = listOf(
        if (fresh) R.string.title_greeting_new else R.string.title_greeting_back,
        R.string.title_poke_1,
        R.string.title_poke_2,
    )
    val lineRes = greetings[poked.coerceAtMost(greetings.lastIndex)]
    val face = when {
        fresh && poked == 0 -> Expression(Mood.Sleeping)
        // 說謊特徵第一次出現在第 2 關，主選單上它不說謊。
        poked >= 2 -> Expression(Mood.Smug)
        else -> Expression(Mood.Normal)
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth > maxHeight
            val hero: @Composable () -> Unit = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                    ButtonCharacter(
                        face.mood,
                        face.tell,
                        if (wide) 120.dp else 150.dp,
                        Modifier
                            .testTag("title_button")
                            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                                poked++
                                cue(Cue.Pop)
                            },
                        pressed = pressed,
                    )
                    SpeechBubbleView(Speech(stringResource(lineRes), face, poked.toLong()), Modifier.widthIn(max = 360.dp))
                }
            }
            val menu: @Composable () -> Unit = {
                Column(Modifier.widthIn(max = 360.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (continueTarget != null) {
                        Button(onClick = { onContinue(continueTarget.first) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("menu_continue")) {
                            Text(stringResource(if (fresh) R.string.menu_start else R.string.menu_continue, continueTarget.second))
                        }
                    }
                    FilledTonalButton(onClick = onLevels, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("menu_levels")) {
                        Text(stringResource(R.string.menu_levels))
                    }
                    OutlinedButton(onClick = onRecords, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("menu_records")) {
                        Text(stringResource(R.string.menu_records))
                    }
                    OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("menu_settings")) {
                        Text(stringResource(R.string.menu_settings))
                    }
                    if (!pro) {
                        TextButton(onClick = onPro, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("menu_pro")) {
                            Text(stringResource(R.string.menu_pro))
                        }
                    }
                    LegalNotice(onLegal)
                }
            }
            if (wide) {
                Row(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { hero() }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { menu() }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
                ) {
                    hero()
                    menu()
                }
            }
        }
    }
}

private fun seasonTitle(season: Int) = when (season) {
    1 -> R.string.select_season_1
    2 -> R.string.select_season_2
    else -> R.string.select_season_3
}

/** 選關畫面上的一列：季標題、章標題或一關。 */
private sealed interface SelectEntry {
    data class Season(val res: Int) : SelectEntry
    data class Header(val chapter: Int) : SelectEntry
    data class LevelItem(val chapter: Int, val level: Level) : SelectEntry
}

@Composable
fun LevelSelectScreen(
    /** （章名, 關卡, 第幾季） */
    chapters: List<Triple<Int, List<Level>, Int>>,
    numberOf: (Int) -> Int,
    isUnlocked: (Int) -> Boolean,
    memory: PlayerMemory,
    onPlay: (Int) -> Unit,
    onBack: () -> Unit,
    needsPro: (Int) -> Boolean = { false },
) {
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val coroutine = androidx.compose.runtime.rememberCoroutineScope()
    // 「待破關」：已經解鎖、可以玩（不需要 Pro 或已經有 Pro）、但還沒通關的關卡。
    fun todo(level: Level) = isUnlocked(level.id) && !needsPro(level.id) && !memory.isCleared(level.id)
    var onlyTodo by rememberSaveable { mutableStateOf(false) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val todoCount = chapters.sumOf { (_, levels) -> levels.count(::todo) }
    val total = chapters.sumOf { it.second.size }

    // 目前要顯示的列（篩選後）。章節沒有待破關卡時，整章都不顯示。
    val entries = run {
        val list = mutableListOf<SelectEntry>()
        var seasonShown = 1
        chapters.forEachIndexed { c, (_, levels, season) ->
            val shown = if (onlyTodo) levels.filter(::todo) else levels
            if (shown.isEmpty()) return@forEachIndexed
            if (season > seasonShown) { seasonShown = season; list += SelectEntry.Season(seasonTitle(season)) }
            list += SelectEntry.Header(c)
            shown.forEach { list += SelectEntry.LevelItem(c, it) }
        }
        list
    }
    val headerAt = entries.withIndex().filter { it.value is SelectEntry.Header }.associate { (i, e) -> (e as SelectEntry.Header).chapter to i }
    // 捲到哪一章，上面的章節按鈕就顯示哪一章。
    val currentChapter by androidx.compose.runtime.remember(entries) {
        androidx.compose.runtime.derivedStateOf {
            val first = gridState.firstVisibleItemIndex
            headerAt.entries.filter { it.value <= first }.maxByOrNull { it.value }?.key ?: headerAt.keys.minOrNull() ?: 0
        }
    }
    fun jumpTo(chapter: Int) {
        if (onlyTodo && headerAt[chapter] == null) onlyTodo = false
        coroutine.launch {
            // 切換篩選後，列表要先重組才知道新的位置。
            kotlinx.coroutines.yield()
            val index = run {
                var seasonShown = 1
                var i = 0
                for ((c, triple) in chapters.withIndex()) {
                    val (_, levels, season) = triple
                    val shown = if (onlyTodo) levels.filter(::todo) else levels
                    if (shown.isEmpty()) continue
                    if (season > seasonShown) { seasonShown = season; i++ }
                    if (c == chapter) return@run i
                    i += 1 + shown.size
                }
                0
            }
            gridState.animateScrollToItem(index)
        }
    }

    Box(Modifier.fillMaxSize()) {
    ScreenScaffold(title = stringResource(R.string.select_title), onBack = onBack) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 章節切換：顯示目前捲到的章，點開可以跳到任何一章。
            val (curTitle, curLevels) = chapters[currentChapter.coerceIn(0, chapters.lastIndex)]
            val curCleared = curLevels.count { memory.isCleared(it.id) }
            val pickerCd = stringResource(R.string.select_picker_cd, stringResource(curTitle))
            Surface(
                onClick = { picking = true },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("select_chapter_picker").semantics { contentDescription = pickerCd },
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(curTitle), style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.select_chapter_progress, curCleared, curLevels.size), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text("  ▾", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            // 篩選：全部／待破關。
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.FilterChip(
                    selected = !onlyTodo,
                    onClick = { onlyTodo = false; coroutine.launch { gridState.scrollToItem(0) } },
                    label = { Text(stringResource(R.string.select_filter_all, total)) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("select_filter_all"),
                )
                androidx.compose.material3.FilterChip(
                    selected = onlyTodo,
                    onClick = { onlyTodo = true; coroutine.launch { gridState.scrollToItem(0) } },
                    label = { Text(stringResource(R.string.select_filter_todo, todoCount)) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("select_filter_todo"),
                )
            }
        }
        if (onlyTodo && entries.isEmpty()) {
            val waitingForPro = chapters.any { (_, levels) -> levels.any { isUnlocked(it.id) && needsPro(it.id) && !memory.isCleared(it.id) } }
            Text(
                stringResource(if (waitingForPro) R.string.select_todo_empty_pro else R.string.select_todo_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(32.dp).testTag("select_todo_empty"),
            )
        }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp).testTag("select_grid"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            entries.forEach { entry ->
                when (entry) {
                    is SelectEntry.Season -> item(key = "season_${entry.res}", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            stringResource(entry.res),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 24.dp, start = 4.dp).semantics { heading() },
                        )
                    }
                    is SelectEntry.Header -> item(key = "chapter_${entry.chapter}", span = { GridItemSpan(maxLineSpan) }) {
                        val (chapterTitle, levels) = chapters[entry.chapter]
                        val cleared = levels.count { memory.isCleared(it.id) }
                        Row(
                            Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(chapterTitle),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f).semantics { heading() },
                            )
                            Text(
                                stringResource(R.string.select_chapter_progress, cleared, levels.size),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    is SelectEntry.LevelItem -> item(key = entry.level.id) {
                        LevelCard(entry.level, numberOf, isUnlocked, needsPro, memory, onPlay, highlight = todo(entry.level))
                    }
                }
            }
        }
    }
    if (picking) {
        ChapterPicker(
            chapters = chapters,
            memory = memory,
            todoIn = { levels -> levels.count(::todo) },
            current = currentChapter,
            onPick = { c -> picking = false; jumpTo(c) },
            onClose = { picking = false },
        )
    }
    }
}

@Composable
private fun LevelCard(
    level: Level,
    numberOf: (Int) -> Int,
    isUnlocked: (Int) -> Boolean,
    needsPro: (Int) -> Boolean,
    memory: PlayerMemory,
    onPlay: (Int) -> Unit,
    highlight: Boolean,
) {
    val unlocked = isUnlocked(level.id)
    val pro = needsPro(level.id)
    val number = numberOf(level.id)
    val cleared = memory.isCleared(level.id)
    val status = stringResource(
        when {
            cleared -> R.string.select_cleared
            unlocked && pro -> R.string.pro_locked_cd
            unlocked -> R.string.select_open
            else -> R.string.select_locked
        },
    )
    val title = stringResource(level.titleRes)
    val cd = stringResource(R.string.select_card_cd, number, title, status)
    Surface(
        onClick = { onPlay(level.id) },
        enabled = unlocked,
        shape = RoundedCornerShape(20.dp),
        // 待破關的關卡用比較醒目的顏色，「全部」裡也一眼看得出來。
        color = when {
            highlight -> MaterialTheme.colorScheme.primaryContainer
            unlocked -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier
            .heightIn(min = 112.dp)
            .testTag("select_level_${level.id}")
            .semantics(mergeDescendants = true) { contentDescription = cd },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.select_number, number), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                if (!unlocked) Icon(WbIcons.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                if (cleared) Icon(WbIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                if (pro && !cleared) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text(
                            stringResource(R.string.pro_locked),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 章節清單：每一章的進度條與待破關數，點了就跳過去。 */
@Composable
private fun ChapterPicker(
    chapters: List<Triple<Int, List<Level>, Int>>,
    memory: PlayerMemory,
    todoIn: (List<Level>) -> Int,
    current: Int,
    onPick: (Int) -> Unit,
    onClose: () -> Unit,
) {
    androidx.activity.compose.BackHandler(onBack = onClose)
    tw.dynity.wrongbutton.core.ui.WbOverlay(
        maxWidth = 560.dp,
        modifier = Modifier.testTag("select_picker"),
        header = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("select_picker_close")) {
                    Icon(WbIcons.Back, contentDescription = stringResource(R.string.records_back))
                }
                Text(stringResource(R.string.select_picker_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            }
        },
    ) {
        var seasonShown = 0
        chapters.forEachIndexed { c, (titleRes, levels, season) ->
            if (season > seasonShown) {
                seasonShown = season
                Text(
                    stringResource(seasonTitle(season)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = if (season > 1) 8.dp else 0.dp).semantics { heading() },
                )
            }
            val cleared = levels.count { memory.isCleared(it.id) }
            val left = todoIn(levels)
            val title = stringResource(titleRes)
            val cd = stringResource(R.string.select_picker_row_cd, title, cleared, levels.size, left)
            Surface(
                onClick = { onPick(c) },
                shape = RoundedCornerShape(14.dp),
                color = if (c == current) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("select_pick_$c").clearAndSetSemantics { contentDescription = cd },
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (left > 0) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary) {
                                Text(
                                    stringResource(R.string.select_todo_badge, left),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(stringResource(R.string.select_chapter_progress, cleared, levels.size), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { if (levels.isEmpty()) 0f else cleared / levels.size.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit, onPro: () -> Unit = {}, onLegal: (LegalKind) -> Unit = {}) {
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val shop by container.monetization.state.collectAsStateWithLifecycle()
    val cue = rememberCuePlayer()
    var confirming by rememberSaveable { mutableStateOf(false) }
    var forgotten by rememberSaveable { mutableStateOf(false) }
    var versionTaps by rememberSaveable { mutableStateOf(0) }
    var contactFallback by rememberSaveable { mutableStateOf(false) }
    var askingCode by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(title = stringResource(R.string.settings_title), onBack = onBack) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SettingSwitch(stringResource(R.string.settings_sound), settings.sound, "settings_sound") { v ->
                    container.settings.update { it.copy(sound = v) }
                    cue(Cue.Toggle)
                }
                SettingSwitch(stringResource(R.string.settings_music), settings.music, "settings_music") { v ->
                    container.settings.update { it.copy(music = v) }
                }
                SettingSwitch(stringResource(R.string.settings_vibration), settings.vibration, "settings_vibration") { v ->
                    container.settings.update { it.copy(vibration = v) }
                }
                SettingSwitch(
                    stringResource(R.string.settings_reduce_motion),
                    settings.reduceMotion,
                    "settings_reduce_motion",
                    stringResource(R.string.settings_reduce_motion_desc),
                ) { v -> container.settings.update { it.copy(reduceMotion = v) } }
                PronounSetting(settings.pronoun) { p ->
                    container.settings.update { it.copy(pronoun = p) }
                    cue(Cue.Toggle)
                }
                ChoiceSetting(
                    label = stringResource(R.string.settings_theme),
                    description = null,
                    options = listOf(
                        tw.dynity.wrongbutton.core.settings.ThemeMode.System to stringResource(R.string.settings_theme_system),
                        tw.dynity.wrongbutton.core.settings.ThemeMode.Light to stringResource(R.string.settings_theme_light),
                        tw.dynity.wrongbutton.core.settings.ThemeMode.Dark to stringResource(R.string.settings_theme_dark),
                    ),
                    current = settings.theme,
                    tagPrefix = "settings_theme",
                ) { t ->
                    container.settings.update { it.copy(theme = t) }
                    cue(Cue.Toggle)
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(role = Role.Button, onClick = onPro)
                        .testTag("settings_pro")
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.settings_pro), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(if (shop.pro) R.string.settings_pro_owned else R.string.settings_pro_free),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    stringResource(R.string.settings_about),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 12.dp).semantics { heading() },
                )
                LinkRow(stringResource(R.string.legal_terms), "settings_terms") { onLegal(LegalKind.Terms) }
                LinkRow(stringResource(R.string.legal_privacy), "settings_privacy") { onLegal(LegalKind.Privacy) }
                val contactContext = androidx.compose.ui.platform.LocalContext.current
                LinkRow(stringResource(R.string.settings_contact), "settings_contact") {
                    // 手機上沒有能寄信的 App 時，改成顯示信箱讓玩家自己寄。
                    if (!openContactEmail(contactContext)) contactFallback = true
                }
                // 只有法規要求的地區（例如歐洲）才會出現：讓玩家隨時修改廣告的同意設定。
                val adMob = container.adMob
                val privacyOptions by (adMob?.privacyOptionsRequired ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsStateWithLifecycle()
                if (adMob != null && privacyOptions) {
                    val activity = androidx.compose.ui.platform.LocalContext.current.findActivity()
                    LinkRow(stringResource(R.string.settings_ad_privacy), "settings_ad_privacy") { activity?.let(adMob::showPrivacyOptions) }
                }
                Text(
                    stringResource(R.string.legal_consent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                Spacer(Modifier.height(16.dp))
                if (forgotten) {
                    SpeechBubbleView(Speech(stringResource(R.string.forget_after), Expression(Mood.Normal), 1))
                }
                OutlinedButton(
                    onClick = { confirming = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings_forget"),
                ) { Text(stringResource(R.string.settings_forget)) }

                // 版本號碼。連點 7 下會出現審查用解鎖碼的輸入框（給 Google Play 審查人員，見 ReviewAccess）。
                Text(
                    stringResource(R.string.settings_version, tw.dynity.wrongbutton.BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .testTag("settings_version")
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            versionTaps++
                            if (versionTaps >= tw.dynity.wrongbutton.core.monetization.ReviewAccess.TAPS_TO_OPEN) {
                                versionTaps = 0
                                askingCode = true
                            }
                        }
                        .padding(vertical = 12.dp),
                )
            }
        }
        if (contactFallback) {
            ContactFallbackDialog(onClose = { contactFallback = false })
        }
        if (askingCode) {
            ReviewCodeDialog(
                onSubmit = { code -> container.monetization.unlockForReview(code) },
                onClose = { askingCode = false },
            )
        }
        if (confirming) {
            WbDialog(face = Expression(Mood.Sad, Tell.Honest)) {
                Text(stringResource(R.string.forget_before), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { confirming = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.forget_cancel))
                    }
                    Button(
                        onClick = {
                            container.memory.update { MemoryOps.forget() }
                            container.director.reset()
                            confirming = false
                            forgotten = true
                            cue(Cue.Swipe)
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("forget_confirm"),
                    ) { Text(stringResource(R.string.forget_confirm)) }
                }
            }
        }
    }
}

/** 「你／妳」二選一。選項文字直接寫在程式裡：放在字串資源的話，「妳」本身也會被替換掉。 */
@Composable
private fun PronounSetting(current: tw.dynity.wrongbutton.core.settings.Pronoun, onChange: (tw.dynity.wrongbutton.core.settings.Pronoun) -> Unit) {
    ChoiceSetting(
        label = stringResource(R.string.settings_pronoun),
        description = stringResource(R.string.settings_pronoun_desc),
        options = listOf(tw.dynity.wrongbutton.core.settings.Pronoun.Neutral to "你", tw.dynity.wrongbutton.core.settings.Pronoun.Feminine to "妳"),
        current = current,
        tagPrefix = "settings_pronoun",
        onChange = onChange,
    )
}

/** 幾個選項選一個（一排 Chip）。 */
@Composable
private fun <T : Enum<T>> ChoiceSetting(
    label: String,
    description: String?,
    options: List<Pair<T, String>>,
    current: T,
    tagPrefix: String,
    onChange: (T) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        if (description != null) {
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.padding(top = 8.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { (value, text) ->
                androidx.compose.material3.FilterChip(
                    selected = current == value,
                    onClick = { if (current != value) onChange(value) },
                    label = { Text(text, style = MaterialTheme.typography.titleMedium) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("${tagPrefix}_${value.name.lowercase()}")
                        .semantics { role = Role.RadioButton },
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, tag: String, description: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .testTag(tag)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description != null) Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** 主畫面最下面：使用即同意服務條款，兩份文件都點得開。 */
@Composable
private fun LegalNotice(onLegal: (LegalKind) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.legal_consent),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("legal_consent"),
        )
        Row(horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = { onLegal(LegalKind.Terms) }, modifier = Modifier.heightIn(min = 48.dp).testTag("menu_terms")) {
                Text(stringResource(R.string.legal_terms), style = MaterialTheme.typography.labelLarge)
            }
            TextButton(onClick = { onLegal(LegalKind.Privacy) }, modifier = Modifier.heightIn(min = 48.dp).testTag("menu_privacy")) {
                Text(stringResource(R.string.legal_privacy), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** 設定頁裡「點了會打開另一頁」的一列。 */
@Composable
private fun LinkRow(label: String, tag: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 審查用解鎖碼的輸入框。碼正確就開通這台手機的 Pro。 */
@Composable
private fun ReviewCodeDialog(onSubmit: (String) -> Boolean, onClose: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Boolean?>(null) }
    WbDialog(face = Expression(Mood.Normal, Tell.Honest)) {
        Text(stringResource(R.string.review_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.review_body), style = MaterialTheme.typography.bodyMedium)
        if (result == true) {
            Text(stringResource(R.string.review_done), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("review_done"))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.records_back)) }
        } else {
            ReviewCodeForm(code, result, onCode = { code = it; result = null }, onCancel = onClose, onSubmit = { result = onSubmit(code) })
        }
    }
}

@Composable
private fun ReviewCodeForm(code: String, result: Boolean?, onCode: (String) -> Unit, onCancel: () -> Unit, onSubmit: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.OutlinedTextField(
            value = code,
            onValueChange = onCode,
            label = { Text(stringResource(R.string.review_field)) },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
            ),
            isError = result == false,
            supportingText = if (result == false) { { Text(stringResource(R.string.review_wrong)) } } else null,
            modifier = Modifier.fillMaxWidth().testTag("review_code"),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.review_cancel)) }
            Button(
                onClick = onSubmit,
                enabled = code.isNotBlank(),
                modifier = Modifier.heightIn(min = 48.dp).testTag("review_submit"),
            ) { Text(stringResource(R.string.review_submit)) }
        }
    }
}

/** 打開 email App 寫信給客服，先帶入版本與手機型號，方便我們查問題。沒有 email App 時什麼都不做。 */
private fun contactBody(context: android.content.Context): String = context.getString(
    R.string.contact_body,
    tw.dynity.wrongbutton.BuildConfig.VERSION_NAME,
    "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
    android.os.Build.VERSION.RELEASE,
)

/** 打開 email App 寫信給客服，先帶入版本與手機型號。沒有 App 能寄信時回傳 false。 */
private fun openContactEmail(context: android.content.Context): Boolean =
    tw.dynity.wrongbutton.core.crash.Contact.sendEmail(context, context.getString(R.string.contact_subject), contactBody(context))

/** 沒有能寄信的 App：顯示信箱，讓玩家複製後用自己習慣的方式寄。 */
@Composable
private fun ContactFallbackDialog(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var copied by remember { mutableStateOf(0) }
    WbDialog(face = Expression(Mood.Normal, Tell.Honest)) {
        Column(Modifier.testTag("contact_fallback"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.contact_fallback_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.contact_fallback_body), style = MaterialTheme.typography.bodyMedium)
            Text(tw.dynity.wrongbutton.core.crash.Contact.SUPPORT_EMAIL, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("contact_address"))
            Button(
                onClick = { if (tw.dynity.wrongbutton.core.crash.Contact.copy(context, tw.dynity.wrongbutton.core.crash.Contact.SUPPORT_EMAIL)) copied = 1 },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("contact_copy_address"),
            ) { Text(stringResource(if (copied == 1) R.string.contact_copied else R.string.contact_copy_address)) }
            OutlinedButton(
                onClick = { if (tw.dynity.wrongbutton.core.crash.Contact.copy(context, contactBody(context))) copied = 2 },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("contact_copy_info"),
            ) { Text(stringResource(if (copied == 2) R.string.contact_copied else R.string.contact_copy_info)) }
            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.records_back)) }
        }
    }
}
