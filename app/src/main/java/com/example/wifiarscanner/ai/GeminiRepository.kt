package com.example.wifiarscanner.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.example.wifiarscanner.data.ScanSummary
import com.example.wifiarscanner.data.toSignalLabel

class GeminiRepository(private val apiKey: String) {

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = 700
            }
        )
    }

    suspend fun getSuggestions(summary: ScanSummary): Result<String> {
        return try {
            val response = model.generateContent(buildPrompt(summary))
            Result.success(response.text ?: "No response received.")
        } catch (e: Exception) {
            Result.failure(Exception("Gemini error: ${e.message}"))
        }
    }

    private fun buildPrompt(summary: ScanSummary): String {
        val dirInfo = summary.directions.joinToString("\n") { d ->
            "  - ${d.label}: avg ${d.avgRssi}dBm (${d.avgRssi.toSignalLabel()}), " +
                "range ${d.minRssi} to ${d.maxRssi}dBm, ${d.pointCount} points"
        }
        val avg = summary.points.map { it.rssi }.average().toInt()
        return """
            You are a home WiFi network expert. A user scanned their home with an AR WiFi signal app.
            
            Scan results:
            - Total data points: ${summary.points.size}
            - Scanned area: ~${"%.0f".format(summary.totalArea)} sq meters
            - Best signal: ${summary.bestRssi}dBm (${summary.bestRssi.toSignalLabel()}) at position (${"%.1f".format(summary.bestX)}m, ${"%.1f".format(summary.bestY)}m)
            - Worst signal: ${summary.worstRssi}dBm (${summary.worstRssi.toSignalLabel()}) at position (${"%.1f".format(summary.worstX)}m, ${"%.1f".format(summary.worstY)}m)
            - Average signal: ${avg}dBm
            - Signal variation: ${Math.abs(summary.bestRssi - summary.worstRssi)}dBm
            
            Signal by direction (relative to scan start):
            $dirInfo
            
            Provide 5 specific, practical recommendations. Use the actual dBm numbers from this data.
            Format each as: [emoji] **Title**: Detail (1-2 sentences).
            Cover: best workspace location, router placement, dead zones, band selection, one pro tip.
            Be direct and specific, not generic. Use emojis.
        """.trimIndent()
    }
}
