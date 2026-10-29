package com.example.clearledger.ui.income

import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain

import com.example.clearledger.currency.testRateRecorder

import com.example.clearledger.data.repository.IncomeEntryRepository
import com.example.clearledger.domain.model.IncomeEntry
import com.example.clearledger.domain.model.IncomeSource
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class IncomeEntriesViewModelTest {
    @org.junit.Before fun mainDispatcher() { kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.test.UnconfinedTestDispatcher()) }
    @org.junit.After fun resetDispatcher() { kotlinx.coroutines.Dispatchers.resetMain() }

    private class FakeRepository : IncomeEntryRepository {
        val saved = mutableListOf<IncomeEntry>()

        override fun loadSources(
            callback: (Result<List<IncomeSource>>) -> Unit
        ) {
            callback(Result.success(emptyList()))
        }

        override fun loadEntries(
            callback: (Result<List<IncomeEntry>>) -> Unit
        ) {
            callback(Result.success(saved.toList()))
        }

        override fun saveEntry(
            entry: IncomeEntry,
            callback: (Result<Unit>) -> Unit
        ) {
            saved += entry
            callback(Result.success(Unit))
        }

        override fun updateStatus(
            entryId: String,
            status: String,
            callback: (Result<Unit>) -> Unit
        ) {
            val index = saved.indexOfFirst { it.id == entryId }
            if (index >= 0) {
                saved[index] = saved[index].copy(status = status)
            }
            callback(Result.success(Unit))
        }

        override fun deleteEntry(
            entryId: String,
            callback: (Result<Unit>) -> Unit
        ) {
            callback(Result.success(Unit))
        }
    }

    private fun save(
        viewModel: IncomeEntriesViewModel,
        amount: String = "100.00",
        currency: String = "USD",
        rate: String = "300.50",
        status: String = "RECEIVED",
        existing: IncomeEntry? = null
    ) {
        viewModel.saveEntry(
            existing = existing,
            sourceId = "test-source",
            amountText = amount,
            currency = currency,
            rateText = rate,
            date = "2026-10-24",
            note = "Currency test",
            status = status
        )
    }

    @Test
    fun foreignIncomeSavesConvertedAmountAndStatus() {
        for (status in listOf("EXPECTED", "RECEIVED")) {
            val repository = FakeRepository()
            val viewModel = IncomeEntriesViewModel(repository, testRateRecorder())

            save(viewModel, status = status)

            val entry = repository.saved.single()
            assertEquals(10_000L, entry.originalAmountMinor)
            assertEquals(3_005_000L, entry.baseAmountMinor)
            assertEquals("USD", entry.originalCurrency)
            assertEquals("300.5", entry.exchangeRate)
            assertEquals(status, entry.status)
            assertEquals("2026-10", entry.monthKey)
            assertNull(viewModel.state.error)
            assertFalse(viewModel.state.isSaving)
        }
    }

    @Test
    fun lkrIncomeUsesIdentityRate() {
        val repository = FakeRepository()
        val viewModel = IncomeEntriesViewModel(repository, testRateRecorder())

        save(viewModel, currency = "LKR", rate = "")

        val entry = repository.saved.single()
        assertEquals("1", entry.exchangeRate)
        assertEquals(10_000L, entry.baseAmountMinor)
    }

    @Test
    fun excessPrecisionDoesNotReachRepository() {
        val repository = FakeRepository()
        val viewModel = IncomeEntriesViewModel(repository, testRateRecorder())

        save(viewModel, amount = "1.234")

        assertTrue(repository.saved.isEmpty())
        assertNotNull(viewModel.state.error)
        assertFalse(viewModel.state.isSaving)
    }

    @Test
    fun invalidRatesDoNotReachRepository() {
        for (rate in listOf("", "0", "-1", "abc")) {
            val repository = FakeRepository()
            val viewModel = IncomeEntriesViewModel(repository, testRateRecorder())

            save(viewModel, rate = rate)

            assertTrue(repository.saved.isEmpty())
            assertNotNull(viewModel.state.error)
        }
    }

    @Test
    fun overflowAndRoundedZeroDoNotReachRepository() {
        for (rate in listOf("1000000000000000000", "0.000000001")) {
            val repository = FakeRepository()
            val viewModel = IncomeEntriesViewModel(repository, testRateRecorder())

            save(viewModel, rate = rate)

            assertTrue(repository.saved.isEmpty())
            assertNotNull(viewModel.state.error)
        }
    }

    @Test
    fun editingPreservesRecordIdentityAndCreationTime() {
        val repository = FakeRepository()
        val viewModel = IncomeEntriesViewModel(repository, testRateRecorder())
        val existing = IncomeEntry(
            id = "existing-entry",
            createdAt = 1234L
        )

        save(viewModel, existing = existing)

        val entry = repository.saved.single()
        assertEquals("existing-entry", entry.id)
        assertEquals(1234L, entry.createdAt)
        assertEquals(3_005_000L, entry.baseAmountMinor)
    }

    @Test
    fun statusCanBeUpdatedWithoutSavingTheWholeEntry() {
        val repository = FakeRepository()
        val viewModel = IncomeEntriesViewModel(repository, testRateRecorder())
        val entry = IncomeEntry(id = "entry-1", status = "EXPECTED")
        repository.saved += entry

        viewModel.updateStatus(entry.id, "RECEIVED")

        assertEquals("RECEIVED", repository.saved.single().status)
        assertEquals("Income status updated.", viewModel.state.message)
    }
}