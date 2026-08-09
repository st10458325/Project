package com.maryhairsalon.data.repository

import android.util.Log
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.maryhairsalon.data.models.User
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

    fun logout() = auth.signOut()

    // Mirrors friendlyAuthError() in components/auth-form.php
    private fun friendlyAuthError(e: Exception): String = when (e) {
        is FirebaseAuthUserCollisionException ->
            "That email is already registered — try logging in instead."
        is FirebaseAuthInvalidCredentialsException ->
            "Incorrect email or password."
        is FirebaseAuthInvalidUserException ->
            "No account found with that email."
        is FirebaseAuthWeakPasswordException ->
            "Password should be at least 8 characters."
        else -> {
            val msg = e.message.orEmpty()
            when {
                "TOO_MANY_ATTEMPTS" in msg -> "Too many attempts — please wait a moment and try again."
                "INVALID_EMAIL" in msg -> "That doesn't look like a valid email address."
                else -> "Something went wrong. Please try again."
            }
        }
    }
}
