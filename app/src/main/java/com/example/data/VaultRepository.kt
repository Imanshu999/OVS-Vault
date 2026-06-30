package com.example.data

import android.util.Base64
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class VaultRepository(private val db: VaultDatabase) {
    private val userDao = db.userDao()
    private val noteDao = db.noteDao()
    private val taskDao = db.taskDao()
    private val noteVersionDao = db.noteVersionDao()

    // Key session state managed in VM, but repository provides helper functions.

    // --- User Security & Keys ---

    /**
     * Hashes a password with SHA-256 + Salt for storage verification.
     */
    private fun hashPassword(password: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.reset()
        digest.update(salt)
        val hashedBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashedBytes, Base64.NO_WRAP)
    }

    /**
     * Registers a user with a master password.
     */
    suspend fun registerUser(username: String, masterPassword: CharArray): Boolean {
        val existing = userDao.getUserByUsername(username)
        if (existing != null) return false // Already exists
        
        val saltBytes = VaultCrypto.generateSalt()
        val saltBase64 = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        
        // Hash for database verification (not for encryption key)
        val passwordHash = hashPassword(String(masterPassword), saltBytes)
        
        val newUser = User(
            username = username,
            passwordHash = passwordHash,
            salt = saltBase64
        )
        userDao.insertUser(newUser)
        return true
    }

    /**
     * Authenticates a user and returns their PBKDF2-derived AES-256 SecretKey.
     * Returns null if authentication fails.
     */
    suspend fun authenticateUser(username: String, masterPassword: CharArray): SecretKey? {
        val user = userDao.getUserByUsername(username) ?: return null
        val saltBytes = Base64.decode(user.salt, Base64.NO_WRAP)
        
        // Verify storage hash matches
        val inputHash = hashPassword(String(masterPassword), saltBytes)
        if (inputHash != user.passwordHash) {
            return null
        }
        
        // Derive AES key using PBKDF2 with the user's unique salt
        return VaultCrypto.deriveKey(masterPassword, saltBytes)
    }

    /**
     * Check if any user is registered (if empty, we show Setup screen).
     */
    suspend fun isFirstRun(): Boolean {
        // Simple heuristic: if we don't find any users, it is first run
        return userDao.getUserByUsername("admin") == null
    }

    // --- Note Operations with Encryption ---

    val allNotesFlow: Flow<List<Note>> = noteDao.getAllNotesFlow()

    suspend fun getAllNotes(): List<Note> = noteDao.getAllNotes()

    fun searchNotesFlow(query: String): Flow<List<Note>> {
        val wildcardQuery = "%$query%"
        return noteDao.searchNotesFlow(wildcardQuery)
    }

    /**
     * Encrypts and inserts/updates a note.
     */
    suspend fun insertEncryptedNote(
        id: Int = 0,
        title: String,
        plainContent: String,
        tags: String,
        language: String,
        sessionKey: SecretKey
    ): Long {
        if (id != 0) {
            // Archive the previous version before editing
            val existingNote = noteDao.getNoteById(id)
            if (existingNote != null) {
                val nextVersionNumber = noteVersionDao.getVersionCountForNote(id) + 1
                val historicalVersion = NoteVersion(
                    noteId = id,
                    encryptedContent = existingNote.encryptedContent,
                    iv = existingNote.iv,
                    versionNumber = nextVersionNumber,
                    savedAt = existingNote.updatedAt
                )
                noteVersionDao.insertVersion(historicalVersion)
            }
        }

        val (encryptedContent, iv) = VaultCrypto.encrypt(plainContent, sessionKey)
        val note = Note(
            id = id,
            title = title,
            encryptedContent = encryptedContent,
            iv = iv,
            tags = tags,
            language = language,
            updatedAt = System.currentTimeMillis()
        )
        return noteDao.insertNote(note)
    }

    /**
     * Decrypts a single note. Returns plain text.
     */
    fun decryptNoteContent(note: Note, sessionKey: SecretKey): String {
        return try {
            VaultCrypto.decrypt(note.encryptedContent, note.iv, sessionKey)
        } catch (e: Exception) {
            "--- DECRYPTION ERROR (INVALID KEY OR CORRUPT DATA) ---"
        }
    }

    // --- Versioning Support Operations ---

    suspend fun getVersionsForNote(noteId: Int): List<NoteVersion> {
        return noteVersionDao.getVersionsForNote(noteId)
    }

    suspend fun restoreNoteToVersion(noteId: Int, version: NoteVersion): Boolean {
        val note = noteDao.getNoteById(noteId) ?: return false
        val restoredNote = note.copy(
            encryptedContent = version.encryptedContent,
            iv = version.iv,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.insertNote(restoredNote)
        return true
    }

    fun decryptVersionContent(version: NoteVersion, sessionKey: SecretKey): String {
        return try {
            VaultCrypto.decrypt(version.encryptedContent, version.iv, sessionKey)
        } catch (e: Exception) {
            "--- DECRYPTION ERROR ---"
        }
    }

    suspend fun deleteNoteById(id: Int): Boolean {
        // Also delete associated versions
        noteVersionDao.deleteVersionsForNote(id)
        return noteDao.deleteNoteById(id) > 0
    }
    
    suspend fun deleteAllNotes() {
        noteDao.deleteAllNotes()
    }

    // --- Task Operations ---

    val allTasksFlow: Flow<List<Task>> = taskDao.getAllTasksFlow()

    suspend fun insertTask(title: String, description: String, priority: String, reminderTime: Long? = null): Long {
        val task = Task(
            title = title,
            description = description,
            priority = priority,
            reminderTime = reminderTime
        )
        return taskDao.insertTask(task)
    }

    suspend fun updateTaskStatus(id: Int, isCompleted: Boolean): Boolean {
        return taskDao.updateTaskStatus(id, isCompleted) > 0
    }

    suspend fun deleteTaskById(id: Int): Boolean {
        return taskDao.deleteTaskById(id) > 0
    }
}
