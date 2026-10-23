package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.FreelanceProject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FreelanceProjectRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {
    private fun projectsCollection() =
        firestore.collection("users")
            .document(auth.currentUser?.uid
                ?: throw IllegalStateException("Please sign in first."))
            .collection("freelanceProjects")

    fun listenToProjects(
        onSuccess: (List<FreelanceProject>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onError("Please sign in to view your projects.")
            return null
        }

        return projectsCollection()
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Could not load projects.")
                    return@addSnapshotListener
                }

                val projects = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val amount = doc.getDouble("agreedAmount")
                    val rate = doc.getDouble("rate")

                    if (amount == null || rate == null) {
                        return@mapNotNull null
                    }

                    FreelanceProject(
                        id = doc.id,
                        userId = doc.getString("userId") ?: uid,
                        name = doc.getString("name").orEmpty(),
                        client = doc.getString("client").orEmpty(),
                        agreedAmount = amount,
                        currency = doc.getString("currency") ?: "LKR",
                        rate = rate,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }

                onSuccess(projects.sortedByDescending { it.createdAt })
            }
    }

    fun addProject(
        name: String,
        client: String,
        agreedAmount: Double,
        currency: String,
        rate: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onError("Please sign in before saving a project.")
            return
        }

        val document = projectsCollection().document()

        val project = FreelanceProject(
            id = document.id,
            userId = uid,
            name = name.trim(),
            client = client.trim(),
            agreedAmount = agreedAmount,
            currency = currency.trim().uppercase(),
            rate = rate,
            createdAt = System.currentTimeMillis()
        )

        document.set(
            mapOf(
                "id" to project.id,
                "userId" to project.userId,
                "name" to project.name,
                "client" to project.client,
                "agreedAmount" to project.agreedAmount,
                "currency" to project.currency,
                "rate" to project.rate,
                "createdAt" to project.createdAt
            )
        ).addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { exception ->
            onError(exception.localizedMessage ?: "Could not save the project.")
        }
    }
}