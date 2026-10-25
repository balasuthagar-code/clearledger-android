package com.example.clearledger.currency

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Conservative compatibility checks, NOT a distributed currency lock. No migrations. */
class FirestoreCurrencySetupStore @Inject constructor(private val auth: FirebaseAuth,
    private val db: FirebaseFirestore) : CurrencySetupStore {
    private fun check(uid: String) { check(uid.isNotBlank() && auth.currentUser?.uid == uid) { "Account changed. Please retry." } }
    override suspend fun initializeLkr(uid: String) {
        check(uid)
        val email = auth.currentUser?.email?.takeIf { it.isNotBlank() }
        val user = db.collection("users").document(uid)
        for (path in HISTORY_PATHS) {
            val records = user.collection(path).limit(1).get(Source.SERVER).await()
            check(uid)
            check(records.isEmpty) { "Existing financial history needs review before a base currency can be set." }
        }
        db.runTransaction { tx ->
            check(uid)
            val current = tx.get(user)
            val stored = current.get("baseCurrency")
            check(stored == null || stored == "LKR") { "The stored currency changed. Please retry." }
            val updates = mutableMapOf<String, Any>("baseCurrency" to "LKR", "updatedAt" to FieldValue.serverTimestamp())
            if (!current.contains("createdAt")) updates["createdAt"] = FieldValue.serverTimestamp()
            if (!current.contains("uid")) updates["uid"] = uid
            if (!current.contains("email") && email != null) updates["email"] = email
            // Preserve existing uid/email, name and every unrelated field.
            tx.set(user, updates, SetOptions.merge())
        }.await()
        check(uid)
    }
    override suspend fun verifyHistory(uid: String) {
        check(uid)
        val user = db.collection("users").document(uid)
        // Existing undocumented base identity is not inferred from today's profile.
        for (path in HISTORY_PATHS.filter { it != "incomeSources" }) {
            val records = user.collection(path).get(Source.SERVER).await()
            check(uid)
            for (record in records.documents) {
                val code = if (path == "savingsGoals") record.get("currency") else record.get("baseCurrency")
                check(HistoricalBasePolicy.accepts(code)) { "Existing financial records have an unknown or different base currency and need review. Contact support." }
            }
        }
    }
    companion object {
        val HISTORY_PATHS = listOf("expenses", "recurring", "incomeEntries", "incomeSources",
            "freelanceProjects", "cryptoEntries", "savingsGoals", "checkins")
    }
}
