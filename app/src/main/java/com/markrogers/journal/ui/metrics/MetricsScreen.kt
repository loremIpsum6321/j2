package com.markrogers.journal.ui.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markrogers.journal.data.model.JournalEntry
import com.markrogers.journal.data.repo.InMemoryRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsScreen() {
    val entries by InMemoryRepository.entries.collectAsState()

    // Date range (initialized from DB once)
    var start by remember { mutableStateOf<LocalDate?>(null) }
    var end by remember { mutableStateOf<LocalDate?>(null) }

    // Compact mode: remove blank days (default ON)
    var compact by remember { mutableStateOf(true) }

    // Tab
    var tab by remember { mutableStateOf(ChartTab.Sleep) }

    // Initialize range once from DB (first entry -> latest entry)
    LaunchedEffect(entries) {
        if (start == null || end == null) {
            if (entries.isNotEmpty()) {
                val dates = entries.map { it.createdAt.atZone(ZoneId.systemDefault()).toLocalDate() }
                start = dates.minOrNull()
                end = dates.maxOrNull()
            } else {
                // fallback if there are no entries yet
                start = LocalDate.now().minusDays(14)
                end = LocalDate.now()
            }
        }
    }

    // Guard until we have a range
    val s = start ?: return
    val e = end ?: return

    val days = remember(s, e, entries, compact) {
        aggregateDays(entries, s, e, compact)
    }

    // Date range picker state
    var showRangeDialog by remember { mutableStateOf(false) }
    val rangeState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = s.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        initialSelectedEndDateMillis = e.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    if (showRangeDialog) {
        DatePickerDialog(
            onDismissRequest = { showRangeDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val selS = rangeState.selectedStartDateMillis
                    val selE = rangeState.selectedEndDateMillis
                    if (selS != null && selE != null) {
                        start = Instant.ofEpochMilli(selS).atZone(ZoneId.systemDefault()).toLocalDate()
                        end = Instant.ofEpochMilli(selE).atZone(ZoneId.systemDefault()).toLocalDate().minusDays(1)
                    }
                    showRangeDialog = false
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showRangeDialog = false }) { Text("Cancel") } }
        ) {
            DateRangePicker(state = rangeState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SegmentedTabs(
            options = listOf(
                SegOpt("Mood", ChartTab.Mood),
                SegOpt("Sleep", ChartTab.Sleep),
                SegOpt("Totals", ChartTab.Hist)
            ),
            selected = tab,
            onSelected = { tab = it }
        )

        // Header chips: range & compact toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateChip(label = "Start", date = s) { showRangeDialog = true }
            DateChip(label = "End", date = e) { showRangeDialog = true }

            Spacer(Modifier.weight(1f))

            FilterChip(
                selected = compact,
                onClick = { compact = !compact },
                label = { Text(if (compact) "❌" else "All") }
            )
        }

        GlowCard(title = "Sleep hours") {
            UnifiedChart(days = days, mode = tab)
        }
    }
}

/* ---------- unified chart (mood+sleep+totals) ---------- */

private enum class ChartTab { Mood, Sleep, Hist }
private data class SegOpt(val label: String, val tab: ChartTab)

