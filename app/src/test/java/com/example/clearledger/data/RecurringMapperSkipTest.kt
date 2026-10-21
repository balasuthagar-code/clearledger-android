package com.example.clearledger.data

import com.example.clearledger.data.dto.RecurringMapper
import com.example.clearledger.testutil.testRecurring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class RecurringMapperSkipTest {

    private fun docWith(vararg extra: Pair<String, Any?>): Map<String, Any?> {
        val base: Map<String, Any?> = RecurringMapper.toMap(testRecurring("r1"), Date(), isNew = true)
        return base + extra
    }

    @Test
    fun skippedMonthKey_isRead() {
        val item = RecurringMapper.fromMap("r1", docWith("skippedMonthKey" to "2026-10"))

        assertEquals("2026-10", item?.skippedMonthKey)
    }

    @Test
    fun withoutTheField_orWhenBlank_nothingIsSkipped() {
        assertNull(RecurringMapper.fromMap("r1", docWith())?.skippedMonthKey)
        assertNull(RecurringMapper.fromMap("r1", docWith("skippedMonthKey" to ""))?.skippedMonthKey)
    }

    @Test
    fun toMap_neverWritesTheSkip_soEditingTheFormCannotResetIt() {
        val skipped = testRecurring("r1", skippedMonthKey = "2026-10")

        assertFalse(RecurringMapper.toMap(skipped, Date(), isNew = false).containsKey("skippedMonthKey"))
    }
}
