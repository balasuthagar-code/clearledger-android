package com.example.clearledger.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.navigation.Routes
import com.example.clearledger.ui.auth.SplashState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

object ProfileRoutes {
    const val ONBOARDING = "profile_onboarding"
    const val EDIT = "profile_edit"
    fun splashDestination(state: SplashState): String? = when (state) {
        SplashState.NavigateHome -> Routes.HOME
        SplashState.NavigateLogin -> Routes.LOGIN
        SplashState.NavigateOnboarding, SplashState.NavigateRegister -> ONBOARDING
        SplashState.Loading, is SplashState.Error -> null
    }
    fun afterAuthSuccess() = Routes.SPLASH
    fun afterSave() = Routes.HOME
}

@HiltViewModel
class ProfileSessionViewModel @Inject constructor(private val account: ProfileAccount) : ViewModel() {
    // New process resolves splash again; rotations retain this Activity-scoped ViewModel.
    val navigationSession = java.util.UUID.randomUUID().toString()
    val uid = account.observeUid().stateIn(viewModelScope, SharingStarted.Eagerly, account.uid)
    private val logoutFailure = MutableStateFlow<String?>(null)
    val logoutError = logoutFailure.asStateFlow()
    fun logout() {
        try { account.logout(); logoutFailure.value = null }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { logoutFailure.value = "Could not sign out. Please try again." }
    }
}
