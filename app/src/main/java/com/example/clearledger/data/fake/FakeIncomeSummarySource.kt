package com.example.clearledger.data.fake

import com.example.clearledger.domain.repository.IncomeSummarySource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject


class FakeIncomeSummarySource @Inject constructor() : IncomeSummarySource {
    override fun observeReceivedBaseAmounts(monthKey: String): Flow<List<Long>> =
        flowOf(listOf(13_000_000L))
}
