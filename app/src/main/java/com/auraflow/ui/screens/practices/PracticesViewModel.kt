package com.auraflow.ui.screens.practices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.auraflow.appContainer
import com.auraflow.data.repository.CustomPracticeRepository
import com.auraflow.domain.plan.CustomPractice
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PracticesViewModel(private val repository: CustomPracticeRepository) : ViewModel() {

    val practices: StateFlow<List<CustomPractice>> =
        repository.practices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { PracticesViewModel(appContainer().customPracticeRepository) }
        }
    }
}
