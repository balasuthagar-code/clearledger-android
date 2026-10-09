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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun register_success_updatesStateToSuccess() = runTest {
        authRepository.resultToReturn = Result.Success(User("123", "test@example.com"))
        viewModel = RegisterViewModel(authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.register("test@example.com", "password123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.registerState.value
        assertTrue(state is UiState.Success)
        assertEquals("123", (state as UiState.Success).data.uid)
    }

    @Test
    fun register_error_updatesStateToErrorWithoutProfileRetry() = runTest {
        authRepository.resultToReturn = Result.Error(Exception("Invalid email format"))
        viewModel = RegisterViewModel(authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.register("invalid-email", "pass")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.registerState.value
        assertTrue(state is UiState.Error)
        assertEquals("Invalid email format", (state as UiState.Error).message)
        assertFalse(state.canRetryProfile)
    }

    @Test
    fun startupCheck_incompleteProfile_setsErrorWithCanRetryTrue() = runTest {
        authRepository.checkResultToReturn = Result.Success(
            AuthRepository.SetupStatus.Incomplete(User("789", "incomplete@example.com"))
        )
        viewModel = RegisterViewModel(authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.registerState.value
        assertTrue(state is UiState.Error)
        assertTrue((state as UiState.Error).canRetryProfile)
    }

    @Test
    fun startupCheck_confirmedProfile_setsSuccess() = runTest {
        authRepository.checkResultToReturn = Result.Success(
            AuthRepository.SetupStatus.Confirmed(User("999", "confirmed@example.com"))
        )
        viewModel = RegisterViewModel(authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.registerState.value
        assertTrue(state is UiState.Success)
        assertEquals("999", (state as UiState.Success).data.uid)
    }

    @Test
    fun retryProfileSetup_success_updatesStateToSuccess() = runTest {
        authRepository.retryResultToReturn = Result.Success(User("456", "retry@example.com"))
        viewModel = RegisterViewModel(authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.retryProfileSetup()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.registerState.value
        assertTrue(state is UiState.Success)
        assertEquals("456", (state as UiState.Success).data.uid)
    }
}

class FakeAuthRepository : AuthRepository {
    var resultToReturn: Result<User> = Result.Success(User())
    var retryResultToReturn: Result<User> = Result.Success(User())
    var checkResultToReturn: Result<AuthRepository.SetupStatus> =
        Result.Success(AuthRepository.SetupStatus.NotAuthenticated)

    override suspend fun register(email: String, password: String): Result<User> {
        return resultToReturn
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return resultToReturn
    }

    override suspend fun logout(): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun retryProfileSetup(): Result<User> {
        return retryResultToReturn
    }

    override suspend fun checkIncompleteSetup(): Result<AuthRepository.SetupStatus> {
        return checkResultToReturn
    }
}
