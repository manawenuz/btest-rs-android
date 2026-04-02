package com.btestrs.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class UploadResult(
    val success: Int = 0,
    val failed: Int = 0,
    val message: String = ""
)

object ResultPoster {

    /** POST single run to /api/results */
    suspend fun postSingle(baseUrl: String, apiKey: String, jsonBody: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("${baseUrl.trimEnd('/')}/api/results")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.doOutput = true
                conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                val body = if (code in 200..299) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                }

                if (code in 200..299) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("HTTP $code: $body"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** POST batch runs to /api/results/batch (gzip-compressed) */
    suspend fun postBatch(baseUrl: String, apiKey: String, gzipPayload: ByteArray): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("${baseUrl.trimEnd('/')}/api/results/batch")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Content-Encoding", "gzip")
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
                conn.connectTimeout = 30000
                conn.readTimeout = 30000
                conn.doOutput = true
                conn.outputStream.use { it.write(gzipPayload) }

                val code = conn.responseCode
                val body = if (code in 200..299) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                }

                if (code in 200..299) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("HTTP $code: $body"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
