package com.lockedapp.vault

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lockedapp.vault.crypto.PinManager
import com.lockedapp.vault.data.VaultItemEntity
import com.lockedapp.vault.data.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    val pinManager = PinManager(application)
    private val repository = VaultRepository(application)

    private val _items = MutableStateFlow<List<VaultItemEntity>>(emptyList())
    val items: StateFlow<List<VaultItemEntity>> = _items.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeItems().collect { _items.value = it }
        }
    }

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            repository.importFile(uri)
        }
    }

    fun deleteItem(item: VaultItemEntity) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    suspend fun openForViewing(item: VaultItemEntity): Uri = repository.openForViewing(item)
}
