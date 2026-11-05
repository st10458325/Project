/**
 * User.kt
 *
 * Data model representing a registered application user profile.
 * Maps directly to Firestore documents in the "users" collection.
 * Encapsulates role assignment ("customer", "employee", "manager") and profile details.
 */
package com.maryhairsalon.data.models

import com.google.firebase.firestore.DocumentId

/**
 * Entity storing user account details, role, and optional profile image.
 */
data class User(
    @DocumentId val id: String = "",
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val contact: String = "",
    val role: String = "customer", // "customer", "employee", "manager"
    val title: String = "",        // optional display role for the About screen, e.g. "Master Barber"
    val photoUrl: String = ""      // optional portrait photo — same manual GitHub-hosting pattern as service images
)
