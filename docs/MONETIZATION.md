# 付費與廣告

| 版本 | 付費 | 廣告 |
|---|---|---|
| debug（開發、自動測試） | Google Play 結帳（`PlayBillingStore`）；側載安裝買不了，開發時用解鎖碼開 Pro | 模擬畫面（`MockAdService`） |
| release（上傳 Play 的 AAB） | Google Play 結帳（`PlayBillingStore`） | AdMob（`AdMobService`），先經過 Google 的同意表單 |

App 沒有模擬付款。廣告的切換由 `app/build.gradle.kts` 的 `REAL_MONETIZATION` 決定（debug＝false、release＝true）。
自動測試用只存在測試裡的 `FakeStore`（`ChapterTestBase.kt`）。

## 價格與優惠

- 價格完全由 Play Console 決定，App 不寫死價格：每次打開 Pro 頁都向 Google Play 重新查詢。
- 要做優惠：在 Play Console 的商品 → 購買選項 `buy` →「新增優惠」，設定折扣與期間。
  App 會挑這個帳號現在買得到、最便宜的方案結帳，並在價格旁顯示「限時優惠 −N%」。
- 查不到價格時（離線、還沒生效）顯示「價格載入中…」，不會顯示錯的價格。

## 規則

- 前 75 關免費，第 76 關以後（目前到第 360 關）都需要 Pro（`Levels.FREE_LEVELS`）。之後新增的章節，已經買 Pro 的人直接可以玩。
- 強制更新：Google Play 上有任何比較新的版本，玩家打開遊戲就會被要求更新（`VersionRules`）。
- 提示：第 1 階免費；第 2、3 階第一次看要先看一則獎勵廣告，看過就記住（`AdRules.FIRST_AD_HINT_TIER`）。
- 全版廣告：免費版每過 5 關，在離開結算畫面時播（`AdRules.CLEARS_PER_AD_BREAK`）；關卡進行中不插播。
- Pro：沒有任何廣告。
- 沒網路、沒有廣告可播、玩家不同意廣告時：**一律放行**（提示直接給、全版廣告跳過）。

## 一、Google Play 結帳的設定

1. 打包 release AAB（見 `docs/RELEASE.md`），上傳到 Play Console 的「內部測試」。
   Play Console 要先看到含有結帳功能的版本，才會開放建立商品。
2. Play Console → 營利 → 產品 → 應用程式內產品 → 建立產品：
   - 產品 ID：**`wrongbutton_pro`**（必須和 `ProProduct.PRODUCT_ID` 一樣，建立後不能改）
   - 一次性購買，價格 NT$150；美國 US$4.99（其他國家可以讓 Play 自動換算）
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

- 隱私權政策、服務條款（在 Luarvex 官網：https://luarvex.com/products/wrongbutton/terms/ 、https://luarvex.com/products/wrongbutton/privacy/ ，App 直接開這兩個網址）：寫明會連網載入廣告與結帳、AdMob 會使用廣告 ID。Play Console 的隱私權政策網址也填這個。
- Play Console 的「資料安全性」表單：申報廣告 ID、裝置資訊、App 互動（由 AdMob 收集）。
- Play Console 的「廣告」聲明：選「含有廣告」。

## 五、給 Google Play 審查員的解鎖碼

審查員不會自己付款，要能不付錢看到第 76 關以後的關卡。

- 入口：設定 → 連點最下面的「版本 x.y.z」7 下 → 輸入解鎖碼。
- 解鎖碼的**明碼只寫在 Play Console →「應用程式存取權」**，不寫進這個公開 repo；
  程式裡只有加鹽後的 SHA-256（`ReviewAccess.EXPECTED_HASH`）。
- 只解鎖那一台手機（`MonetizationState.reviewAccess`），Google Play 查不到購買紀錄時也不會被收回。
- 換碼：用下面的方式算出新碼的雜湊，填進 `EXPECTED_HASH`，發新版，並更新 Play Console。
  ```
  python3 -c "import hashlib,getpass;print(hashlib.sha256(('wrongbutton-review:'+getpass.getpass()).encode()).hexdigest())"
  ```

## 六、促銷代碼（送 Pro 給團隊成員、合作對象、抽獎）

1. Play Console → 營利 →「促銷代碼」→ 建立促銷活動。
2. 類型選「一次性產品」，商品選 `wrongbutton_pro`，填數量與期限，產生代碼後下載 CSV。
3. 收到代碼的人：Play 商店 → 頭像 →「付款和訂閱」→「兌換代碼」，輸入代碼。
   也可以直接打開 `https://play.google.com/redeem?code=代碼`。
4. 回到遊戲就會開通 Pro（App 每次回到前景都會向 Google Play 查購買紀錄），換手機或重裝也會自動恢復。

每一季可以產生的代碼數量有上限，Play Console 會顯示剩餘數量。代碼兌換後就和正式購買一樣，不需要另外確認。
