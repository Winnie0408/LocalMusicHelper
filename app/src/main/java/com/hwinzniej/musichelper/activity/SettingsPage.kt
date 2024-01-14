package com.hwinzniej.musichelper.activity

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LifecycleOwner

class SettingsPage(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
) {
    var enableDynamicColor = mutableStateOf(false)
    var selectedThemeMode = mutableIntStateOf(0)
    var selectedLanguage = mutableIntStateOf(0)
    var enableAutoCheckUpdate = mutableStateOf(false)

}