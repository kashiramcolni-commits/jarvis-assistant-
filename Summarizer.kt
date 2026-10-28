package com.jarvis.assistant.telecom

interface Summarizer {
    suspend fun summarize(number: String?, startedAt: Long, turns: List<Turn>): CallSummary
}

/**
 * Offline heuristic summarizer. To use an LLM, implement [Summarizer] and send the transcript to
 * your own backend (do not ship an API key inside the APK).
 */
class RuleBasedSummarizer : Summarizer {
    private val urgent = listOf("urgent", "emergency", "asap", "hospital", "accident", "important",
        "deadline", "immediately", "police", "tomorrow", "today")
    private val spam = listOf("offer", "loan", "insurance", "survey", "free", "prize", "credit card",
        "discount", "promotion", "subscription", "limited time")

    override suspend fun summarize(number: String?, startedAt: Long, turns: List<Turn>): CallSummary {
        val caller = turns.filter { it.speaker == "Caller" }.joinToString(" ") { it.text }
        val lower = caller.lowercase()
        val isUrgent = urgent.any { it in lower }
        val isSpam = spam.any { it in lower }
        val (callBack, reason) = when {
            caller.isBlank() -> false to "Caller left no message."
            isUrgent && !isSpam -> true to "Sounds time-sensitive."
            isSpam -> false to "Looks like a sales or promotional call."
            else -> true to "Real message left; worth a quick reply."
        }
        val summary = if (caller.isBlank()) "No message was left." else caller.take(240)
        val transcript = turns.joinToString("\n") { "${it.speaker}: ${it.text}" }
        return CallSummary(number, startedAt, transcript, summary, callBack, reason)
    }
}
