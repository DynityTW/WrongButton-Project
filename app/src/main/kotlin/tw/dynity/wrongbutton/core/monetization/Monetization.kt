package tw.dynity.wrongbutton.core.monetization

import androidx.datastore.core.DataStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

/**
 * 付費與廣告的狀態。**和 PlayerMemory 分開存**：「讓 The Button 忘記我」不會清掉 Pro，也不會清掉已經看過廣告換到的提示。
 *
 * 正式版（release）用 Google Play 結帳（[PlayBillingStore]）與 AdMob（[AdMobService]）；
 * 測試版（debug）與自動測試用模擬的 [MockStore] 與 [MockAdService]，不連網、不扣款。
 * Pro 以 Play 查詢到的購買紀錄為準，這裡只當快取（離線時也能玩已經買的關卡）。
 */
@Serializable
data class MonetizationState(
    val pro: Boolean = false,
    /** 每一關已經靠看廣告解開到第幾階提示（關卡 id → 階數）。解開過的提示之後再看不用再看廣告。 */
    val adHints: Map<Int, Int> = emptyMap(),
    /** 上一次全版廣告之後又過了幾關（重玩也算）。存起來，關掉 App 再開也不會歸零。 */
    val clearsSinceAd: Int = 0,
    /** 用審查用解鎖碼開通的（只在這台手機）。Google Play 查不到購買紀錄時也不會被收回。 */
    val reviewAccess: Boolean = false,
)

/** 廣告規則。 */
object AdRules {
    /** 第 1 階提示免費；從這一階開始，每一階第一次看要先看一則獎勵廣告。 */
    const val FIRST_AD_HINT_TIER = 2

    /** 免費版每過幾關，在離開結算畫面時播一則全版廣告。 */
    const val CLEARS_PER_AD_BREAK = 5
}

/** Pro 的商品資訊。 */
object ProProduct {
    /** Google Play Console 裡「應用程式內產品」的商品 ID。建立後永遠不能改。 */
    const val PRODUCT_ID = "wrongbutton_pro"

    /** 還沒從 Play 拿到價格時（離線、模擬版）顯示的價格。 */
    const val PRICE_TEXT = "NT$120"
}

sealed interface PurchaseResult {
    data object Purchased : PurchaseResult
    data object Cancelled : PurchaseResult

    /** 付款還在處理（例如選了超商代碼繳費）。付完之後 Play 會通知，Pro 會自動開通。 */
    data object Pending : PurchaseResult

    /** 沒有連上 Google Play、商品還沒上架等。 */
    data object Failed : PurchaseResult
}

enum class RestoreResult { Found, NotFound, Failed }

/** 付費介面。 */
interface Store {
    /** 要顯示的價格（Play 查到的當地價格；還沒查到時是 null）。 */
    val price: StateFlow<String?>

    /** 模擬版：畫面上要標示「模擬購買」，並提供測試用的取消按鈕。 */
    val isMock: Boolean

    /** 最近一次失敗的原因（Google Play 的回應代碼與說明），顯示在 Pro 頁方便找問題。沒有失敗時是 null。 */
    val lastError: StateFlow<String?> get() = MutableStateFlow(null)

    suspend fun purchasePro(activity: android.app.Activity): PurchaseResult

    /** 恢復購買：向 Play 查詢這個 Google 帳號買過的東西。 */
    suspend fun restore(): RestoreResult
}

/** Google Play 結帳無法建立時（理論上不會發生）：不能買，但已經買過的快取照常有效，遊戲不會因此閃退。 */
class UnavailableStore : Store {
    override val price: StateFlow<String?> = MutableStateFlow(null)
    override val isMock = false
    override val lastError: StateFlow<String?> = MutableStateFlow("Google Play 結帳元件無法建立")
    override suspend fun purchasePro(activity: android.app.Activity): PurchaseResult = PurchaseResult.Failed
    override suspend fun restore(): RestoreResult = RestoreResult.Failed
}

