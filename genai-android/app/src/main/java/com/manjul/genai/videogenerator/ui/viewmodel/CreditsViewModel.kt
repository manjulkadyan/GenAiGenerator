package com.manjul.genai.videogenerator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.manjul.genai.videogenerator.data.model.UserCredits
import com.manjul.genai.videogenerator.data.repository.CreditsRepository
import com.manjul.genai.videogenerator.data.repository.RepositoryProvider
import com.manjul.genai.videogenerator.utils.AnalyticsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CreditsViewModel(
    private val repository: CreditsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(UserCredits(credits = 0))
    val state: StateFlow<UserCredits> = _state

    init {
        viewModelScope.launch {
            repository.observeCredits().collect { credits ->
                val previousCredits = _state.value.credits
                _state.value = credits
                
                // Update user property when credits change
                AnalyticsManager.setCreditBalance(credits.credits)
                
                // Track credit balance changes (only if significant change)
                if (kotlin.math.abs(previousCredits - credits.credits) >= 10) {
                    AnalyticsManager.log("Credit balance changed: $previousCredits -> ${credits.credits}")
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                CreditsViewModel(RepositoryProvider.creditsRepository)
            }
        }
    }
}
