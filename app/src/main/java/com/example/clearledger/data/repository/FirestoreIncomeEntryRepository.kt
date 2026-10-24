
package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.IncomeEntry
import com.example.clearledger.domain.model.IncomeSource
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class FirestoreIncomeEntryRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IncomeEntryRepository {

    private fun userId(): String? {
        return auth.currentUser?.uid
    }

    private fun entries() =
        userId()?.let { uid ->
            db.collection("users")
                .document(uid)
                .collection("incomeEntries")
        }

    private fun sources() =
        userId()?.let { uid ->
            db.collection("users")
                .document(uid)
                .collection("incomeSources")
        }

    // Safely reads timestamps stored as numbers, Firestore Timestamps,
    // or numeric strings.
    private fun readLongField(
        doc: DocumentSnapshot,
        field: String
    ): Long {
        return when (val value = doc.get(field)) {
            is Number -> value.toLong()
            is Timestamp -> value.toDate().time
            is Date -> value.time
            is String -> value.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    override fun loadSources(
        callback: (Result<List<IncomeSource>>) -> Unit
    ) {
        val uid = userId()
        val collection = sources()

        if (uid == null || collection == null) {
            callback(Result.failure(Exception("Please sign in first.")))
            return
        }

        collection.get()
            .addOnSuccessListener { snapshot ->
                val result = snapshot.documents
                    .map { doc ->
                        IncomeSource(
                            id = doc.id,
                            userId = doc.getString("userId") ?: uid,
                            name = doc.getString("name") ?: "",
                            type = doc.getString("type") ?: "OTHER",
                            customType = doc.getString("customType") ?: "",
                            defaultCurrency =
                                doc.getString("defaultCurrency") ?: "LKR",
                            isActive = doc.getBoolean("isActive") ?: true,
                            createdAt = readLongField(doc, "createdAt"),
                            updatedAt = readLongField(doc, "updatedAt")
                        )
                    }
                    .filter { it.isActive }

                callback(Result.success(result))
            }
            .addOnFailureListener { error ->
                callback(Result.failure(error))
            }
    }

    override fun loadEntries(
        callback: (Result<List<IncomeEntry>>) -> Unit
    ) {
        val uid = userId()
        val collection = entries()

        if (uid == null || collection == null) {
            callback(Result.failure(Exception("Please sign in first.")))
            return
        }

        collection.get()
            .addOnSuccessListener { snapshot ->
                val result = snapshot.documents
                    .mapNotNull { doc ->
                        val timestamp = doc.getTimestamp("date")

                        val dateString = timestamp
                            ?.toDate()
                            ?.toInstant()
                            ?.atZone(ZoneId.systemDefault())
                            ?.toLocalDate()
                            ?.toString()
                            ?: doc.getString("date")
                            ?: return@mapNotNull null

                        IncomeEntry(
                            id = doc.id,
                            userId = doc.getString("userId") ?: uid,
                            sourceId = doc.getString("sourceId") ?: "",
                            originalAmountMinor =
                                (doc.get("originalAmountMinor") as? Number)
                                    ?.toLong() ?: 0L,
                            originalCurrency =
                                doc.getString("originalCurrency")
                                    ?: doc.getString("currency")
                                    ?: "LKR",
                            exchangeRate =
                                doc.getString("exchangeRate") ?: "1",
                            baseAmountMinor =
                                (doc.get("baseAmountMinor") as? Number)
                                    ?.toLong() ?: 0L,
                            date = dateString,
                            monthKey = doc.getString("monthKey")
                                ?: dateString.take(7),
                            status = doc.getString("status") ?: "EXPECTED",
                            note = doc.getString("note") ?: "",
                            createdAt = readLongField(doc, "createdAt"),
                            updatedAt = readLongField(doc, "updatedAt")
                        )
                    }
                    .sortedByDescending { it.date }

                callback(Result.success(result))
            }
            .addOnFailureListener { error ->
                callback(Result.failure(error))
            }
    }

    override fun saveEntry(
        entry: IncomeEntry,
        callback: (Result<Unit>) -> Unit
    ) {
        val uid = userId()
        val collection = entries()

        if (uid == null || collection == null) {
            callback(Result.failure(Exception("Please sign in first.")))
            return
        }

        try {
            val date = LocalDate.parse(entry.date)
            val instant = date
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()

            val dateTimestamp = Timestamp(Date.from(instant))
            val now = System.currentTimeMillis()

            val data = hashMapOf<String, Any>(
                "userId" to uid,
                "sourceId" to entry.sourceId,
                "originalAmountMinor" to entry.originalAmountMinor,
                "originalCurrency" to entry.originalCurrency,
                "exchangeRate" to entry.exchangeRate,
                "baseAmountMinor" to entry.baseAmountMinor,
                "date" to dateTimestamp,
                "monthKey" to entry.monthKey,
                "status" to entry.status,
                "note" to entry.note.trim(),
                "updatedAt" to now
            )

            if (entry.id.isBlank()) {
                data["createdAt"] = now

                collection.add(data)
                    .addOnSuccessListener {
                        callback(Result.success(Unit))
                    }
                    .addOnFailureListener { error ->
                        callback(Result.failure(error))
                    }
            } else {
                collection.document(entry.id)
                    .update(data)
                    .addOnSuccessListener {
                        callback(Result.success(Unit))
                    }
                    .addOnFailureListener { error ->
                        callback(Result.failure(error))
                    }
            }
        } catch (error: Exception) {
            callback(Result.failure(error))
        }
    }

    override fun deleteEntry(
        entryId: String,
        callback: (Result<Unit>) -> Unit
    ) {
        val collection = entries()

        if (userId() == null || collection == null) {
            callback(Result.failure(Exception("Please sign in first.")))
            return
        }

        collection.document(entryId)
            .delete()
            .addOnSuccessListener {
                callback(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                callback(Result.failure(error))
            }
    }
}
