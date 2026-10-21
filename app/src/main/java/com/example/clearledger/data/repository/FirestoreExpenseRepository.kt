package com.example.clearledger.data.repository

import com.example.clearledger.core.common.Result
import com.example.clearledger.data.dto.ExpenseMapper
import com.example.clearledger.domain.model.Expense
import com.example.clearledger.domain.repository.ExpenseRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
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
 * Expenses live at users/{uid}/expenses/{id}, so the security rules ("only the owner") cover them.
 *
 * Query note: the list asks only "monthKey == X" (one equality filter). That needs NO composite
 * index; sorting by date happens in GroupExpensesByDateUseCase.
 */
class FirestoreExpenseRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ExpenseRepository {

    private fun expensesCollection(): CollectionReference {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("User is not signed in")
        return firestore.collection("users").document(uid).collection("expenses")
    }

    override fun observeExpensesForMonth(monthKey: String): Flow<List<Expense>> = callbackFlow {
        val registration = expensesCollection()
            .whereEqualTo("monthKey", monthKey)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)                      // ViewModel's .catch turns this into an error state
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    doc.data?.let { ExpenseMapper.fromMap(doc.id, it.withDates()) }
                }
                trySend(expenses)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Newest expenses first. Ordering by one field needs NO composite index (Firestore creates single-field
     * indexes automatically). The limit keeps the read cost small.
     */
    override fun observeRecentExpenses(limit: Int): Flow<List<Expense>> = callbackFlow {
        val registration = expensesCollection()
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    doc.data?.let { ExpenseMapper.fromMap(doc.id, it.withDates()) }
                }
                trySend(expenses)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getExpense(id: String): Result<Expense?> = safeCall(
        "Could not load this expense. Check your connection and try again."
    ) {
        val ref = expensesCollection().document(id)
        // The expense was already shown in the list, so try the local cache first: instant and
        // works offline. Fall back to the server if it is not cached.
        val snapshot = try {
            ref.get(Source.CACHE).await()
        } catch (e: FirebaseFirestoreException) {
            ref.get().await()
        }
        snapshot.data?.let { ExpenseMapper.fromMap(snapshot.id, it.withDates()) }
    }

    override suspend fun addExpense(expense: Expense): Result<String> = safeCall(
        "Could not save the expense. Please try again."
    ) {
        val document = expensesCollection().document()          // id is generated locally
        document.set(ExpenseMapper.toMap(expense, Date(), isNew = true)).awaitOrQueue()
        document.id
    }

    override suspend fun updateExpense(expense: Expense): Result<Unit> = safeCall(
        "Could not update the expense. Please try again."
    ) {
        expensesCollection().document(expense.id)
            .update(ExpenseMapper.toMap(expense, Date(), isNew = false))   // fails if it was deleted meanwhile
            .awaitOrQueue()
    }

    override suspend fun upsertExpense(expense: Expense): Result<Unit> = safeCall(
        "Could not save the expense. Please try again."
    ) {
        expensesCollection().document(expense.id)
            .set(ExpenseMapper.toMap(expense, Date(), isNew = true))
            .awaitOrQueue()
    }

    override suspend fun deleteExpense(id: String): Result<Unit> = safeCall(
        "Could not delete the expense. Please try again."
    ) {
        expensesCollection().document(id).delete().awaitOrQueue()
    }

    // ---- helpers ----

    /** Firestore returns Timestamp objects; the pure mapper understands java.util.Date. */
    private fun Map<String, Any?>.withDates(): Map<String, Any?> =
        mapValues { (_, value) -> if (value is Timestamp) value.toDate() else value }

    private suspend fun <T> safeCall(errorMessage: String, block: suspend () -> T): Result<T> = try {
        Result.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(Exception(errorMessage, e))
    }

    /**
     * A Firestore write Task only completes after the SERVER confirms it, which never happens while
     * the phone is offline. The SDK has already stored the write locally (the snapshot listener
     * shows it immediately) and will sync it later, so after WRITE_TIMEOUT_MS we report success
     * instead of freezing the Save button. Real errors (e.g. permission denied) arrive quickly and
     * are thrown. This is the "offline writes are queued" behaviour from the SRS (FR-SYS-02).
     */
    private suspend fun Task<Void>.awaitOrQueue() {
        withTimeoutOrNull(WRITE_TIMEOUT_MS) { await() }
    }

    private companion object {
        const val WRITE_TIMEOUT_MS = 2_000L
    }
}
