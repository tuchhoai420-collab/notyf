package com.example

import com.example.ai.NotiAiEngine
import com.example.data.model.NotificationCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `test ai engine detects urgent notifications`() {
    val category = NotiAiEngine.detectCategory(
      packageName = "com.slack",
      title = "DevOps Alert",
      content = "URGENTE: Incidente en servidor de producción"
    )
    assertEquals(NotificationCategory.URGENT, category)

    val priority = NotiAiEngine.calculatePriority(
      category = category,
      title = "DevOps Alert",
      content = "URGENTE: Incidente en producción"
    )
    assertTrue(priority >= 3)
  }

  @Test
  fun `test friendly app name resolution`() {
    assertEquals("WhatsApp", NotiAiEngine.resolveFriendlyAppName("com.whatsapp"))
    assertEquals("Telegram", NotiAiEngine.resolveFriendlyAppName("org.telegram.messenger"))
    assertEquals("Slack", NotiAiEngine.resolveFriendlyAppName("com.slack"))
    assertEquals("Gmail", NotiAiEngine.resolveFriendlyAppName("com.google.android.gm"))
    assertEquals("BBVA", NotiAiEngine.resolveFriendlyAppName("com.bbva.mobile"))
  }
}
