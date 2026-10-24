
package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.WeeklyCheckIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreWeeklyCheckInRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : WeeklyCheckInRepository {

    private fun checkInsCollection() =
        firestore.collection("users")
            .document(
                auth.currentUser?.uid
                    ?: throw IllegalStateException(
                        "Please sign in to access weekly check-ins."
                    )
            )
            .collection("checkins")

    override fun getWeeklyCheckIns(): Flow<List<WeeklyCheckIn>> =
        callbackFlow {
            val registration = checkInsCollection()
                .orderBy("weekKey", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val checkIns = snapshot?.documents?.map { document ->
                        WeeklyCheckIn(
                            id = document.id,
                            weekKey = document.getString("weekKey") ?: "",
                            expectedBalanceMinor =
                                document.getLong("expectedBalanceMinor") ?: 0L,
                            reportedBalanceMinor =
                                document.getLong("reportedBalanceMinor") ?: 0L,
                            gapMinor =
                                document.getLong("gapMinor") ?: 0L,
                            note = document.getString("note") ?: "",
                            createdAt = document.getLong("createdAt") ?: 0L,
                            updatedAt = document.getLong("updatedAt") ?: 0L
                        )
                    } ?: emptyList()

                    trySend(checkIns)
                }

            awaitClose {
                registration.remove()
            }
        }

    override suspend fun saveWeeklyCheckIn(
        checkIn: WeeklyCheckIn
    ) {
        val collection = checkInsCollection()
        val now = System.currentTimeMillis()

        val document = if (checkIn.id.isBlank()) {
            collection.document()
        } else {
            collection.document(checkIn.id)
        }

        val existingDocument = document.get().await()

        val createdAt = if (existingDocument.exists()) {
            existingDocument.getLong("createdAt") ?: now
        } else {
            now
        }

        val data = mapOf(
            "weekKey" to checkIn.weekKey,
            "expectedBalanceMinor" to checkIn.expectedBalanceMinor,
            "reportedBalanceMinor" to checkIn.reportedBalanceMinor,
            "gapMinor" to (
                    checkIn.reportedBalanceMinor -
                            checkIn.expectedBalanceMinor
                    ),
            "note" to checkIn.note.trim(),
            "createdAt" to createdAt,
            "updatedAt" to now
        )

        document.set(data).await()
    }
}
//thsianth