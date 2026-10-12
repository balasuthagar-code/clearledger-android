package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.SavingsGoal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSavingsGoalRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : SavingsGoalRepository {

    private fun goalsCollection() =
        firestore.collection("users")
            .document(auth.currentUser?.uid
                ?: throw IllegalStateException("Please sign in first"))
            .collection("savingsGoals")

    override fun getSavingsGoals(): Flow<List<SavingsGoal>> =
        callbackFlow {
            val uid = auth.currentUser?.uid

            if (uid == null) {
                trySend(emptyList())
                close()
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

                    val goals = snapshot?.documents?.mapNotNull { document ->
                        document.toObject(SavingsGoal::class.java)
                            ?.copy(id = document.id)
                    }.orEmpty()

                    trySend(goals)
                }

            awaitClose { registration.remove() }
        }

    override suspend fun addSavingsGoal(goal: SavingsGoal) {
        val document = goalsCollection().document()

        document.set(
            mapOf(
                "id" to document.id,
                "name" to goal.name.trim(),
                "targetAmount" to goal.targetAmount,
                "currency" to goal.currency,
                "deadline" to goal.deadline,
                "startingAmount" to goal.startingAmount
            )
        ).await()
    }
}