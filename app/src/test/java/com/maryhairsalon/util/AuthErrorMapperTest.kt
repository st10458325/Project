package com.maryhairsalon.util

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies that raw Firebase exceptions are translated into friendly,
 * user-safe messages. MockK stands in for the Firebase exception classes
 * so the tests run on the plain JVM (no emulator needed — important for CI).
 */
class AuthErrorMapperTest {

    @Test
    fun `existing email maps to already-registered message`() {
        val e = mockk<FirebaseAuthUserCollisionException>()
        assertEquals("That email is already registered — try logging in instead.", friendlyAuthError(e))
    }

    @Test
    fun `wrong password maps to incorrect credentials message`() {
        val e = mockk<FirebaseAuthInvalidCredentialsException>()
        assertEquals("Incorrect email or password.", friendlyAuthError(e))
    }

    @Test
    fun `unknown user maps to no-account message`() {
        val e = mockk<FirebaseAuthInvalidUserException>()
        assertEquals("No account found with that email.", friendlyAuthError(e))
    }

    @Test
    fun `weak password maps to length hint`() {
        val e = mockk<FirebaseAuthWeakPasswordException>()
        assertEquals("Password should be at least 8 characters.", friendlyAuthError(e))
    }

    @Test
    fun `too many attempts is detected from the raw message`() {
        val e = Exception("We have blocked requests: TOO_MANY_ATTEMPTS_TRY_LATER")
        assertEquals("Too many attempts — please wait a moment and try again.", friendlyAuthError(e))
    }

    @Test
    fun `invalid email is detected from the raw message`() {
        val e = Exception("INVALID_EMAIL")
        assertEquals("That doesn't look like a valid email address.", friendlyAuthError(e))
    }

    @Test
    fun `anything else falls back to a generic message`() {
        assertEquals("Something went wrong. Please try again.", friendlyAuthError(Exception("boom")))
    }
}