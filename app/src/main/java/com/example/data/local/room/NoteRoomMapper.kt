package com.example.data.local.room

import com.example.data.model.Note
import com.example.data.model.NoteBlock
import com.example.data.model.NoteHistoryVersion
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Maps between domain Note models and Room NoteEntity models.
 */
object NoteRoomMapper {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val blocksType = Types.newParameterizedType(List::class.java, NoteBlock::class.java)
    private val blocksAdapter = moshi.adapter<List<NoteBlock>>(blocksType)

    private val tagsType = Types.newParameterizedType(List::class.java, String::class.java)
    private val tagsAdapter = moshi.adapter<List<String>>(tagsType)

    private val historyType = Types.newParameterizedType(List::class.java, NoteHistoryVersion::class.java)
    private val historyAdapter = moshi.adapter<List<NoteHistoryVersion>>(historyType)

    fun toEntity(note: Note, positionOrder: Int = 0): NoteEntity {
        return NoteEntity(
            id = note.id,
            title = note.title,
            contentJson = try { blocksAdapter.toJson(note.blocks) } catch (e: Exception) { "[]" },
            categoryId = note.categoryId,
            tagsJson = try { tagsAdapter.toJson(note.tags) } catch (e: Exception) { "[]" },
            tint = note.tint,
            font = note.font,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
            reminderEnabled = note.reminderEnabled,
            reminderAtEpochMillis = note.reminderAtEpochMillis,
            isPinned = note.isPinned,
            historyJson = try { historyAdapter.toJson(note.history) } catch (e: Exception) { "[]" },
            positionOrder = positionOrder
        )
    }

    fun toDomain(entity: NoteEntity): Note {
        val blocks = try {
            blocksAdapter.fromJson(entity.contentJson) ?: listOf(NoteBlock())
        } catch (e: Exception) {
            listOf(NoteBlock(content = entity.contentJson))
        }

        val tags = try {
            tagsAdapter.fromJson(entity.tagsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val history = try {
            historyAdapter.fromJson(entity.historyJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return Note(
            id = entity.id,
            title = entity.title,
            blocks = if (blocks.isNotEmpty()) blocks else listOf(NoteBlock()),
            categoryId = entity.categoryId,
            tags = tags,
            tint = entity.tint,
            font = entity.font,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            reminderEnabled = entity.reminderEnabled,
            reminderAtEpochMillis = entity.reminderAtEpochMillis,
            isPinned = entity.isPinned,
            history = history
        )
    }
}
