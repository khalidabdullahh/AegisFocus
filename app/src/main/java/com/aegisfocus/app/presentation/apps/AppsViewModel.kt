package com.aegisfocus.app.presentation.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.domain.model.AppCategory
import com.aegisfocus.app.domain.model.AppInfo
import com.aegisfocus.app.domain.model.SessionMode
import com.aegisfocus.app.domain.repository.AppDiscoveryRepository
import com.aegisfocus.app.engine.FocusSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppsUiState(
    val apps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: AppCategory? = null,
    val blockedPackages: Set<String> = emptySet(),
    val isStrictLocked: Boolean = false,
    val isLoading: Boolean = false
)

class AppsViewModel(
    private val appDiscoveryRepository: AppDiscoveryRepository,
    private val focusSessionManager: FocusSessionManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<AppCategory?>(null)
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _blockedPackages = MutableStateFlow<Set<String>>(emptySet())
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<AppsUiState> = combine(
        _allApps,
        _searchQuery,
        _selectedCategory,
        _blockedPackages,
        focusSessionManager.activeSession,
        _isLoading
    ) { apps, query, category, blocked, activeSession, loading ->
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
            isLoading = loading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppsUiState())

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val apps = appDiscoveryRepository.loadInstalledApps()
            val blocked = appDiscoveryRepository.getBlockedPackageNames().toSet()
            _allApps.value = apps
            _blockedPackages.value = blocked
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

        val updated = _blockedPackages.value.toMutableSet()
        if (isBlocked) updated.add(packageName) else updated.remove(packageName)
        _blockedPackages.value = updated

        viewModelScope.launch {
            appDiscoveryRepository.setAppBlocked(packageName, isBlocked)
        }
    }

    fun selectAllFiltered() {
        val active = focusSessionManager.activeSession.value
        if (active != null && active.mode == SessionMode.STRICT && !active.isExpired) return

        val filteredPkgs = uiState.value.apps.map { it.packageName }
        val updated = _blockedPackages.value.toMutableSet().apply { addAll(filteredPkgs) }
        _blockedPackages.value = updated

        viewModelScope.launch {
            appDiscoveryRepository.setAppsBlocked(filteredPkgs, true)
        }
    }

    fun deselectAllFiltered() {
        val active = focusSessionManager.activeSession.value
        if (active != null && active.mode == SessionMode.STRICT && !active.isExpired) return

        val filteredPkgs = uiState.value.apps.map { it.packageName }
        val updated = _blockedPackages.value.toMutableSet().apply { removeAll(filteredPkgs.toSet()) }
        _blockedPackages.value = updated

        viewModelScope.launch {
            appDiscoveryRepository.setAppsBlocked(filteredPkgs, false)
        }
    }
}
