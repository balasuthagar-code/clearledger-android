package com.example.clearledger.domain.model

/**
 * The ONLY thing the dashboard needs to know about the primary savings goal (FR-DASH-05).
 * The goals module (Member 2) fills it from its own goal documents; the dashboard never reads
 * or writes goal documents itself.
 *
 * Amounts are minor units in the base currency (BR-3: saved = starting amount + contributions).
 */
data class GoalSummary(
    val name: String,
    val savedMinor: Long,
    val targetMinor: Long,
    val requiredMonthlyMinor: Long? = null,   // BR-4, null if the goals module does not provide it yet
    val statusText: String? = null            // e.g. "On track" / "Behind" / "Achieved" (BR-5), optional
) {
    /** 0.0..1.0 for the progress bar; BR-3: min(saved / target, 1). Safe when target is 0. */
    val progressFraction: Float
        get() = if (targetMinor <= 0L) 0f else (savedMinor.toDouble() / targetMinor.toDouble()).coerceIn(0.0, 1.0).toFloat()

    val progressPercent: Int get() = Math.round(progressFraction * 100f)
}
