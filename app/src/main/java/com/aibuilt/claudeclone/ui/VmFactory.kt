package com.aibuilt.claudeclone.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aibuilt.claudeclone.AppContainer
import com.aibuilt.claudeclone.ui.artifacts.ArtifactsViewModel
import com.aibuilt.claudeclone.ui.chat.ChatViewModel
import com.aibuilt.claudeclone.ui.home.HomeViewModel
import com.aibuilt.claudeclone.ui.projects.ProjectDetailViewModel
import com.aibuilt.claudeclone.ui.projects.ProjectsViewModel
import com.aibuilt.claudeclone.ui.settings.SettingsViewModel

object VmFactory {

    fun app(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer { AppViewModel(container.settingsRepository) }
    }

    fun home(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            HomeViewModel(container.chatRepository, container.projectRepository)
        }
    }

    fun chat(container: AppContainer, conversationId: String): ViewModelProvider.Factory =
        viewModelFactory {
            initializer {
                ChatViewModel(
                    chatRepository = container.chatRepository,
                    artifactRepository = container.artifactRepository,
                    projectRepository = container.projectRepository,
                    settingsRepository = container.settingsRepository,
                    api = container.api,
                    conversationId = conversationId
                )
            }
        }

    fun artifacts(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            ArtifactsViewModel(container.artifactRepository)
        }
    }

    fun projects(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            ProjectsViewModel(container.projectRepository, container.chatRepository)
        }
    }

    fun projectDetail(container: AppContainer, projectId: String): ViewModelProvider.Factory =
        viewModelFactory {
            initializer {
                ProjectDetailViewModel(container.projectRepository, projectId)
            }
        }

    fun settings(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            SettingsViewModel(
                settingsRepository = container.settingsRepository,
                api = container.api,
                chatRepository = container.chatRepository,
                artifactRepository = container.artifactRepository,
                projectRepository = container.projectRepository
            )
        }
    }
}
