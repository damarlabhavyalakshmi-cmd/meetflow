package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "meetflow_prefs")

data class SessionState(
    val currentUserId: String? = null,
    val activeWorkspaceId: String = "ws_core_product",
    val darkModeOverride: Boolean? = null,
    val demoSeeded: Boolean = false
)

class SessionPreferences(private val context: Context) {
    private object Keys {
        val USER_ID = stringPreferencesKey("current_user_id")
        val WORKSPACE_ID = stringPreferencesKey("active_workspace_id")
        val DARK_MODE = stringPreferencesKey("dark_mode_pref") // "system", "light", "dark"
        val DEMO_SEEDED = booleanPreferencesKey("demo_seeded")
    }

    val sessionFlow: Flow<SessionState> = context.dataStore.data.map { prefs ->
        val darkPref = when (prefs[Keys.DARK_MODE]) {
            "dark" -> true
            "light" -> false
            else -> null
        }
        SessionState(
            currentUserId = prefs[Keys.USER_ID],
            activeWorkspaceId = prefs[Keys.WORKSPACE_ID] ?: "ws_core_product",
            darkModeOverride = darkPref,
            demoSeeded = prefs[Keys.DEMO_SEEDED] ?: false
        )
    }

    suspend fun setCurrentUser(userId: String?) {
        context.dataStore.edit { prefs ->
            if (userId == null) prefs.remove(Keys.USER_ID)
            else prefs[Keys.USER_ID] = userId
        }
    }

    suspend fun setActiveWorkspace(workspaceId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WORKSPACE_ID] = workspaceId
        }
    }

    suspend fun setDarkMode(override: Boolean?) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = when (override) {
                true -> "dark"
                false -> "light"
                null -> "system"
            }
        }
    }

    suspend fun markDemoSeeded() {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEMO_SEEDED] = true
        }
    }
}
