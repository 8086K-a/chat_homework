package com.example.chat.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import java.io.StringReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OpenAiService {

    companion object {
        private const val TAG = "OpenAiService"
        const val DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode"
        const val DEFAULT_API_KEY = "sk-02b22a78e8b546008a6a99a66da2d365"
        const val DEFAULT_MODEL = "qwen3.5-flash"
        const val ALTERNATE_MODEL = "qwen3.5-flash"
    }

    private var baseUrl: String = DEFAULT_BASE_URL
    private var apiKey: String = DEFAULT_API_KEY

    private val gson = Gson()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun sendMessageStream(
        input: Any,
        model: String = DEFAULT_MODEL,
    ): Flow<String> = flow {
        val requestBody = ResponseRequest(
            model = model,
            input = input,
            stream = true,
            tools = listOf(
                ToolDef(type = "web_search"),
                ToolDef(type = "web_extractor"),
                ToolDef(type = "code_interpreter"),
            ),
        )

        val jsonBody = gson.toJson(requestBody)
        Log.d(TAG, "=== 请求体 ===")
        Log.d(TAG, jsonBody)

        val body = jsonBody.toRequestBody("application/json".toMediaType())

        val url = "$baseUrl/v1/responses"
        Log.d(TAG, "POST $url")

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        Log.d(TAG, "=== 响应状态: ${response.code} ===")
        Log.d(TAG, "Content-Type: ${response.header("Content-Type")}")

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: ""
            Log.e(TAG, "API 错误: code=${response.code}, body=$errorBody")
            throw Exception("API 请求失败: $errorBody")
        }

        val source = response.body?.source()
        if (source == null) {
            val fullBody = response.body?.string() ?: ""
            Log.w(TAG, "source 为 null, 尝试解析完整body: $fullBody")
            val reader = JsonReader(StringReader(fullBody))
            reader.isLenient = true
            val root = JsonParser.parseReader(reader).asJsonObject
            val text = extractTextFromResponse(root)
            if (text != null) { emit(text); return@flow }
            throw Exception("响应体为空")
        }

        val allLines = mutableListOf<String>()
        var foundAnyData = false

        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                allLines.add(line)

                val trimmed = line.trimStart()
                if (trimmed.startsWith("data:")) {
                    foundAnyData = true
                    val data = trimmed.removePrefix("data:").trim()
                    if (data.isBlank() || data == "[DONE]" || data.startsWith("[DONE]")) continue

                    try {
                        val reader = JsonReader(StringReader(data))
                        reader.isLenient = true
                        val element = JsonParser.parseReader(reader)
                        if (element.isJsonObject) {
                            val text = extractTextFromStreamJson(element.asJsonObject)
                            if (text != null) {
                                emit(text)
                            }
                        }
                    } catch (_: Exception) { }
                }
            }
        } finally {
            response.close()
        }

        Log.d(TAG, "=== 原始响应行数: ${allLines.size}, 含 data: $foundAnyData ===")

        if (foundAnyData) return@flow
        if (allLines.isEmpty()) throw Exception("响应为空")

        val wholeBody = allLines.joinToString("\n")
        Log.d(TAG, "=== 尝试解析完整 JSON 响应 ===\n$wholeBody")

        try {
            val reader = JsonReader(StringReader(wholeBody))
            reader.isLenient = true
            val root = JsonParser.parseReader(reader).asJsonObject
            val text = extractTextFromResponse(root)
            if (text != null) {
                Log.d(TAG, "JSON 解析成功: $text")
                emit(text)
                return@flow
            }
            Log.e(TAG, "JSON 中未找到可识别的文本字段")
            throw Exception("API 响应格式不符: ${wholeBody.take(200)}")
        } catch (e: Exception) {
            if (e.message?.startsWith("API 响应") == true) throw e
            Log.e(TAG, "JSON 解析异常: ${e.message}")
            throw Exception("无法解析响应: ${wholeBody.take(200)}")
        }
    }.flowOn(Dispatchers.IO)

    fun updateApiConfig(baseUrl: String, apiKey: String) {
        this.baseUrl = baseUrl.trim().removeSuffix("/").ifBlank { DEFAULT_BASE_URL }
        this.apiKey = apiKey.trim().ifBlank { DEFAULT_API_KEY }
    }

    fun currentBaseUrl(): String = baseUrl

    fun currentApiKey(): String = apiKey

    private fun extractTextFromStreamJson(json: com.google.gson.JsonObject): String? {
        val evType = json.get("type")?.asString ?: ""
        if (evType == "error" || evType == "response.done" || evType == "done") return null

        if (evType == "response.output_text.delta") {
            val delta = json.get("delta")?.asString
            if (!delta.isNullOrBlank()) return delta
        }

        return null
    }

    private fun extractTextFromResponse(root: com.google.gson.JsonObject): String? {
        val outputArray = root.getAsJsonArray("output")
        if (outputArray != null) {
            for (item in outputArray) {
                val obj = item.asJsonObject
                if (obj.get("type")?.asString == "message") {
                    val contentArray = obj.getAsJsonArray("content")
                    if (contentArray != null && contentArray.size() > 0) {
                        return contentArray[0].asJsonObject.get("text")?.asString
                    }
                }
            }
        }

        val resp = root.getAsJsonObject("response")
        val respOutput = resp?.getAsJsonArray("output")
        if (respOutput != null) {
            for (item in respOutput) {
                val obj = item.asJsonObject
                if (obj.get("type")?.asString == "message") {
                    val contentArray = obj.getAsJsonArray("content")
                    if (contentArray != null && contentArray.size() > 0) {
                        return contentArray[0].asJsonObject.get("text")?.asString
                    }
                }
            }
        }

        val choices = root.getAsJsonArray("choices")
        if (choices != null && choices.size() > 0) {
            val txt = choices[0].asJsonObject.getAsJsonObject("message")?.get("content")?.asString
            if (!txt.isNullOrBlank()) return txt
        }

        return null
    }
}
