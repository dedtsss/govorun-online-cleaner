package com.govorun.lite.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class GigaChatClient(private val context: Context) {

    sealed class Error(message: String) : Exception(message) {
        object MissingAuthorizationKey : Error("Не задан Authorization Key")
        object EmptyResponse : Error("Пустой ответ от AI")
        class Api(message: String) : Error(message)
        class Network(cause: IOException) : Error(cause.message ?: "Ошибка сети")
    }

    fun cleanupText(input: String): String {
        val authKey = AiCleanerPrefs.getAuthorizationKey(context)
        if (authKey.isBlank()) throw Error.MissingAuthorizationKey

        val token = requestAccessToken(authKey)
        val prompt = PromptBuilder.build(AiCleanerPrefs.getMode(context))
        val model = AiCleanerPrefs.getModel(context)

        val payload = JSONObject()
            .put("model", model)
            .put("temperature", 0.2)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", prompt))
                .put(JSONObject().put("role", "user").put("content", input)))

        val responseJson = postJson(
            url = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions",
            headers = mapOf("Authorization" to "Bearer $token"),
            body = payload.toString()
        )

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
        return token
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
                throw Error.Api("API ошибка ($code): ${raw.take(200)}")
            }
            return JSONObject(raw)
        } catch (e: IOException) {
            throw Error.Network(e)
        } finally {
            connection.disconnect()
        }
    }
}
