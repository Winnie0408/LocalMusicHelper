package com.hwinzniej.musichelper.utils

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.hwinzniej.musichelper.data.DataStoreConstants

class MyDataStore : Application() {
    val dataStore: DataStore<Preferences> by preferencesDataStore(name = DataStoreConstants.SETTINGS_PREFERENCES)
}