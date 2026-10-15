package com.example.clearledger.data.repository

import com.example.clearledger.core.common.Result
import com.example.clearledger.data.dto.CategoryDto
import com.example.clearledger.data.dto.toDomain
import com.example.clearledger.data.dto.toDto
import com.example.clearledger.domain.model.DefaultCategories
import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.repository.CategoryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** All data lives under users/{uid}/..., so the security rules can say "only the owner may touch it". */
class FirestoreCategoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CategoryRepository {

    private fun categoriesCollection(): CollectionReference {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("User is not signed in")
        return firestore.collection("users").document(uid).collection("categories")
    }

    override fun observeCategories(): Flow<List<ExpenseCategory>> = callbackFlow {
        val registration = categoriesCollection()
            .orderBy("sortOrder")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)               // the ViewModel's .catch turns this into an error state
                    return@addSnapshotListener
                }
                val categories = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    doc.toObject(CategoryDto::class.java)?.toDomain(doc.id)
                }
                trySend(categories)
            }
        awaitClose { registration.remove() }    // stop listening when nobody collects any more
    }

    override suspend fun seedDefaultCategoriesIfEmpty(): Result<Unit> {
        return try {
            val collection = categoriesCollection()
            val existing = collection.limit(1).get().await()
            if (existing.isEmpty) {
                val batch = firestore.batch()
                DefaultCategories.all.forEach { batch.set(collection.document(it.id), it.toDto()) }
                batch.commit().await()
            }
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(Exception("Could not set up your categories. Please try again.", e))
        }
    }
}
