package com.manjul.genai.videogenerator.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.manjul.genai.videogenerator.data.model.VideoJobStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoJobDao {
    @Query("SELECT * FROM video_jobs WHERE id = :id")
    suspend fun getJobById(id: String): VideoJobEntity?

    @Query("SELECT * FROM video_jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<VideoJobEntity>>

    @Query("SELECT * FROM video_jobs WHERE status = :status ORDER BY createdAt DESC")
    fun getJobsByStatus(status: VideoJobStatus): Flow<List<VideoJobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: VideoJobEntity)

    @Update
    suspend fun updateJob(job: VideoJobEntity)

    @Query("UPDATE video_jobs SET localFilePath = :filePath WHERE id = :id")
    suspend fun updateLocalFilePath(id: String, filePath: String?)

    @Query("UPDATE video_jobs SET thumbnailPath = :thumbnailPath WHERE id = :id")
    suspend fun updateThumbnailPath(id: String, thumbnailPath: String?)

    @Query("UPDATE video_jobs SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: VideoJobStatus)

    @Query("DELETE FROM video_jobs WHERE id = :id")
    suspend fun deleteJob(id: String)
}

