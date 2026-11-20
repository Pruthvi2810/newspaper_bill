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

data class UserProfile(
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val role: String = "Proprietor"
)

class AuthPreferences(private val context: Context) {
    private val keyIsLoggedIn = booleanPreferencesKey("is_logged_in")
    private val keyUsername = stringPreferencesKey("saved_username")
    private val keyPassword = stringPreferencesKey("saved_password")
    private val keyFullName = stringPreferencesKey("profile_full_name")
    private val keyPhone = stringPreferencesKey("profile_phone")
    private val keyEmail = stringPreferencesKey("profile_email")
    private val keyRole = stringPreferencesKey("profile_role")

    val isLoggedIn: Flow<Boolean> = context.authDataStore.data.map { prefs ->
        prefs[keyIsLoggedIn] ?: false
    }

    val userProfile: Flow<UserProfile> = context.authDataStore.data.map { prefs ->
        UserProfile(
            isLoggedIn = prefs[keyIsLoggedIn] ?: false,
            username = prefs[keyUsername] ?: "",
            fullName = prefs[keyFullName] ?: "",
            phone = prefs[keyPhone] ?: "",
            email = prefs[keyEmail] ?: "",
            role = prefs[keyRole] ?: "Proprietor"
        )
    }

    suspend fun authenticate(username: String, password: String): Boolean {
        val prefs = context.authDataStore.data.first()
        val storedUser = prefs[keyUsername]?.takeIf { it.isNotBlank() } ?: DEFAULT_USERNAME
        val storedPass = prefs[keyPassword]?.takeIf { it.isNotBlank() } ?: DEFAULT_PASSWORD
        val valid = username.trim().equals(storedUser, ignoreCase = true) && password == storedPass
        if (valid) {
            context.authDataStore.edit {
                it[keyIsLoggedIn] = true
                if (it[keyFullName].isNullOrBlank()) {
                    it[keyFullName] = storedUser.replaceFirstChar { c -> c.uppercase() }
                }
            }
        }
        return valid
    }

    suspend fun createAccount(
        fullName: String,
        username: String,
        password: String,
        phone: String,
        email: String
    ) {
        context.authDataStore.edit { prefs ->
            prefs[keyIsLoggedIn] = true
            prefs[keyFullName] = fullName.trim()
            prefs[keyUsername] = username.trim()
            prefs[keyPassword] = password
            prefs[keyPhone] = phone.trim()
            prefs[keyEmail] = email.trim()
        }
    }

    suspend fun updateProfile(
        fullName: String,
        phone: String,
        email: String,
        role: String
    ) {
        context.authDataStore.edit { prefs ->
            prefs[keyFullName] = fullName.trim()
            prefs[keyPhone] = phone.trim()
            prefs[keyEmail] = email.trim()
            prefs[keyRole] = role.trim().ifBlank { "Proprietor" }
        }
    }

    suspend fun logout() {
        context.authDataStore.edit { it[keyIsLoggedIn] = false }
    }

    companion object {
        const val DEFAULT_USERNAME = "admin"
        const val DEFAULT_PASSWORD = "admin123"
    }
}
