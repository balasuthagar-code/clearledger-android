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

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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

        val uid = firebaseUser.uid
        val fallbackEmail = firebaseUser.email ?: cleanEmail

        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            val user = if (doc.exists()) {
                doc.toObject(User::class.java) ?: User(uid = uid, email = fallbackEmail)
            } else {
                User(uid = uid, email = fallbackEmail)
            }
            Result.Success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(Exception("Signed in, but could not load profile.", e))
        }
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
            firestore.collection("users")
                .document(user.uid)
                .set(user)
                .await()

            Result.Success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(
                Exception(
                    "Your account was created, but your profile could not " +
                            "be saved. Account setup is incomplete.",
                    e
                )
            )
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
            firestore.collection("users")
                .document(user.uid)
                .set(user, SetOptions.merge())
                .await()

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

        val uid = firebaseUser.uid
        val email = firebaseUser.email ?: ""

        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (!doc.exists()) {
                Result.Success(
                    AuthRepository.SetupStatus.Incomplete(User(uid = uid, email = email))
                )
            } else {
                val user = doc.toObject(User::class.java) ?: User(uid = uid, email = email)
                Result.Success(
                    AuthRepository.SetupStatus.Confirmed(user)
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(Exception("Could not verify setup status. Please check your connection.", e))
        }
    }
}