package com.example.engine

import com.example.model.ActionCategory
import com.example.model.ActionResult
import com.example.model.ActionType
import com.example.model.AssistantLanguage
import com.example.model.PendingAction
import java.util.Locale
import java.util.regex.Pattern

class MaxBrain {

    data class ParsedIntent(
        val actionType: ActionType,
        val language: AssistantLanguage,
        val targetParam: String = "",
        val secondaryParam: String = "",
        val numericParam: Int = 0,
        val isSensitive: Boolean = false,
        val directReply: String? = null,
        val isPotentialGeminiQuery: Boolean = false
    )

    fun detectLanguage(input: String): AssistantLanguage {
        val bengaliCharCount = input.count { it in '\u0980'..'\u09FF' }
        val devanagariCharCount = input.count { it in '\u0900'..'\u097F' }

        if (bengaliCharCount > 1) return AssistantLanguage.BENGALI
        if (devanagariCharCount > 1) return AssistantLanguage.HINDI

        val lower = input.lowercase()
        val hinglishWords = listOf(
            "kholo", "chalu", "karo", "band", "batao", "kaise", "kya", "bhai", "mera", "meri",
            "lagao", "badhao", "kam", "sunao", "dekh", "hai", "mujhe", "tum", "aap", "bolo", "hoga", "shuru"
        )
        val words = lower.split(Regex("\\s+"))
        val hinglishMatch = words.any { it in hinglishWords }
        if (hinglishMatch) return AssistantLanguage.HINGLISH

        return AssistantLanguage.ENGLISH
    }

    fun isConfirmationAffirmative(text: String): Boolean {
        val lower = text.trim().lowercase()
        return lower in listOf(
            "yes", "yep", "yeah", "confirm", "ok", "okay", "sure", "proceed",
            "haan", "ha", "han", "kardo", "thik hai", "theek hai",
            "হাঁ", "হ্যাঁ", "ঠিক আছে", "করুন", "হ্যা",
            "हाँ", "हा", "हाँ कर दो", "कन्फर्म", "ठीक है", "करो"
        )
    }

    fun isConfirmationNegative(text: String): Boolean {
        val lower = text.trim().lowercase()
        return lower in listOf(
            "no", "cancel", "stop", "never mind", "abort", "nope",
            "nahi", "mat karo", "rok do", "cancel karo",
            "না", "করবেন না", "বাতিল", "বন্ধ করো",
            "नहीं", "मत करो", "रोक दो", "रद्द करो"
        )
    }

