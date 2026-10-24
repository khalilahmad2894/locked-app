package com.lockedapp.vault.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromType(type: VaultItemType): String = type.name

    @TypeConverter
    fun toType(value: String): VaultItemType = VaultItemType.valueOf(value)
}
