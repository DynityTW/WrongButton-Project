import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * 上傳金鑰（upload key）。金鑰檔與密碼絕對不能進 repo：
 * 從專案根目錄的 keystore.properties（已列入 .gitignore）或環境變數讀取，兩者都沒有時正式版不簽名。
 * 步驟見 docs/RELEASE.md。
 */
val signingProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(key: String, env: String): String? = signingProps.getProperty(key) ?: System.getenv(env)
val uploadStore = signingValue("storeFile", "WB_UPLOAD_STORE_FILE")

/**
 * AdMob 的 ID。這些不是密碼，本來就會包在 App 裡。
 * TEST_* 是 Google 公開的測試 ID：只會顯示測試廣告、不會有收入，開發時一定要用它們（點自己的正式廣告會被停權）。
 * 拿到正式 ID 後，只要換掉下面三個 RELEASE_* 值；debug 版永遠用測試 ID。
 */
/**
 * 版本號：主版本.次版本.修正版（例如 0.8.0）。
 * versionCode 由版本號算出：主 × 10000 + 次 × 100 + 修正（0.8.0 → 800）。
 * 「主版本或次版本」變了，玩家會被要求強制更新（見 core/update/ForcedUpdate.kt）；只改修正版不會。
 * 發新版時只改這三個數字。
 */
val VERSION_MAJOR = 0
val VERSION_MINOR = 8
val VERSION_PATCH = 3

val ADMOB_TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
val ADMOB_TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
val ADMOB_TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
val ADMOB_RELEASE_APP_ID = ADMOB_TEST_APP_ID
val ADMOB_RELEASE_REWARDED_ID = ADMOB_TEST_REWARDED_ID
val ADMOB_RELEASE_INTERSTITIAL_ID = ADMOB_TEST_INTERSTITIAL_ID

// 正式版還在用測試廣告 ID 時，打包會印出明顯的警告（內部測試可以，正式上架前一定要換）。
gradle.taskGraph.whenReady {
    val releasing = allTasks.any { it.name == "bundleRelease" || it.name == "assembleRelease" }
    val testIds = listOf(ADMOB_RELEASE_APP_ID, ADMOB_RELEASE_REWARDED_ID, ADMOB_RELEASE_INTERSTITIAL_ID).any { "3940256099942544" in it }
    if (releasing && testIds) {
        logger.warn(
            "\n⚠️  正式版還在用 AdMob 的「測試」廣告 ID：不會有廣告收入。\n" +
                "    拿到正式 ID 後，換掉 app/build.gradle.kts 最上面的 ADMOB_RELEASE_*。\n",
        )
    }
}

android {
    namespace = "tw.dynity.wrongbutton"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.wrongbutton.app"
        minSdk = 26
        targetSdk = 37
        versionCode = VERSION_MAJOR * 10000 + VERSION_MINOR * 100 + VERSION_PATCH
        versionName = "$VERSION_MAJOR.$VERSION_MINOR.$VERSION_PATCH"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["admobAppId"] = ADMOB_TEST_APP_ID
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"$ADMOB_TEST_REWARDED_ID\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$ADMOB_TEST_INTERSTITIAL_ID\"")
        // false：模擬的付費與廣告（不連網、不扣款），自動測試和 debug 版用。true：Google Play 結帳與 AdMob。
        // debug 版想測真的結帳與廣告時：./gradlew assembleDebug -Pwb.realMonetization=true
        buildConfigField("boolean", "REAL_MONETIZATION", (findProperty("wb.realMonetization") ?: "false").toString())
    }

    signingConfigs {
        if (uploadStore != null) {
            create("upload") {
                storeFile = rootProject.file(uploadStore)
                storePassword = signingValue("storePassword", "WB_UPLOAD_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "WB_UPLOAD_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "WB_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (uploadStore != null) signingConfig = signingConfigs.getByName("upload")
            manifestPlaceholders["admobAppId"] = ADMOB_RELEASE_APP_ID
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"$ADMOB_RELEASE_REWARDED_ID\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$ADMOB_RELEASE_INTERSTITIAL_ID\"")
            buildConfigField("boolean", "REAL_MONETIZATION", "true")
            // 找閃退原因用：-Pwb.noMinify=true 打包「不經 R8」的正式版，和一般正式版比較。
            isMinifyEnabled = findProperty("wb.noMinify") != "true"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.maxHeapSize = "2g"
                // Robolectric 自己下載 android-all 執行環境；走與 Gradle 相同的 Maven Central 鏡像。
                it.systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.datastore)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.billing.ktx)
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.play.app.update.ktx)
    // Google 的套件會帶進很舊的 androidx.fragment（1.1）；ActivityResult API（更新畫面）需要 1.3 以上。
    implementation(libs.androidx.fragment)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.compose.ui.test.junit4)
}

// ---- 權限守門：合併後的 manifest 只能出現白名單裡的權限，其他一律讓建置失敗。 ----
abstract class VerifyPermissionsTask : DefaultTask() {
    @get:InputFile
    abstract val manifest: RegularFileProperty

    @get:Input
    abstract val allowed: SetProperty<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val text = manifest.get().asFile.readText()
        val tags = Regex("""<(uses-permission(-sdk-23)?|permission|permission-group|permission-tree)\b[^>]*>""").findAll(text).map { it.value }.toList()
        val names = tags.map { Regex("""android:name="([^"]+)"""").find(it)?.groupValues?.get(1) ?: it }
        val extra = names.filterNot { it in allowed.get() }
        check(extra.isEmpty()) { "合併後的 manifest 出現了白名單以外的權限：\n" + extra.joinToString("\n") }
        report.get().asFile.writeText("OK：只有白名單裡的權限。\n" + names.distinct().joinToString("\n"))
    }
}

/**
 * 權限白名單。
 * - DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION：androidx.core 自己定義、只給本 App 用的簽章權限。
 *   Android 8–12 上 ContextCompat.registerReceiver(…, RECEIVER_NOT_EXPORTED) 需要它，拿掉會當掉；它不是向使用者請求的權限。
 * - com.android.vending.BILLING：Google Play 結帳。
 * - INTERNET、ACCESS_NETWORK_STATE：只有載入廣告與結帳時會連網。
 * - com.google.android.gms.permission.AD_ID：AdMob 的廣告 ID（Play Console 的資料安全性表單要照實申報）。
 * - WAKE_LOCK、FOREGROUND_SERVICE：AdMob 內部用的 WorkManager 需要；拿掉可能會閃退。都不會跳出權限對話框。
 * Privacy Sandbox 的 ACCESS_ADSERVICES_* 已在 AndroidManifest.xml 移除。
 */
fun allowedPermissions(applicationId: String) = setOf(
    "$applicationId.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
    "com.android.vending.BILLING",
    "android.permission.INTERNET",
    "android.permission.ACCESS_NETWORK_STATE",
    "com.google.android.gms.permission.AD_ID",
    "android.permission.WAKE_LOCK",
    "android.permission.FOREGROUND_SERVICE",
)

androidComponents {
    onVariants { variant ->
        val name = variant.name.replaceFirstChar { it.uppercase() }
        val verify = tasks.register<VerifyPermissionsTask>("verify${name}Permissions") {
            manifest.set(variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.MERGED_MANIFEST))
            allowed.set(variant.applicationId.map { allowedPermissions(it) })
            report.set(layout.buildDirectory.file("reports/permissions/${variant.name}.txt"))
        }
        tasks.matching { it.name == "assemble$name" || it.name == "bundle$name" }.configureEach { dependsOn(verify) }
    }
}