    fun parseCommand(rawText: String): ParsedIntent {
        val clean = rawText.trim()
        val language = detectLanguage(clean)
        val lower = clean.lowercase()

        // Strip wake phrase if present
        val stripped = lower
            .replace(Regex("^(max|hey max|ok max|ম্যাক্স|मैक्स)[,\\s]+"), "")
            .trim()

        // 1. SECURITY CHECKS: Never bypass lock, never guess PIN/password
        if (containsAny(stripped, "bypass pin", "bypass password", "hack", "crack pin", "unlock without pin", "password batao", "pin batao", "পাসওয়ার্ড", "লক বাইপাস")) {
            val response = when (language) {
                AssistantLanguage.BENGALI -> "অ্যান্ড্রয়েড নিরাপত্তা নীতি অনুসারে পাসওয়ার্ড বা লক বাইপাস করা সম্ভব নয়।"
                AssistantLanguage.HINDI -> "एंड्रॉइड सुरक्षा नीति के अनुसार पासवर्ड या स्क्रीन लॉक को बायपास नहीं किया जा सकता।"
                AssistantLanguage.HINGLISH -> "Android security ke tehat PIN ya password bypass karna strictly restricted hai."
                AssistantLanguage.ENGLISH -> "I cannot bypass Android lock-screen security, reveal credentials, or perform unauthorized operations."
            }
            return ParsedIntent(ActionType.CHAT, language, directReply = response)
        }

        // 2. LOCK PHONE
        if (containsAny(stripped, "lock phone", "lock screen", "phone lock karo", "phone lock", "ফোন লক করো", "स्क्रीन लॉक करो")) {
            return ParsedIntent(ActionType.LOCK_PHONE, language, isSensitive = true)
        }

        // 3. SOS / EMERGENCY STROBE
        if (containsAny(stripped, "sos", "emergency light", "strobe", "এসওএস", "বিপদ বাতি", "एसओएस")) {
            return ParsedIntent(ActionType.SOS_FLASH, language)
        }

        // 4. FLASHLIGHT / TORCH
        if (containsAny(stripped, "torch on", "flashlight on", "turn on torch", "turn on flashlight", "torch chalu", "টর্চ অন", "টর্চ চালু", "টর্চ জ্বালাও", "टॉर्च ऑन", "टॉर्च चालू", "टॉर्च जलाओ")) {
            return ParsedIntent(ActionType.TORCH_ON, language)
        }
        if (containsAny(stripped, "torch off", "flashlight off", "turn off torch", "turn off flashlight", "torch band", "টর্চ অফ", "টর্চ বন্ধ", "টর্চ নেভাও", "टॉर्च बंद", "टॉर्च ऑफ", "टॉर्च बुझाओ")) {
            return ParsedIntent(ActionType.TORCH_OFF, language)
        }

        // 5. PHONE CALLS (SENSITIVE)
        val callPatternEn = Regex("call\\s+([a-zA-Z0-9\\+\\s]+)")
        val callPatternHinglish = Regex("([a-zA-Z0-9\\s]+)\\s+ko\\s+call\\s*(karo|lagao)?")
        val callPatternBn = Regex("([^\n\r]+?)\\s*(কে|ক)\\s*ফোন\\s*(করো|করুন|দাও)?")
        val callPatternHi = Regex("([^\n\r]+?)\\s*(को)\\s*कॉल\\s*(करो|लगाओ)?")

        if (callPatternHinglish.containsMatchIn(stripped) && containsAny(stripped, "call", "phone")) {
            val match = callPatternHinglish.find(stripped)
            val name = match?.groupValues?.get(1)?.trim() ?: "Contact"
            return ParsedIntent(ActionType.MAKE_CALL, language, targetParam = name, isSensitive = true)
        }
        if (callPatternBn.containsMatchIn(stripped)) {
            val match = callPatternBn.find(stripped)
            val name = match?.groupValues?.get(1)?.trim() ?: "যোগাযোগ"
            return ParsedIntent(ActionType.MAKE_CALL, language, targetParam = name, isSensitive = true)
        }
        if (callPatternHi.containsMatchIn(stripped)) {
            val match = callPatternHi.find(stripped)
            val name = match?.groupValues?.get(1)?.trim() ?: "संपर्क"
            return ParsedIntent(ActionType.MAKE_CALL, language, targetParam = name, isSensitive = true)
        }
        if (callPatternEn.containsMatchIn(stripped)) {
            val match = callPatternEn.find(stripped)
            val name = match?.groupValues?.get(1)?.trim() ?: "Contact"
            return ParsedIntent(ActionType.MAKE_CALL, language, targetParam = name, isSensitive = true)
        }

        // 6. SEND SMS (SENSITIVE)
        if (containsAny(stripped, "send message", "send sms", "message bhejo", "sms bhejo", "মেসেজ পাঠাও", "বার্তা পাঠাও", "संदेश भेजो")) {
            val recipient = "Contact"
            val body = stripped.replace(Regex(".*(message|sms|বার্তা|संदेश)\\s*"), "").trim()
            return ParsedIntent(ActionType.SEND_SMS, language, targetParam = recipient, secondaryParam = body.ifBlank { "Hello" }, isSensitive = true)
        }

        // 7. DIAGNOSTICS & SYSTEM HEALTH
        if (containsAny(stripped, "diagnostic", "device status", "phone status", "ram", "storage", "স্ট্যাটাস", "ফোন স্ট্যাটাস", "स्थिति", "फोन स्थिति", "system health", "phone health")) {
            return ParsedIntent(ActionType.DIAGNOSTICS, language)
        }

        // 8. RINGER / SOUND MODES
        if (containsAny(stripped, "silent mode", "mute phone", "সাইলেন্ট", "साइलेंट")) {
            return ParsedIntent(ActionType.RINGER_MODE, language, targetParam = "silent")
        }
        if (containsAny(stripped, "vibrate mode", "vibration mode", "ভাইব্রেট", "वाइब्रेट")) {
            return ParsedIntent(ActionType.RINGER_MODE, language, targetParam = "vibrate")
        }
        if (containsAny(stripped, "normal mode", "ring mode", "নরমাল মোড", "नॉर्मल मोड")) {
            return ParsedIntent(ActionType.RINGER_MODE, language, targetParam = "normal")
        }

        // 9. VOLUME CONTROLS
        if (containsAny(stripped, "volume up", "sound badhao", "awaz badhao", "ভলিউম বাড়াও", "আওয়াজ বাড়াও", "वॉल्यूम बढ़ाओ", "आवाज बढ़ाओ")) {
            return ParsedIntent(ActionType.VOLUME_UP, language)
        }
        if (containsAny(stripped, "volume down", "sound kam karo", "awaz kam", "ভলিউম কমাও", "আওয়াজ কমাও", "वॉल्यूम कम करो", "आवाज कम करो")) {
            return ParsedIntent(ActionType.VOLUME_DOWN, language)
        }
        if (containsAny(stripped, "mute", "volume mute", "chup", "নিঃশব্দ", "म्यूट")) {
            return ParsedIntent(ActionType.VOLUME_MUTE, language)
        }

        // 10. MEDIA PLAYBACK
        if (containsAny(stripped, "play", "pause", "gan bajao", "gana chalu", "gaan chalao", "গান চালাও", "গান থামাও", "गाना चलाओ", "गाना रोको")) {
            return ParsedIntent(ActionType.MEDIA_PLAY_PAUSE, language)
        }
        if (containsAny(stripped, "next song", "next track", "agla gana", "পরের গান", "अगला गाना")) {
            return ParsedIntent(ActionType.MEDIA_NEXT, language)
        }
        if (containsAny(stripped, "previous song", "prev song", "pichhla gana", "আগের গান", "पिछला गाना")) {
            return ParsedIntent(ActionType.MEDIA_PREV, language)
        }

        // 11. TAKE SCREENSHOT
        if (containsAny(stripped, "screenshot", "screen shot", "স্ক্রিনশট", "स्क्रीनशॉट")) {
            return ParsedIntent(ActionType.TAKE_SCREENSHOT, language)
        }

        // 12. BATTERY CHECK
        if (containsAny(stripped, "battery", "charge koto", "battery kitni", "চার্জ কত", "बैटरी कितनी")) {
            return ParsedIntent(ActionType.BATTERY_CHECK, language)
        }

        // 13. ALARM
        val alarmPattern = Regex("(\\d{1,2})(:(\\d{2}))?\\s*(am|pm|am|pm)?", RegexOption.IGNORE_CASE)
        if (containsAny(stripped, "alarm", "অ্যালার্ম", "अलार्म")) {
            val match = alarmPattern.find(stripped)
            var hour = 7
            var min = 0
            if (match != null) {
                val h = match.groupValues[1].toIntOrNull() ?: 7
                val m = match.groupValues[3].toIntOrNull() ?: 0
                val ampm = match.groupValues[4].lowercase()
                hour = if (ampm == "pm" && h < 12) h + 12 else if (ampm == "am" && h == 12) 0 else h
                min = m
            }
            return ParsedIntent(ActionType.SET_ALARM, language, numericParam = hour * 60 + min)
        }

        // 14. TIMER
        val timerPattern = Regex("(\\d+)\\s*(min|minute|sec|second|মিনিট|সেকেন্ড|मिनट|सेकंड)")
        if (containsAny(stripped, "timer", "টাইমার", "टाइमर")) {
            val match = timerPattern.find(stripped)
            val num = match?.groupValues?.get(1)?.toIntOrNull() ?: 5
            val unit = match?.groupValues?.get(2)?.lowercase() ?: "min"
            val totalSec = if (unit.startsWith("sec") || unit.contains("সেকেন্ড") || unit.contains("सेकंड")) num else num * 60
            return ParsedIntent(ActionType.SET_TIMER, language, numericParam = totalSec)
        }

        // 15. CREATE NOTE
        if (containsAny(stripped, "note likho", "take note", "save note", "create note", "নোট লেখো", "নোট নাও", "नोट लिखो")) {
            val content = stripped
                .replace(Regex("^(take note|note likho|save note|create note|নোট লেখো|নোট নাও|नोट लिखो)\\s*(that|ki|যে)?\\s*"), "")
                .trim()
            return ParsedIntent(ActionType.CREATE_NOTE, language, targetParam = content.ifBlank { "Important reminder note" })
        }

        // 16. TRANSLATE
        if (containsAny(stripped, "translate", "অনুবাদ", "अनुवाद")) {
            val query = stripped.replace(Regex("^(translate|অনুবাদ করো|अनुवाद करो)\\s*"), "").trim()
            return ParsedIntent(ActionType.TRANSLATE, language, targetParam = query, isPotentialGeminiQuery = true)
        }

        // 17. NAVIGATION
        if (containsAny(stripped, "navigate to", "take me to", "rasta", "kahan hai", "রাস্তা", "পথ", "रास्ता")) {
            val dest = stripped
                .replace(Regex("^(navigate to|take me to|directions to|রাস্তা দেখাও|रास्ता दिखाओ)\\s*"), "")
                .replace("ka rasta", "")
                .trim()
            return ParsedIntent(ActionType.START_NAVIGATION, language, targetParam = dest.ifBlank { "Home" })
        }

        // 18. SETTINGS
        if (containsAny(stripped, "wifi", "wi-fi", "ওয়াইফাই", "वाईफाई")) {
            return ParsedIntent(ActionType.OPEN_SETTINGS, language, targetParam = "wifi")
        }
        if (containsAny(stripped, "bluetooth", "ব্লুটুথ", "ब्लूटूथ")) {
            return ParsedIntent(ActionType.OPEN_SETTINGS, language, targetParam = "bluetooth")
        }
        if (containsAny(stripped, "setting", "সেটিংস", "सेटिंग")) {
            return ParsedIntent(ActionType.OPEN_SETTINGS, language, targetParam = "general")
        }

        // 19. OPEN APP
        val appList = listOf("youtube", "camera", "chrome", "maps", "whatsapp", "calculator", "clock", "gmail", "spotify")
        for (app in appList) {
            if (stripped.contains(app)) {
                return ParsedIntent(ActionType.OPEN_APP, language, targetParam = app)
            }
        }
        if (containsAny(stripped, "ইউটিউব", "यूट्यूब")) return ParsedIntent(ActionType.OPEN_APP, language, targetParam = "youtube")
        if (containsAny(stripped, "ক্যামেরা", "कैमरा")) return ParsedIntent(ActionType.OPEN_APP, language, targetParam = "camera")
        if (containsAny(stripped, "ক্যালকুলেটর", "कैलकुलेटर")) return ParsedIntent(ActionType.OPEN_APP, language, targetParam = "calculator")

        // 20. CALCULATE / MATH
        val mathResult = tryEvaluateMath(stripped)
        if (mathResult != null) {
            return ParsedIntent(ActionType.CALCULATE, language, directReply = mathResult)
        }

        // 21. WEB SEARCH
        if (containsAny(stripped, "search for", "google for", "গুগলে সার্চ করো", "सर्च करो")) {
            val query = stripped.replace(Regex("^(search for|search|google for|গুগলে সার্চ করো|सर्च करो)\\s*"), "").trim()
            return ParsedIntent(ActionType.SEARCH_WEB, language, targetParam = query)
        }

        // 22. CONVERSATIONAL / CHAT / GEMINI REASONING
        val isComplexQuery = stripped.length > 25 || containsAny(stripped, "why", "how", "explain", "who", "tell me", "poem", "joke", "code", "কেন", "কিভাবে", "বোঝাও", "কবিতা", "কৌতুক", "क्यों", "कैसे", "समझाओ", "कविता", "चुटकुला")
        val chatResponse = generateConversationalResponse(stripped, language)
        return ParsedIntent(ActionType.CHAT, language, directReply = chatResponse, isPotentialGeminiQuery = isComplexQuery)
    }

