package com.example.clearledger.data.source

import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.CurrencySnapshotMetadata
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import javax.inject.Inject

/** Uses the existing FirebaseModule provider; observes only, never signs in/out or writes. */
class FirebaseCurrencyAuthSource @Inject constructor(
    private val auth: FirebaseAuth
) : CurrencyAuthSource {
    override fun observeUid(onUid: (String?) -> Unit): CurrencySubscription {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            onUid(firebaseAuth.currentUser?.uid)
        }
        // Firebase invokes a new AuthStateListener with the current state as well as later changes.
        auth.addAuthStateListener(listener)
        return CurrencySubscription { auth.removeAuthStateListener(listener) }
    }
}

/** Raw field access retains wrong-type values for validation rather than coercing them to Strings. */
class FirestoreCurrencyDocumentSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : CurrencyDocumentSource {
    override fun observe(
        uid: String,
        onResult: (Result<CurrencyDocument>) -> Unit
    ): CurrencySubscription {
        val registration = firestore.collection("users").document(uid)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    onResult(Result.Error(error))
                } else if (snapshot == null) {
                    onResult(Result.Error(IllegalStateException("User document snapshot was absent")))
                } else {
                    onResult(Result.Success(CurrencyDocument(
                        exists = snapshot.exists(),
                        baseCurrency = snapshot.get("baseCurrency"),
                        metadata = CurrencySnapshotMetadata(
                            isFromCache = snapshot.metadata.isFromCache,
                            hasPendingWrites = snapshot.metadata.hasPendingWrites()
                        )
                    )))
                }
            }
        return CurrencySubscription { registration.remove() }
    }
}
