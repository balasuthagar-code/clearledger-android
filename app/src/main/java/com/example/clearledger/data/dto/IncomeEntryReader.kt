package com.example.clearledger.data.dto

/**
 * Reads ONE income-entry document (a Firestore Map) for the dashboard.
 * SRS 3.3 schema: users/{uid}/incomeEntries/{id} with status, baseAmountMinor, monthKey, ...
 *
 * BR-6 / FR-INC-07: only RECEIVED entries count as income. EXPECTED entries are ignored.
 * Returns the base-currency amount in minor units, or null when the entry must not be counted
 * (expected, or the amount is missing/invalid).
 */
object IncomeEntryReader {

    fun receivedBaseAmount(data: Map<String, Any?>): Long? {
        val status = (data["status"] as? String)?.trim()
        if (status == null || !status.equals("RECEIVED", ignoreCase = true)) return null
        val amount = (data["baseAmountMinor"] as? Number)?.toLong() ?: return null
        return if (amount > 0L) amount else null
    }
}
