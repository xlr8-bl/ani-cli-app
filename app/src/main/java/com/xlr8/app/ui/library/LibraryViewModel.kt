package com.xlr8.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xlr8.app.data.local.WatchlistEntity
import com.xlr8.app.data.repository.LibraryRepository
import com.xlr8.app.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: LibraryRepository,
) : ViewModel() {

    constructor() : this(ServiceLocator.libraryRepository)

    val watchlist: StateFlow<List<WatchlistEntity>> =
        repository.observeWatchlist()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun remove(anilistId: Int) = viewModelScope.launch { repository.remove(anilistId) }
}
