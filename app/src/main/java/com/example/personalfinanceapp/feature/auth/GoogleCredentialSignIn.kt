package com.example.personalfinanceapp.feature.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException

sealed interface GoogleCredentialResult {
    data class Success(val idToken: String) : GoogleCredentialResult
    data object Cancelled : GoogleCredentialResult
    data class Failure(val message: String) : GoogleCredentialResult
}

object GoogleCredentialSignIn {
    suspend fun requestIdToken(activity: Activity): GoogleCredentialResult {
        val clientIdResource = activity.resources.getIdentifier(
            "default_web_client_id",
            "string",
            activity.packageName
        )
        if (clientIdResource == 0) {
            return GoogleCredentialResult.Failure(
                "Google sign-in is not configured for this app."
            )
        }

        val googleIdOption = GetSignInWithGoogleOption.Builder(
            serverClientId = activity.getString(clientIdResource)
        ).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = CredentialManager.create(activity).getCredential(
                context = activity,
                request = request
            )
            val credential = response.credential
            if (
                credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleCredentialResult.Success(googleCredential.idToken)
            } else {
                GoogleCredentialResult.Failure("Google sign-in returned an unsupported credential.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d("GoogleSignIn", "Cancelled", e)
            GoogleCredentialResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.e("GoogleSignIn", "NoCredentialException: ${e.message}", e)
            GoogleCredentialResult.Failure("No Google accounts found. Please add one in Settings.")
        } catch (e: GetCredentialException) {
            Log.e("GoogleSignIn", "GetCredentialException: ${e.message}", e)
            GoogleCredentialResult.Failure("Google sign-in failed. Please try again.")
        } catch (e: IllegalArgumentException) {
            Log.e("GoogleSignIn", "IllegalArgumentException", e)
            GoogleCredentialResult.Failure("Google sign-in returned an invalid credential.")
        } catch (exception: CancellationException) {
            throw exception
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "Exception", e)
            GoogleCredentialResult.Failure("Google sign-in failed. Please try again.")
        }
    }

    suspend fun clearCredentialState(context: Context): Result<Unit> = runCatching {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }
}
