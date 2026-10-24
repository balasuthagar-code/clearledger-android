package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.FreelanceMilestone
import com.example.clearledger.domain.model.FreelanceProject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

class FreelanceProjectRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun projectsCollection() =
        auth.currentUser?.uid?.let { uid ->
            db.collection("users")
                .document(uid)
                .collection("freelanceProjects")
        }

    fun listenToProjects(
        onSuccess: (List<FreelanceProject>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val collection = projectsCollection()

        if (collection == null) {
            onError("Please sign in to view your projects.")
            return null
        }

        return collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.localizedMessage ?: "Unable to load projects.")
                return@addSnapshotListener
            }

            val projects = snapshot?.documents.orEmpty().mapNotNull { document ->
                try {
                    val milestones = (document.get("milestones") as? List<*>)
                        .orEmpty()
                        .mapNotNull { item ->
                            val map = item as? Map<*, *> ?: return@mapNotNull null

                            FreelanceMilestone(
                                id = map["id"] as? String ?: "",
                                title = map["title"] as? String ?: "",
                                amountMinor = (map["amountMinor"] as? Number)?.toLong() ?: 0L,
                                dueDate = map["dueDate"] as? String ?: "",
                                status = map["status"] as? String
                                    ?: FreelanceMilestoneDefaults.PENDING
                            )
                        }

                    FreelanceProject(
                        id = document.id,
                        userId = document.getString("userId") ?: "",
                        name = document.getString("name") ?: "",
                        client = document.getString("client") ?: "",
                        agreedAmountMinor = document.getLong("agreedAmountMinor") ?: 0L,
                        currency = document.getString("currency") ?: "LKR",
                        exchangeRate = document.getString("exchangeRate") ?: "1",
                        baseAmountMinor = document.getLong("baseAmountMinor") ?: 0L,
                        notes = document.getString("notes") ?: "",
                        milestones = milestones,
                        createdAt = document.getLong("createdAt") ?: 0L,
                        updatedAt = document.getLong("updatedAt") ?: 0L
                    )
                } catch (e: Exception) {
                    null
                }
            }

            onSuccess(projects)
        }
    }

    fun addProject(
        name: String,
        client: String,
        agreedAmountMinor: Long,
        currency: String,
        exchangeRate: String,
        baseAmountMinor: Long,
        notes: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onError("Please sign in before adding a project.")
            return
        }

        val now = System.currentTimeMillis()
        val document = projectsCollection()?.document()

        if (document == null) {
            onError("Unable to access your projects.")
            return
        }

        val data = hashMapOf(
            "id" to document.id,
            "userId" to user.uid,
            "name" to name.trim(),
            "client" to client.trim(),
            "agreedAmountMinor" to agreedAmountMinor,
            "currency" to currency,
            "exchangeRate" to exchangeRate,
            "baseAmountMinor" to baseAmountMinor,
            "notes" to notes.trim(),
            "milestones" to emptyList<Map<String, Any>>(),
            "createdAt" to now,
            "updatedAt" to now
        )

        document.set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error ->
                onError(error.localizedMessage ?: "Unable to save the project.")
            }
    }

    fun updateMilestones(
        projectId: String,
        milestones: List<FreelanceMilestone>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val collection = projectsCollection()

        if (collection == null) {
            onError("Please sign in before updating a project.")
            return
        }

        val milestoneMaps = milestones.map { milestone ->
            mapOf(
                "id" to milestone.id,
                "title" to milestone.title,
                "amountMinor" to milestone.amountMinor,
                "dueDate" to milestone.dueDate,
                "status" to milestone.status
            )
        }

        collection.document(projectId)
            .set(
                mapOf(
                    "milestones" to milestoneMaps,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error ->
                onError(error.localizedMessage ?: "Unable to update milestones.")
            }
    }

    fun deleteProject(
        projectId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val collection = projectsCollection()

        if (collection == null) {
            onError("Please sign in before deleting a project.")
            return
        }

        collection.document(projectId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error ->
                onError(error.localizedMessage ?: "Unable to delete the project.")
            }
    }
}

private object FreelanceMilestoneDefaults {
    const val PENDING = "Pending"
}