package com.example.clearledger.data.source

import android.content.Context
import android.content.SharedPreferences
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** App-private, single-process store. Hilt supplies one instance; no preferences opened on construction. */
class SharedPreferencesRateMemoryStorage(
    context: Context,
    private val io: CoroutineDispatcher
) : RateMemoryStorage {
    private val appContext = context.applicationContext
    private val lock = Mutex()
    private var preferences: SharedPreferences? = null // Accessed only under lock on io.
    private val uncertainKeys = mutableSetOf<String>()

    private fun preferences(): SharedPreferences = preferences ?: appContext.getSharedPreferences(
        FILE_NAME, Context.MODE_PRIVATE
    ).also { preferences = it }

    override suspend fun read(key: String): String? = withContext(io) {
        lock.withLock {
            // commit() can update Android's in-memory map even if its disk write fails.
            // Never advertise that uncertain same-process value as a persisted prefill.
            if (key in uncertainKeys) throw IOException("Unconfirmed rate-memory storage")
            try { preferences().getString(key, null) }
            catch (_: ClassCastException) { throw CorruptRateMemoryStorageException() }
        }
    }

    override suspend fun write(key: String, record: String): Unit = withContext(io) {
        lock.withLock {
            uncertainKeys += key
            if (!preferences().edit().putString(key, record).commit()) {
                throw IOException("Rate-memory persistence was not confirmed")
            }
            uncertainKeys -= key
        }
    }

    companion object { const val FILE_NAME = "clearledger_rate_memory_v1" }
}
