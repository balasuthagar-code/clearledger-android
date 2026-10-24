package com.example.clearledger.data.repository

import com.example.clearledger.core.common.Currencies
import com.example.clearledger.data.source.CurrencyDocument
import com.example.clearledger.domain.model.BaseCurrencyState
import com.example.clearledger.domain.model.InvalidBaseCurrencyReason

/** Strict stored-code policy: no trim, case folding, coercion or LKR fallback. */
internal object BaseCurrencyStateResolver {
    fun resolve(uid: String, document: CurrencyDocument): BaseCurrencyState {
        val metadata = document.metadata
        if (!document.exists) return BaseCurrencyState.MissingDocument(uid, metadata)
        val raw = document.baseCurrency ?: return BaseCurrencyState.Unconfigured(uid, metadata)
        if (raw !is String) {
            return BaseCurrencyState.Invalid(uid, InvalidBaseCurrencyReason.WRONG_TYPE, metadata)
        }
        if (raw.isBlank()) {
            return BaseCurrencyState.Invalid(uid, InvalidBaseCurrencyReason.BLANK, metadata)
        }
        if (raw !in Currencies.supported) {
            return BaseCurrencyState.Invalid(uid, InvalidBaseCurrencyReason.UNSUPPORTED_CODE, metadata)
        }
        return BaseCurrencyState.Configured(uid, raw, metadata)
    }
}
