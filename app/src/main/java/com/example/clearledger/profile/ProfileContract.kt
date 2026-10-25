package com.example.clearledger.profile

import com.example.clearledger.core.common.Currencies
import com.example.clearledger.domain.model.User
import java.time.Instant

/** Metadata wraps the existing User; raw presence is never inferred from constructor defaults. */
data class ProfileRecord(val user: User, val exists: Boolean, val categoriesReady: Boolean,
    val complete: Boolean, val storedCurrency: Any?)

object ProfileContract {
    const val MAX_NAME_CODE_POINTS = 80
    fun nameError(name: String): String? = when {
        name.trim().isBlank() -> "Enter your display name."
        name.trim().codePointCount(0, name.trim().length) > MAX_NAME_CODE_POINTS -> "Use 80 characters or fewer."
        else -> null
    }
    fun decode(uid: String, fields: Map<String, Any?>?): ProfileRecord {
        val storedUid = fields?.get("uid") as? String ?: ""
        require(storedUid.isEmpty() || storedUid == uid) { "Profile account does not match. Contact support." }
        val name = fields?.get("displayName") as? String ?: ""
        val currency = fields?.get("baseCurrency") as? String ?: ""
        val email = fields?.get("email") as? String ?: ""
        val user = User(storedUid, email, name, currency)
        val complete = fields != null && storedUid == uid && uid.isNotBlank() && email.isNotBlank() &&
            name == name.trim() && nameError(name) == null && currency in Currencies.supported &&
            fields["profileCategoriesReady"] is Boolean && fields["createdAt"] is Instant && fields["updatedAt"] is Instant
        return ProfileRecord(user, fields != null, fields?.get("profileCategoriesReady") == true,
            complete, fields?.get("baseCurrency"))
    }
}

/** Adapter maps this token to FieldValue.serverTimestamp(), never a device timestamp. */
enum class ProfileServerValue { TIMESTAMP }

object ProfileMerge {
    fun fields(uid: String, email: String, name: String, currency: String,
        old: Map<String, Any?>?): Map<String, Any> {
        require(uid.isNotBlank() && email.isNotBlank()) { "Sign in again to confirm your account email." }
        require(ProfileContract.nameError(name) == null) { ProfileContract.nameError(name) ?: "Invalid name." }
        require(currency in Currencies.supported) { "Choose a supported currency." }
        val oldUid = old?.get("uid")
        require(oldUid == null || oldUid == "" || oldUid == uid) { "Profile account does not match. Contact support." }
        return buildMap {
            put("uid", uid); put("email", email); put("displayName", name.trim()); put("baseCurrency", currency)
            put("profileCategoriesReady", old?.get("profileCategoriesReady") as? Boolean ?: false)
            if (old?.get("createdAt") !is Instant) put("createdAt", ProfileServerValue.TIMESTAMP)
            put("updatedAt", ProfileServerValue.TIMESTAMP)
        }
    }
}
