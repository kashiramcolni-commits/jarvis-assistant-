package com.jarvis.assistant.command

import android.accessibilityservice.AccessibilityService
import android.content.Context
import com.jarvis.assistant.control.JarvisAccessibilityService
import com.jarvis.assistant.control.Messaging
import com.jarvis.assistant.control.SystemControls
import kotlinx.coroutines.delay

class CommandExecutor(private val ctx: Context) {
    private val needA11y = "Please enable the Jarvis accessibility service first."

    suspend fun execute(c: Command): String {
        val a11y = JarvisAccessibilityService.instance
        return when (c) {
            is Command.Toggle -> toggle(c, a11y)
            is Command.Volume -> {
                if (c.percent != null) SystemControls.setVolume(ctx, c.percent)
                else SystemControls.nudgeVolume(ctx, c.delta ?: 1)
                "Done."
            }
            is Command.Brightness ->
                if (SystemControls.setBrightness(ctx, c.percent)) "Brightness set to ${c.percent} percent."
                else "Please grant the modify system settings permission."
            is Command.OpenApp ->
                if (SystemControls.launchApp(ctx, c.name)) "Opening ${c.name}." else "I couldn't find ${c.name}."
            is Command.Scroll -> a11y?.let { if (it.scroll(c.down)) "Scrolling." else "Nothing to scroll." } ?: needA11y
            is Command.Tap -> a11y?.let { if (it.clickByText(c.label)) "Tapped ${c.label}." else "I can't see ${c.label}." } ?: needA11y
            Command.Back -> a11y?.let { it.global(AccessibilityService.GLOBAL_ACTION_BACK); "Okay." } ?: needA11y
            Command.Home -> a11y?.let { it.global(AccessibilityService.GLOBAL_ACTION_HOME); "Okay." } ?: needA11y
            is Command.WhatsApp -> Messaging.whatsApp(ctx, c.contact, c.message)
            is Command.Call -> Messaging.call(ctx, c.contact)
            is Command.Unknown -> "Sorry, I don't know how to do that yet."
        }
    }

    private suspend fun toggle(c: Command.Toggle, a11y: JarvisAccessibilityService?): String {
        val current = if (c.wifi) SystemControls.wifiOn(ctx) else SystemControls.bluetoothOn(ctx)
        if (current == c.on) return "${c.name} is already ${if (c.on) "on" else "off"}."
        a11y ?: return needA11y
        // Android 10+ forbids apps from flipping Wi-Fi/Bluetooth directly, so we tap the Quick Settings tile.
        a11y.global(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
        delay(800)
        val labels = if (c.wifi) listOf("Internet", "Wi-Fi") else listOf("Bluetooth")
        val ok = a11y.awaitClick { labels.any { a11y.clickByText(it) } }
        delay(400)
        a11y.global(AccessibilityService.GLOBAL_ACTION_BACK)
        return if (ok) "${c.name} turned ${if (c.on) "on" else "off"}." else "I couldn't find the ${c.name} tile."
    }
}
