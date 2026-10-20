package com.example.clearledger.data

import com.example.clearledger.data.dto.ExpenseMapper
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.model.PaymentMethod
import com.example.clearledger.testutil.testExpense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class ExpenseMapperTest {

    private val day = LocalDate.of(2026, 10, 20)
    private val dayAsDate: Date = Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant())

    private fun validDoc(vararg overrides: Pair<String, Any?>): Map<String, Any?> =
        mapOf<String, Any?>(
            "amountMinor" to 125_050L,
            "currency" to "LKR",
            "exchangeRate" to "1",
            "baseAmountMinor" to 125_050L,
            "categoryId" to "food",
            "classification" to "DISCRETIONARY",
            "paymentMethod" to "CARD",
            "merchant" to "Coffee",
            "note" to "",
            "date" to dayAsDate,
            "monthKey" to "2026-10"
        ) + overrides

    @Test
    fun writeThenRead_roundTrips() {
        val original = testExpense(id = "x", baseMinor = 125_050, merchant = "Coffee", classification = ExpenseClassification.COMMITTED)
        val now = Date(1_700_000_000_000)

        val map = ExpenseMapper.toMap(original, now, isNew = true)
        val back = ExpenseMapper.fromMap("x", map)

        assertEquals(original.copy(createdAtMillis = now.time), back)
    }

    @Test
    fun toMap_newAddsCreatedAt_updateDoesNot() {
        val e = testExpense()
        assertTrue(ExpenseMapper.toMap(e, Date(), isNew = true).containsKey("createdAt"))
        assertFalse(ExpenseMapper.toMap(e, Date(), isNew = false).containsKey("createdAt"))
        assertTrue(ExpenseMapper.toMap(e, Date(), isNew = false).containsKey("updatedAt"))
    }

    @Test
    fun toMap_usesSrsFieldNames() {
        val keys = ExpenseMapper.toMap(testExpense(), Date(), isNew = true).keys
        listOf(
            "amountMinor", "currency", "exchangeRate", "baseAmountMinor", "categoryId", "classification",
            "paymentMethod", "merchant", "note", "date", "monthKey", "createdAt", "updatedAt"
        ).forEach { assertTrue("missing field $it", it in keys) }
    }

    @Test
    fun fromMap_acceptsIsoStringAndEpochMillisDates() {
        assertEquals(day, ExpenseMapper.fromMap("a", validDoc("date" to "2026-10-20"))?.date)
        assertEquals(day, ExpenseMapper.fromMap("a", validDoc("date" to "2026-10-20T10:15:00"))?.date)
        assertEquals(day, ExpenseMapper.fromMap("a", validDoc("date" to dayAsDate.time))?.date)
    }

    @Test
    fun fromMap_numbersMayBeAnyNumericType() {
        val e = ExpenseMapper.fromMap("a", validDoc("amountMinor" to 5000, "baseAmountMinor" to 5000.0))
        assertEquals(5000L, e?.amountMinor)
        assertEquals(5000L, e?.baseAmountMinor)
    }

    @Test
    fun fromMap_missingOrBadMonthKey_isDerivedFromDate() {
        assertEquals("2026-10", ExpenseMapper.fromMap("a", validDoc("monthKey" to null))?.monthKey)
        assertEquals("2026-10", ExpenseMapper.fromMap("a", validDoc("monthKey" to "2026-1"))?.monthKey)
    }

    @Test
    fun fromMap_storedMonthKeyWins() {
        assertEquals("2026-09", ExpenseMapper.fromMap("a", validDoc("monthKey" to "2026-09"))?.monthKey)
    }

    @Test
    fun fromMap_brokenDocuments_returnNull() {
        assertNull(ExpenseMapper.fromMap("a", validDoc("amountMinor" to null)))
        assertNull(ExpenseMapper.fromMap("a", validDoc("date" to null)))
        assertNull(ExpenseMapper.fromMap("a", validDoc("date" to "not a date")))
    }

    @Test
    fun fromMap_unknownEnums_fallBackSafely() {
        val e = ExpenseMapper.fromMap("a", validDoc("classification" to "???", "paymentMethod" to "crypto"))
        assertEquals(ExpenseClassification.DISCRETIONARY, e?.classification)
        assertEquals(PaymentMethod.OTHER, e?.paymentMethod)
    }

    @Test
    fun fromMap_enumsAreCaseAndSpaceTolerant() {
        val e = ExpenseMapper.fromMap("a", validDoc("classification" to "committed", "paymentMethod" to "bank transfer"))
        assertEquals(ExpenseClassification.COMMITTED, e?.classification)
        assertEquals(PaymentMethod.BANK_TRANSFER, e?.paymentMethod)
    }

    @Test
    fun fromMap_missingBaseAmount_isDerived() {
        // base currency: same as the amount
        assertEquals(125_050L, ExpenseMapper.fromMap("a", validDoc("baseAmountMinor" to null))?.baseAmountMinor)
        // foreign currency: amount x rate (BR-2)
        val usd = ExpenseMapper.fromMap(
            "a", validDoc("currency" to "USD", "amountMinor" to 10_000L, "exchangeRate" to "300.50", "baseAmountMinor" to null)
        )
        assertEquals(3_005_000L, usd?.baseAmountMinor)
        // foreign currency with an unusable rate cannot be repaired
        assertNull(ExpenseMapper.fromMap("a", validDoc("currency" to "USD", "exchangeRate" to "abc", "baseAmountMinor" to null)))
    }

    @Test
    fun fromMap_numericExchangeRate_becomesString() {
        assertEquals("300.5", ExpenseMapper.fromMap("a", validDoc("exchangeRate" to 300.5))?.exchangeRate)
    }

    @Test
    fun fromMap_createdAt_isReadAsMillis() {
        val e = ExpenseMapper.fromMap("a", validDoc("createdAt" to Date(42_000)))
        assertNotNull(e)
        assertEquals(42_000L, e?.createdAtMillis)
    }
}
