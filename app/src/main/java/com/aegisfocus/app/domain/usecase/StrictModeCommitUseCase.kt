package com.aegisfocus.app.domain.usecase

import com.aegisfocus.app.domain.subscription.EntitlementManager

data class StrictModeCommitRequest(
    val durationMinutes: Int,
    val blockedPackages: List<String>,
    val profileName: String,
    val expectedEndTimeMs: Long
)

sealed class StrictModeCommitResult {
    data class ReadyToCommit(val request: StrictModeCommitRequest) : StrictModeCommitResult()
    data class ExceedsFreeLimit(val maxMinutes: Int) : StrictModeCommitResult()
    object Committed : StrictModeCommitResult()
}

/**
 * Orchestrates the "Ready to commit?" Strict Mode commitment flow.
 * Shows the review dialog → validates entitlement → confirms lock-in.
 */
class StrictModeCommitUseCase(private val entitlementManager: EntitlementManager) {

    /** Step 1: Validate the request before showing the commit dialog. */
    suspend fun validateRequest(request: StrictModeCommitRequest): StrictModeCommitResult {
        val canCommit = entitlementManager.canCommitStrictSession(request.durationMinutes)
        return if (canCommit) {
            StrictModeCommitResult.ReadyToCommit(request)
        } else {
            StrictModeCommitResult.ExceedsFreeLimit(
                maxMinutes = entitlementManager.maxStrictDurationMinutes()
            )
        }
    }

    /** Step 2: User explicitly confirmed — return Committed. Session should start immediately after. */
    fun confirm(): StrictModeCommitResult = StrictModeCommitResult.Committed
}
