package com.example.clearledger.domain.model

data class FreelanceProject(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val client: String = "",
    val agreedAmountMinor: Long = 0L,
    val currency: String = "LKR",
    val exchangeRate: String = "1",
    val baseAmountMinor: Long = 0L,
    val notes: String = "",
    val milestones: List<FreelanceMilestone> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val completedAmountMinor: Long
        get() = milestones
            .filter { it.status == MilestoneStatus.COMPLETED }
            .sumOf { it.amountMinor }

    val progress: Float
        get() = if (agreedAmountMinor <= 0L) {
            0f
        } else {
            (completedAmountMinor.toFloat() / agreedAmountMinor.toFloat())
                .coerceIn(0f, 1f)
        }
}

data class FreelanceMilestone(
    val id: String = "",
    val title: String = "",
    val amountMinor: Long = 0L,
    val dueDate: String = "",
    val status: String = MilestoneStatus.PENDING
) {
    fun isOverdue(today: String): Boolean {
        return status == MilestoneStatus.PENDING &&
                dueDate.isNotBlank() &&
                dueDate < today
    }
}

object MilestoneStatus {
    const val PENDING = "Pending"
    const val COMPLETED = "Completed"

    val values = listOf(PENDING, COMPLETED)
}
//thisanth