package com.jarvis.assistant.command

sealed interface Command {
    data class Toggle(val wifi: Boolean, val on: Boolean) : Command {
        val name get() = if (wifi) "Wi-Fi" else "Bluetooth"
    }
    data class Volume(val delta: Int? = null, val percent: Int? = null) : Command
    data class Brightness(val percent: Int) : Command
    data class OpenApp(val name: String) : Command
    data class Scroll(val down: Boolean) : Command
    data class Tap(val label: String) : Command
    data class WhatsApp(val contact: String, val message: String) : Command
    data class Call(val contact: String) : Command
    data object Back : Command
    data object Home : Command
    data class Unknown(val text: String) : Command
}

/** Rule-based parser. Swap for an LLM intent classifier later; the Command types stay the same. */
object CommandParser {
    private val whatsapp1 = Regex("""(?:send )?(?:a )?whats ?app(?: message)? to (.+?) (?:saying|that says|say) (.+)""")
    private val whatsapp2 = Regex("""(?:message|text) (.+?) on whats ?app (?:saying|that says|say) (.+)""")
    private val call = Regex("""^(?:call|phone|dial) (.+)""")
    private val open = Regex("""^(?:open|launch|start) (.+)""")
    private val tap = Regex("""^(?:tap|click|press|select) (?:on )?(.+)""")
    private val num = Regex("""(\d{1,3})""")

    fun parse(raw: String): Command {
        val t = raw.lowercase().trim().removeSuffix(".")
        whatsapp1.find(t)?.let { return Command.WhatsApp(it.groupValues[1].trim(), it.groupValues[2].trim()) }
        whatsapp2.find(t)?.let { return Command.WhatsApp(it.groupValues[1].trim(), it.groupValues[2].trim()) }

        val wifi = Regex("""wi-?fi""").containsMatchIn(t)
        val bt = "bluetooth" in t
        if (wifi || bt) {
            val off = listOf("off", "disable", "turn off").any { it in t }
            return Command.Toggle(wifi = wifi, on = !off)
        }
        if ("volume" in t) {
            val n = num.find(t)?.groupValues?.get(1)?.toInt()
            return when {
                n != null -> Command.Volume(percent = n.coerceIn(0, 100))
                "up" in t || "increase" in t || "raise" in t -> Command.Volume(delta = 1)
                "down" in t || "decrease" in t || "lower" in t -> Command.Volume(delta = -1)
                else -> Command.Unknown(raw)
            }
        }
        if ("brightness" in t) {
            val n = num.find(t)?.groupValues?.get(1)?.toInt()
            if (n != null) return Command.Brightness(n.coerceIn(1, 100))
            if ("max" in t || "full" in t) return Command.Brightness(100)
            if ("low" in t || "dim" in t) return Command.Brightness(15)
        }
        if (t.startsWith("scroll")) return Command.Scroll(down = "up" !in t)
        if (t == "go back" || t == "back") return Command.Back
        if (t == "go home" || t == "home") return Command.Home
        call.find(t)?.let { return Command.Call(it.groupValues[1].trim()) }
        open.find(t)?.let { return Command.OpenApp(it.groupValues[1].trim()) }
        tap.find(t)?.let { return Command.Tap(it.groupValues[1].trim()) }
        return Command.Unknown(raw)
    }
}