/** 模擬的付費：直接改本機狀態，不連網、不扣款。 */
class MockStore(private val cache: MonetizationRepository) : Store {
    override val price: StateFlow<String?> = MutableStateFlow(ProProduct.PRICE_TEXT)
    override val isMock = true

    override suspend fun purchasePro(activity: android.app.Activity): PurchaseResult {
        cache.setPro(true)
        return PurchaseResult.Purchased
    }

    override suspend fun restore(): RestoreResult = if (cache.state.value.pro) RestoreResult.Found else RestoreResult.NotFound
}

class MonetizationRepository(
    private val store: DataStore<MonetizationState>,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(MonetizationState())
    private val writeLock = Mutex()
    val state: StateFlow<MonetizationState> = _state.asStateFlow()

    init {
        scope.launch {
            val stored = store.data.first()
            // 讀檔完成前如果已經有改動（例如很早就買了 Pro），不能被舊資料蓋掉：兩邊合併。
            // 這份狀態只會增加（買了 Pro、看過的提示變多），所以合併就是取兩邊的聯集與最大值。
            val before = _state.value
            _state.update { current -> merge(stored, current) }
            if (_state.value != stored && before != MonetizationState()) persist()
        }
    }

    private fun persist() {
        scope.launch { writeLock.withLock { val snapshot = _state.value; store.updateData { snapshot } } }
    }

    companion object {
        fun merge(a: MonetizationState, b: MonetizationState) = MonetizationState(
            pro = a.pro || b.pro,
            adHints = (a.adHints.keys + b.adHints.keys).associateWith { maxOf(a.adHints[it] ?: 0, b.adHints[it] ?: 0) },
            clearsSinceAd = maxOf(a.clearsSinceAd, b.clearsSinceAd),
            reviewAccess = a.reviewAccess || b.reviewAccess,
        )
    }

    fun update(transform: (MonetizationState) -> MonetizationState) {
        _state.update(transform)
        persist()
    }

    /** Pro 的快取。正式版只由 [PlayBillingStore] 依 Play 的購買紀錄設定。 */
    fun setPro(pro: Boolean) {
        // 審查用解鎖碼開通的，不會因為 Google Play 查不到購買紀錄而被收回。
        val effective = pro || _state.value.reviewAccess
        if (_state.value.pro != effective) update { it.copy(pro = effective) }
    }

    /** 輸入審查用解鎖碼：正確就開通這台手機的 Pro，回傳 true。 */
    fun unlockForReview(code: String): Boolean {
        if (!ReviewAccess.matches(code)) return false
        update { it.copy(pro = true, reviewAccess = true) }
        return true
    }

    /** 測試與模擬版用：直接開通 Pro。 */
    fun purchasePro() = setPro(true)

    /** 模擬版用：取消 Pro，方便測試免費版的流程。正式版畫面上沒有這個按鈕。 */
    fun revokeProForTesting() = update { it.copy(pro = false, reviewAccess = false) }

    /** 過了一關。Pro 不計數。 */
    fun recordClear() {
        if (_state.value.pro) return
        update { it.copy(clearsSinceAd = it.clearsSinceAd + 1) }
    }

    /** 現在離開結算畫面時，要不要先播一則全版廣告。 */
    fun adBreakDue(): Boolean = _state.value.let { !it.pro && it.clearsSinceAd >= AdRules.CLEARS_PER_AD_BREAK }

    /** 全版廣告播過了（或沒有廣告可播），重新計數。 */
    fun adBreakDone() = update { it.copy(clearsSinceAd = 0) }

    fun recordAdHint(levelId: Int, tier: Int) = update { s ->
        s.copy(adHints = s.adHints + (levelId to maxOf(tier, s.adHints[levelId] ?: 0)))
    }
}

sealed interface AdResult {
    /** 看完了，給獎勵。 */
    data object Rewarded : AdResult

