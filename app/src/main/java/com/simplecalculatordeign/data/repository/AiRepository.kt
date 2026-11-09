package com.simplecalculatordeign.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

data class ChatTurn(val fromUser: Boolean, val text: String)

/**
 * Client for YOUR secure AI backend (see /backend in the project). The APK only
 * knows the backend URL; the AI provider's secret key stays on the server.
 *
 * Request:  POST {baseUrl}/v1/math-assistant  {"question": "...", "history": [{"role":"user|assistant","content":"..."}]}
 * Response: {"answer": "..."}
 */
class AiRepository(private val baseUrl: String) {

    val isConfigured: Boolean get() = baseUrl.startsWith("https://") || baseUrl.startsWith("http://")

    suspend fun ask(question: String, history: List<ChatTurn>): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("AI backend is not configured"))
        var connection: HttpURLConnection? = null
        try {
            val body = JSONObject().apply {
                put("question", question.take(MAX_QUESTION))
                put("history", JSONArray().apply {
                    history.takeLast(10).forEach { turn ->
                        put(JSONObject().apply {
                            put("role", if (turn.fromUser) "user" else "assistant")
                            put("content", turn.text.take(MAX_QUESTION))
                        })
                    }
                })
            }.toString()

            connection = (URL("$baseUrl/v1/math-assistant").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(text).optString("error") }.getOrNull()
                return@withContext Result.failure(IOException(message?.takeIf { it.isNotBlank() } ?: "Server error ($code)"))
            }
            val answer = JSONObject(text).optString("answer")
            if (answer.isBlank()) Result.failure(IOException("Empty answer from server"))
            else Result.success(answer)
        } catch (e: SocketTimeoutException) {
            Result.failure(IOException("The AI server took too long to respond"))
        } catch (e: IOException) {
            Result.failure(IOException("Could not reach the AI server. Check your internet connection."))
        } catch (e: Exception) {
            Result.failure(IOException("Unexpected response from the AI server"))
        } finally {
            connection?.disconnect()
        }
    }

    private companion object {
        const val MAX_QUESTION = 2_000
    }
}
