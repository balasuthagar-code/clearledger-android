package com.example.clearledger.profile

import com.example.clearledger.domain.model.User
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class ProfileContractTest {
    @Test fun completeRequiresEveryStoredField() {
        assertTrue(ProfileContract.decode("A", completeFields()).complete)
        for (field in completeFields().keys) assertFalse(field,
            ProfileContract.decode("A", completeFields() - field).complete)
        assertFalse(ProfileContract.decode("A", null).complete)
    }
    @Test fun defaultsCannotFillMissingBaseCurrency() {
        assertEquals("LKR", User().baseCurrency)
        assertEquals("", ProfileContract.decode("A", completeFields() - "baseCurrency").user.baseCurrency)
    }
    @Test fun invalidValuesAndUntrimmedNamesAreIncomplete() {
        for (code in listOf("", "JPY", "lkr")) assertFalse(ProfileContract.decode("A", completeFields() + ("baseCurrency" to code)).complete)
        for (name in listOf("", "  ", " Nila ")) assertFalse(ProfileContract.decode("A", completeFields() + ("displayName" to name)).complete)
        assertFalse(ProfileContract.decode("A", completeFields() + ("createdAt" to 123L)).complete)
        assertFalse(ProfileContract.decode("A", completeFields() + ("profileCategoriesReady" to "true")).complete)
    }
    @Test fun unicodeNamesAreSupported() {
        assertNull(ProfileContract.nameError(" நிலா අමා 🌿 "))
        assertEquals("நிலா", ProfileMerge.fields("A", "a@b.test", " நிலா ", "LKR", null)["displayName"])
    }
    @Test fun eightyCodePointsAccepted() { assertNull(ProfileContract.nameError("🌿".repeat(80))) }
    @Test fun eightyOneCodePointsRejected() { assertNotNull(ProfileContract.nameError("🌿".repeat(81))) }
    @Test(expected = IllegalArgumentException::class) fun mismatchedUidRejected() { ProfileContract.decode("A", completeFields("B")) }
    @Test fun mergePlanPreservesCreatedAtAndUsesServerTokens() {
        val old = completeFields() + ("custom" to "keep")
        val update = ProfileMerge.fields("A", "a@b.test", " Nila ", "LKR", old)
        assertFalse(update.containsKey("custom")); assertFalse(update.containsKey("createdAt"))
        assertEquals(ProfileServerValue.TIMESTAMP, update["updatedAt"])
        assertEquals(Instant.ofEpochSecond(100), (old + update)["createdAt"])
        assertEquals("keep", (old + update)["custom"])
        val fresh = ProfileMerge.fields("A", "a@b.test", "Nila", "LKR", null)
        assertEquals(ProfileServerValue.TIMESTAMP, fresh["createdAt"])
        assertEquals(false, fresh["profileCategoriesReady"])
    }
}
