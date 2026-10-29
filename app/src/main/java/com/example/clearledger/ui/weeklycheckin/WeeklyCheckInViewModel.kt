
package com.example.clearledger.ui.weeklycheckin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.data.repository.WeeklyCheckInRepository
import com.example.clearledger.domain.model.WeeklyCheckIn
import com.example.clearledger.domain.repository.ExpenseRepository
import com.example.clearledger.domain.repository.IncomeSummarySource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import com.example.clearledger.core.common.MonthKey

@HiltViewModel
class WeeklyCheckInViewModel @Inject constructor(
    private val repository: WeeklyCheckInRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeSummarySource: IncomeSummarySource
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyCheckInUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadWeeklyCheckIns()
    }

    private fun loadWeeklyCheckIns() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val monthKey = MonthKey.current()
                combine(
                    repository.getWeeklyCheckIns(),
                    expenseRepository.observeExpensesForMonth(monthKey),
                    incomeSummarySource.observeReceivedBaseAmounts(monthKey)
                ) { checkIns, expenses, income ->
                    checkIns to (income.sum() - expenses.sumOf { it.baseAmountMinor })
                }.collect { (checkIns, expectedBalance) ->
                    _uiState.update {
                        it.copy(
                            checkIns = checkIns,
                            expectedBalanceMinor = expectedBalance,
                            isLoading = false
                        )
                    }
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message
                            ?: "Unable to load weekly check-ins."
                    )
                }
            }
        }
    }

    fun onReportedBalanceChange(value: String) {
        _uiState.update {
            it.copy(
                reportedBalanceText = value,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onNoteChange(value: String) {
        _uiState.update {
            it.copy(
                note = value,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun saveWeeklyCheckIn() {
        val currentState = _uiState.value
        val reportedBalance = currentState.reportedBalanceMinor

        if (reportedBalance == null) {
            _uiState.update {
                it.copy(
                    errorMessage = "Enter a valid balance greater than or equal to zero."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    errorMessage = null,
                    successMessage = null
                )
            }

            try {
                val weekKey = LocalDate.now()
                    .with(DayOfWeek.MONDAY)
                    .toString()

                val existingCheckIn = currentState.checkIns
                    .firstOrNull { it.weekKey == weekKey }

                val checkIn = WeeklyCheckIn(
                    id = existingCheckIn?.id ?: "",
                    weekKey = weekKey,
                    expectedBalanceMinor =
                        currentState.expectedBalanceMinor,
                    reportedBalanceMinor = reportedBalance,
                    gapMinor = reportedBalance -
                            currentState.expectedBalanceMinor,
                    note = currentState.note.trim(),
                    createdAt = existingCheckIn?.createdAt ?: 0L,
                    updatedAt = System.currentTimeMillis()
                )

                repository.saveWeeklyCheckIn(checkIn)

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        successMessage = "Weekly check-in saved.",
                        reportedBalanceText = "",
                        note = ""
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = exception.message
                            ?: "Unable to save the weekly check-in."
                    )
                }
            }
        }
    }
}
// thsianth