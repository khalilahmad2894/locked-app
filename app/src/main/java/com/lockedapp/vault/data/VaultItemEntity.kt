package com.lockedapp.vault.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VaultItemType { IMAGE, VIDEO, AUDIO, DOCUMENT, OTHER }

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val mimeType: String,
    val storedFileName: String,
    val type: VaultItemType,
    val sizeBytes: Long,
    val dateAdded: Long
)
