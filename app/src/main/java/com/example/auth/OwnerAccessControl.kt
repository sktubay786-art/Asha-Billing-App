package com.example.auth

import com.google.firebase.auth.FirebaseUser

object OwnerAccessControl {
    const val WORKSPACE_OWNER_ID: String = "asha_owner_shared"

    val ALLOWED_OWNER_EMAILS: Set<String> = setOf(
        "sktubay786@gmail.com",
        "gamingtubay@gmail.com"
    )

    val ALLOWED_OWNER_PHONES: Set<String> = setOf(
        "8710065540",
        "9564061920"
    )

    fun normalizeEmail(raw: String?): String =
        raw?.trim()?.lowercase().orEmpty()

    fun normalizeIndianPhone10(raw: String?): String {
        val digits = raw.orEmpty().replace(Regex("""\D"""), "")
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    fun isAllowedOwnerEmail(email: String?): Boolean {
        val normalized = normalizeEmail(email)
        return normalized.isNotEmpty() && normalized in ALLOWED_OWNER_EMAILS
    }

    fun isAllowedOwnerPhone(phone: String?): Boolean {
        val normalized = normalizeIndianPhone10(phone)
        return normalized.length == 10 && normalized in ALLOWED_OWNER_PHONES
    }

    fun isAuthorizedAppEditor(user: FirebaseUser?): Boolean {
        if (user == null) return false
        return isAllowedOwnerEmail(user.email) || isAllowedOwnerPhone(user.phoneNumber)
    }

    fun workspaceIdFor(user: FirebaseUser): String {
        return if (isAuthorizedAppEditor(user)) WORKSPACE_OWNER_ID else user.uid
    }

    fun ownerIdentityLabel(user: FirebaseUser?): String {
        if (user == null) return "Not signed in"
        val email = user.email?.takeIf { it.isNotBlank() }
        val phone = user.phoneNumber?.takeIf { it.isNotBlank() }
        return email ?: phone ?: "Signed In"
    }
}
