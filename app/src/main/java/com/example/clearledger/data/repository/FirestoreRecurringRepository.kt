package com.example.clearledger.data.repository

import com.example.clearledger.core.common.Result
import com.example.clearledger.data.dto.RecurringMapper
import com.example.clearledger.domain.model.RecurringItem
import com.example.clearledger.domain.repository.RecurringRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import javax.inject.Inject

/**
 * Recurring commitments live at users/{uid}/recurring/{id}. The list loads the whole (small)
 * collection and sorts by name on the device, so no index is needed.
 * Offline behaviour of writes is explained in FirestoreExpenseRepository (same approach).
 */
class FirestoreRecurringRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : RecurringRepository {

    private fun collection(): CollectionReference {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("User is not signed in")
        return firestore.collection("users").document(uid).collection("recurring")
    }

    override fun observeRecurring(): Flow<List<RecurringItem>> = callbackFlow {
        val registration = collection().addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val items = snapshot?.documents.orEmpty()
                .mapNotNull { doc -> doc.data?.let { RecurringMapper.fromMap(doc.id, it.withDates()) } }
                .sortedBy { it.name.lowercase() }
            trySend(items)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getRecurring(id: String): Result<RecurringItem?> = safeCall(
        "Could not load this item. Check your connection and try again."
    ) {
        val ref = collection().document(id)
        val snapshot = try {
            ref.get(Source.CACHE).await()
        } catch (e: FirebaseFirestoreException) {
            ref.get().await()
        }
        snapshot.data?.let { RecurringMapper.fromMap(snapshot.id, it.withDates()) }
    }

    override suspend fun addRecurring(item: RecurringItem): Result<String> = safeCall(
        "Could not save the item. Please try again."
    ) {
        val document = collection().document()
        document.set(RecurringMapper.toMap(item, Date(), isNew = true)).awaitOrQueue()
        document.id
    }

    override suspend fun updateRecurring(item: RecurringItem): Result<Unit> = safeCall(
        "Could not update the item. Please try again."
    ) {
        collection().document(item.id).update(RecurringMapper.toMap(item, Date(), isNew = false)).awaitOrQueue()
    }

    override suspend fun setActive(id: String, active: Boolean): Result<Unit> = safeCall(
        "Could not change the item. Please try again."
    ) {
        collection().document(id).update(mapOf("isActive" to active, "updatedAt" to Date())).awaitOrQueue()
    }

    override suspend fun deleteRecurring(id: String): Result<Unit> = safeCall(
        "Could not delete the item. Please try again."
    ) {
        collection().document(id).delete().awaitOrQueue()
    }

    // ---- helpers ----

    private fun Map<String, Any?>.withDates(): Map<String, Any?> =
        mapValues { (_, value) -> if (value is Timestamp) value.toDate() else value }

    private suspend fun <T> safeCall(errorMessage: String, block: suspend () -> T): Result<T> = try {
        Result.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(Exception(errorMessage, e))
    }

    private suspend fun Task<Void>.awaitOrQueue() {
        withTimeoutOrNull(WRITE_TIMEOUT_MS) { await() }
    }

    private companion object {
        const val WRITE_TIMEOUT_MS = 2_000L
    }
}
