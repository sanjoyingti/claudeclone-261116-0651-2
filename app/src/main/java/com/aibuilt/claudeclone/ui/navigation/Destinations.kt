package com.aibuilt.claudeclone.ui.navigation

object Destinations {
    const val SPLASH = "splash"
    const val HOME = "home"

    const val CHAT = "chat/{conversationId}"
    fun chat(conversationId: String) = "chat/$conversationId"

    const val ARTIFACTS = "artifacts"
    const val ARTIFACT = "artifact/{artifactId}"
    fun artifact(artifactId: String) = "artifact/$artifactId"

    const val PROJECTS = "projects"
    const val PROJECT = "project/{projectId}"
    fun project(projectId: String) = "project/$projectId"

    const val SETTINGS = "settings"

    const val ARG_CONVERSATION_ID = "conversationId"
    const val ARG_ARTIFACT_ID = "artifactId"
    const val ARG_PROJECT_ID = "projectId"

    val topLevel = setOf(HOME, ARTIFACTS, PROJECTS, SETTINGS)
}
