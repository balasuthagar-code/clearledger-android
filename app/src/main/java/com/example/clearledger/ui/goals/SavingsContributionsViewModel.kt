
package com.example.clearledger.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.data.repository.SavingsContributionRepository
import com.example.clearledger.data.repository.SavingsGoalRepository
import com.example.clearledger.domain.model.SavingsContribution
import com.example.clearledger.domain.model.SavingsGoal
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SavingsContributionsViewModel @Inject constructor(
    private val goalRepository: SavingsGoalRepository,
    private val contributionRepository: SavingsContributionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SavingsContributionsUiState()
    )

    val uiState: StateFlow<SavingsContributionsUiState> =
        _uiState.asStateFlow()

    private var contributionsJob: Job? = null

    init {
        loadGoals()
    }

    private fun loadGoals() {
        viewModelScope.launch {
            goalRepository.getSavingsGoals()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message
                                ?: "Could not load savings goals."
                        )
                    }
                }
                .collect { goals ->
                    val previousGoalId =
                        _uiState.value.selectedGoal?.id

                    val selectedGoal = goals.firstOrNull {
                        it.id == previousGoalId
                    } ?: goals.firstOrNull()

                    _uiState.update {
                        it.copy(
                            goals = goals,
                            selectedGoal = selectedGoal,
                            isLoading = false,
                            error = null
                        )
                    }

                    if (selectedGoal == null) {
                        contributionsJob?.cancel()
                        _uiState.update {
                            it.copy(contributions = emptyList())
                        }
                    } else if (selectedGoal.id != previousGoalId) {
                        selectGoal(selectedGoal)
                    }
                }
        }
    }

    fun selectGoal(goal: SavingsGoal) {
        contributionsJob?.cancel()

        _uiState.update {
            it.copy(
                selectedGoal = goal,
                contributions = emptyList(),
                error = null,
                message = null
            )
        }

        contributionsJob = viewModelScope.launch {
            contributionRepository.getContributions(goal.id)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message
                                ?: "Could not load contributions."
                        )
                    }
                }
                .collect { contributions ->
                    _uiState.update {
                        it.copy(contributions = contributions)
                    }
                }
        }
    }

    fun saveContribution(
        amountText: String,
        date: String,
        note: String,
        editing: SavingsContribution? = null
    ) {
        val goal = _uiState.value.selectedGoal

        if (goal == null) {
            _uiState.update {
                it.copy(error = "Please select a savings goal.")
            }
            return
        }

        val amount = amountText.toDoubleOrNull()

        if (amount == null || !amount.isFinite() || amount <= 0.0) {
            _uiState.update {
                it.copy(
                    error = "Enter an amount greater than zero."
                )
            }
            return
        }

        val validDate = try {
            !LocalDate.parse(date).isAfter(LocalDate.now())
        } catch (_: Exception) {
            false
        }

        if (!validDate) {
            _uiState.update {
                it.copy(
                    error = "Enter a valid date that is not in the future."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    error = null,
                    message = null
                )
            }

            try {

                val amountMinor = (amount * 100).toLong()

                val contribution = SavingsContribution(
                    id = editing?.id ?: "",
                    goalId = goal.id,
                    amountMinor = amountMinor,
                    date = date,
                    note = note.trim(),
                    createdAt = editing?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )



                if (editing == null) {
                    contributionRepository.addContribution(
                        contribution
                    )
                } else {
                    contributionRepository.updateContribution(
                        contribution
                    )
                }

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = if (editing == null) {
                            "Contribution added."
                        } else {
                            "Contribution updated."
                        },
                        error = null
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = error.message
                            ?: "Could not save contribution."
                    )
                }
            }
        }
    }

    fun deleteContribution(
        contribution: SavingsContribution
    ) {
        viewModelScope.launch {
            try {
                contributionRepository.deleteContribution(
                    goalId = contribution.goalId,
                    contributionId = contribution.id
                )

                _uiState.update {
                    it.copy(
                        message = "Contribution deleted.",
                        error = null
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        error = error.message
                            ?: "Could not delete contribution."
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                error = null,
                message = null
            )
        }
    }
}
// thisanth
