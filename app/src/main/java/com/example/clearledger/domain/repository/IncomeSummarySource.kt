package com.example.clearledger.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * The ONLY thing the dashboard needs from Member 2's income module: the base-currency amounts
 * (minor units) of income entries whose status is RECEIVED in the given month.
 *
 * Member 2 implements this on top of their IncomeRepository and changes one line in ExpenseModule.
 * Until then a fake is bound (see data/fake) so Member 3 is never blocked.
 */
interface IncomeSummarySource {
    fun observeReceivedBaseAmounts(monthKey: String): Flow<List<Long>>
}
