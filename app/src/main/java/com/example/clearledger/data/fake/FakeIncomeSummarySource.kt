package com.example.clearledger.data.fake

import com.example.clearledger.domain.repository.IncomeSummarySource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * TEMPORARY: pretends Kavindu received a LKR 130,000.00 salary every month.
 * Member 2 replaces this by implementing IncomeSummarySource on top of the real income repository
 * and changing the binding in ExpenseModule (a one-line change).
 */
class FakeIncomeSummarySource @Inject constructor() : IncomeSummarySource {
    override fun observeReceivedBaseAmounts(monthKey: String): Flow<List<Long>> =
        flowOf(listOf(13_000_000L))
}
