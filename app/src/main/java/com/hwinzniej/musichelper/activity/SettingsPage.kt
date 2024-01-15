package com.hwinzniej.musichelper.activity

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LifecycleOwner

class SettingsPage(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
) {
    var selectedLanguage = mutableStateOf("")
    var enableAutoCheckUpdate = mutableStateOf(true)

}