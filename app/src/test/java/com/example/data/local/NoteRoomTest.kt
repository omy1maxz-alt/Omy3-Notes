package com.example.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.room.AppDatabase
import com.example.data.local.room.NoteDao
import com.example.data.local.room.NoteEntity
import com.example.data.local.room.NoteRoomMapper
import com.example.data.model.BlockType
import com.example.data.model.Note
import com.example.data.model.NoteBlock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class NoteRoomTest {

    private lateinit var database: AppDatabase
    private lateinit var noteDao: NoteDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        noteDao = database.noteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveNote() = runBlocking {
        val noteEntity = NoteEntity(
            id = "note_test_1",
            title = "Test Note",
            contentJson = "[]",
            categoryId = "cat_work",
            tint = "mint",
            isPinned = true
        )

        noteDao.insertNote(noteEntity)

        val retrieved = noteDao.getNoteById("note_test_1")
        assertNotNull(retrieved)
        assertEquals("Test Note", retrieved?.title)
        assertEquals("cat_work", retrieved?.categoryId)
        assertEquals("mint", retrieved?.tint)
        assertTrue(retrieved?.isPinned == true)
    }

    @Test
    fun testGetAllNotesFlow() = runBlocking {
        val note1 = NoteEntity(id = "n1", title = "Note 1", isPinned = false, positionOrder = 1)
        val note2 = NoteEntity(id = "n2", title = "Pinned Note", isPinned = true, positionOrder = 0)

        noteDao.insertNotes(listOf(note1, note2))

        val all = noteDao.getAllNotes().first()
        assertEquals(2, all.size)
        // Pinned notes should come first
        assertEquals("Pinned Note", all[0].title)
        assertEquals("Note 1", all[1].title)
    }

    @Test
    fun testDeleteNote() = runBlocking {
        val note = NoteEntity(id = "del_1", title = "To Delete")
        noteDao.insertNote(note)
        assertEquals(1, noteDao.getNoteCount())

        noteDao.deleteNoteById("del_1")
        assertEquals(0, noteDao.getNoteCount())
    }

    @Test
    fun testNoteRoomMapper() {
        val domainNote = Note(
            id = UUID.randomUUID().toString(),
            title = "Domain Note",
            blocks = listOf(
                NoteBlock(type = BlockType.TEXT, content = "Hello Room!"),
                NoteBlock(type = BlockType.BULLET, content = "Item 1")
            ),
            categoryId = "cat_ideas",
            tags = listOf("room", "persistence"),
            tint = "sky",
            isPinned = true
        )

        val entity = NoteRoomMapper.toEntity(domainNote, positionOrder = 5)
        assertEquals(domainNote.id, entity.id)
        assertEquals("Domain Note", entity.title)
        assertEquals(5, entity.positionOrder)
        assertEquals("sky", entity.tint)

        val mappedBack = NoteRoomMapper.toDomain(entity)
        assertEquals(domainNote.id, mappedBack.id)
        assertEquals(domainNote.title, mappedBack.title)
        assertEquals(2, mappedBack.blocks.size)
        assertEquals("Hello Room!", mappedBack.blocks[0].content)
        assertEquals(BlockType.BULLET, mappedBack.blocks[1].type)
        assertEquals(listOf("room", "persistence"), mappedBack.tags)
    }
}
