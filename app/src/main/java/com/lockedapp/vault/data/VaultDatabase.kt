package com.lockedapp.vault.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [VaultItemEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile private var instance: VaultDatabase? = null

        fun getInstance(context: Context): VaultDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "vault_metadata.db"
                ).build().also { instance = it }
            }
    }
}
