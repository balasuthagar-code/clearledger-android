
package com.example.clearledger.core.common

import com.example.clearledger.domain.model.IncomeSource
import com.example.clearledger.domain.model.IncomeSourceTypes
import com.example.clearledger.domain.model.SupportedCurrencies

object IncomeSourceValidator {

    fun validate(source: IncomeSource): String? {
        if (source.name.isBlank()) {
            return "Please enter a source name."
        }

        if (source.type !in IncomeSourceTypes.values) {
            return "Please select a valid source type."
        }

        if (source.type == "OTHER" && source.customType.isBlank()) {
            return "Please specify the other income source type."
        }

        if (source.defaultCurrency !in SupportedCurrencies.values) {
            return "Please select a valid currency."
        }

        return null
    }
}
