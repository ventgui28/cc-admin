package com.ventgui.app.data.utils

import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    private val ALL_AUTHENTICATORS = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    /**
     * DEBUG version: always shows the raw error code + message in onError so we can diagnose
     * OPPO ColorOS / other OEM issues.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onNoBiometrics: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // Log BiometricManager canAuthenticate result for debugging
        val manager = BiometricManager.from(activity)
        val statusAll   = manager.canAuthenticate(ALL_AUTHENTICATORS)
        val statusStrong = manager.canAuthenticate(BIOMETRIC_STRONG)
        val statusWeak   = manager.canAuthenticate(BIOMETRIC_WEAK)
        Log.d("BiometricHelper", "canAuthenticate(ALL)=$statusAll  STRONG=$statusStrong  WEAK=$statusWeak")

        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Log.d("BiometricHelper", "SUCCESS type=${result.authenticationType}")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.d("BiometricHelper", "ERROR code=$errorCode msg=$errString")
                when (errorCode) {
                    BiometricPrompt.ERROR_HW_NOT_PRESENT,
                    BiometricPrompt.ERROR_NO_BIOMETRICS,
                    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> {
                        // Show debug info on screen instead of opening password dialog
                        onError("Erro $errorCode: $errString")
                    }

                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onError("")

                    else -> onError("Erro $errorCode: $errString")
                }
            }

            override fun onAuthenticationFailed() {
                Log.d("BiometricHelper", "FAILED (individual attempt rejected)")
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(ALL_AUTHENTICATORS)
            .setConfirmationRequired(false)
            .build()

        prompt.authenticate(info)
    }
}
