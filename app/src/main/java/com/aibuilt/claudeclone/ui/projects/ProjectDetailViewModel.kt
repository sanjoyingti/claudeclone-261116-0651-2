package com.aibuilt.claudeclone.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.claudeclone.data.ProjectRepository
import com.aibuilt.claudeclone.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProjectDetailViewModel(
    private val repository: ProjectRepository,
    private val projectId: String
) : ViewModel() {

    val isNew: Boolean = projectId == NEW_PROJECT_ID

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _instructions = MutableStateFlow("")
    val instructions: StateFlow<String> = _instructions.asStateFlow()

    private val _createdId = MutableStateFlow<String?>(null)
    val createdId: StateFlow<String?> = _createdId.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        if (!isNew) {
            viewModelScope.launch {
                val project = repository.projects
                    .first { list -> list.any { it.id == projectId } }
                    .firstOrNull { it.id == projectId }
                if (project != null) {
                    _name.value = project.name
                    _description.value = project.description
                    _instructions.value = project.instructions
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _name.value = value
        _error.value = null
    }

    fun onDescriptionChange(value: String) {
        _description.value = value
    }

    fun onInstructionsChange(value: String) {
        _instructions.value = value
    }

    fun dismissError() {
        _error.value = null
    }

    fun save() {
        val cleanedName = _name.value.trim()
        if (cleanedName.isEmpty()) {
            _error.value = "Give the project a name first."
            return
        }
        viewModelScope.launch {
            val targetId = _createdId.value ?: projectId.takeIf { !isNew }
            val existing = targetId?.let { repository.find(it) }
            if (existing != null) {
                repository.save(
                    existing.copy(
                        name = cleanedName,
                        description = _description.value.trim(),
                        instructions = _instructions.value.trim()
                    )
                )
            } else {
                val project = Project(
                    name = cleanedName,
                    description = _description.value.trim(),
                    instructions = _instructions.value.trim()
                )
                repository.save(project)
                _createdId.value = project.id
            }
            _saved.value = true
        }
    }

    fun project(): Project? = repository.find(_createdId.value ?: projectId)

    companion object {
        const val NEW_PROJECT_ID = "new"
    }
}
