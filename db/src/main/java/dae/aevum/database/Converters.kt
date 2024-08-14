package dae.aevum.database

import androidx.room.TypeConverter
import dae.aevum.utils.IssueId
import java.time.Instant

object Converters {
    @JvmStatic
    @TypeConverter
    fun fromIssueId(value: IssueId?): String? {
        return value?.value
    }

    @JvmStatic
    @TypeConverter
    fun toIssueId(value: String?): IssueId? {
        return value?.let { IssueId(it) }
    }

    @JvmStatic
    @TypeConverter
    fun fromInstant(value: Instant?): Long? {
        return value?.toEpochMilli()
    }

    @JvmStatic
    @TypeConverter
    fun toInstant(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }
}