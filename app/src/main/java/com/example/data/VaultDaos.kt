package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updated_at DESC")
    fun getAllNotesFlow(): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY updated_at DESC")
    suspend fun getAllNotes(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Int): Note?

    /**
     * High-Performance Instant Index Query.
     * Uses indexed `title` field for instant SQLite search.
     */
    @Query("SELECT * FROM notes WHERE title LIKE :query OR tags LIKE :query OR language LIKE :query ORDER BY updated_at DESC")
    fun searchNotesFlow(query: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int): Int
    
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY created_at DESC")
    fun getAllTasksFlow(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Query("UPDATE tasks SET is_completed = :isCompleted WHERE id = :id")
    suspend fun updateTaskStatus(id: Int, isCompleted: Boolean): Int

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int): Int
}

@Dao
interface NoteVersionDao {
    @Query("SELECT * FROM note_versions WHERE note_id = :noteId ORDER BY version_number DESC")
    suspend fun getVersionsForNote(noteId: Int): List<NoteVersion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: NoteVersion): Long

    @Query("SELECT COUNT(*) FROM note_versions WHERE note_id = :noteId")
    suspend fun getVersionCountForNote(noteId: Int): Int

    @Query("DELETE FROM note_versions WHERE note_id = :noteId")
    suspend fun deleteVersionsForNote(noteId: Int): Int
}
