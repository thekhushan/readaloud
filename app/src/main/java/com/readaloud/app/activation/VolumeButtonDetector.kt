package com.readaloud.app.activation

import android.view.KeyEvent

class VolumeButtonDetector {
    private var lastUpPress: Long = 0L
    private var lastDownPress: Long = 0L

    fun onKeyEvent(event: KeyEvent, intervalMs: Long): Boolean {
        if (event.action != KeyEvent.ACTION_UP) return false
        val now = event.eventTime
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                val detected = now - lastUpPress in 1..intervalMs
                lastUpPress = now
                detected
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val detected = now - lastDownPress in 1..intervalMs
                lastDownPress = now
                detected
            }
            else -> false
        }
    }
}
