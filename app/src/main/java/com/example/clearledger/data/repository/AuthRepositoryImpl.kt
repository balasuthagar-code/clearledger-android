package com.example.clearledger.data.repository

import android.util.Patterns
import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.User
import com.example.clearledger.domain.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.example.clearledger.profile.ProfileRepository

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val profiles: ProfileRepository
) : AuthRepository {

    override suspend fun login(
        email: String,
        password: String
    ): Result<User> {
        val cleanEmail = email.trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Result.Error(
                IllegalArgumentException("Enter a valid email address.")
            )
        }

        if (password.isEmpty()) {
            return Result.Error(
                IllegalArgumentException("Password cannot be empty.")
            )
        }

        val firebaseUser = try {
            auth.signInWithEmailAndPassword(cleanEmail, password)
                .await()
                .user
                ?: return Result.Error(
                    IllegalStateException("Could not confirm sign in.")
                )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = when {
                e is FirebaseAuthInvalidUserException || e is FirebaseAuthInvalidCredentialsException ->
                    "Invalid email or password. Please try again."

                e is FirebaseNetworkException ->
                    "Could not connect. Check your internet connection."

                e is com.google.firebase.auth.FirebaseAuthException && e.errorCode == "ERROR_TOO_MANY_REQUESTS" ->
                    "Too many failed login attempts. Please try again later."

                else ->
                    "Could not sign in. Please try again."
            }
            return Result.Error(Exception(message, e))
        }

        // Auth success and profile readiness are separate. Splash owns profile read/retry.
        if (auth.currentUser?.uid != firebaseUser.uid) return Result.Error(IllegalStateException("Account changed. Please retry."))
        return Result.Success(User(firebaseUser.uid, firebaseUser.email ?: cleanEmail))
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            auth.signOut()
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(Exception("Could not sign out. Please try again.", e))
        }
    }

    override suspend fun register(
        email: String,
        password: String
    ): Result<User> {
        val cleanEmail = email.trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Result.Error(
                IllegalArgumentException("Enter a valid email address.")
            )
        }

        if (password.length < 8) {
            return Result.Error(
                IllegalArgumentException(
                    "Password must contain at least 8 characters."
                )
            )
        }

        val firebaseUser = try {
            auth.createUserWithEmailAndPassword(cleanEmail, password)
                .await()
                .user
                ?: return Result.Error(
                    IllegalStateException(
                        "Could not confirm account creation."
                    )
                )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = when (e) {
                is FirebaseAuthUserCollisionException ->
                    "This email is already registered. Please sign in."

                is FirebaseAuthWeakPasswordException ->
                    "This password does not meet the password requirements."

                is FirebaseAuthInvalidCredentialsException ->
                    "Enter a valid email address."

                is FirebaseNetworkException ->
                    "Could not connect. Check your internet connection."

                else ->
                    "Could not create your account. Please try again."
            }

            return Result.Error(Exception(message, e))
        }

        val user = User(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: cleanEmail
        )

        return try {
            mergeAuthUser(user)

            Result.Success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Account creation succeeded; a missing profile is recoverable through onboarding.
            if (auth.currentUser?.uid == user.uid) Result.Success(user)
            else Result.Error(IllegalStateException("Account changed. Please retry."))
        }
    }

    override suspend fun retryProfileSetup(): Result<User> {
        val firebaseUser = auth.currentUser
            ?: return Result.Error(Exception("No authenticated user found."))

        val user = User(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: ""
        )

        return try {
            mergeAuthUser(user)

            Result.Success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(
                Exception("Could not save your profile. Please try again.", e)
            )
        }
    }

    override suspend fun checkIncompleteSetup(): Result<AuthRepository.SetupStatus> {
        val firebaseUser = auth.currentUser
            ?: return Result.Success(AuthRepository.SetupStatus.NotAuthenticated)

        return when (val result = profiles.load(firebaseUser.uid)) {
            is Result.Error -> result
            is Result.Success -> Result.Success(if (result.data.complete)
                AuthRepository.SetupStatus.Confirmed(result.data.user)
                else AuthRepository.SetupStatus.Incomplete(result.data.user))
        }
    }

    private suspend fun mergeAuthUser(user: User) {
        check(auth.currentUser?.uid == user.uid) { "Account changed. Please retry." }
        val ref = firestore.collection("users").document(user.uid)
        firestore.runTransaction { tx ->
            check(auth.currentUser?.uid == user.uid) { "Account changed. Please retry." }
            val storedUid = tx.get(ref).get("uid")
            check(storedUid == null || storedUid == "" || storedUid == user.uid) { "Profile account does not match." }
            tx.set(ref, authFields(user), SetOptions.merge())
            Unit
        }.await()
        check(auth.currentUser?.uid == user.uid) { "Account changed. Please retry." }
    }

    // Never serialize User defaults into a profile during auth or recovery.
    private fun authFields(user: User): Map<String, Any> = buildMap {
        put("uid", user.uid)
        if (user.email.isNotBlank()) put("email", user.email)
    }
}
