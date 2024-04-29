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
    val PLAYLIST_SOURCE_PLATFORM = intPreferencesKey("playlist_source_platform")
    val GET_PLAYLIST_METHOD = intPreferencesKey("get_playlist_method")
    val KUGOU_TOKEN = stringPreferencesKey("kugou_token")
    val KUGOU_USER_ID = stringPreferencesKey("kugou_user_id")
    val UM_FILE_LEGAL = booleanPreferencesKey("um_file_legal")
    val UM_SUPPORT_OVERWRITE = booleanPreferencesKey("um_support_overwrite")
    val HAPTIC_STRENGTH = intPreferencesKey("haptic_strength")
    val TAG_OVERWRITE = booleanPreferencesKey("tag_overwrite")
    val TAG_LYRICIST = booleanPreferencesKey("tag_lyricist")
    val TAG_COMPOSER = booleanPreferencesKey("tag_composer")
    val TAG_ARRANGER = booleanPreferencesKey("tag_arranger")
    val SORT_METHOD = intPreferencesKey("sort_method")
    val SLOW_MODE = booleanPreferencesKey("slow_mode")
}