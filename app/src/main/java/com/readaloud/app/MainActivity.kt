package com.readaloud.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.readaloud.app.ui.ReadAloudApp

class MainActivity : ComponentActivity() {
    private var route by mutableStateOf(Routes.ANALYSIS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        route = routeFromIntent(intent)
        setContent {
            ReadAloudApp(
                initialRoute = route
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        route = routeFromIntent(intent)
    }

    private fun routeFromIntent(intent: Intent?): String {
        return when (intent?.action) {
            Actions.OPEN_SETTINGS -> Routes.SETTINGS
            Actions.OPEN_ONBOARDING -> Routes.ONBOARDING
            else -> Routes.ANALYSIS
        }
    }
}

object Actions {
    const val SHOW_ANALYSIS = "com.readaloud.app.SHOW_ANALYSIS"
    const val OPEN_SETTINGS = "com.readaloud.app.OPEN_SETTINGS"
    const val OPEN_ONBOARDING = "com.readaloud.app.OPEN_ONBOARDING"
}

object Routes {
    const val ANALYSIS = "analysis"
    const val SETTINGS = "settings"
    const val ONBOARDING = "onboarding"
}
