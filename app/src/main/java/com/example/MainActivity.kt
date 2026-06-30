package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFD0BCFF),
                    onPrimary = Color(0xFF381E72),
                    surface = Color(0xFF2B2930),
                    background = Color(0xFF1C1B1F),
                    outline = Color(0xFF49454F)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1C1B1F)
                ) {
                    VaultApp()
                }
            }
        }
    }
}

@Composable
fun VaultApp(viewModel: VaultViewModel = viewModel()) {
    val authState by viewModel.authUiState.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = authState,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "AuthTransition"
    ) { state ->
        when (state) {
            is AuthUiState.Checking -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFD0BCFF))
                }
            }
            is AuthUiState.NeedsSetup -> {
                SetupMasterKeyScreen(viewModel = viewModel)
            }
            is AuthUiState.Locked -> {
                LoginMasterKeyScreen(viewModel = viewModel)
            }
            is AuthUiState.Unlocked -> {
                MainVaultDashboard(viewModel = viewModel, username = state.username)
            }
        }
    }
}

// --- SECURE SETUP SCREEN ---
@Composable
fun SetupMasterKeyScreen(viewModel: VaultViewModel) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val error by viewModel.authError.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Icon Placeholder
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFD0BCFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock Logo",
                tint = Color(0xFF381E72),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Create Offline Vault",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE6E1E5)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Initialize an offline AES-256 encrypted database vault. Your master key is processed locally using PBKDF2 and is never transmitted off your device.",
            fontSize = 13.sp,
            color = Color(0xFFCAC4D0),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Master Cryptographic Password", color = Color(0xFFCAC4D0)) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) iconVisibility else iconVisibilityOff,
                        contentDescription = "Toggle password visibility",
                        tint = Color(0xFFCAC4D0)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFFE6E1E5),
                unfocusedTextColor = Color(0xFFCAC4D0),
                focusedBorderColor = Color(0xFFD0BCFF),
                unfocusedBorderColor = Color(0xFF49454F)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("setup_password_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password", color = Color(0xFFCAC4D0)) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFFE6E1E5),
                unfocusedTextColor = Color(0xFFCAC4D0),
                focusedBorderColor = Color(0xFFD0BCFF),
                unfocusedBorderColor = Color(0xFF49454F)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("setup_confirm_password_input")
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (password != confirmPassword) {
                    Toast.makeText(viewModel.getApplication(), "Passwords do not match!", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.setupMasterPassword(password)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD0BCFF),
                contentColor = Color(0xFF381E72)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("initialize_vault_button")
        ) {
            Text("Initialize Cryptographic Vault", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security indicators info card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2B2930))
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "🔒 Cryptographic Specifications",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFFD0BCFF)
                )
                Text("• Key Derivation: PBKDF2WithHmacSHA256", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                Text("• Encryption Cipher: AES-256 GCM NoPadding", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                Text("• Security: Zero-knowledge client-side decryption", fontSize = 11.sp, color = Color(0xFFCAC4D0))
            }
        }
    }
}

// --- SECURE LOGIN / UNLOCK SCREEN ---
@Composable
fun LoginMasterKeyScreen(viewModel: VaultViewModel) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val error by viewModel.authError.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFD0BCFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock Logo",
                tint = Color(0xFF381E72),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "OVS Database Locked",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE6E1E5)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your offline master password to decrypt notes, view code snippets, and load interactive sql command buffers.",
            fontSize = 13.sp,
            color = Color(0xFFCAC4D0),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Master Password", color = Color(0xFFCAC4D0)) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) iconVisibility else iconVisibilityOff,
                        contentDescription = "Toggle password visibility",
                        tint = Color(0xFFCAC4D0)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFFE6E1E5),
                unfocusedTextColor = Color(0xFFCAC4D0),
                focusedBorderColor = Color(0xFFD0BCFF),
                unfocusedBorderColor = Color(0xFF49454F)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_password_input")
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (password.isNotEmpty()) {
                    viewModel.authenticate(password = password)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD0BCFF),
                contentColor = Color(0xFF381E72)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("unlock_vault_button")
        ) {
            Text("Unlock Database", fontWeight = FontWeight.Bold)
        }

        var showBiometricSimulator by remember { mutableStateOf(false) }
        val canUseBiometric = viewModel.canUseBiometric()

        if (canUseBiometric) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showBiometricSimulator = true },
                border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD0BCFF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("biometric_unlock_button")
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = iconFingerprint,
                        contentDescription = "Fingerprint Scan",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFD0BCFF)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Biometric Scan Unlock", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (showBiometricSimulator) {
            BiometricSimulatorDialog(
                viewModel = viewModel,
                onDismiss = { showBiometricSimulator = false }
            )
        }
    }
}

// --- UNLOCKED PRIMARY VAULT INTERFACE ---
@Composable
fun MainVaultDashboard(viewModel: VaultViewModel, username: String) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    
    // Dialog Controller States
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedNoteForViewing by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF1C1B1F),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF2B2930),
                tonalElevation = 0.dp,
                modifier = Modifier
                    .height(80.dp)
                    .border(BorderStroke(0.5.dp, Color(0xFF49454F)))
            ) {
                val tabs = listOf(
                    Triple(0, "Vault", iconShield),
                    Triple(1, "Snippets", iconCode),
                    Triple(2, "Tasks", Icons.Default.CheckCircle),
                    Triple(3, "Terminal", iconTerminal)
                )

                tabs.forEach { (index, label, icon) ->
                    val isSelected = currentTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.currentTab.value = index },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Color(0xFF4F378B) else Color.Transparent)
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFFCAC4D0)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab != 3) { // Hide FAB in SQL Terminal console
                FloatingActionButton(
                    onClick = {
                        if (currentTab == 2) {
                            showAddTaskDialog = true
                        } else {
                            showAddNoteDialog = true
                        }
                    },
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("action_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // HEADER AREA
            HeaderSection(
                viewModel = viewModel,
                searchQuery = searchQuery,
                onSettingsClick = { showSettingsDialog = true }
            )

            // ACTIVE SCREEN CONTENT
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (currentTab) {
                    0 -> VaultTabContent(viewModel = viewModel, onNoteClick = { selectedNoteForViewing = it })
                    1 -> SnippetsTabContent(viewModel = viewModel, onNoteClick = { selectedNoteForViewing = it })
                    2 -> TasksTabContent(viewModel = viewModel)
                    3 -> TerminalTabContent(viewModel = viewModel)
                }
            }
        }
    }

    // --- POPUP DIALOGS ---
    if (showAddNoteDialog) {
        AddNoteDialog(
            viewModel = viewModel,
            onDismiss = { showAddNoteDialog = false }
        )
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            viewModel = viewModel,
            onDismiss = { showAddTaskDialog = false }
        )
    }

    if (showSettingsDialog) {
        VaultSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (selectedNoteForViewing != null) {
        NoteDetailDialog(
            note = selectedNoteForViewing!!,
            viewModel = viewModel,
            onDismiss = { selectedNoteForViewing = null }
        )
    }
}

