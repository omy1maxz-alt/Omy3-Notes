package com.example.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room Entity representing a Note in local SQLite database storage.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "title")
    val title: String = "",

    @ColumnInfo(name = "content_json")
    val contentJson: String = "[]",

    @ColumnInfo(name = "category_id")
    val categoryId: String? = null,

    @ColumnInfo(name = "tags_json")
    val tagsJson: String = "[]",

    @ColumnInfo(name = "tint")
    val tint: String = "cream",

    @ColumnInfo(name = "font")
    val font: String = "sans",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "reminder_enabled")
    val reminderEnabled: Boolean = false,

    @ColumnInfo(name = "reminder_at_epoch_millis")
    val reminderAtEpochMillis: Long? = null,

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "history_json")
    val historyJson: String = "[]",

    @ColumnInfo(name = "position_order")
    val positionOrder: Int = 0
)