    /** 玩家中途關掉，不給獎勵。 */
    data object Dismissed : AdResult

    /** 沒有廣告可以播（沒網路、沒有填充）。**不能因此卡住玩家**：一律直接給提示。 */
    data object NotAvailable : AdResult
}

/** 廣告介面。 */
interface AdService {
    /** 獎勵廣告：看完才給提示。 */
    suspend fun showRewarded(): AdResult

    /** 全版（插頁式）廣告：只在關卡之間播，播完或關掉都一樣繼續。 */
    suspend fun showInterstitial(): AdResult
}

enum class MockAdKind { Rewarded, Interstitial }

/** 畫面上正在播的模擬廣告。 */
data class MockAd(val serial: Long, val seconds: Int, val kind: MockAdKind = MockAdKind.Rewarded)

/**
 * 模擬的獎勵廣告：在畫面最上層蓋一張清楚標示「模擬廣告」的卡片，倒數 [seconds] 秒後可以領取。
 * 不模仿任何真實廣告或品牌，也不出現在關卡畫面裡（只會在玩家自己按「顯示提示」之後出現）。
 */
class MockAdService(private val seconds: Int = 5, private val interstitialSeconds: Int = 5) : AdService {
    private val _current = MutableStateFlow<MockAd?>(null)
    val current: StateFlow<MockAd?> = _current.asStateFlow()
    private var pending: CompletableDeferred<AdResult>? = null
    private var serial = 0L

    override suspend fun showRewarded(): AdResult = show(MockAd(++serial, seconds, MockAdKind.Rewarded))

    override suspend fun showInterstitial(): AdResult = show(MockAd(++serial, interstitialSeconds, MockAdKind.Interstitial))

    private suspend fun show(ad: MockAd): AdResult {
        pending?.complete(AdResult.Dismissed)
        val deferred = CompletableDeferred<AdResult>()
        pending = deferred
        _current.value = ad
        return deferred.await()
    }

    /** 由模擬廣告的畫面呼叫。 */
    fun finish(result: AdResult) {
        _current.value = null
        pending?.complete(result)
        pending = null
    }
}

/** 提示要不要先看廣告。關卡引擎只認這個介面。 */
interface HintGate {
    /** 第 [tier] 階提示（從 1 開始）是否不用看廣告就能看。 */
    fun isFree(levelId: Int, tier: Int): Boolean

    /** 看廣告解開第 [tier] 階提示。回傳 true 代表可以顯示。 */
    suspend fun unlock(levelId: Int, tier: Int): Boolean

    /** 狀態改變（買了 Pro、看了廣告）時會變的值，給畫面重組用。 */
    val changes: StateFlow<*>

    companion object {
        /** 測試或不需要廣告時用：提示永遠免費。 */
        val AlwaysFree = object : HintGate {
            override fun isFree(levelId: Int, tier: Int) = true
            override suspend fun unlock(levelId: Int, tier: Int) = true
            override val changes: StateFlow<*> = MutableStateFlow(Unit)
        }
    }
}

/**
 * 第 1 階提示免費；第 2 階起，每一階第一次看要先看一則廣告，看過就記下來，之後再看不用再看。Pro 全部免費。
 */
class AdHintGate(private val repo: MonetizationRepository, private val ads: AdService) : HintGate {
    override val changes: StateFlow<MonetizationState> get() = repo.state

    override fun isFree(levelId: Int, tier: Int): Boolean {
        val s = repo.state.value
        return s.pro || tier < AdRules.FIRST_AD_HINT_TIER || (s.adHints[levelId] ?: 0) >= tier
    }

    override suspend fun unlock(levelId: Int, tier: Int): Boolean {
        if (isFree(levelId, tier)) return true
        return when (ads.showRewarded()) {
            AdResult.Rewarded, AdResult.NotAvailable -> {
                repo.recordAdHint(levelId, tier)
                true
            }
            AdResult.Dismissed -> false
        }
    }
}
