package com.aibuilt.claudeclone

import android.content.Context
import com.aibuilt.claudeclone.data.ArtifactRepository
import com.aibuilt.claudeclone.data.ChatRepository
import com.aibuilt.claudeclone.data.ProjectRepository
import com.aibuilt.claudeclone.data.SettingsRepository
import com.aibuilt.claudeclone.network.ClaudeApi
import java.io.File

class AppContainer(context: Context) {

    private val storageDirectory: File = File(context.filesDir, "claudeclone").apply { mkdirs() }

    val settingsRepository: SettingsRepository = SettingsRepository(context)
    val chatRepository: ChatRepository = ChatRepository(storageDirectory)
    val artifactRepository: ArtifactRepository = ArtifactRepository(storageDirectory)
    val projectRepository: ProjectRepository = ProjectRepository(storageDirectory)
    val api: ClaudeApi = ClaudeApi()
}