@Composable
private fun UnifiedChart(
    days: List<DayAgg>,
    mode: ChartTab,
    height: Dp = 260.dp
) {
    val shape = RoundedCornerShape(24.dp)
    val outline = remember { Brush.linearGradient(listOf(Color(0xFF6C63FF), Color(0xFF00C8FF))) }
    val labelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(160, 220, 230, 255)
            textSize = 26f
            isAntiAlias = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, outline, shape)
            .padding(20.dp)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (days.isEmpty()) return@Canvas

            val w = size.width
            val h = size.height
            val left = 8f
            val right = w - 8f
            val top = 8f
            val bottom = h - 28f

            val stepX = (right - left) / max(1, days.size - 1)
            val xs = days.indices.map { i -> left + i * stepX }

            if (mode != ChartTab.Hist) {
                val counts = days.map { it.cX + it.cY + it.cZ + it.cW }
                val maxCount = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
                val desiredBarW = min(36f, stepX * .55f)
                val maxAllowedDiameter = (bottom - top) / maxCount
                val barW = min(desiredBarW, maxAllowedDiameter)

                val barBrush = Brush.verticalGradient(
                    0f to Color(0xFF7E6AF4).copy(alpha = .38f),
                    1f to Color(0xFF0EA5E9).copy(alpha = .38f)
                )

                days.forEachIndexed { i, _ ->
                    val total = counts[i]
                    if (total <= 0) return@forEachIndexed
                    val barHeight = barW * total
                    val x = xs[i] - barW / 2f
                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(x, bottom - barHeight),
                        size = Size(barW, barHeight),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                }
            }

            // mood & sleep lines
            val maxSleepForLine = max(8f, days.maxOf { it.sleep })
            val minSleepForLine = 0f

            val moodPoints = days.mapIndexedNotNull { i, d ->
                d.mood?.let { m ->
                    val y = bottom - ((m - 1f) / 4f).coerceIn(0f, 1f) * (bottom - top)
                    Offset(xs[i], y)
                }
            }
            val sleepPoints = days.mapIndexed { i, d ->
                val y = bottom - ((d.sleep - minSleepForLine) / (maxSleepForLine - minSleepForLine)).coerceIn(0f, 1f) * (bottom - top)
                Offset(xs[i], y)
            }
            val moodPath = smoothPath(moodPoints)
            val sleepPath = smoothPath(sleepPoints)

            fun DrawScope.line(path: Path, color: Color, dominant: Boolean) {
                val glow = if (dominant) 0.90f else 0.35f
                val width = if (dominant) 6f else 4f
                drawPath(path = path, color = color.copy(alpha = glow), style = Stroke(width = width))
                val pts = if (color == graphPurple) moodPoints else sleepPoints
                pts.forEach {
                    drawCircle(color.copy(alpha = glow), radius = if (dominant) 5.5f else 4f, center = it)
                    drawCircle(Color.Black.copy(alpha = 0.6f), radius = 1.5f, center = it, style = Stroke(1.2f))
                }
            }

            when (mode) {
                ChartTab.Mood -> {
                    line(sleepPath, graphCyan, dominant = false)
                    line(moodPath, graphPurple, dominant = true)
                }
                ChartTab.Sleep -> {
                    line(moodPath, graphPurple, dominant = false)
                    line(sleepPath, graphCyan, dominant = true)
                }
                ChartTab.Hist -> {
                    drawTotalsStacksExact(days = days, xs = xs, top = top, bottom = bottom, stepX = stepX)
                    line(moodPath, graphPurple, dominant = false)
                    line(sleepPath, graphCyan, dominant = false)
                }
            }

            // X labels (thinned)
            val labFmt = DateTimeFormatter.ofPattern("M/d")
            val stride = max(1, days.size / 8)
            days.forEachIndexed { i, d ->
                if (i % stride == 0) {
                    drawIntoCanvas { cnv ->
                        cnv.nativeCanvas.drawText(
                            d.date.format(labFmt),
                            xs[i] - 18f,
                            h - 4f,
                            labelPaint
                        )
                    }
                }
            }
        }
    }
}

/**
 * Totals view:
 *  • ballDiameter == barWidth (clamped so tallest stack fits exactly)
 *  • balls have ZERO vertical spacing
 *  • outline height == ballDiameter * (cX+cY+cZ+cW)
 */
private fun DrawScope.drawTotalsStacksExact(
    days: List<DayAgg>,
    xs: List<Float>,
    top: Float,
    bottom: Float,
    stepX: Float
) {
    val counts = days.map { it.cX + it.cY + it.cZ + it.cW }
    val maxCount = counts.maxOrNull()?.coerceAtLeast(1) ?: 1

    val desiredBarW = min(36f, stepX * .55f)
    val maxAllowedDiameter = (bottom - top) / maxCount
    val ballDiameter = min(desiredBarW, maxAllowedDiameter)
    val radius = ballDiameter / 2f
    val barW = ballDiameter
    val corner = CornerRadius(radius, radius)

    days.forEachIndexed { i, d ->
        val cx = xs[i]
        val total = counts[i]
        if (total <= 0) return@forEachIndexed

        val barHeight = ballDiameter * total
        val left = cx - barW / 2f

        drawRoundRect(
            color = Color(0xFF9EB8FF).copy(alpha = 0.25f),
            topLeft = Offset(left, bottom - barHeight),
            size = Size(barW, barHeight),
            cornerRadius = corner,
            style = Stroke(width = 1.25f)
        )

        var k = 0
        fun put(n: Int, color: Color) {
            repeat(n) {
                val cy = bottom - (k + 0.5f) * ballDiameter
                drawCircle(color = color, radius = radius * 0.95f, center = Offset(cx, cy))
                k++
            }
        }
        put(d.cW, colorW)
        put(d.cZ, colorZ)
        put(d.cY, colorY)
        put(d.cX, colorX)
    }
}

