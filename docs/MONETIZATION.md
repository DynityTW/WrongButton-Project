# 付費與廣告

| 版本 | 付費 | 廣告 |
|---|---|---|
| debug（開發、自動測試） | 模擬（`MockStore`），不連網、不扣款，有「取消 Pro」按鈕 | 模擬畫面（`MockAdService`） |
| release（上傳 Play 的 AAB） | Google Play 結帳（`PlayBillingStore`） | AdMob（`AdMobService`），先經過 Google 的同意表單 |

切換由 `app/build.gradle.kts` 的 `REAL_MONETIZATION` 決定（debug＝false、release＝true）。

## 規則

- 前 60 關免費，第 61–300 關需要 Pro（`Levels.FREE_LEVELS`）。
- 提示：第 1 階免費；第 2、3 階第一次看要先看一則獎勵廣告，看過就記住（`AdRules.FIRST_AD_HINT_TIER`）。
- 全版廣告：免費版每過 5 關，在離開結算畫面時播（`AdRules.CLEARS_PER_AD_BREAK`）；關卡進行中不插播。
- Pro：沒有任何廣告。
- 沒網路、沒有廣告可播、玩家不同意廣告時：**一律放行**（提示直接給、全版廣告跳過）。

## 一、Google Play 結帳的設定

1. 打包 release AAB（見 `docs/RELEASE.md`），上傳到 Play Console 的「內部測試」。
   Play Console 要先看到含有結帳功能的版本，才會開放建立商品。
2. Play Console → 營利 → 產品 → 應用程式內產品 → 建立產品：
   - 產品 ID：**`wrongbutton-pro`**（必須和 `ProProduct.PRODUCT_ID` 一樣，建立後不能改）
   - 一次性購買，價格 NT$120（其他國家可以讓 Play 自動換算）
   - 啟用
3. Play Console → 設定 → 授權測試：加入測試者的 Gmail。授權測試人員購買不會真的扣款。
4. 測試者從內部測試連結安裝（一定要從 Play 安裝，側載的 APK 不能結帳），在 Pro 頁購買。

App 的行為：
- 啟動時自動向 Play 查詢購買紀錄（＝自動恢復購買）。查到 Pro 就開通；查詢成功但沒有 Pro（例如退款）就回到免費版；離線時維持原狀。
- 付款完成的購買會自動確認（acknowledge），否則 Google 3 天後會自動退款。
- 待付款（例如超商代碼繳費）不會開通，付完後 Play 通知 App 自動開通。

## 二、AdMob 的設定

1. AdMob → 應用程式 → 新增應用程式（Android，尚未上架），取得**應用程式 ID**。
2. 在這個 App 底下建立兩個廣告單元：
   - **獎勵廣告**（提示用），獎勵：1 個「提示」
   - **插頁式廣告**（每 5 關一次的全版廣告）
3. AdMob → 隱私權與訊息：為這個 App 建立並發布「歐洲法規」的同意訊息。
4. 把三個 ID 填進 `app/build.gradle.kts` 最上面的 `ADMOB_RELEASE_*`：
   ```
   val ADMOB_RELEASE_APP_ID = "ca-app-pub-你的編號~應用程式"
   val ADMOB_RELEASE_REWARDED_ID = "ca-app-pub-你的編號/獎勵廣告單元"
   val ADMOB_RELEASE_INTERSTITIAL_ID = "ca-app-pub-你的編號/插頁式廣告單元"
   ```
   debug 版永遠用 Google 的測試 ID。
5. 上架後，在 AdMob 把這個 App 連結到 Google Play 上的商店資訊，並在官網放 `app-ads.txt`。

**不要點自己的正式廣告**，也不要請朋友點，AdMob 會停權。自己測試時用測試 ID，或在 AdMob 把手機加為測試裝置。

## 三、權限

合併後的 manifest 只能有 `allowedPermissions`（`app/build.gradle.kts`）裡的權限，建置時會檢查：
BILLING、INTERNET、ACCESS_NETWORK_STATE、AD_ID、WAKE_LOCK、FOREGROUND_SERVICE。
AdMob 帶進來的 Privacy Sandbox 權限（`ACCESS_ADSERVICES_*`）已在 `AndroidManifest.xml` 移除。
都不會跳出權限對話框。

## 四、上架前要一起改的

- 隱私權政策、服務條款（以及 App 內建版本，`tools/legal/sync_legal.py`）：寫明會連網載入廣告與結帳、AdMob 會使用廣告 ID。
- Play Console 的「資料安全性」表單：申報廣告 ID、裝置資訊、App 互動（由 AdMob 收集）。
- Play Console 的「廣告」聲明：選「含有廣告」。
