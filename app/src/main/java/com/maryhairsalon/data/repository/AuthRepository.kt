package com.maryhairsalon.data.repository

import android.util.Log
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.maryhairsalon.data.models.User
import com.maryhairsalon.util.friendlyAuthError
import kotlinx.coroutines.tasks.await
import java.time.Instant

data class SignupData(
    val name: String,
    val surname: String,
    val email: String,
    val contact: String,
    val password: String
)

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUser get() = auth.currentUser

    suspend fun login(email: String, password: String): AuthResult = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        AuthResult.Success(result.user!!)
    } catch (e: Exception) {
        AuthResult.Error(friendlyAuthError(e))
    }

    suspend fun signup(data: SignupData): AuthResult = try {
        Log.d("AuthRepo", "Starting signup for ${data.email}")
        val result = auth.createUserWithEmailAndPassword(data.email, data.password).await()
        val firebaseUser = result.user!!

        firebaseUser.updateProfile(
            userProfileChangeRequest { displayName = "${data.name} ${data.surname}".trim() }
        ).await()

        val userData = mapOf(
            "name" to data.name,
            "surname" to data.surname,
            "email" to data.email,
            "contact" to data.contact,
            "role" to "customer",
            "createdAt" to Instant.now().toString()
        )

        Log.d("AuthRepo", "Writing user doc to Firestore: $userData")
        db.collection("users").document(firebaseUser.uid).set(userData).await()
        Log.d("AuthRepo", "Firestore write successful for ${firebaseUser.uid}")

        runCatching { firebaseUser.sendEmailVerification().await() }
            .onFailure { Log.e("AuthRepo", "Failed to send verification email", it) }

        AuthResult.Success(firebaseUser)
    } catch (e: Exception) {
        Log.e("AuthRepo", "Signup failed", e)
        AuthResult.Error(friendlyAuthError(e))
    }

    suspend fun getUserRole(uid: String): String = try {
        Log.d("AuthRepo", "Fetching role for uid: $uid")
        val doc = db.collection("users").document(uid).get().await()
        val role = doc.getString("role") ?: "customer"
        Log.d("AuthRepo", "Found role: $role")
        role
    } catch (e: Exception) {
        Log.e("AuthRepo", "Failed to get user role", e)
        "customer"
    }

    suspend fun getUser(uid: String): User? = try {
        db.collection("users").document(uid).get().await().toObject(User::class.java)
    } catch (e: Exception) {
        Log.e("AuthRepo", "Failed to get user", e)
        null
    }

    suspend fun updateProfile(uid: String, name: String, surname: String, contact: String): AuthResult = try {
        db.collection("users").document(uid)
            .update(mapOf("name" to name, "surname" to surname, "contact" to contact))
            .await()
        currentUser?.updateProfile(
            userProfileChangeRequest { displayName = "$name $surname".trim() }
        )?.await()
        AuthResult.Success(currentUser!!)
    } catch (e: Exception) {
        Log.e("AuthRepo", "Failed to update profile", e)
        AuthResult.Error("Couldn't save your changes. Please try again.")
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult {
        val user = currentUser ?: return AuthResult.Error("You're not logged in.")
        val email = user.email ?: return AuthResult.Error("This account has no email on file.")
        return try {
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            AuthResult.Success(user)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error("Your current password is incorrect.")
        } catch (e: Exception) {
            Log.e("AuthRepo", "Failed to change password", e)
            AuthResult.Error(friendlyAuthError(e))
        }
    }

    suspend fun sendEmailVerification(): AuthResult {
        val user = currentUser ?: return AuthResult.Error("You're not logged in.")
        return try {
            user.sendEmailVerification().await()
            AuthResult.Success(user)
        } catch (e: Exception) {
            Log.e("AuthRepo", "Failed to send verification email", e)
            AuthResult.Error("Couldn't send the verification email. Please try again in a moment.")
        }
    }

    /** Pulls fresh verification state AND forces a new ID token — the security rule reads email_verified from the token itself, which won't reflect a just-verified email until the token is force-refreshed. Mirrors the web app's refresh button in verify-banner.js. */
    suspend fun refreshEmailVerifiedStatus(): Boolean {
        val user = currentUser ?: return false
        return try {
            user.reload().await()
            user.getIdToken(true).await()
            user.isEmailVerified
        } catch (e: Exception) {
            Log.e("AuthRepo", "Failed to refresh verification status", e)
            user.isEmailVerified
        }
    }

    fun logout() = auth.signOut()

    suspend fun signInWithGoogle(idToken: String): AuthResult = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val firebaseUser = result.user!!

        // Only write a user doc the first time this Google account signs in —
        // repeat sign-ins must never overwrite an existing role or profile edits.
        val existingDoc = db.collection("users").document(firebaseUser.uid).get().await()
        if (!existingDoc.exists()) {
            val displayName = firebaseUser.displayName.orEmpty().trim()
            val parts = displayName.split(" ", limit = 2)
            val name = parts.getOrElse(0) { "" }
            val surname = parts.getOrElse(1) { "" }

            val userData = mapOf(
                "name" to name,
                "surname" to surname,
                "email" to (firebaseUser.email ?: ""),
                "contact" to "",
                "role" to "customer",
                "createdAt" to Instant.now().toString()
            )
            db.collection("users").document(firebaseUser.uid).set(userData).await()
        }

        AuthResult.Success(firebaseUser)
    } catch (e: FirebaseAuthUserCollisionException) {
        AuthResult.Error("An account with this email already exists. Please log in with your email and password instead.")
    } catch (e: Exception) {
        Log.e("AuthRepo", "Google sign-in failed", e)
        AuthResult.Error(friendlyAuthError(e))
    }
}