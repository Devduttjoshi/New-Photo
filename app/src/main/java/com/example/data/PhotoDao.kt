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
    @Query("SELECT * FROM photos ORDER BY dateAdded DESC")
    fun getAllPhotos(): Flow<List<PhotoItem>>

    @Query("SELECT * FROM photos WHERE source = :source ORDER BY dateAdded DESC")
    fun getPhotosBySource(source: String): Flow<List<PhotoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<PhotoItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoItem): Long

    @Update
    suspend fun update(photo: PhotoItem)

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

    @Query("SELECT * FROM photos WHERE isCached = 0 AND source = 'CLOUD'")
    suspend fun getUncachedCloudPhotos(): List<PhotoItem>
}
