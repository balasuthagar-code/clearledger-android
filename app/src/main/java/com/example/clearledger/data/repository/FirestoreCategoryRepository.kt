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
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import com.google.firebase.firestore.Source
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    private val seedMutex = Mutex()

    override suspend fun seedDefaultCategoriesIfEmpty(): Result<Unit> = seedMutex.withLock {
        try {
            val uid = auth.currentUser?.uid ?: error("Please sign in first.")
            val collection = firestore.collection("users").document(uid).collection("categories")
            val existing = collection.limit(1).get(Source.SERVER).await()
            check(auth.currentUser?.uid == uid) { "Account changed. Retry setup." }
            if (existing.isEmpty) {
                firestore.runTransaction { tx ->
                    check(auth.currentUser?.uid == uid) { "Account changed. Retry setup." }
                    val defaults = DefaultCategories.all.map { it to collection.document(it.id) }
                    // Read every fixed ID before any write. Never reset an existing favourite/category.
                    val snapshots = defaults.map { (_, ref) -> tx.get(ref) }
                    if (snapshots.none { it.exists() }) {
                        defaults.forEach { (category, ref) -> tx.set(ref, category.toDto()) }
                    }
                    Unit
                }.await()
            }
            check(auth.currentUser?.uid == uid) { "Account changed. Retry setup." }
            Result.Success(Unit)
        } catch (e: CancellationException) { throw e }
          catch (e: Exception) { Result.Error(Exception("Could not set up your categories. Please retry.", e)) }
    }

    /**
     * The DTO stores the flag as "favourite" (see CategoryDto). Like the other writes, an offline phone
     * queues it: after 2 seconds we report success and the SDK syncs later.
     */
    override suspend fun setFavourite(id: String, favourite: Boolean): Result<Unit> {
        return try {
            withTimeoutOrNull(WRITE_TIMEOUT_MS) {
                categoriesCollection().document(id).update("favourite", favourite).await()
            }
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(Exception("Could not update your favourite categories. Please try again.", e))
        }
    }

    private companion object {
        const val WRITE_TIMEOUT_MS = 2_000L
    }
}
