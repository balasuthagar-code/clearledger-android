package com.example.clearledger.data.fake

import com.example.clearledger.core.common.MonthKey
import com.example.clearledger.domain.model.Expense
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.model.PaymentMethod
import com.example.clearledger.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TEMPORARY (Sprint 1 only): in-memory expenses so the dashboard data flow can be built and
 * demonstrated before Firestore expenses exist. DELETE in Sprint 2 when FirestoreExpenseRepository
 * is written, and change the binding in ExpenseModule.
 */
@Singleton
class FakeExpenseRepository @Inject constructor() : ExpenseRepository {

    private val today = LocalDate.now()
    private val thisMonth = MonthKey.of(today)

    private val data = MutableStateFlow(
        listOf(
            sample("e1", 3_400_000, "rent", ExpenseClassification.COMMITTED, "Landlord"),
            sample("e2", 480_000, "subscriptions", ExpenseClassification.COMMITTED, "Gym"),
            sample("e3", 1_200_000, "food", ExpenseClassification.DISCRETIONARY, "Coffee & dining"),
            sample("e4", 350_000, "transport", ExpenseClassification.DISCRETIONARY, "PickMe")
        )
    )

    override fun observeExpensesForMonth(monthKey: String): Flow<List<Expense>> =
        data.map { list -> list.filter { it.monthKey == monthKey } }

    private fun sample(
        id: String, minor: Long, category: String, c: ExpenseClassification, merchant: String
    ) = Expense(
        id = id, amountMinor = minor, currency = "LKR", exchangeRate = "1", baseAmountMinor = minor,
        categoryId = category, classification = c, paymentMethod = PaymentMethod.CARD,
        merchant = merchant, date = today, monthKey = thisMonth
    )
}
