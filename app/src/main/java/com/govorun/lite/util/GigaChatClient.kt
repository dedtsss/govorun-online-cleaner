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
        private const val MAX_INPUT_CHARS = 6_000
        private const val DEFAULT_TOKEN_TTL_MS = 25 * 60 * 1000L
        private const val TOKEN_RENEW_SKEW_MS = 60 * 1000L

        @Volatile private var cachedToken: String? = null
        @Volatile private var cachedTokenExpiryMs: Long = 0L
        @Volatile private var cachedTokenAuthKey: String? = null
    }

    sealed class Error(message: String) : Exception(message) {
        object MissingAuthorizationKey : Error("Не задан Authorization Key")
        object InputTooLong : Error("Текст слишком длинный для AI-очистки (максимум 6000 символов)")
        object EmptyResponse : Error("Пустой ответ от AI")
        object RateLimited : Error("Превышен лимит запросов GigaChat (HTTP 429)")
        class Api(message: String) : Error(message)
        class Network(cause: IOException) : Error(cause.message ?: "Ошибка сети")
    }

    fun cleanupText(input: String): String {
        if (input.length > MAX_INPUT_CHARS) throw Error.InputTooLong
        val authKey = AiCleanerPrefs.getAuthorizationKey(context)
        if (authKey.isBlank()) throw Error.MissingAuthorizationKey

        val prompt = PromptBuilder.build(AiCleanerPrefs.getMode(context))
        val model = AiCleanerPrefs.getModel(context)
        val responseJson = requestCleanupWithRetry(authKey, prompt, model, input)

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

    private fun requestCleanupWithRetry(
        authKey: String,
        prompt: String,
        model: String,
        input: String
    ): JSONObject {
        val firstToken = getAccessToken(authKey)
        return try {
            postCompletion(firstToken, prompt, model, input)
        } catch (e: UnauthorizedException) {
            clearCachedToken()
            val retryToken = getAccessToken(authKey)
            postCompletion(retryToken, prompt, model, input)
        }
    }

    private fun postCompletion(token: String, prompt: String, model: String, input: String): JSONObject {
        val payload = JSONObject()
            .put("model", model)
            .put("temperature", 0.2)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", prompt))
                .put(JSONObject().put("role", "user").put("content", input)))

        return postJson(
            url = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions",
            headers = mapOf("Authorization" to "Bearer $token"),
            body = payload.toString()
        )
    }

    private fun getAccessToken(authKey: String): String {
        val now = System.currentTimeMillis()
        val token = cachedToken
        if (
            !token.isNullOrBlank() &&
            cachedTokenAuthKey == authKey &&
            now + TOKEN_RENEW_SKEW_MS < cachedTokenExpiryMs
        ) {
            return token
        }
        if (cachedTokenAuthKey != authKey) {
            clearCachedToken()
        }
        return requestAccessToken(authKey)
    }

    private fun requestAccessToken(authKey: String): String {
        val responseJson = try {
            postForm(
                url = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth",
                headers = mapOf(
                    "Authorization" to "Basic $authKey",
                    "RqUID" to UUID.randomUUID().toString()
                ),
                formBody = "scope=GIGACHAT_API_PERS"
            )
        } catch (_: UnauthorizedException) {
            clearCachedToken()
            throw Error.Api("Сохраненный Authorization Key GigaChat недействителен или отозван")
        }

        val token = responseJson.optString("access_token").trim()
        if (token.isBlank()) throw Error.Api("OAuth не вернул access_token")
        val expiresAtMs = parseTokenExpiryMs(responseJson)
        cachedToken = token
        cachedTokenExpiryMs = expiresAtMs
        cachedTokenAuthKey = authKey
        return token
    }

    private fun parseTokenExpiryMs(responseJson: JSONObject): Long {
        val now = System.currentTimeMillis()
        val expiresAtRaw = responseJson.optLong("expires_at", 0L)
        if (expiresAtRaw > 0L) {
            return if (expiresAtRaw > 1_000_000_000_000L) expiresAtRaw else expiresAtRaw * 1000L
        }
        val expiresInSec = responseJson.optLong("expires_in", 0L)
        if (expiresInSec > 0L) return now + (expiresInSec * 1000L)
        return now + DEFAULT_TOKEN_TTL_MS
    }

    private fun clearCachedToken() {
        cachedToken = null
        cachedTokenExpiryMs = 0L
        cachedTokenAuthKey = null
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
                if (code == 401) throw UnauthorizedException()
                if (code == 429) throw Error.RateLimited
                throw Error.Api("API ошибка ($code): ${raw.take(200)}")
            }
            return JSONObject(raw)
        } catch (e: IOException) {
            throw Error.Network(e)
        } finally {
            connection.disconnect()
        }
    }

    private class UnauthorizedException : Exception()
}