// --- HEADER COMPONENT ---
@Composable
fun HeaderSection(viewModel: VaultViewModel, searchQuery: String, onSettingsClick: () -> Unit) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Title Pair
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFD0BCFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "OV",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF381E72),
                        fontSize = 15.sp
                    )
                }
                Column {
                    Text(
                        text = "Offline Vault",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE6E1E5)
                    )
                    Text(
                        text = "Zero-Knowledge Storage",
                        fontSize = 11.sp,
                        color = Color(0xFFD0BCFF)
                    )
                }
            }

            // Right Action Pair
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Settings button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2B2930))
                        .clickable { onSettingsClick() }
                        .testTag("vault_settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconSettings,
                        contentDescription = "Vault Settings",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(18.dp)
                    )
                }
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2B2930)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconCloudOff,
                        contentDescription = "Sync Disabled",
                        tint = Color(0xFFCAC4D0),
                        modifier = Modifier.size(18.dp)
                    )
                }
                // Logout action
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2B2930))
                        .clickable { viewModel.logout() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconLockOpen,
                        contentDescription = "Lock Database",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Input Bar (only on Vault & Snippets tabs)
        if (currentTab == 0 || currentTab == 1) {
            val noteCount by viewModel.notesList.collectAsStateWithLifecycle()
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = {
                    Text(
                        text = "Search encrypted index (${noteCount.size} items)...",
                        color = Color(0xFFCAC4D0),
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = Color(0xFFCAC4D0),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFFCAC4D0)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFE6E1E5),
                    unfocusedTextColor = Color(0xFFCAC4D0),
                    focusedContainerColor = Color(0xFF2B2930),
                    unfocusedContainerColor = Color(0xFF2B2930),
                    focusedBorderColor = Color(0xFF49454F),
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("vault_search_input")
            )
        }
    }
}

// --- TAB CONTENT 1: VAULT LIST ---
@Composable
fun VaultTabContent(viewModel: VaultViewModel, onNoteClick: (Note) -> Unit) {
    val notes by viewModel.notesList.collectAsStateWithLifecycle()
    var filterIndex by remember { mutableStateOf(0) } // 0: All, 1: Documents, 2: Locked

    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontal Filters
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filterOptions = listOf("All Items", "Programmer Files", "Secure Keys")
            items(filterOptions.size) { index ->
                val isSelected = filterIndex == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF49454F) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF938F99) else Color(0xFF49454F),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { filterIndex = index }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = filterOptions[index],
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) Color(0xFFE6E1E5) else Color(0xFFCAC4D0)
                    )
                }
            }
        }

        val filteredNotes = remember(notes, filterIndex) {
            when (filterIndex) {
                1 -> notes.filter { it.isCodeNote }
                2 -> notes.filter { it.tags.lowercase().contains("crypto") || it.tags.lowercase().contains("key") || it.tags.lowercase().contains("secure") }
                else -> notes
            }
        }

        if (filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = iconFilterList,
                        contentDescription = "No items",
                        tint = Color(0xFF49454F),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No notes matched index criteria.",
                        color = Color(0xFFCAC4D0),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredNotes, key = { it.id }) { note ->
                    NoteCardItem(note = note, viewModel = viewModel, onClick = { onNoteClick(note) })
                }
            }
        }
    }
}

// --- TAB CONTENT 2: SNIPPETS VIEW (ONLY CODE NOTES) ---
@Composable
fun SnippetsTabContent(viewModel: VaultViewModel, onNoteClick: (Note) -> Unit) {
    val notes by viewModel.notesList.collectAsStateWithLifecycle()
    val codeNotes = remember(notes) { notes.filter { it.isCodeNote } }

    if (codeNotes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = iconCode,
                    contentDescription = "No snippets",
                    tint = Color(0xFF49454F),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No programming code snippets stored.",
                    color = Color(0xFFCAC4D0),
                    fontSize = 13.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(codeNotes, key = { it.id }) { note ->
                NoteCardItem(note = note, viewModel = viewModel, onClick = { onNoteClick(note) })
            }
        }
    }
}

// --- TAB CONTENT 3: TASKS BOARD ---
@Composable
fun TasksTabContent(viewModel: VaultViewModel) {
    val tasks by viewModel.tasksList.collectAsStateWithLifecycle()

    if (tasks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = iconTaskAlt,
                    contentDescription = "No tasks",
                    tint = Color(0xFF49454F),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No active infrastructure tasks.",
                    color = Color(0xFFCAC4D0),
                    fontSize = 13.sp
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Daily Infrastructure Tasks",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE6E1E5),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskCardItem(task = task, viewModel = viewModel)
                }
            }
        }
    }
}

