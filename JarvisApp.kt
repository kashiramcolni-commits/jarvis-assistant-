package com.jarvis.assistant

import android.app.Application
import com.jarvis.assistant.telecom.CallStore
import com.jarvis.assistant.util.Notifs

class JarvisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifs.ensureChannels(this)
        CallStore.init(this)
    }
}
