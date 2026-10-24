package com.lockedapp.vault

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lockedapp.vault.crypto.PinManager
import com.lockedapp.vault.data.ThumbnailCache
import com.lockedapp.vault.data.VaultItemEntity
import com.lockedapp.vault.data.VaultItemType
import com.lockedapp.vault.data.VaultRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    val pinManager = PinManager(application)
    private val repository = VaultRepository(application)

    val items: StateFlow<List<VaultItemEntity>> = repository.observeItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val photos: StateFlow<List<VaultItemEntity>> = items
        .map { list -> list.filter { it.type == VaultItemType.IMAGE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videos: StateFlow<List<VaultItemEntity>> = items
        .map { list -> list.filter { it.type == VaultItemType.VIDEO } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val files: StateFlow<List<VaultItemEntity>> = items
        .map { list ->
            list.filter {
                it.type == VaultItemType.DOCUMENT || it.type == VaultItemType.AUDIO || it.type == VaultItemType.OTHER
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Shared with PhotoViewerScreen so it doesn't need the full list passed through nav args. */
    var photoViewerStartIndex: Int = 0
    var activeVideoItem: VaultItemEntity? = null

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            repository.importFile(uri)
        }
    }

    fun importCapturedPhoto(jpegBytes: ByteArray) {
        viewModelScope.launch {
            repository.importCapturedPhoto(jpegBytes)
        }
    }

    fun newPrivateVideoTempFile() = repository.newPrivateVideoTempFile()

    fun importCapturedVideo(tempFile: java.io.File) {
        viewModelScope.launch {
            repository.importCapturedVideo(tempFile)
        }
    }

    fun deleteItem(item: VaultItemEntity) {
        ThumbnailCache.remove(item.id)
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    suspend fun openForViewing(item: VaultItemEntity): Uri = repository.openForViewing(item)

    suspend fun thumbnailFor(item: VaultItemEntity): Bitmap? {
        ThumbnailCache.get(item.id)?.let { return it }
        val bitmap = when (item.type) {
            VaultItemType.IMAGE -> repository.decodeImageBitmap(item, maxDimension = 300)
            VaultItemType.VIDEO -> repository.decodeVideoThumbnail(item)
            else -> null
        }
        bitmap?.let { ThumbnailCache.put(item.id, it) }
        return bitmap
    }

    suspend fun fullImageFor(item: VaultItemEntity): Bitmap? =
        repository.decodeImageBitmap(item, maxDimension = 2048)
}
