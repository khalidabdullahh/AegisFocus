package com.aegisfocus.app.presentation.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.domain.model.AppCategory
import com.aegisfocus.app.domain.model.AppInfo
import com.aegisfocus.app.domain.model.FocusSession
import com.aegisfocus.app.domain.model.SessionMode
import com.aegisfocus.app.domain.repository.AppDiscoveryRepository
import com.aegisfocus.app.domain.subscription.EntitlementManager
import com.aegisfocus.app.engine.FocusSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppsUiState(
    val apps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: AppCategory? = null,
    val blockedPackages: Set<String> = emptySet(),
    val isStrictLocked: Boolean = false,
    val isPro: Boolean = false,
    val maxAllowedApps: Int = 3,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AppsViewModel(
    private val appDiscoveryRepository: AppDiscoveryRepository,
    private val focusSessionManager: FocusSessionManager,
    private val entitlementManager: EntitlementManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<AppCategory?>(null)
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _blockedPackages = MutableStateFlow<Set<String>>(emptySet())
    private val _isLoading = MutableStateFlow(true)
    private val _isPro = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AppsUiState> = combine(
        _allApps,
        _searchQuery,
        _selectedCategory,
        _blockedPackages,
        focusSessionManager.activeSession,
        _isLoading,
        _isPro,
        _errorMessage
    ) { flows: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val apps = flows[0] as List<AppInfo>
        val query = flows[1] as String
        val category = flows[2] as AppCategory?
        @Suppress("UNCHECKED_CAST")
        val blocked = flows[3] as Set<String>
        val activeSession = flows[4] as FocusSession?
        val loading = flows[5] as Boolean
        val pro = flows[6] as Boolean
        val error = flows[7] as String?

        val isStrict = activeSession != null && activeSession.mode == SessionMode.STRICT && !activeSession.isExpired

        val filtered = apps.filter { app ->
            val matchesQuery = query.isBlank() ||
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
            val matchesCategory = category == null || app.category == category
            matchesQuery && matchesCategory
        }

        AppsUiState(
            apps = filtered,
            searchQuery = query,
            selectedCategory = category,
            blockedPackages = blocked,
            isStrictLocked = isStrict,
            isPro = pro,
            maxAllowedApps = if (pro) Int.MAX_VALUE else 3,
            isLoading = loading,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppsUiState())

    init {
        loadApps()
        observeBlockedApps()
        observeEntitlement()
    }

    private fun observeBlockedApps() {
        viewModelScope.launch {
            appDiscoveryRepository.getBlockedPackageNamesFlow().collect { blocked ->
                _blockedPackages.value = blocked
            }
        }
    }

    private fun observeEntitlement() {
        viewModelScope.launch {
            entitlementManager.activePlan.collect {
                _isPro.value = entitlementManager.isPro()
            }
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val apps = appDiscoveryRepository.loadInstalledApps()
            val blocked = appDiscoveryRepository.getBlockedPackageNames().toSet()
            val isPro = entitlementManager.isPro()
            _allApps.value = apps
            _blockedPackages.value = blocked
            _isPro.value = isPro
            _isLoading.value = false
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: AppCategory?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun toggleApp(packageName: String, isBlocked: Boolean) {
        val active = focusSessionManager.activeSession.value
        if (active != null && active.mode == SessionMode.STRICT && !active.isExpired) {
            return // Immutable under Strict Mode
        }

        viewModelScope.launch {
            val isPro = entitlementManager.isPro()
            val current = _blockedPackages.value

            if (isBlocked && !isPro && current.size >= 3 && !current.contains(packageName)) {
                _errorMessage.value = "Free plan allows blocking up to 3 apps. Upgrade to Pro for unlimited apps."
                return@launch
            }

            _errorMessage.value = null
            appDiscoveryRepository.setAppBlocked(packageName, isBlocked)
        }
    }

    fun selectAllFiltered() {
        val active = focusSessionManager.activeSession.value
        if (active != null && active.mode == SessionMode.STRICT && !active.isExpired) return

        viewModelScope.launch {
            val isPro = entitlementManager.isPro()
            val filteredPkgs = uiState.value.apps.map { it.packageName }
            if (!isPro && (filteredPkgs.size > 3 || _blockedPackages.value.size + filteredPkgs.size > 3)) {
                _errorMessage.value = "Free plan allows blocking up to 3 apps. Upgrade to Pro for unlimited apps."
                return@launch
            }

            _errorMessage.value = null
            appDiscoveryRepository.setAppsBlocked(filteredPkgs, true)
        }
    }

    fun deselectAllFiltered() {
        val active = focusSessionManager.activeSession.value
        if (active != null && active.mode == SessionMode.STRICT && !active.isExpired) return

        val filteredPkgs = uiState.value.apps.map { it.packageName }
        viewModelScope.launch {
            _errorMessage.value = null
            appDiscoveryRepository.setAppsBlocked(filteredPkgs, false)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
