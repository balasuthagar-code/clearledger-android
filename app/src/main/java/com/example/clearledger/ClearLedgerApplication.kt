package com.example.clearledger

import android.app.Application
import com.example.clearledger.notifications.ReminderScheduler
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ClearLedgerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val firestore = FirebaseFirestore.getInstance()
        firestore.setFirestoreSettings(
            FirebaseFirestoreSettings.Builder(firestore.firestoreSettings)
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
        )
        ReminderScheduler.schedule(this)
    }
}
