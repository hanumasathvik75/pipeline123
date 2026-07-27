package com.wifiar.scanner.ai

import com.wifiar.scanner.data.ScanSummary
import com.wifiar.scanner.data.toSignalLabel
import kotlin.math.abs

data class Suggestion(
    val emoji: String,
    val title: String,
    val detail: String,
    val priority: Int  // 1 = high, 2 = medium, 3 = low
)

object SuggestionEngine {

    fun analyze(summary: ScanSummary): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()

        val bestLabel = summary.bestRssi.toSignalLabel()
        val worstLabel = summary.worstRssi.toSignalLabel()
        val spread = abs(summary.bestRssi - summary.worstRssi)
        val avgRssi = summary.points.map { it.rssi }.average().toInt()

        // 1. Best workspace spot
        suggestions.add(
            Suggestion(
                emoji = "💼",
                title = "Best spot to work from",
                detail = "The strongest signal zone ($bestLabel, ${summary.bestRssi} dBm) " +
                    "is ${formatPosition(summary.bestX, summary.bestY)} from where you started. " +
                    "This is ideal for video calls and fast browsing.",
                priority = 1
            )
        )

        // 2. Dead zone warning
        if (summary.worstRssi < -75) {
            suggestions.add(
                Suggestion(
                    emoji = "🚫",
                    title = "Dead zone detected",
                    detail = "Signal drops to $worstLabel (${summary.worstRssi} dBm) at " +
                        "${formatPosition(summary.worstX, summary.worstY)} from start. " +
                        "Avoid video calls or large downloads here.",
                    priority = 1
                )
            )
        }

        // 3. Router placement advice based on signal spread
        when {
            spread > 40 -> suggestions.add(
                Suggestion(
                    emoji = "📡",
                    title = "Move your router",
                    detail = "There's a ${spread}dBm difference across your space — that's huge. " +
                        "Your router is likely tucked in a corner or behind walls. " +
                        "Move it to the most central open area of your home for even coverage.",
                    priority = 1
                )
            )
            spread > 20 -> suggestions.add(
                Suggestion(
                    emoji = "📡",
                    title = "Router position could be improved",
                    detail = "${spread}dBm variation detected. Consider moving your router " +
                        "slightly toward the weak zone to balance coverage.",
                    priority = 2
                )
            )
            else -> suggestions.add(
                Suggestion(
                    emoji = "✅",
                    title = "Router placement is good",
                    detail = "Only ${spread}dBm variation — your router is well positioned for this space.",
                    priority = 3
                )
            )
        }

        // 4. Overall quality assessment
        val qualityTip = when {
            avgRssi >= -60 -> Suggestion(
                emoji = "🟢",
                title = "Overall: Strong network",
                detail = "Average signal is ${avgRssi}dBm. Your WiFi coverage in this area is excellent. " +
                    "You can reliably stream 4K and do video calls anywhere in this zone.",
                priority = 3
            )
            avgRssi >= -70 -> Suggestion(
                emoji = "🟡",
                title = "Overall: Acceptable network",
                detail = "Average signal is ${avgRssi}dBm. Good for browsing and HD streaming, " +
                    "but may stutter on 4K or large uploads in weaker spots.",
                priority = 2
            )
            else -> Suggestion(
                emoji = "🔴",
                title = "Overall: Weak network",
                detail = "Average signal is ${avgRssi}dBm — below comfortable threshold. " +
                    "Consider a WiFi extender/mesh node, or move closer to your router for important tasks.",
                priority = 1
            )
        }
        suggestions.add(qualityTip)

        // 5. Directional advice
        val bestDir = summary.directions.maxByOrNull { it.avgRssi }
        val worstDir = summary.directions.minByOrNull { it.avgRssi }
        if (bestDir != null && worstDir != null && bestDir.label != worstDir.label) {
            suggestions.add(
                Suggestion(
                    emoji = "🧭",
                    title = "Signal is stronger to the ${bestDir.label}",
                    detail = "${bestDir.label} zone averages ${bestDir.avgRssi}dBm vs " +
                        "${worstDir.avgRssi}dBm in the ${worstDir.label}. " +
                        "Your router is likely positioned to the ${bestDir.label} of your scanned area.",
                    priority = 2
                )
            )
        }

        // 6. 5GHz vs 2.4GHz tip based on signal strength
        if (avgRssi < -65) {
            suggestions.add(
                Suggestion(
                    emoji = "📶",
                    title = "Try switching to 2.4GHz band",
                    detail = "At this distance, 2.4GHz penetrates walls better than 5GHz. " +
                        "Connect to your router's 2.4GHz network for better range.",
                    priority = 2
                )
            )
        } else if (avgRssi >= -55) {
            suggestions.add(
                Suggestion(
                    emoji = "⚡",
                    title = "Use 5GHz band here",
                    detail = "Strong signal detected (${avgRssi}dBm). Switch to your router's 5GHz " +
                        "network for faster speeds — it has more bandwidth at close range.",
                    priority = 3
                )
            )
        }

        // 7. Obstacle tip if spread is high
        if (spread > 30) {
            suggestions.add(
                Suggestion(
                    emoji = "🧱",
                    title = "Walls or obstacles detected",
                    detail = "The sharp signal drop suggests thick walls, metal objects, or large " +
                        "appliances between your router and weak zones. Microwaves, mirrors, and " +
                        "concrete walls can cut signal by 20–30dBm.",
                    priority = 2
                )
            )
        }

        return suggestions.sortedBy { it.priority }
    }

    private fun formatPosition(x: Float, y: Float): String {
        val dist = kotlin.math.sqrt(x * x + y * y)
        val direction = when {
            y > 0 && abs(y) > abs(x) -> "north"
            y < 0 && abs(y) > abs(x) -> "south"
            x > 0 -> "east"
            else -> "west"
        }
        return "%.1fm ${direction}".format(dist)
    }
}
