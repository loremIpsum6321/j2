package com.markrogers.journal.ui.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markrogers.journal.data.prefs.AppPrefs
import com.markrogers.journal.data.prefs.PreferencesRepository
import com.markrogers.journal.data.repo.InMemoryRepository
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import java.time.LocalDate
import java.time.ZoneId


// Keep these in sync with Metrics colors
private val colorX = Color(0xFF6EE7B7) // green
private val colorY = Color(0xFFF8D477) // yellow
private val colorZ = Color(0xFFFF6B6B) // red
private val colorW = Color(0xFF60A5FA) // blue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefsRepo = remember(context) { PreferencesRepository(context) }
    val prefs by prefsRepo.prefsFlow.collectAsState(initial = AppPrefs())

    // Quick-emoji slots (from DataStore)
    var quick by remember(prefs.quickEmojis) { mutableStateOf(prefs.quickEmojis) }
    // Multi-select mood (up to 3; oldest evicted)
    var moods by remember { mutableStateOf<List<String>>(emptyList()) }

    var title by remember { mutableStateOf(TextFieldValue("")) }
    var body by remember { mutableStateOf(TextFieldValue("")) }
    var x by remember { mutableStateOf(false) }
    var y by remember { mutableStateOf(false) }
    var z by remember { mutableStateOf(false) }
    var w by remember { mutableStateOf(false) } // fourth toggle


    var showPickerFor by remember { mutableStateOf<Int?>(null) }
    // All entries so we can look up today's sleep value
    val allEntries by InMemoryRepository.entries.collectAsState()

    // Prefill sleep with today's latest non-zero value (if any)
    val today = remember { LocalDate.now() }
    val todaysSleep = remember(allEntries, today) {
        allEntries
            .asSequence()
            .filter { it.createdAt.atZone(ZoneId.systemDefault()).toLocalDate() == today }
            .sortedBy { it.createdAt }     // oldest -> newest
            .map { it.sleepHours }
            .filter { it > 0f }
            .lastOrNull()
    }

    // <-- This replaces the old sleep declaration
    var sleep by remember(todaysSleep) { mutableStateOf(todaysSleep ?: 7f) }

    Text(
        if (todaysSleep != null) "Sleep is per-day. Editing updates today’s value."
        else "Set last night’s sleep (applies to today).",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Entry") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        InMemoryRepository.addEntry(
                            title = title.text,
                            body = body.text,
                            moodEmojis = moods,
                            moodRating = moodRatingFromEmojis(moods),
                            toggleX = x,
                            toggleY = y,
                            toggleZ = z,
                            toggleW = w,
                            sleepHours = sleep
                        )
                        onBack()
                    }) { Text("Save") }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- Emoji row (centered + evenly spaced) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                quick.forEachIndexed { idx, e ->
                    QuickEmojiChip(
                        emoji = if (e.isNotBlank()) e else "🙂",
                        selected = moods.contains(e),
                        onClick = { moods = toggleEmoji(moods, e, limit = 3) },
                        onLongPress = { showPickerFor = idx }
                    )
                }
                ClearChip(
                    enabled = moods.isNotEmpty(),
                    onClear = { moods = emptyList() }
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // --- Body: fixed height, internal scroll ---
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Body") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp), // fixed height; internal scroll
                minLines = 6
            )

            // --- Toggles (keep colors mapped to metrics balls) ---
            Text("Toggles", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Switch(
                    checked = x, onCheckedChange = { x = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colorX,
                        checkedTrackColor = colorX.copy(alpha = 0.45f)
                    )
                )
                Switch(
                    checked = y, onCheckedChange = { y = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colorY,
                        checkedTrackColor = colorY.copy(alpha = 0.45f)
                    )
                )
                Switch(
                    checked = z, onCheckedChange = { z = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colorZ,
                        checkedTrackColor = colorZ.copy(alpha = 0.45f)
                    )
                )
                Switch(
                    checked = w, onCheckedChange = { w = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colorW,
                        checkedTrackColor = colorW.copy(alpha = 0.45f)
                    )
                )
            }

            Spacer(Modifier.height(20.dp))

            Text("Sleep hours: ${sleep.toInt()}h")

            // --- Thicker, glowing slider ---
            GlowingSlider(
                value = sleep,
                onValueChange = { sleep = it },
                valueRange = 0f..14f,
                steps = 14,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            )

            Spacer(Modifier.height(28.dp)) // move slider a bit lower from bottom bar
        }
    }

    // Emoji picker dialog for replacing a quick slot
    if (showPickerFor != null) {
        AlertDialog(
            onDismissRequest = { showPickerFor = null },
            confirmButton = {},
            text = {
                Column {
                    Text("Pick emoji for slot ${showPickerFor!! + 1}")
                    // very small grid without extra deps
                    FlowRowEmojis(
                        choices = emojiChoices,
                        onPick = { e ->
                            val idx = showPickerFor!!
                            val updated = quick.toMutableList().apply { this[idx] = e }
                            quick = updated
                            scope.launch { prefsRepo.setQuickEmoji(idx, e) }
                            showPickerFor = null
                        }
                    )
                }
            }
        )
    }
}

