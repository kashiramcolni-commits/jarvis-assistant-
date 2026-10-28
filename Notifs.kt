package com.jarvis.assistant.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object Notifs {
    const val CH_LISTEN = "listen"
    const val CH_CALL = "call"
    const val CH_SUMMARY = "summary"

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_LISTEN, "Listening", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_CALL, "Incoming calls", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CH_SUMMARY, "Call summaries", NotificationManager.IMPORTANCE_DEFAULT))
    }
}
