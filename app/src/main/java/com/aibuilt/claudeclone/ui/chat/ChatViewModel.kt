package com.aibuilt.claudeclone.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.claudeclone.data.ArtifactExtractor
import com.aibuilt.claudeclone.data.ArtifactRepository
import com.aibuilt.claudeclone.data.ChatRepository
import com.aibuilt.claudeclone.data.ProjectRepository
import com.aibuilt.claudeclone.data.SettingsRepository
import com.aibuilt.claudeclone.model.Artifact
import com.aibuilt.claudeclone.model.ChatMessage
import com.aibuilt.claudeclone.model.Conversation
import com.aibuilt.claudeclone.model.Project
import com.aibuilt.claudeclone.network.ApiMessage
import com.aibuilt.claudeclone.network.ApiResult
import com.aibuilt.claudeclone.network.ClaudeApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val artifactRepository: ArtifactRepository,
    private val projectRepository: ProjectRepository,
    private val settingsRepository: SettingsRepository,
    private val api: ClaudeApi,
    private val conversationId: String
) : ViewModel() {

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val conversation: StateFlow<Conversation?> = chatRepository.conversations
        .map { list -> list.firstOrNull { it.id == conversationId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val project: StateFlow<Project?> = combine(
        chatRepository.conversations,
        projectRepository.projects
    ) { conversations, projects ->
        val projectId = conversations.firstOrNull { it.id == conversationId }?.projectId
        projects.firstOrNull { it.id == projectId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val projects: StateFlow<List<Project>> = projectRepository.projects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artifacts: StateFlow<List<Artifact>> = artifactRepository.artifacts
        .map { list -> list.filter { it.conversationId == conversationId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onInputChange(value: String) {
        _input.value = value
    }

    fun dismissError() {
        _error.value = null
    }

    fun useSuggestion(text: String) {
        _input.value = text
    }

    fun assignProject(projectId: String?) {
        viewModelScope.launch { chatRepository.setProject(conversationId, projectId) }
    }

    fun send() {
        val text = _input.value.trim()
        if (text.isEmpty() || _sending.value) return
        _input.value = ""
        _error.value = null

        viewModelScope.launch {
            _sending.value = true
            try {
                chatRepository.appendMessage(
                    conversationId,
                    ChatMessage(role = ChatMessage.ROLE_USER, content = text)
                )
                val current = chatRepository.find(conversationId)
                if (current != null && current.title == "New chat") {
                    chatRepository.rename(
                        conversationId,
                        text.lineSequence().first().take(48).ifBlank { "New chat" }
                    )
                }

                val apiKey = settingsRepository.apiKey()
                if (apiKey.isBlank()) {
                    _error.value = "No API key yet. Add one in Settings."
                    return@launch
                }

                val settings = settingsRepository.settings.first()
                val history = chatRepository.find(conversationId)?.messages.orEmpty()
                    .takeLast(40)
                    .map { ApiMessage(it.role, it.content) }
                val instructions = projectRepository.find(current?.projectId.orEmpty())
                    ?.instructions.orEmpty()
                val system = listOf(instructions, settings.systemPrompt)
                    .filter { it.isNotBlank() }
                    .joinToString("\n\n")

                when (
                    val result = api.sendMessage(
                        baseUrl = settings.baseUrl,
                        apiKey = apiKey,
                        model = settings.model,
                        maxTokens = settings.maxTokens,
                        temperature = settings.temperature,
                        systemPrompt = system,
                        messages = history
                    )
                ) {
                    is ApiResult.Success -> {
                        val extracted = ArtifactExtractor.extract(result.text, conversationId)
                        extracted.forEach { artifactRepository.add(it) }
                        chatRepository.appendMessage(
                            conversationId,
                            ChatMessage(
                                role = ChatMessage.ROLE_ASSISTANT,
                                content = result.text,
                                artifactIds = extracted.map { it.id }
                            )
                        )
                    }

                    is ApiResult.Failure -> _error.value = result.message
                }
            } finally {
                _sending.value = false
            }
        }
    }
}