/* ---------- Small helper for a compact emoji grid in the dialog ---------- */
@Composable
private fun FlowRowEmojis(
    choices: List<String>,
    onPick: (String) -> Unit
) {
    val perRow = 6
    val rows = remember(choices) { choices.chunked(perRow) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { e ->
                    TextButton(onClick = { onPick(e) }) {
                        Text(e, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
    }
}

/* ---------- Fancy glowing slider (drawn under a normal Slider) ---------- */
@Composable
private fun GlowingSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        // Glow track behind
        Canvas(modifier = Modifier
            .matchParentSize()
            .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            // Convert 10.dp to px using the current density (DrawScope is a Density)
            val h = 10f * density   // same as 10.dp in pixels
            val r = h / 2
            // soft "glow" underlay (wide, transparent)
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF7C6BFF).copy(alpha = 0.25f),
                        Color(0xFF05D2FF).copy(alpha = 0.25f)
                    )
                ),
                topLeft = Offset(0f, center.y - h / 2),
                size = Size(size.width, h),
                cornerRadius = CornerRadius(r, r)
            )
            // bright core
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF7C6BFF), Color(0xFF05D2FF))
                ),
                topLeft = Offset(0f, center.y - (h * 0.35f)),
                size = Size(size.width, h * 0.7f),
                cornerRadius = CornerRadius(r, r),
                style = Stroke(width = h * 0.7f)
            )
        }

        // Actual Slider (tracks transparent so our custom track shows through)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                thumbColor = Color.White
            )
        )
    }
}

// ----- Helpers & UI pieces -----

private val emojiChoices = listOf(
    // Connected / positive
    "🙂","😀","😄","😊","🤗","😌","😎","🥰","😍","🥹","🤩","😇",
    // Neutral / reflective
    "😐","😶","😑","🤔","😏","🥲","🫨","😳",
    // Distressed
    "😔","😞","🥺","😣","😩","🙁","😢","😭",
    // Anxious / fearful
    "😬","😟","😰","😱","😨","😥",
    // Overwhelmed / irritable / angry
    "😫","😵‍💫","🤯","😒","😤","😠","😡","🤬",
    // misc
    "🔥","💪","💋","😘","🥰","😍","💘","💖","💞","💕","😉","😏","😈","🤤","🥵","🫦"
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickEmojiChip(
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val shape: Shape = MaterialTheme.shapes.large
    Surface(
        tonalElevation = if (selected) 4.dp else 0.dp,
        shape = shape,
        modifier = Modifier
            .shadow(if (selected) 8.dp else 0.dp, shape, clip = false)
            .border(
                width = if (selected) 2.dp else 1.dp,
                brush = SolidColor(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                shape = shape
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
    }
}

/** Matches the emoji chips; disabled/greyed until there's something to clear. */
@Composable
private fun ClearChip(
    enabled: Boolean,
    onClear: () -> Unit
) {
    val shape: Shape = MaterialTheme.shapes.large
    val borderColor =
        if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val xColor =
        if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    Surface(
        tonalElevation = 0.dp,
        shape = shape,
        modifier = Modifier
            .border(width = 1.dp, brush = SolidColor(borderColor), shape = shape)
            .then(if (enabled) Modifier.clickable(onClick = onClear) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text("❌", color = xColor, style = MaterialTheme.typography.titleLarge)
    }
}

/** Toggle/evict with max size. */
private fun toggleEmoji(current: List<String>, emoji: String, limit: Int): List<String> {
    if (current.contains(emoji)) return current.filterNot { it == emoji }
    val appended = current + emoji
    return if (appended.size <= limit) appended else appended.drop(appended.size - limit)
}

/** Very simple mood rating heuristic based on the first (primary) emoji. */
private fun moodRatingFromEmojis(list: List<String>): Int? {
    val e = list.firstOrNull() ?: return null
    return when (e) {
        "😀", "🤩" -> 5
        "🙂", "😎" -> 4
        "😐" -> 3
        "🙁", "😤" -> 2
        "😢", "😵" -> 1
        else -> null
    }
}
