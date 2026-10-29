package com.example.clearledger.data.repository

import com.example.clearledger.data.dto.IncomeEntryReader
import com.example.clearledger.domain.repository.IncomeSummarySource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Date
import javax.inject.Inject

/**
 * Real implementation of IncomeSummarySource: live base-currency amounts of RECEIVED income
 * entries of one month, read from users/{uid}/incomeEntries.
 *
 * Income documents exist in two compatible schema versions. Read the collection and filter locally
 * so legacy records that have incomeDate/date instead of monthKey are also reflected on the home
 * dashboard.
 */
class FirestoreIncomeSummarySource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : IncomeSummarySource {

    override fun observeReceivedBaseAmounts(monthKey: String): Flow<List<Long>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("User is not signed in")
        val registration = firestore.collection("users").document(uid).collection("incomeEntries")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val amounts = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    if (!belongsToMonth(data, monthKey)) return@mapNotNull null
                    IncomeEntryReader.receivedBaseAmount(data)
                }
                trySend(amounts)
            }
        awaitClose { registration.remove() }
    }

    private fun belongsToMonth(data: Map<String, Any?>, monthKey: String): Boolean {
        val storedMonth = data["monthKey"] as? String
        if (storedMonth != null) return storedMonth == monthKey

        val dateValue = data["incomeDate"] ?: data["date"] ?: return false
        val date = when (dateValue) {
            is Timestamp -> dateValue.toDate()
            is Date -> dateValue
            is String -> runCatching { java.time.LocalDate.parse(dateValue) }.getOrNull()
                ?.toString()
                ?: return false
            else -> null
        }

        return when (date) {
            is Date -> {
                val localDate = date.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                localDate.toString().startsWith("$monthKey-")
            }
            is String -> date.startsWith("$monthKey-")
            else -> false
        }
    }
}
