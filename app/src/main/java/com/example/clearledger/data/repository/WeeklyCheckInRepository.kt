
package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.WeeklyCheckIn
import kotlinx.coroutines.flow.Flow

interface WeeklyCheckInRepository {

    fun getWeeklyCheckIns(): Flow<List<WeeklyCheckIn>>

    suspend fun saveWeeklyCheckIn(
        checkIn: WeeklyCheckIn
    )
}
//thinsanth