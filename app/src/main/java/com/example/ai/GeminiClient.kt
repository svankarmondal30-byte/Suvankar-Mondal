package com.example.ai

import com.example.BuildConfig
import com.example.model.AssistantLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY.trim()

    fun isConfigured(): Boolean {
        return apiKey.isNotBlank() && !apiKey.equals("MY_GEMINI_API_KEY", ignoreCase = true)
    }

    suspend fun generateResponse(
        prompt: String,
        userLanguage: AssistantLanguage,
        conversationContext: List<Pair<String, Boolean>> = emptyList()
    ): String? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val systemInstruction = """
                You are MAX, a personal smartphone AI assistant.
                Traits: Smart, calm, fast, confident, helpful, natural, slightly futuristic.
                Rules:
                1. Always reply in the SAME language the user uses: Bengali (বাংলা), Hindi (हिन्दी), Hinglish, or English.
                2. Keep answers concise, clear, and direct, suitable for a voice assistant.
                3. Never claim an action was done on the phone unless instructed by verified tools.
                4. Respect Android security.
            """.trimIndent()

            val contentsArray = JSONArray()

            // Include recent conversation context (up to 3 turns)
            conversationContext.takeLast(6).forEach { (msgText, isUser) ->
                val role = if (isUser) "user" else "model"
                val contentObj = JSONObject()
                contentObj.put("role", role)
                val partsArray = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", msgText)
                partsArray.put(partObj)
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }

            // Current user query
            val currentContent = JSONObject()
            currentContent.put("role", "user")
            val currentParts = JSONArray()
            val textPart = JSONObject()
            textPart.put("text", prompt)
            currentParts.put(textPart)
            currentContent.put("parts", currentParts)
            contentsArray.put(currentContent)

            val rootJson = JSONObject()
            rootJson.put("contents", contentsArray)

            val systemObj = JSONObject()
            val sysParts = JSONArray()
            val sysPart = JSONObject()
            sysPart.put("text", systemInstruction)
            sysParts.put(sysPart)
            systemObj.put("parts", sysParts)
            rootJson.put("systemInstruction", systemObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = rootJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text")?.trim()
                    }
                }
            }
        } catch (_: Exception) {}

        null
    }
}
