package com.markrogers.journal.data.db

import androidx.room.TypeConverter
import java.time.Instant

class Converters {
    @TypeConverter fun toInstant(epochSeconds: Long?): Instant? =
        epochSeconds?.let { Instant.ofEpochSecond(it) }

    @TypeConverter fun fromInstant(instant: Instant?): Long? =
        instant?.epochSecond
}
