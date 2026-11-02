package com.example.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.LocalModelManager
import com.example.ai.ModelStatus
import com.example.data.model.AiSummary
import com.example.data.model.NotificationCategory
import com.example.data.model.NotificationItem
import com.example.data.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotiViewModel(
    private val repository: NotificationRepository,
    val modelManager: LocalModelManager
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow(NotificationCategory.ALL)
    val selectedCategory: StateFlow<NotificationCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterOnlyUnread = MutableStateFlow(false)
    val filterOnlyUnread: StateFlow<Boolean> = _filterOnlyUnread.asStateFlow()

    private val _isGeneratingSummary = MutableStateFlow(false)
    val isGeneratingSummary: StateFlow<Boolean> = _isGeneratingSummary.asStateFlow()

    private val _summaryError = MutableStateFlow<String?>(null)
    val summaryError: StateFlow<String?> = _summaryError.asStateFlow()

    private val _aiModelPreference = MutableStateFlow("NotiAI Edge Engine v2.4 (On-Device)")
    val aiModelPreference: StateFlow<String> = _aiModelPreference.asStateFlow()

    private val _isServicePermissionGranted = MutableStateFlow(false)
    val isServicePermissionGranted: StateFlow<Boolean> = _isServicePermissionGranted.asStateFlow()

    val localModelStatus: StateFlow<ModelStatus> = modelManager.status

    val totalCount: StateFlow<Int> = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unreadCount: StateFlow<Int> = repository.unreadCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val urgentCount: StateFlow<Int> = repository.urgentCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val latestSummary: StateFlow<AiSummary?> = repository.latestSummary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSummaries: StateFlow<List<AiSummary>> = repository.allSummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val displayedNotifications: StateFlow<List<NotificationItem>> = combine(
        _selectedCategory,
        _searchQuery,
        _filterOnlyUnread
    ) { category, query, onlyUnread ->
        Triple(category, query, onlyUnread)
    }.flatMapLatest { (category, query, onlyUnread) ->
        if (query.isNotBlank()) {
            repository.search(query)
        } else if (category == NotificationCategory.ALL) {
            if (onlyUnread) repository.unreadNotifications else repository.allNotifications
        } else if (category == NotificationCategory.URGENT) {
            repository.urgentNotifications
        } else {
            repository.getByCategory(category.name)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshPermission(context: Context) {
        _isServicePermissionGranted.value = isNotificationServiceEnabled(context)
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun setCategory(category: NotificationCategory) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFilterOnlyUnread() {
        _filterOnlyUnread.value = !_filterOnlyUnread.value
    }

    fun setAiModel(model: String) {
        _aiModelPreference.value = model
    }

    fun generateExecutiveSummary() {
        viewModelScope.launch {
            _isGeneratingSummary.value = true
            _summaryError.value = null
            try {
                repository.generateAndSaveSummary(_aiModelPreference.value)
            } catch (e: Exception) {
                _summaryError.value = e.localizedMessage ?: "Error al generar el resumen de IA."
            } finally {
                _isGeneratingSummary.value = false
            }
        }
    }

    fun toggleReadStatus(id: Long, currentIsRead: Boolean) {
        viewModelScope.launch {
            repository.setRead(id, !currentIsRead)
        }
    }

    fun toggleStarredStatus(id: Long, currentIsStarred: Boolean) {
        viewModelScope.launch {
            repository.setStarred(id, !currentIsStarred)
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun startModelDownload() {
        viewModelScope.launch {
            modelManager.downloadModel()
        }
    }

    fun cancelModelDownload() {
        modelManager.cancelDownload()
    }

    fun deleteDownloadedModel() {
        modelManager.deleteModel()
    }

    fun getStorageMb(): Long = modelManager.getAvailableStorageMb()

    companion object {
        fun isNotificationServiceEnabled(context: Context): Boolean {
            val pkgName = context.packageName
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: ""
            if (flat.isNotEmpty()) {
                val names = flat.split(":")
                for (name in names) {
                    val cn = ComponentName.unflattenFromString(name)
                    if (cn != null && cn.packageName == pkgName) {
                        return true
                    }
                }
            }
            return false
        }

        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val repo = NotificationRepository(context.applicationContext)
                    val modelMgr = LocalModelManager(context.applicationContext)
                    return NotiViewModel(repo, modelMgr) as T
                }
            }
    }
}
