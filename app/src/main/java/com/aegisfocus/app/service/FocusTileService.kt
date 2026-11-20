package com.aegisfocus.app.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.aegisfocus.app.core.di.ServiceLocator
import com.aegisfocus.app.domain.model.SessionMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Quick Settings tile: one tap starts a 25 minute normal focus session. */
class FocusTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        val manager = ServiceLocator.getContainer(applicationContext).focusSessionManager
        if (manager.activeSession.value == null) {
            scope.launch {
                manager.startSession(name = "Quick Focus", durationMinutes = 25, mode = SessionMode.NORMAL)
                refreshTile()
            }
        }
        refreshTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val active = ServiceLocator.getContainer(applicationContext).focusSessionManager.activeSession.value != null
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (active) "Focus on" else "Focus 25"
        tile.updateTile()
    }
}
