package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.AlbumLinkItem
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumLinkDao {
    @Query("SELECT * FROM album_links ORDER BY dateAdded DESC")
    fun getAllAlbumLinks(): Flow<List<AlbumLinkItem>>

    @Query("SELECT * FROM album_links ORDER BY dateAdded DESC")
    suspend fun getAllAlbumLinksList(): List<AlbumLinkItem>

    @Query("SELECT * FROM album_links WHERE url = :url LIMIT 1")
    suspend fun getAlbumByUrl(url: String): AlbumLinkItem?

    @Query("SELECT * FROM album_links WHERE isIncludedInSlideshow = 1")
    suspend fun getIncludedAlbumLinks(): List<AlbumLinkItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(link: AlbumLinkItem): Long

    @Update
    suspend fun update(link: AlbumLinkItem)

    @Query("UPDATE album_links SET isIncludedInSlideshow = :isIncluded WHERE id = :id")
    suspend fun setIncludedInSlideshow(id: Long, isIncluded: Boolean)

    @Query("DELETE FROM album_links WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM album_links WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("SELECT COUNT(*) FROM album_links")
    suspend fun getCount(): Int
}
