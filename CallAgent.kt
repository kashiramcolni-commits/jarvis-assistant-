package com.jarvis.assistant.telecom

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.jarvis.assistant.util.Notifs
import com.jarvis.assistant.voice.Speech
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import android.telecom.Call

/**
 * Converses with an unknown caller. Audio runs over the speakerphone: TTS plays out loud and the mic
 * picks up the caller. Android gives third-party apps no direct access to the call audio stream.
 */
class CallAgent(
    private val ctx: Context,
    private val call: Call,
    private val number: String?,
    private val summarizer: Summarizer = RuleBasedSummarizer()
) {
    suspend fun run() {
        val speech = Speech(ctx)
        val started = System.currentTimeMillis()
        val turns = mutableListOf<Turn>()
        suspend fun say(t: String) { turns += Turn("Jarvis", t); speech.speak(t) }

        withTimeoutOrNull(5000) { while (call.state != Call.STATE_ACTIVE) delay(200) }
        delay(600)
        val owner = AgentPolicy.ownerName(ctx)
        // Disclosure up front: many jurisdictions require consent to record or transcribe calls.
        say("Hello, this is Jarvis, the assistant for $owner. $owner can't take your call right now. " +
            "This call is being recorded and transcribed. Please say your name and why you're calling.")

        var silent = 0
        var answers = 0
        while (answers < 3 && call.state == Call.STATE_ACTIVE) {
            val heard = speech.listen()
            if (heard.isNullOrBlank()) {
                if (++silent >= 2) break
                say("Sorry, I didn't catch that. Could you repeat?")
                continue
            }
            turns += Turn("Caller", heard)
            answers++
            if (answers > 1 && Regex("""\b(no|nope|that's all|nothing|that is all)\b""").containsMatchIn(heard.lowercase())) break
            if (answers < 3) say(if (answers == 1) "Thank you. Anything else you'd like me to pass on?" else "Got it. Is there more?")
        }
        say("Thank you. I'll pass your message along. Goodbye.")
        delay(300)
        if (call.state != Call.STATE_DISCONNECTED) call.disconnect()
        speech.shutdown()

        val result = summarizer.summarize(number, started, turns)
        CallStore.add(result)
        notify(result)
    }

    private fun notify(s: CallSummary) {
        val text = "${s.summary}\n${if (s.callBack) "Recommend: call back" else "Recommend: skip"} (${s.reason})"
        val n = NotificationCompat.Builder(ctx, Notifs.CH_SUMMARY)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Jarvis took a call from ${s.number ?: "unknown"}")
            .setContentText(s.summary.take(80))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(s.startedAt.toInt(), n)
    }
}
