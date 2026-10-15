package com.example.clearledger.domain.model

/**
 * The assignment says Kavindu "has no concept of what proportion of his income is discretionary
 * versus committed" (section 3.3).
 * COMMITTED      = hard to avoid (rent, subscriptions, gym auto-debit)
 * DISCRETIONARY  = he could choose to spend less (coffee, UberEats, entertainment)
 */
enum class ExpenseClassification { COMMITTED, DISCRETIONARY }
