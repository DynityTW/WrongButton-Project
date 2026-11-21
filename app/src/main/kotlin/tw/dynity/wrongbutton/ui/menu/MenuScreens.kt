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

@Composable
fun LevelSelectScreen(
    /** （章名, 關卡, 是否第二季） */
    chapters: List<Triple<Int, List<Level>, Boolean>>,
    numberOf: (Int) -> Int,
    isUnlocked: (Int) -> Boolean,
    memory: PlayerMemory,
    onPlay: (Int) -> Unit,
    onBack: () -> Unit,
    needsPro: (Int) -> Boolean = { false },
) {
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val coroutine = androidx.compose.runtime.rememberCoroutineScope()
    // 每一章標題在格狀清單裡的位置，給「跳到第 N 章」用。
    val headerIndex = remember(chapters) {
        var i = 0
        var seasonShown = false
        chapters.map { (_, levels, season2) ->
            if (season2 && !seasonShown) { seasonShown = true; i++ } // 「第二季」標題也佔一格
            val at = i
            i += 1 + levels.size
            at
        }
    }
    ScreenScaffold(title = stringResource(R.string.select_title), onBack = onBack) {
        // 章節捷徑：300 關不用一路捲下去。
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth().testTag("select_chapters"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(chapters.size) { c ->
                val (titleRes, levels) = chapters[c]
                val cleared = levels.count { memory.isCleared(it.id) }
                val title = stringResource(titleRes)
                val short = title.substringBefore('　')
                val cd = stringResource(R.string.select_jump_cd, title)
                androidx.compose.material3.AssistChip(
                    onClick = { coroutine.launch { gridState.animateScrollToItem(headerIndex[c]) } },
                    label = { Text("$short  " + stringResource(R.string.select_chapter_progress, cleared, levels.size)) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("select_jump_$c")
                        .semantics { contentDescription = cd },
                )
            }
        }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp).testTag("select_grid"),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            var seasonHeaderShown = false
            chapters.forEach { (chapterTitle, levels, season2) ->
            if (season2 && !seasonHeaderShown) {
                seasonHeaderShown = true
                item(key = "season_2", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(R.string.select_season_2),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 24.dp, start = 4.dp).semantics { heading() },
                    )
                }
            }
            item(key = "chapter_$chapterTitle", span = { GridItemSpan(maxLineSpan) }) {
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
            items(levels, key = { it.id }) { level ->
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
                    color = if (unlocked) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant,
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
            }
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(24.dp)) }
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
