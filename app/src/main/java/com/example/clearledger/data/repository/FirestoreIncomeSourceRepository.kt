package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.IncomeSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreIncomeSourceRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : IncomeSourceRepository {

    private fun incomeSourcesCollection() =
        firestore.collection("users")
            .document(
                auth.currentUser?.uid
                    ?: throw IllegalStateException("User is not logged in.")
            )
            .collection("incomeSources")

    override fun getIncomeSources(): Flow<List<IncomeSource>> = callbackFlow {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            close(IllegalStateException("User is not logged in."))
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("incomeSources")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val sources = snapshot.documents.mapNotNull { document ->
                        document.toObject(IncomeSource::class.java)
                            ?.copy(id = document.id)
                    }

                    trySend(sources)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun addIncomeSource(source: IncomeSource) {
        require(source.name.isNotBlank()) {
            "Income source name cannot be empty."
        }

        val document = incomeSourcesCollection().document()

        val sourceData = hashMapOf(
            "id" to document.id,
            "name" to source.name.trim(),
            "type" to source.type,
            "defaultCurrency" to source.defaultCurrency,
            "active" to source.active
        )

        document.set(sourceData).await()
    }

    override suspend fun updateIncomeSource(source: IncomeSource) {
        require(source.id.isNotBlank()) {
            "Income source ID cannot be empty."
        }

        require(source.name.isNotBlank()) {
            "Income source name cannot be empty."
        }

        val sourceData = mapOf(
            "name" to source.name.trim(),
            "type" to source.type,
            "defaultCurrency" to source.defaultCurrency,
            "active" to source.active
        )

        incomeSourcesCollection()
            .document(source.id)
            .update(sourceData)
            .await()
    }
}