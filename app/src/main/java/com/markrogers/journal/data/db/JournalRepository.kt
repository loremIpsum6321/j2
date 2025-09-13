package com.markrogers.journal.data.db

import com.markrogers.journal.data.model.JournalEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import kotlin.math.roundToInt

private fun List<String>.toCsv(): String = joinToString(",")
private fun String.fromCsv(): List<String> =
    if (isBlank()) emptyList() else split(",").map { it.trim() }

private fun EntryEntity.toUi(): JournalEntry =
    JournalEntry(
        id = id,
        createdAt = createdAt,
        title = title,
        body = body,
        moodRating = moodRating,
        moodEmojis = moodEmojisCsv.fromCsv(),
        toggleX = toggleX,
        toggleY = toggleY,
        toggleZ = toggleZ,
        toggleW = toggleW,
        sleepHours = sleepMinutes?.let { it / 60f } ?: 0f
    )

private fun JournalEntry.toEntity(): EntryEntity =
    EntryEntity(
        id = id ?: 0L,
        createdAt = createdAt ?: Instant.now(),
        title = title.orEmpty(),
        body = body.orEmpty(),
        moodRating = moodRating,
        moodEmojisCsv = moodEmojis.toCsv(),
        toggleX = toggleX,
        toggleY = toggleY,
        toggleZ = toggleZ,
        toggleW = toggleW,
        sleepMinutes = if (sleepHours > 0f) (sleepHours * 60).roundToInt() else null
    )

class JournalRepository(private val dao: JournalDao) {
    fun observeAll(): Flow<List<JournalEntry>> =
        dao.observeAll().map { list -> list.map { it.toUi() } }

    suspend fun upsert(entry: JournalEntry) =
        dao.upsert(entry.toEntity())

    suspend fun upsertAll(entries: List<JournalEntry>) =
        dao.upsertAll(entries.map { it.toEntity() })

    suspend fun clearAll() = dao.clearAll()
    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun getAllOnce(): List<JournalEntry> = dao.getAllOnce().map { it.toUi() }
}
