package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * REPRESENTATION OF THE SQL SCHEMA IN KOTLIN ROOM ENTITIES.
 * 
 * --- SQL Equivalent Schema ---
 * 
 * CREATE TABLE IF NOT EXISTS `users` (
 *     `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
 *     `username` TEXT NOT NULL,
 *     `password_hash` TEXT NOT NULL,
 *     `salt` TEXT NOT NULL,
 *     `created_at` INTEGER NOT NULL
 * );
 * 
 * CREATE TABLE IF NOT EXISTS `notes` (
 *     `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
 *     `title` TEXT NOT NULL,
 *     `encrypted_content` TEXT NOT NULL,
 *     `iv` TEXT NOT NULL,
 *     `tags` TEXT NOT NULL,
 *     `language` TEXT NOT NULL,
 *     `updated_at` INTEGER NOT NULL
 * );
 * CREATE INDEX IF NOT EXISTS `index_notes_title` ON `notes` (`title`);
 * 
 * CREATE TABLE IF NOT EXISTS `tasks` (
 *     `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
 *     `title` TEXT NOT NULL,
 *     `description` TEXT NOT NULL,
 *     `is_completed` INTEGER NOT NULL DEFAULT 0,
 *     `priority` TEXT NOT NULL,
 *     `created_at` INTEGER NOT NULL
 * );
 */

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    val salt: String, // Base64 encoded salt
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notes",
    indices = [Index(value = ["title"])] // Index for fast indexing/search
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    @ColumnInfo(name = "encrypted_content") val encryptedContent: String,
    val iv: String, // Base64 encoded Initialization Vector (IV)
    val tags: String = "", // Comma-separated tags, e.g. "kotlin,security,sql"
    val language: String = "Text", // "Kotlin", "JS", "Python", "SQL", "Rust", "Go", "Markdown", "Text"
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    // Helper function to check if this is code support note
    val isCodeNote: Boolean
        get() = language != "Text" && language != "Markdown"
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    val priority: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW"
    @ColumnInfo(name = "reminder_time") val reminderTime: Long? = null, // Milliseconds timestamp
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "note_versions",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["note_id"])]
)
data class NoteVersion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "note_id") val noteId: Int,
    @ColumnInfo(name = "encrypted_content") val encryptedContent: String,
    val iv: String,
    @ColumnInfo(name = "version_number") val versionNumber: Int,
    @ColumnInfo(name = "saved_at") val savedAt: Long = System.currentTimeMillis()
)
