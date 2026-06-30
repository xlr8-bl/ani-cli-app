package com.xlr8.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xlr8.app.data.local.RecentSearchEntity
import com.xlr8.app.data.repository.SearchRepository
import com.xlr8.app.di.ServiceLocator
import com.xlr8.app.domain.model.Anime
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchFilters(
    val genre: String? = null,
    val year: Int? = null,
    val format: String? = null,
) {
    val isEmpty: Boolean get() = genre == null && year == null && format == null
}

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<Anime> = emptyList(),
    val filters: SearchFilters = SearchFilters(),
    val errorMessage: String? = null,
    val hasSearched: Boolean = false,
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val repository: SearchRepository,
) : ViewModel() {

    constructor() : this(ServiceLocator.searchRepository)

    private val _query = MutableStateFlow("")
    private val _filters = MutableStateFlow(SearchFilters())
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    val recentSearches: StateFlow<List<RecentSearchEntity>> =
        repository.recentSearches().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            combine(
                _query.debounce(300).distinctUntilChanged(),
                _filters,
            ) { query, filters -> query to filters }
                .collectLatest { (query, filters) -> runSearch(query, filters) }
        }
    }

    private suspend fun runSearch(query: String, filters: SearchFilters) {
        if (query.isBlank() && filters.isEmpty) {
            _state.value = _state.value.copy(
                query = query, filters = filters, results = emptyList(),
                isLoading = false, hasSearched = false, errorMessage = null,
            )
            return
        }
        _state.value = _state.value.copy(query = query, filters = filters, isLoading = true, errorMessage = null)
        try {
            val results = repository.search(
                query = query,
                genre = filters.genre,
                seasonYear = filters.year,
                format = filters.format,
            )
            _state.value = _state.value.copy(results = results, isLoading = false, hasSearched = true)
        } catch (t: Throwable) {
            _state.value = _state.value.copy(
                isLoading = false, hasSearched = true,
                errorMessage = t.message ?: "Search failed.",
            )
        }
    }

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun onSubmit() {
        viewModelScope.launch { repository.rememberQuery(_query.value) }
    }

    fun useRecent(query: String) {
        _query.value = query
    }

    fun setGenre(genre: String?) { _filters.value = _filters.value.copy(genre = genre) }
    fun setYear(year: Int?) { _filters.value = _filters.value.copy(year = year) }
    fun setFormat(format: String?) { _filters.value = _filters.value.copy(format = format) }

    fun clearRecent(query: String) {
        viewModelScope.launch { repository.removeRecent(query) }
    }
}
