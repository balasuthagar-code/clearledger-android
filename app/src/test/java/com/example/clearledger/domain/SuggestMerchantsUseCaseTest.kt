package com.example.clearledger.domain

import com.example.clearledger.domain.model.MerchantSuggestion
import com.example.clearledger.domain.usecase.SuggestMerchantsUseCase
import com.example.clearledger.testutil.testExpense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SuggestMerchantsUseCaseTest {

    private val useCase = SuggestMerchantsUseCase()

    @Test
    fun buildMemory_groupsCaseInsensitively_countsUses_andKeepsTheLatestSpelling() {
        val memory = useCase.buildMemory(
            listOf(
                testExpense("1", merchant = "pickme", categoryId = "transport", date = LocalDate.of(2026, 10, 1)),
                testExpense("2", merchant = "PickMe", categoryId = "transport", date = LocalDate.of(2026, 10, 5)),
                testExpense("3", merchant = " PICKME ", categoryId = "transport", date = LocalDate.of(2026, 10, 3))
            )
        )

        val pickMe = memory.single()
        assertEquals("PickMe", pickMe.name)          // the most recent spelling
        assertEquals(3, pickMe.useCount)
        assertEquals("transport", pickMe.categoryId)
    }

    @Test
    fun buildMemory_ignoresExpensesWithoutAMerchant() {
        val memory = useCase.buildMemory(listOf(testExpense("1", merchant = ""), testExpense("2", merchant = "   ")))

        assertTrue(memory.isEmpty())
    }

    @Test
    fun buildMemory_usesTheMostFrequentCategory() {
        val memory = useCase.buildMemory(
            listOf(
                testExpense("1", merchant = "Keells", categoryId = "food"),
                testExpense("2", merchant = "Keells", categoryId = "food"),
                testExpense("3", merchant = "Keells", categoryId = "other")
            )
        )

        assertEquals("food", memory.single().categoryId)
    }

    @Test
    fun buildMemory_onATie_prefersTheMostRecentCategory() {
        val memory = useCase.buildMemory(
            listOf(
                testExpense("1", merchant = "Cargills", categoryId = "food", date = LocalDate.of(2026, 9, 1)),
                testExpense("2", merchant = "Cargills", categoryId = "other", date = LocalDate.of(2026, 10, 1))
            )
        )

        assertEquals("other", memory.single().categoryId)
    }

    @Test
    fun buildMemory_ordersByUseCount_thenName() {
        val memory = useCase.buildMemory(
            listOf(
                testExpense("1", merchant = "Zed"),
                testExpense("2", merchant = "Alpha"),
                testExpense("3", merchant = "Mid"), testExpense("4", merchant = "Mid")
            )
        )

        assertEquals(listOf("Mid", "Alpha", "Zed"), memory.map { it.name })
    }

    private val memory = listOf(
        MerchantSuggestion("Keells", "food", 9),
        MerchantSuggestion("PickMe", "transport", 8),
        MerchantSuggestion("Starbucks", "food", 7),
        MerchantSuggestion("Spotify", "subscriptions", 6),
        MerchantSuggestion("Uber Eats", "food", 5),
        MerchantSuggestion("Cargills", "food", 4)
    )

    @Test
    fun blankInput_returnsTheMostUsedMerchants_limitedToFive() {
        val result = useCase(memory, "")

        assertEquals(5, result.size)
        assertEquals("Keells", result.first().name)
    }

    @Test
    fun typing_filtersCaseInsensitively_withPrefixMatchesFirst() {
        val result = useCase(memory, "S")

        // Names STARTING with "s" come first; names merely containing it follow.
        assertEquals(listOf("Starbucks", "Spotify"), result.take(2).map { it.name })
        assertTrue(result.map { it.name }.containsAll(listOf("Keells", "Cargills", "Uber Eats")))
    }

    @Test
    fun anExactMatch_isStillSuggested_soItsCategoryCanBeRecalled() {
        val result = useCase(memory, "keells")

        assertEquals(listOf("Keells"), result.map { it.name })
        assertEquals("food", result.single().categoryId)
    }

    @Test
    fun anExactMatch_comesBeforePartialMatches() {
        val result = useCase(
            listOf(
                MerchantSuggestion("Test Cafe Colombo", "food", 9),
                MerchantSuggestion("Test Cafe", "food", 1)
            ),
            "test cafe"
        )

        assertEquals(listOf("Test Cafe", "Test Cafe Colombo"), result.map { it.name })
    }

    @Test
    fun noMatch_returnsNothing() {
        assertTrue(useCase(memory, "zzz").isEmpty())
    }
}
