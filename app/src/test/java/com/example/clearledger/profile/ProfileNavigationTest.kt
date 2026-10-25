package com.example.clearledger.profile

import com.example.clearledger.navigation.Routes
import com.example.clearledger.ui.auth.SplashState
import org.junit.Assert.*
import org.junit.Test

/** Tests the routing functions called by AppNavigation; not an Android back-stack test. */
class ProfileNavigationTest {
    @Test fun splashDestinationsUseExistingAuthRoutes() {
        assertEquals(Routes.LOGIN, ProfileRoutes.splashDestination(SplashState.NavigateLogin))
        assertEquals(ProfileRoutes.ONBOARDING, ProfileRoutes.splashDestination(SplashState.NavigateOnboarding))
        assertEquals(Routes.HOME, ProfileRoutes.splashDestination(SplashState.NavigateHome))
    }
    @Test fun loadingAndReadErrorsDoNotNavigate() {
        assertNull(ProfileRoutes.splashDestination(SplashState.Loading))
        assertNull(ProfileRoutes.splashDestination(SplashState.Error("Read failed")))
    }
    @Test fun authSuccessResolvesSetupAndSaveReturnsHome() {
        assertEquals(Routes.SPLASH, ProfileRoutes.afterAuthSuccess())
        assertEquals(Routes.HOME, ProfileRoutes.afterSave())
    }
}
