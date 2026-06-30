package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.crypto.SecretKey

sealed interface AuthUiState {
    object Checking : AuthUiState
    object NeedsSetup : AuthUiState
    object Locked : AuthUiState
    data class Unlocked(val username: String) : AuthUiState
}

sealed class SqlResult {
    data class SuccessSelect(val columns: List<String>, val rows: List<List<String>>) : SqlResult()
    data class SuccessMutation(val message: String) : SqlResult()
    data class Error(val errorMessage: String) : SqlResult()
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val database = VaultDatabase.getDatabase(application)
    private val repository = VaultRepository(database)

    // Auth & Key State
    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Checking)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private var sessionKey: SecretKey? = null
    
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Note Versions State Flow
    private val _noteVersions = MutableStateFlow<List<NoteVersion>>(emptyList())
    val noteVersions: StateFlow<List<NoteVersion>> = _noteVersions.asStateFlow()

    // Biometric Preferences & Keys Setup
    private val prefs = application.getSharedPreferences("ovs_prefs", android.content.Context.MODE_PRIVATE)
    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean("biometric_enabled", false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private fun getOrCreateBiometricKey(): javax.crypto.SecretKey {
        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "ovs_biometric_key_v1"
        if (!keyStore.containsAlias(alias)) {
            val keyGenerator = javax.crypto.KeyGenerator.getInstance(
                "AES",
                "AndroidKeyStore"
            )
            val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                alias,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }
        return (keyStore.getEntry(alias, null) as java.security.KeyStore.SecretKeyEntry).secretKey
    }

    private fun encryptMasterPassword(password: String): Pair<String, String>? {
        return try {
            val key = getOrCreateBiometricKey()
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
            val encryptedBase64 = android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.NO_WRAP)
            val ivBase64 = android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP)
            Pair(encryptedBase64, ivBase64)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decryptMasterPassword(encryptedBase64: String, ivBase64: String): String? {
        return try {
            val key = getOrCreateBiometricKey()
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            val iv = android.util.Base64.decode(ivBase64, android.util.Base64.NO_WRAP)
            val gcmSpec = javax.crypto.spec.GCMParameterSpec(128, iv)
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, gcmSpec)
            val encryptedBytes = android.util.Base64.decode(encryptedBase64, android.util.Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun setBiometricEnabled(enabled: Boolean, masterPasswordIfEnabling: String? = null) {
        if (enabled && masterPasswordIfEnabling != null) {
            val encryptedPair = encryptMasterPassword(masterPasswordIfEnabling)
            if (encryptedPair != null) {
                prefs.edit().apply {
                    putBoolean("biometric_enabled", true)
                    putString("encrypted_password", encryptedPair.first)
                    putString("biometric_iv", encryptedPair.second)
                    apply()
                }
                _isBiometricEnabled.value = true
            }
        } else {
            prefs.edit().apply {
                putBoolean("biometric_enabled", false)
                remove("encrypted_password")
                remove("biometric_iv")
                apply()
            }
            _isBiometricEnabled.value = false
        }
    }

    fun canUseBiometric(): Boolean {
        return prefs.getBoolean("biometric_enabled", false) &&
               !prefs.getString("encrypted_password", "").isNullOrEmpty()
    }

    fun authenticateWithBiometric() {
        if (!canUseBiometric()) return
        val encryptedPassword = prefs.getString("encrypted_password", "") ?: return
        val iv = prefs.getString("biometric_iv", "") ?: return
        val decryptedPassword = decryptMasterPassword(encryptedPassword, iv)
        if (decryptedPassword != null) {
            authenticate(password = decryptedPassword)
        } else {
            _authError.value = "Biometric Decryption Failed"
        }
    }

    // Notes & Tasks Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active screen / dialog controllers
    var currentTab = MutableStateFlow(0) // 0: Vault, 1: Snippets, 2: Tasks, 3: SQL Terminal

    // Flow for notes lists
    @OptIn(FlowPreview::class)
    val notesList: StateFlow<List<Note>> = _searchQuery
        .debounce(150)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.allNotesFlow
            } else {
                repository.searchNotesFlow(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val tasksList: StateFlow<List<Task>> = repository.allTasksFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // SQL Console State
    private val _sqlConsoleOutput = MutableStateFlow<SqlResult?>(null)
    val sqlConsoleOutput: StateFlow<SqlResult?> = _sqlConsoleOutput.asStateFlow()

    init {
        checkFirstRun()
    }

    private fun checkFirstRun() {
        viewModelScope.launch {
            if (repository.isFirstRun()) {
                _authUiState.value = AuthUiState.NeedsSetup
            } else {
                _authUiState.value = AuthUiState.Locked
            }
        }
    }

    // --- Actions ---

    fun setupMasterPassword(password: String) {
        if (password.length < 4) {
            _authError.value = "Master key must be at least 4 characters."
            return
        }
        viewModelScope.launch {
            _authError.value = null
            val success = repository.registerUser("admin", password.toCharArray())
            if (success) {
                // Instantly authenticate
                authenticate("admin", password)
            } else {
                _authError.value = "Registration failed."
            }
        }
    }

    fun authenticate(username: String = "admin", password: String) {
        viewModelScope.launch {
            _authError.value = null
            val key = repository.authenticateUser(username, password.toCharArray())
            if (key != null) {
                sessionKey = key
                _authUiState.value = AuthUiState.Unlocked(username)
                // Seed a few programmers' notes if database is clean
                seedDataIfEmpty()
            } else {
                _authError.value = "Invalid Master Password!"
            }
        }
    }

    fun logout() {
        sessionKey = null
        _authUiState.value = AuthUiState.Locked
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Encryption Actions ---

    fun decryptNote(note: Note): String {
        val key = sessionKey ?: return "Vault is locked!"
        return repository.decryptNoteContent(note, key)
    }

    fun createNote(title: String, plainContent: String, tags: String, language: String) {
        val key = sessionKey ?: return
        viewModelScope.launch {
            repository.insertEncryptedNote(
                title = title,
                plainContent = plainContent,
                tags = tags,
                language = language,
                sessionKey = key
            )
        }
    }

    fun updateNote(id: Int, title: String, plainContent: String, tags: String, language: String) {
        val key = sessionKey ?: return
        viewModelScope.launch {
            repository.insertEncryptedNote(
                id = id,
                title = title,
                plainContent = plainContent,
                tags = tags,
                language = language,
                sessionKey = key
            )
            // Trigger refresh
            _searchQuery.value = _searchQuery.value
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            repository.deleteNoteById(id)
        }
    }

    // --- Version History Actions ---

    fun loadVersions(noteId: Int) {
        viewModelScope.launch {
            _noteVersions.value = repository.getVersionsForNote(noteId)
        }
    }

    fun decryptVersion(version: NoteVersion): String {
        val key = sessionKey ?: return "Vault is locked!"
        return repository.decryptVersionContent(version, key)
    }

    fun restoreVersion(noteId: Int, version: NoteVersion) {
        viewModelScope.launch {
            repository.restoreNoteToVersion(noteId, version)
            loadVersions(noteId)
            // Trigger refresh
            _searchQuery.value = _searchQuery.value
        }
    }

    // --- Task Actions ---

    fun createTask(title: String, description: String, priority: String, reminderDelayMs: Long? = null) {
        viewModelScope.launch {
            val reminderTime = if (reminderDelayMs != null) System.currentTimeMillis() + reminderDelayMs else null
            val taskId = repository.insertTask(title, description, priority, reminderTime)
            if (reminderTime != null && taskId > 0) {
                scheduleTaskReminder(taskId.toInt(), title, description, reminderTime)
            }
        }
    }

    private fun scheduleTaskReminder(taskId: Int, title: String, description: String, triggerTimeMs: Long) {
        val context = getApplication<Application>().applicationContext
        val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = android.content.Intent(context, com.example.data.TaskReminderReceiver::class.java).apply {
            putExtra("task_id", taskId)
            putExtra("task_title", title)
            putExtra("task_desc", description)
        }
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } else {
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        }
    }

    fun toggleTaskStatus(id: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateTaskStatus(id, isCompleted)
        }
    }

    fun deleteTask(id: Int) {
        viewModelScope.launch {
            repository.deleteTaskById(id)
        }
    }

    // --- Direct SQL Terminal Execution (Custom Programmer Feature) ---

    fun executeSql(sql: String) {
        viewModelScope.launch {
            try {
                val db = database.openHelper.writableDatabase
                val trimmedSql = sql.trim()
                if (trimmedSql.uppercase().startsWith("SELECT")) {
                    val cursor = db.query(trimmedSql)
                    val columnNames = cursor.columnNames.toList()
                    val rows = mutableListOf<List<String>>()
                    while (cursor.moveToNext()) {
                        val row = mutableListOf<String>()
                        for (i in 0 until cursor.columnCount) {
                            row.add(cursor.getString(i) ?: "NULL")
                        }
                        rows.add(row)
                    }
                    cursor.close()
                    _sqlConsoleOutput.value = SqlResult.SuccessSelect(columnNames, rows)
                } else {
                    db.execSQL(trimmedSql)
                    _sqlConsoleOutput.value = SqlResult.SuccessMutation("Query executed successfully on offline SQLite database.")
                }
            } catch (e: Exception) {
                _sqlConsoleOutput.value = SqlResult.Error(e.message ?: "Syntax/SQLite error")
            }
        }
    }

    fun clearSqlOutput() {
        _sqlConsoleOutput.value = null
    }

    // --- Seed Demo Data (1000+ notes support / demo data) ---

    private suspend fun seedDataIfEmpty() {
        val notes = repository.getAllNotes()
        if (notes.isEmpty()) {
            val key = sessionKey ?: return
            
            // Seed a few beautiful starting developer notes
            repository.insertEncryptedNote(
                title = "React Native Secure Auth & SQLite.js",
                plainContent = "import React, { useState } from 'react';\nimport { View, Text, TextInput, Button, StyleSheet, Alert } from 'react-native';\nimport * as Keychain from 'react-native-keychain';\nimport SQLite from 'react-native-sqlite-2';\n\nconst db = SQLite.openDatabase('ovs_secure_vault.db', '1.0', '', 1);\n\nexport default function SecureAuthFlow() {\n  const [pin, setPin] = useState('');\n  const [isAuthenticated, setIsAuthenticated] = useState(false);\n\n  const registerUser = async (password) => {\n    try {\n      await Keychain.setGenericPassword('ovs_master_user', password, {\n        accessible: Keychain.ACCESSIBLE.WHEN_UNLOCKED_THIS_DEVICE_ONLY,\n        securityLevel: Keychain.SECURITY_LEVEL.SECURE_HARDWARE\n      });\n      Alert.alert('Success', 'Offline master key registered!');\n    } catch (e) { console.error(e); }\n  };\n\n  const authenticateUser = async (enteredPin) => {\n    try {\n      const credentials = await Keychain.getGenericPassword();\n      if (credentials && credentials.password === enteredPin) {\n        setIsAuthenticated(true);\n        Alert.alert('Unlocked', 'AES-256 decryption key loaded!');\n      } else {\n        Alert.alert('Denied', 'Invalid master credentials.');\n      }\n    } catch (e) { console.error(e); }\n  };\n\n  return (\n    <View style={styles.container}>\n      <Text style={styles.title}>OVS Secure Local Authentication</Text>\n      <TextInput style={styles.input} placeholder=\"Enter Master PIN\" secureTextEntry keyboardType=\"numeric\" value={pin} onChangeText={setPin} />\n      <Button title=\"Unlock Vault\" onPress={() => authenticateUser(pin)} />\n    </View>\n  );\n}",
                tags = "react-native,sqlite,auth,security",
                language = "TypeScript",
                sessionKey = key
            )

            repository.insertEncryptedNote(
                title = "AES-256 Utils.ts",
                plainContent = "const encrypt = (data: string) => {\n  const iv = crypto.randomBytes(12);\n  const cipher = crypto.createCipheriv('aes-256-gcm', masterKey, iv);\n  return cipher.update(data) + cipher.final();\n};",
                tags = "TypeScript,crypto,node",
                language = "TypeScript",
                sessionKey = key
            )

            repository.insertEncryptedNote(
                title = "Local database indexing strategy",
                plainContent = "To keep search under 1ms on local files:\n1. Use indexed columns for fields used in WHERE clauses.\n2. Add index: CREATE INDEX idx_notes_title ON notes (title);\n3. Query using LIKE with leading/trailing wildcards sparingly.",
                tags = "sqlite,sql,performance",
                language = "Markdown",
                sessionKey = key
            )

            repository.insertEncryptedNote(
                title = "Rust fast memory allocation.rs",
                plainContent = "pub fn allocate_nodes(count: usize) -> Vec<Node> {\n    let mut nodes = Vec::with_capacity(count);\n    for i in 0..count {\n        nodes.push(Node::new(i));\n    }\n    nodes\n}",
                tags = "Rust,memory",
                language = "Rust",
                sessionKey = key
            )

            repository.insertEncryptedNote(
                title = "Ktor local routing endpoint.kt",
                plainContent = "routing {\n    get(\"/api/v1/vault\") {\n        val notes = repository.getNotes()\n        call.respond(notes)\n    }\n}",
                tags = "Kotlin,ktor",
                language = "Kotlin",
                sessionKey = key
            )

            // Seed some tasks
            repository.insertTask(
                title = "Audit local SQLite indices",
                description = "Verify performance using EXPLAIN QUERY PLAN.",
                priority = "HIGH"
            )
            repository.insertTask(
                title = "Rotate master cryptographic keys",
                description = "Execute PBKDF2 salt rotation script on vault files.",
                priority = "MEDIUM"
            )
            repository.insertTask(
                title = "Optimise lazy rendering frames",
                description = "Profile Jetpack Compose lists to avoid recomposition ripples.",
                priority = "LOW"
            )
            
            // Seed 1000 placeholder developer notes to prove high performance search!
            // We do this in a single transaction to maintain speed.
            database.runInTransaction {
                viewModelScope.launch {
                    for (i in 1..1000) {
                        repository.insertEncryptedNote(
                            title = "Developer Snippet Index #$i",
                            plainContent = "Automatically generated secure payload #$i for instant indexing performance benchmarking. Contains AES-256 GCM encrypted contents.",
                            tags = "benchmark,generated,snippet",
                            language = "Text",
                            sessionKey = key
                        )
                    }
                }
            }
        }
    }
}
