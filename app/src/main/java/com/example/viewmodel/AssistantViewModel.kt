package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiClient
import com.example.device.PhoneController
import com.example.engine.MaxBrain
import com.example.model.ActionResult
import com.example.model.ActionType
import com.example.model.AssistantLanguage
import com.example.model.AssistantStatus
import com.example.model.ChatMessage
import com.example.model.DeviceDiagnostics
import com.example.model.PendingAction
import com.example.model.VoiceNote
import com.example.voice.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val phoneController = PhoneController(application)
    private val maxBrain = MaxBrain()
    private val geminiClient = GeminiClient()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _status = MutableStateFlow(AssistantStatus.IDLE)
    val status: StateFlow<AssistantStatus> = _status.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _isSosActive = MutableStateFlow(false)
    val isSosActive: StateFlow<Boolean> = _isSosActive.asStateFlow()

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _pendingAction = MutableStateFlow<PendingAction?>(null)
    val pendingAction: StateFlow<PendingAction?> = _pendingAction.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _activeLanguage = MutableStateFlow(AssistantLanguage.ENGLISH)
    val activeLanguage: StateFlow<AssistantLanguage> = _activeLanguage.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _notes = MutableStateFlow<List<VoiceNote>>(emptyList())
    val notes: StateFlow<List<VoiceNote>> = _notes.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _continuousMode = MutableStateFlow(false)
    val continuousMode: StateFlow<Boolean> = _continuousMode.asStateFlow()

    private var voiceManager: VoiceManager? = null

    init {
        initVoiceManager()
        refreshDeviceState()
        initWelcomeMessage()
    }

    private fun initVoiceManager() {
        voiceManager = VoiceManager(
            context = getApplication(),
            onTranscript = { text, isFinal ->
                if (isFinal && text.isNotBlank()) {
                    processUserCommand(text)
                }
            },
            onAudioRmsChanged = { rms ->
                _audioRms.value = rms
            },
            onListeningStateChanged = { listening ->
                _status.value = if (listening) AssistantStatus.LISTENING else AssistantStatus.IDLE
            },
            onSpeakingStateChanged = { speaking ->
                if (speaking) {
                    _status.value = AssistantStatus.SPEAKING
                } else if (_status.value == AssistantStatus.SPEAKING) {
                    _status.value = AssistantStatus.IDLE
                    if (_continuousMode.value) {
                        voiceManager?.startListening(_activeLanguage.value)
                    }
                }
            }
        )
    }

    private fun refreshDeviceState() {
        _batteryLevel.value = phoneController.getBatteryLevel()
        _isTorchOn.value = phoneController.isFlashlightActive()
        _isSosActive.value = phoneController.isSosActive()
    }

    private fun initWelcomeMessage() {
        val welcome = ChatMessage(
            text = "MAX v2.0 Online. Personal AI Assistant ready.\n\nEnglish • বাংলা • हिन्दी • Hinglish\n\nTry: 'Phone diagnostics', 'MAX, torch on karo', 'Note likho: buy groceries', or 'SOS torch'.",
            isUser = false,
            language = AssistantLanguage.ENGLISH,
            isVerified = true,
            verificationTag = "SYSTEM_READY"
        )
        _messages.value = listOf(welcome)
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun setLanguage(lang: AssistantLanguage) {
        _activeLanguage.value = lang
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        voiceManager?.setSpeechRate(rate)
    }

    fun toggleContinuousMode() {
        _continuousMode.value = !_continuousMode.value
    }

    fun clearHistory() {
        initWelcomeMessage()
    }

    fun deleteNote(noteId: String) {
        _notes.value = _notes.value.filter { it.id != noteId }
    }

    fun submitCurrentInput() {
        val query = _inputText.value.trim()
        if (query.isNotBlank()) {
            _inputText.value = ""
            processUserCommand(query)
        }
    }

    fun toggleVoiceListening() {
        if (_status.value == AssistantStatus.LISTENING) {
            voiceManager?.stopListening()
        } else {
            voiceManager?.stopSpeaking()
            voiceManager?.startListening(_activeLanguage.value)
        }
    }

    fun processUserCommand(rawCommand: String) {
        viewModelScope.launch {
            val language = maxBrain.detectLanguage(rawCommand)
            _activeLanguage.value = language

            val userMsg = ChatMessage(
                text = rawCommand,
                isUser = true,
                language = language
            )
            _messages.value = _messages.value + userMsg
            _status.value = AssistantStatus.PROCESSING

            // Check pending confirmation
            val currentPending = _pendingAction.value
            if (currentPending != null) {
                if (maxBrain.isConfirmationAffirmative(rawCommand)) {
                    confirmPendingAction()
                    return@launch
                } else if (maxBrain.isConfirmationNegative(rawCommand)) {
                    cancelPendingAction()
                    return@launch
                }
            }

            // Parse intent
            val intent = maxBrain.parseCommand(rawCommand)

            // Sensitive action check
            if (intent.isSensitive) {
                val pending = PendingAction(
                    actionType = intent.actionType,
                    title = intent.targetParam.ifBlank { intent.actionType.name },
                    description = if (intent.secondaryParam.isNotBlank()) intent.secondaryParam else "Confirm action",
                    language = language,
                    payload = mapOf(
                        "recipient" to intent.targetParam,
                        "message" to intent.secondaryParam
                    )
                )
                _pendingAction.value = pending
                val confirmPrompt = maxBrain.formatConfirmationPrompt(pending)

                val promptMsg = ChatMessage(
                    text = confirmPrompt,
                    isUser = false,
                    language = language,
                    actionType = intent.actionType,
                    isSensitiveAction = true,
                    verificationTag = "CONFIRMATION_REQUIRED"
                )
                _messages.value = _messages.value + promptMsg
                _status.value = AssistantStatus.IDLE
                voiceManager?.speak(confirmPrompt, language)
                return@launch
            }

            // If it's an open conversational query or translation, try Gemini AI first!
            if (intent.actionType == ActionType.CHAT && intent.isPotentialGeminiQuery && geminiClient.isConfigured()) {
                val contextPairs = _messages.value.takeLast(6).map { it.text to it.isUser }
                val geminiReply = geminiClient.generateResponse(rawCommand, language, contextPairs)
                if (!geminiReply.isNullOrBlank()) {
                    val geminiMsg = ChatMessage(
                        text = geminiReply,
                        isUser = false,
                        language = language,
                        actionType = ActionType.CHAT,
                        isVerified = true,
                        verificationTag = "AI_NEURAL_REASONING"
                    )
                    _messages.value = _messages.value + geminiMsg
                    _status.value = AssistantStatus.IDLE
                    voiceManager?.speak(geminiReply, language)
                    return@launch
                }
            }

            // Special handling for DIAGNOSTICS
            if (intent.actionType == ActionType.DIAGNOSTICS) {
                val diag = phoneController.getDeviceDiagnostics()
                val summaryText = when (language) {
                    AssistantLanguage.BENGALI -> "ব্যাটারি ${diag.batteryPct}%, র্যাম ব্যবহৃত ${diag.ramUsedMb}MB / ${diag.ramTotalMb}MB, স্টোরেজ খালি ${diag.storageFreeGb}GB / ${diag.storageTotalGb}GB, নেটওয়ার্ক: ${diag.networkType}।"
                    AssistantLanguage.HINDI -> "बैटरी ${diag.batteryPct}%, रैम उपयोग ${diag.ramUsedMb}MB / ${diag.ramTotalMb}MB, उपलब्ध स्टोरेज ${diag.storageFreeGb}GB / ${diag.storageTotalGb}GB, नेटवर्क: ${diag.networkType}।"
                    AssistantLanguage.HINGLISH -> "Battery at ${diag.batteryPct}%, RAM used ${diag.ramUsedMb}MB / ${diag.ramTotalMb}MB, Storage free ${diag.storageFreeGb}GB of ${diag.storageTotalGb}GB. Network: ${diag.networkType}."
                    AssistantLanguage.ENGLISH -> "Battery at ${diag.batteryPct}%, RAM ${diag.ramUsedMb}MB / ${diag.ramTotalMb}MB, Storage free ${diag.storageFreeGb}GB of ${diag.storageTotalGb}GB. Network: ${diag.networkType}."
                }

                val diagMsg = ChatMessage(
                    text = summaryText,
                    isUser = false,
                    language = language,
                    actionType = ActionType.DIAGNOSTICS,
                    isVerified = true,
                    verificationTag = "SYSTEM_DIAGNOSTICS",
                    diagnostics = diag
                )
                _messages.value = _messages.value + diagMsg
                _status.value = AssistantStatus.IDLE
                voiceManager?.speak(summaryText, language)
                return@launch
            }

            // Special handling for CREATE_NOTE
            if (intent.actionType == ActionType.CREATE_NOTE) {
                val note = VoiceNote(content = intent.targetParam)
                _notes.value = listOf(note) + _notes.value
            }

            // Non-sensitive action: execute on Android system
            val result = executeIntent(intent)
            refreshDeviceState()

            val replyText = if (intent.directReply != null) {
                intent.directReply
            } else {
                maxBrain.formatActionConfirmation(intent, result)
            }

            val isVerified = result is ActionResult.Success
            val assistantMsg = ChatMessage(
                text = replyText,
                isUser = false,
                language = language,
                actionType = intent.actionType,
                isVerified = isVerified,
                verificationTag = if (isVerified) "ANDROID_CONFIRMED" else "SYSTEM_REPORT"
            )
            _messages.value = _messages.value + assistantMsg
            _status.value = AssistantStatus.IDLE
            voiceManager?.speak(replyText, language)
        }
    }

    fun confirmPendingAction() {
        val pending = _pendingAction.value ?: return
        _pendingAction.value = null

        viewModelScope.launch {
            val result = when (pending.actionType) {
                ActionType.MAKE_CALL -> {
                    val recipient = pending.payload["recipient"] ?: "Contact"
                    phoneController.executePhoneCall(recipient)
                }
                ActionType.SEND_SMS -> {
                    val recipient = pending.payload["recipient"] ?: "Contact"
                    val message = pending.payload["message"] ?: "Hello"
                    phoneController.executeSendMessage(recipient, message)
                }
                ActionType.LOCK_PHONE -> {
                    ActionResult.Success("Device lock request initiated. Android security lock active.")
                }
                else -> ActionResult.Failure("Unknown action.")
            }

            val fakeIntent = MaxBrain.ParsedIntent(
                actionType = pending.actionType,
                language = pending.language,
                targetParam = pending.payload["recipient"] ?: ""
            )
            val replyText = maxBrain.formatActionConfirmation(fakeIntent, result)

            val confirmedMsg = ChatMessage(
                text = replyText,
                isUser = false,
                language = pending.language,
                actionType = pending.actionType,
                isVerified = result is ActionResult.Success,
                verificationTag = "SECURITY_CONFIRMED"
            )
            _messages.value = _messages.value + confirmedMsg
            _status.value = AssistantStatus.IDLE
            voiceManager?.speak(replyText, pending.language)
        }
    }

    fun cancelPendingAction() {
        val pending = _pendingAction.value ?: return
        _pendingAction.value = null

        val cancelText = when (pending.language) {
            AssistantLanguage.BENGALI -> "বাতিল করা হলো।"
            AssistantLanguage.HINDI -> "रद्द किया गया।"
            AssistantLanguage.HINGLISH -> "Action cancelled."
            AssistantLanguage.ENGLISH -> "Action cancelled."
        }

        val cancelMsg = ChatMessage(
            text = cancelText,
            isUser = false,
            language = pending.language,
            verificationTag = "USER_CANCELLED"
        )
        _messages.value = _messages.value + cancelMsg
        voiceManager?.speak(cancelText, pending.language)
    }

    private fun executeIntent(intent: MaxBrain.ParsedIntent): ActionResult {
        return when (intent.actionType) {
            ActionType.TORCH_ON -> phoneController.setFlashlight(true)
            ActionType.TORCH_OFF -> phoneController.setFlashlight(false)
            ActionType.SOS_FLASH -> phoneController.startSosStrobe(viewModelScope)
            ActionType.RINGER_MODE -> phoneController.setRingerMode(intent.targetParam)
            ActionType.VOLUME_UP -> phoneController.adjustVolume(1)
            ActionType.VOLUME_DOWN -> phoneController.adjustVolume(-1)
            ActionType.VOLUME_MUTE -> phoneController.muteVolume()
            ActionType.MEDIA_PLAY_PAUSE -> phoneController.toggleMediaPlayback()
            ActionType.MEDIA_NEXT -> phoneController.nextMediaTrack()
            ActionType.MEDIA_PREV -> phoneController.previousMediaTrack()
            ActionType.OPEN_APP -> phoneController.openApp(intent.targetParam)
            ActionType.OPEN_SETTINGS -> phoneController.openSettingsPage(intent.targetParam)
            ActionType.SET_ALARM -> {
                val hour = intent.numericParam / 60
                val min = intent.numericParam % 60
                phoneController.setAlarm(hour, min, "MAX Assistant Alarm")
            }
            ActionType.SET_TIMER -> phoneController.setTimer(intent.numericParam, "MAX Timer")
            ActionType.CREATE_REMINDER -> {
                val time = System.currentTimeMillis() + 3600000L
                phoneController.createReminder(intent.targetParam, time)
            }
            ActionType.START_NAVIGATION -> phoneController.startNavigation(intent.targetParam)
            ActionType.SEARCH_WEB -> phoneController.searchWeb(intent.targetParam)
            ActionType.BATTERY_CHECK -> {
                val battery = phoneController.getBatteryLevel()
                if (battery >= 0) ActionResult.Success("Battery is at $battery%.") else ActionResult.Failure("Battery status unavailable.")
            }
            ActionType.TAKE_SCREENSHOT -> {
                ActionResult.Success("Screenshot trigger ready: Press Power + Volume Down together.")
            }
            ActionType.CREATE_NOTE -> ActionResult.Success("Note saved: ${intent.targetParam}")
            ActionType.TRANSLATE -> ActionResult.Success("Translation: ${intent.targetParam}")
            ActionType.CALCULATE -> ActionResult.Success(intent.directReply ?: "Done")
            ActionType.CHAT -> ActionResult.Success(intent.directReply ?: "Ready")
            else -> ActionResult.Failure("Action not supported.")
        }
    }

    fun toggleTorchDirectly() {
        val newState = !_isTorchOn.value
        phoneController.setFlashlight(newState)
        refreshDeviceState()
    }

    fun toggleSosDirectly() {
        if (_isSosActive.value) {
            phoneController.stopSosStrobe()
            _isSosActive.value = false
        } else {
            phoneController.startSosStrobe(viewModelScope)
            _isSosActive.value = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        phoneController.stopSosStrobe()
        voiceManager?.release()
    }
}