    fun formatConfirmationPrompt(action: PendingAction): String {
        return when (action.actionType) {
            ActionType.MAKE_CALL -> {
                val target = action.payload["recipient"] ?: "Contact"
                when (action.language) {
                    AssistantLanguage.BENGALI -> "$target কে কল করবেন? নিশ্চিত করুন।"
                    AssistantLanguage.HINDI -> "$target को कॉल करें? कन्फर्म करें?"
                    AssistantLanguage.HINGLISH -> "Calling $target. Confirm?"
                    AssistantLanguage.ENGLISH -> "Calling $target. Confirm?"
                }
            }
            ActionType.SEND_SMS -> {
                val recipient = action.payload["recipient"] ?: "Contact"
                val msg = action.payload["message"] ?: ""
                when (action.language) {
                    AssistantLanguage.BENGALI -> "$recipient কে বার্তা পাঠাবেন: '$msg'? নিশ্চিত করুন।"
                    AssistantLanguage.HINDI -> "$recipient को संदेश भेजें: '$msg'? कन्फर्म करें?"
                    AssistantLanguage.HINGLISH -> "Send message to $recipient: '$msg'? Confirm?"
                    AssistantLanguage.ENGLISH -> "Send message to $recipient: '$msg'? Confirm?"
                }
            }
            ActionType.LOCK_PHONE -> {
                when (action.language) {
                    AssistantLanguage.BENGALI -> "ফোন লক করবেন? নিশ্চিত করুন।"
                    AssistantLanguage.HINDI -> "फोन लॉक करना चाहते हैं? पुष्टि करें।"
                    AssistantLanguage.HINGLISH -> "Lock device screen? Confirm?"
                    AssistantLanguage.ENGLISH -> "Lock device screen? Confirm?"
                }
            }
            else -> "Confirm action?"
        }
    }

