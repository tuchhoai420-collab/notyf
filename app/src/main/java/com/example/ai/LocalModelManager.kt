package com.example.ai

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed interface ModelStatus {
    data object NotInstalled : ModelStatus
    data class Downloading(val bytesRead: Long, val totalBytes: Long, val progress: Float) : ModelStatus
    data class Installed(val filePath: String, val sizeMb: Double, val modelName: String) : ModelStatus
    data class Error(val message: String) : ModelStatus
}

class LocalModelManager(private val context: Context) {
    private val TAG = "LocalModelManager"

    val modelName = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
    // HuggingFace direct resolve URL for Qwen2.5-0.5B Instruct GGUF
    val modelUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"

    private val modelsDir: File
        get() = File(context.filesDir, "models").apply { if (!exists()) mkdirs() }

    val modelFile: File
        get() = File(modelsDir, modelName)

    private val _status = MutableStateFlow<ModelStatus>(ModelStatus.NotInstalled)
    val status: StateFlow<ModelStatus> = _status.asStateFlow()

    private var downloadJob: Job? = null
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    init {
        checkStatus()
    }

    fun checkStatus() {
        if (modelFile.exists() && modelFile.length() > 0) {
            val sizeMb = modelFile.length().toDouble() / (1024 * 1024)
            _status.value = ModelStatus.Installed(
                filePath = modelFile.absolutePath,
                sizeMb = String.format("%.2f", sizeMb).toDoubleOrNull() ?: sizeMb,
                modelName = modelName
            )
        } else {
            _status.value = ModelStatus.NotInstalled
        }
    }

    suspend fun downloadModel() = withContext(Dispatchers.IO) {
        if (_status.value is ModelStatus.Downloading) return@withContext

        val tempFile = File(modelsDir, "$modelName.tmp")
        try {
            _status.value = ModelStatus.Downloading(0L, 0L, 0f)

            val request = Request.Builder()
                .url(modelUrl)
                .header("User-Agent", "NotiAI/1.0 (Android)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("Error del servidor HTTP ${response.code}: ${response.message}")
            }

            val body = response.body ?: throw IOException("Cuerpo de respuesta vacío")
            val totalBytes = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Long = 0
                    var read: Int

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read

                        val progress = if (totalBytes > 0) {
                            (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f)
                        } else 0f

                        _status.value = ModelStatus.Downloading(bytesRead, totalBytes, progress)
                    }
                    output.flush()
                }
            }

            // Rename tmp file to final destination
            if (tempFile.exists()) {
                if (modelFile.exists()) modelFile.delete()
                tempFile.renameTo(modelFile)
            }

            checkStatus()
            Log.i(TAG, "Modelo local descargado exitosamente en ${modelFile.absolutePath}")
        } catch (e: CancellationException) {
            tempFile.delete()
            _status.value = ModelStatus.NotInstalled
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Fallo al descargar el modelo local", e)
            tempFile.delete()
            _status.value = ModelStatus.Error(e.localizedMessage ?: "Fallo de conexión al descargar el modelo")
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        val tempFile = File(modelsDir, "$modelName.tmp")
        if (tempFile.exists()) tempFile.delete()
        _status.value = ModelStatus.NotInstalled
    }

    fun deleteModel(): Boolean {
        if (modelFile.exists()) {
            val deleted = modelFile.delete()
            checkStatus()
            return deleted
        }
        return false
    }

    fun getAvailableStorageMb(): Long {
        return try {
            val stat = StatFs(context.filesDir.path)
            (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)
        } catch (e: Exception) {
            -1L
        }
    }
}
