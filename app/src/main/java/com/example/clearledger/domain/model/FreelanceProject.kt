package com.example.clearledger.domain.model

data class FreelanceProject(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val client: String = "",
    val clientContact: String = "",
    val description: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val workDescription: String = "",
    val status: String = FreelanceProjectStatus.PLANNED,
    val paymentStatus: String = FreelancePaymentStatus.UNPAID,
    val totalPaidBaseMinor: Long = 0L,
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

    val outstandingBalanceMinor: Long
        get() = (baseAmountMinor - totalPaidBaseMinor).coerceAtLeast(0L)

    val progress: Float
        get() = if (agreedAmountMinor <= 0L) {
            0f
        } else {
            (completedAmountMinor.toFloat() / agreedAmountMinor.toFloat())
                .coerceIn(0f, 1f)
        }

}

object FreelanceProjectStatus {
    const val PLANNED = "PLANNED"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val COMPLETED = "COMPLETED"
    const val CANCELLED = "CANCELLED"
    val values = listOf(PLANNED, IN_PROGRESS, COMPLETED, CANCELLED)
}

object FreelancePaymentStatus {
    const val UNPAID = "UNPAID"
    const val PARTIALLY_PAID = "PARTIALLY_PAID"
    const val PAID = "PAID"
    val values = listOf(UNPAID, PARTIALLY_PAID, PAID)
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