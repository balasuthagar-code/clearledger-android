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

    private fun contributionsCollection(goalId: String) =
        firestore.collection("users")
            .document(
                auth.currentUser?.uid
                    ?: throw IllegalStateException("Please sign in first")
            )
            .collection("savingsGoals")
            .document(goalId)
            .collection("contributions")

    override fun getContributions(
        goalId: String
    ): Flow<List<SavingsContribution>> = callbackFlow {

        val uid = auth.currentUser?.uid

        if (uid == null) {
            close(IllegalStateException("Please sign in first"))
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

                val contributions =
                    snapshot?.documents?.mapNotNull { document ->
                        document.toObject(SavingsContribution::class.java)
                            ?.copy(
                                id = document.id,
                                goalId = goalId
                            )
                    }.orEmpty()
                        .sortedByDescending { it.date }

                trySend(contributions)
            }

        awaitClose {
            registration.remove()
        }
    }

    override suspend fun addContribution(
        contribution: SavingsContribution
    ) {
        require(contribution.amount > 0) {
            "Contribution amount must be greater than zero."
        }

        val document = contributionsCollection(
            contribution.goalId
        ).document()

        document.set(
            mapOf(
                "id" to document.id,
                "goalId" to contribution.goalId,
                "amount" to contribution.amount,
                "date" to contribution.date,
                "note" to contribution.note
            )
        ).await()
    }

    override suspend fun updateContribution(
        contribution: SavingsContribution
    ) {
        require(contribution.id.isNotBlank()) {
            "Contribution ID is missing."
        }

        require(contribution.amount > 0) {
            "Contribution amount must be greater than zero."
        }

        contributionsCollection(contribution.goalId)
            .document(contribution.id)
            .update(
                mapOf(
                    "amount" to contribution.amount,
                    "date" to contribution.date,
                    "note" to contribution.note
                )
            ).await()
    }

    override suspend fun deleteContribution(
        goalId: String,
        contributionId: String
    ) {
        contributionsCollection(goalId)
            .document(contributionId)
            .delete()
            .await()
    }
}