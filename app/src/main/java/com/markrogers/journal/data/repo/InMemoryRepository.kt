package com.markrogers.journal.data.repo

import android.content.Context
import com.markrogers.journal.data.db.AppDatabase
import com.markrogers.journal.data.db.JournalRepository
import com.markrogers.journal.data.model.JournalEntry
import com.markrogers.journal.data.model.TodoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * Room-backed facade the UI already uses.
 * - Call initialize(appContext) once (done from AppRoot).
 * - UI reads [entries].
 * - Calendar/Timeline helpers return your app's models.
 */
object InMemoryRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var repo: JournalRepository

    private val _entries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val entries: StateFlow<List<JournalEntry>> = _entries

    @Volatile private var initialized = false

    fun initialize(appContext: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val dao = AppDatabase.get(appContext).journalDao()
            repo = JournalRepository(dao)
            scope.launch {
                repo.observeAll().collectLatest { list ->
                    _entries.value = list
                }
            }
            initialized = true
        }
    }

    fun addEntry(
        title: String,
        body: String,
        moodEmojis: List<String>,
        moodRating: Int?,
        toggleX: Boolean,
        toggleY: Boolean,
        toggleZ: Boolean,
        toggleW: Boolean,
        sleepHours: Float
    ) {
        scope.launch {
            repo.upsert(
                JournalEntry(
                    id = 0L, // 0L => auto-generate in Room
                    createdAt = Instant.now(),
                    title = title,
                    body = body,
                    moodEmojis = moodEmojis,
                    moodRating = moodRating,
                    toggleX = toggleX,
                    toggleY = toggleY,
                    toggleZ = toggleZ,
                    toggleW = toggleW,
                    sleepHours = sleepHours
                )
            )
        }
    }

    fun clearAll() {
        scope.launch { repo.clearAll() }
    }

    /** Permanently deletes an entry by id (used by Timeline swipe-to-dismiss). */
    fun deleteEntry(id: Long) {
        scope.launch { repo.deleteById(id) }
    }

    /**
     * Restores an entry after delete (Snackbar “Undo”).
     * Reinserts the same content; using id = 0L so Room auto-generates a new PK.
     * The original createdAt is preserved.
     */
    fun restoreEntry(e: JournalEntry) {
        scope.launch {
            repo.upsert(e.copy(id = 0L))
        }
    }

    /** Demo data generator (used by Metrics) */
    fun generateDummy(start: LocalDate, end: LocalDate) {
        scope.launch {
            val days = generateSequence(start) { it.plusDays(1) }
                .takeWhile { !it.isAfter(end) }
                .toList()

            val zone = ZoneId.systemDefault()
            val list = days.map { d ->
                val hrs = 4.5f + Random.nextFloat() * 4.5f
                val created = d.atStartOfDay(zone).toInstant()
                JournalEntry(
                    id = 0L,
                    createdAt = created,
                    title = "",
                    body = "",
                    moodEmojis = emptyList(),
                    moodRating = listOf(1, 2, 3, 4, 5).random(),
                    toggleX = Random.nextBoolean(),
                    toggleY = Random.nextBoolean(),
                    toggleZ = Random.nextBoolean(),
                    toggleW = Random.nextBoolean(),
                    sleepHours = hrs
                )
            }
            repo.upsertAll(list)
        }
    }

    // -------------------- Calendar / Timeline helpers (use your app models) --------------------

    /**
     * Returns todos for a given date using your TodoItem model.
     * Rule: any entry with a non-blank title is considered a todo; toggleX = done.
     */
    fun todosOn(date: LocalDate): List<TodoItem> {
        val zone = ZoneId.systemDefault()
        return _entries.value
            .filter { it.createdAt.atZone(zone).toLocalDate() == date && it.title.isNotBlank() }
            .map { e ->
                TodoItem(
                    id = e.id,
                    date = date,
                    text = e.title,
                    done = e.toggleX
                )
            }
    }

    /** Adds a todo on the specified date (stored as a JournalEntry). */
    fun addTodo(date: LocalDate, text: String) {
        val created = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        scope.launch {
            repo.upsert(
                JournalEntry(
                    id = 0L,
                    createdAt = created,
                    title = text,
                    body = "",
                    moodEmojis = emptyList(),
                    moodRating = null,
                    toggleX = false, // not done yet
                    toggleY = false,
                    toggleZ = false,
                    toggleW = false,
                    sleepHours = 0f
                )
            )
        }
    }

    /** Toggles the todo's done state (maps to toggleX). */
    fun toggleTodo(id: Long) {
        val current = _entries.value.firstOrNull { it.id == id } ?: return
        scope.launch {
            repo.upsert(current.copy(toggleX = !current.toggleX))
        }
    }

    // -------------------------------------------------------------------------------
}
