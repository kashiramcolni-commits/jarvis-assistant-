package com.jarvis.assistant.wake

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import com.jarvis.assistant.BuildConfig
import com.jarvis.assistant.command.CommandExecutor
import com.jarvis.assistant.command.CommandParser
import com.jarvis.assistant.util.Notifs
import com.jarvis.assistant.voice.Speech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground microphone service that listens for the wake word.
 * Porcupine and SpeechRecognizer can't share the mic, so Porcupine pauses while a command is captured.
 */
class WakeWordService : Service() {
    private var porcupine: PorcupineManager? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var speech: Speech
    @Volatile private var busy = false

    override fun onCreate() {
        super.onCreate()
        Notifs.ensureChannels(this)
        val n: Notification = NotificationCompat.Builder(this, Notifs.CH_LISTEN)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Jarvis is listening")
            .setContentText("Say \"Jarvis\" to give a command")
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(this, 1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        speech = Speech(this)
        startPorcupine()
    }

    private fun startPorcupine() {
        try {
            porcupine = PorcupineManager.Builder()
                .setAccessKey(BuildConfig.PICOVOICE_KEY)
                // Built-in keyword is "Jarvis". For the exact phrase "Hey Jarvis", train it in the
                // Picovoice Console, put the .ppn in assets/ and use .setKeywordPath("hey_jarvis.ppn").
                .setKeyword(Porcupine.BuiltInKeyword.JARVIS)
                .setSensitivity(0.7f)
                .build(applicationContext, PorcupineManagerCallback { _ -> onWake() })
            porcupine?.start()
        } catch (e: PorcupineException) {
            Log.e("Jarvis", "Porcupine failed: ${e.message}")
            stopSelf()
        }
    }

    private fun onWake() {
        if (busy) return
        busy = true
        scope.launch {
            try {
                porcupine?.stop()
                ToneGenerator(AudioManager.STREAM_MUSIC, 80).startTone(ToneGenerator.TONE_PROP_BEEP)
                val heard = speech.listen()
                if (heard == null) {
                    speech.speak("I didn't catch that.")
                } else {
                    val reply = CommandExecutor(this@WakeWordService).execute(CommandParser.parse(heard))
                    speech.speak(reply)
                }
            } finally {
                runCatching { porcupine?.start() }
                busy = false
            }
        }
    }

    override fun onDestroy() {
        runCatching { porcupine?.stop(); porcupine?.delete() }
        speech.shutdown()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(ctx: Context) =
            ContextCompat.startForegroundService(ctx, Intent(ctx, WakeWordService::class.java))
        fun stop(ctx: Context) = ctx.stopService(Intent(ctx, WakeWordService::class.java))
    }
}
