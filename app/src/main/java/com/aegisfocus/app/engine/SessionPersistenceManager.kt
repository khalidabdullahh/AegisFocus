package com.aegisfocus.app.engine

import com.aegisfocus.app.domain.model.FocusSession
import com.aegisfocus.app.domain.usecase.RestoreActiveSessionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionPersistenceManager(
    private val restoreActiveSessionUseCase: RestoreActiveSessionUseCase
) {
    suspend fun restoreActiveSessionOnLaunch(): FocusSession? = withContext(Dispatchers.IO) {
        restoreActiveSessionUseCase()
    }
}
