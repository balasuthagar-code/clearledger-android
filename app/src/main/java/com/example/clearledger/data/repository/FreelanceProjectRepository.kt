package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.FreelanceMilestone
import com.example.clearledger.domain.model.FreelanceProject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

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
                        clientContact = document.getString("clientContact") ?: "",
                        description = document.getString("description") ?: "",
                        startDate = document.getString("startDate") ?: "",
                        endDate = document.getString("endDate") ?: "",
                        workDescription = document.getString("workDescription") ?: "",
                        status = document.getString("status") ?: "PLANNED",
                        paymentStatus = document.getString("paymentStatus") ?: "UNPAID",
                        totalPaidBaseMinor = document.getLong("totalPaidBaseMinor") ?: 0L,
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
        clientContact: String = "",
        description: String = "",
        startDate: String = "",
        endDate: String = "",
        workDescription: String = "",
        status: String = "PLANNED",
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
            "clientContact" to clientContact.trim(),
            "description" to description.trim(),
            "startDate" to startDate,
            "endDate" to endDate,
            "workDescription" to workDescription.trim(),
            "status" to status,
            "paymentStatus" to "UNPAID",
            "totalPaidBaseMinor" to 0L,
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

    suspend fun recordPayment(
                project: FreelanceProject,
                amountMinor: Long,
                paymentDate: String,
                note: String
            ) {
                val user = auth.currentUser ?: error("Please sign in before recording payment.")
                val projects = projectsCollection() ?: error("Please sign in before recording payment.")
                val projectRef = projects.document(project.id)
                val paymentId = "${project.id}_${paymentDate}_${amountMinor}"
                val paymentRef = projectRef.collection("payments").document(paymentId)
                val incomeRef = db.collection("users").document(user.uid)
                    .collection("incomeEntries").document("freelance_$paymentId")
                val sourceRef = db.collection("users").document(user.uid)
                    .collection("incomeSources").document("freelance")
                val date = Timestamp(Date.from(LocalDate.parse(paymentDate)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant()))

                db.runTransaction { transaction ->
                    if (transaction.get(paymentRef).exists() || transaction.get(incomeRef).exists()) {
                        throw IllegalStateException("This payment has already been recorded.")
                    }
                    val current = transaction.get(projectRef)
                    val paid = (current.getLong("totalPaidBaseMinor") ?: 0L) + amountMinor
                    val agreed = current.getLong("baseAmountMinor") ?: project.baseAmountMinor
                    val status = when {
                        paid >= agreed -> "PAID"
                        paid > 0L -> "PARTIALLY_PAID"
                        else -> "UNPAID"
                    }
                    transaction.set(paymentRef, mapOf(
                        "id" to paymentId, "projectId" to project.id, "userId" to user.uid,
                        "amountMinor" to amountMinor, "date" to date, "note" to note.trim(),
                        "createdAt" to System.currentTimeMillis()
                    ))
                    transaction.set(projectRef, mapOf(
                        "totalPaidBaseMinor" to paid, "paymentStatus" to status,
                        "updatedAt" to System.currentTimeMillis()
                    ), SetOptions.merge())
                    transaction.set(sourceRef, mapOf(
                        "userId" to user.uid, "name" to "Freelance Projects", "type" to "FREELANCE",
                        "defaultCurrency" to "LKR", "isActive" to true,
                        "createdAt" to System.currentTimeMillis(), "updatedAt" to System.currentTimeMillis()
                    ), SetOptions.merge())
                    transaction.set(incomeRef, mapOf(
                        "userId" to user.uid, "sourceId" to "freelance",
                        "originalAmountMinor" to amountMinor, "originalCurrency" to "LKR",
                        "exchangeRate" to "1", "baseAmountMinor" to amountMinor, "date" to date,
                        "monthKey" to paymentDate.take(7), "status" to "RECEIVED",
                        "note" to "Freelance payment: ${project.name}. ${note.trim()}",
                        "freelanceProjectId" to project.id, "freelancePaymentId" to paymentId,
                        "createdAt" to System.currentTimeMillis(), "updatedAt" to System.currentTimeMillis()
                    ))
                    null
                }.await()
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

    fun updateProjectStatus(
        projectId: String,
        status: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val collection = projectsCollection()
        if (collection == null) {
            onError("Please sign in before updating the project.")
            return
        }
        if (status !in com.example.clearledger.domain.model.FreelanceProjectStatus.values) {
            onError("Please select a valid project status.")
            return
        }

        collection.document(projectId)
            .update(
                mapOf(
                    "status" to status,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error ->
                onError(error.localizedMessage ?: "Unable to update project status.")
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