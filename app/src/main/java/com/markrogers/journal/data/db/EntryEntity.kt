package com.markrogers.journal.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Storage model (Room). We keep it simple and map to your UI model.
 * - moodEmojis are stored as CSV (max 3)
 * - sleepMinutes keeps precision without floats
 */
@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val createdAt: Instant = Instant.now(),
    val title: String = "",
    val body: String = "",
    val moodRating: Int? = null,
    val moodEmojisCsv: String = "",
    val toggleX: Boolean = false,
    val toggleY: Boolean = false,
    val toggleZ: Boolean = false,
    val toggleW: Boolean = false,
    val sleepMinutes: Int? = null
)
