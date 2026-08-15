package com.readaloud.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.readaloud.app.model.AppSettings
import com.readaloud.app.model.MessageLanguage
import com.readaloud.app.model.ReadAloudMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

data class VoiceAvailability(
    val locale: Locale,
    val label: String,
    val availableOffline: Boolean
)

data class SpeechState(
    val initialized: Boolean = false,
    val currentMessageId: String? = null,
    val isSpeaking: Boolean = false,
    val isPaused: Boolean = false,
    val error: String? = null,
    val voices: List<VoiceAvailability> = emptyList()
)

class TtsManager(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tts: TextToSpeech? = null
    private var queue: List<ReadAloudMessage> = emptyList()
    private var queueIndex: Int = 0
    private var queueSettings: AppSettings = AppSettings()
    private var lastMessage: ReadAloudMessage? = null
    private var paused = false

    private val _state = MutableStateFlow(SpeechState())
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    init {
        tts = TextToSpeech(appContext) { status ->
            scope.launch {
                if (status == TextToSpeech.SUCCESS) {
                    configureListener()
                    _state.value = _state.value.copy(
                        initialized = true,
                        voices = checkVoices(),
                        error = null
                    )
                } else {
                    _state.value = _state.value.copy(
                        initialized = false,
                        error = "Android Text-to-Speech could not start."
                    )
                }
            }
        }
    }

    fun refreshVoices() {
        _state.value = _state.value.copy(voices = checkVoices())
    }

    fun speak(message: ReadAloudMessage, settings: AppSettings) {
        queue = emptyList()
        paused = false
        queueSettings = settings
        speakInternal(message, settings, flush = true)
    }

    fun playAll(messages: List<ReadAloudMessage>, settings: AppSettings) {
        if (messages.isEmpty()) return
        queue = messages
        queueIndex = 0
        queueSettings = settings
        paused = false
        speakInternal(messages.first(), settings, flush = true)
    }

    fun pause() {
        if (!_state.value.isSpeaking) return
        paused = true
        tts?.stop()
        _state.value = _state.value.copy(isSpeaking = false, isPaused = true)
    }

    fun resume() {
        if (!_state.value.isPaused) return
        paused = false
        val message = if (queue.isNotEmpty()) queue.getOrNull(queueIndex) else null
        val resumable = message ?: lastMessage
        if (resumable != null) {
            speakInternal(resumable, queueSettings, flush = true)
        }
    }

    fun stop() {
        paused = false
        queue = emptyList()
        queueIndex = 0
        tts?.stop()
        _state.value = _state.value.copy(
            currentMessageId = null,
            isSpeaking = false,
            isPaused = false,
            error = null
        )
    }

    fun testVoice(language: MessageLanguage, settings: AppSettings) {
        val sample = when (language) {
            MessageLanguage.HINDI -> "नमस्ते, ReadAloud हिंदी आवाज तैयार है।"
            MessageLanguage.GUJARATI -> "નમસ્તે, ReadAloud ગુજરાતી અવાજ તૈયાર છે."
            else -> "Hello, ReadAloud English voice is ready."
        }
        speak(
            ReadAloudMessage(
                originalText = sample,
                processedText = sample,
                language = language,
                locale = language.locale
            ),
            settings
        )
    }

    fun installVoiceDataIntent(): Intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun speakInternal(message: ReadAloudMessage, settings: AppSettings, flush: Boolean) {
        val engine = tts ?: return
        if (!_state.value.initialized) {
            _state.value = _state.value.copy(error = "Text-to-Speech is still starting.")
            return
        }
        val languageReady = prepareOfflineVoice(engine, message.locale)
        if (!languageReady) {
            _state.value = _state.value.copy(
                error = "Offline ${message.language.displayName} voice is not installed."
            )
            return
        }
        engine.setSpeechRate(settings.speechSpeed.coerceIn(0.5f, 1.8f))
        engine.setPitch(settings.pitch.coerceIn(0.5f, 1.8f))
        val utteranceId = "${message.id}:${UUID.randomUUID()}"
        lastMessage = message
        val params = Bundle()
        val result = engine.speak(
            message.processedText.ifBlank { message.originalText },
            if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
            params,
            utteranceId
        )
        if (result == TextToSpeech.SUCCESS) {
            _state.value = _state.value.copy(
                currentMessageId = message.id,
                isSpeaking = true,
                isPaused = false,
                error = null
            )
        } else {
            _state.value = _state.value.copy(error = "Text-to-Speech could not speak this message.")
        }
    }

    private fun configureListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                scope.launch {
                    _state.value = _state.value.copy(isSpeaking = true, isPaused = false)
                }
            }

            override fun onDone(utteranceId: String?) {
                scope.launch {
                    if (!paused && queue.isNotEmpty()) {
                        queueIndex += 1
                        val next = queue.getOrNull(queueIndex)
                        if (next != null) {
                            speakInternal(next, queueSettings, flush = true)
                        } else {
                            stop()
                        }
                    } else if (!paused) {
                        _state.value = _state.value.copy(
                            currentMessageId = null,
                            isSpeaking = false,
                            isPaused = false
                        )
                    }
                }
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                scope.launch {
                    if (!paused) {
                        _state.value = _state.value.copy(isSpeaking = false)
                    }
                }
            }

            @Deprecated("Deprecated in Android framework")
            override fun onError(utteranceId: String?) {
                scope.launch {
                    _state.value = _state.value.copy(
                        currentMessageId = null,
                        isSpeaking = false,
                        isPaused = false,
                        error = "Text-to-Speech hit an error."
                    )
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                onError(utteranceId)
            }
        })
    }

    private fun prepareOfflineVoice(engine: TextToSpeech, locale: Locale): Boolean {
        val available = engine.isLanguageAvailable(locale)
        if (available < TextToSpeech.LANG_AVAILABLE) return false
        val voices = engine.voices
        if (voices.isNullOrEmpty()) {
            engine.language = locale
            return true
        }
        val exactOffline = voices.firstOrNull {
            !it.isNetworkConnectionRequired &&
                it.locale.language == locale.language &&
                (locale.country.isBlank() || it.locale.country == locale.country)
        }
        val sameLanguageOffline = voices.firstOrNull {
            !it.isNetworkConnectionRequired && it.locale.language == locale.language
        }
        val voice = exactOffline ?: sameLanguageOffline ?: return false
        engine.voice = voice
        return true
    }

    private fun checkVoices(): List<VoiceAvailability> {
        val engine = tts ?: return emptyList()
        val targets = listOf(
            VoiceAvailability(Locale("hi", "IN"), "Hindi voice", false),
            VoiceAvailability(Locale("gu", "IN"), "Gujarati voice", false),
            VoiceAvailability(Locale.ENGLISH, "English voice", false)
        )
        val voices = engine.voices
        return targets.map { target ->
            val available = if (voices.isNullOrEmpty()) {
                engine.isLanguageAvailable(target.locale) >= TextToSpeech.LANG_AVAILABLE
            } else {
                voices.any {
                    !it.isNetworkConnectionRequired &&
                        it.locale.language == target.locale.language
                }
            }
            target.copy(availableOffline = available)
        }
    }
}
