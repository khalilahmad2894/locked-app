package com.lockedapp.vault.crypto

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Stores only a salted SHA-256 hash of the PIN inside EncryptedSharedPreferences
 * (itself backed by an Android Keystore key) — the plaintext PIN is never persisted.
 */
class PinManager(context: Context) {

    private val prefs = CryptoManager.securePrefs(context, "vault_secure_prefs")

    fun isPinSet(): Boolean = prefs.contains(KEY_HASH)

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hash(pin, salt)
        prefs.edit()
            .putString(KEY_SALT, salt.toHex())
            .putString(KEY_HASH, hash)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saltHex = prefs.getString(KEY_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_HASH, null) ?: return false
        val salt = saltHex.fromHex()
        return hash(pin, salt) == storedHash
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    private fun hash(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    companion object {
        private const val KEY_SALT = "pin_salt"
        private const val KEY_HASH = "pin_hash"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }
}
