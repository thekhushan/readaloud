package com.readaloud.app.activation

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.readaloud.app.Actions
import com.readaloud.app.MainActivity
import com.readaloud.app.accessibility.ReadAloudAccessibilityService

class ReadAloudTileService : TileService() {
    override fun onStartListening() {
        qsTile?.state = if (ReadAloudAccessibilityService.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val service = ReadAloudAccessibilityService.instance
        if (service != null) {
            service.analyzeCurrentScreenAndOpen()
        } else {
            val intent = Intent(this, MainActivity::class.java)
                .setAction(Actions.OPEN_ONBOARDING)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }
}
