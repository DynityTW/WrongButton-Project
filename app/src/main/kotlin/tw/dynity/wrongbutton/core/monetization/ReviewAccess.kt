package tw.dynity.wrongbutton.core.monetization

import java.security.MessageDigest

/**
 * 給 Google Play 審查員的解鎖碼：審查員不會自己付款，要能不付錢就看到 Pro 的全部內容。
 *
 * - 入口藏在「設定」最下面：連點版本號碼 7 下，輸入解鎖碼。
 * - App 裡只存解鎖碼加鹽後的 SHA-256，**不存明碼**（這個 repo 是公開的，明碼只寫在 Play Console 的「應用程式存取權」）。
 * - 只解鎖這一台手機（存在本機的付費狀態裡），不影響 Google Play 的購買紀錄。
 * - 碼外流的話：換一組新碼，把新的雜湊填進 [EXPECTED_HASH]，發新版，並更新 Play Console。
 */
object ReviewAccess {
    private const val SALT = "wrongbutton-review:"
    private const val EXPECTED_HASH = "dcef5e02629509e8c38e272df7590d5c15847a4abbc762ac4488499ca8a92153"

    /** 測試用：換成假碼的雜湊，避免測試裡出現真的解鎖碼。 */
    internal var expectedHash: String = EXPECTED_HASH

    const val TAPS_TO_OPEN = 7

    fun hash(code: String): String =
        MessageDigest.getInstance("SHA-256").digest((SALT + code).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    /** 前後空白不算（貼上時常常多一個空白）。 */
    fun matches(input: String): Boolean = hash(input.trim()) == expectedHash
}
