package com.example.clearledger.testutil

import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.RecurringFrequency
import com.example.clearledger.domain.model.RecurringItem
import com.example.clearledger.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** A real, in-memory RecurringRepository for unit tests, with a switch to simulate failures. */
open class InMemoryRecurringRepository(initial: List<RecurringItem> = emptyList()) : RecurringRepository {

    val data = MutableStateFlow(initial)
    val added = mutableListOf<RecurringItem>()
    val updated = mutableListOf<RecurringItem>()
    val deletedIds = mutableListOf<String>()
    var failWrites = false
    private var nextId = 1

    override fun observeRecurring(): Flow<List<RecurringItem>> = data

    override suspend fun getRecurring(id: String): Result<RecurringItem?> =
        Result.Success(data.value.firstOrNull { it.id == id })

    override suspend fun addRecurring(item: RecurringItem): Result<String> {
        if (failWrites) return Result.Error(Exception("Could not save the item. Please try again."))
        val saved = item.copy(id = "r${nextId++}")
        added += saved
        data.update { it + saved }
        return Result.Success(saved.id)
    }

    override suspend fun updateRecurring(item: RecurringItem): Result<Unit> {
        if (failWrites) return Result.Error(Exception("Could not update the item. Please try again."))
        updated += item
        data.update { list -> list.map { if (it.id == item.id) item else it } }
        return Result.Success(Unit)
    }

    override suspend fun setActive(id: String, active: Boolean): Result<Unit> {
        if (failWrites) return Result.Error(Exception("Could not change the item. Please try again."))
        data.update { list -> list.map { if (it.id == id) it.copy(isActive = active) else it } }
        return Result.Success(Unit)
    }

    override suspend fun deleteRecurring(id: String): Result<Unit> {
        if (failWrites) return Result.Error(Exception("Could not delete the item. Please try again."))
        deletedIds += id
        data.update { list -> list.filterNot { it.id == id } }
        return Result.Success(Unit)
    }
}

/** Builds a recurring item with sensible defaults; override only what a test cares about. */
fun testRecurring(
    id: String = "r1",
    name: String = "Rent",
    baseMinor: Long = 3_400_000,
    frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    isActive: Boolean = true,
    categoryId: String = "rent",
    dueDay: Int = 1,
    dueMonth: Int = 1,
    currency: String = "LKR",
    amountMinor: Long = baseMinor
) = RecurringItem(
    id = id, name = name, amountMinor = amountMinor, currency = currency,
    exchangeRate = if (currency == "LKR") "1" else "300", baseAmountMinor = baseMinor,
    categoryId = categoryId, frequency = frequency, dueDay = dueDay, dueMonth = dueMonth, isActive = isActive
)
