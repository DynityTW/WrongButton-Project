# 打包 AAB 並上傳 Google Play

Google Play 只收 AAB（Android App Bundle），而且一定要用「上傳金鑰」簽名。
金鑰只放在你們自己的電腦上，**不要放進 repo，也不要傳給任何人（包括 AI）**。

需要的環境：JDK 17 以上，以及 Android SDK（裝 Android Studio 會一起裝好）。

---

## 一、第一次：產生上傳金鑰（只做一次）

在你的電腦上，到一個 repo 以外的資料夾執行（Android Studio 內建的 JDK 也有 `keytool`）：

```
keytool -genkeypair -v -keystore wrongbutton-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

它會問兩次密碼、你的名字和組織（照實填，玩家看不到）。完成後會得到 `wrongbutton-upload.jks`。

**一定要備份**：`.jks` 檔案和密碼，存在兩個不同的地方（例如密碼管理器 + 雲端硬碟的加密資料夾）。
弄丟的話，Play App Signing 可以申請重設上傳金鑰，但要聯絡 Google、等審核，期間不能發新版。

也可以用 Android Studio：Build → Generate Signed App Bundle or APK → Android App Bundle → Create new…

## 二、告訴專案金鑰在哪（每台電腦設一次）

在專案根目錄（和 `settings.gradle.kts` 同一層）建立 `keystore.properties`：

```
storeFile=/你的路徑/wrongbutton-upload.jks
storePassword=你的金鑰庫密碼
keyAlias=upload
keyPassword=你的金鑰密碼
```

Windows 的路徑用正斜線，例如 `storeFile=C:/keys/wrongbutton-upload.jks`。

`keystore.properties`、`*.jks`、`*.keystore` 都已經列在 `.gitignore`，不會被提交。
也可以改用環境變數：`WB_UPLOAD_STORE_FILE`、`WB_UPLOAD_STORE_PASSWORD`、`WB_UPLOAD_KEY_ALIAS`、`WB_UPLOAD_KEY_PASSWORD`。

兩者都沒設定時，正式版不會簽名，Play 會拒收。

## 三、每次發版

1. **改版本號**：打開 `app/build.gradle.kts`，改最上面的 `VERSION_MAJOR`、`VERSION_MINOR`、`VERSION_PATCH`。
   `versionName`（例如 0.8.1）和 `versionCode`（主 × 10000 ＋ 次 × 100 ＋ 修正，例如 801）會自動算出來，一定比上一版大。
   - Google Play 上有任何比較新的版本，舊版玩家打開遊戲時都會被要求**強制更新**
     （Google Play 的應用程式內更新；只對從 Play 安裝的版本有效）。
2. **跑測試**：
   ```
   ./gradlew testDebugUnitTest
   ```
3. **打包**：
   ```
   ./gradlew bundleRelease
   ```
   Windows 用 `gradlew.bat bundleRelease`。
4. 成品在：
   ```
   app/build/outputs/bundle/release/app-release.aab
   ```
   R8 的對照表（mapping）已經包在 AAB 裡，Play Console 的當機報告會自動還原成看得懂的程式位置。

## 四、上傳到 Play Console

1. Play Console → 你的 App → **測試與發布** → **內部測試** → **建立新版本**。
2. 第一次上傳時，選擇讓 **Google 管理並保護你的應用程式簽署金鑰（Play App Signing）**。
   之後 Google 用「應用程式簽署金鑰」簽給玩家的版本，你們只負責保管「上傳金鑰」。
3. 上傳 `app-release.aab`，填版本說明，儲存 → 審查 → 推出。
4. 在「測試人員」分頁加入測試者的 Gmail，把加入連結傳給他們。

之後依序推進：內部測試 → 封閉測試（新的個人開發者帳號依目前規定，要至少 12 位測試者連續參與 14 天）→ 正式版。

## 五、正式版第一次要特別測的地方

正式版有開 R8（縮減與混淆程式碼），偶爾會讓只在正式版才出現的問題冒出來。從 Play 安裝測試版後，至少走一次：

- 打開 App、玩第 1–3 關、按提示、看模擬廣告
- 關掉 App 再打開：進度、設定都還在
- 設定 → 服務條款、隱私權政策
- Pro 頁：模擬購買、恢復購買
- 旋轉螢幕、切到背景再回來

閃退的話，到 Play Console → Android Vitals → 當機與 ANR 看報告。

## 常見錯誤

| 訊息 | 原因 |
|---|---|
| 版本代碼 N 已經使用過 | `versionCode` 沒有加 1 |
| 你上傳的 APK 或 Android App Bundle 未經簽署 | 沒有 `keystore.properties`，或路徑寫錯 |
| 簽署金鑰錯誤 | 用了別支金鑰；Play 只認第一次上傳時的那支上傳金鑰 |
| `Keystore was tampered with, or password was incorrect` | 密碼打錯 |
