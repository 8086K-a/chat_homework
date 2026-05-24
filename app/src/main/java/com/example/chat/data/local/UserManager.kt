package com.example.chat.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

data class UserInfo(
    val email: String = "",
    val name: String = "",
    val token: String = "",
)

class UserManager(private val context: Context) {

    companion object {
        private val KEY_EMAIL = stringPreferencesKey("email")
        private val KEY_NAME = stringPreferencesKey("name")
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    val user: Flow<UserInfo> = context.dataStore.data.map { prefs ->
        UserInfo(
            email = prefs[KEY_EMAIL] ?: "",
            name = prefs[KEY_NAME] ?: "",
            token = prefs[KEY_TOKEN] ?: "",
        )
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOGGED_IN] ?: false
    }

    suspend fun saveUser(email: String, name: String, token: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_EMAIL] = email
            prefs[KEY_NAME] = name
            prefs[KEY_TOKEN] = token
            prefs[KEY_LOGGED_IN] = true
        }
    }

    suspend fun updateUser(email: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_EMAIL] = email
            prefs[KEY_NAME] = name
            prefs[KEY_LOGGED_IN] = true
        }
    }

    suspend fun logout() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_EMAIL)
            prefs.remove(KEY_NAME)
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_LOGGED_IN)
        }
    }
}
