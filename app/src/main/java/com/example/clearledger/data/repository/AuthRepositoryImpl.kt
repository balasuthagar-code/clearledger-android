package com.example.clearledger.data.repository

import android.util.Patterns
import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.User
import com.example.clearledger.domain.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

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
}