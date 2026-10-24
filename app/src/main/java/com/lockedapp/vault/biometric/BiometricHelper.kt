package com.lockedapp.vault.biometric

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    /** True if the device has at least one enrolled biometric (face or fingerprint) available. */
    fun isAvailable(activity: FragmentActivity): Boolean {
        val manager = BiometricManager.from(activity)
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    onError("Face/biometric not recognized")
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Vault")
            .setSubtitle("Use your face or fingerprint")
            .setNegativeButtonText("Use PIN instead")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        prompt.authenticate(info)
    }
}
