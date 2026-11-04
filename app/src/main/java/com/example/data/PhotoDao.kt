package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.PhotoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY albumProgressionIndex ASC, id ASC")
    fun getAllPhotos(): Flow<List<PhotoItem>>

    @Query("SELECT * FROM photos ORDER BY albumProgressionIndex ASC, id ASC")
    suspend fun getAllPhotosList(): List<PhotoItem>

    @Query("SELECT * FROM photos WHERE source = :source ORDER BY albumProgressionIndex ASC, id ASC")
    fun getPhotosBySource(source: String): Flow<List<PhotoItem>>

    @Query("SELECT * FROM photos WHERE weekLabel = :weekLabel ORDER BY albumProgressionIndex ASC, id ASC")
    fun getPhotosByWeek(weekLabel: String): Flow<List<PhotoItem>>

    @Query("SELECT DISTINCT weekLabel FROM photos WHERE weekLabel != '' ORDER BY dateTakenMillis DESC")
    fun getDistinctWeeks(): Flow<List<String>>

    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    suspend fun getPhotoById(id: Long): PhotoItem?

    @Query("SELECT * FROM photos WHERE uri = :uri LIMIT 1")
    suspend fun getPhotoByUri(uri: String): PhotoItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<PhotoItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoItem): Long

    @Update
    suspend fun update(photo: PhotoItem)

    @Query("UPDATE photos SET lastShownTimestamp = :timestamp, showCount = showCount + 1 WHERE id = :photoId")
    suspend fun recordPhotoShown(photoId: Long, timestamp: Long)

    @Query("UPDATE photos SET isCached = 0, localCachedPath = NULL, fileSizeBytes = 0 WHERE id = :photoId")
    suspend fun markEvicted(photoId: Long)

    @Query("SELECT * FROM photos WHERE isCached = 1 ORDER BY lastShownTimestamp ASC, dateAdded ASC LIMIT :limit")
    suspend fun getOldestCachedPhotos(limit: Int): List<PhotoItem>

    @Query("SELECT COALESCE(SUM(fileSizeBytes), 0) FROM photos WHERE isCached = 1")
    suspend fun getCachedTotalBytes(): Long

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM photos WHERE source = :source")
    suspend fun deleteBySource(source: String)

    @Query("DELETE FROM photos")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM photos")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM photos WHERE isCached = 1")
    suspend fun getCachedCount(): Int

    @Query("SELECT * FROM photos WHERE isCached = 0 AND source = 'CLOUD' ORDER BY albumProgressionIndex ASC LIMIT :limit")
    suspend fun getUncachedCloudPhotos(limit: Int = 20): List<PhotoItem>
}
