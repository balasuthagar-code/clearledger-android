package com.example.clearledger.profile

import kotlinx.coroutines.flow.Flow

interface ProfileAccount {
    val uid: String?
    val email: String?
    fun observeUid(): Flow<String?>
    fun logout()
}
internal fun ProfileAccount.requireOwner(uid: String) {
    check(uid.isNotBlank() && this.uid == uid) { "Your account changed. Please sign in again." }
}
interface ProfileStore {
    suspend fun read(uid: String): Map<String, Any?>?
    /** Must compare the observed base in a transaction, then merge without replacing other fields. */
    suspend fun save(uid: String, email: String, name: String, currency: String, expectedCurrency: Any?)
    suspend fun markCategoriesReady(uid: String, expectedCurrency: Any?)
    suspend fun hasDocuments(uid: String, collection: String): Boolean
}
