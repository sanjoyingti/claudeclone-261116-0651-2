package com.aibuilt.claudeclone.data

import com.aibuilt.claudeclone.model.Artifact
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

class ArtifactRepository(private val directory: File) {

    private val file = File(directory, "artifacts.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _artifacts = MutableStateFlow<List<Artifact>>(emptyList())
    val artifacts: StateFlow<List<Artifact>> = _artifacts.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        scope.launch { load() }
    }

    private suspend fun load() = mutex.withLock {
        if (_loaded.value) return@withLock
        runCatching {
            if (file.exists()) {
                _artifacts.value = json.decodeFromString<List<Artifact>>(file.readText())
            }
        }
        _loaded.value = true
    }

    private fun write(list: List<Artifact>) {
        runCatching {
            if (!directory.exists()) directory.mkdirs()
            file.writeText(json.encodeToString(list))
        }
    }

    private suspend fun update(block: (List<Artifact>) -> List<Artifact>) = mutex.withLock {
        val updated = block(_artifacts.value)
        _artifacts.value = updated
        write(updated)
    }

    suspend fun add(artifact: Artifact) {
        update { list -> list + artifact }
    }

    suspend fun delete(artifactId: String) {
        update { list -> list.filterNot { it.id == artifactId } }
    }

    suspend fun deleteForConversation(conversationId: String) {
        update { list -> list.filterNot { it.conversationId == conversationId } }
    }

    suspend fun clearAll() {
        update { emptyList() }
    }

    fun find(artifactId: String): Artifact? = _artifacts.value.firstOrNull { it.id == artifactId }
}
