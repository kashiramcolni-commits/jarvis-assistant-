package com.jarvis.assistant.telecom

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import androidx.core.app.NotificationCompat
import com.jarvis.assistant.MainActivity
import com.jarvis.assistant.util.Notifs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object CallHolder { @Volatile var call: Call? = null }

/** Requires the Default Dialer role. Auto-answers unknown callers; other calls get a notification. */
class JarvisInCallService : InCallService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCallAdded(call: Call) {
        CallHolder.call = call
        val number = call.details.handle?.schemeSpecificPart
        if (call.state == Call.STATE_RINGING) {
            if (AgentPolicy.shouldAutoAnswer(this, number)) {
                scope.launch {
                    delay(2500) // let it ring briefly so it looks natural
                    if (call.state == Call.STATE_RINGING) {
                        call.answer(VideoProfile.STATE_AUDIO_ONLY)
                        setAudioRoute(CallAudioState.ROUTE_SPEAKER)
                        CallAgent(this@JarvisInCallService, call, number).run()
                    }
                }
            } else {
                showIncoming(number)
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        if (CallHolder.call == call) CallHolder.call = null
        getSystemService(NotificationManager::class.java).cancel(42)
    }

    private fun showIncoming(number: String?) {
        fun action(a: String) = PendingIntent.getBroadcast(
            this, a.hashCode(), Intent(this, CallActionReceiver::class.java).setAction(a),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, Notifs.CH_CALL)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Incoming call")
            .setContentText(number ?: "Unknown")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(open, true)
            .addAction(0, "Answer", action("ANSWER"))
            .addAction(0, "Decline", action("DECLINE"))
            .setOngoing(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(42, n)
    }
}

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            "ANSWER" -> CallHolder.call?.answer(VideoProfile.STATE_AUDIO_ONLY)
            "DECLINE" -> CallHolder.call?.reject(false, null)
        }
        ctx.getSystemService(NotificationManager::class.java).cancel(42)
    }
}
