package tw.dynity.wrongbutton.core.monetization

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/**
 * 判斷購買紀錄的純邏輯（不碰 BillingClient，方便測試）。
 */
object PurchaseRules {
    private fun isPro(p: Purchase) = ProProduct.PRODUCT_ID in p.products

    /** 已經付款完成的 Pro。 */
    fun owned(purchases: List<Purchase>): List<Purchase> =
        purchases.filter { isPro(it) && it.purchaseState == Purchase.PurchaseState.PURCHASED }

    /** 付款還在處理中的 Pro（例如超商代碼繳費還沒繳）。這時候還不能開通。 */
    fun pending(purchases: List<Purchase>): Boolean =
        purchases.any { isPro(it) && it.purchaseState == Purchase.PurchaseState.PENDING }

    /** 付款完成但還沒確認的：3 天內沒確認，Google 會自動退款。 */
    fun needsAcknowledge(purchases: List<Purchase>): List<Purchase> = owned(purchases).filter { !it.isAcknowledged }

    fun result(purchases: List<Purchase>): PurchaseResult = when {
        owned(purchases).isNotEmpty() -> PurchaseResult.Purchased
        pending(purchases) -> PurchaseResult.Pending
        else -> PurchaseResult.Failed
    }
}

/**
 * Google Play 結帳。
 *
 * - 啟動時連上 Play、查價格、查這個帳號買過什麼（＝自動恢復購買）。
 * - Pro 以 Play 的購買紀錄為準：查詢成功而且沒有 Pro（例如退款了），就把快取改回免費版；
 *   查詢失敗（離線）時不動快取，已經買的人離線也能繼續玩。
 * - 付款完成的購買一定要確認（acknowledge），否則 3 天後 Google 會自動退款。
 */
