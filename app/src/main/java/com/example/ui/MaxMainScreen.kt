package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.AssistantLanguage
import com.example.model.AssistantStatus
import com.example.ui.components.AssistantSettingsSheet
import com.example.ui.components.ConfirmationCard
import com.example.ui.components.MessageBubble
import com.example.ui.components.OrbVisualizer
import com.example.ui.components.QuickActionsRow
import com.example.ui.components.VoiceNotesSheet
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.AssistantViewModel

@Composable
fun MaxMainScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val status by viewModel.status.collectAsState()
    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val isSosActive by viewModel.isSosActive.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val pendingAction by viewModel.pendingAction.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val activeLanguage by viewModel.activeLanguage.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val continuousMode by viewModel.continuousMode.collectAsState()

    var showNotesSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleVoiceListening()
        } else {
            viewModel.processUserCommand("Microphone permission was denied. You can still type commands directly.")
        }
    }

    val onMicClick = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.toggleVoiceListening()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground),
        containerColor = ObsidianBackground,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top App Bar
            TopCyberneticHeader(
                activeLanguage = activeLanguage,
                onSelectLanguage = { viewModel.setLanguage(it) },
                onOpenSettings = { showSettingsSheet = true }
            )

            // AI Core Orb section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OrbVisualizer(
                        status = status,
                        audioRms = audioRms,
                        onClick = onMicClick
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when (status) {
                            AssistantStatus.LISTENING -> "● LISTENING TO VOICE COMMAND..."
                            AssistantStatus.PROCESSING -> "● PROCESSING INTENT..."
                            AssistantStatus.SPEAKING -> "● SPEAKING..."
                            AssistantStatus.IDLE -> "TAP ORB OR MIC TO SPEAK"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.1.sp,
                            fontSize = 11.sp
                        ),
                        color = when (status) {
                            AssistantStatus.LISTENING -> CyanNeon
                            AssistantStatus.PROCESSING -> MatrixGreen
                            AssistantStatus.SPEAKING -> ElectricBlue
                            AssistantStatus.IDLE -> TextTertiary
                        }
                    )
                }
            }

            // Message Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("conversation_stream")
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(message = msg)
                }
            }

            // Sensitive Action Confirmation Card
            AnimatedVisibility(
                visible = pendingAction != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                pendingAction?.let { action ->
                    ConfirmationCard(
                        pendingAction = action,
                        onConfirm = { viewModel.confirmPendingAction() },
                        onCancel = { viewModel.cancelPendingAction() }
                    )
                }
            }

            // Quick Actions Bar
            QuickActionsRow(
                isTorchOn = isTorchOn,
                isSosActive = isSosActive,
                batteryLevel = batteryLevel,
                notesCount = notes.size,
                onToggleTorch = { viewModel.toggleTorchDirectly() },
                onToggleSos = { viewModel.toggleSosDirectly() },
                onOpenDiagnostics = { viewModel.processUserCommand("Phone diagnostics") },
                onOpenNotes = { showNotesSheet = true },
                onVolumeUp = { viewModel.processUserCommand("Volume up") },
                onVolumeDown = { viewModel.processUserCommand("Volume down") },
                onCommandSelected = { cmd -> viewModel.processUserCommand(cmd) }
            )

            // Bottom Command Input Bar
            BottomInputBar(
                inputText = inputText,
                onInputTextChanged = { viewModel.setInputText(it) },
                onSend = { viewModel.submitCurrentInput() },
                onMicClick = onMicClick,
                isListening = status == AssistantStatus.LISTENING
            )
        }
    }

    // Voice Notes Sheet
    if (showNotesSheet) {
        VoiceNotesSheet(
            notes = notes,
            onDeleteNote = { viewModel.deleteNote(it) },
            onDismiss = { showNotesSheet = false }
        )
    }

    // Assistant Settings Sheet
    if (showSettingsSheet) {
        AssistantSettingsSheet(
            speechRate = speechRate,
            isContinuousMode = continuousMode,
            onSpeechRateChange = { viewModel.setSpeechRate(it) },
            onToggleContinuousMode = { viewModel.toggleContinuousMode() },
            onClearHistory = { viewModel.clearHistory() },
            onDismiss = { showSettingsSheet = false }
        )
    }
}

@Composable
fun TopCyberneticHeader(
    activeLanguage: AssistantLanguage,
    onSelectLanguage: (AssistantLanguage) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedLangMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Logo & Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(CyanNeon, ElectricBlue))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    ),
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "MAX",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "AI SMARTPHONE ASSISTANT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = CyanNeon
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Language selector button
            Box {
                Surface(
                    onClick = { expandedLangMenu = true },
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.testTag("language_selector_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${activeLanguage.flag} ${activeLanguage.displayName}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = TextPrimary
                        )
                    }
                }

                DropdownMenu(
                    expanded = expandedLangMenu,
                    onDismissRequest = { expandedLangMenu = false },
                    modifier = Modifier.background(ObsidianSurfaceElevated)
                ) {
                    AssistantLanguage.entries.forEach { lang ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${lang.flag} ${lang.displayName}",
                                    color = if (lang == activeLanguage) CyanNeon else TextPrimary
                                )
                            },
                            onClick = {
                                onSelectLanguage(lang)
                                expandedLangMenu = false
                            }
                        )
                    }
                }
            }

            // Security Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianSurfaceElevated,
                border = BorderStroke(1.dp, MatrixGreen.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security Active",
                        tint = MatrixGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "SECURE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MatrixGreen
                    )
                }
            }

            // Settings Button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ObsidianSurfaceElevated)
                    .border(1.dp, ObsidianBorder, CircleShape)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun BottomInputBar(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = ObsidianSurface,
        border = BorderStroke(width = 1.dp, color = ObsidianBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                placeholder = {
                    Text(
                        text = "Ask MAX or type command...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = TextTertiary
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyanNeon,
                    focusedContainerColor = ObsidianSurfaceElevated,
                    unfocusedContainerColor = ObsidianSurfaceElevated
                ),
                maxLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (inputText.isNotBlank()) {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(CyanNeon)
                        .testTag("send_command_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) CyanNeon else ObsidianSurfaceElevated
                        )
                        .border(
                            1.dp,
                            if (isListening) CyanNeon else ObsidianBorder,
                            CircleShape
                        )
                        .testTag("voice_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (isListening) "Listening" else "Activate Voice",
                        tint = if (isListening) Color.Black else CyanNeon,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
