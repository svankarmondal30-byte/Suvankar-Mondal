package com.example.model

enum class AssistantLanguage(val displayName: String, val code: String, val flag: String) {
    ENGLISH("English", "en", "🇺🇸"),
    BENGALI("বাংলা", "bn", "🇧🇩"),
    HINDI("हिन्दी", "hi", "🇮🇳"),
    HINGLISH("Hinglish", "hi-Latn", "🌐")
}

enum class AssistantStatus {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING
}

enum class ActionCategory {
    APP_CONTROL,
    HARDWARE,
    MEDIA,
    COMMUNICATION,
    PRODUCTIVITY,
    NAVIGATION,
    INFORMATION,
    SECURITY
}

enum class ActionType(val category: ActionCategory, val isSensitive: Boolean) {
    OPEN_APP(ActionCategory.APP_CONTROL, false),
    TORCH_ON(ActionCategory.HARDWARE, false),
    TORCH_OFF(ActionCategory.HARDWARE, false),
    SOS_FLASH(ActionCategory.HARDWARE, false),
    VOLUME_UP(ActionCategory.HARDWARE, false),
    VOLUME_DOWN(ActionCategory.HARDWARE, false),
    VOLUME_MUTE(ActionCategory.HARDWARE, false),
    RINGER_MODE(ActionCategory.HARDWARE, false),
    MEDIA_PLAY_PAUSE(ActionCategory.MEDIA, false),
    MEDIA_NEXT(ActionCategory.MEDIA, false),
    MEDIA_PREV(ActionCategory.MEDIA, false),
    SET_ALARM(ActionCategory.PRODUCTIVITY, false),
    SET_TIMER(ActionCategory.PRODUCTIVITY, false),
    CREATE_REMINDER(ActionCategory.PRODUCTIVITY, false),
    CREATE_NOTE(ActionCategory.PRODUCTIVITY, false),
    MAKE_CALL(ActionCategory.COMMUNICATION, true),
    SEND_SMS(ActionCategory.COMMUNICATION, true),
    START_NAVIGATION(ActionCategory.NAVIGATION, false),
    SEARCH_WEB(ActionCategory.INFORMATION, false),
    CALCULATE(ActionCategory.INFORMATION, false),
    TRANSLATE(ActionCategory.INFORMATION, false),
    OPEN_SETTINGS(ActionCategory.HARDWARE, false),
    TAKE_SCREENSHOT(ActionCategory.HARDWARE, false),
    BATTERY_CHECK(ActionCategory.INFORMATION, false),
    DIAGNOSTICS(ActionCategory.INFORMATION, false),
    LOCK_PHONE(ActionCategory.SECURITY, true),
    CHAT(ActionCategory.INFORMATION, false)
}

data class DeviceDiagnostics(
    val batteryPct: Int,
    val isCharging: Boolean,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val storageFreeGb: Double,
    val storageTotalGb: Double,
    val networkType: String,
    val androidVersion: String,
    val deviceModel: String
)

data class VoiceNote(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class ActionResult {
    data class Success(val message: String, val detail: String? = null) : ActionResult()
    data class RequiresConfirmation(
        val confirmationPrompt: String,
        val actionType: ActionType,
        val targetName: String,
        val payload: Map<String, String>
    ) : ActionResult()
    data class PermissionNeeded(val permission: String, val message: String) : ActionResult()
    data class Failure(val reason: String) : ActionResult()
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val language: AssistantLanguage = AssistantLanguage.ENGLISH,
    val actionType: ActionType? = null,
    val isVerified: Boolean = false,
    val verificationTag: String? = null,
    val isSensitiveAction: Boolean = false,
    val diagnostics: DeviceDiagnostics? = null
)

data class PendingAction(
    val actionType: ActionType,
    val title: String,
    val description: String,
    val language: AssistantLanguage,
    val payload: Map<String, String>
)
