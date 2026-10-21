package com.example.clearledger.navigation

/** Routes of the recurring feature (own file, so merging never conflicts with Member 1's Routes.kt). */
object RecurringRoutes {
    const val ARG_RECURRING_ID = "recurringId"
    const val LIST = "recurring"
    const val FORM = "recurring_form?$ARG_RECURRING_ID={$ARG_RECURRING_ID}"

    fun add(): String = "recurring_form"
    fun edit(recurringId: String): String = "recurring_form?$ARG_RECURRING_ID=$recurringId"
}
