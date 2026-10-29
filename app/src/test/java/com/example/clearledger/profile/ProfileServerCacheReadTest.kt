package com.example.clearledger.profile

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ProfileServerCacheReadTest {
    private class TestServerException(val failure: ProfileServerFailure) : Exception()
    private fun classify(error: Exception): ProfileServerFailure =
        (error as? TestServerException)?.failure ?: ProfileServerFailure.OTHER

    private fun unavailable() = TestServerException(ProfileServerFailure.UNAVAILABLE)

    @Test
    fun serverSuccessUsesFreshDocumentWithoutReadingCache() = runTest {
        val server = completeFields() + ("displayName" to "Server name")
        var cacheReads = 0

        val result = readProfileServerFirst(
            serverRead = { server },
            cacheRead = { cacheReads++; completeFields() },
            cacheValid = { cachedProfileIsReady("A", it, false) },
            serverFailure = ::classify
        )

        assertEquals("Server name", result["displayName"])
        assertEquals(0, cacheReads)
    }

    @Test
    fun unavailableServerUsesCompleteCachedProfile() = runTest {
        val cached = completeFields() + ("profileCategoriesReady" to true)

        val result = readProfileServerFirst<Map<String, Any?>>(
            serverRead = { throw unavailable() },
            cacheRead = { cached },
            cacheValid = { cachedProfileIsReady("A", it, false) },
            serverFailure = ::classify
        )

        assertTrue(cachedProfileIsReady("A", result, false))
        assertEquals(true, result["profileCategoriesReady"])
    }

    @Test
    fun timedOutServerUsesCompleteCachedProfile() = runTest {
        val cached = completeFields() + ("profileCategoriesReady" to true)
        val timeout = TestServerException(ProfileServerFailure.DEADLINE_EXCEEDED)

        val result = readProfileServerFirst<Map<String, Any?>>(
            serverRead = { throw timeout },
            cacheRead = { cached },
            cacheValid = { cachedProfileIsReady("A", it, false) },
            serverFailure = ::classify
        )

        assertEquals(cached, result)
    }

    @Test
    fun unavailableServerAcceptsCompleteCachedProfileWithPendingWrites() = runTest {
        val cached = completeFields() + ("profileCategoriesReady" to true)

        val result = readProfileServerFirst<Map<String, Any?>>(
            serverRead = { throw unavailable() },
            cacheRead = { cached },
            cacheValid = { cachedProfileIsReady("A", it, hasPendingWrites = true) },
            serverFailure = ::classify
        )

        assertEquals(cached, result)
    }

    @Test
    fun unavailableServerWithoutCachedDocumentRequiresRetry() = runTest {
        try {
            readProfileServerFirst<Map<String, Any?>>(
                serverRead = { throw unavailable() },
                cacheRead = { null },
                cacheValid = { true },
                serverFailure = ::classify
            )
            fail("A missing cached profile must not be treated as incomplete onboarding")
        } catch (e: OfflineProfileUnavailableException) {
            assertTrue(e.message.orEmpty().contains("Connect and retry"))
        }
    }

    @Test
    fun authenticationAndUnrelatedFailuresNeverReadCache() = runTest {
        val failures = listOf(
            TestServerException(ProfileServerFailure.UNAUTHENTICATED),
            IllegalStateException("Unexpected read failure")
        )
        for (failure in failures) {
            var cacheReads = 0
            try {
                readProfileServerFirst<Map<String, Any?>>(
                    serverRead = { throw failure },
                    cacheRead = { cacheReads++; completeFields() },
                    cacheValid = { true },
                    serverFailure = ::classify
                )
                fail("Authentication and unrelated failures must not read cache")
            } catch (e: Exception) {
                assertSame(failure, e)
                assertEquals(0, cacheReads)
            }
        }
    }

    @Test
    fun invalidCachedProfileIsNotAccepted() = runTest {
        val cached = completeFields() - "baseCurrency"
        assertFalse(cachedProfileIsReady("A", cached, false))
        assertFalse(cachedProfileIsReady("A", completeFields(), false))
        assertFalse(cachedProfileIsReady("A", completeFields(), true))

        try {
            readProfileServerFirst<Map<String, Any?>>(
                serverRead = { throw unavailable() },
                cacheRead = { cached },
                cacheValid = { cachedProfileIsReady("A", it, false) },
                serverFailure = ::classify
            )
            fail("An incomplete cached profile must not open Home")
        } catch (_: OfflineProfileUnavailableException) {
        }
    }

    @Test
    fun permissionDeniedNeverReadsCache() = runTest {
        val denied = TestServerException(ProfileServerFailure.PERMISSION_DENIED)
        var cacheReads = 0

        try {
            readProfileServerFirst<Map<String, Any?>>(
                serverRead = { throw denied },
                cacheRead = { cacheReads++; completeFields() },
                cacheValid = { true },
                serverFailure = ::classify
            )
            fail("Permission denial must remain authoritative")
        } catch (e: TestServerException) {
            assertSame(denied, e)
            assertEquals(0, cacheReads)
        }
    }
}
