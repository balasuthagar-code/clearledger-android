
package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.IncomeSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class IncomeSourceSummary(
    val source: IncomeSource,
    val total: Double = 0.0,
    val entryCount: Int = 0,
    val amountEntryCount: Int = 0
)

class FirestoreIncomeSourceRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun uid(): String? = auth.currentUser?.uid

    private fun sources() =
        uid()?.let {
            db.collection("users").document(it)
                .collection("incomeSources")
        }

    private fun entries() =
        uid()?.let {
            db.collection("users").document(it)
                .collection("incomeEntries")
        }

    fun loadSources(
        callback: (kotlin.Result<List<IncomeSourceSummary>>) -> Unit
    ) {
        val userId = uid()
        val sourceCollection = sources()
        val entryCollection = entries()

        if (userId == null || sourceCollection == null || entryCollection == null) {
            callback(Result.failure(Exception("Please sign in first.")))
            return
        }

        sourceCollection.get()
            .addOnSuccessListener { sourceSnapshot ->
                val sourceList = sourceSnapshot.documents.map { doc ->
                    IncomeSource(
                        id = doc.id,
                        userId = doc.getString("userId") ?: userId,
                        name = doc.getString("name") ?: "",
                        type = doc.getString("type") ?: "OTHER",
                        customType = doc.getString("customType") ?: "",
                        defaultCurrency =
                            doc.getString("defaultCurrency") ?: "LKR",
                        isActive = doc.getBoolean("isActive") ?: true,
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        updatedAt = doc.getLong("updatedAt") ?: 0L
                    )
                }

                entryCollection.get()
                    .addOnSuccessListener { entrySnapshot ->
                        val summaries = sourceList.map { source ->
                            val linkedEntries = entrySnapshot.documents.filter {
                                it.getString("sourceId") == source.id
                            }

                            val matchingAmounts = linkedEntries.mapNotNull { entry ->
                                val currency =
                                    entry.getString("originalCurrency")
                                        ?: entry.getString("currency")
                                        ?: source.defaultCurrency

                                val amountMinor =
                                    (entry.get("originalAmountMinor") as? Number)?.toLong()
                                        ?: (entry.get("amountMinor") as? Number)?.toLong()
                                val amount = amountMinor?.toDouble()?.div(100.0)

                                if (
                                    currency == source.defaultCurrency &&
                                    amount != null
                                ) {
                                    amount
                                } else {
                                    null
                                }
                            }

                            IncomeSourceSummary(
                                source = source,
                                total = matchingAmounts.sum(),
                                entryCount = linkedEntries.size,
                                amountEntryCount = matchingAmounts.size
                            )
                        }

                        callback(Result.success(summaries))
                    }
                    .addOnFailureListener {
                        callback(Result.failure(it))
                    }
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    fun saveSource(
        source: IncomeSource,
        callback: (kotlin.Result<Unit>) -> Unit
    ) {
        val userId = uid()
        val collection = sources()

        if (userId == null || collection == null) {
            callback(Result.failure(Exception("Please sign in first.")))
            return
        }

        val now = System.currentTimeMillis()
        val data = hashMapOf<String, Any>(
            "userId" to userId,
            "name" to source.name.trim(),
            "type" to source.type,
            "customType" to source.customType.trim(),
            "defaultCurrency" to source.defaultCurrency,
            "isActive" to source.isActive,
            "updatedAt" to now
        )

        if (source.id.isBlank()) {
            data["createdAt"] = now

            collection.add(data)
                .addOnSuccessListener {
                    callback(Result.success(Unit))
                }
                .addOnFailureListener {
                    callback(Result.failure(it))
                }
        } else {
            collection.document(source.id).update(data)
                .addOnSuccessListener {
                    callback(Result.success(Unit))
                }
                .addOnFailureListener {
                    callback(Result.failure(it))
                }
        }
    }

    fun deleteSource(
        sourceId: String,
        callback: (kotlin.Result<Unit>) -> Unit
    ) {
        val collection = sources()
        val entryCollection = entries()

        if (uid() == null || collection == null || entryCollection == null) {
            callback(Result.failure(Exception("Please sign in first.")))
            return
        }

        entryCollection.whereEqualTo("sourceId", sourceId).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    callback(
                        Result.failure(
                            Exception(
                                "This source has income entries. " +
                                        "Deactivate it instead to keep your history."
                            )
                        )
                    )
                    return@addOnSuccessListener
                }

                collection.document(sourceId).delete()
                    .addOnSuccessListener {
                        callback(Result.success(Unit))
                    }
                    .addOnFailureListener {
                        callback(Result.failure(it))
                    }
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    fun setActive(
        sourceId: String,
        active: Boolean,
        callback: (kotlin.Result<Unit>) -> Unit
    ) {
        val collection = sources()

        if (uid() == null || collection == null) {
            callback(Result.failure(Exception("Please sign in first.")))
            return
        }

        collection.document(sourceId).update(
            mapOf(
                "isActive" to active,
                "updatedAt" to System.currentTimeMillis()
            )
        )
            .addOnSuccessListener {
                callback(Result.success(Unit))
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }
}
// thsianth