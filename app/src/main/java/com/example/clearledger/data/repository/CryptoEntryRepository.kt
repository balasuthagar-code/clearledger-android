package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.CryptoEntry
import com.example.clearledger.domain.model.CryptoEntryType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import javax.inject.Inject
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.math.BigDecimal

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
                        quantity = document.getString("quantity") ?: "0",
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
            "quantity" to entry.quantity,
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

    suspend fun addIntegratedEntry(
            entry: CryptoEntry,
            currentHoldingQuantity: BigDecimal
    ) {
            val user = auth.currentUser ?: error("Please sign in before saving an entry.")
            val entries = entriesCollection() ?: error("Please sign in before saving an entry.")
            val cryptoRef = entries.document()
            val holdingRef = db.collection("users").document(user.uid)
                .collection("cryptoHoldings").document(entry.assetName.trim().lowercase())
            val expenseRef = db.collection("users").document(user.uid)
                .collection("expenses").document("crypto_${cryptoRef.id}")
            val incomeRef = db.collection("users").document(user.uid)
                .collection("incomeEntries").document("crypto_${cryptoRef.id}")
            val sourceRef = db.collection("users").document(user.uid)
                .collection("incomeSources").document("crypto")
            val quantity = entry.quantity.toBigDecimalOrNull() ?: error("Crypto quantity must be valid.")

            db.runTransaction { transaction ->
                val storedQuantity = transaction.get(holdingRef).getString("quantity")
                    ?.toBigDecimalOrNull() ?: currentHoldingQuantity
                val nextQuantity = when (entry.type) {
                    CryptoEntryType.BUY -> {
                        if (storedQuantity > BigDecimal.ZERO) throw IllegalStateException("You already own this cryptocurrency.")
                        storedQuantity + quantity
                    }
                    CryptoEntryType.SELL -> {
                        if (storedQuantity <= BigDecimal.ZERO || quantity > storedQuantity) {
                            throw IllegalStateException("You do not own enough of this cryptocurrency.")
                        }
                        storedQuantity - quantity
                    }
                    else -> throw IllegalArgumentException("Only Buy and Sell are supported.")
                }
                val now = System.currentTimeMillis()
                transaction.set(cryptoRef, mapOf(
                    "id" to cryptoRef.id, "userId" to user.uid, "assetName" to entry.assetName.trim(),
                    "date" to entry.date, "monthKey" to entry.monthKey, "type" to entry.type,
                    "quantity" to entry.quantity, "originalCurrency" to entry.originalCurrency,
                    "originalValueMinor" to entry.originalValueMinor, "exchangeRate" to entry.exchangeRate,
                    "baseValueMinor" to entry.baseValueMinor, "costBasisBaseMinor" to entry.costBasisBaseMinor,
                    "note" to entry.note.trim(), "createdAt" to now, "updatedAt" to now
                ))
                transaction.set(holdingRef, mapOf(
                    "assetName" to entry.assetName.trim(),
                    "quantity" to nextQuantity.stripTrailingZeros().toPlainString(), "updatedAt" to now
                ))
                val date = Timestamp(Date.from(LocalDate.parse(entry.date)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant()))
                if (entry.type == CryptoEntryType.BUY) {
                    transaction.set(expenseRef, mapOf(
                        "amountMinor" to entry.baseValueMinor, "currency" to "LKR",
                        "exchangeRate" to "1", "baseAmountMinor" to entry.baseValueMinor,
                        "categoryId" to "crypto_purchase", "classification" to "DISCRETIONARY",
                        "paymentMethod" to "OTHER", "merchant" to entry.assetName,
                        "note" to "Cryptocurrency purchase", "date" to date, "monthKey" to entry.monthKey,
                        "cryptoTransactionId" to cryptoRef.id, "createdAt" to date, "updatedAt" to date
                    ))
                } else {
                    transaction.set(sourceRef, mapOf(
                        "userId" to user.uid, "name" to "Cryptocurrency", "type" to "CRYPTO",
                        "defaultCurrency" to "LKR", "isActive" to true, "updatedAt" to now, "createdAt" to now
                    ))
                    transaction.set(incomeRef, mapOf(
                        "userId" to user.uid, "sourceId" to "crypto",
                        "originalAmountMinor" to entry.baseValueMinor, "originalCurrency" to "LKR",
                        "exchangeRate" to "1", "baseAmountMinor" to entry.baseValueMinor, "date" to date,
                        "monthKey" to entry.monthKey, "status" to "RECEIVED",
                        "note" to "Cryptocurrency sale: ${entry.assetName}",
                        "cryptoTransactionId" to cryptoRef.id, "createdAt" to now, "updatedAt" to now
                    ))
                }
                null
            }.await()
    }

    suspend fun deleteTransaction(entry: CryptoEntry, currentEntries: List<CryptoEntry>) {
        val user = auth.currentUser ?: error("Please sign in before deleting an entry.")
        val entries = entriesCollection() ?: error("Please sign in before deleting an entry.")
        val cryptoRef = entries.document(entry.id)
        val holdingRef = db.collection("users").document(user.uid)
            .collection("cryptoHoldings").document(entry.assetName.trim().lowercase())
        val financialCollection = db.collection("users").document(user.uid)
            .collection(if (entry.type == CryptoEntryType.BUY) "expenses" else "incomeEntries")
        val financialRef = financialCollection.document("crypto_${entry.id}")
        val remainingQuantity = currentEntries
            .filter { it.id != entry.id && it.assetName.equals(entry.assetName, ignoreCase = true) }
            .fold(BigDecimal.ZERO) { total, remaining ->
                val quantity = remaining.quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO
                if (remaining.type == CryptoEntryType.BUY) total + quantity else total - quantity
            }

        db.runTransaction { transaction ->
            if (!transaction.get(cryptoRef).exists()) {
                throw IllegalStateException("This cryptocurrency transaction was already deleted.")
            }
            transaction.delete(cryptoRef)
            transaction.delete(financialRef)
            if (remainingQuantity > BigDecimal.ZERO) {
                transaction.set(holdingRef, mapOf(
                    "assetName" to entry.assetName.trim(),
                    "quantity" to remainingQuantity.stripTrailingZeros().toPlainString(),
                    "updatedAt" to System.currentTimeMillis()
                ))
            } else {
                transaction.delete(holdingRef)
            }
            null
        }.await()
    }

    suspend fun deleteHolding(assetName: String, currentEntries: List<CryptoEntry>) {
        val user = auth.currentUser ?: error("Please sign in before deleting a holding.")
        val entries = entriesCollection() ?: error("Please sign in before deleting a holding.")
        val assetEntries = currentEntries.filter {
            it.assetName.equals(assetName, ignoreCase = true)
        }
        val holdingRef = db.collection("users").document(user.uid)
            .collection("cryptoHoldings").document(assetName.trim().lowercase())

        db.runTransaction { transaction ->
            val refs = assetEntries.map { entries.document(it.id) }
            refs.forEach { ref ->
                if (!transaction.get(ref).exists()) {
                    throw IllegalStateException("This cryptocurrency holding was already deleted.")
                }
            }
            assetEntries.forEach { entry ->
                transaction.delete(entries.document(entry.id))
                val collection = if (entry.type == CryptoEntryType.BUY) {
                    "expenses"
                } else {
                    "incomeEntries"
                }
                transaction.delete(
                    db.collection("users").document(user.uid)
                        .collection(collection).document("crypto_${entry.id}")
                )
            }
            transaction.delete(holdingRef)
            null
        }.await()
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