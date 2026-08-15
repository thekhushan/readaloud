package com.readaloud.app.accessibility

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.readaloud.app.Actions
import com.readaloud.app.MainActivity
import com.readaloud.app.ReadAloudApplication
import com.readaloud.app.activation.VolumeButtonDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class ReadAloudAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val volumeButtonDetector = VolumeButtonDetector()
    private var accessibilityButtonCallback: AccessibilityButtonController.AccessibilityButtonCallback? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        current = WeakReference(this)
        registerAccessibilityButton()
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            accessibilityButtonCallback?.let {
                accessibilityButtonController.unregisterAccessibilityButtonCallback(it)
            }
        }
        current = WeakReference(null)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events are not processed continuously. ReadAloud only analyzes after user activation.
    }

    override fun onInterrupt() {
        (application as? ReadAloudApplication)?.ttsManager?.stop()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val app = application as ReadAloudApplication
        val settings = app.settingsRepository.read()
        if (settings.doubleVolumeEnabled &&
            volumeButtonDetector.onKeyEvent(event, settings.doublePressIntervalMs)
        ) {
            analyzeCurrentScreenAndOpen()
        }
        return false
    }

    fun analyzeCurrentScreenAndOpen() {
        val app = application as ReadAloudApplication
        scope.launch {
            app.analysisRepository.analyzeFromAccessibilityService(this@ReadAloudAccessibilityService)
            openAnalysisScreen()
        }
    }

    private fun openAnalysisScreen() {
        val intent = Intent(this, MainActivity::class.java)
            .setAction(Actions.SHOW_ANALYSIS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        runCatching { startActivity(intent) }
    }

    private fun registerAccessibilityButton() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val callback = object : AccessibilityButtonController.AccessibilityButtonCallback() {
            override fun onClicked(controller: AccessibilityButtonController) {
                analyzeCurrentScreenAndOpen()
            }

            override fun onAvailabilityChanged(
                controller: AccessibilityButtonController,
                available: Boolean
            ) = Unit
        }
        accessibilityButtonCallback = callback
        accessibilityButtonController.registerAccessibilityButtonCallback(callback)
    }

    companion object {
        private var current: WeakReference<ReadAloudAccessibilityService?> = WeakReference(null)

        val instance: ReadAloudAccessibilityService?
            get() = current.get()

        val isRunning: Boolean
            get() = current.get() != null
    }
}
