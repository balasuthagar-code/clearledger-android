package com.example.clearledger.data

import com.example.clearledger.data.dto.CategoryDto
import com.example.clearledger.data.dto.toDomain
import com.example.clearledger.data.dto.toDto
import com.example.clearledger.domain.model.DefaultCategories
import com.example.clearledger.domain.model.ExpenseClassification
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryDtoTest {

    @Test
    fun roundTrip_keepsEveryField() {
        DefaultCategories.all.forEach { original ->
            assertEquals(original, original.toDto().toDomain(original.id))
        }
    }

    @Test
    fun unknownClassificationFallsBackToDiscretionary() {
        val dto = CategoryDto(name = "Broken", defaultClassification = "SOMETHING_ELSE")
        assertEquals(ExpenseClassification.DISCRETIONARY, dto.toDomain("x").defaultClassification)
    }
}