    fun formatActionConfirmation(intent: ParsedIntent, result: ActionResult): String {
        return when (result) {
            is ActionResult.Success -> {
                when (intent.actionType) {
                    ActionType.TORCH_ON -> when (intent.language) {
                        AssistantLanguage.BENGALI -> "টর্চ চালু করা হয়েছে।"
                        AssistantLanguage.HINDI -> "टॉर्च चालू कर दी गई है।"
                        AssistantLanguage.HINGLISH -> "Torch on kar diya."
                        AssistantLanguage.ENGLISH -> "Flashlight turned on."
                    }
                    ActionType.TORCH_OFF -> when (intent.language) {
                        AssistantLanguage.BENGALI -> "টর্চ বন্ধ করা হয়েছে।"
                        AssistantLanguage.HINDI -> "टॉर्च बंद कर दी गई है।"
                        AssistantLanguage.HINGLISH -> "Torch off kar diya."
                        AssistantLanguage.ENGLISH -> "Flashlight turned off."
                    }
                    ActionType.SOS_FLASH -> when (intent.language) {
                        AssistantLanguage.BENGALI -> "জরুরি এসওএস বীকন চালু হয়েছে।"
                        AssistantLanguage.HINDI -> "आपातकालीन एसओएस बीकन सक्रिय है।"
                        AssistantLanguage.HINGLISH -> "Emergency SOS beacon active."
                        AssistantLanguage.ENGLISH -> "Emergency SOS beacon activated."
                    }
                    ActionType.RINGER_MODE -> when (intent.language) {
                        AssistantLanguage.BENGALI -> "শব্দ মোড পরিবর্তিত হয়েছে।"
                        AssistantLanguage.HINDI -> "साउंड मोड बदल दिया गया है।"
                        AssistantLanguage.HINGLISH -> "Ringer mode updated."
                        AssistantLanguage.ENGLISH -> result.message
                    }
                    ActionType.VOLUME_UP, ActionType.VOLUME_DOWN, ActionType.VOLUME_MUTE -> when (intent.language) {
                        AssistantLanguage.BENGALI -> "ভলিউম সমন্বয় করা হয়েছে।"
                        AssistantLanguage.HINDI -> "वॉल्यूम समायोजित किया गया।"
                        AssistantLanguage.HINGLISH -> "Volume updated."
                        AssistantLanguage.ENGLISH -> result.message
                    }
                    ActionType.MEDIA_PLAY_PAUSE -> when (intent.language) {
                        AssistantLanguage.BENGALI -> "মিডিয়া চালানো বা থামানো হলো।"
                        AssistantLanguage.HINDI -> "मीडिया प्ले/पॉज़ आदेश भेजा गया।"
                        AssistantLanguage.HINGLISH -> "Media toggled."
                        AssistantLanguage.ENGLISH -> "Media playback toggled."
                    }
                    ActionType.OPEN_APP -> when (intent.language) {
                        AssistantLanguage.BENGALI -> "${intent.targetParam} খোলা হচ্ছে।"
                        AssistantLanguage.HINDI -> "${intent.targetParam} खोला जा रहा है।"
                        AssistantLanguage.HINGLISH -> "Opening ${intent.targetParam}."
                        AssistantLanguage.ENGLISH -> "Opening ${intent.targetParam}."
                    }
                    ActionType.SET_ALARM -> when (intent.language) {
                        AssistantLanguage.BENGALI -> "অ্যালার্ম নির্ধারণ করা হয়েছে।"
                        AssistantLanguage.HINDI -> "अलार्म सेट कर दिया गया है।"
                        AssistantLanguage.HINGLISH -> "Alarm set."
                        AssistantLanguage.ENGLISH -> result.message
                    }
                    ActionType.SET_TIMER -> when (intent.language) {
                        AssistantLanguage.BENGALI -> "টাইমার শুরু করা হয়েছে।"
                        AssistantLanguage.HINDI -> "टाइमर शुरू कर दिया गया है।"
                        AssistantLanguage.HINGLISH -> "Timer started."
                        AssistantLanguage.ENGLISH -> result.message
                    }
                    ActionType.CREATE_NOTE -> when (intent.language) {
                        AssistantLanguage.BENGALI -> "নোট সংরক্ষণ করা হয়েছে।"
                        AssistantLanguage.HINDI -> "नोट सुरक्षित कर लिया गया है।"
                        AssistantLanguage.HINGLISH -> "Note saved."
                        AssistantLanguage.ENGLISH -> "Note saved to assistant memory."
                    }
                    ActionType.DIAGNOSTICS -> when (intent.language) {
                        AssistantLanguage.BENGALI -> "ডিভাইস ডায়াগনস্টিক রিপোর্ট প্রস্তুত।"
                        AssistantLanguage.HINDI -> "डिवाइस डायग्नोस्टिक रिपोर्ट तैयार है।"
                        AssistantLanguage.HINGLISH -> "Device diagnostic report ready."
                        AssistantLanguage.ENGLISH -> "Device system report generated."
                    }
                    else -> result.message
                }
            }
            is ActionResult.Failure -> {
                when (intent.language) {
                    AssistantLanguage.BENGALI -> "অ্যাকশন সম্পন্ন করা যায়নি: ${result.reason}"
                    AssistantLanguage.HINDI -> "कार्रवाई पूरी नहीं हो सकी: ${result.reason}"
                    AssistantLanguage.HINGLISH -> "Could not complete action: ${result.reason}"
                    AssistantLanguage.ENGLISH -> "Unable to complete action: ${result.reason}"
                }
            }
            is ActionResult.PermissionNeeded -> {
                when (intent.language) {
                    AssistantLanguage.BENGALI -> "এই কাজের জন্য অনুমতি প্রয়োজন: ${result.permission}"
                    AssistantLanguage.HINDI -> "इसके लिए अनुमति आवश्यक है: ${result.permission}"
                    AssistantLanguage.HINGLISH -> "Permission required: ${result.permission}"
                    AssistantLanguage.ENGLISH -> "Permission needed: ${result.permission}"
                }
            }
            is ActionResult.RequiresConfirmation -> result.confirmationPrompt
        }
    }

