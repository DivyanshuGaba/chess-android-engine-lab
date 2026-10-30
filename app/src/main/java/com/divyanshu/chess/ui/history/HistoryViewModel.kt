package com.divyanshu.chess.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyanshu.chess.domain.model.GameRecord
import com.divyanshu.chess.domain.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val isLoading: Boolean = true,
    val games: List<GameRecord> = emptyList(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: GameRepository,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = repository.observeAll()
        .map { HistoryUiState(isLoading = false, games = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun onDelete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun onDeleteAll() {
        viewModelScope.launch { repository.deleteAll() }
    }
}
