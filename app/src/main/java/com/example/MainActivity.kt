package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.NotiBottomNav
import com.example.ui.components.NotiTopBar
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.ModelScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SummaryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NotiViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: NotiViewModel = viewModel(
                    factory = NotiViewModel.provideFactory(this)
                )

                // Request POST_NOTIFICATIONS on Android 13+ (API 33+)
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Handled */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!hasPerm) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Monitor lifecycle to dynamically detect when the user grants Notification Access in Settings
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshPermission(this@MainActivity)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                NotiApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun NotiApp(viewModel: NotiViewModel) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val onlyUnread by viewModel.filterOnlyUnread.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingSummary.collectAsStateWithLifecycle()
    val summaryError by viewModel.summaryError.collectAsStateWithLifecycle()
    val aiModel by viewModel.aiModelPreference.collectAsStateWithLifecycle()
    val isPermissionGranted by viewModel.isServicePermissionGranted.collectAsStateWithLifecycle()
    val localModelStatus by viewModel.localModelStatus.collectAsStateWithLifecycle()

    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()
    val urgentCount by viewModel.urgentCount.collectAsStateWithLifecycle()
    val latestSummary by viewModel.latestSummary.collectAsStateWithLifecycle()
    val allSummaries by viewModel.allSummaries.collectAsStateWithLifecycle()
    val notifications by viewModel.displayedNotifications.collectAsStateWithLifecycle()

    val openNotificationSettings = {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(fallback)
        }
    }

    // Handle back button on sub-tabs
    BackHandler(enabled = selectedTab != 0) {
        viewModel.selectTab(0)
    }

    Scaffold(
        topBar = {
            NotiTopBar(
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::setSearchQuery,
                activeModel = aiModel
            )
        },
        bottomBar = {
            NotiBottomNav(
                selectedTab = selectedTab,
                onTabSelected = viewModel::selectTab,
                unreadCount = unreadCount
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> SummaryScreen(
                    latestSummary = latestSummary,
                    allSummaries = allSummaries,
                    totalCount = totalCount,
                    unreadCount = unreadCount,
                    urgentCount = urgentCount,
                    isGenerating = isGenerating,
                    error = summaryError,
                    isPermissionGranted = isPermissionGranted,
                    onOpenSettings = openNotificationSettings,
                    onGenerateSummary = viewModel::generateExecutiveSummary
                )

                1 -> FeedScreen(
                    notifications = notifications,
                    selectedCategory = selectedCategory,
                    onCategorySelected = viewModel::setCategory,
                    onlyUnread = onlyUnread,
                    onToggleOnlyUnread = viewModel::toggleFilterOnlyUnread,
                    onMarkAllRead = viewModel::markAllAsRead,
                    onToggleRead = viewModel::toggleReadStatus,
                    onToggleStar = viewModel::toggleStarredStatus,
                    onDelete = viewModel::deleteNotification,
                    isPermissionGranted = isPermissionGranted
                )

                2 -> ModelScreen(
                    modelStatus = localModelStatus,
                    availableStorageMb = viewModel.getStorageMb(),
                    onStartDownload = viewModel::startModelDownload,
                    onCancelDownload = viewModel::cancelModelDownload,
                    onDeleteModel = viewModel::deleteDownloadedModel
                )

                3 -> SettingsScreen(
                    currentModel = aiModel,
                    onModelChange = viewModel::setAiModel,
                    totalCount = totalCount,
                    isPermissionGranted = isPermissionGranted,
                    onOpenSettings = openNotificationSettings,
                    onClearAllData = viewModel::clearAllData
                )
            }
        }
    }
}
