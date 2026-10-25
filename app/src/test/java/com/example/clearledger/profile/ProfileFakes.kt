package com.example.clearledger.profile

import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant

class FakeProfileAccount(initial: String? = "A") : ProfileAccount {
    val users = MutableStateFlow(initial)
    override val uid get() = users.value
    override val email get() = uid?.let { "$it@example.test" }
    override fun observeUid(): Flow<String?> = users
    override fun logout() { users.value = null }
}
class FakeProfileStore : ProfileStore {
    val records = mutableMapOf<String, Map<String, Any?>>()
    val writes = mutableListOf<Pair<String, Map<String, Any>>>()
    val history = mutableSetOf<Pair<String, String>>()
    val checks = mutableListOf<Pair<String, String>>()
    var readError = false
    var writeError = false
    var markerError = false
    var deniedCollection: String? = null
    var beforeRead: suspend () -> Unit = {}
    var beforeSave: suspend () -> Unit = {}
    private fun merge(uid: String, fields: Map<String, Any>) {
        writes += uid to fields
        records[uid] = records[uid].orEmpty() + fields.mapValues { (_, v) ->
            if (v == ProfileServerValue.TIMESTAMP) Instant.ofEpochSecond(200) else v
        }
    }
    override suspend fun read(uid: String): Map<String, Any?>? {
        beforeRead()
        if (readError) error("Read denied")
        return records[uid]
    }
    override suspend fun save(uid: String, email: String, name: String, currency: String, expectedCurrency: Any?) {
        beforeSave()
        if (writeError) error("Write denied")
        val old = records[uid]
        check(old?.get("baseCurrency") == expectedCurrency) { "Currency changed" }
        merge(uid, ProfileMerge.fields(uid, email, name, currency, old))
    }
    override suspend fun markCategoriesReady(uid: String, expectedCurrency: Any?) {
        if (markerError) error("Marker denied")
        val profile = ProfileContract.decode(uid, records[uid])
        check(profile.complete && profile.storedCurrency == expectedCurrency)
        merge(uid, mapOf("profileCategoriesReady" to true, "updatedAt" to ProfileServerValue.TIMESTAMP))
    }
    override suspend fun hasDocuments(uid: String, collection: String): Boolean {
        checks += uid to collection
        if (collection == deniedCollection) error("History denied")
        return (uid to collection) in history
    }
}
class FakeProfileCategories : CategoryRepository {
    var calls = 0
    var fail = false
    override fun observeCategories(): Flow<List<ExpenseCategory>> = flowOf(emptyList())
    override suspend fun seedDefaultCategoriesIfEmpty(): Result<Unit> {
        calls++
        return if (fail) Result.Error(Exception("Seed denied")) else Result.Success(Unit)
    }
    override suspend fun setFavourite(id: String, favourite: Boolean): Result<Unit> = Result.Success(Unit)
}
fun completeFields(uid: String = "A") = mapOf<String, Any?>("uid" to uid, "email" to "$uid@example.test",
    "displayName" to "Nila", "baseCurrency" to "LKR", "profileCategoriesReady" to false,
    "createdAt" to Instant.ofEpochSecond(100), "updatedAt" to Instant.ofEpochSecond(100))
fun categoriesForSplash(uid: String = "123"): ProfileCategories {
    val store = FakeProfileStore().apply { records[uid] = completeFields(uid) }
    return ProfileCategories(store, FakeProfileAccount(uid), FakeProfileCategories())
}
