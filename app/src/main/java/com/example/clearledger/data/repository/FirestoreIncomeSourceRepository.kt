
package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.IncomeSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreIncomeSourceRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IncomeSourceRepository {

    private fun sourcesCollection() =
        firestore.collection("users")
            .document(requireUserId())
            .collection("incomeSources")

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in first.")

    override fun getIncomeSources(): Flow<List<IncomeSource>> = callbackFlow {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            close(IllegalStateException("Please sign in first."))
            return@callbackFlow
        }

        val registration = firestore.collection("users")
            .document(uid)
            .collection("incomeSources")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val sources = snapshot?.documents.orEmpty().map { document ->
                    IncomeSource(
                        id = document.id,
                        name = document.getString("name").orEmpty(),
                        type = document.getString("type").orEmpty(),
                        defaultCurrency = document.getString("defaultCurrency") ?: "LKR",
                        active = document.getBoolean("active") ?: true
                    )
                }

                trySend(sources)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun addIncomeSource(source: IncomeSource) {
        val data = mapOf(
            "name" to source.name.trim(),
            "type" to source.type,
            "defaultCurrency" to source.defaultCurrency,
            "active" to source.active
        )

        sourcesCollection().add(data).await()
    }

    override suspend fun updateIncomeSource(source: IncomeSource) {
        require(source.id.isNotBlank()) { "Income source ID is required." }

        val data = mapOf(
            "name" to source.name.trim(),
            "type" to source.type,
            "defaultCurrency" to source.defaultCurrency,
            "active" to source.active
        )

        sourcesCollection()
            .document(source.id)
            .update(data)
            .await()
    }
}
