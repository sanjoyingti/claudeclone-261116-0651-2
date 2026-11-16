package com.aibuilt.claudeclone.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.claudeclone.data.ChatRepository
import com.aibuilt.claudeclone.data.ProjectRepository
import com.aibuilt.claudeclone.model.Conversation
import com.aibuilt.claudeclone.model.Project
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val chatRepository: ChatRepository,
    projectRepository: ProjectRepository
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = chatRepository.conversations
        .map { list -> list.sortedByDescending { it.updatedAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val projects: StateFlow<List<Project>> = projectRepository.projects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val loaded: StateFlow<Boolean> = chatRepository.loaded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun create(projectId: String?, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val conversation = chatRepository.create(title = "New chat", projectId = projectId)
            onCreated(conversation.id)
        }
    }

    fun rename(conversationId: String, title: String) {
        val cleaned = title.trim()
        if (cleaned.isEmpty()) return
        viewModelScope.launch { chatRepository.rename(conversationId, cleaned) }
    }

    fun delete(conversationId: String) {
        viewModelScope.launch { chatRepository.delete(conversationId) }
    }
}
