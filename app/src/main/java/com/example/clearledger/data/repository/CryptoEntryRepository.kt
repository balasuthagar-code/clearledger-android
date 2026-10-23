package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.CryptoEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import javax.inject.Inject

class CryptoEntryRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    private fun entriesCollection() =
        auth.currentUser?.uid?.let { uid ->
            db.collection("users")
                .document(uid)
                .collection("cryptoEntries")
        }

    fun listenToEntries(
        onSuccess: (List<CryptoEntry>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val collection = entriesCollection()

        if (collection == null) {
            onError("Please sign in to view cryptocurrency entries.")
            return null
        }

        return collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.localizedMessage ?: "Unable to load crypto entries.")
                return@addSnapshotListener
            }

            val entries = snapshot?.documents.orEmpty().mapNotNull { document ->
                try {
                    CryptoEntry(
                        id = document.id,
                        userId = document.getString("userId") ?: "",
                        assetName = document.getString("assetName") ?: "",
                        date = document.getString("date") ?: "",
                        monthKey = document.getString("monthKey") ?: "",
                        type = document.getString("type") ?: "Buy",
                        originalCurrency =
                            document.getString("originalCurrency") ?: "LKR",
                        originalValueMinor =
                            document.getLong("originalValueMinor") ?: 0L,
                        exchangeRate = document.getString("exchangeRate") ?: "1",
                        baseValueMinor =
                            document.getLong("baseValueMinor") ?: 0L,
                        costBasisBaseMinor =
                            document.getLong("costBasisBaseMinor") ?: 0L,
                        note = document.getString("note") ?: "",
                        createdAt = document.getLong("createdAt") ?: 0L,
                        updatedAt = document.getLong("updatedAt") ?: 0L
                    )
                } catch (_: Exception) {
                    null
                }
            }.sortedByDescending { it.date }

            onSuccess(entries)
        }
    }

    fun addEntry(
        entry: CryptoEntry,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser
        val collection = entriesCollection()

        if (user == null || collection == null) {
            onError("Please sign in before saving an entry.")
            return
        }

        val now = System.currentTimeMillis()
        val document = collection.document()

        val data = hashMapOf(
            "id" to document.id,
            "userId" to user.uid,
            "assetName" to entry.assetName.trim(),
            "date" to entry.date,
            "monthKey" to entry.monthKey,
            "type" to entry.type,
            "originalCurrency" to entry.originalCurrency,
            "originalValueMinor" to entry.originalValueMinor,
            "exchangeRate" to entry.exchangeRate,
            "baseValueMinor" to entry.baseValueMinor,
            "costBasisBaseMinor" to entry.costBasisBaseMinor,
            "note" to entry.note.trim(),
            "createdAt" to now,
            "updatedAt" to now
        )

        document.set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error ->
                onError(error.localizedMessage ?: "Unable to save the entry.")
            }
    }

    fun deleteEntry(
        entryId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val collection = entriesCollection()

        if (collection == null) {
            onError("Please sign in before deleting an entry.")
            return
        }

        collection.document(entryId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error ->
                onError(error.localizedMessage ?: "Unable to delete the entry.")
            }
    }
}
// thisanth