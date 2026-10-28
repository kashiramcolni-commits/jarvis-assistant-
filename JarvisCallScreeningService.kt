package com.jarvis.assistant.telecom

import android.telecom.Call
import android.telecom.CallScreeningService

/** Runs before the phone rings. Unknown callers are silenced so Jarvis can answer them. */
class JarvisCallScreeningService : CallScreeningService() {
    override fun onScreenCall(details: Call.Details) {
        val response = CallResponse.Builder()
        if (details.callDirection == Call.Details.DIRECTION_INCOMING) {
            val number = details.handle?.schemeSpecificPart
            if (AgentPolicy.shouldAutoAnswer(this, number)) response.setSilenceCall(true)
        }
        respondToCall(details, response.build())
    }
}
