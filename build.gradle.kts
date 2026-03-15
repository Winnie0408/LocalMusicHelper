// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "9.0.1" apply false
    id("com.android.library") version "9.0.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10" apply false
    id("com.google.devtools.ksp") version "2.3.2" apply false
    id("com.cookpad.android.plugin.license-tools") version "1.2.8" // 需要把生成的yml文件放到项目根目录，才能生成license.html
}