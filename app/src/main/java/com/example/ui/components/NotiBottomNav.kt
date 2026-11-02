package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun NotiBottomNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    unreadCount: Int,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        // Tab 0: Resumen
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == 0) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                    contentDescription = "Resumen IA"
                )
            },
            label = { Text("Resumen") },
            modifier = Modifier.testTag("nav_tab_summary")
        )

        // Tab 1: Feed
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge {
                                Text(if (unreadCount > 99) "99+" else unreadCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (selectedTab == 1) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                        contentDescription = "Feed de Notificaciones"
                    )
                }
            },
            label = { Text("Feed") },
            modifier = Modifier.testTag("nav_tab_feed")
        )

        // Tab 2: Modelo IA Local
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == 2) Icons.Filled.Memory else Icons.Outlined.Memory,
                    contentDescription = "Modelo Local"
                )
            },
            label = { Text("Modelo IA") },
            modifier = Modifier.testTag("nav_tab_model")
        )

        // Tab 3: Ajustes
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "Ajustes y Permisos"
                )
            },
            label = { Text("Ajustes") },
            modifier = Modifier.testTag("nav_tab_settings")
        )
    }
}