// --- TAB CONTENT 4: SQL TERMINAL CONSOLE ---
@Composable
fun TerminalTabContent(viewModel: VaultViewModel) {
    var queryInput by remember { mutableStateOf("") }
    val consoleOutput by viewModel.sqlConsoleOutput.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = "SQLite Offline Compiler",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE6E1E5)
        )
        Text(
            text = "Run relational SQL statements directly on Room tables.",
            fontSize = 11.sp,
            color = Color(0xFFCAC4D0),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Monospaced output buffer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F0E11))
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header console bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                    }
                    Text(
                        text = "ovs-sqlite-session",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF938F99)
                    )
                }

                Divider(color = Color(0xFF2B2930), thickness = 0.5.dp)

                // Render buffer output
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                ) {
                    if (consoleOutput == null) {
                        Text(
                            text = "ovs-vault$ Type SQL query and hit execute.\n\nQuick Tables Available:\n• users (id, username, password_hash, salt, created_at)\n• notes (id, title, encrypted_content, iv, tags, language, updated_at)\n• tasks (id, title, description, is_completed, priority, created_at)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF938F99),
                            lineHeight = 16.sp
                        )
                    } else {
                        when (val result = consoleOutput!!) {
                            is SqlResult.SuccessMutation -> {
                                Text(
                                    text = "ovs-vault$ Success:\n${result.message}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF27C93F)
                                )
                            }
                            is SqlResult.SuccessSelect -> {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "ovs-vault$ Returned ${result.rows.size} rows:",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFF27C93F),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    
                                    // Header Column row
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        result.columns.forEach { col ->
                                            Text(
                                                text = col.uppercase(),
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = Color(0xFFD0BCFF),
                                                modifier = Modifier.widthIn(min = 80.dp, max = 150.dp)
                                            )
                                        }
                                    }
                                    Divider(color = Color(0xFF49454F), thickness = 0.5.dp)

                                    // Rows data
                                    result.rows.forEach { row ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            row.forEach { cell ->
                                                Text(
                                                    text = cell,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFFCAC4D0),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.widthIn(min = 80.dp, max = 150.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            is SqlResult.Error -> {
                                Text(
                                    text = "ovs-vault$ Error executing buffer:\n${result.errorMessage}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFFFF5F56)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Preset query triggers
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf(
                "SELECT * FROM notes LIMIT 3" to "Select Notes",
                "SELECT * FROM tasks" to "Select Tasks",
                "SELECT sqlite_version()" to "DB Version",
                "DELETE FROM notes WHERE title LIKE 'Developer Snippet Index%'" to "Delete Demo Data"
            )
            items(presets.size) { index ->
                val (sql, label) = presets[index]
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2B2930))
                        .clickable { queryInput = sql }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = label, fontSize = 11.sp, color = Color(0xFFD0BCFF))
                }
            }
        }

        // Buffer input bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = queryInput,
                onValueChange = { queryInput = it },
                placeholder = { Text("Enter SQLite Query...", color = Color(0xFFCAC4D0), fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFE6E1E5),
                    unfocusedTextColor = Color(0xFFCAC4D0),
                    focusedContainerColor = Color(0xFF2B2930),
                    unfocusedContainerColor = Color(0xFF2B2930),
                    focusedBorderColor = Color(0xFFD0BCFF),
                    unfocusedBorderColor = Color(0xFF49454F)
                ),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("sql_terminal_input")
            )

            // Submit Button
            IconButton(
                onClick = {
                    if (queryInput.isNotEmpty()) {
                        viewModel.executeSql(queryInput)
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFD0BCFF))
                    .testTag("execute_sql_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Execute",
                    tint = Color(0xFF381E72)
                )
            }

            // Clear Button
            IconButton(
                onClick = {
                    queryInput = ""
                    viewModel.clearSqlOutput()
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF49454F))
            ) {
                Icon(
                    imageVector = iconDeleteSweep,
                    contentDescription = "Clear Session",
                    tint = Color(0xFFCAC4D0)
                )
            }
        }
    }
}

// --- CARD ITEM: NOTE CARD CARD ---
@Composable
fun NoteCardItem(note: Note, viewModel: VaultViewModel, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2B2930))
            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD0BCFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = Color(0xFFCAC4D0),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Short dynamic snippet view (decrypted in memory cleanly)
            val decryptedPreview = remember(note) {
                val full = viewModel.decryptNote(note)
                if (full.length > 110) "${full.take(110)}..." else full
            }

            if (note.isCodeNote) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1C1B1F))
                        .padding(12.dp)
                ) {
                    Text(
                        text = decryptedPreview,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFA6A6A6),
                        lineHeight = 15.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = decryptedPreview,
                    fontSize = 12.sp,
                    color = Color(0xFFCAC4D0),
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle information / tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedDate = remember(note.updatedAt) {
                    val sdf = SimpleDateFormat("HH:mm a", Locale.getDefault())
                    sdf.format(Date(note.updatedAt))
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Updated $formattedDate",
                        fontSize = 10.sp,
                        color = Color(0xFF938F99)
                    )
                    Text(text = "•", fontSize = 10.sp, color = Color(0xFF938F99))
                    Text(
                        text = "${(note.encryptedContent.length / 1024.0).let { "%.1f".format(it) }} KB",
                        fontSize = 10.sp,
                        color = Color(0xFF938F99)
                    )
                }

                // Language Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF381E72))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = note.language,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD0BCFF)
                    )
                }
            }
        }
    }
}

