package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.IncomeEntry
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

class FirestoreIncomeEntryRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : IncomeEntryRepository {

    private fun collection() =
        firestore.collection("users")
            .document(auth.currentUser?.uid
                ?: throw IllegalStateException("Please log in first"))
            .collection("incomeEntries")

    override fun getIncomeEntries(): Flow<List<IncomeEntry>> =
        callbackFlow {
            val listener = collection()
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val entries = snapshot?.documents?.mapNotNull { document ->
                        val timestamp = document.getTimestamp("date")
                        val date = timestamp?.toDate()?.toInstant()
                            ?.atZone(ZoneId.of("Asia/Colombo"))
                            ?.toLocalDate()?.toString() ?: return@mapNotNull null

                        IncomeEntry(
                            id = document.id,
                            sourceId = document.getString("sourceId") ?: "",
                            sourceName = document.getString("sourceName") ?: "",
                            originalAmountMinor = document.getLong("originalAmountMinor") ?: 0L,
                            currency = document.getString("currency") ?: "LKR",
                            exchangeRate = document.getString("exchangeRate") ?: "1",
                            baseAmountMinor = document.getLong("baseAmountMinor") ?: 0L,
                            date = date,
                            monthKey = document.getString("monthKey") ?: date.substring(0, 7),
                            note = document.getString("note") ?: "",
                            status = document.getString("status") ?: "EXPECTED"
                        )
                    } ?: emptyList()

                    trySend(entries)
                }

            awaitClose { listener.remove() }
        }

    override suspend fun addIncomeEntry(entry: IncomeEntry) {
        val date = LocalDate.parse(entry.date)
            .atStartOfDay(ZoneId.of("Asia/Colombo"))
            .toInstant()

        val data = mapOf(
            "sourceId" to entry.sourceId,
            "sourceName" to entry.sourceName,
            "originalAmountMinor" to entry.originalAmountMinor,
            "currency" to entry.currency,
            "exchangeRate" to entry.exchangeRate,
            "baseAmountMinor" to entry.baseAmountMinor,
            "date" to Timestamp(Date.from(date)),
            "monthKey" to entry.monthKey,
            "note" to entry.note,
            "status" to entry.status,
            "createdAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )

        collection().add(data).await()
    }

    override suspend fun updateIncomeEntry(entry: IncomeEntry) {
        require(entry.id.isNotBlank()) { "Income entry ID is missing" }

        val date = LocalDate.parse(entry.date)
            .atStartOfDay(ZoneId.of("Asia/Colombo"))
            .toInstant()

        val data = mapOf(
            "sourceId" to entry.sourceId,
            "sourceName" to entry.sourceName,
            "originalAmountMinor" to entry.originalAmountMinor,
            "currency" to entry.currency,
            "exchangeRate" to entry.exchangeRate,
            "baseAmountMinor" to entry.baseAmountMinor,
            "date" to Timestamp(Date.from(date)),
            "monthKey" to entry.monthKey,
            "note" to entry.note,
            "status" to entry.status,
            "updatedAt" to Timestamp.now()
        )

        collection().document(entry.id).update(data).await()
    }

    override suspend fun deleteIncomeEntry(entryId: String) {
        require(entryId.isNotBlank()) { "Income entry ID is missing" }
        collection().document(entryId).delete().await()
    }
}
