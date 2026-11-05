package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.FreelanceMilestone
import com.example.clearledger.domain.model.FreelanceProject
import com.google.firebase.firestore.ListenerRegistration

interface FreelanceProjectRepository {
    fun listenToProjects(
        onSuccess: (List<FreelanceProject>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration?

    fun addProject(
        name: String,
        client: String,
        clientContact: String = "",
        description: String = "",
        startDate: String = "",
        endDate: String = "",
        workDescription: String = "",
        status: String = "PLANNED",
        agreedAmountMinor: Long,
        currency: String,
        exchangeRate: String,
        baseAmountMinor: Long,
        notes: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )

    suspend fun recordPayment(
        project: FreelanceProject,
        amountMinor: Long,
        paymentDate: String,
        note: String
    )

    fun updateMilestones(
        projectId: String,
        milestones: List<FreelanceMilestone>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )

    fun updateProjectStatus(
        projectId: String,
        status: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )

    fun deleteProject(
        projectId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
}
