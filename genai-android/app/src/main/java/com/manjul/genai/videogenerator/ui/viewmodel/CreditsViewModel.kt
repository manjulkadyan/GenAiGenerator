package com.manjul.genai.videogenerator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.manjul.genai.videogenerator.data.model.UserCredits
import com.manjul.genai.videogenerator.data.repository.CreditsRepository
import com.manjul.genai.videogenerator.data.repository.RepositoryProvider
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
                _state.value = credits
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