    private fun generateConversationalResponse(query: String, language: AssistantLanguage): String {
        return when {
            containsAny(query, "hello", "hi", "namaste", "nomoshkar", "নমস্কার", "হ্যালো", "नमस्ते") -> {
                when (language) {
                    AssistantLanguage.BENGALI -> "নমস্কার। আমি ম্যাক্স, আপনার ব্যক্তিগত এআই সহকারী। বলুন কিভাবে সাহায্য করতে পারি?"
                    AssistantLanguage.HINDI -> "नमस्ते। मैं मैक्स हूँ, आपका निजी स्मार्ट सहायक। आज क्या आदेश है?"
                    AssistantLanguage.HINGLISH -> "Hello! MAX here. Ready for your command."
                    AssistantLanguage.ENGLISH -> "Hello. MAX online and ready. What can I do for you?"
                }
            }
            containsAny(query, "who are you", "tum kaun ho", "tumi ke", "তুমি কে", "तुम कौन हो") -> {
                when (language) {
                    AssistantLanguage.BENGALI -> "আমি ম্যাক্স, আপনার ব্যক্তিগত এআই ভয়েস সহকারী।"
                    AssistantLanguage.HINDI -> "मैं मैक्स हूँ, आपका निजी स्मार्ट एআই वॉयस असिस्टेंट।"
                    AssistantLanguage.HINGLISH -> "I am MAX, your personal smartphone AI voice assistant."
                    AssistantLanguage.ENGLISH -> "I am MAX, your personal smartphone AI voice assistant."
                }
            }
            containsAny(query, "how are you", "kemon acho", "kaise ho", "কেমন আছো", "कैसे हो") -> {
                when (language) {
                    AssistantLanguage.BENGALI -> "আমি দুর্দান্ত আছি, সিস্টেম সম্পূর্ণ সচল। আপনার কি প্রয়োজন?"
                    AssistantLanguage.HINDI -> "सभी सिस्टम पूरी तरह सक्रिय हैं। बताइए क्या काम है?"
                    AssistantLanguage.HINGLISH -> "All systems running fast and calm. How can I help?"
                    AssistantLanguage.ENGLISH -> "Operating at peak efficiency. Ready for your command."
                }
            }
            else -> {
                when (language) {
                    AssistantLanguage.BENGALI -> "আমি প্রস্তুত। অ্যাপ খোলা, টর্চ, কল, অ্যালার্ম, নোট নেওয়া বা যেকোনো তথ্য জানতে পারেন।"
                    AssistantLanguage.HINDI -> "आदेश दें। मैं ऐप्स, टॉर्च, कॉल, टाइमर, नोट्स और सेटिंग्स नियंत्रित कर सकता हूँ।"
                    AssistantLanguage.HINGLISH -> "Understood. Tell me to open apps, toggle torch, make calls, take notes, or check diagnostics."
                    AssistantLanguage.ENGLISH -> "Ready. I can launch apps, control device toggles, manage alarms, save notes, or diagnose device."
                }
            }
        }
    }

