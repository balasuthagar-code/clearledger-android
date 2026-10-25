package com.example.clearledger.profile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var store: FakeProfileStore
    private lateinit var account: FakeProfileAccount
    private lateinit var seed: FakeProfileCategories
    private fun newVm(edit: Boolean = false): ProfileFormViewModel {
        val repository = DefaultProfileRepository(store, account, CurrencyEligibilityPolicy(store, account))
        val categories = ProfileCategories(store, account, seed)
        return if (edit) ProfileEditViewModel(repository, account, categories) else ProfileSetupViewModel(repository, account, categories)
    }
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        store = FakeProfileStore(); account = FakeProfileAccount(); seed = FakeProfileCategories()
    }
    @After fun cleanup() { Dispatchers.resetMain() }
    @Test fun onboardingDefaultsLkrAndPreventsRepeatedSave() = runTest {
        val vm = newVm(); advanceUntilIdle()
        assertEquals("LKR", (vm.state.value as ProfileSetupState.Editing).draft.baseCurrency)
        vm.editName(" நிலா "); vm.save(); vm.save(); advanceUntilIdle()
        assertTrue(vm.state.value is ProfileSetupState.Completed)
        assertEquals(1, seed.calls)
        assertEquals(1, store.writes.count { "displayName" in it.second })
        vm.save(); advanceUntilIdle(); assertEquals(1, seed.calls)
    }
    @Test fun readFailureRetryThenMissingProfileCanBeCompleted() = runTest {
        store.readError = true; val vm = newVm(); advanceUntilIdle()
        assertTrue(vm.state.value is ProfileSetupState.Error)
        store.readError = false; vm.retry(); advanceUntilIdle()
        assertTrue(vm.state.value is ProfileSetupState.Editing)
        vm.editName("Nila"); vm.save(); advanceUntilIdle()
        assertTrue(vm.state.value is ProfileSetupState.Completed)
    }
    @Test fun saveFailurePreservesDraftAndRetrySucceeds() = runTest {
        val vm = newVm(); advanceUntilIdle(); vm.editName("Nila"); vm.editCurrency("EUR")
        store.writeError = true; vm.save(); advanceUntilIdle()
        assertEquals("Nila", (vm.state.value as ProfileSetupState.Error).draft?.displayName)
        vm.retry(); assertEquals("EUR", (vm.state.value as ProfileSetupState.Editing).draft.baseCurrency)
        store.writeError = false; vm.save(); advanceUntilIdle()
        assertTrue(vm.state.value is ProfileSetupState.Completed)
    }
    @Test fun categoryFailureRetryDoesNotRepeatProfileSave() = runTest {
        seed.fail = true; val vm = newVm(); advanceUntilIdle(); vm.editName("Nila"); vm.save(); advanceUntilIdle()
        assertTrue((vm.state.value as ProfileSetupState.Error).saved)
        assertEquals(false, store.records["A"]?.get("profileCategoriesReady"))
        seed.fail = false; vm.retry(); vm.retry(); advanceUntilIdle()
        assertTrue(vm.state.value is ProfileSetupState.Completed)
        assertEquals(1, store.writes.count { "displayName" in it.second }); assertEquals(2, seed.calls)
        assertEquals(true, store.records["A"]?.get("profileCategoriesReady"))
        val restart = newVm(); advanceUntilIdle()
        assertTrue(restart.state.value is ProfileSetupState.Completed); assertEquals(2, seed.calls)
    }
    @Test fun markerFailureNeverReportsCompletionAndCanResume() = runTest {
        store.markerError = true; val vm = newVm(); advanceUntilIdle(); vm.editName("Nila"); vm.save(); advanceUntilIdle()
        assertTrue((vm.state.value as ProfileSetupState.Error).saved)
        store.markerError = false; vm.retry(); advanceUntilIdle()
        assertTrue(vm.state.value is ProfileSetupState.Completed)
    }
    @Test fun editorShowsExistingProfileAndNameEditSaves() = runTest {
        store.records["A"] = completeFields() + ("profileCategoriesReady" to true)
        val vm = newVm(edit = true); advanceUntilIdle()
        assertEquals("A@example.test", (vm.state.value as ProfileSetupState.Editing).draft.email)
        vm.editName("New name"); vm.save(); advanceUntilIdle()
        assertTrue(vm.state.value is ProfileSetupState.Completed); assertEquals(0, seed.calls)
    }
    @Test fun editorRejectsCurrencyChangeButRetainsDraft() = runTest {
        store.records["A"] = completeFields(); store.history += "A" to "expenses"
        val vm = newVm(edit = true); advanceUntilIdle(); vm.editCurrency("USD"); vm.save(); advanceUntilIdle()
        assertEquals("USD", (vm.state.value as ProfileSetupState.Error).draft?.baseCurrency)
        assertEquals("LKR", store.records["A"]?.get("baseCurrency"))
    }
    @Test fun blankNameValidationIsVisible() = runTest {
        val vm = newVm(); advanceUntilIdle(); vm.save()
        assertNotNull((vm.state.value as ProfileSetupState.Editing).draft.nameError)
    }
    @Test fun switchingAccountCancelsWorkAndClearsOldDraft() = runTest {
        val vm = newVm(); advanceUntilIdle(); vm.editName("Private A")
        val gate = CompletableDeferred<Unit>(); store.beforeSave = { gate.await() }
        vm.save(); runCurrent(); account.users.value = "B"; runCurrent(); gate.complete(Unit); advanceUntilIdle()
        assertEquals("", (vm.state.value as ProfileSetupState.Editing).draft.displayName)
        assertTrue(store.writes.isEmpty())
    }
    @Test fun logoutClearsDraft() = runTest {
        val vm = newVm(); advanceUntilIdle(); vm.editName("Private A"); vm.logout(); advanceUntilIdle()
        assertEquals(ProfileSetupState.SignedOut, vm.state.value)
    }
}
