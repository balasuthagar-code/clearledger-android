package com.example.clearledger.domain

import com.example.clearledger.domain.model.DefaultCategories
import com.example.clearledger.domain.model.ExpenseClassification
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultCategoriesTest {

    private fun classificationOf(id: String) =
        DefaultCategories.all.first { it.id == id }.defaultClassification

    @Test
    fun thereAreSevenCategoriesWithUniqueIds() {
        assertEquals(7, DefaultCategories.all.size)
        assertEquals(7, DefaultCategories.all.map { it.id }.toSet().size)
    }

    @Test
    fun rentAndSubscriptionsAreCommitted() {
        assertEquals(ExpenseClassification.COMMITTED, classificationOf("rent"))
        assertEquals(ExpenseClassification.COMMITTED, classificationOf("subscriptions"))
    }

    @Test
    fun otherCategoriesAreDiscretionary() {
        listOf("food", "transport", "entertainment", "health_fitness", "other").forEach {
            assertEquals(ExpenseClassification.DISCRETIONARY, classificationOf(it))
        }
    }
}