    private fun tryEvaluateMath(input: String): String? {
        val expr = input
            .replace("calculate", "")
            .replace("what is", "")
            .replace("math", "")
            .replace("গুণ", "*")
            .replace("ভাগ", "/")
            .replace("যোগ", "+")
            .replace("বিয়োগ", "-")
            .replace("गुना", "*")
            .replace("into", "*")
            .replace("divided by", "/")
            .replace("plus", "+")
            .replace("minus", "-")
            .replace("percent of", "%")
            .trim()

        val pctMatcher = Regex("([0-9.]+)\\s*%\\s*([0-9.]+)").find(expr)
        if (pctMatcher != null) {
            val pct = pctMatcher.groupValues[1].toDoubleOrNull() ?: return null
            val total = pctMatcher.groupValues[2].toDoubleOrNull() ?: return null
            val result = (pct / 100.0) * total
            return "$pct% of $total = $result"
        }

        val basicMatcher = Regex("([0-9.]+)\\s*([+*\\/-])\\s*([0-9.]+)").find(expr)
        if (basicMatcher != null) {
            val a = basicMatcher.groupValues[1].toDoubleOrNull() ?: return null
            val op = basicMatcher.groupValues[2]
            val b = basicMatcher.groupValues[3].toDoubleOrNull() ?: return null
            val res = when (op) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> if (b != 0.0) a / b else return "Division by zero."
                else -> return null
            }
            val formattedRes = if (res % 1.0 == 0.0) res.toLong().toString() else String.format(Locale.US, "%.2f", res)
            val formattedA = if (a % 1.0 == 0.0) a.toLong().toString() else a.toString()
            val formattedB = if (b % 1.0 == 0.0) b.toLong().toString() else b.toString()
            return "$formattedA $op $formattedB = $formattedRes"
        }
        return null
    }

    private fun containsAny(text: String, vararg targets: String): Boolean {
        return targets.any { text.contains(it, ignoreCase = true) }
    }
}
