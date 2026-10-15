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
        val viewModel = SplashViewModel(authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SplashState.NavigateHome, viewModel.splashState.value)
    }

    @Test
    fun splash_notAuthenticated_navigatesLogin() = runTest {
        authRepository.checkResultToReturn = Result.Success(
            AuthRepository.SetupStatus.NotAuthenticated
        )
        val viewModel = SplashViewModel(authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SplashState.NavigateLogin, viewModel.splashState.value)
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