class PlayBillingStore(
    context: Context,
    private val cache: MonetizationRepository,
    private val scope: CoroutineScope,
) : Store, PurchasesUpdatedListener {

    private val _offer = MutableStateFlow<ProOffer?>(null)
    override val offer: StateFlow<ProOffer?> = _offer.asStateFlow()

    /** 目前要結帳的方案（最便宜的購買選項或優惠）。 */
    private var chosenToken: String? = null

    private val _lastError = MutableStateFlow<String?>(null)
    override val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private fun fail(step: String, result: BillingResult?) {
        _lastError.value = if (result == null) step else "$step：${codeName(result.responseCode)}（${result.responseCode}）" +
            result.debugMessage.takeIf { it.isNotBlank() }?.let { "　$it" }.orEmpty()
    }

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private val connectLock = Mutex()
    private var details: ProductDetails? = null
    private var waiting: CompletableDeferred<PurchaseResult>? = null

    init {
        scope.launch(Dispatchers.Main) {
            if (connect()) {
                loadProduct()
                restore()
            }
        }
    }

    /** BillingClient 的呼叫一律在主執行緒；任何例外都記錄下來、當作失敗，不讓遊戲閃退。 */
    private suspend fun <T> safe(fallback: T, block: suspend () -> T): T = withContext(Dispatchers.Main) {
        try {
            block()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            tw.dynity.wrongbutton.core.crash.CrashReport.recordNonFatal(e)
            fallback
        }
    }

    private suspend fun connect(): Boolean = safe(false) { connectInner() }

    private suspend fun connectInner(): Boolean = connectLock.withLock {
        if (client.isReady) return@withLock true
        suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode != BillingResponseCode.OK) fail("連線 Google Play 失敗", result)
                    if (cont.isActive) cont.resume(result.responseCode == BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() {
                    // 已開啟自動重新連線；這裡只要確保等待中的呼叫不會永遠卡住。
                    fail("和 Google Play 的連線中斷", null)
                    if (cont.isActive) cont.resume(false)
                }
            })
        }
    }

    private suspend fun loadProduct(): ProductDetails? = safe(null) { loadProductInner() }

    /** 打開 Pro 頁時呼叫：每次都重新查，Play Console 改了價格或開了優惠，玩家馬上看得到。 */
    override suspend fun refresh() {
        if (connect()) loadProduct()
    }

    private suspend fun loadProductInner(): ProductDetails? {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(ProProduct.PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingResponseCode.OK) {
            fail("查詢商品失敗", result.billingResult)
            return null
        }
        val found = result.productDetailsList?.firstOrNull { it.productId == ProProduct.PRODUCT_ID }
        if (found == null) {
            fail(
                "找不到商品 ${ProProduct.PRODUCT_ID}：商品可能還沒啟用、剛建立還沒生效、這個國家沒有價格，" +
                    "或這個 Google 帳號不是測試人員／App 不是從 Play 安裝的",
                null,
            )
            return null
        }
        _lastError.value = null
        details = found
        // 一個商品可以有好幾個方案（購買選項、限時優惠）；只會列出這個帳號現在買得到的。挑最便宜的。
        val offers = found.oneTimePurchaseOfferDetailsList ?: listOfNotNull(found.oneTimePurchaseOfferDetails)
        val candidates = offers.map {
            OfferCandidate(
                token = it.offerToken,
                priceMicros = it.priceAmountMicros,
                formattedPrice = it.formattedPrice,
                fullPriceMicros = it.fullPriceMicros,
                percentOff = it.discountDisplayInfo?.percentageDiscount,
            )
        }
        val best = OfferRules.pick(candidates)
        chosenToken = best?.token
        _offer.value = best?.let { ProOffer(it.formattedPrice, OfferRules.percentOff(it)) }
        return found
    }

    override suspend fun purchasePro(activity: Activity): PurchaseResult = safe(PurchaseResult.Failed) { purchaseInner(activity) }

    private suspend fun purchaseInner(activity: Activity): PurchaseResult {
        if (!connect()) return PurchaseResult.Failed
        // 結帳前再查一次，確保用的是現在的價格與優惠。
        val product = loadProduct() ?: return PurchaseResult.Failed
        val offerToken = chosenToken
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .apply { if (offerToken != null) setOfferToken(offerToken) }
            .build()
        val flow = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams)).build()

        val deferred = CompletableDeferred<PurchaseResult>()
        waiting?.complete(PurchaseResult.Cancelled)
        waiting = deferred
        val launch = client.launchBillingFlow(activity, flow)
        when (launch.responseCode) {
            BillingResponseCode.OK -> Unit
            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                waiting = null
                return if (restore() == RestoreResult.Found) PurchaseResult.Purchased else PurchaseResult.Failed
            }
            BillingResponseCode.USER_CANCELED -> {
                waiting = null
                return PurchaseResult.Cancelled
            }
            else -> {
                waiting = null
                fail("無法開啟付款畫面", launch)
                return PurchaseResult.Failed
            }
        }
        return deferred.await()
    }

    /** Play 通知購買結果（包含之後才付款完成的待處理購買）。 */
    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        val deferred = waiting
        waiting = null
        when (result.responseCode) {
            BillingResponseCode.OK -> scope.launch(Dispatchers.Main) {
                val list = purchases.orEmpty()
                grant(list)
                deferred?.complete(PurchaseRules.result(list))
            }
            BillingResponseCode.USER_CANCELED -> deferred?.complete(PurchaseResult.Cancelled)
            BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch(Dispatchers.Main) {
                deferred?.complete(if (restore() == RestoreResult.Found) PurchaseResult.Purchased else PurchaseResult.Failed)
            }
            else -> {
                fail("付款沒有完成", result)
                deferred?.complete(PurchaseResult.Failed)
            }
        }
    }

    override suspend fun restore(): RestoreResult = safe(RestoreResult.Failed) { restoreInner() }

    private suspend fun restoreInner(): RestoreResult {
        if (!connect()) return RestoreResult.Failed
        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        )
        if (result.billingResult.responseCode != BillingResponseCode.OK) return RestoreResult.Failed
        val owned = PurchaseRules.owned(result.purchasesList)
        grant(result.purchasesList)
        // 查詢成功、而且沒有 Pro（例如已經退款）：回到免費版。
        if (owned.isEmpty()) cache.setPro(false)
        return if (owned.isEmpty()) RestoreResult.NotFound else RestoreResult.Found
    }

    /** 開通已付款的 Pro，並確認還沒確認的購買。 */
    private suspend fun grant(purchases: List<Purchase>) {
        if (PurchaseRules.owned(purchases).isNotEmpty()) cache.setPro(true)
        PurchaseRules.needsAcknowledge(purchases).forEach { p ->
            safe(Unit) { client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) }
        }
    }

    private fun codeName(code: Int): String = when (code) {
        BillingResponseCode.OK -> "OK"
        BillingResponseCode.USER_CANCELED -> "使用者取消"
        BillingResponseCode.SERVICE_UNAVAILABLE -> "Google Play 服務暫時無法使用（網路？）"
        BillingResponseCode.BILLING_UNAVAILABLE -> "這個帳號或裝置不能用 Google Play 結帳（Play 商店版本、帳號或國家）"
        BillingResponseCode.ITEM_UNAVAILABLE -> "商品無法購買（未啟用或這個國家沒有價格）"
        BillingResponseCode.DEVELOPER_ERROR -> "App 設定錯誤（通常是 App 不是從 Play 安裝的，或簽名不符）"
        BillingResponseCode.ERROR -> "Google Play 發生錯誤"
        BillingResponseCode.ITEM_ALREADY_OWNED -> "已經擁有"
        BillingResponseCode.ITEM_NOT_OWNED -> "沒有擁有這個商品"
        BillingResponseCode.SERVICE_DISCONNECTED -> "和 Google Play 的連線中斷"
        BillingResponseCode.FEATURE_NOT_SUPPORTED -> "這支手機不支援這項功能"
        BillingResponseCode.NETWORK_ERROR -> "網路錯誤"
        else -> "未知的回應"
    }
}
