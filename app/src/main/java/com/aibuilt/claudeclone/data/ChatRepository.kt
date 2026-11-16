package com.aibuilt.claudeclone.data

import com.aibuilt.claudeclone.model.ChatMessage
import com.aibuilt.claudeclone.model.Conversation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

class ChatRepository(private val directory: File) {

    private val file = File(directory, "conversations.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        scope.launch { load() }
    }

    private suspend fun load() = mutex.withLock {
        if (_loaded.value) return@withLock
        runCatching {
            if (file.exists()) {
                _conversations.value = json.decodeFromString<List<Conversation>>(file.readText())
            }
        }
        _loaded.value = true
    }

    private fun write(list: List<Conversation>) {
        runCatching {
            if (!directory.exists()) directory.mkdirs()
            file.writeText(json.encodeToString(list))
        }
    }

    private suspend fun update(block: (List<Conversation>) -> List<Conversation>) = mutex.withLock {
        val updated = block(_conversations.value)
        _conversations.value = updated
        write(updated)
    }

    suspend fun create(title: String, projectId: String?): Conversation {
        val conversation = Conversation(title = title, projectId = projectId)
        update { list -> list + conversation }
        return conversation
    }

    suspend fun appendMessage(conversationId: String, message: ChatMessage) {
        update { list ->
            list.map { conversation ->
                if (conversation.id == conversationId) {
                    conversation.copy(
                        messages = conversation.messages + message,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    conversation
                }
            }
        }
    }

    suspend fun rename(conversationId: String, title: String) {
        update { list ->
            list.map { if (it.id == conversationId) it.copy(title = title) else it }
        }
    }

    suspend fun setProject(conversationId: String, projectId: String?) {
        update { list ->
            list.map { if (it.id == conversationId) it.copy(projectId = projectId) else it }
        }
    }

    suspend fun delete(conversationId: String) {
        update { list -> list.filterNot { it.id == conversationId } }
    }

    suspend fun clearAll() {
        update { emptyList() }
    }

    fun find(conversationId: String): Conversation? =
        _conversations.value.firstOrNull { it.id == conversationId }
}
