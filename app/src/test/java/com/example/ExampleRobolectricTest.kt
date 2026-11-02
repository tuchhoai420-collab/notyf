package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.LocalModelManager
import com.example.ai.ModelStatus
import com.example.ai.NotiAiEngine
import com.example.data.database.AppDatabase
import com.example.data.model.NotificationCategory
import com.example.data.model.NotificationItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NotiAI", appName)
  }

  @Test
  fun `test ai engine detection and classification`() {
    val category = NotiAiEngine.detectCategory(
      packageName = "com.slack",
      title = "Equipo DevOps",
      content = "Reunión urgente en 5 minutos para revisar el deploy"
    )
    assertEquals(NotificationCategory.URGENT, category)

    val priority = NotiAiEngine.calculatePriority(
      category = category,
      title = "Equipo DevOps",
      content = "Reunión urgente en 5 minutos"
    )
    assertTrue("La prioridad de un evento urgente debe ser >= 3", priority >= 3)
  }

  @Test
  fun `test room database persistence and dao`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getDatabase(context)
    val dao = db.notificationDao()

    val item = NotificationItem(
      packageName = "com.whatsapp",
      appName = "WhatsApp",
      title = "Carlos",
      content = "Hola, te envié los documentos del contrato",
      timestamp = System.currentTimeMillis()
    )

    val id = dao.insert(item)
    assertTrue("El ID generado debe ser mayor a 0", id > 0)

    val recent = dao.getRecentList(10)
    assertTrue("Debe existir al menos un registro en la base de datos", recent.isNotEmpty())
    assertEquals("WhatsApp", recent.first().appName)
  }

  @Test
  fun `test local model manager initialization`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val modelManager = LocalModelManager(context)
    assertNotNull(modelManager.modelName)
    assertTrue(modelManager.status.value is ModelStatus.NotInstalled || modelManager.status.value is ModelStatus.Installed)
  }
}
