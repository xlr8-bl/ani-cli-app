package com.xlr8.app.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.xlr8.app.data.download.DownloadWorker
import com.xlr8.app.data.local.DownloadDao
import com.xlr8.app.data.local.DownloadEntity
import com.xlr8.app.data.local.DownloadStatus
import com.xlr8.app.domain.model.TranslationType
import com.xlr8.app.domain.model.VideoSource
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Manages offline downloads: enqueues a WorkManager job per episode, tracks state in
 * Room, and exposes pause/resume/cancel. Progressive (mp4) sources are supported;
 * HLS-only sources can't be saved offline yet and are rejected by the caller.
 */
class DownloadRepository(
    private val context: Context,
    private val dao: DownloadDao,
) {

    fun observeAll(): Flow<List<DownloadEntity>> = dao.observeAll()

    fun observeStorageUsed(): Flow<Long> = dao.observeStorageUsed()

    suspend fun completedFor(anilistId: Int, episode: Int): DownloadEntity? {
        val entity = dao.findCompleted(anilistId, episode) ?: return null
        // Guard against a row whose file was deleted out from under us.
        return if (File(entity.localPath).exists()) entity else null
    }

    suspend fun find(anilistId: Int, episode: Int, translation: TranslationType): DownloadEntity? =
        dao.find(anilistId, episode, translation.apiValue)

    /** Queues a progressive download for an episode at the given source/quality. */
    suspend fun enqueue(
        anilistId: Int,
        episode: Int,
        translation: TranslationType,
        title: String,
        coverImageUrl: String?,
        source: VideoSource,
    ) {
        val file = fileFor(anilistId, episode, translation)
        file.parentFile?.mkdirs()
        dao.upsert(
            DownloadEntity(
                anilistId = anilistId,
                episode = episode,
                translation = translation.apiValue,
                title = title,
                coverImageUrl = coverImageUrl,
                quality = source.quality,
                sourceUrl = source.url,
                localPath = file.absolutePath,
                totalBytes = 0,
                downloadedBytes = if (file.exists()) file.length() else 0,
                status = DownloadStatus.QUEUED.name,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        enqueueWork(anilistId, episode, translation, ExistingWorkPolicy.REPLACE)
    }

    suspend fun pause(anilistId: Int, episode: Int, translation: TranslationType) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(anilistId, episode, translation))
        dao.updateStatus(anilistId, episode, translation.apiValue, DownloadStatus.PAUSED.name, now())
    }

    suspend fun resume(anilistId: Int, episode: Int, translation: TranslationType) {
        dao.updateStatus(anilistId, episode, translation.apiValue, DownloadStatus.QUEUED.name, now())
        enqueueWork(anilistId, episode, translation, ExistingWorkPolicy.REPLACE)
    }

    suspend fun delete(anilistId: Int, episode: Int, translation: TranslationType) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(anilistId, episode, translation))
        dao.find(anilistId, episode, translation.apiValue)?.let { File(it.localPath).delete() }
        dao.delete(anilistId, episode, translation.apiValue)
    }

    private fun enqueueWork(
        anilistId: Int,
        episode: Int,
        translation: TranslationType,
        policy: ExistingWorkPolicy,
    ) {
        val data = Data.Builder()
            .putInt(DownloadWorker.KEY_ANILIST_ID, anilistId)
            .putInt(DownloadWorker.KEY_EPISODE, episode)
            .putString(DownloadWorker.KEY_TRANSLATION, translation.apiValue)
            .build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(data)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(anilistId, episode, translation), policy, request)
    }

    private fun fileFor(anilistId: Int, episode: Int, translation: TranslationType): File {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "downloads")
        return File(dir, "${anilistId}_${episode}_${translation.apiValue}.mp4")
    }

    private fun now() = System.currentTimeMillis()

    companion object {
        const val TAG = "xlr8_download"
        fun workName(anilistId: Int, episode: Int, translation: TranslationType) =
            "download_${anilistId}_${episode}_${translation.apiValue}"
    }
}
