package com.example.clearledger.data.repository

import com.example.clearledger.data.dto.IncomeEntryReader
import com.example.clearledger.domain.repository.IncomeSummarySource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * Real implementation of IncomeSummarySource: live base-currency amounts of RECEIVED income
 * entries of one month, read from users/{uid}/incomeEntries.
 *
 * It filters on monthKey only (one equality filter = no composite index) and keeps the
 * RECEIVED check in IncomeEntryReader. VERIFY the field names against your income documents in the
 * Firebase console before switching the binding in ExpenseModule (see the guide).
 */
class FirestoreIncomeSummarySource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : IncomeSummarySource {

    override fun observeReceivedBaseAmounts(monthKey: String): Flow<List<Long>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("User is not signed in")
        val registration = firestore.collection("users").document(uid).collection("incomeEntries")
            .whereEqualTo("monthKey", monthKey)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val amounts = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    doc.data?.let { IncomeEntryReader.receivedBaseAmount(it) }
                }
                trySend(amounts)
            }
        awaitClose { registration.remove() }
    }
}
