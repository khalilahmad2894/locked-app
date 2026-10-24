package com.lockedapp.vault.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY dateAdded DESC")
    fun observeAll(): Flow<List<VaultItemEntity>>

    @Insert
    suspend fun insert(item: VaultItemEntity): Long

    @Delete
    suspend fun delete(item: VaultItemEntity)
}
