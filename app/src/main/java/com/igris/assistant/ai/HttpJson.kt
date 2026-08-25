package com.igris.assistant.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/** Minimal dependency-free JSON HTTP client used by cloud/local providers. */
internal object HttpJson {
    suspend fun post(url: String, headers: Map<String, String>, body: JSONObject, timeoutMs: Int = 20_000): JSONObject =
        withContext(Dispatchers.IO) {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            conn.outputStream.use { os -> OutputStreamWriter(os, Charsets.UTF_8).use { it.write(body.toString()) } }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) throw IllegalStateException("HTTP $code: ${text.take(300)}")
            JSONObject(text)
        }

    suspend fun get(url: String, headers: Map<String, String> = emptyMap(), timeoutMs: Int = 8_000): JSONObject =
        withContext(Dispatchers.IO) {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) throw IllegalStateException("HTTP $code: ${text.take(300)}")
            JSONObject(text)
        }
}
