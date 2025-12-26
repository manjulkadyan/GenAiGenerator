package com.manjul.genai.videogenerator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.manjul.genai.videogenerator.data.model.VideoEffect
import com.manjul.genai.videogenerator.data.repository.EffectsRepository
import com.manjul.genai.videogenerator.data.repository.FirebaseEffectsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Sorting options for effects
 */
enum class EffectSortOption(val label: String) {
    POPULAR("🔥 Popular"),
    NEW("✨ New"),
    NAME("A-Z"),
    CREDITS("💰 Price")
}

/**
 * Filter categories for effects
 */
enum class EffectFilterCategory(val label: String, val marker: String?) {
    ALL("All", null),
    TRENDING("🔥 Trending", "hot"),
    NEW("✨ New", "new"),
}

data class EffectsState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val effects: List<VideoEffect> = emptyList(),
    val filteredEffects: List<VideoEffect> = emptyList(),
    val selectedCategory: EffectFilterCategory = EffectFilterCategory.ALL,
    val sortOption: EffectSortOption = EffectSortOption.POPULAR,
    val searchQuery: String = "",
    val savedScrollIndex: Int = 0,
    val savedScrollOffset: Int = 0
)

class EffectsViewModel(
    private val repository: EffectsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EffectsState())
    val state: StateFlow<EffectsState> = _state

    init {
        loadEffects()
    }

    private fun loadEffects() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.getEffects()
                .catch { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Failed to load effects"
                        )
                    }
                }
                .collect { effects ->
                    _state.update { currentState ->
                        val sortedEffects = sortAndFilterEffects(
                            effects,
                            currentState.selectedCategory,
                            currentState.sortOption,
                            currentState.searchQuery
                        )
                        currentState.copy(
                            isLoading = false,
                            effects = effects,
                            filteredEffects = sortedEffects
                        )
                    }
                }
        }
    }

    fun setCategory(category: EffectFilterCategory) {
        _state.update { currentState ->
            val sortedEffects = sortAndFilterEffects(
                currentState.effects,
                category,
                currentState.sortOption,
                currentState.searchQuery
            )
            currentState.copy(
                selectedCategory = category,
                filteredEffects = sortedEffects
            )
        }
    }

    fun setSortOption(option: EffectSortOption) {
        _state.update { currentState ->
            val sortedEffects = sortAndFilterEffects(
                currentState.effects,
                currentState.selectedCategory,
                option,
                currentState.searchQuery
            )
            currentState.copy(
                sortOption = option,
                filteredEffects = sortedEffects
            )
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { currentState ->
            val sortedEffects = sortAndFilterEffects(
                currentState.effects,
                currentState.selectedCategory,
                currentState.sortOption,
                query
            )
            currentState.copy(
                searchQuery = query,
                filteredEffects = sortedEffects
            )
        }
    }

    fun saveScrollPosition(index: Int, offset: Int) {
        _state.update { it.copy(savedScrollIndex = index, savedScrollOffset = offset) }
    }

    fun refresh() {
        loadEffects()
    }

    private fun sortAndFilterEffects(
        effects: List<VideoEffect>,
        category: EffectFilterCategory,
        sortOption: EffectSortOption,
        searchQuery: String
    ): List<VideoEffect> {
        // Step 1: Filter by category
        val categoryFiltered = when (category) {
            EffectFilterCategory.ALL -> effects
            EffectFilterCategory.TRENDING -> effects.filter { it.marker == "hot" }
            EffectFilterCategory.NEW -> effects.filter { it.marker == "new" }
        }

        // Step 2: Filter by search query
        val searchFiltered = if (searchQuery.isBlank()) {
            categoryFiltered
        } else {
            categoryFiltered.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.prompt.contains(searchQuery, ignoreCase = true)
            }
        }

        // Step 3: Sort
        return when (sortOption) {
            EffectSortOption.POPULAR -> {
                // Popular: "hot" first, then "new", then "default"
                searchFiltered.sortedWith(
                    compareBy<VideoEffect> {
                        when (it.marker) {
                            "hot" -> 0
                            "new" -> 1
                            else -> 2
                        }
                    }.thenBy { it.name }
                )
            }
            EffectSortOption.NEW -> {
                // New: "new" first, then "hot", then rest
                searchFiltered.sortedWith(
                    compareBy<VideoEffect> {
                        when (it.marker) {
                            "new" -> 0
                            "hot" -> 1
                            else -> 2
                        }
                    }.thenBy { it.name }
                )
            }
            EffectSortOption.NAME -> {
                searchFiltered.sortedBy { it.name }
            }
            EffectSortOption.CREDITS -> {
                searchFiltered.sortedBy { it.credits }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                EffectsViewModel(FirebaseEffectsRepository())
            }
        }
    }
}


