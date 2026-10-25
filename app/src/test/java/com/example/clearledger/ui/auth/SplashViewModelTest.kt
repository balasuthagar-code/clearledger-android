package com.example.clearledger.ui.auth

import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.User
import com.example.clearledger.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import com.example.clearledger.profile.*
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: FakeAuthRepositoryForSplash

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepositoryForSplash()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun splash_confirmedProfile_navigatesHome() = runTest {
        authRepository.checkResultToReturn = Result.Success(
            AuthRepository.SetupStatus.Confirmed(User("123", "user@example.com"))
        )
        val viewModel = SplashViewModel(authRepository, categoriesForSplash())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SplashState.NavigateHome, viewModel.splashState.value)
    }

    @Test
    fun splash_notAuthenticated_navigatesLogin() = runTest {
        authRepository.checkResultToReturn = Result.Success(
            AuthRepository.SetupStatus.NotAuthenticated
        )
        val viewModel = SplashViewModel(authRepository, categoriesForSplash())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SplashState.NavigateLogin, viewModel.splashState.value)
    }
    @Test fun incompleteProfileRoutesOnboarding() = runTest {
        authRepository.checkResultToReturn = Result.Success(AuthRepository.SetupStatus.Incomplete(User("123")))
        val vm = SplashViewModel(authRepository, categoriesForSplash())
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(SplashState.NavigateOnboarding, vm.splashState.value)
    }
    @Test fun readFailureRetainsRetryableError() = runTest {
        authRepository.checkResultToReturn = Result.Error(Exception("Offline"))
        val vm = SplashViewModel(authRepository, categoriesForSplash())
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(SplashState.Error("Offline"), vm.splashState.value)
        authRepository.checkResultToReturn = Result.Success(AuthRepository.SetupStatus.NotAuthenticated)
        vm.checkSession(); testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(SplashState.NavigateLogin, vm.splashState.value)
    }
    @Test fun categoryFailureDoesNotNavigateHome() = runTest {
        authRepository.checkResultToReturn = Result.Success(AuthRepository.SetupStatus.Confirmed(User("123")))
        val store = FakeProfileStore().apply { records["123"] = completeFields("123") }
        val seeds = FakeProfileCategories().apply { fail = true }
        val vm = SplashViewModel(authRepository, ProfileCategories(store, FakeProfileAccount("123"), seeds))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.splashState.value is SplashState.Error)
        seeds.fail = false; vm.checkSession(); testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(SplashState.NavigateHome, vm.splashState.value)
    }

}

class FakeAuthRepositoryForSplash : AuthRepository {
    var checkResultToReturn: Result<AuthRepository.SetupStatus> =
        Result.Success(AuthRepository.SetupStatus.NotAuthenticated)

    override suspend fun register(email: String, password: String): Result<User> = Result.Success(User())
    override suspend fun login(email: String, password: String): Result<User> = Result.Success(User())
    override suspend fun logout(): Result<Unit> = Result.Success(Unit)
    override suspend fun retryProfileSetup(): Result<User> = Result.Success(User())
    override suspend fun checkIncompleteSetup(): Result<AuthRepository.SetupStatus> = checkResultToReturn
}
