package com.maryhairsalon.ui.auth

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.maryhairsalon.R
import kotlinx.coroutines.launch

@Composable
fun GoogleSignInButton(
    enabled: Boolean,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    OutlinedButton(
        onClick = {
            scope.launch {
                try {
                    val credentialManager = CredentialManager.create(context)
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false) // show all Google accounts on the device, not just ones already used with this app
                        .setServerClientId(context.getString(R.string.google_web_client_id))
                        .build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val response = credentialManager.getCredential(context, request)
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
                    onIdToken(googleIdTokenCredential.idToken)
                } catch (e: GetCredentialException) {
                    Log.e("GoogleSignIn", "Credential retrieval failed", e)
                    onError("Google sign-in was cancelled or is unavailable right now.")
                } catch (e: Exception) {
                    Log.e("GoogleSignIn", "Unexpected error", e)
                    onError("Something went wrong with Google sign-in. Please try again.")
                }
            }
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
    ) {
        Text(stringResource(R.string.action_continue_with_google), fontWeight = FontWeight.SemiBold)
    }
}