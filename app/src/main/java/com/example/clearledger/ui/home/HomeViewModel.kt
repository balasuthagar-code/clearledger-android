
package com.example.clearledger.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.core.common.Result
import com.example.clearledger.data.repository.IncomeEntryRepository
import com.example.clearledger.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth

data class HomeUiState(
    val isLoading: Boolean = true,
    val receivedIncomeMinor: Long = 0L,
    val expectedIncomeMinor: Long = 0L,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val incomeEntryRepository: IncomeEntryRepository
) : ViewModel() {

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadIncomeSummary()
    }

    fun loadIncomeSummary() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null
        )

        incomeEntryRepository.loadEntries { result ->
            result.fold(
                onSuccess = { entries ->
                    val currentMonth = YearMonth.now().toString()

                    val monthlyEntries = entries.filter { entry ->
                        entry.monthKey == currentMonth
                    }

                    _uiState.value = HomeUiState(
                        isLoading = false,
                        receivedIncomeMinor = monthlyEntries
                            .filter { it.status == "RECEIVED" }
                            .sumOf { it.baseAmountMinor },
                        expectedIncomeMinor = monthlyEntries
                            .filter { it.status == "EXPECTED" }
                            .sumOf { it.baseAmountMinor },
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                            ?: "Unable to load income."
                    )
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            when (authRepository.logout()) {
                is Result.Success -> _logoutEvent.emit(Unit)
                is Result.Error -> _logoutEvent.emit(Unit)
            }
        }
    }
}
