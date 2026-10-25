package com.example.clearledger.data.source

import com.example.clearledger.domain.repository.RateMemoryAccount
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

/** Reuses the existing FirebaseAuth provider; identity getter only, with no Firebase data access. */
class FirebaseRateMemoryAccount @Inject constructor(private val auth: FirebaseAuth) : RateMemoryAccount {
    override fun currentUid(): String? = auth.currentUser?.uid
}
