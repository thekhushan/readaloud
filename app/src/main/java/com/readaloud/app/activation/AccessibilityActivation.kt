package com.readaloud.app.activation

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import com.readaloud.app.accessibility.ReadAloudAccessibilityService

object AccessibilityActivation {
    fun isServiceEnabled(context: Context): Boolean {
        val expected = "${context.packageName}/${ReadAloudAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expected, ignoreCase = true)) return true
        }
        return false
    }

    fun accessibilitySettingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
