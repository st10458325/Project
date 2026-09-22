package com.maryhairsalon.util

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/** Maps a raw Firebase Auth exception to a message safe to show a user. Pure — no I/O — so it's directly unit-testable. */
fun friendlyAuthError(e: Exception): String = when (e) {
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
