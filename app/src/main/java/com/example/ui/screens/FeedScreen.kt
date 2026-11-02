package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationCategory
import com.example.data.model.NotificationItem
import com.example.ui.theme.EmeraldTertiaryDark
import com.example.ui.theme.UrgentRed
import com.example.ui.theme.WarningAmber

@Composable
fun FeedScreen(
    notifications: List<NotificationItem>,
    selectedCategory: NotificationCategory,
    onCategorySelected: (NotificationCategory) -> Unit,
    onlyUnread: Boolean,
    onToggleOnlyUnread: () -> Unit,
    onMarkAllRead: () -> Unit,
    onToggleRead: (Long, Boolean) -> Unit,
    onToggleStar: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    isPermissionGranted: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Horizontal Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            items(NotificationCategory.entries.toTypedArray()) { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category.displayName) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Subheader: Count and Quick Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${notifications.size} notificaciones",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onToggleOnlyUnread,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .padding(end = 6.dp)
                ) {
                    Text(
                        text = if (onlyUnread) "Ver todas" else "Solo sin leer",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                TextButton(
                    onClick = onMarkAllRead,
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Marcar todas como leídas",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Leídas", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // List of Notifications or Real Empty State
        if (notifications.isEmpty()) {
            RealEmptyFeedState(
                isPermissionGranted = isPermissionGranted,
                onOpenSettings = {
                    try {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val fallback = Intent(Settings.ACTION_SETTINGS)
                        context.startActivity(fallback)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(notifications, key = { it.id }) { item ->
                    NotificationCard(
                        item = item,
                        onToggleRead = { onToggleRead(item.id, item.isRead) },
                        onToggleStar = { onToggleStar(item.id, item.isStarred) },
                        onDelete = { onDelete(item.id) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    item: NotificationItem,
    onToggleRead: () -> Unit,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit
) {
    val relativeTime = formatRelativeTime(item.timestamp)
    val appBadgeColor = getAppColor(item.packageName)

    val priorityColor = when (item.priority) {
        4 -> UrgentRed
        3 -> WarningAmber
        1 -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.secondary
    }

    val priorityLabel = when (item.priority) {
        4 -> "CRÍTICA"
        3 -> "ALTA"
        1 -> "BAJA"
        else -> "MEDIA"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRead) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (item.isRead) 1.dp else 3.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notification_item_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: App icon/badge + App name + Timestamp + Star
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(appBadgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.appName.take(1).uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.appName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = priorityColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = priorityLabel,
                                color = priorityColor,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onToggleStar,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (item.isStarred) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Destacar",
                        tint = if (item.isStarred) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title (Sender / Topic)
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Content body
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Actions: Read state & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleRead,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (item.isRead) Icons.Default.MarkEmailUnread else Icons.Default.MarkEmailRead,
                        contentDescription = if (item.isRead) "Marcar como no leída" else "Marcar como leída",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar notificación",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RealEmptyFeedState(
    isPermissionGranted: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPermissionGranted) MaterialTheme.colorScheme.primaryContainer
                        else WarningAmber.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPermissionGranted) Icons.Default.NotificationsNone else Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = if (isPermissionGranted) MaterialTheme.colorScheme.primary else WarningAmber,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (isPermissionGranted) "Esperando notificaciones entrantes" else "Acceso de escucha pendiente",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isPermissionGranted) {
                    "El servicio NotiAI está en ejecución. Cuando recibas mensajes de WhatsApp, Telegram, Slack, correos o alertas bancarias, aparecerán aquí automáticamente."
                } else {
                    "Para interceptar notificaciones reales, debes activar el permiso de 'Acceso a notificaciones' en los ajustes de Android."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!isPermissionGranted) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Habilitar Permiso de Notificaciones")
                }
            }
        }
    }
}

fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Hace un momento"
        minutes < 60 -> "Hace ${minutes}m"
        hours < 24 -> "Hace ${hours}h"
        days == 1L -> "Ayer"
        else -> "Hace ${days}d"
    }
}

fun getAppColor(packageName: String): Color {
    return when {
        packageName.contains("whatsapp") -> Color(0xFF25D366)
        packageName.contains("telegram") -> Color(0xFF0088CC)
        packageName.contains("slack") -> Color(0xFF4A154B)
        packageName.contains("teams") -> Color(0xFF6264A7)
        packageName.contains("gmail") || packageName.contains("gm") -> Color(0xFFEA4335)
        packageName.contains("bbva") -> Color(0xFF004481)
        packageName.contains("santander") -> Color(0xFFEC0000)
        packageName.contains("twitter") || packageName.contains("x.android") -> Color(0xFF1DA1F2)
        packageName.contains("instagram") -> Color(0xFFE1306C)
        else -> Color(0xFF4F46E5)
    }
}
