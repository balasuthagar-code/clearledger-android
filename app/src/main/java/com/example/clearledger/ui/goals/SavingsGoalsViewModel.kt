package com.example.clearledger.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.core.common.SavingsGoalValidator
import com.example.clearledger.data.repository.SavingsGoalRepository
import com.example.clearledger.domain.model.SavingsGoal
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SavingsGoalsViewModel @Inject constructor(
    private val repository: SavingsGoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavingsGoalsUiState())
    val uiState: StateFlow<SavingsGoalsUiState> = _uiState.asStateFlow()

    init {
        loadGoals()
    }

    private fun loadGoals() {
        viewModelScope.launch {
            repository.getSavingsGoals()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Could not load savings goals."
                        )
                    }
                }
                .collect { goals ->
                    _uiState.update {
                        it.copy(
                            goals = goals,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun createGoal(
        name: String,
        targetAmount: String,
        currency: String,
        deadline: String,
        startingAmount: String
    ) {
        val validationError = SavingsGoalValidator.validate(
            name,
            targetAmount,
            currency,
            deadline,
            startingAmount
        )

        if (validationError != null) {
            _uiState.update {
                it.copy(error = validationError, message = null)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSaving = true, error = null, message = null)
            }

            try {

                val targetAmountValue = targetAmount.toDoubleOrNull() ?: 0.0
                val startingAmountValue = startingAmount.toDoubleOrNull() ?: 0.0

                val goal = SavingsGoal(
                    name = name.trim(),
                    targetAmountMinor = (targetAmountValue * 100).toLong(),
                    currency = currency,
                    deadline = deadline.trim(),
                    startingSavedMinor = (startingAmountValue * 100).toLong()
                )



                repository.addSavingsGoal(goal)

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = "Savings goal created successfully."
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = error.message ?: "Could not save the goal."
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(error = null, message = null)
        }
    }
}
//thisanth