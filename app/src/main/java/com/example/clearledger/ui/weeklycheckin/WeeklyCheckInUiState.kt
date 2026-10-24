
package com.example.clearledger.ui.weeklycheckin

import com.example.clearledger.domain.model.WeeklyCheckIn

data class WeeklyCheckInUiState(
    val checkIns: List<WeeklyCheckIn> = emptyList(),
    val expectedBalanceMinor: Long = 0L,
    val reportedBalanceText: String = "",
    val note: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val reportedBalanceMinor: Long?
        get() = reportedBalanceText
            .trim()
            .toBigDecimalOrNull()
            ?.takeIf { it >= java.math.BigDecimal.ZERO }
            ?.multiply(java.math.BigDecimal("100"))
            ?.toLong()
}
//thsianth