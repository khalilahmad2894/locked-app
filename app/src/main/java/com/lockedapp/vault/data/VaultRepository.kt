package com.lockedapp.vault.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.lockedapp.vault.crypto.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class VaultRepository(private val context: Context) {

    private val dao = VaultDatabase.getInstance(context).vaultDao()
    private val vaultDir: File by lazy {
        File(context.filesDir, "vault").apply { mkdirs() }
    }

    fun observeItems(): Flow<List<VaultItemEntity>> = dao.observeAll()

    suspend fun importFile(uri: Uri): VaultItemEntity = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        var displayName = uri.lastPathSegment ?: "file"
        var size = 0L

        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }

        val storedFileName = UUID.randomUUID().toString()
        val target = File(vaultDir, storedFileName)
        val encryptedFile = CryptoManager.encryptedFile(context, target)

        encryptedFile.openFileOutput().use { out ->
            resolver.openInputStream(uri)?.use { input ->
                input.copyTo(out)
            } ?: error("Could not open source file")
        }

        val item = VaultItemEntity(
            displayName = displayName,
            mimeType = mimeType,
            storedFileName = storedFileName,
            type = typeFromMime(mimeType),
            sizeBytes = if (size > 0) size else target.length(),
            dateAdded = System.currentTimeMillis()
        )
        val id = dao.insert(item)
        item.copy(id = id)
    }

    suspend fun deleteItem(item: VaultItemEntity) = withContext(Dispatchers.IO) {
        File(vaultDir, item.storedFileName).delete()
        dao.delete(item)
    }

    /** Decrypts the file into a private cache folder and returns a FileProvider content Uri for viewing. */
    suspend fun openForViewing(item: VaultItemEntity): Uri = withContext(Dispatchers.IO) {
        val source = File(vaultDir, item.storedFileName)
        val encryptedFile = CryptoManager.encryptedFile(context, source)

        val tempDir = File(context.cacheDir, "vault_temp").apply { mkdirs() }
        val tempFile = File(tempDir, item.displayName)
        tempFile.outputStream().use { out ->
            encryptedFile.openFileInput().use { input ->
                input.copyTo(out)
            }
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
    }

    private fun typeFromMime(mimeType: String): VaultItemType = when {
        mimeType.startsWith("image/") -> VaultItemType.IMAGE
        mimeType.startsWith("video/") -> VaultItemType.VIDEO
        mimeType.startsWith("audio/") -> VaultItemType.AUDIO
        mimeType == "application/pdf" ||
            mimeType.startsWith("text/") ||
            mimeType.contains("document") -> VaultItemType.DOCUMENT
        else -> VaultItemType.OTHER
    }
}
