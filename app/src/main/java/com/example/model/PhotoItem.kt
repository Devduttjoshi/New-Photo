package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PhotoSource(val displayName: String) {
    USB("USB Storage"),
    CLOUD("Cloud Synced"),
    PRELOADED("Ambient Gallery"),
    LOCAL("Local Storage")
}

@Entity(tableName = "photos")
data class PhotoItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val localCachedPath: String? = null,
    val title: String,
    val source: String,
    val albumName: String = "All Photos",
    val dateAdded: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0,
    val isCached: Boolean = false,
    val width: Int = 1920,
    val height: Int = 1080
) {
    /**
     * Resolves display model: if local cached file exists, use it to ensure 0 network usage offline.
     */
    val displayUri: String
        get() = localCachedPath ?: uri
}
