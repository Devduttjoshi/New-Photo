package com.example.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PhotoSource(val displayName: String) {
    USB("USB Storage"),
    CLOUD("Cloud Synced"),
    PRELOADED("Ambient Gallery"),
    LOCAL("Local Storage")
}

@Entity(
    tableName = "photos",
    indices = [
        Index(value = ["weekLabel"]),
        Index(value = ["albumProgressionIndex"]),
        Index(value = ["isCached"])
    ]
)
data class PhotoItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val localCachedPath: String? = null,
    val title: String,
    val source: String,
    val albumName: String = "All Photos",
    val albumProgressionIndex: Int = 0,
    val dateAdded: Long = System.currentTimeMillis(),
    val dateTakenMillis: Long = System.currentTimeMillis(),
    val weekLabel: String = "All Photos", // e.g. "Oct 2024 • Week 43"
    val fileSizeBytes: Long = 0,
    val isCached: Boolean = false,
    val width: Int = 1920,
    val height: Int = 1080,
    val lastShownTimestamp: Long = 0L,
    val showCount: Int = 0
) {
    /**
     * Resolves display model: if local cached file exists on disk, use it to ensure 0 network usage offline.
     */
    val displayUri: String
        get() = localCachedPath ?: uri
}
