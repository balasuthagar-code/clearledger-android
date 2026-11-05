package com.example.clearledger.data.dto

import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.model.ExpenseClassification


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
