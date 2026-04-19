package com.govorun.lite.model

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

/**
 * Downloads the GigaAM v3 model files over HTTPS with progress callbacks.
 * Uses plain HttpURLConnection — no OkHttp dependency.
 * Skips files that are already fully downloaded (size matches).
 */
object ModelDownloader {

    private const val TAG = "ModelDownloader"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 60_000
    private const val BUFFER_SIZE = 64 * 1024

    data class Progress(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val currentFile: String
    )

    sealed class Result {
        object Success : Result()
        data class Failed(val message: String, val cause: Throwable? = null) : Result()
    }

    /**
     * Downloads all GigaAM files into [GigaAmModel.modelDir]. Resumable per-file
     * in the sense that fully-downloaded files are skipped; partial files are restarted.
     * Cancel via coroutine cancellation.
     */
    suspend fun downloadAll(
        context: Context,
        onProgress: (Progress) -> Unit
    ): Result = withContext(Dispatchers.IO) {
        val dir = GigaAmModel.modelDir(context)
        val totalBytes = GigaAmModel.TOTAL_BYTES
        var downloaded = 0L

        for (spec in GigaAmModel.FILES) {
            if (!coroutineContext.isActive) return@withContext Result.Failed("cancelled")
            val target = File(dir, spec.name)
            if (target.exists() && target.length() == spec.sizeBytes) {
                downloaded += spec.sizeBytes
                onProgress(Progress(downloaded, totalBytes, spec.name))
                continue
            }
            if (target.exists()) target.delete()

            try {
                downloadOne(spec, target) { fileBytes ->
                    onProgress(Progress(downloaded + fileBytes, totalBytes, spec.name))
                }
                downloaded += spec.sizeBytes
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for ${spec.name}", e)
                target.delete()
                return@withContext Result.Failed("Не удалось скачать ${spec.name}: ${e.message}", e)
            }
        }

        if (GigaAmModel.isInstalled(context)) Result.Success
        else Result.Failed("Проверка файлов модели не прошла")
    }

    private suspend fun downloadOne(
        spec: GigaAmModel.ModelFile,
        target: File,
        onBytes: (Long) -> Unit
    ) {
        val conn = (URL(spec.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
        }
        try {
            if (conn.responseCode !in 200..299) {
                throw IllegalStateException("HTTP ${conn.responseCode} for ${spec.url}")
            }
            conn.inputStream.use { input ->
                FileOutputStream(target).use { out ->
                    val buf = ByteArray(BUFFER_SIZE)
                    var totalForFile = 0L
                    while (true) {
                        if (!coroutineContext.isActive) throw InterruptedException("cancelled")
                        val read = input.read(buf)
                        if (read == -1) break
                        out.write(buf, 0, read)
                        totalForFile += read
                        onBytes(totalForFile)
                    }
                }
            }
            if (target.length() != spec.sizeBytes) {
                throw IllegalStateException("Size mismatch: got ${target.length()} expected ${spec.sizeBytes}")
            }
        } finally {
            conn.disconnect()
        }
    }
}
