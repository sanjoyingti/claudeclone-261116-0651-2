package com.aibuilt.claudeclone.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ApiMessage(val role: String, val content: String)

sealed interface ApiResult {
    data class Success(val text: String) : ApiResult
    data class Failure(val message: String) : ApiResult
}

class ClaudeApi {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(
        baseUrl: String,
        apiKey: String,
        model: String,
        maxTokens: Int,
        temperature: Float,
        systemPrompt: String,
        messages: List<ApiMessage>
    ): ApiResult = withContext(Dispatchers.IO) {
        val url = buildUrl(baseUrl)
        val payload = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("max_tokens", JsonPrimitive(maxTokens))
            put("temperature", JsonPrimitive(temperature))
            if (systemPrompt.isNotBlank()) {
                put("system", JsonPrimitive(systemPrompt))
            }
            put("messages", buildJsonArray {
                messages.forEach { message ->
                    add(buildJsonObject {
                        put("role", JsonPrimitive(message.role))
                        put("content", JsonPrimitive(message.content))
                    })
                }
            })
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", API_VERSION)
            .addHeader("content-type", "application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext ApiResult.Failure(parseError(response.code, body))
                }
                val text = parseText(body)
                if (text.isBlank()) {
                    ApiResult.Failure("The server returned an empty reply.")
                } else {
                    ApiResult.Success(text)
                }
            }
        }.getOrElse { error ->
            val reason = when (error) {
                is IOException -> "Network error: ${error.message ?: "check your connection and server address"}"
                else -> error.message ?: "Unknown error"
            }
            ApiResult.Failure(reason)
        }
    }

    suspend fun testConnection(baseUrl: String, apiKey: String, model: String): ApiResult {
        return sendMessage(
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = model,
            maxTokens = 16,
            temperature = 0f,
            systemPrompt = "",
            messages = listOf(ApiMessage("user", "Say OK"))
        )
    }

    private fun buildUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        return if (trimmed.endsWith("/v1/messages")) {
            trimmed
        } else if (trimmed.endsWith("/v1")) {
            "$trimmed/messages"
        } else {
            "$trimmed/v1/messages"
        }
    }

    private fun parseText(body: String): String {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return ""
        val content = root["content"] as? JsonArray ?: return ""
        return content.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            if (item["type"]?.jsonPrimitive?.content == "text") {
                item["text"]?.jsonPrimitive?.content
            } else {
                null
            }
        }.joinToString("\n").trim()
    }

    private fun parseError(code: Int, body: String): String {
        val parsed = runCatching {
            val root = json.parseToJsonElement(body).jsonObject
            root["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
        }.getOrNull()
        val friendly = when (code) {
            401 -> "Invalid API key."
            403 -> "Access forbidden for this key."
            404 -> "Endpoint not found. Check the base URL."
            429 -> "Rate limit reached. Try again shortly."
            in 500..599 -> "The server had a problem. Try again."
            else -> "Request failed ($code)."
        }
        return if (parsed.isNullOrBlank()) friendly else "$friendly $parsed"
    }

    private companion object {
        const val API_VERSION = "2023-06-01"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