// --- CARD ITEM: TASK CARD CARD ---
@Composable
fun TaskCardItem(task: Task, viewModel: VaultViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2B2930))
            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox Container
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(
                        2.dp,
                        if (task.isCompleted) Color(0xFFD0BCFF) else Color(0xFF938F99),
                        RoundedCornerShape(4.dp)
                    )
                    .background(if (task.isCompleted) Color(0xFFD0BCFF) else Color.Transparent)
                    .clickable { viewModel.toggleTaskStatus(task.id, !task.isCompleted) },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color(0xFF381E72),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Task texts
            Column {
                Text(
                    text = task.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (task.isCompleted) Color(0xFF938F99) else Color(0xFFE6E1E5),
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )
                if (task.description.isNotEmpty()) {
                    Text(
                        text = task.description,
                        fontSize = 11.sp,
                        color = Color(0xFF938F99),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (task.reminderTime != null && task.reminderTime!! > 0L) {
                    val formattedTime = remember(task.reminderTime) {
                        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                        sdf.format(Date(task.reminderTime!!))
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Reminder set",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Reminder: $formattedTime",
                            fontSize = 10.sp,
                            color = Color(0xFFD0BCFF)
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority Tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when (task.priority.uppercase()) {
                            "HIGH" -> Color(0xFF8C1D18)
                            "MEDIUM" -> Color(0xFF4F378B)
                            else -> Color(0xFF1C1B1F)
                        }
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = task.priority,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6E1E5)
                )
            }

            // Delete trigger
            IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                Icon(
                    imageVector = iconDeleteOutline,
                    contentDescription = "Delete",
                    tint = Color(0xFFCAC4D0),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// --- POPUP DIALOGS IMPLEMENTATION ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteDialog(viewModel: VaultViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf("Text") }
    var expandedLangDropdown by remember { mutableStateOf(false) }

    val languages = listOf("Text", "Kotlin", "TypeScript", "Rust", "SQL", "Go", "Markdown", "Python")

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF2B2930))
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Encrypt New Note",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6E1E5)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", color = Color(0xFFCAC4D0)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFCAC4D0),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_note_title_input")
                )

                // Select Language
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedLangDropdown = true },
                        border = BorderStroke(1.dp, Color(0xFF49454F)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE6E1E5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Language: $selectedLang", fontSize = 13.sp)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedLangDropdown,
                        onDismissRequest = { expandedLangDropdown = false },
                        modifier = Modifier.background(Color(0xFF2B2930))
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang, color = Color(0xFFE6E1E5)) },
                                onClick = {
                                    selectedLang = lang
                                    expandedLangDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated)", color = Color(0xFFCAC4D0)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFCAC4D0),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Secure Plaintext Body", color = Color(0xFFCAC4D0)) },
                    minLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFCAC4D0),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_note_body_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color(0xFFCAC4D0))
                    }
                    Button(
                        onClick = {
                            if (title.isNotEmpty() && content.isNotEmpty()) {
                                viewModel.createNote(
                                    title = title,
                                    plainContent = content,
                                    tags = tags,
                                    language = selectedLang
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("save_note_button")
                    ) {
                        Text("Secure Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddTaskDialog(viewModel: VaultViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var reminderDelayMs by remember { mutableStateOf<Long?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF2B2930))
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Add Infrastructure Task",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6E1E5)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Header", color = Color(0xFFCAC4D0)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFCAC4D0),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_task_title_input")
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Detailed Description", color = Color(0xFFCAC4D0)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFCAC4D0),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Priority Select triggers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val priorities = listOf("LOW", "MEDIUM", "HIGH")
                    priorities.forEach { p ->
                        val isSelected = priority == p
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF4F378B) else Color(0xFF1C1B1F))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFD0BCFF) else Color(0xFF49454F),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { priority = p }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = p,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFFCAC4D0)
                            )
                        }
                    }
                }

                // Offline Reminder Section
                Text(
                    text = "Offline Alert Reminder",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD0BCFF),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val reminderOptions = listOf(
                        "None" to null,
                        "10s" to 10 * 1000L,
                        "1m" to 60 * 1000L,
                        "5m" to 300 * 1000L,
                        "1h" to 3600 * 1000L
                    )
                    reminderOptions.forEach { (label, value) ->
                        val isSelected = reminderDelayMs == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF381E72) else Color(0xFF1C1B1F))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFD0BCFF) else Color(0xFF49454F),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { reminderDelayMs = value }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFFCAC4D0)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color(0xFFCAC4D0))
                    }
                    Button(
                        onClick = {
                            if (title.isNotEmpty()) {
                                viewModel.createTask(
                                    title = title,
                                    description = desc,
                                    priority = priority,
                                    reminderDelayMs = reminderDelayMs
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("save_task_button")
                    ) {
                        Text("Create", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NoteDetailDialog(note: Note, viewModel: VaultViewModel, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val decryptedContent = remember(note) { viewModel.decryptNote(note) }

    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember(note) { mutableStateOf(note.title) }
    var editContent by remember(note, decryptedContent) { mutableStateOf(decryptedContent) }
    var editTags by remember(note) { mutableStateOf(note.tags) }
    var editLanguage by remember(note) { mutableStateOf(note.language) }

    var showHistory by remember { mutableStateOf(false) }
    val versions by viewModel.noteVersions.collectAsStateWithLifecycle()

    LaunchedEffect(note.id) {
        viewModel.loadVersions(note.id)
    }

    val sdf = remember { java.text.SimpleDateFormat("dd MMM, HH:mm:ss", java.util.Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF2B2930))
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isEditing) {
                    // READ MODE HEADER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = note.title,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE6E1E5)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                                note.tags.split(",").filter { it.trim().isNotEmpty() }.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF1C1B1F))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = tag.trim(), fontSize = 9.sp, color = Color(0xFFCAC4D0))
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF381E72))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = note.language,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD0BCFF)
                            )
                        }
                    }

                    Divider(color = Color(0xFF49454F), thickness = 0.5.dp)

                    // READ MODE CONTENT DISPLAY
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (note.isCodeNote) Color(0xFF1C1B1F) else Color(0xFF201F24))
                            .border(
                                1.dp,
                                if (note.isCodeNote) Color(0xFF49454F) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = decryptedContent,
                            fontFamily = if (note.isCodeNote) FontFamily.Monospace else FontFamily.Default,
                            fontSize = 12.sp,
                            color = Color(0xFFE6E1E5),
                            lineHeight = 18.sp
                        )
                    }

                    // VERSION HISTORY SECTION
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1C1B1F))
                            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHistory = !showHistory }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = iconHistory,
                                    contentDescription = null,
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Version History",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE6E1E5)
                                )
                            }
                            Text(
                                text = "${versions.size} backup(s)",
                                fontSize = 11.sp,
                                color = Color(0xFFD0BCFF)
                            )
                        }

                        if (showHistory) {
                            Divider(color = Color(0xFF49454F), thickness = 0.5.dp)
                            if (versions.isEmpty()) {
                                Text(
                                    text = "No previous versions saved yet. Any edit you make will automatically archive a historic copy here.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFCAC4D0),
                                    modifier = Modifier.padding(12.dp)
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 160.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    versions.forEachIndexed { idx, ver ->
                                        val verContent = viewModel.decryptVersion(ver)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(0.5.dp, Color(0xFF2B2930))
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Version #${versions.size - idx}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFE6E1E5)
                                                )
                                                Text(
                                                    text = "Saved: " + sdf.format(java.util.Date(ver.savedAt)),
                                                    fontSize = 10.sp,
                                                    color = Color(0xFFCAC4D0)
                                                )
                                                Text(
                                                    text = verContent.take(45) + if (verContent.length > 45) "..." else "",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFFD0BCFF),
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.restoreVersion(note.id, ver)
                                                    Toast.makeText(context, "Restored successfully!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = iconHistory,
                                                    contentDescription = "Restore this version",
                                                    tint = Color(0xFFD0BCFF),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = Color(0xFF49454F), thickness = 0.5.dp)

                    // READ MODE ACTIONS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.deleteNote(note.id)
                                onDismiss()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete note",
                                tint = Color(0xFFFF5F56)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(decryptedContent))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                border = BorderStroke(1.dp, Color(0xFF49454F)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD0BCFF))
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = iconContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("Copy Source", fontSize = 11.sp)
                                }
                            }

                            Button(
                                onClick = { isEditing = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F378B), contentColor = Color(0xFFE6E1E5))
                            ) {
                                Text("Edit Note", fontSize = 11.sp)
                            }

                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF49454F), contentColor = Color(0xFFE6E1E5))
                            ) {
                                Text("Done", fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    // EDITING MODE UI
                    Text(
                        text = "Edit Vault Snippet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE6E1E5)
                    )

                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title", color = Color(0xFFCAC4D0)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFE6E1E5),
                            unfocusedTextColor = Color(0xFFCAC4D0),
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editTags,
                        onValueChange = { editTags = it },
                        label = { Text("Tags (comma separated)", color = Color(0xFFCAC4D0)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFE6E1E5),
                            unfocusedTextColor = Color(0xFFCAC4D0),
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editLanguage,
                        onValueChange = { editLanguage = it },
                        label = { Text("Language/Type (e.g. Kotlin, Markdown)", color = Color(0xFFCAC4D0)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFE6E1E5),
                            unfocusedTextColor = Color(0xFFCAC4D0),
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("Content", color = Color(0xFFCAC4D0)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFE6E1E5),
                            unfocusedTextColor = Color(0xFFCAC4D0),
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F)
                        ),
                        textStyle = TextStyle(fontFamily = if (note.isCodeNote) FontFamily.Monospace else FontFamily.Default),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 220.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { isEditing = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color(0xFFCAC4D0))
                        }

                        Button(
                            onClick = {
                                if (editTitle.isNotEmpty()) {
                                    viewModel.updateNote(
                                        id = note.id,
                                        title = editTitle,
                                        plainContent = editContent,
                                        tags = editTags,
                                        language = editLanguage
                                    )
                                    isEditing = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaultSettingsDialog(viewModel: VaultViewModel, onDismiss: () -> Unit) {
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    var enterPasswordForBiometric by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF2B2930))
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Vault Protection Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6E1E5)
                )

                Divider(color = Color(0xFF49454F), thickness = 0.5.dp)

                if (!enterPasswordForBiometric) {
                    // Biometric Setup row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1C1B1F))
                            .clickable {
                                if (isBiometricEnabled) {
                                    viewModel.setBiometricEnabled(false)
                                } else {
                                    enterPasswordForBiometric = true
                                }
                            }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                imageVector = iconFingerprint,
                                contentDescription = null,
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Biometric Lock",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE6E1E5)
                                )
                                Text(
                                    text = "Unlock database securely with your fingerprint.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFCAC4D0)
                                )
                            }
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    enterPasswordForBiometric = true
                                } else {
                                    viewModel.setBiometricEnabled(false)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF381E72),
                                checkedTrackColor = Color(0xFFD0BCFF),
                                uncheckedThumbColor = Color(0xFFCAC4D0),
                                uncheckedTrackColor = Color(0xFF49454F)
                            )
                        )
                    }

                    // Diagnostics row
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1C1B1F))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Offline Cryptographic Diagnostics",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD0BCFF)
                        )
                        val diagnosticFields = listOf(
                            "Master Derivation" to "PBKDF2WithHmacSHA1",
                            "Encryption Cipher" to "AES/GCM/NoPadding (256-bit)",
                            "Local Storage" to "Room SQLite v3.0 (Encrypted)",
                            "Key Management" to "AndroidKeyStore System Sandbox"
                        )
                        diagnosticFields.forEach { (label, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = label, fontSize = 11.sp, color = Color(0xFFCAC4D0))
                                Text(text = value, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFE6E1E5))
                            }
                        }
                    }
                } else {
                    // Enter password to confirm biometric setup
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Verify Password",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE6E1E5)
                        )
                        Text(
                            text = "Enter your current master password to secure your biometric credentials on this device.",
                            fontSize = 11.sp,
                            color = Color(0xFFCAC4D0)
                        )
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Master Password", color = Color(0xFFCAC4D0)) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) iconVisibility else iconVisibilityOff,
                                        contentDescription = null,
                                        tint = Color(0xFFCAC4D0)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFCAC4D0),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMsg != null) {
                            Text(text = errorMsg!!, color = Color(0xFFFF5F56), fontSize = 11.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    enterPasswordForBiometric = false
                                    passwordInput = ""
                                    errorMsg = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back", color = Color(0xFFCAC4D0))
                            }
                            Button(
                                onClick = {
                                    if (passwordInput.isNotEmpty()) {
                                        viewModel.setBiometricEnabled(true, passwordInput)
                                        enterPasswordForBiometric = false
                                        passwordInput = ""
                                        errorMsg = null
                                    } else {
                                        errorMsg = "Password cannot be empty"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Enable", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (!enterPasswordForBiometric) {
                    Divider(color = Color(0xFF49454F), thickness = 0.5.dp)
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF49454F), contentColor = Color(0xFFE6E1E5))
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BiometricSimulatorDialog(viewModel: VaultViewModel, onDismiss: () -> Unit) {
    var scanProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        // Run a gorgeous, high-fidelity scanning progress simulation
        while (scanProgress < 1.0f) {
            kotlinx.coroutines.delay(60)
            scanProgress += 0.05f
        }
        // Upon success, authenticate with biometric
        viewModel.authenticateWithBiometric()
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1C1B1F))
                .border(1.dp, Color(0xFFD0BCFF), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Biometric Lock",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6E1E5)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pulsing glowing fingerprint container
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF381E72))
                        .border(2.dp, Color(0xFFD0BCFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconFingerprint,
                        contentDescription = "Pulsing Scan",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Verifying secure fingerprint pattern offline...",
                    fontSize = 13.sp,
                    color = Color(0xFFCAC4D0),
                    textAlign = TextAlign.Center
                )

                // Beautiful custom styling simulator progress bar
                LinearProgressIndicator(
                    progress = { scanProgress },
                    color = Color(0xFFD0BCFF),
                    trackColor = Color(0xFF2B2930),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFFFF5F56))
                }
            }
        }
    }
}

