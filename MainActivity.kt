package com.jarvis.assistant

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jarvis.assistant.telecom.AgentPolicy
import com.jarvis.assistant.telecom.CallStore
import com.jarvis.assistant.wake.WakeWordService
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private var tick by mutableIntStateOf(0)

    private val runtimePerms = arrayOf(
        Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE, Manifest.permission.ANSWER_PHONE_CALLS,
        Manifest.permission.READ_CALL_LOG, Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.BLUETOOTH_CONNECT
    )
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }
    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { tick++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { Screen(tick) } } }
    }

    override fun onResume() { super.onResume(); tick++ }

    private data class Item(val title: String, val why: String, val granted: Boolean, val onGrant: () -> Unit)

    private fun requestRole(role: String) {
        val rm = getSystemService(RoleManager::class.java)
        if (rm.isRoleAvailable(role) && !rm.isRoleHeld(role)) roleLauncher.launch(rm.createRequestRoleIntent(role))
    }

    private fun items(): List<Item> {
        val rm = getSystemService(RoleManager::class.java)
        val a11yList = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return listOf(
            Item("Microphone, contacts, phone, notifications", "Wake word, calling, caller lookup",
                runtimePerms.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
            ) { permLauncher.launch(runtimePerms) },
            Item("Accessibility service", "Tap, scroll, toggle settings, press Send",
                a11yList.contains(packageName) && a11yList.contains("JarvisAccessibilityService")
            ) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            Item("Display over other apps", "Lets Jarvis open apps from the background",
                Settings.canDrawOverlays(this)
            ) { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) },
            Item("Modify system settings", "Brightness control",
                Settings.System.canWrite(this)
            ) { startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))) },
            Item("Call screening app", "Silences unknown callers before they ring",
                rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            ) { requestRole(RoleManager.ROLE_CALL_SCREENING) },
            Item("Default phone app", "Needed to answer calls on your behalf",
                rm.isRoleHeld(RoleManager.ROLE_DIALER)
            ) { requestRole(RoleManager.ROLE_DIALER) }
        )
    }

    @Composable
    private fun Screen(tick: Int) {
        val list = remember(tick) { items() }
        val calls by CallStore.items.collectAsState()
        var listening by remember { mutableStateOf(false) }
        var agent by remember { mutableStateOf(AgentPolicy.enabled(this)) }
        var owner by remember { mutableStateOf(AgentPolicy.ownerName(this).let { if (it == "the owner") "" else it }) }

        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Jarvis", style = MaterialTheme.typography.headlineMedium) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Listen for \"Jarvis\"", Modifier.weight(1f))
                    Switch(listening, {
                        listening = it
                        if (it) WakeWordService.start(this@MainActivity) else WakeWordService.stop(this@MainActivity)
                    })
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Answer unknown callers", Modifier.weight(1f))
                    Switch(agent, { agent = it; AgentPolicy.setEnabled(this@MainActivity, it) })
                }
            }
            item {
                OutlinedTextField(owner, { owner = it; AgentPolicy.setOwnerName(this@MainActivity, it.ifBlank { "the owner" }) },
                    label = { Text("Your name (spoken to callers)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            item { Text("Setup", style = MaterialTheme.typography.titleMedium) }
            items(list) { i ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(i.title, style = MaterialTheme.typography.bodyLarge)
                            Text(i.why, style = MaterialTheme.typography.bodySmall)
                        }
                        if (i.granted) Text("Granted") else Button(onClick = i.onGrant) { Text("Grant") }
                    }
                }
            }
            item { Text("Recent calls", style = MaterialTheme.typography.titleMedium) }
            if (calls.isEmpty()) item { Text("No screened calls yet.") }
            items(calls) { c ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${c.number ?: "Unknown"} · ${DateFormat.getDateTimeInstance().format(Date(c.startedAt))}",
                            style = MaterialTheme.typography.labelMedium)
                        Text(c.summary)
                        Text(if (c.callBack) "Call back: ${c.reason}" else "Skip: ${c.reason}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
