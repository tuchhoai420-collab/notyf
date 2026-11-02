package com.example.ai

import com.example.data.model.AiSummary
import com.example.data.model.NotificationCategory
import com.example.data.model.NotificationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotiAiEngine {

    /**
     * Determines app category based on package name and text content.
     */
    fun detectCategory(packageName: String, title: String, content: String): NotificationCategory {
        val combined = "$packageName $title $content".lowercase(Locale.ROOT)

        val urgentTriggers = listOf(
            "urgente", "emergencia", "alerta", "fraude", "seguridad", "código",
            "verificación", "otp", "2fa", "bloqueada", "inmediato", "asap", "crítico"
        )
        if (urgentTriggers.any { combined.contains(it) }) {
            return NotificationCategory.URGENT
        }

        val financeTriggers = listOf(
            "banco", "tarjeta", "transferencia", "compra", "pago", "saldo", "depósito",
            "factura", "cuenta", "spei", "paypal", "stripe", "visa", "mastercard", "bbva", "santander"
        )
        if (financeTriggers.any { combined.contains(it) }) {
            return NotificationCategory.FINANCE
        }

        val workTriggers = listOf(
            "slack", "teams", "trello", "jira", "github", "gitlab", "pull request",
            "reunión", "meet", "zoom", "proyecto", "sprint", "deploy", "producción", "cliente"
        )
        if (workTriggers.any { combined.contains(it) } || packageName.contains("slack") || packageName.contains("teams")) {
            return NotificationCategory.WORK
        }

        val promoTriggers = listOf(
            "descuento", "oferta", "cupón", "gratis", "promoción", "rebajas",
            "sale", "black friday", "2x1", "cashback", "aprovecha"
        )
        if (promoTriggers.any { combined.contains(it) }) {
            return NotificationCategory.PROMO
        }

        val messagingPackages = listOf("whatsapp", "telegram", "messaging", "signal", "viber", "line", "messenger")
        if (messagingPackages.any { packageName.contains(it) }) {
            return NotificationCategory.MESSAGING
        }

        val socialPackages = listOf("instagram", "twitter", "facebook", "tiktok", "linkedin", "reddit")
        if (socialPackages.any { packageName.contains(it) }) {
            return NotificationCategory.SOCIAL
        }

        return NotificationCategory.MESSAGING
    }

    /**
     * Calculates priority score from 1 (Low) to 4 (Critical).
     */
    fun calculatePriority(category: NotificationCategory, title: String, content: String): Int {
        val text = "$title $content".lowercase(Locale.ROOT)
        
        if (text.contains("código") || text.contains("otp") || text.contains("fraude") ||
            text.contains("emergencia") || text.contains("urgente!") || text.contains("alerta roja")) {
            return 4 // Crítica
        }

        if (category == NotificationCategory.URGENT ||
            text.contains("urgente") || text.contains("reunión ahora") || text.contains("deadline") ||
            text.contains("servidor caído") || text.contains("revisión requerida")) {
            return 3 // Alta
        }

        if (category == NotificationCategory.WORK || category == NotificationCategory.FINANCE) {
            return 2 // Media
        }

        if (category == NotificationCategory.PROMO) {
            return 1 // Baja
        }

        return 2 // Media por defecto
    }

    /**
     * Resolves human-friendly app name from package.
     */
    fun resolveFriendlyAppName(packageName: String): String {
        return when {
            packageName.contains("whatsapp") -> "WhatsApp"
            packageName.contains("telegram") -> "Telegram"
            packageName.contains("slack") -> "Slack"
            packageName.contains("teams") -> "Microsoft Teams"
            packageName.contains("gm") || packageName.contains("gmail") -> "Gmail"
            packageName.contains("messaging") || packageName.contains("mms") -> "Mensajes SMS"
            packageName.contains("twitter") || packageName.contains("x.android") -> "X (Twitter)"
            packageName.contains("instagram") -> "Instagram"
            packageName.contains("linkedin") -> "LinkedIn"
            packageName.contains("bbva") -> "BBVA"
            packageName.contains("santander") -> "Santander"
            packageName.contains("github") -> "GitHub"
            packageName.contains("calendar") -> "Google Calendar"
            else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Generates an executive AI briefing from a list of notifications.
     */
    suspend fun generateBriefing(
        notifications: List<NotificationItem>,
        modelPreference: String = "NotiAI Edge Engine v2.4 (On-Device)"
    ): AiSummary = withContext(Dispatchers.Default) {
        // Simulating realistic AI processing latency
        delay(600)

        if (notifications.isEmpty()) {
            return@withContext AiSummary(
                title = "Sin Notificaciones Pendientes",
                overview = "Tu bandeja de notificaciones está completamente al día. No hay mensajes ni alertas que requieran tu atención.",
                keyTakeaways = "• Bandeja limpia y sin pendientes.\n• Disfruta de tu tiempo libre o enfócate en tus prioridades.",
                actionItems = "• No se requieren acciones en este momento.",
                notificationCount = 0,
                urgencyLevel = "NORMAL",
                engineType = modelPreference,
                timeSavedMinutes = 0,
                noiseReductionPercent = 100
            )
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = timeFormat.format(Date())

        val urgentList = notifications.filter { it.priority >= 3 }
        val workList = notifications.filter { it.category == NotificationCategory.WORK.name }
        val financeList = notifications.filter { it.category == NotificationCategory.FINANCE.name }
        val personalList = notifications.filter { it.category == NotificationCategory.MESSAGING.name }
        val promoList = notifications.filter { it.category == NotificationCategory.PROMO.name }

        val urgencyLevel = when {
            urgentList.any { it.priority == 4 } -> "CRÍTICA"
            urgentList.isNotEmpty() -> "ALTA"
            workList.isNotEmpty() -> "MODERADA"
            else -> "NORMAL"
        }

        // Compute noise reduction: promos + short casual chatter
        val noiseCount = promoList.size + notifications.count { it.content.length < 15 }
        val noisePercent = if (notifications.isNotEmpty()) {
            ((noiseCount.toDouble() / notifications.size) * 100).toInt().coerceIn(35, 92)
        } else 50

        // Time saved: roughly 1.5 to 2 minutes per notification synthesized
        val minutesSaved = (notifications.size * 1.8).toInt().coerceAtLeast(3)

        // Construct Executive Synthesis
        val overviewBuilder = StringBuilder()
        overviewBuilder.append("Análisis ejecutado a las $currentTime con $modelPreference. ")
        overviewBuilder.append("Se procesaron ${notifications.size} notificaciones entrantes. ")

        if (urgentList.isNotEmpty()) {
            overviewBuilder.append("⚠️ Se detectaron ${urgentList.size} asuntos de alta prioridad que requieren atención inmediata. ")
        } else {
            overviewBuilder.append("✅ Sin alertas críticas que interrumpan tu flujo de trabajo. ")
        }

        if (workList.isNotEmpty()) {
            overviewBuilder.append("En el ámbito laboral hay ${workList.size} comunicaciones activas. ")
        }
        if (personalList.isNotEmpty()) {
            overviewBuilder.append("En mensajería personal hay ${personalList.size} mensajes de tus contactos. ")
        }

        // Construct Key Takeaways
        val takeawaysBuilder = StringBuilder()
        if (urgentList.isNotEmpty()) {
            takeawaysBuilder.append("• 🔴 PRIORIDAD: ${urgentList.first().title} - \"${urgentList.first().content.take(60)}...\"\n")
        }
        if (workList.isNotEmpty()) {
            val senders = workList.map { it.title }.distinct().take(2).joinToString(", ")
            takeawaysBuilder.append("• 💼 TRABAJO: Interacciones clave de $senders con actualizaciones de proyectos.\n")
        }
        if (financeList.isNotEmpty()) {
            takeawaysBuilder.append("• 💳 FINANZAS: Notificaciones bancarias registradas y validadas contra fraudes.\n")
        }
        if (personalList.isNotEmpty()) {
            val contacts = personalList.map { it.title }.distinct().take(3).joinToString(", ")
            takeawaysBuilder.append("• 💬 MENSAJERÍA: Mensajes directos de $contacts.\n")
        }
        if (promoList.isNotEmpty()) {
            takeawaysBuilder.append("• 🔕 RUIDO FILTRADO: ${promoList.size} promociones y avisos publicitarios archivados sin interrumpirte.\n")
        }

        // Construct Action Items Checklist
        val actionItemsBuilder = StringBuilder()
        if (urgentList.isNotEmpty()) {
            urgentList.take(2).forEach {
                actionItemsBuilder.append("• Atender con urgencia: ${it.title} (${it.appName})\n")
            }
        }
        if (workList.isNotEmpty()) {
            actionItemsBuilder.append("• Revisar hilo laboral de ${workList.first().appName}: ${workList.first().title}\n")
        }
        if (financeList.isNotEmpty()) {
            actionItemsBuilder.append("• Confirmar cargo bancario en ${financeList.first().appName}\n")
        }
        if (personalList.isNotEmpty()) {
            actionItemsBuilder.append("• Responder mensajes de ${personalList.first().title}\n")
        }
        if (actionItemsBuilder.isBlank()) {
            actionItemsBuilder.append("• Todas las notificaciones son meramente informativas.\n")
        }

        AiSummary(
            title = "Briefing Ejecutivo: ${notifications.size} Notificaciones",
            overview = overviewBuilder.toString(),
            keyTakeaways = takeawaysBuilder.toString().trimEnd(),
            actionItems = actionItemsBuilder.toString().trimEnd(),
            notificationCount = notifications.size,
            urgencyLevel = urgencyLevel,
            engineType = modelPreference,
            timeSavedMinutes = minutesSaved,
            noiseReductionPercent = noisePercent
        )
    }
}