// --- Dynamic drawables for custom items as fallback ---
val iconCloudOff: ImageVector
    get() = ImageVector.Builder(
        name = "CloudOff",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(19.35f, 10.04f)
            curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
            curveTo(10.28f, 4f, 8.73f, 4.59f, 7.5f, 5.57f)
            lineTo(9.12f, 7.19f)
            curveTo(10.0f, 6.43f, 10.95f, 6f, 12f, 6f)
            curveTo(14.76f, 6f, 17f, 8.24f, 17f, 11f)
            verticalLineTo(12f)
            horizontalLineTo(18.5f)
            curveTo(19.88f, 12f, 21f, 13.12f, 21f, 14.5f)
            curveTo(21f, 15.42f, 20.5f, 16.21f, 19.74f, 16.63f)
            lineTo(21.19f, 18.08f)
            curveTo(22.31f, 17.22f, 23f, 15.94f, 23f, 14.5f)
            curveTo(23f, 12.11f, 21.35f, 10.15f, 19.35f, 10.04f)
            close()
            moveTo(3f, 5.27f)
            lineTo(5.06f, 7.33f)
            curveTo(2.83f, 7.84f, 1.1f, 9.72f, 1.1f, 12f)
            curveTo(1.1f, 14.76f, 3.34f, 17f, 6.1f, 17f)
            horizontalLineTo(14.73f)
            lineTo(17.73f, 20f)
            lineTo(19f, 18.73f)
            lineTo(4.27f, 4f)
            lineTo(3f, 5.27f)
            close()
            moveTo(6.1f, 15f)
            curveTo(4.44f, 15f, 3.1f, 13.66f, 3.1f, 12f)
            curveTo(3.1f, 10.53f, 4.15f, 9.31f, 5.56f, 9.05f)
            lineTo(11.5f, 15f)
            horizontalLineTo(6.1f)
            close()
        }
    }.build()

