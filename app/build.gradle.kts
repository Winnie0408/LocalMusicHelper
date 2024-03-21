plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.hwinzniej.musichelper"
    compileSdk = 34
//    useLibrary("org.apache.http.legacy")

    defaultConfig {
        applicationId = "com.hwinzniej.musichelper"
        minSdk = 26
        targetSdk = 34
        versionCode = 33
        versionName = "1.1.7.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    packaging {
        resources.excludes.add("META-INF/*")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.6"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.annotation:annotation:1.7.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.02"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // UI Tests
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.3")

    //SaltUI
    implementation("com.github.Moriafly:SaltUI:0.1.0-dev53")

    //JAudioTagger
    implementation("org.bitbucket.ijabz:jaudiotagger:7b004a1")

    //Kotlin协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    //Room数据库
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    //FastJson
    implementation("com.alibaba.fastjson2:fastjson2-kotlin:2.0.47")

    //Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    //DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    //OkHttp3
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    //Markdown渲染器
    implementation("com.github.jeziellago:compose-markdown:0.4.1")

    //Google ZXing二维码生成
    implementation("com.google.zxing:core:3.5.3")

    //拖拽排序-可注释
//    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

    //Jsoup-可注释
//    implementation("org.jsoup:jsoup:1.17.2")

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