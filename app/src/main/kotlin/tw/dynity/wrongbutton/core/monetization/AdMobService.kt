package tw.dynity.wrongbutton.core.monetization

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * AdMob：提示用的獎勵廣告，與每 5 關一次的全版（插頁式）廣告。
 *
 * - 先透過 Google 的同意表單（UMP）取得同意，[canRequestAds] 為 true 之後才初始化、才載入廣告。
 * - 廣告預先載入；要播的時候還沒載好，最多等幾秒，等不到就回傳 [AdResult.NotAvailable]，
 *   遊戲一律放行（提示直接給、全版廣告跳過），**不會因為沒網路或沒廣告卡住玩家**。
 * - 廣告內容分級上限設為 T（青少年），因為遊戲的目標年齡是 13 歲以上。
 */
class AdMobService(
    context: Context,
    private val rewardedUnitId: String,
    private val interstitialUnitId: String,
) : AdService {
    private val app = context.applicationContext
    private var activityRef = WeakReference<Activity>(null)
    private val started = AtomicBoolean(false)
    private var rewarded: RewardedAd? = null
    private var interstitial: InterstitialAd? = null
    private var loadingRewarded = false
    private var loadingInterstitial = false

    private val _privacyOptionsRequired = MutableStateFlow(false)

    /** 依地區法規（例如歐洲），設定頁要提供「廣告隱私權選項」。 */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /** MainActivity 在 onResume 呼叫：廣告需要一個正在顯示的 Activity 才能播。 */
    fun attach(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    /**
     * App 啟動時呼叫：更新同意狀態，需要時顯示同意表單，然後才開始載入廣告。
     * 上次已經同意過的話，不用等表單就先開始。
     */
    fun gatherConsent(activity: Activity) = guard {
        attach(activity)
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                guard {
                    // 畫面已經關掉（例如切換稱呼時重建）就不顯示表單，下次開啟再問。
                    if (activity.isFinishing || activity.isDestroyed) return@guard
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                        guard {
                            updatePrivacyOptions(consent)
                            if (consent.canRequestAds()) start()
                        }
                    }
                }
            },
            {
                // 沒網路之類：沿用上次的同意結果。
                guard { if (consent.canRequestAds()) start() }
            },
        )
        if (consent.canRequestAds()) start()
    }

    /** 設定頁的「廣告隱私權選項」：讓玩家隨時修改同意的內容。 */
    fun showPrivacyOptions(activity: Activity) = guard {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            updatePrivacyOptions(UserMessagingPlatform.getConsentInformation(activity))
        }
    }

    /** 廣告 SDK 丟出的任何例外都記錄下來，不讓遊戲閃退（最多就是沒有廣告，提示照給）。 */
    private fun guard(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            tw.dynity.wrongbutton.core.crash.CrashReport.recordNonFatal(e)
        }
    }

    private val main = android.os.Handler(android.os.Looper.getMainLooper())

    private fun updatePrivacyOptions(consent: ConsentInformation) {
        _privacyOptionsRequired.value =
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun start() {
        if (!started.compareAndSet(false, true)) return
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
                .build(),
        )
        guard {
            MobileAds.initialize(app) {
                // 初始化完成的通知不一定在主執行緒；載入廣告一律回到主執行緒做。
                main.post {
                    preloadRewarded()
                    preloadInterstitial()
                }
            }
        }
    }

    private fun preloadRewarded() {
        if (!started.get() || rewarded != null || loadingRewarded) return
        loadingRewarded = true
        guard { RewardedAd.load(app, rewardedUnitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                loadingRewarded = false
                rewarded = ad
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                loadingRewarded = false
            }
        }) }
    }

    private fun preloadInterstitial() {
        if (!started.get() || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        guard { InterstitialAd.load(app, interstitialUnitId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                loadingInterstitial = false
                interstitial = ad
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                loadingInterstitial = false
            }
        }) }
    }

    /** 等廣告載好，最多 [WAIT_MS]。 */
    private suspend fun <T> await(current: () -> T?, preload: () -> Unit): T? {
        current()?.let { return it }
        preload()
        return withTimeoutOrNull(WAIT_MS) {
            while (current() == null) kotlinx.coroutines.delay(200)
            current()
        }
    }

    override suspend fun showRewarded(): AdResult = withContext(Dispatchers.Main.immediate) {
        val activity = activityRef.get() ?: return@withContext AdResult.NotAvailable
        if (!started.get()) return@withContext AdResult.NotAvailable
        val ad = await({ rewarded }, ::preloadRewarded) ?: return@withContext AdResult.NotAvailable
        rewarded = null
        val result = suspendCancellableCoroutine { cont ->
            var earned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    if (cont.isActive) cont.resume(if (earned) AdResult.Rewarded else AdResult.Dismissed)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    if (cont.isActive) cont.resume(AdResult.NotAvailable)
                }
            }
            try {
                ad.show(activity) { earned = true }
            } catch (e: Exception) {
                tw.dynity.wrongbutton.core.crash.CrashReport.recordNonFatal(e)
                if (cont.isActive) cont.resume(AdResult.NotAvailable)
            }
        }
        preloadRewarded()
        result
    }

    override suspend fun showInterstitial(): AdResult = withContext(Dispatchers.Main.immediate) {
        val activity = activityRef.get() ?: return@withContext AdResult.NotAvailable
        if (!started.get()) return@withContext AdResult.NotAvailable
        val ad = await({ interstitial }, ::preloadInterstitial) ?: return@withContext AdResult.NotAvailable
        interstitial = null
        val result = suspendCancellableCoroutine { cont ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    if (cont.isActive) cont.resume(AdResult.Rewarded)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    if (cont.isActive) cont.resume(AdResult.NotAvailable)
                }
            }
            try {
                ad.show(activity)
            } catch (e: Exception) {
                tw.dynity.wrongbutton.core.crash.CrashReport.recordNonFatal(e)
                if (cont.isActive) cont.resume(AdResult.NotAvailable)
            }
        }
        preloadInterstitial()
        result
    }

    private companion object {
        const val WAIT_MS = 4_000L
    }
}