val iconTerminal: ImageVector
    get() = ImageVector.Builder(
        name = "Terminal",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(20f, 4f)
            horizontalLineTo(4f)
            curveTo(2.89f, 4f, 2f, 4.89f, 2f, 6f)
            verticalLineTo(18f)
            curveTo(2f, 19.11f, 2.89f, 20f, 4f, 20f)
            horizontalLineTo(20f)
            curveTo(21.1f, 20f, 22f, 19.11f, 22f, 18f)
            verticalLineTo(6f)
            curveTo(22f, 4.89f, 21.1f, 4f, 20f, 4f)
            close()
            moveTo(20f, 18f)
            horizontalLineTo(4f)
            verticalLineTo(8f)
            horizontalLineTo(20f)
            verticalLineTo(18f)
            close()
            moveTo(12f, 17f)
            horizontalLineTo(18f)
            verticalLineTo(15f)
            horizontalLineTo(12f)
            verticalLineTo(17f)
            close()
            moveTo(6f, 16f)
            lineTo(7.41f, 17.41f)
            lineTo(10.83f, 14f)
            lineTo(7.41f, 10.59f)
            lineTo(6f, 12f)
            lineTo(8f, 14f)
            lineTo(6f, 16f)
            close()
        }
    }.build()

val iconShield: ImageVector
    get() = ImageVector.Builder(
        name = "Shield",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 1f)
            lineTo(3f, 5f)
            verticalLineTo(11f)
            curveTo(3f, 16.55f, 6.84f, 21.74f, 12f, 23f)
            curveTo(17.16f, 21.74f, 21f, 16.55f, 21f, 11f)
            verticalLineTo(5f)
            lineTo(12f, 1f)
            close()
            moveTo(12f, 11.99f)
            horizontalLineTo(19f)
            curveTo(18.47f, 16.11f, 15.72f, 19.78f, 12f, 20.93f)
            verticalLineTo(11.99f)
            horizontalLineTo(5f)
            verticalLineTo(6.3f)
            lineTo(12f, 3.19f)
            verticalLineTo(11.99f)
            close()
        }
    }.build()

val iconDeleteOutline: ImageVector
    get() = ImageVector.Builder(
        name = "DeleteOutline",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(6f, 19f)
            curveTo(6f, 20.1f, 6.9f, 21f, 8f, 21f)
            horizontalLineTo(16f)
            curveTo(17.1f, 21f, 18f, 20.1f, 18f, 19f)
            verticalLineTo(7f)
            horizontalLineTo(6f)
            verticalLineTo(19f)
            close()
            moveTo(8f, 9f)
            horizontalLineTo(16f)
            verticalLineTo(19f)
            horizontalLineTo(8f)
            verticalLineTo(9f)
            close()
            moveTo(15.5f, 4f)
            lineTo(14.5f, 3f)
            horizontalLineTo(9.5f)
            lineTo(8.5f, 4f)
            horizontalLineTo(5f)
            verticalLineTo(6f)
            horizontalLineTo(19f)
            verticalLineTo(4f)
            horizontalLineTo(15.5f)
            close()
        }
    }.build()

val iconFingerprint: ImageVector
    get() = ImageVector.Builder(
        name = "Fingerprint",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 1f)
            curveTo(8.13f, 1f, 5f, 4.13f, 5f, 8f)
            verticalLineTo(10f)
            horizontalLineTo(7f)
            verticalLineTo(8f)
            curveTo(7f, 5.24f, 9.24f, 3f, 12f, 3f)
            curveTo(14.76f, 3f, 17f, 5.24f, 17f, 8f)
            verticalLineTo(12f)
            horizontalLineTo(19f)
            verticalLineTo(8f)
            curveTo(19f, 4.13f, 15.87f, 1f, 12f, 1f)
            close()
            moveTo(12f, 5f)
            curveTo(10.34f, 5f, 9f, 6.34f, 9f, 8f)
            verticalLineTo(15f)
            horizontalLineTo(11f)
            verticalLineTo(8f)
            curveTo(11f, 7.45f, 11.45f, 7f, 12f, 7f)
            curveTo(12.55f, 7f, 13f, 7.45f, 13f, 8f)
            verticalLineTo(17f)
            horizontalLineTo(15f)
            verticalLineTo(8f)
            curveTo(15f, 6.34f, 13.66f, 5f, 12f, 5f)
            close()
            moveTo(12f, 9f)
            curveTo(11.45f, 9f, 11f, 9.45f, 11f, 10f)
            verticalLineTo(20f)
            horizontalLineTo(13f)
            verticalLineTo(10f)
            curveTo(13f, 9.45f, 12.55f, 9f, 12f, 9f)
            close()
        }
    }.build()

val iconHistory: ImageVector
    get() = ImageVector.Builder(
        name = "History",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(13f, 3f)
            curveTo(8.03f, 3f, 4f, 7.03f, 4f, 12f)
            horizontalLineTo(1f)
            lineTo(4.89f, 15.89f)
            lineTo(5f, 16f)
            lineTo(9f, 12f)
            horizontalLineTo(6f)
            curveTo(6f, 8.13f, 9.13f, 5f, 13f, 5f)
            curveTo(16.87f, 5f, 20f, 8.13f, 20f, 12f)
            curveTo(20f, 15.87f, 16.87f, 19f, 13f, 19f)
            curveTo(11.07f, 19f, 9.32f, 18.21f, 8.06f, 16.94f)
            lineTo(6.64f, 18.36f)
            curveTo(8.27f, 19.99f, 10.51f, 21f, 13f, 21f)
            curveTo(17.97f, 21f, 22f, 16.97f, 22f, 12f)
            curveTo(22f, 7.03f, 17.97f, 3f, 13f, 3f)
            close()
            moveTo(12.5f, 7f)
            verticalLineTo(13f)
            lineTo(17.5f, 16f)
            lineTo(18.25f, 14.75f)
            lineTo(14f, 12.25f)
            verticalLineTo(7f)
            horizontalLineTo(12.5f)
            close()
        }
    }.build()

