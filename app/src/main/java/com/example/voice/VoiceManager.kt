package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.model.AssistantLanguage
import java.util.Locale

class VoiceManager(
    private val context: Context,
    private val onTranscript: (String, Boolean) -> Unit,
    private val onAudioRmsChanged: (Float) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var isCurrentlyListening = false

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                textToSpeech?.language = Locale.ENGLISH
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeakingStateChanged(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeakingStateChanged(false)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onSpeakingStateChanged(false)
                    }
                })
            }
        }
    }

    fun speak(text: String, language: AssistantLanguage) {
        if (!isTtsInitialized || textToSpeech == null) return

        val locale = when (language) {
            AssistantLanguage.BENGALI -> Locale.forLanguageTag("bn-IN")
            AssistantLanguage.HINDI -> Locale.forLanguageTag("hi-IN")
            AssistantLanguage.HINGLISH -> Locale.forLanguageTag("en-IN")
            AssistantLanguage.ENGLISH -> Locale.US
        }

        val result = textToSpeech?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            textToSpeech?.language = Locale.US
        }

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MAX_${System.currentTimeMillis()}")
        }
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "MAX_MSG")
    }

    fun setSpeechRate(rate: Float) {
        textToSpeech?.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        textToSpeech?.setPitch(pitch)
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        onSpeakingStateChanged(false)
    }

    fun startListening(preferredLang: AssistantLanguage = AssistantLanguage.ENGLISH) {
        if (isCurrentlyListening) {
            stopListening()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onTranscript("Speech recognition service not available on this device.", true)
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isCurrentlyListening = true
                        onListeningStateChanged(true)
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        onAudioRmsChanged(rmsdB)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isCurrentlyListening = false
                        onListeningStateChanged(false)
                    }

                    override fun onError(error: Int) {
                        isCurrentlyListening = false
                        onListeningStateChanged(false)
                    }

                    override fun onResults(results: Bundle?) {
                        isCurrentlyListening = false
                        onListeningStateChanged(false)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onTranscript(matches[0], true)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onTranscript(matches[0], false)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val langCode = when (preferredLang) {
                AssistantLanguage.BENGALI -> "bn-IN"
                AssistantLanguage.HINDI -> "hi-IN"
                AssistantLanguage.HINGLISH -> "hi-Latn"
                AssistantLanguage.ENGLISH -> "en-US"
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {
            isCurrentlyListening = false
            onListeningStateChanged(false)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        isCurrentlyListening = false
        onListeningStateChanged(false)
    }

    fun release() {
        try {
            speechRecognizer?.destroy()
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
    }
}
