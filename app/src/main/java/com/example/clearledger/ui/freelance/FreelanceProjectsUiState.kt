package com.example.clearledger.ui.freelance

import com.example.clearledger.domain.model.FreelanceProject

data class FreelanceProjectsUiState(
    val projects: List<FreelanceProject> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val updatingStatusProjectId: String? = null
)
