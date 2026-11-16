package com.aibuilt.claudeclone.data

import com.aibuilt.claudeclone.model.Project
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

class ProjectRepository(private val directory: File) {

    private val file = File(directory, "projects.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        scope.launch { load() }
    }

    private suspend fun load() = mutex.withLock {
        if (_loaded.value) return@withLock
        runCatching {
            if (file.exists()) {
                _projects.value = json.decodeFromString<List<Project>>(file.readText())
            }
        }
        _loaded.value = true
    }

    private fun write(list: List<Project>) {
        runCatching {
            if (!directory.exists()) directory.mkdirs()
            file.writeText(json.encodeToString(list))
        }
    }

    private suspend fun update(block: (List<Project>) -> List<Project>) = mutex.withLock {
        val updated = block(_projects.value)
        _projects.value = updated
        write(updated)
    }

    suspend fun save(project: Project) {
        update { list ->
            if (list.any { it.id == project.id }) {
                list.map { if (it.id == project.id) project else it }
            } else {
                list + project
            }
        }
    }

    suspend fun delete(projectId: String) {
        update { list -> list.filterNot { it.id == projectId } }
    }

    suspend fun clearAll() {
        update { emptyList() }
    }

    fun find(projectId: String): Project? = _projects.value.firstOrNull { it.id == projectId }
}
