package com.aibuilt.claudeclone.ui.artifacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.claudeclone.data.ArtifactRepository
import com.aibuilt.claudeclone.model.Artifact
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArtifactsViewModel(private val repository: ArtifactRepository) : ViewModel() {

    val artifacts: StateFlow<List<Artifact>> = repository.artifacts
        .map { list -> list.sortedByDescending { it.createdAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val loaded: StateFlow<Boolean> = repository.loaded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun delete(artifactId: String) {
        viewModelScope.launch { repository.delete(artifactId) }
    }
}
