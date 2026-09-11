package com.aegisfocus.app.engine

import com.aegisfocus.app.domain.model.FocusSession
import com.aegisfocus.app.domain.model.SessionMode
import com.aegisfocus.app.domain.model.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BlockingRuleRepository(
    private val matcher: BlockedAppMatcher
) {
    private val _activeBlockedPackages = MutableStateFlow<Set<String>>(emptySet())
    val activeBlockedPackages: StateFlow<Set<String>> = _activeBlockedPackages.asStateFlow()

    fun updateFromSession(session: FocusSession?) {
        if (session != null && session.status == SessionStatus.ACTIVE && !session.isExpired) {
            val packages = session.blockedPackageNames.toSet()
            val isStrict = session.mode == SessionMode.STRICT
            matcher.updateBlockedPackages(packages, isStrict)
            _activeBlockedPackages.value = packages
        } else {
            matcher.clear()
            _activeBlockedPackages.value = emptySet()
        }
    }

    fun isBlocked(packageName: String, myPackageName: String): Boolean {
        return matcher.isAppBlocked(packageName, myPackageName)
    }

    fun isStrictGuarded(): Boolean = matcher.isStrictGuarded()
}