val iconSettings: ImageVector
    get() = ImageVector.Builder(
        name = "Settings",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(19.14f, 12.94f)
            curveTo(19.18f, 12.63f, 19.2f, 12.32f, 19.2f, 12f)
            curveTo(19.2f, 11.68f, 19.18f, 11.37f, 19.14f, 11.06f)
            lineTo(21.35f, 9.34f)
            curveTo(21.55f, 9.18f, 21.61f, 8.9f, 21.48f, 8.67f)
            lineTo(19.38f, 5.03f)
            curveTo(19.25f, 4.8f, 18.97f, 4.71f, 18.73f, 4.81f)
            lineTo(16.12f, 5.86f)
            curveTo(15.58f, 5.44f, 14.99f, 5.1f, 14.34f, 4.82f)
            lineTo(13.95f, 2.03f)
            curveTo(13.91f, 1.79f, 13.7f, 1.6f, 13.46f, 1.6f)
            horizontalLineTo(9.26f)
            curveTo(9.02f, 1.6f, 8.81f, 1.79f, 8.77f, 2.03f)
            lineTo(8.38f, 4.82f)
            curveTo(7.73f, 5.1f, 7.14f, 5.44f, 6.6f, 5.86f)
            lineTo(3.99f, 4.81f)
            curveTo(3.75f, 4.71f, 3.47f, 4.8f, 3.34f, 5.03f)
            lineTo(1.24f, 8.67f)
            curveTo(1.11f, 8.9f, 1.17f, 9.18f, 1.37f, 9.34f)
            lineTo(3.58f, 11.06f)
            curveTo(3.54f, 11.37f, 3.52f, 11.68f, 3.52f, 12f)
            curveTo(3.52f, 12.32f, 3.54f, 12.63f, 3.58f, 12.94f)
            lineTo(1.37f, 14.66f)
            curveTo(1.17f, 14.82f, 1.11f, 15.1f, 1.24f, 15.33f)
            lineTo(3.34f, 18.97f)
            curveTo(3.47f, 19.2f, 3.75f, 19.29f, 3.99f, 19.19f)
            lineTo(6.6f, 18.14f)
            curveTo(7.14f, 18.56f, 7.73f, 18.9f, 8.38f, 18.9f)
            lineTo(8.77f, 21.97f)
            curveTo(8.81f, 21.97f, 9.02f, 22.4f, 9.26f, 22.4f)
            horizontalLineTo(13.46f)
            curveTo(13.7f, 22.4f, 13.91f, 22.21f, 13.95f, 21.97f)
            lineTo(14.34f, 19.18f)
            curveTo(14.99f, 18.9f, 15.58f, 18.56f, 16.12f, 18.14f)
            lineTo(18.73f, 19.19f)
            curveTo(18.97f, 19.29f, 19.25f, 19.2f, 19.38f, 18.97f)
            lineTo(21.48f, 15.33f)
            curveTo(21.55f, 15.1f, 21.55f, 14.82f, 21.35f, 14.66f)
            lineTo(19.14f, 12.94f)
            close()
            moveTo(11.36f, 15.6f)
            curveTo(9.37f, 15.6f, 7.76f, 13.99f, 7.76f, 12f)
            curveTo(7.76f, 10.01f, 9.37f, 8.4f, 11.36f, 8.4f)
            curveTo(13.35f, 8.4f, 14.96f, 10.01f, 14.96f, 12f)
            curveTo(14.96f, 13.99f, 13.35f, 15.6f, 11.36f, 15.6f)
            close()
        }
    }.build()

val iconDeleteSweep: ImageVector
    get() = ImageVector.Builder(
        name = "DeleteSweep",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(15f, 16f)
            horizontalLineTo(19f)
            verticalLineTo(18f)
            horizontalLineTo(15f)
            close()
            moveTo(15f, 8f)
            horizontalLineTo(22f)
            verticalLineTo(10f)
            horizontalLineTo(15f)
            close()
            moveTo(15f, 12f)
            horizontalLineTo(21f)
            verticalLineTo(14f)
            horizontalLineTo(15f)
            close()
            moveTo(3f, 18f)
            curveTo(3f, 19.1f, 3.9f, 20f, 5f, 20f)
            horizontalLineTo(11f)
            curveTo(12.1f, 20f, 13f, 19.1f, 13f, 18f)
            verticalLineTo(8f)
            horizontalLineTo(3f)
            close()
            moveTo(5f, 10f)
            horizontalLineTo(11f)
            verticalLineTo(18f)
            horizontalLineTo(5f)
            close()
            moveTo(9f, 4f)
            lineTo(8f, 3f)
            horizontalLineTo(5f)
            lineTo(4f, 4f)
            horizontalLineTo(1f)
            verticalLineTo(6f)
            horizontalLineTo(12f)
            verticalLineTo(4f)
            close()
        }
    }.build()

val iconVisibility: ImageVector
    get() = ImageVector.Builder(
        name = "Visibility",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 4.5f)
            curveTo(7f, 4.5f, 2.73f, 7.61f, 1f, 12f)
            curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
            curveTo(17f, 19.5f, 21.27f, 16.39f, 23f, 12f)
            curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f)
            close()
            moveTo(12f, 17f)
            curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f)
            curveTo(7f, 9.24f, 9.24f, 7f, 12f, 7f)
            curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f)
            curveTo(17f, 14.76f, 14.76f, 17f, 12f, 17f)
            close()
            moveTo(12f, 9f)
            curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
            curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
            curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
            curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
            close()
        }
    }.build()

