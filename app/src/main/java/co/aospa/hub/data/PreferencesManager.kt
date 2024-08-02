package co.aospa.hub.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesManager(private val context: Context) {
    private val Context.dataStore by preferencesDataStore(name = "settings")

    companion object {
        private val LAST_SUCCESSFUL_CHECK_KEY = longPreferencesKey("last_successful_check")
    }

    val lastSuccessfulCheck: Flow<Long?>
        get() = context.dataStore.data.map { preferences ->
            preferences[LAST_SUCCESSFUL_CHECK_KEY]
        }

    suspend fun saveLastSuccessfulCheck(date: Date) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SUCCESSFUL_CHECK_KEY] = date.time
        }
    }
}