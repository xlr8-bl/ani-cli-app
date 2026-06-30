package com.xlr8.app.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xlr8.app.data.local.DownloadEntity
import com.xlr8.app.data.repository.DownloadRepository
import com.xlr8.app.di.ServiceLocator
import com.xlr8.app.domain.model.TranslationType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val downloads: List<DownloadEntity> = emptyList(),
    val storageUsedBytes: Long = 0,
)

class DownloadsViewModel(
    private val repository: DownloadRepository,
) : ViewModel() {

    constructor() : this(ServiceLocator.downloadRepository)

    val state: StateFlow<DownloadsUiState> =
        combine(repository.observeAll(), repository.observeStorageUsed()) { downloads, used ->
            DownloadsUiState(downloads = downloads, storageUsedBytes = used)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadsUiState())

    private fun translationOf(entity: DownloadEntity): TranslationType =
        if (entity.translation == TranslationType.DUB.apiValue) TranslationType.DUB else TranslationType.SUB

    fun pause(entity: DownloadEntity) = viewModelScope.launch {
        repository.pause(entity.anilistId, entity.episode, translationOf(entity))
    }

    fun resume(entity: DownloadEntity) = viewModelScope.launch {
        repository.resume(entity.anilistId, entity.episode, translationOf(entity))
    }

    fun delete(entity: DownloadEntity) = viewModelScope.launch {
        repository.delete(entity.anilistId, entity.episode, translationOf(entity))
    }
}
