package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class AuthPreferences(private val context: Context) {
    private val keyIsLoggedIn = booleanPreferencesKey("is_logged_in")
    private val keyUsername = stringPreferencesKey("saved_username")
    private val keyPassword = stringPreferencesKey("saved_password")

    val isLoggedIn: Flow<Boolean> = context.authDataStore.data.map { prefs ->
        prefs[keyIsLoggedIn] ?: false
    }

    val currentUsername: Flow<String> = context.authDataStore.data.map { prefs ->
        prefs[keyUsername] ?: DEFAULT_USERNAME
    }

    suspend fun authenticate(username: String, password: String): Boolean {
        val prefs = context.authDataStore.data.first()
        val storedUser = prefs[keyUsername] ?: DEFAULT_USERNAME
        val storedPass = prefs[keyPassword] ?: DEFAULT_PASSWORD
        val valid = username.trim().equals(storedUser, ignoreCase = true) && password == storedPass
        if (valid) {
            context.authDataStore.edit { it[keyIsLoggedIn] = true }
        }
        return valid
    }

    suspend fun logout() {
        context.authDataStore.edit { it[keyIsLoggedIn] = false }
    }

    companion object {
        const val DEFAULT_USERNAME = "admin"
        const val DEFAULT_PASSWORD = "admin123"
    }
}
