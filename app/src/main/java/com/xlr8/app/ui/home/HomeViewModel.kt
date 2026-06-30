package com.xlr8.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xlr8.app.data.repository.DiscoveryRepository
import com.xlr8.app.data.repository.JustAired
import com.xlr8.app.di.ServiceLocator
import com.xlr8.app.domain.model.Anime
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Immutable snapshot the Home screen renders. */
data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val spotlight: List<Anime> = emptyList(),
    val justAired: List<JustAired> = emptyList(),
    val trending: List<Anime> = emptyList(),
    val popularThisSeason: List<Anime> = emptyList(),
    val upcoming: List<Anime> = emptyList(),
    val top100: List<Anime> = emptyList(),
) {
    val hasContent: Boolean
        get() = trending.isNotEmpty() || popularThisSeason.isNotEmpty() || top100.isNotEmpty()
}

class HomeViewModel(
    private val repository: DiscoveryRepository,
) : ViewModel() {

    // No-arg constructor so the default Compose `viewModel()` factory can build it.
    constructor() : this(ServiceLocator.discoveryRepository)

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() = load(isRefresh = true)

    fun retry() = load()

    private fun load(isRefresh: Boolean = false) {
        _state.value = _state.value.copy(
            isLoading = !isRefresh && !_state.value.hasContent,
            isRefreshing = isRefresh,
            errorMessage = null,
        )
        viewModelScope.launch {
            try {
                // Fetch the feeds concurrently; AniList tolerates these in parallel.
                coroutineScope {
                    val trending = async { repository.trending() }
                    val popular = async { repository.popularThisSeason() }
                    val upcoming = async { repository.upcomingNextSeason() }
                    val top = async { repository.top100() }
                    val aired = async { repository.justAired() }

                    val trendingList = trending.await()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        spotlight = trendingList.filter { !it.bannerImageUrl.isNullOrBlank() }.take(6),
                        trending = trendingList,
                        popularThisSeason = popular.await(),
                        upcoming = upcoming.await(),
                        top100 = top.await(),
                        justAired = aired.await(),
                    )
                }
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = t.message ?: "Couldn't reach AniList. Check your connection.",
                )
            }
        }
    }
}