val iconVisibilityOff: ImageVector
    get() = ImageVector.Builder(
        name = "VisibilityOff",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 7f)
            curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f)
            curveTo(17f, 13.54f, 16.31f, 14.91f, 15.22f, 15.84f)
            lineTo(17.07f, 17.69f)
            curveTo(18.52f, 16.27f, 19.64f, 14.47f, 20.35f, 12.5f)
            curveTo(18.66f, 8.14f, 14.43f, 5f, 9.5f, 5f)
            curveTo(8.13f, 5f, 6.82f, 5.24f, 5.61f, 5.68f)
            lineTo(7.16f, 7.23f)
            curveTo(7.9f, 7.08f, 8.69f, 7f, 9.5f, 7f)
            close()
            moveTo(1.27f, 3f)
            lineTo(2.54f, 4.27f)
            curveTo(1.47f, 5.4f, 0.58f, 6.78f, 0f, 8.32f)
            curveTo(1.69f, 12.68f, 5.92f, 15.82f, 10.85f, 15.82f)
            curveTo(12.22f, 15.82f, 13.53f, 15.58f, 14.74f, 15.14f)
            lineTo(16.01f, 16.41f)
            curveTo(14.47f, 17.31f, 12.72f, 17.82f, 10.85f, 17.82f)
            curveTo(5.92f, 17.82f, 1.69f, 14.68f, 0f, 10.32f)
            curveTo(0.79f, 9.17f, 1.8f, 8.16f, 2.95f, 7.37f)
            lineTo(1.27f, 3f)
            close()
            moveTo(9.5f, 11f)
            curveTo(9.5f, 11.55f, 9.95f, 12f, 10.5f, 12f)
            curveTo(11.05f, 12f, 11.5f, 11.55f, 11.5f, 11f)
            curveTo(11.5f, 10.45f, 11.05f, 10f, 10.5f, 10f)
            curveTo(9.95f, 10f, 9.5f, 10.45f, 9.5f, 11f)
            close()
        }
    }.build()

val iconContentCopy: ImageVector
    get() = ImageVector.Builder(
        name = "ContentCopy",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(16f, 1f)
            horizontalLineTo(4f)
            curveTo(2.9f, 1f, 2f, 1.9f, 2f, 3f)
            verticalLineTo(17f)
            horizontalLineTo(4f)
            verticalLineTo(3f)
            horizontalLineTo(16f)
            verticalLineTo(1f)
            close()
            moveTo(19f, 5f)
            horizontalLineTo(8f)
            curveTo(6.9f, 5f, 6f, 5.9f, 6f, 7f)
            verticalLineTo(21f)
            curveTo(6f, 22.1f, 6.9f, 23f, 8f, 23f)
            horizontalLineTo(19f)
            curveTo(20.1f, 23f, 21f, 22.1f, 21f, 21f)
            verticalLineTo(7f)
            curveTo(21f, 5.9f, 20.1f, 5f, 19f, 5f)
            close()
            moveTo(19f, 21f)
            horizontalLineTo(8f)
            verticalLineTo(7f)
            horizontalLineTo(19f)
            verticalLineTo(21f)
            close()
        }
    }.build()

val iconCode: ImageVector
    get() = ImageVector.Builder(
        name = "Code",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(9.4f, 16.6f)
            lineTo(4.8f, 12f)
            lineTo(9.4f, 7.4f)
            lineTo(8f, 6f)
            lineTo(2f, 12f)
            lineTo(8f, 18f)
            close()
            moveTo(14.6f, 16.6f)
            lineTo(19.2f, 12f)
            lineTo(14.6f, 7.4f)
            lineTo(16f, 6f)
            lineTo(22f, 12f)
            lineTo(16f, 18f)
            close()
        }
    }.build()

val iconLockOpen: ImageVector
    get() = ImageVector.Builder(
        name = "LockOpen",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 17f)
            curveTo(13.1f, 17f, 14f, 16.1f, 14f, 15f)
            curveTo(14f, 13.9f, 13.1f, 13f, 12f, 13f)
            curveTo(10.9f, 13f, 10f, 13.9f, 10f, 15f)
            curveTo(10f, 16.1f, 10.9f, 17f, 12f, 17f)
            close()
            moveTo(18f, 8f)
            horizontalLineTo(17f)
            verticalLineTo(6f)
            curveTo(17f, 3.24f, 14.76f, 1f, 12f, 1f)
            curveTo(9.24f, 1f, 7f, 3.24f, 7f, 6f)
            horizontalLineTo(9f)
            curveTo(9f, 4.34f, 10.34f, 3f, 12f, 3f)
            curveTo(13.66f, 3f, 15f, 4.34f, 15f, 6f)
            verticalLineTo(8f)
            horizontalLineTo(6f)
            curveTo(4.9f, 8f, 4f, 8.9f, 4f, 10f)
            verticalLineTo(20f)
            curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
            horizontalLineTo(18f)
            curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
            verticalLineTo(10f)
            curveTo(20f, 8.9f, 19.1f, 8f, 18f, 8f)
            close()
            moveTo(18f, 20f)
            horizontalLineTo(6f)
            verticalLineTo(10f)
            horizontalLineTo(18f)
            verticalLineTo(20f)
            close()
        }
    }.build()

val iconFilterList: ImageVector
    get() = ImageVector.Builder(
        name = "FilterList",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(10f, 18f)
            horizontalLineTo(14f)
            verticalLineTo(16f)
            horizontalLineTo(10f)
            verticalLineTo(18f)
            close()
            moveTo(3f, 6f)
            verticalLineTo(8f)
            horizontalLineTo(21f)
            verticalLineTo(6f)
            horizontalLineTo(3f)
            close()
            moveTo(6f, 13f)
            horizontalLineTo(18f)
            verticalLineTo(11f)
            horizontalLineTo(6f)
            verticalLineTo(13f)
            close()
        }
    }.build()

val iconTaskAlt: ImageVector
    get() = ImageVector.Builder(
        name = "TaskAlt",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(22f, 9.24f)
            lineTo(20.18f, 10.45f)
            curveTo(20.7f, 11.53f, 21f, 12.73f, 21f, 14f)
            curveTo(21f, 18.97f, 16.97f, 23f, 12f, 23f)
            curveTo(7.03f, 23f, 3f, 18.97f, 3f, 14f)
            curveTo(3f, 9.03f, 7.03f, 5f, 12f, 5f)
            curveTo(13.91f, 5f, 15.68f, 5.6f, 17.15f, 6.63f)
            lineTo(18.57f, 5.21f)
            curveTo(16.74f, 3.82f, 14.47f, 3f, 12f, 3f)
            curveTo(5.92f, 3f, 1f, 7.92f, 1f, 14f)
            curveTo(1f, 20.08f, 5.92f, 25f, 12f, 25f)
            curveTo(18.08f, 25f, 23f, 20.08f, 23f, 14f)
            curveTo(23f, 12.31f, 22.62f, 10.7f, 21.96f, 9.24f)
            close()
            moveTo(11f, 17.17f)
            lineTo(7.41f, 13.59f)
            lineTo(6f, 15f)
            lineTo(11f, 20f)
            lineTo(22f, 9f)
            lineTo(20.59f, 7.59f)
            lineTo(11f, 17.17f)
            close()
        }
    }.build()
