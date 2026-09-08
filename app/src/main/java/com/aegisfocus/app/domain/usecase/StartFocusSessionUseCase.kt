package com.aegisfocus.app.domain.usecase

import com.aegisfocus.app.domain.model.FocusSession
import com.aegisfocus.app.domain.model.SessionMode
import com.aegisfocus.app.domain.model.SessionStatus
import com.aegisfocus.app.domain.repository.AppDiscoveryRepository
import com.aegisfocus.app.domain.repository.FocusSessionRepository

class StartFocusSessionUseCase(
    private val sessionRepository: FocusSessionRepository,
    private val appDiscoveryRepository: AppDiscoveryRepository
) {
    suspend operator fun invoke(
        name: String = "Focus Session",
        durationMinutes: Int = 25,
        mode: SessionMode = SessionMode.NORMAL,
        blockedPackageNames: List<String>? = null,
        profileId: Long? = null
    ): Result<FocusSession> {
        val existing = sessionRepository.getActiveSession()
        if (existing != null && !existing.isExpired && existing.status == SessionStatus.ACTIVE) {
            return Result.failure(IllegalStateException("A focus session is already active"))
        }

        val resolvedBlockedApps = if (!blockedPackageNames.isNullOrEmpty()) {
            blockedPackageNames
        } else {
            appDiscoveryRepository.getBlockedPackageNames().ifEmpty {
                // Fallback default distracting packages
                listOf(
                    "com.google.android.youtube",
                    "com.instagram.android",
                    "com.twitter.android",
                    "com.zhiliaoapp.musically",
                    "com.reddit.frontpage",
                    "com.facebook.katana"
                )
            }
        }

        val now = System.currentTimeMillis()
        val end = now + (durationMinutes * 60 * 1000L)

        val newSession = FocusSession(
            name = if (name.isBlank()) "Focus Session" else name.trim(),
            startEpochMs = now,
            endEpochMs = end,
            durationMinutes = durationMinutes,
            mode = mode,
            status = SessionStatus.ACTIVE,
            blockedPackageNames = resolvedBlockedApps,
            profileId = profileId
        )

        val id = sessionRepository.startSession(newSession)
        return Result.success(newSession.copy(id = id))
    }
}
