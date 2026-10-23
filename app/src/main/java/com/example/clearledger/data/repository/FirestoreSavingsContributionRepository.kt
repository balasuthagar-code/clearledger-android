
package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.SavingsContribution
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreSavingsContributionRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : SavingsContributionRepository {

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in first.")
    }

    private fun contributionsCollection(
        goalId: String
    ) = firestore.collection("users")
        .document(requireUserId())
        .collection("savingsGoals")
        .document(goalId)
        .collection("contributions")

    override fun getContributions(
        goalId: String
    ): Flow<List<SavingsContribution>> = callbackFlow {

        val uid = auth.currentUser?.uid

        if (uid == null) {
            close(IllegalStateException("Please sign in first."))
            return@callbackFlow
        }

        val collection = firestore.collection("users")
            .document(uid)
            .collection("savingsGoals")
            .document(goalId)
            .collection("contributions")

        val registration: ListenerRegistration =
            collection.addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val contributions = snapshot?.documents
                    ?.mapNotNull { document ->
                        document.toObject(
                            SavingsContribution::class.java
                        )?.copy(
                            id = document.id,
                            goalId = goalId
                        )
                    }
                    ?.sortedByDescending { it.date }
                    .orEmpty()

                trySend(contributions)
            }

        awaitClose {
            registration.remove()
        }
    }

    override suspend fun addContribution(
        contribution: SavingsContribution
    ) {
        require(contribution.goalId.isNotBlank()) {
            "Goal ID is required."
        }

        require(contribution.amountMinor > 0L) {
            "Contribution amount must be greater than zero."
        }

        require(contribution.date.isNotBlank()) {
            "Contribution date is required."
        }

        val now = System.currentTimeMillis()
        val document = contributionsCollection(
            contribution.goalId
        ).document()

        val data = mapOf(
            "id" to document.id,
            "goalId" to contribution.goalId,
            "amountMinor" to contribution.amountMinor,
            "date" to contribution.date,
            "note" to contribution.note,
            "createdAt" to now,
            "updatedAt" to now
        )

        document.set(data).await()
    }

    override suspend fun updateContribution(
        contribution: SavingsContribution
    ) {
        require(contribution.goalId.isNotBlank()) {
            "Goal ID is required."
        }

        require(contribution.id.isNotBlank()) {
            "Contribution ID is required."
        }

        require(contribution.amountMinor > 0L) {
            "Contribution amount must be greater than zero."
        }

        require(contribution.date.isNotBlank()) {
            "Contribution date is required."
        }

        val data = mapOf(
            "amountMinor" to contribution.amountMinor,
            "date" to contribution.date,
            "note" to contribution.note,
            "updatedAt" to System.currentTimeMillis()
        )

        contributionsCollection(contribution.goalId)
            .document(contribution.id)
            .update(data)
            .await()
    }

    override suspend fun deleteContribution(
        goalId: String,
        contributionId: String
    ) {
        require(goalId.isNotBlank()) {
            "Goal ID is required."
        }

        require(contributionId.isNotBlank()) {
            "Contribution ID is required."
        }

        contributionsCollection(goalId)
            .document(contributionId)
            .delete()
            .await()
    }
}
// thisanth