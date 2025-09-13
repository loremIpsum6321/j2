package com.markrogers.journal.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.markrogers.journal.data.prefs.*
import com.markrogers.journal.data.repo.InMemoryRepository
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
fun SettingsScreen(repo: PreferencesRepository) {
    val prefs by repo.prefsFlow.collectAsState(initial = AppPrefs())
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    // ---- observe entries for export ----
    val entries by InMemoryRepository.entries.collectAsState()

    // ---- export/import launchers (Storage Access Framework) ----
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    // Use a DTO so we don’t depend on Room-specific fields
                    val payload = entries.map {
                        EntryJson(
                            createdAt = it.createdAt.toEpochMilli(),
                            title = it.title,
                            body = it.body,
                            moodEmojis = it.moodEmojis,
                            moodRating = it.moodRating,
                            toggleX = it.toggleX,
                            toggleY = it.toggleY,
                            toggleZ = it.toggleZ,
                            toggleW = it.toggleW,
                            sleepHours = it.sleepHours
                        )
                    }
                    val json = Gson().toJson(payload)
                    ctx.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(ctx, "Exported ${payload.size} entries", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: ""
                    val type = object : TypeToken<List<EntryJson>>() {}.type
                    val list: List<EntryJson> = Gson().fromJson(json, type) ?: emptyList()

                    // Append imported items
                    list.forEach { d ->
                        InMemoryRepository.restoreEntry(
                            com.markrogers.journal.data.model.JournalEntry(
                                id = 0L, // new row
                                createdAt = Instant.ofEpochMilli(d.createdAt),
                                title = d.title ?: "",
                                body = d.body ?: "",
                                moodEmojis = d.moodEmojis ?: emptyList(),
                                moodRating = d.moodRating,
                                toggleX = d.toggleX,
                                toggleY = d.toggleY,
                                toggleZ = d.toggleZ,
                                toggleW = d.toggleW,
                                sleepHours = d.sleepHours ?: 0f
                            )
                        )
                    }
                    Toast.makeText(ctx, "Imported ${list.size} entries", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.values().forEach { m ->
                FilterChip(
                    selected = prefs.theme == m,
                    onClick = { scope.launch { repo.setTheme(m) } },
                    label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
        Divider()

        Text("AI Provider", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(AiProvider.NONE, AiProvider.OPENAI, AiProvider.GEMINI).forEach { p ->
                FilterChip(
                    selected = prefs.provider == p,
                    onClick = { scope.launch { repo.setProvider(p) } },
                    label = { Text(p.name) }
                )
            }
        }
        OutlinedTextField(
            value = prefs.openAiKey,
            onValueChange = { v -> scope.launch { repo.setOpenAiKey(v) } },
            label = { Text("OpenAI API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = prefs.geminiKey,
            onValueChange = { v -> scope.launch { repo.setGeminiKey(v) } },
            label = { Text("Gemini API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Divider()

        Text("Security", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Require biometric to open")
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = prefs.requireBiometric,
                onCheckedChange = { v -> scope.launch { repo.setBiometric(v) } }
            )
        }

        Divider()

        // ---------------- Data section (Export / Import / Clear all) ----------------
        Text("Data", style = MaterialTheme.typography.titleMedium)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    val defaultName = "journal_export_${System.currentTimeMillis()}.json"
                    exportLauncher.launch(defaultName)
                },
                modifier = Modifier.weight(1f)
            ) { Text("Export JSON") }

            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier.weight(1f)
            ) { Text("Import JSON") }
        }

        OutlinedButton(
            onClick = { showClearConfirm = true },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Clear all data") }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Delete all data?") },
            text = { Text("This will permanently remove all entries on this device.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    scope.launch { InMemoryRepository.clearAll() }
                    Toast.makeText(ctx, "All data cleared", Toast.LENGTH_SHORT).show()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

/** Lightweight export/import shape (stable, not tied to Room’s internal entity). */
private data class EntryJson(
    val createdAt: Long,
    val title: String? = null,
    val body: String? = null,
    val moodEmojis: List<String>? = null,
    val moodRating: Int? = null,
    val toggleX: Boolean = false,
    val toggleY: Boolean = false,
    val toggleZ: Boolean = false,
    val toggleW: Boolean = false,
    val sleepHours: Float? = 0f
)
