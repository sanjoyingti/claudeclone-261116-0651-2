package com.aibuilt.claudeclone.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String = ROLE_USER,
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val artifactIds: List<String> = emptyList()
) {
    val isUser: Boolean get() = role == ROLE_USER

    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
    }
}

@Serializable
data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New chat",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val projectId: String? = null,
    val messages: List<ChatMessage> = emptyList()
) {
    val preview: String
        get() = messages.lastOrNull { it.isUser }?.content?.replace("\n", " ")?.take(80).orEmpty()
}

@Serializable
data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val instructions: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class Artifact(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val language: String = "text",
    val content: String = "",
    val conversationId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class AppSettings(
    val baseUrl: String = DEFAULT_BASE_URL,
    val model: String = DEFAULT_MODEL,
    val systemPrompt: String = "",
    val maxTokens: Int = 1024,
    val temperature: Float = 0.7f,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://api.anthropic.com"
        const val DEFAULT_MODEL = "claude-sonnet-4-5"
    }
}
