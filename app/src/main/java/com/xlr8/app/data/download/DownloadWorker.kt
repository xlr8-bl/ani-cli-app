package com.xlr8.app.data.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.xlr8.app.data.local.DownloadDao
import com.xlr8.app.data.local.DownloadEntity
import com.xlr8.app.data.local.DownloadStatus
import com.xlr8.app.data.remote.allanime.AllAnimeApi
import com.xlr8.app.di.ServiceLocator
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

/**
 * Streams a progressive (mp4) episode to local storage with HTTP-Range resume so a paused
 * or interrupted download continues from where it stopped. Progress is written to Room.
 */
class DownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val dao: DownloadDao get() = ServiceLocator.database.downloadDao()

    override suspend fun doWork(): Result {
        val anilistId = inputData.getInt(KEY_ANILIST_ID, -1)
        val episode = inputData.getInt(KEY_EPISODE, -1)
        val translation = inputData.getString(KEY_TRANSLATION) ?: return Result.failure()
        if (anilistId < 0 || episode < 0) return Result.failure()

        val entity = dao.find(anilistId, episode, translation) ?: return Result.failure()
        runCatching { setForeground(foregroundInfo(entity.title)) }
        dao.updateStatus(anilistId, episode, translation, DownloadStatus.RUNNING.name, now())

        return try {
            download(entity)
            dao.updateStatus(anilistId, episode, translation, DownloadStatus.COMPLETED.name, now())
            Result.success()
        } catch (stop: StoppedException) {
            // Cancelled by pause/cancel — leave the partial file for resume.
            dao.updateStatus(anilistId, episode, translation, DownloadStatus.PAUSED.name, now())
            Result.success()
        } catch (t: Throwable) {
            dao.updateStatus(anilistId, episode, translation, DownloadStatus.FAILED.name, now())
            Result.retry()
        }
    }

    private suspend fun download(entity: DownloadEntity) {
        val file = File(entity.localPath)
        file.parentFile?.mkdirs()
        var existing = if (file.exists()) file.length() else 0L

        val connection = (URL(entity.sourceUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            entity.sourceUrlHeaders().forEach { (k, v) -> setRequestProperty(k, v) }
            if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
        }
        connection.connect()

        val code = connection.responseCode
        // If the server ignored our Range (200 instead of 206), restart from scratch.
        if (existing > 0 && code == HttpURLConnection.HTTP_OK) {
            file.delete()
            existing = 0
        }
        if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
            throw IllegalStateException("HTTP $code for ${entity.sourceUrl}")
        }

        val contentLength = connection.contentLengthLong.takeIf { it > 0 } ?: 0L
        val total = if (contentLength > 0) existing + contentLength else 0L

        connection.inputStream.use { input ->
            RandomAccessFile(file, "rw").use { out ->
                out.seek(existing)
                val buffer = ByteArray(64 * 1024)
                var downloaded = existing
                var lastReport = 0L
                while (true) {
                    if (!currentCoroutineContext().isActive || isStopped) throw StoppedException()
                    val read = input.read(buffer)
                    if (read == -1) break
                    out.write(buffer, 0, read)
                    downloaded += read
                    // Throttle DB writes to roughly every 1 MB.
                    if (downloaded - lastReport >= 1_000_000) {
                        lastReport = downloaded
                        dao.updateProgress(entity.anilistId, entity.episode, entity.translation, downloaded, total, now())
                    }
                }
                dao.updateProgress(
                    entity.anilistId, entity.episode, entity.translation,
                    downloaded, if (total > 0) total else downloaded, now(),
                )
            }
        }
        connection.disconnect()
    }

    private fun foregroundInfo(title: String): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW,
            )
            manager.createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun now() = System.currentTimeMillis()

    /** Raised when WorkManager stops the worker (pause/cancel). */
    private class StoppedException : Exception()

    companion object {
        const val KEY_ANILIST_ID = "anilistId"
        const val KEY_EPISODE = "episode"
        const val KEY_TRANSLATION = "translation"
        private const val CHANNEL_ID = "xlr8_downloads"
        private const val NOTIFICATION_ID = 4801
    }
}

/** Headers required by AllAnime CDNs for the download request. */
private fun DownloadEntity.sourceUrlHeaders(): Map<String, String> = mapOf(
    "Referer" to AllAnimeApi.REFERER,
    "User-Agent" to AllAnimeApi.USER_AGENT,
)
