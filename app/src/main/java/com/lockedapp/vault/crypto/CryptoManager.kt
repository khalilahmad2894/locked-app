package com.lockedapp.vault.crypto

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

/**
 * Wraps Android Keystore access. The AES key never leaves hardware-backed
 * storage; files on disk are only ever written/read through EncryptedFile.
 */
object CryptoManager {

    private var masterKey: MasterKey? = null

    private fun getMasterKey(context: Context): MasterKey {
        return masterKey ?: MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            .also { masterKey = it }
    }

    fun encryptedFile(context: Context, file: File): EncryptedFile {
        return EncryptedFile.Builder(
            context.applicationContext,
            file,
            getMasterKey(context),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }

    fun securePrefs(context: Context, name: String) = EncryptedSharedPreferences.create(
        context.applicationContext,
        name,
        getMasterKey(context),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
