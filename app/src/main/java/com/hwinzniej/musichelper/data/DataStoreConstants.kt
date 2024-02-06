package com.hwinzniej.musichelper.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object DataStoreConstants {
    const val SETTINGS_PREFERENCES = "settings"
    val KEY_LANGUAGE = stringPreferencesKey("language")
    val KEY_THEME_MODE = intPreferencesKey("theme_mode")
    val KEY_ENABLE_DYNAMIC_COLOR = booleanPreferencesKey("enable_dynamic_color")
    val KEY_ENABLE_AUTO_CHECK_UPDATE = booleanPreferencesKey("enable_auto_check_update")
    val KEY_USE_ROOT_ACCESS = booleanPreferencesKey("use_root_access")
    val KEY_ENABLE_HAPTIC = booleanPreferencesKey("enable_haptic")
    val KEY_ENCRYPT_SERVER = stringPreferencesKey("encrypt_server")
    val LAST_LOGIN_TIMESTAMP = longPreferencesKey("last_login_timestamp")
    val NETEASE_USER_ID = stringPreferencesKey("netease_user_id")
}