package com.aibuilt.claudeclone.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.claudeclone.data.ChatRepository
import com.aibuilt.claudeclone.data.ProjectRepository
import com.aibuilt.claudeclone.model.Project
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(
    private val projectRepository: ProjectRepository,
    chatRepository: ChatRepository
) : ViewModel() {

    val projects: StateFlow<List<Project>> = projectRepository.projects
        .map { list -> list.sortedByDescending { it.createdAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val loaded: StateFlow<Boolean> = projectRepository.loaded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val chatCounts: StateFlow<Map<String, Int>> = chatRepository.conversations
        .map { list -> list.mapNotNull { it.projectId }.groupingBy { it }.eachCount() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun create(name: String, description: String, onCreated: (String) -> Unit) {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return
        viewModelScope.launch {
            val project = Project(name = cleaned, description = description.trim())
            projectRepository.save(project)
            onCreated(project.id)
        }
    }

    fun delete(projectId: String) {
        viewModelScope.launch { projectRepository.delete(projectId) }
    }
}
