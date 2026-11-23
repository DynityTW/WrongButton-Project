package tw.dynity.wrongbutton.ui.pro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.engine.Speech
import tw.dynity.wrongbutton.core.engine.SpeechBubbleView
import tw.dynity.wrongbutton.core.monetization.AdResult
import tw.dynity.wrongbutton.core.monetization.MockAd
import tw.dynity.wrongbutton.core.monetization.MonetizationRepository
import tw.dynity.wrongbutton.core.monetization.ProProduct
import tw.dynity.wrongbutton.core.monetization.PurchaseResult
import tw.dynity.wrongbutton.core.ui.WbDialog
import tw.dynity.wrongbutton.core.ui.WbOverlay
import tw.dynity.wrongbutton.levels.Levels
import tw.dynity.wrongbutton.ui.menu.ScreenScaffold
import kotlinx.coroutines.launch

/**
 * Pro 購買頁。一款講「介面會騙人」的遊戲，自己的付費頁一定要最誠實：
 * 價格寫在最上面、沒有倒數、沒有預先勾選、「以後再說」和購買一樣大、The Button 用誠實的臉說話。
 */
@Composable
fun ProScreen(
    repo: MonetizationRepository,
    store: tw.dynity.wrongbutton.core.monetization.Store,
    onBack: () -> Unit,
    onTerms: () -> Unit = {},
) {
    val state by repo.state.collectAsStateWithLifecycle()
    val offer by store.offer.collectAsStateWithLifecycle()
    // 每次打開都向 Google Play 查現在的價格與優惠（Play Console 改價或開優惠，這裡就會跟著變）。
    androidx.compose.runtime.LaunchedEffect(store) { store.refresh() }
    val activity = androidx.compose.ui.platform.LocalContext.current.findActivity()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var busy by rememberSaveable { mutableStateOf(false) }
    var confirming by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableIntStateOf(0) }
    val proLevels = Levels.all.size - Levels.FREE_LEVELS

    androidx.compose.foundation.layout.Box {
        ScreenScaffold(title = stringResource(R.string.pro_title), onBack = onBack) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .testTag("pro_screen"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val face = Expression(if (state.pro) Mood.Smug else Mood.Normal, Tell.Honest)
                // 還沒買的時候，The Button 會說幾句（誠實的）推銷台詞；點它換下一句。
                val pitches = pitchLines(offer, proLevels)
                var pitchIndex by rememberSaveable { mutableIntStateOf(kotlin.random.Random.nextInt(0, 1_000)) }
                ButtonCharacter(
                    face.mood,
                    face.tell,
                    110.dp,
                    Modifier
                        .testTag("pro_character")
                        .clickable(role = androidx.compose.ui.semantics.Role.Button) {
                            pitchIndex++
                            message = 0
                        },
                )
                val text = when {
                    message != 0 -> stringResource(message)
                    state.pro -> stringResource(R.string.pro_line_owned)
                    else -> pitches[pitchIndex % pitches.size]
                }
                SpeechBubbleView(Speech(text, face, (text.hashCode().toLong() shl 8) + pitchIndex), Modifier.widthIn(max = 420.dp).testTag("pro_speech"))

                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                offer?.price ?: stringResource(R.string.pro_price_loading),
                                style = if (offer != null) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.semantics { heading() }.testTag("pro_price"),
                            )
                            offer?.percentOff?.let { pct ->
                                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primary) {
                                    Text(
                                        stringResource(R.string.pro_sale, pct),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp).testTag("pro_sale"),
                                    )
                                }
                            }
                        }
                        Text(stringResource(R.string.pro_once), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        listOf(
                            stringResource(R.string.pro_benefit_levels, Levels.FREE_LEVELS + 1, Levels.all.size, proLevels),
                            stringResource(R.string.pro_benefit_hints),
                            stringResource(R.string.pro_benefit_free_stays, Levels.FREE_LEVELS),
                        ).forEach { Text("・$it", style = MaterialTheme.typography.bodyLarge) }
                    }
                }

                // 購買或恢復失敗時，顯示 Google Play 回傳的原因（小字），方便判斷是商品設定、帳號還是安裝來源的問題。
                val storeError by store.lastError.collectAsStateWithLifecycle()
                if (message == R.string.pro_line_store_failed && storeError != null) {
                    Text(
                        storeError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().testTag("pro_error"),
                    )
                }

                if (state.pro) {
                    Text(stringResource(R.string.pro_owned), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("pro_owned"))
                } else {
                    Button(
                        onClick = { confirming = true },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("pro_buy"),
                    ) {
                        Text(offer?.let { stringResource(R.string.pro_buy, it.price) } ?: stringResource(R.string.pro_buy_plain))
                    }
                    Text(
                        stringResource(R.string.pro_terms_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    TextButton(onClick = onTerms, modifier = Modifier.heightIn(min = 48.dp).testTag("pro_terms")) {
                        Text(stringResource(R.string.legal_terms))
                    }
                    OutlinedButton(
                        onClick = {
                            busy = true
                            scope.launch {
                                message = when (store.restore()) {
                                    tw.dynity.wrongbutton.core.monetization.RestoreResult.Found -> R.string.pro_line_restored
                                    tw.dynity.wrongbutton.core.monetization.RestoreResult.NotFound -> R.string.pro_line_nothing_to_restore
                                    tw.dynity.wrongbutton.core.monetization.RestoreResult.Failed -> R.string.pro_line_store_failed
                                }
                                busy = false
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("pro_restore"),
                    ) { Text(stringResource(R.string.pro_restore)) }
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("pro_later")) {
                        Text(stringResource(R.string.pro_later))
                    }
                }
            }
        }
        if (confirming) {
            WbDialog(face = Expression(Mood.Normal, Tell.Honest)) {
                Text(stringResource(R.string.pro_confirm_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Text(
                    offer?.let { stringResource(R.string.pro_confirm_body, it.price) } ?: stringResource(R.string.pro_confirm_body_plain),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { confirming = false }, modifier = Modifier.heightIn(min = 48.dp).testTag("pro_confirm_cancel")) {
                        Text(stringResource(R.string.pro_confirm_cancel))
                    }
                    Button(
                        onClick = {
                            confirming = false
                            val host = activity ?: return@Button
                            busy = true
                            scope.launch {
                                message = when (store.purchasePro(host)) {
                                    PurchaseResult.Purchased -> R.string.pro_line_thanks
                                    PurchaseResult.Cancelled -> 0
                                    PurchaseResult.Pending -> R.string.pro_line_pending
                                    PurchaseResult.Failed -> R.string.pro_line_store_failed
                                }
                                busy = false
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("pro_confirm_buy"),
                    ) { Text(stringResource(R.string.pro_confirm_buy)) }
                }
            }
        }
    }
}

/**
 * 模擬廣告，蓋在整個畫面最上層。清楚標示是模擬廣告；不模仿任何真實廣告、品牌或系統畫面。
 * - 獎勵廣告：只在玩家自己按「看廣告顯示提示」之後出現；倒數完才能領取，中途可以不看。
 * - 全版廣告：只在關卡之間（離開結算畫面時）出現；倒數完才能關閉。
 */
@Composable
fun MockAdOverlay(ad: MockAd, onFinish: (AdResult) -> Unit) {
    var left by rememberSaveable(ad.serial) { mutableIntStateOf(ad.seconds) }
    LaunchedEffect(ad.serial) {
        while (left > 0) {
            delay(1_000)
            left--
        }
    }
    WbOverlay(modifier = Modifier.testTag("mock_ad")) {
        Text(stringResource(R.string.ad_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        val interstitial = ad.kind == tw.dynity.wrongbutton.core.monetization.MockAdKind.Interstitial
        Text(
            stringResource(if (interstitial) R.string.ad_break_title else R.string.ad_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)) {
            Text(
                if (interstitial) stringResource(R.string.ad_break_body, tw.dynity.wrongbutton.core.monetization.AdRules.CLEARS_PER_AD_BREAK) else stringResource(R.string.ad_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(20.dp),
            )
        }
        Text(
            when {
                left > 0 && interstitial -> stringResource(R.string.ad_break_countdown, left)
                left > 0 -> stringResource(R.string.ad_countdown, left)
                interstitial -> stringResource(R.string.ad_break_done)
                else -> stringResource(R.string.ad_done)
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("mock_ad_countdown"),
        )
        if (interstitial) {
            Button(
                onClick = { onFinish(AdResult.Rewarded) },
                enabled = left == 0,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("mock_ad_break_close"),
            ) { Text(stringResource(R.string.ad_break_close)) }
        } else {
            Button(
                onClick = { onFinish(AdResult.Rewarded) },
                enabled = left == 0,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("mock_ad_claim"),
            ) { Text(stringResource(R.string.ad_claim)) }
            TextButton(onClick = { onFinish(AdResult.Dismissed) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("mock_ad_close")) {
                Text(stringResource(R.string.ad_close))
            }
        }
    }
}

/** 從 Compose 的 Context 找到 Activity（Google Play 結帳畫面需要它）。 */
internal tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Pro 頁的推銷台詞：幽默但誠實（不罪惡感綁架、不假倒數——這個遊戲整整一章都在笑那些手法）。
 * 和價格有關的句子用 Google Play 上的實際價格算，優惠期間也會跟著變。
 */
@Composable
private fun pitchLines(offer: tw.dynity.wrongbutton.core.monetization.ProOffer?, proLevels: Int): List<String> {
    val lines = mutableListOf(stringResource(R.string.pro_line_pitch))
    val twd = offer?.currency == "TWD"
    if (offer != null) lines += stringResource(if (twd) R.string.pro_joke_bento else R.string.pro_joke_coffee)
    val micros = offer?.priceMicros
    if (twd && micros != null && micros > 0) {
        lines += stringResource(R.string.pro_joke_per_level, tw.dynity.wrongbutton.core.monetization.ProPitch.perLevel(micros, proLevels))
    }
    lines += stringResource(R.string.pro_joke_levels, proLevels)
    lines += stringResource(R.string.pro_joke_no_ads)
    lines += stringResource(R.string.pro_joke_no_subscription)
    lines += stringResource(R.string.pro_joke_caffeine)
    lines += stringResource(R.string.pro_joke_still_lies)
    lines += stringResource(R.string.pro_joke_look_at_me)
    lines += stringResource(R.string.pro_joke_teaser)
    lines += stringResource(R.string.pro_joke_sleep)
    lines += stringResource(R.string.pro_joke_airplane, tw.dynity.wrongbutton.levels.Levels.FREE_LEVELS + 1)
    lines += stringResource(R.string.pro_joke_hints)
    lines += stringResource(R.string.pro_joke_unlimited)
    lines += stringResource(R.string.pro_joke_remember)
    lines += stringResource(R.string.pro_joke_once)
    lines += stringResource(R.string.pro_joke_two_people)
    // 有優惠時說是真的優惠；沒有優惠時才說「沒有限時倒數」，免得自相矛盾。
    lines += stringResource(if (offer?.percentOff != null) R.string.pro_joke_real_sale else R.string.pro_joke_no_countdown)
    return lines
}
