package com.hwinzniej.musichelper.activity

import android.content.Context
import androidx.compose.runtime.mutableStateOf

class SettingsPage(
    val context: Context,
) {
    var enableAutoCheckUpdate = mutableStateOf(true)
}