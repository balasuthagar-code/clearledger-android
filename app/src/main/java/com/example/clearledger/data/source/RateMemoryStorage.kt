package com.example.clearledger.data.source

/** One key's string is one consistent record. Implementations serialize access and throw on failure. */
interface RateMemoryStorage {
    suspend fun read(key: String): String?
    /** Return only after durable storage acknowledgement; no fire-and-forget writes. */
    suspend fun write(key: String, record: String)
}

/** Wrong stored preference type, with no sensitive value in the exception. */
class CorruptRateMemoryStorageException : IllegalStateException("Invalid rate-memory storage type")
