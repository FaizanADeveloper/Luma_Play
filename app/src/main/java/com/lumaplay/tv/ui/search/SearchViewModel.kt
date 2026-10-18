package com.lumaplay.tv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumaplay.tv.data.repository.SearchRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing query updates, debounce scheduling, category filtering,
 * and result state for Universal Offline Search.
 */
class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val debounceMs: Long = DEFAULT_DEBOUNCE_MS
) : ViewModel() {

    companion object {
        const val DEFAULT_DEBOUNCE_MS = 250L
    }

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch(coroutineDispatcher) {
            _queryFlow
                .map { it.trim() }
                .distinctUntilChanged()
                .collectLatest { trimmedQuery ->
                    if (trimmedQuery.isEmpty()) {
                        _uiState.update { it.copy(status = SearchStatus.Idle) }
                        return@collectLatest
                    }

                    if (debounceMs > 0L) {
                        delay(debounceMs)
                    }

                    _uiState.update { it.copy(status = SearchStatus.Loading) }

                    val searchResult = searchRepository.search(trimmedQuery)

                    _uiState.update { current ->
                        // Guard against updating with a stale result if query was changed
                        if (current.query.trim() != trimmedQuery) return@update current

                        searchResult.fold(
                            onSuccess = { results ->
                                current.copy(
                                    status = if (results.isEmpty) {
                                        SearchStatus.Empty
                                    } else {
                                        SearchStatus.Success(results)
                                    }
                                )
                            },
                            onFailure = { error ->
                                current.copy(
                                    status = SearchStatus.Error(
                                        error.message ?: "Failed to perform search"
                                    )
                                )
                            }
                        )
                    }
                }
        }
    }

    /**
     * Updates the current search query.
     *
     * The typed query is reflected immediately in [uiState] for fluid UI input,
     * while the underlying repository search is debounced by [debounceMs].
     */
    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        _queryFlow.value = newQuery
    }

    /**
     * Updates the active filter category without issuing redundant repository requests.
     */
    fun onCategorySelected(category: SearchCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    /**
     * Clears the active query and resets status to [SearchStatus.Idle].
     */
    fun onClearQuery() {
        _uiState.update { it.copy(query = "", status = SearchStatus.Idle) }
        _queryFlow.value = ""
    }
}
