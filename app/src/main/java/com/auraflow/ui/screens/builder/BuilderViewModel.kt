package com.auraflow.ui.screens.builder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.auraflow.appContainer
import com.auraflow.data.repository.CustomPracticeRepository
import com.auraflow.domain.audio.NatureSound
import com.auraflow.domain.plan.BreathTechnique
import com.auraflow.domain.plan.CustomPractice
import com.auraflow.domain.plan.PracticeBlock
import com.auraflow.domain.plan.PracticeStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BuilderUiState(
    val loading: Boolean = true,
    val editing: Boolean = false,
    val practice: CustomPractice = CustomPractice(
        name = "",
        blocks = listOf(PracticeBlock(PracticeStage.BREATH, 3), PracticeBlock(PracticeStage.SILENCE, 5))
    ),
    val error: String? = null,
    val saved: Boolean = false
) {
    val hasBreath: Boolean get() = practice.blocks.any { it.stage == PracticeStage.BREATH }
    val canAddBlock: Boolean get() = practice.blocks.size < CustomPractice.MAX_BLOCKS
}

class BuilderViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: CustomPracticeRepository
) : ViewModel() {
    private val id: Long = savedStateHandle.get<Long>("id") ?: 0L

    private val _state = MutableStateFlow(BuilderUiState())
    val state: StateFlow<BuilderUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = if (id > 0) repository.get(id) else null
            _state.update { s ->
                if (existing != null) s.copy(loading = false, editing = true, practice = existing) else s.copy(loading = false)
            }
        }
    }

    fun setName(name: String) = edit { it.copy(name = name.take(CustomPractice.MAX_NAME)) }

    fun addBlock(stage: PracticeStage) = edit {
        if (it.blocks.size >= CustomPractice.MAX_BLOCKS) it else it.copy(blocks = it.blocks + PracticeBlock(stage, DEFAULT_MINUTES))
    }

    fun removeBlock(index: Int) = edit { it.copy(blocks = it.blocks.filterIndexed { i, _ -> i != index }) }

    fun changeMinutes(index: Int, delta: Int) = edit { p ->
        p.copy(blocks = p.blocks.mapIndexed { i, b ->
            if (i == index) b.copy(minutes = (b.minutes + delta).coerceIn(1, CustomPractice.MAX_BLOCK_MINUTES)) else b
        })
    }

    fun moveBlock(index: Int, direction: Int) = edit { p ->
        val target = index + direction
        if (target !in p.blocks.indices) {
            p
        } else {
            val blocks = p.blocks.toMutableList()
            blocks[index] = blocks[target].also { blocks[target] = blocks[index] }
            p.copy(blocks = blocks)
        }
    }

    fun selectTechnique(technique: BreathTechnique) = edit { it.copy(technique = technique) }

    fun selectSound(sound: NatureSound?) = edit { it.copy(sound = sound) }

    fun save() {
        val practice = _state.value.practice
        val error = CustomPractice.validate(practice)
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            repository.save(practice.copy(name = practice.name.trim()))
            _state.update { it.copy(saved = true) }
        }
    }

    private fun edit(change: (CustomPractice) -> CustomPractice) =
        _state.update { it.copy(practice = change(it.practice), error = null) }

    companion object {
        private const val DEFAULT_MINUTES = 3

        val Factory = viewModelFactory {
            initializer { BuilderViewModel(createSavedStateHandle(), appContainer().customPracticeRepository) }
        }
    }
}
