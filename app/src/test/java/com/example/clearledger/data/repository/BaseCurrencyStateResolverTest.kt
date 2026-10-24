package com.example.clearledger.data.repository

import com.example.clearledger.core.common.Currencies
import com.example.clearledger.data.source.CurrencyDocument
import com.example.clearledger.domain.model.BaseCurrencyState
import com.example.clearledger.domain.model.CurrencySnapshotMetadata
import com.example.clearledger.domain.model.InvalidBaseCurrencyReason
import org.junit.Assert.assertEquals
import org.junit.Test

class BaseCurrencyStateResolverTest {
    private val metadata = CurrencySnapshotMetadata(isFromCache = false, hasPendingWrites = false)
    private fun resolve(raw: Any?, exists: Boolean = true) =
        BaseCurrencyStateResolver.resolve("user-a", CurrencyDocument(exists, raw, metadata))

    @Test fun supportsEveryCatalogueCodeWithoutLkrDefault() {
        for (currency in Currencies.supported) {
            assertEquals(BaseCurrencyState.Configured("user-a", currency, metadata), resolve(currency))
        }
    }

    @Test fun missingDocumentIsDifferentFromMissingOrNullField() {
        assertEquals(BaseCurrencyState.MissingDocument("user-a", metadata), resolve(null, exists = false))
        // The SDK adapter represents an absent field and an explicit null as null.
        assertEquals(BaseCurrencyState.Unconfigured("user-a", metadata), resolve(null))
    }

    @Test fun rejectsWrongTypesWithoutStringCoercion() {
        for (raw in listOf(1L, 1.0, true, listOf("LKR"), mapOf("code" to "LKR"))) {
            assertEquals(BaseCurrencyState.Invalid("user-a", InvalidBaseCurrencyReason.WRONG_TYPE, metadata), resolve(raw))
        }
    }

    @Test fun blankAndUnsupportedCodesAreNotConfigured() {
        for (raw in listOf("", " ", "\t\n")) {
            assertEquals(BaseCurrencyState.Invalid("user-a", InvalidBaseCurrencyReason.BLANK, metadata), resolve(raw))
        }
        for (raw in listOf("JPY", "lkr", "usd", " LKR", "LKR ")) {
            assertEquals(BaseCurrencyState.Invalid("user-a", InvalidBaseCurrencyReason.UNSUPPORTED_CODE, metadata), resolve(raw))
        }
    }

    @Test fun metadataSurvivesEverySnapshotState() {
        for (flags in listOf(
            CurrencySnapshotMetadata(true, false), CurrencySnapshotMetadata(false, true),
            CurrencySnapshotMetadata(true, true), CurrencySnapshotMetadata(false, false)
        )) {
            assertEquals(BaseCurrencyState.Configured("a", "USD", flags),
                BaseCurrencyStateResolver.resolve("a", CurrencyDocument(true, "USD", flags)))
            assertEquals(BaseCurrencyState.Unconfigured("a", flags),
                BaseCurrencyStateResolver.resolve("a", CurrencyDocument(true, null, flags)))
            assertEquals(BaseCurrencyState.MissingDocument("a", flags),
                BaseCurrencyStateResolver.resolve("a", CurrencyDocument(false, null, flags)))
            assertEquals(BaseCurrencyState.Invalid("a", InvalidBaseCurrencyReason.WRONG_TYPE, flags),
                BaseCurrencyStateResolver.resolve("a", CurrencyDocument(true, 7L, flags)))
        }
    }
}
