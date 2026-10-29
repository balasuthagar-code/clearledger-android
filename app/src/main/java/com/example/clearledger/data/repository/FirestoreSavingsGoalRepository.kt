
package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.SavingsGoal
import com.example.clearledger.domain.model.GoalSummary
import com.example.clearledger.domain.repository.PrimaryGoalSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreSavingsGoalRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val contributionRepository: SavingsContributionRepository
) : SavingsGoalRepository, PrimaryGoalSource {

    private fun goalsCollection() =
        firestore.collection("users")
            .document(
                auth.currentUser?.uid
                    ?: throw IllegalStateException("Please sign in first.")
            )
            .collection("savingsGoals")

    private fun observeGoalDocuments(): Flow<List<SavingsGoal>> = callbackFlow {
            val uid = auth.currentUser?.uid

            if (uid == null) {
                close(IllegalStateException("Please sign in first."))
                return@callbackFlow
            }

            val collection = firestore.collection("users")
                .document(uid)
                .collection("savingsGoals")

            val registration: ListenerRegistration =
                collection.addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val goals = snapshot?.documents
                        ?.mapNotNull { document ->
                            document.toObject(SavingsGoal::class.java)
                                ?.copy(
                                    id = document.id,
                                    userId = uid
                                )
                        }
                        .orEmpty()

                    trySend(goals)
                }

            awaitClose {
                registration.remove()
            }
        }

    override fun getSavingsGoals(): Flow<List<SavingsGoal>> =
        observeGoalDocuments().flatMapLatest { goals ->
            if (goals.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    goals.map { goal -> contributionRepository.getContributions(goal.id) }
                ) { contributionLists ->
                    goals.mapIndexed { index, goal ->
                        goal.copy(
                            contributions = contributionLists[index]
                                .distinctBy { it.id }
                        )
                    }
                }
            }
        }

    override suspend fun addSavingsGoal(goal: SavingsGoal) {
        require(goal.name.isNotBlank()) {
            "Goal name is required."
        }

        require(goal.targetAmountMinor > 0L) {
            "Target amount must be greater than zero."
        }

        require(goal.startingSavedMinor >= 0L) {
            "Starting saved amount cannot be negative."
        }

        require(goal.startingSavedMinor <= goal.targetAmountMinor) {
            "Starting saved amount cannot exceed the target."
        }

        require(goal.currency.isNotBlank()) {
            "Currency is required."
        }

        require(goal.deadline.isNotBlank()) {
            "Deadline is required."
        }

        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in first.")

        val now = System.currentTimeMillis()
        val document = goalsCollection().document()

        val goalData = mapOf(
            "id" to document.id,
            "userId" to uid,
            "name" to goal.name.trim(),
            "targetAmountMinor" to goal.targetAmountMinor,
            "currency" to goal.currency,
            "deadline" to goal.deadline,
            "startingSavedMinor" to goal.startingSavedMinor,
            "isPrimary" to goal.isPrimary,
            "createdAt" to now,
            "updatedAt" to now
        )

        document.set(goalData).await()
    }

    override fun observePrimaryGoal(): Flow<GoalSummary?> =
        getSavingsGoals()
            .map { goals -> goals.firstOrNull { it.isPrimary } ?: goals.firstOrNull() }
            .flatMapLatest { goal ->
                if (goal == null) {
                    kotlinx.coroutines.flow.flowOf(null)
                } else {
                    contributionRepository.getContributions(goal.id).map { contributions ->
                        val saved = goal.startingSavedMinor + contributions
                            .distinctBy { it.id }
                            .sumOf { it.amountMinor }
                        val months = runCatching {
                            ChronoUnit.MONTHS.between(
                                java.time.YearMonth.now(),
                                java.time.YearMonth.from(LocalDate.parse(goal.deadline))
                            ).coerceAtLeast(1L)
                        }.getOrDefault(1L)
                        GoalSummary(
                            name = goal.name,
                            savedMinor = saved,
                            targetMinor = goal.targetAmountMinor,
                            requiredMonthlyMinor = ((goal.targetAmountMinor - saved)
                                .coerceAtLeast(0L) + months - 1L) / months
                        )
                    }
                }
            }

    suspend fun updateSavingsGoal(goal: SavingsGoal) {
        require(goal.id.isNotBlank()) {
            "Goal ID is required."
        }

        require(goal.name.isNotBlank()) {
            "Goal name is required."
        }

        require(goal.targetAmountMinor > 0L) {
            "Target amount must be greater than zero."
        }

        require(goal.startingSavedMinor in 0L..goal.targetAmountMinor) {
            "Starting saved amount must be between zero and the target."
        }

        val updatedData = mapOf(
            "name" to goal.name.trim(),
            "targetAmountMinor" to goal.targetAmountMinor,
            "currency" to goal.currency,
            "deadline" to goal.deadline,
            "startingSavedMinor" to goal.startingSavedMinor,
            "isPrimary" to goal.isPrimary,
            "updatedAt" to System.currentTimeMillis()
        )

        goalsCollection()
            .document(goal.id)
            .update(updatedData)
            .await()
    }

    suspend fun deleteSavingsGoal(goalId: String) {
        require(goalId.isNotBlank()) {
            "Goal ID is required."
        }

        goalsCollection()
            .document(goalId)
            .delete()
            .await()
    }
}
// thisanth