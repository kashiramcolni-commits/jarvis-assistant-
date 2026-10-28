package com.jarvis.assistant.control

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.provider.Settings

object SystemControls {
    fun nudgeVolume(ctx: Context, delta: Int) {
        val am = ctx.getSystemService(AudioManager::class.java)
        am.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (delta > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun setVolume(ctx: Context, percent: Int) {
        val am = ctx.getSystemService(AudioManager::class.java)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, max * percent / 100, AudioManager.FLAG_SHOW_UI)
    }

    /** Needs the "Modify system settings" special permission (WRITE_SETTINGS). */
    fun setBrightness(ctx: Context, percent: Int): Boolean {
        if (!Settings.System.canWrite(ctx)) return false
        val cr = ctx.contentResolver
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, (percent * 255 / 100).coerceIn(1, 255))
        return true
    }

    fun launchApp(ctx: Context, name: String): Boolean {
        val pm = ctx.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val match = pm.queryIntentActivities(launcher, 0)
            .firstOrNull { it.loadLabel(pm).toString().contains(name, ignoreCase = true) } ?: return false
        val intent = pm.getLaunchIntentForPackage(match.activityInfo.packageName) ?: return false
        // Background activity starts are allowed because the user granted the overlay permission.
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    fun wifiOn(ctx: Context): Boolean =
        ctx.applicationContext.getSystemService(WifiManager::class.java).isWifiEnabled

    fun bluetoothOn(ctx: Context): Boolean = try {
        ctx.getSystemService(BluetoothManager::class.java).adapter?.isEnabled == true
    } catch (e: SecurityException) { false }
}
