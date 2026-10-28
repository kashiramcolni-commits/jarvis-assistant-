package com.jarvis.assistant.telecom

import android.content.Context
import com.jarvis.assistant.control.Contacts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Turn(val speaker: String, val text: String)

data class CallSummary(
    val number: String?,
    val startedAt: Long,
    val transcript: String,
    val summary: String,
    val callBack: Boolean,
    val reason: String
)

/** Simple JSON-file store. Swap for Room if the history grows. */
object CallStore {
    private val _items = MutableStateFlow<List<CallSummary>>(emptyList())
    val items: StateFlow<List<CallSummary>> = _items.asStateFlow()
    private lateinit var file: File

    fun init(ctx: Context) {
        file = File(ctx.filesDir, "calls.json")
        if (file.exists()) runCatching {
            val arr = JSONArray(file.readText())
            _items.value = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                CallSummary(o.optString("number").ifBlank { null }, o.getLong("t"), o.getString("transcript"),
                    o.getString("summary"), o.getBoolean("callBack"), o.getString("reason"))
            }
        }
    }

    @Synchronized fun add(s: CallSummary) {
        _items.value = listOf(s) + _items.value
        val arr = JSONArray()
        _items.value.forEach {
            arr.put(JSONObject().put("number", it.number ?: "").put("t", it.startedAt)
                .put("transcript", it.transcript).put("summary", it.summary)
                .put("callBack", it.callBack).put("reason", it.reason))
        }
        file.writeText(arr.toString())
    }
}

object AgentPolicy {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("jarvis", Context.MODE_PRIVATE)
    fun enabled(ctx: Context) = prefs(ctx).getBoolean("agent_enabled", false)
    fun setEnabled(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean("agent_enabled", v).apply()
    fun ownerName(ctx: Context) = prefs(ctx).getString("owner", "the owner") ?: "the owner"
    fun setOwnerName(ctx: Context, v: String) = prefs(ctx).edit().putString("owner", v).apply()

    /** Only unknown numbers are answered by Jarvis; saved contacts ring normally. */
    fun shouldAutoAnswer(ctx: Context, number: String?) = enabled(ctx) && !Contacts.isKnown(ctx, number)
}
