package com.example.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "album_links",
    indices = [
        Index(value = ["url"], unique = true)
    ]
)
data class AlbumLinkItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val albumName: String = "Google Photos Album",
    val photoCount: Int = 0,
    val dateAdded: Long = System.currentTimeMillis(),
    val isIncludedInSlideshow: Boolean = true
)
