package com.example.clearledger.ui.freelance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.data.repository.FreelanceProjectRepository
import com.example.clearledger.domain.model.FreelanceProject
import com.example.clearledger.domain.model.FreelanceProjectStatus
import com.example.clearledger.domain.model.FreelanceMilestone
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class FreelanceProjectsViewModel @Inject constructor(
    private val repository: FreelanceProjectRepository
) : ViewModel() {

    var state = androidx.compose.runtime.mutableStateOf(FreelanceProjectsUiState())
        private set

    private var listener = repository.listenToProjects(
        onSuccess = { projects ->
            state.value = state.value.copy(
                projects = projects,
                isLoading = false,
                error = null
            )
        },
        onError = { message ->
            state.value = state.value.copy(isLoading = false, error = message)
        }
    )

    fun addProject(
        name: String,
        client: String,
        clientContact: String,
        description: String,
        startDate: String,
        endDate: String,
        workDescription: String,
        status: String,
        agreedAmountMinor: Long,
        currency: String,
        exchangeRate: String,
        baseAmountMinor: Long,
        notes: String,
        onSuccess: () -> Unit
    ) {
        state.value = state.value.copy(isSaving = true, error = null)
        repository.addProject(
            name, client, clientContact, description, startDate, endDate,
            workDescription, status, agreedAmountMinor, currency, exchangeRate,
            baseAmountMinor, notes,
            onSuccess = {
                state.value = state.value.copy(isSaving = false, error = null)
                onSuccess()
            },
            onError = { message ->
                state.value = state.value.copy(isSaving = false, error = message)
            }
        )
    }

    fun updateProjectStatus(projectId: String, status: String) {
        state.value = state.value.copy(
            updatingStatusProjectId = projectId,
            error = null,
            message = null
        )
        repository.updateProjectStatus(
            projectId,
            status,
            onSuccess = {
                state.value = state.value.copy(
                    updatingStatusProjectId = null,
                    message = "Project status updated.",
                    error = null
                )
            },
            onError = { message ->
                state.value = state.value.copy(
                    updatingStatusProjectId = null,
                    message = null,
                    error = message
                )
            }
        )
    }

    fun deleteProject(projectId: String, onComplete: () -> Unit) {
        repository.deleteProject(
            projectId,
            onSuccess = {
                state.value = state.value.copy(error = null)
                onComplete()
            },
            onError = { message ->
                state.value = state.value.copy(error = message)
                onComplete()
            }
        )
    }

    fun recordPayment(
        project: FreelanceProject,
        amountMinor: Long,
        paymentDate: String,
        note: String,
        onSuccess: () -> Unit
    ) {
        state.value = state.value.copy(isSaving = true, error = null)
        viewModelScope.launch {
            runCatching {
                repository.recordPayment(project, amountMinor, paymentDate, note)
            }.onSuccess {
                state.value = state.value.copy(isSaving = false, error = null)
                onSuccess()
            }.onFailure {
                state.value = state.value.copy(
                    isSaving = false,
                    error = it.message ?: "Unable to record payment."
                )
            }
        }
    }

    fun updateMilestones(
        projectId: String,
        milestones: List<FreelanceMilestone>
    ) {
        repository.updateMilestones(
            projectId,
            milestones,
            onSuccess = { state.value = state.value.copy(error = null) },
            onError = { message -> state.value = state.value.copy(error = message) }
        )
    }

    fun clearMessages() {
        state.value = state.value.copy(error = null, message = null)
    }

    fun showError(message: String) {
        state.value = state.value.copy(error = message, message = null)
    }

    override fun onCleared() {
        listener?.remove()
        listener = null
        super.onCleared()
    }
}
