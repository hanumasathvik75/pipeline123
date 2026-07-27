package com.wifiar.scanner.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.wifiar.scanner.data.ScanSummary
import com.wifiar.scanner.data.toSignalLabel

class GeminiRepository(private val apiKey: String) {

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = 600
            }
        )
    }

    suspend fun getSuggestions(summary: ScanSummary): Result<String> {
        return try {
            val prompt = buildPrompt(summary)
            val response = model.generateContent(prompt)
            Result.success(response.text ?: "No response received")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(summary: ScanSummary): String {
        val dirInfo = summary.directions.joinToString("\n") { dir ->
            "  - ${dir.label}: avg ${dir.avgRssi}dBm (${dir.avgRssi.toSignalLabel()}), " +
                "min ${dir.minRssi}dBm, max ${dir.maxRssi}dBm, ${dir.pointCount} scan points"
        }
        return """
            You are a WiFi network expert. A user has scanned their home using an AR WiFi scanner app.
            Here is the scan data:
            
            - Total scan points: ${summary.points.size}
            - Estimated area scanned: ${"%,.0f".format(summary.totalArea)} sq meters
            - Best signal: ${summary.bestRssi}dBm (${summary.bestRssi.toSignalLabel()}) at position (${"%.1f".format(summary.bestX)}m, ${"%.1f".format(summary.bestY)}m)
            - Worst signal: ${summary.worstRssi}dBm (${summary.worstRssi.toSignalLabel()}) at position (${"%.1f".format(summary.worstX)}m, ${"%.1f".format(summary.worstY)}m)
            - Signal spread: ${Math.abs(summary.bestRssi - summary.worstRssi)}dBm
            
            Signal by direction:
            $dirInfo
            
            Give 4-5 concise, practical, personalized recommendations:
            1. Where in the home to work/video call
            2. Whether/where to reposition the router
            3. Any dead zone warnings
            4. Band selection advice (5GHz vs 2.4GHz)
            5. One actionable next step
            
            Keep each recommendation to 1-2 sentences. Be specific using the actual dBm values. Use emojis for readability.
        """.trimIndent()
    }
}
