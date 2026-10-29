package com.example.clearledger.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.LocalDate

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val user = FirebaseAuth.getInstance().currentUser ?: return Result.success()
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user.uid)
        val today = LocalDate.now()

        if (today.dayOfWeek == DayOfWeek.MONDAY) {
            val weekKey = today.with(DayOfWeek.MONDAY).toString()
            val checkIns = userRef.collection("checkins")
                .whereEqualTo("weekKey", weekKey)
                .limit(1)
                .get()
                .await()
            if (checkIns.isEmpty) {
                ReminderNotifications.show(
                    applicationContext,
                    ReminderNotifications.WEEKLY_CHECK_IN_ID,
                    "Weekly check-in due",
                    "Record your actual balance and reflect on this week's spending."
                )
            }
        }

        val projects = userRef.collection("freelanceProjects").get().await()
        projects.documents.forEach { document ->
            val paymentStatus = document.getString("paymentStatus") ?: "UNPAID"
            val endDate = document.getString("endDate").orEmpty()
            if (paymentStatus != "PAID" && endDate.isNotBlank() && endDate <= today.toString()) {
                val projectId = document.id
                val projectName = document.getString("name") ?: "freelance project"
                ReminderNotifications.show(
                    applicationContext,
                    projectId.hashCode(),
                    "Freelance payment follow-up",
                    "Payment is still outstanding for $projectName."
                )
            }
        }

        return Result.success()
    }
}
