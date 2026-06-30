package com.xlr8.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xlr8.app.data.repository.AnimeDetailRepository
import com.xlr8.app.data.repository.LibraryRepository
import com.xlr8.app.di.ServiceLocator
import com.xlr8.app.domain.model.AnimeDetail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val detail: AnimeDetail? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModel(
    private val repository: AnimeDetailRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    // No-arg constructor for the default Compose `viewModel()` factory.
    constructor() : this(ServiceLocator.animeDetailRepository, ServiceLocator.libraryRepository)

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    private val _currentId = MutableStateFlow<Int?>(null)

    /** Whether the currently-shown show is in the watchlist (live from Room). */
    val inWatchlist: StateFlow<Boolean> = _currentId
        .flatMapLatest { id -> if (id == null) flowOf(false) else libraryRepository.isInWatchlist(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Loads [anilistId], skipping the network if it's already shown. */
    fun load(anilistId: Int) {
        if (_currentId.value == anilistId && _state.value.detail != null) return
        fetch(anilistId)
    }

    /** Switches the page to another show in the season chain, in place. */
    fun switchTo(anilistId: Int) = fetch(anilistId)

    fun retry() = _currentId.value?.let { fetch(it) }

    fun toggleWatchlist() {
        val anime = _state.value.detail?.anime ?: return
        viewModelScope.launch {
            if (inWatchlist.value) libraryRepository.remove(anime.anilistId)
            else libraryRepository.add(anime)
        }
    }

    private fun fetch(anilistId: Int) {
        _currentId.value = anilistId
        _state.value = DetailUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val detail = repository.detail(anilistId)
                // Guard against a slow earlier request landing after a season switch.
                if (_currentId.value == anilistId) {
                    _state.value = DetailUiState(isLoading = false, detail = detail)
                }
            } catch (t: Throwable) {
                if (_currentId.value == anilistId) {
                    _state.value = DetailUiState(
                        isLoading = false,
                        errorMessage = t.message ?: "Couldn't load this show.",
                    )
                }
            }
        }
    }
}
