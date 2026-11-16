package com.aibuilt.claudeclone.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.aibuilt.claudeclone.model.AppSettings
import com.aibuilt.claudeclone.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    private val securePrefs: SharedPreferences by lazy { createSecurePreferences() }

    /**
     * Keystore-backed preferences for the API key. Falls back to plain preferences
     * when the platform keystore is unavailable so the app still works.
     * Uses the MasterKeys alias API from security-crypto 1.0.0.
     */
    private fun createSecurePreferences(): SharedPreferences {
        val appContext = context.applicationContext
        return runCatching {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                SECURE_FILE,
                masterKeyAlias,
                appContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as SharedPreferences
        }.getOrElse {
            appContext.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE)
        }
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            baseUrl = preferences[Keys.BASE_URL] ?: AppSettings.DEFAULT_BASE_URL,
            model = preferences[Keys.MODEL] ?: AppSettings.DEFAULT_MODEL,
            systemPrompt = preferences[Keys.SYSTEM_PROMPT].orEmpty(),
            maxTokens = preferences[Keys.MAX_TOKENS] ?: 1024,
            temperature = preferences[Keys.TEMPERATURE] ?: 0.7f,
            themeMode = runCatching {
                ThemeMode.valueOf(preferences[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM)
        )
    }

    suspend fun setBaseUrl(value: String) = put { it[Keys.BASE_URL] = value }

    suspend fun setModel(value: String) = put { it[Keys.MODEL] = value }

    suspend fun setSystemPrompt(value: String) = put { it[Keys.SYSTEM_PROMPT] = value }

    suspend fun setMaxTokens(value: Int) = put { it[Keys.MAX_TOKENS] = value }

    suspend fun setTemperature(value: Float) = put { it[Keys.TEMPERATURE] = value }

    suspend fun setThemeMode(value: ThemeMode) = put { it[Keys.THEME_MODE] = value.name }

    private suspend fun put(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit { preferences -> block(preferences) }
    }

    suspend fun apiKey(): String = withContext(Dispatchers.IO) {
        runCatching { securePrefs.getString(KEY_API_KEY, "") }.getOrNull().orEmpty()
    }

    suspend fun setApiKey(value: String) = withContext(Dispatchers.IO) {
        runCatching { securePrefs.edit().putString(KEY_API_KEY, value).apply() }
        Unit
    }

    private companion object {
        const val KEY_API_KEY = "api_key"
        const val SECURE_FILE = "secure_prefs"
        const val FALLBACK_FILE = "secure_prefs_fallback"
    }
}
