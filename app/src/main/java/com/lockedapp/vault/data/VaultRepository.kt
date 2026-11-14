package com.lockedapp.vault.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
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

    /** Takes JPEG bytes straight from CameraX (already rotated) and writes them encrypted —
     *  the photo never exists as a plaintext file and is never registered with MediaStore/Gallery. */
    suspend fun importCapturedPhoto(jpegBytes: ByteArray): VaultItemEntity = withContext(Dispatchers.IO) {
        val storedFileName = UUID.randomUUID().toString()
        val target = File(vaultDir, storedFileName)
        CryptoManager.encryptedFile(context, target).openFileOutput().use { out ->
            out.write(jpegBytes)
        }

        val item = VaultItemEntity(
            displayName = "Camera_${System.currentTimeMillis()}.jpg",
            mimeType = "image/jpeg",
            storedFileName = storedFileName,
            type = VaultItemType.IMAGE,
            sizeBytes = jpegBytes.size.toLong(),
            dateAdded = System.currentTimeMillis()
        )
        val id = dao.insert(item)
        item.copy(id = id)
    }

    /** A private, non-public location for CameraX to record into — video muxing needs a
     *  seekable File, so it can't be written straight into EncryptedFile's cipher stream.
     *  It lives under filesDir (never scanned by MediaStore/Gallery) and is deleted the
     *  moment [importCapturedVideo] has re-encrypted it into the vault. */
    fun newPrivateVideoTempFile(): File {
        val dir = File(context.filesDir, "video_tmp").apply { mkdirs() }
        return File(dir, "${UUID.randomUUID()}.mp4")
    }

    suspend fun importCapturedVideo(tempFile: File): VaultItemEntity = withContext(Dispatchers.IO) {
        val storedFileName = UUID.randomUUID().toString()
        val target = File(vaultDir, storedFileName)
        CryptoManager.encryptedFile(context, target).openFileOutput().use { out ->
            tempFile.inputStream().use { input -> input.copyTo(out) }
        }
        val size = tempFile.length()
        tempFile.delete()

        val item = VaultItemEntity(
            displayName = "Video_${System.currentTimeMillis()}.mp4",
            mimeType = "video/mp4",
            storedFileName = storedFileName,
            type = VaultItemType.VIDEO,
            sizeBytes = size,
            dateAdded = System.currentTimeMillis()
        )
        val id = dao.insert(item)
        item.copy(id = id)
    }

    /** Creates a new text note, or overwrites [existing]'s content in place (same id, new backing file). */
    suspend fun saveTextNote(existing: VaultItemEntity?, content: String): VaultItemEntity =
        withContext(Dispatchers.IO) {
            val bytes = content.toByteArray(Charsets.UTF_8)
            val storedFileName = UUID.randomUUID().toString()
            CryptoManager.encryptedFile(context, File(vaultDir, storedFileName)).openFileOutput().use { out ->
                out.write(bytes)
            }

            val title = content.lineSequence().firstOrNull { it.isNotBlank() }
                ?.take(40)?.trim()
                ?.ifBlank { null }
                ?: "Note_${System.currentTimeMillis()}"

            if (existing != null) {
                File(vaultDir, existing.storedFileName).delete()
                val updated = existing.copy(
                    displayName = title,
                    storedFileName = storedFileName,
                    sizeBytes = bytes.size.toLong(),
                    dateAdded = System.currentTimeMillis()
                )
                dao.update(updated)
                updated
            } else {
                val item = VaultItemEntity(
                    displayName = title,
                    mimeType = "text/plain",
                    storedFileName = storedFileName,
                    type = VaultItemType.TEXT_NOTE,
                    sizeBytes = bytes.size.toLong(),
                    dateAdded = System.currentTimeMillis()
                )
                val id = dao.insert(item)
                item.copy(id = id)
            }
        }

    suspend fun readTextNote(item: VaultItemEntity): String = withContext(Dispatchers.IO) {
        val source = File(vaultDir, item.storedFileName)
        if (!source.exists()) return@withContext ""
        val bytes = CryptoManager.encryptedFile(context, source).openFileInput().use { it.readBytes() }
        String(bytes, Charsets.UTF_8)
    }

    /** Same rationale as [newPrivateVideoTempFile] — MediaRecorder needs a seekable File. */
    fun newPrivateVoiceTempFile(): File {
        val dir = File(context.filesDir, "voice_tmp").apply { mkdirs() }
        return File(dir, "${UUID.randomUUID()}.m4a")
    }

    suspend fun importCapturedVoiceNote(tempFile: File): VaultItemEntity = withContext(Dispatchers.IO) {
        val storedFileName = UUID.randomUUID().toString()
        CryptoManager.encryptedFile(context, File(vaultDir, storedFileName)).openFileOutput().use { out ->
            tempFile.inputStream().use { input -> input.copyTo(out) }
        }
        val size = tempFile.length()
        tempFile.delete()

        val item = VaultItemEntity(
            displayName = "Voice note ${System.currentTimeMillis()}",
            mimeType = "audio/mp4",
            storedFileName = storedFileName,
            type = VaultItemType.VOICE_NOTE,
            sizeBytes = size,
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

    /** Decodes a downsampled bitmap straight from the encrypted stream — no plaintext ever touches disk. */
    suspend fun decodeImageBitmap(item: VaultItemEntity, maxDimension: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            val source = File(vaultDir, item.storedFileName)
            if (!source.exists()) return@withContext null
            val encryptedFile = CryptoManager.encryptedFile(context, source)

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            encryptedFile.openFileInput().use { BitmapFactory.decodeStream(it, null, bounds) }

            val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            encryptedFile.openFileInput().use { BitmapFactory.decodeStream(it, null, opts) }
        }

    /** Extracts a single frame as a thumbnail; briefly decrypts to cache to use MediaMetadataRetriever. */
    suspend fun decodeVideoThumbnail(item: VaultItemEntity): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val uri = openForViewing(item)
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val frame = retriever.frameAtTime
            retriever.release()
            frame
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqSize: Int): Int {
        var sample = 1
        while (width / sample > reqSize * 2 && height / sample > reqSize * 2) sample *= 2
        return sample
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
