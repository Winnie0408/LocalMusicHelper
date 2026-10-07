import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.compose") version "1.12.1"
    id("org.jetbrains.kotlin.plugin.compose")
}

// 签名配置来源：环境变量 > 根目录 local.properties（均不纳入版本管理）
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists())
        file.inputStream().use { load(it) }
}

fun signingProperty(name: String): String? =
    System.getenv(name) ?: localProperties.getProperty(name)

val releaseStoreFile = signingProperty("RELEASE_STORE_FILE")?.let { file(it) }
val releaseStorePassword = signingProperty("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = signingProperty("RELEASE_KEY_ALIAS")
val releaseKeyPassword = signingProperty("RELEASE_KEY_PASSWORD")
val hasReleaseSigning = releaseStoreFile?.exists() == true &&
        !releaseStorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()

configure<ApplicationExtension> {
    namespace = "com.hwinzniej.musichelper"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hwinzniej.musichelper"
        minSdk = 26
        targetSdk = 37
        versionCode = 85
        versionName = "1.7.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    packaging {
        resources.excludes.add("META-INF/*")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning)
                signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))

    // Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // UI Tests
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    //SaltUI
    implementation("io.github.moriafly:salt-ui:3.0.0-beta02")

    //JAudioTagger
//    implementation("org.bitbucket.ijabz:jaudiotagger:7b004a1")
    implementation("com.github.maxbruecken:jaudiotagger-android:master")

    //Kotlin协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.fragment:fragment-ktx:1.9.1")

    //Room数据库
    implementation("androidx.room:room-runtime:2.8.5")
    annotationProcessor("androidx.room:room-compiler:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    //FastJson
    implementation("com.alibaba.fastjson2:fastjson2-kotlin:2.0.65")

    //Navigation
    implementation("androidx.navigation:navigation-compose:2.10.2")

    //DataStore
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    //OkHttp3
    implementation("com.squareup.okhttp3:okhttp:5.5.0")

    //Markdown渲染器
    implementation("com.github.jeziellago:compose-markdown:0.5.8")

    //Google ZXing二维码生成
    implementation("com.google.zxing:core:3.5.4")

    //Splashscreen
    implementation("androidx.core:core-splashscreen:1.2.0")

    //Apache Common Csv
    implementation("org.apache.commons:commons-csv:1.14.1")

    //拖拽排序-可注释
//    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

    //Jsoup-可注释
//    implementation("org.jsoup:jsoup:1.17.2")

    //Ksoup-不可注释，可用于跨平台
    implementation("com.fleeksoft.ksoup:ksoup:0.2.6")

    //HtmlUnit-可注释
//    implementation("org.htmlunit:htmlunit3-android:3.7.0")

    //XmlAPI-可注释
//    implementation("xml-apis:xml-apis:2.0.2")


    // Optional - Included automatically by material, only add when you need
    // the icons but not the material library (e.g. when using Material3 or a
    // custom design system based on Foundation)
    //implementation("androidx.compose.material:material-icons-core")
    // Optional - Add full set of material icons
    //implementation("androidx.compose.material:material-icons-extended")
    // Optional - Add window size utils
    //implementation("androidx.compose.material3:material3-window-size-class")

    // Optional - Integration with activities
    //implementation("androidx.activity:activity-compose:1.6.1")
    // Optional - Integration with ViewModels
    //implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.5.1")
    // Optional - Integration with LiveData
    //implementation("androidx.compose.runtime:runtime-livedata")
    // Optional - Integration with RxJava
    //implementation("androidx.compose.runtime:runtime-rxjava2")
}