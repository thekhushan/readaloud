package com.readaloud.app

import android.app.Application
import com.readaloud.app.analysis.AnalysisRepository
import com.readaloud.app.language.SpeechTextProcessor
import com.readaloud.app.settings.SettingsRepository
import com.readaloud.app.speech.TtsManager

class ReadAloudApplication : Application() {
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var speechTextProcessor: SpeechTextProcessor
        private set
    lateinit var ttsManager: TtsManager
        private set
    lateinit var analysisRepository: AnalysisRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        speechTextProcessor = SpeechTextProcessor()
        ttsManager = TtsManager(this)
        analysisRepository = AnalysisRepository(
            settingsRepository = settingsRepository,
            speechTextProcessor = speechTextProcessor
        )
    }
}
