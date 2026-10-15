package com.example.clearledger.ui.auth

import com.example.clearledger.core.common.Result
import com.example.clearledger.core.common.UiState
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: FakeAuthRepositoryForLogin
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepositoryForLogin()
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun login_success_updatesStateToSuccess() = runTest {
        authRepository.resultToReturn = Result.Success(User("123", "test@example.com"))

        viewModel.login("test@example.com", "password123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.loginState.value
        assertTrue(state is UiState.Success)
        assertEquals("123", (state as UiState.Success).data.uid)
    }

    @Test
    fun login_error_updatesStateToError() = runTest {
        authRepository.resultToReturn = Result.Error(Exception("Invalid credentials"))

        viewModel.login("test@example.com", "wrongpass")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.loginState.value
        assertTrue(state is UiState.Error)
        assertEquals("Invalid credentials", (state as UiState.Error).message)
    }
}

class FakeAuthRepositoryForLogin : AuthRepository {
    var resultToReturn: Result<User> = Result.Success(User())
    var retryResultToReturn: Result<User> = Result.Success(User())
    var checkResultToReturn: Result<AuthRepository.SetupStatus> =
        Result.Success(AuthRepository.SetupStatus.NotAuthenticated)

    override suspend fun register(email: String, password: String): Result<User> = resultToReturn
    override suspend fun login(email: String, password: String): Result<User> = resultToReturn
    override suspend fun logout(): Result<Unit> = Result.Success(Unit)
    override suspend fun retryProfileSetup(): Result<User> = retryResultToReturn
    override suspend fun checkIncompleteSetup(): Result<AuthRepository.SetupStatus> = checkResultToReturn
}
