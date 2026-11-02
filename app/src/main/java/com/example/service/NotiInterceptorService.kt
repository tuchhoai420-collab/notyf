package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotiInterceptorService : NotificationListenerService() {
    private val TAG = "NotiInterceptorService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: NotificationRepository

    override fun onCreate() {
        super.onCreate()
        repository = NotificationRepository(applicationContext)
        Log.i(TAG, "NotiInterceptorService iniciado con éxito.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        try {
            val pkg = sbn.packageName ?: return
            // Ignore notifications from this app itself to avoid infinite feedback loops
            if (pkg == applicationContext.packageName) return

            val extras = sbn.notification?.extras ?: return
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                ?: extras.getString("android.title")
                ?: "Sin remitente"

            val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras.getString("android.text")
                ?: ""

            // Skip empty notifications or persistent ongoing background progress with no text
            if (text.isBlank() && title.isBlank()) return

            val postTime = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()

            serviceScope.launch {
                repository.addNotification(
                    packageName = pkg,
                    title = title,
                    content = text,
                    timestamp = postTime
                )
                Log.d(TAG, "Notificación interceptada de $pkg: $title - $text")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando notificación entrante", e)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Acceso de escucha de notificaciones CONECTADO.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Acceso de escucha de notificaciones DESCONECTADO.")
    }
}
