package com.aibuilt.claudeclone.data

import com.aibuilt.claudeclone.model.Artifact
import java.util.UUID

object ArtifactExtractor {

    private val codeFence = Regex("```([A-Za-z0-9_+#\\-. ]*)\\n([\\s\\S]*?)```")

    fun extract(text: String, conversationId: String): List<Artifact> {
        val now = System.currentTimeMillis()
        return codeFence.findAll(text).mapIndexedNotNull { index, match ->
            val content = match.groupValues[2].trimEnd('\n')
            if (content.isBlank()) return@mapIndexedNotNull null
            val language = match.groupValues[1].trim().ifBlank { "text" }
            Artifact(
                id = UUID.randomUUID().toString(),
                title = titleFor(language, content, index),
                language = language,
                content = content,
                conversationId = conversationId,
                createdAt = now + index
            )
        }.toList()
    }

    private fun titleFor(language: String, content: String, index: Int): String {
        val firstLine = content.lineSequence().firstOrNull()?.trim().orEmpty()
        val looksLikeName = firstLine.length in 3..60 && !firstLine.endsWith(";") &&
            (firstLine.startsWith("//") || firstLine.startsWith("#") || firstLine.contains("class ") ||
                firstLine.contains("fun ") || firstLine.contains("function "))
        return when {
            looksLikeName -> firstLine.removePrefix("//").removePrefix("#").trim()
            language != "text" -> "${language.replaceFirstChar { it.uppercase() }} snippet ${index + 1}"
            else -> "Snippet ${index + 1}"
        }
    }
}
