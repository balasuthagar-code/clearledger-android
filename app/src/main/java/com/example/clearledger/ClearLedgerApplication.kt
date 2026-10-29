package com.example.clearledger

import android.app.Application
import com.example.clearledger.notifications.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ClearLedgerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.schedule(this)
    }
}