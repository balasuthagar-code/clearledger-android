package com.example.clearledger.data.dto

/**
 * Reads ONE income-entry document (a Firestore Map) for the dashboard.
 * SRS 3.3 schema: users/{uid}/incomeEntries/{id} with status, baseAmountMinor, monthKey, ...
 *
 * BR-6 / FR-INC-07: explicitly EXPECTED entries are ignored. Older income documents used
 * amountMinor without a status field; those documents represent actual received income and are
 * still supported.
 */
object IncomeEntryReader {

    fun receivedBaseAmount(data: Map<String, Any?>): Long? {
        val status = (data["status"] as? String)?.trim()
        if (status != null && !status.equals("RECEIVED", ignoreCase = true)) return null
        if (status == null && data["amountMinor"] !is Number) return null

        val amount = (
            (data["baseAmountMinor"] as? Number)?.toLong()
                ?: (data["amountMinor"] as? Number)?.toLong()
            ) ?: return null
        return if (amount > 0L) amount else null
    }
}
