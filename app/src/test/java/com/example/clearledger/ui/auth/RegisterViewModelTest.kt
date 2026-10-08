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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository()
        viewModel = RegisterViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun register_success_updatesStateToSuccess() = runTest {
        authRepository.resultToReturn = Result.Success(User("123", "test@example.com"))

        viewModel.register("test@example.com", "password123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.registerState.value
        assert(state is UiState.Success)
        assertEquals("123", (state as UiState.Success).data.uid)
    }

    @Test
    fun register_error_updatesStateToError() = runTest {
        authRepository.resultToReturn = Result.Error(Exception("Error message"))

        viewModel.register("test@example.com", "password123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.registerState.value
        assert(state is UiState.Error)
        assertEquals("Error message", (state as UiState.Error).message)
    }

    @Test
    fun retryProfileSetup_success_updatesStateToSuccess() = runTest {
        authRepository.retryResultToReturn = Result.Success(User("456", "retry@example.com"))

        viewModel.retryProfileSetup()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.registerState.value
        assert(state is UiState.Success)
        assertEquals("456", (state as UiState.Success).data.uid)
    }
}

class FakeAuthRepository : AuthRepository {
    var resultToReturn: Result<User> = Result.Success(User())
    var retryResultToReturn: Result<User> = Result.Success(User())
    var checkResultToReturn: Result<User?> = Result.Success(null)

    override suspend fun register(email: String, password: String): Result<User> {
        return resultToReturn
    }

    override suspend fun retryProfileSetup(): Result<User> {
        return retryResultToReturn
    }

    override suspend fun checkIncompleteSetup(): Result<User?> {
        return checkResultToReturn
    }
}
