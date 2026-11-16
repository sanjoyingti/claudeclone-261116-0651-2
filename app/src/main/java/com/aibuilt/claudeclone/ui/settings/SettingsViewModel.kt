package com.aibuilt.claudeclone.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.claudeclone.data.ArtifactRepository
import com.aibuilt.claudeclone.data.ChatRepository
import com.aibuilt.claudeclone.data.ProjectRepository
import com.aibuilt.claudeclone.data.SettingsRepository
import com.aibuilt.claudeclone.model.AppSettings
import com.aibuilt.claudeclone.model.ThemeMode
import com.aibuilt.claudeclone.network.ApiResult
import com.aibuilt.claudeclone.network.ClaudeApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ConnectionTest {
    data object Idle : ConnectionTest
    data object Running : ConnectionTest
    data class Success(val message: String) : ConnectionTest
    data class Failure(val message: String) : ConnectionTest
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val api: ClaudeApi,
    private val chatRepository: ChatRepository,
    private val artifactRepository: ArtifactRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _test = MutableStateFlow<ConnectionTest>(ConnectionTest.Idle)
    val test: StateFlow<ConnectionTest> = _test.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch { _apiKey.value = settingsRepository.apiKey() }
    }

    fun setBaseUrl(value: String) = viewModelScope.launch { settingsRepository.setBaseUrl(value.trim()) }

    fun setModel(value: String) = viewModelScope.launch { settingsRepository.setModel(value.trim()) }

    fun setSystemPrompt(value: String) = viewModelScope.launch { settingsRepository.setSystemPrompt(value) }

    fun setMaxTokens(value: Int) = viewModelScope.launch { settingsRepository.setMaxTokens(value) }

    fun setTemperature(value: Float) = viewModelScope.launch { settingsRepository.setTemperature(value) }

    fun setThemeMode(value: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(value) }

    fun setApiKey(value: String) {
        _apiKey.value = value
        viewModelScope.launch { settingsRepository.setApiKey(value.trim()) }
    }

    fun saveConnection(baseUrl: String, model: String, systemPrompt: String, maxTokens: Int, temperature: Float) {
        viewModelScope.launch {
            if (!baseUrl.trim().startsWith("http")) {
                _message.value = "The base URL must start with http:// or https://"
                return@launch
            }
            settingsRepository.setBaseUrl(baseUrl.trim())
            settingsRepository.setModel(model.trim())
            settingsRepository.setSystemPrompt(systemPrompt)
            settingsRepository.setMaxTokens(maxTokens)
            settingsRepository.setTemperature(temperature)
            _message.value = "Settings saved."
        }
    }

    fun testConnection() {
        if (_test.value is ConnectionTest.Running) return
        viewModelScope.launch {
            _test.value = ConnectionTest.Running
            val current = settings.value
            val key = settingsRepository.apiKey()
            if (key.isBlank()) {
                _test.value = ConnectionTest.Failure("Add an API key first.")
                return@launch
            }
            when (val result = api.testConnection(current.baseUrl, key, current.model)) {
                is ApiResult.Success -> _test.value = ConnectionTest.Success("Connection works.")
                is ApiResult.Failure -> _test.value = ConnectionTest.Failure(result.message)
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            chatRepository.clearAll()
            artifactRepository.clearAll()
            projectRepository.clearAll()
            _message.value = "All chats, artifacts and projects deleted."
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
