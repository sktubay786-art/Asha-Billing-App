package com.example.auth

import com.google.firebase.auth.FirebaseUser

object OwnerAccessControl {
    const val WORKSPACE_OWNER_ID: String = "asha_owner_shared"

    val ALLOWED_OWNER_EMAILS: Set<String> = setOf(
        "sktubay786@gmail.com",
        "gamingtubay@gmail.com"
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

    fun isAuthorizedAppEditor(user: FirebaseUser?): Boolean {
        if (user == null) return false
        return isAllowedOwnerEmail(user.email)
    }

    fun workspaceIdFor(user: FirebaseUser): String {
        return if (isAuthorizedAppEditor(user)) WORKSPACE_OWNER_ID else user.uid
    }

    fun ownerIdentityLabel(user: FirebaseUser?): String {
        if (user == null) return "Not signed in"
        val email = user.email?.takeIf { it.isNotBlank() }
        return email ?: "Signed In"
    }
}
