package com.example.clearledger.profile

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

internal class OfflineProfileUnavailableException(cause: Throwable? = null) :
    IllegalStateException("Your profile could not be verified offline. Connect and retry.", cause)

internal enum class ProfileServerFailure {
    UNAVAILABLE, DEADLINE_EXCEEDED, PERMISSION_DENIED, UNAUTHENTICATED, OTHER
}

internal fun canUseCachedProfile(failure: ProfileServerFailure): Boolean =
    failure == ProfileServerFailure.UNAVAILABLE || failure == ProfileServerFailure.DEADLINE_EXCEEDED

internal fun profileServerFailure(error: Exception): ProfileServerFailure =
    if (error is FirebaseFirestoreException) {
        when (error.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE -> ProfileServerFailure.UNAVAILABLE
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> ProfileServerFailure.DEADLINE_EXCEEDED
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> ProfileServerFailure.PERMISSION_DENIED
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> ProfileServerFailure.UNAUTHENTICATED
            else -> ProfileServerFailure.OTHER
        }
    } else ProfileServerFailure.OTHER

@Suppress("UNUSED_PARAMETER")
internal fun cachedProfileIsReady(
    uid: String,
    fields: Map<String, Any?>?,
    hasPendingWrites: Boolean
): Boolean {
    if (fields == null) return false
    val profile = ProfileContract.decode(uid, fields)
    return profile.complete && profile.categoriesReady
}

internal suspend fun <T> readProfileServerFirst(
    serverRead: suspend () -> T,
    cacheRead: suspend () -> T?,
    cacheValid: (T) -> Boolean,
    serverFailure: (Exception) -> ProfileServerFailure
): T {
    try {
        return serverRead()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (!canUseCachedProfile(serverFailure(e))) throw e
        val cached = cacheRead()
        if (cached == null || !cacheValid(cached)) throw OfflineProfileUnavailableException(e)
        return cached
    }
}

class FirebaseProfileAccount @Inject constructor(private val auth: FirebaseAuth) : ProfileAccount {
    override val uid get() = auth.currentUser?.uid
    override val email get() = auth.currentUser?.email
    override fun logout() = auth.signOut()
    override fun observeUid(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()
}

class FirestoreProfileStore @Inject constructor(private val firestore: FirebaseFirestore,
    private val account: ProfileAccount) : ProfileStore {
    private fun normalize(fields: Map<String, Any>?): Map<String, Any?>? = fields?.mapValues { (_, v) ->
        if (v is Timestamp) Instant.ofEpochSecond(v.seconds, v.nanoseconds.toLong()) else v
    }
    private fun serverFields(fields: Map<String, Any>) = fields.mapValues { (_, v) ->
        if (v == ProfileServerValue.TIMESTAMP) FieldValue.serverTimestamp() else v
    }
    override suspend fun read(uid: String): Map<String, Any?>? {
        account.requireOwner(uid)
        val document = firestore.collection("users").document(uid)
        val snapshot = readProfileServerFirst(
            serverRead = { document.get(Source.SERVER).await() },
            cacheRead = {
                account.requireOwner(uid)
                try {
                    document.get(Source.CACHE).await()
                } catch (cacheError: FirebaseFirestoreException) {
                    if (cacheError.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                        cacheError.code == FirebaseFirestoreException.Code.NOT_FOUND
                    ) null else throw cacheError
                }
            },
            cacheValid = { cached ->
                cached.exists() && cachedProfileIsReady(
                    uid,
                    normalize(cached.data),
                    cached.metadata.hasPendingWrites()
                )
            },
            serverFailure = ::profileServerFailure
        )
        account.requireOwner(uid)
        return normalize(snapshot.data)
    }
    override suspend fun save(uid: String, email: String, name: String, currency: String, expectedCurrency: Any?) {
        account.requireOwner(uid)
        val ref = firestore.collection("users").document(uid)
        firestore.runTransaction { tx ->
            account.requireOwner(uid)
            val old = normalize(tx.get(ref).data)
            check(old?.get("baseCurrency") == expectedCurrency) { "Profile currency changed. Reload before saving." }
            tx.set(ref, serverFields(ProfileMerge.fields(uid, email, name, currency, old)), SetOptions.merge())
            Unit
        }.await()
        account.requireOwner(uid)
    }
    override suspend fun markCategoriesReady(uid: String, expectedCurrency: Any?) {
        account.requireOwner(uid)
        val ref = firestore.collection("users").document(uid)
        firestore.runTransaction { tx ->
            account.requireOwner(uid)
            val profile = ProfileContract.decode(uid, normalize(tx.get(ref).data))
            check(profile.complete && profile.storedCurrency == expectedCurrency) { "Profile changed. Reload and retry setup." }
            tx.set(ref, mapOf("profileCategoriesReady" to true, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
            Unit
        }.await()
        account.requireOwner(uid)
    }
    override suspend fun hasDocuments(uid: String, collection: String): Boolean {
        require(collection in CurrencyEligibilityPolicy.COLLECTIONS)
        account.requireOwner(uid)
        val docs = firestore.collection("users").document(uid).collection(collection).limit(1).get(Source.SERVER).await()
        account.requireOwner(uid)
        return !docs.isEmpty
    }
}
