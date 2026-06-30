package com.xlr8.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xlr8.app.data.repository.AnimeDetailRepository
import com.xlr8.app.di.ServiceLocator
import com.xlr8.app.domain.model.AnimeDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val detail: AnimeDetail? = null,
)

class DetailViewModel(
    private val repository: AnimeDetailRepository,
) : ViewModel() {

    // No-arg constructor for the default Compose `viewModel()` factory.
    constructor() : this(ServiceLocator.animeDetailRepository)

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    private var currentId: Int? = null

    /** Loads [anilistId], skipping the network if it's already shown. */
    fun load(anilistId: Int) {
        if (currentId == anilistId && _state.value.detail != null) return
        fetch(anilistId)
    }

    /** Switches the page to another show in the season chain, in place. */
    fun switchTo(anilistId: Int) = fetch(anilistId)

    fun retry() = currentId?.let { fetch(it) }

    private fun fetch(anilistId: Int) {
        currentId = anilistId
        _state.value = DetailUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val detail = repository.detail(anilistId)
                // Guard against a slow earlier request landing after a season switch.
                if (currentId == anilistId) {
                    _state.value = DetailUiState(isLoading = false, detail = detail)
                }
            } catch (t: Throwable) {
                if (currentId == anilistId) {
                    _state.value = DetailUiState(
                        isLoading = false,
                        errorMessage = t.message ?: "Couldn't load this show.",
                    )
                }
            }
        }
    }
}
