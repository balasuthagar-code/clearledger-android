package com.example.clearledger.domain.repository

import kotlinx.coroutines.flow.Flow


interface IncomeSummarySource {
    fun observeReceivedBaseAmounts(monthKey: String): Flow<List<Long>>
}
