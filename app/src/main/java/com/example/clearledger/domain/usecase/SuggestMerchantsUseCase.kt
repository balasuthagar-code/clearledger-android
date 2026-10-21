package com.example.clearledger.domain.usecase

import com.example.clearledger.domain.model.Expense
import com.example.clearledger.domain.model.MerchantSuggestion
import javax.inject.Inject

/**
 * FR-EXP-08 merchant memory. Two pure steps, so they are easy to unit test:
 *  1. [buildMemory]: from the user's recent expenses, one entry per merchant (case-insensitive) with
 *     the category it is usually filed under and how often it was used.
 *  2. [invoke]: filter that memory by what the user has typed so far.
 */
class SuggestMerchantsUseCase @Inject constructor() {

    fun buildMemory(expenses: List<Expense>): List<MerchantSuggestion> =
        expenses
            .filter { it.merchant.isNotBlank() }
            .groupBy { it.merchant.trim().lowercase() }
            .map { (_, group) ->
                val latest = group.maxWithOrNull(compareBy<Expense> { it.date }.thenBy { it.createdAtMillis })!!
                MerchantSuggestion(
                    name = latest.merchant.trim(),            // keep the spelling the user used last
                    categoryId = usualCategory(group, latest),
                    useCount = group.size
                )
            }
            .sortedWith(compareByDescending<MerchantSuggestion> { it.useCount }.thenBy { it.name.lowercase() })

    /** Blank [typed] -> the most used merchants; otherwise names starting with it, then names containing it. */
    operator fun invoke(memory: List<MerchantSuggestion>, typed: String, limit: Int = DEFAULT_LIMIT): List<MerchantSuggestion> {
        val query = typed.trim().lowercase()
        if (query.isEmpty()) return memory.take(limit)
        return memory
            .filter { it.name.lowercase().contains(query) && it.name.lowercase() != query }
            .sortedBy { if (it.name.lowercase().startsWith(query)) 0 else 1 }   // stable: keeps most-used first
            .take(limit)
    }

    /** The most frequent category of the merchant; on a tie the one used most recently wins. */
    private fun usualCategory(group: List<Expense>, latest: Expense): String {
        val counts = group.filter { it.categoryId.isNotBlank() }.groupingBy { it.categoryId }.eachCount()
        if (counts.isEmpty()) return ""
        val top = counts.values.maxOrNull() ?: return ""
        return if ((counts[latest.categoryId] ?: 0) == top) latest.categoryId
        else counts.entries.first { it.value == top }.key
    }

    private companion object {
        const val DEFAULT_LIMIT = 5
    }
}
