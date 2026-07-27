package com.example.wifiarscanner.ai

import com.example.wifiarscanner.data.ScanSummary
import com.example.wifiarscanner.data.toSignalLabel
import kotlin.math.abs
import kotlin.math.sqrt

data class Suggestion(
    val emoji: String,
    val title: String,
    val detail: String,
    val priority: Int  // 1 = high, 2 = medium, 3 = low
)

object SuggestionEngine {

    fun analyze(summary: ScanSummary): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()

        val spread = abs(summary.bestRssi - summary.worstRssi)
        val avgRssi = summary.points.map { it.rssi }.average().toInt()
        val bestLabel = summary.bestRssi.toSignalLabel()
        val worstLabel = summary.worstRssi.toSignalLabel()

        // 1. Best workspace location
        suggestions.add(
            Suggestion(
                emoji = "💼",
                title = "Best spot to work from",
                detail = "The strongest signal zone ($bestLabel, ${summary.bestRssi} dBm) is " +
                    "${formatPos(summary.bestX, summary.bestY)} from where you started scanning. " +
                    "Ideal for video calls, uploads, and latency-sensitive tasks.",
                priority = 1
            )
        )

        // 2. Dead zone
        if (summary.worstRssi < -73) {
            suggestions.add(
                Suggestion(
                    emoji = "🚫",
                    title = "Dead zone detected",
                    detail = "Signal drops to $worstLabel (${summary.worstRssi} dBm) " +
                        "${formatPos(summary.worstX, summary.worstY)} from start. " +
                        "Streaming and video calls will likely stutter or fail here.",
                    priority = 1
                )
            )
        }

        // 3. Router placement advice
        when {
            spread > 35 -> suggestions.add(
                Suggestion(
                    emoji = "📡",
                    title = "Move your router — big dead zones",
                    detail = "${spread}dBm signal variation is very high. Your router is likely " +
                        "in a corner or behind thick walls. Move it to the most open, central " +
                        "location in your home — ideally elevated (shelf vs floor).",
                    priority = 1
                )
            )
            spread > 18 -> suggestions.add(
                Suggestion(
                    emoji = "📡",
                    title = "Router could be repositioned",
                    detail = "${spread}dBm variation detected. Shifting the router a few meters " +
                        "toward the weaker zone should balance coverage noticeably.",
                    priority = 2
                )
            )
            else -> suggestions.add(
                Suggestion(
                    emoji = "✅",
                    title = "Router placement looks good",
                    detail = "Only ${spread}dBm variation across the scanned area — your router " +
                        "is distributing signal well in this space.",
                    priority = 3
                )
            )
        }

        // 4. Overall quality
        suggestions.add(
            when {
                avgRssi >= -58 -> Suggestion(
                    emoji = "🟢",
                    title = "Overall: Strong network",
                    detail = "Average signal is ${avgRssi}dBm — excellent. Expect reliable 4K " +
                        "streaming, fast gaming, and rock-solid video calls in most of this zone.",
                    priority = 3
                )
                avgRssi >= -68 -> Suggestion(
                    emoji = "🟡",
                    title = "Overall: Acceptable network",
                    detail = "Average ${avgRssi}dBm — good for browsing and HD video, " +
                        "but 4K or large uploads may be inconsistent in weaker spots.",
                    priority = 2
                )
                else -> Suggestion(
                    emoji = "🔴",
                    title = "Overall: Weak network coverage",
                    detail = "Average ${avgRssi}dBm is below comfortable threshold. Consider a WiFi " +
                        "mesh node or extender, or move key devices physically closer to the router.",
                    priority = 1
                )
            }
        )

        // 5. Directional insight
        val bestDir = summary.directions.maxByOrNull { it.avgRssi }
        val worstDir = summary.directions.minByOrNull { it.avgRssi }
        if (bestDir != null && worstDir != null && bestDir.label != worstDir.label) {
            suggestions.add(
                Suggestion(
                    emoji = "🧭",
                    title = "Signal strongest to the ${bestDir.label}",
                    detail = "${bestDir.label} zone averages ${bestDir.avgRssi}dBm vs " +
                        "${worstDir.avgRssi}dBm in the ${worstDir.label}. Your router is likely " +
                        "positioned toward the ${bestDir.label} side of this space.",
                    priority = 2
                )
            )
        }

        // 6. Band selection tip
        when {
            avgRssi < -65 -> suggestions.add(
                Suggestion(
                    emoji = "📶",
                    title = "Switch to 2.4GHz band",
                    detail = "At this distance, 2.4GHz penetrates walls and travels farther than " +
                        "5GHz. Connect to your router's 2.4GHz network for better range here.",
                    priority = 2
                )
            )
            avgRssi >= -52 -> suggestions.add(
                Suggestion(
                    emoji = "⚡",
                    title = "Use 5GHz band here",
                    detail = "Strong signal (${avgRssi}dBm)! Switch to your router's 5GHz network — " +
                        "it has more bandwidth channels and less interference at close range.",
                    priority = 3
                )
            )
        }

        // 7. Physical obstacle tip
        if (spread > 28) {
            suggestions.add(
                Suggestion(
                    emoji = "🧱",
                    title = "Obstacles are blocking signal",
                    detail = "The sharp ${spread}dBm drop suggests thick walls, concrete, metal " +
                        "appliances, or mirrors between your router and weak zones. " +
                        "Even repositioning the router by 1-2m around obstacles can help significantly.",
                    priority = 2
                )
            )
        }

        return suggestions.sortedBy { it.priority }
    }

    private fun formatPos(x: Float, y: Float): String {
        val dist = sqrt(x * x + y * y)
        val dir = when {
            y > 0 && abs(y) >= abs(x) -> "north"
            y < 0 && abs(y) >= abs(x) -> "south"
            x > 0 -> "east"
            else -> "west"
        }
        return "%.1fm %s".format(dist, dir)
    }
}
