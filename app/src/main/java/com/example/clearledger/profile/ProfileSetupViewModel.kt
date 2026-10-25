package com.example.clearledger.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.core.common.Currencies
import com.example.clearledger.core.common.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileDraft(val displayName: String = "", val baseCurrency: String = "LKR", val email: String = "",
    val expectedCurrency: Any? = null, val nameError: String? = null)
sealed interface ProfileSetupState {
    data object Loading : ProfileSetupState
    data class Editing(val draft: ProfileDraft) : ProfileSetupState
    data class Saving(val draft: ProfileDraft) : ProfileSetupState
    data class Completed(val profile: ProfileRecord) : ProfileSetupState
    data class Error(val message: String, val draft: ProfileDraft?, val saved: Boolean = false) : ProfileSetupState
    data object SignedOut : ProfileSetupState
}

/** Same state machine for setup and explicit profile editing; route-owned instances never share drafts. */
open class ProfileFormViewModel(private val repository: ProfileRepository, private val account: ProfileAccount,
    private val categories: ProfileCategories, private val editMode: Boolean) : ViewModel() {
    private val mutable = MutableStateFlow<ProfileSetupState>(ProfileSetupState.Loading)
    val state = mutable.asStateFlow()
    private var owner: String? = null
    private var generation = 0L
    private var job: Job? = null
    private var saved: ProfileRecord? = null
    init {
        viewModelScope.launch {
            account.observeUid().collect { uid ->
                job?.cancel(); generation++; saved = null; owner = uid
                mutable.value = if (uid == null) ProfileSetupState.SignedOut else ProfileSetupState.Loading
                if (uid != null) load(uid, generation)
            }
        }
    }
    private fun current(uid: String, ticket: Long) = owner == uid && account.uid == uid && generation == ticket
    private fun draft(profile: ProfileRecord) = ProfileDraft(profile.user.displayName,
        profile.user.baseCurrency.takeIf { it in Currencies.supported } ?: "LKR",
        profile.user.email.ifBlank { account.email ?: "" }, profile.storedCurrency)
    private fun load(uid: String, ticket: Long) {
        job = viewModelScope.launch {
            mutable.value = ProfileSetupState.Loading
            when (val result = repository.load(uid)) {
                is Result.Error -> if (current(uid, ticket)) mutable.value = ProfileSetupState.Error(
                    result.exception.message ?: "Could not load profile. Check your connection and retry.", null)
                is Result.Success -> if (current(uid, ticket)) {
                    if (result.data.complete && !editMode) { saved = result.data; finish(uid, ticket, result.data) }
                    else mutable.value = ProfileSetupState.Editing(draft(result.data))
                }
            }
        }
    }
    fun editName(value: String) {
        val draft = (mutable.value as? ProfileSetupState.Editing)?.draft ?: return
        mutable.value = ProfileSetupState.Editing(draft.copy(displayName = value, nameError = null))
    }
    fun editCurrency(value: String) {
        val draft = (mutable.value as? ProfileSetupState.Editing)?.draft ?: return
        if (value in Currencies.supported) mutable.value = ProfileSetupState.Editing(draft.copy(baseCurrency = value))
    }
    fun save() {
        val uid = owner ?: return; val ticket = generation
        if (!current(uid, ticket) || job?.isActive == true) return
        val draft = (mutable.value as? ProfileSetupState.Editing)?.draft ?: return
        val error = ProfileContract.nameError(draft.displayName)
        if (error != null) { mutable.value = ProfileSetupState.Editing(draft.copy(nameError = error)); return }
        mutable.value = ProfileSetupState.Saving(draft)
        job = viewModelScope.launch {
            when (val result = repository.save(uid, draft.displayName, draft.baseCurrency, draft.expectedCurrency)) {
                is Result.Error -> if (current(uid, ticket)) mutable.value = ProfileSetupState.Error(
                    result.exception.message ?: "Could not save. Check your connection and retry.", draft)
                is Result.Success -> if (current(uid, ticket)) { saved = result.data; finish(uid, ticket, result.data) }
            }
        }
    }
    private suspend fun finish(uid: String, ticket: Long, profile: ProfileRecord) {
        mutable.value = ProfileSetupState.Saving(draft(profile))
        when (val result = categories.ensureReady(uid)) {
            is Result.Success -> if (current(uid, ticket)) mutable.value = ProfileSetupState.Completed(profile.copy(categoriesReady = true))
            is Result.Error -> if (current(uid, ticket)) mutable.value = ProfileSetupState.Error(
                "Your profile is saved. Category setup could not finish. Check your connection and retry.", draft(profile), true)
        }
    }
    fun retry() {
        val uid = owner ?: return; val ticket = generation
        if (!current(uid, ticket) || job?.isActive == true) return
        val error = mutable.value as? ProfileSetupState.Error ?: return
        val profile = saved
        if (error.saved && profile != null) job = viewModelScope.launch { finish(uid, ticket, profile) }
        else if (error.draft != null) mutable.value = ProfileSetupState.Editing(error.draft)
        else load(uid, ticket)
    }
    fun reload() {
        val uid = owner ?: return
        if (job?.isActive == true || account.uid != uid) return
        saved = null; load(uid, generation)
    }
    fun logout() {
        job?.cancel(); generation++; saved = null
        try { account.logout(); owner = null; mutable.value = ProfileSetupState.SignedOut }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { mutable.value = ProfileSetupState.Error("Could not sign out. Please try again.", null) }
    }
}

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(repository: ProfileRepository, account: ProfileAccount,
    categories: ProfileCategories) : ProfileFormViewModel(repository, account, categories, false)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(repository: ProfileRepository, account: ProfileAccount,
    categories: ProfileCategories) : ProfileFormViewModel(repository, account, categories, true)
