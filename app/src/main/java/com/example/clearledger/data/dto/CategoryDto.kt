package com.example.clearledger.data.dto

import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.model.ExpenseClassification

/**
 * Firestore document shape for users/{uid}/categories/{id}. Default values let Firestore create it
 * with a no-argument constructor.
 *
 * GOTCHA (good viva point): Firestore uses Java-bean naming. A Kotlin property called
 * `isFavourite` is saved under the key "favourite" (the "is" prefix is stripped), which silently
 * breaks reads. The DTO therefore uses `favourite` / `builtIn`, and the mapper converts them to
 * the domain names `isFavourite` / `isBuiltIn`.
 */
data class CategoryDto(
    val name: String = "",
    val defaultClassification: String = ExpenseClassification.DISCRETIONARY.name,
    val favourite: Boolean = false,
    val builtIn: Boolean = true,
    val sortOrder: Int = 0
)

fun CategoryDto.toDomain(id: String): ExpenseCategory = ExpenseCategory(
    id = id,
    name = name,
    defaultClassification = runCatching { ExpenseClassification.valueOf(defaultClassification) }
        .getOrDefault(ExpenseClassification.DISCRETIONARY),
    isFavourite = favourite,
    isBuiltIn = builtIn,
    sortOrder = sortOrder
)

fun ExpenseCategory.toDto(): CategoryDto = CategoryDto(
    name = name,
    defaultClassification = defaultClassification.name,
    favourite = isFavourite,
    builtIn = isBuiltIn,
    sortOrder = sortOrder
)
