package com.govorun.lite.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class GigaChatClient(private val context: Context) {
    companion object {
        private const val MAX_INPUT_LENGTH = 6000
        @Volatile private var cachedToken: String? = null
        @Volatile private var cachedTokenExpiresAtMs: Long = 0L
    }

    sealed class Error(message: String) : Exception(message) {
        object MissingAuthorizationKey : Error("Не задан Authorization Key")
        object TooLongInput : Error("Слишком длинный текст")
        object EmptyResponse : Error("Пустой ответ от AI")
        object RateLimited : Error("Лимит запросов GigaChat, попробуйте позже")
        class Api(message: String) : Error(message)
        class Network(cause: IOException) : Error(cause.message ?: "Ошибка сети")
    }

    fun cleanupText(input: String): String {
        val normalized = input.trim()
        if (normalized.isBlank()) throw Error.EmptyResponse
        if (normalized.length > MAX_INPUT_LENGTH) throw Error.TooLongInput

        val authKey = AiCleanerPrefs.getAuthorizationKey(context)
        if (authKey.isBlank()) throw Error.MissingAuthorizationKey

        val prompt = PromptBuilder.build(AiCleanerPrefs.getMode(context))
        val model = AiCleanerPrefs.getModel(context)

        val payload = JSONObject()
            .put("model", model)
            .put("temperature", 0.2)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", prompt))
                .put(JSONObject().put("role", "user").put("content", normalized)))

        val responseJson = executeWithTokenRetry(authKey) { token ->
            postJson(
                url = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions",
                headers = mapOf("Authorization" to "Bearer $token"),
                body = payload.toString()
            )
        }

        val cleaned = responseJson
            .optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            ?.trim()
            .orEmpty()

        if (cleaned.isBlank()) throw Error.EmptyResponse
        return cleaned
    }

    private fun requestAccessToken(authKey: String): String {
        val now = System.currentTimeMillis()
        val current = cachedToken
        if (!current.isNullOrBlank() && now < cachedTokenExpiresAtMs) {
            return current
        }

        val responseJson = postForm(
            url = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth",
            headers = mapOf(
                "Authorization" to "Basic $authKey",
                "RqUID" to UUID.randomUUID().toString()
            ),
            formBody = "scope=GIGACHAT_API_PERS"
        )

        val token = responseJson.optString("access_token").trim()
        if (token.isBlank()) throw Error.Api("OAuth не вернул access_token")
        val expiresAt = extractTokenExpiryMs(responseJson, now)
        cachedToken = token
        cachedTokenExpiresAtMs = expiresAt
        return token
    }

    private fun extractTokenExpiryMs(responseJson: JSONObject, nowMs: Long): Long {
        val expiresAtRaw = responseJson.optLong("expires_at", 0L)
        if (expiresAtRaw > 0L) {
            return if (expiresAtRaw < 10_000_000_000L) expiresAtRaw * 1000L else expiresAtRaw
        }
        val expiresInSec = responseJson.optLong("expires_in", 1800L).coerceAtLeast(60L)
        return nowMs + (expiresInSec * 1000L) - 30_000L
    }

    private fun clearTokenCache() {
        cachedToken = null
        cachedTokenExpiresAtMs = 0L
    }

    private fun executeWithTokenRetry(authKey: String, block: (String) -> JSONObject): JSONObject {
        val firstToken = requestAccessToken(authKey)
        return try {
            block(firstToken)
        } catch (e: HttpStatusException) {
            if (e.code == 401) {
                clearTokenCache()
                val retryToken = requestAccessToken(authKey)
                block(retryToken)
            } else {
                throw e
            }
        }
    }

    private fun postForm(url: String, headers: Map<String, String>, formBody: String): JSONObject {
        return executePost(url, headers + ("Content-Type" to "application/x-www-form-urlencoded"), formBody)
    }

    private fun postJson(url: String, headers: Map<String, String>, body: String): JSONObject {
        return executePost(url, headers + ("Content-Type" to "application/json"), body)
    }

    private fun executePost(url: String, headers: Map<String, String>, body: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            setRequestProperty("Accept", "application/json")
        }

        try {
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            if (code !in 200..299) {
                if (code == 429) throw Error.RateLimited
                if (code == 401) throw HttpStatusException(code, raw)
                throw Error.Api("API ошибка ($code): ${raw.take(200)}")
            }
            return JSONObject(raw)
        } catch (e: IOException) {
            throw Error.Network(e)
        } finally {
            connection.disconnect()
        }
    }

    private class HttpStatusException(val code: Int, val rawBody: String) :
        Exception("HTTP $code: ${rawBody.take(120)}")
}