/* ---------- aggregation ---------- */

private data class DayAgg(
    val date: LocalDate,
    val sleep: Float,
    val mood: Float?,
    val cX: Int, val cY: Int, val cZ: Int, val cW: Int
)

/**
 * If compact = true, we only include dates that have entries (no blanks).
 * If compact = false, we include every day from start..end.
 * Sleep is "latest non-zero per day" to avoid multiple-entry skew.
 */
private fun aggregateDays(
    entries: List<JournalEntry>,
    start: LocalDate,
    end: LocalDate,
    compact: Boolean
): List<DayAgg> {
    if (end.isBefore(start)) return emptyList()

    val grouped = entries.groupBy { it.createdAt.atZone(ZoneId.systemDefault()).toLocalDate() }

    val dates: List<LocalDate> = if (compact) {
        grouped.keys.filter { !it.isBefore(start) && !it.isAfter(end) }.sorted()
    } else {
        generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .toList()
    }

    fun latestNonZeroSleep(list: List<JournalEntry>): Float {
        return list.asSequence()
            .sortedBy { it.createdAt }
            .map { it.sleepHours }
            .filter { it > 0f }
            .lastOrNull() ?: 0f
    }

    return dates.map { d ->
        val list = grouped[d].orEmpty()
        val sleepForDay = latestNonZeroSleep(list)
        val moodAvg = list.mapNotNull { it.moodRating?.toFloat() }
            .let { if (it.isEmpty()) null else it.average().toFloat() }
        DayAgg(
            date = d,
            sleep = sleepForDay,
            mood = moodAvg,
            cX = list.count { it.toggleX },
            cY = list.count { it.toggleY },
            cZ = list.count { it.toggleZ },
            cW = list.count { it.toggleW }
        )
    }
}

/* ---------- UI bits ---------- */

private val graphPurple = Color(0xFF9A7BFF)
private val graphCyan = Color(0xFF00E6FF)

private val colorX = Color(0xFF6EE7B7)
private val colorY = Color(0xFFF8D477)
private val colorZ = Color(0xFFFF6B6B)
private val colorW = Color(0xFF60A5FA)

@Composable
private fun SegmentedTabs(
    options: List<SegOpt>,
    selected: ChartTab,
    onSelected: (ChartTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(Color(0xFF7C6BFF), Color(0xFF05D2FF))),
                RoundedCornerShape(28.dp)
            )
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        options.forEach { opt ->
            val selectedNow = opt.tab == selected
            val bg = if (selectedNow) Brush.horizontalGradient(
                listOf(Color(0xFF7C6BFF), Color(0xFF05D2FF))
            ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            Surface(
                shape = RoundedCornerShape(22.dp),
                tonalElevation = if (selectedNow) 6.dp else 0.dp,
                modifier = Modifier
                    .height(40.dp)
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = bg, shape = RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp)
                        .clickable { onSelected(opt.tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selectedNow) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (selectedNow) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun GlowCard(
    title: String,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    val border = Brush.linearGradient(listOf(Color(0xFF7C6BFF), Color(0xFF05D2FF)))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, border, shape)
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun DateChip(
    label: String,
    date: LocalDate,
    onPick: () -> Unit
) {
    AssistChip(
        onClick = onPick,
        label = {
            val fmt = DateTimeFormatter.ISO_DATE
            Text("$label: ${date.format(fmt)}")
        }
    )
}

/* ---------- path helper ---------- */

private fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        val mid = Offset((prev.x + curr.x) * 0.5f, (prev.y + curr.y) * 0.5f)
        path.quadraticBezierTo(prev.x, prev.y, mid.x, mid.y)
    }
    path.lineTo(points.last().x, points.last().y)
    return path
}
