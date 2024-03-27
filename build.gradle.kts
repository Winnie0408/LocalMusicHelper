// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.3.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("com.google.devtools.ksp") version "1.9.23-1.0.19" apply false
    id("com.cookpad.android.plugin.license-tools") version "1.2.8" // 需要把生成的yml文件放到项目根目录，才能生成license.html
}