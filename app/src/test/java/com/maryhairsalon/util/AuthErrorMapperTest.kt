package com.maryhairsalon.util

import com.google.firebase.auth.FirebaseAuthUserCollisionException
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthErrorMapperTest {

    // Firebase's own exception classes often don't expose public constructors,
    // so we use MockK to create a real instance of that type without calling
    // one — this sidesteps that entirely rather than fighting it.
    @Test
    fun `collision exception maps to already-registered message`() {
        val exception = mockk<FirebaseAuthUserCollisionException>()
        assertEquals(
            "That email is already registered — try logging in instead.",
            friendlyAuthError(exception)
        )
    }

    @Test
    fun `TOO_MANY_ATTEMPTS message maps to rate-limit warning`() {
        assertEquals(
            "Too many attempts — please wait a moment and try again.",
            friendlyAuthError(Exception("TOO_MANY_ATTEMPTS: blocked"))
        )
    }

    @Test
    fun `unrecognized exception falls back to generic message`() {
        assertEquals(
            "Something went wrong. Please try again.",
            friendlyAuthError(Exception("some unrelated failure"))
        )
    }
}
