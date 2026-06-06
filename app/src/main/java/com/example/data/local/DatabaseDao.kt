package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DatabaseDao {
    @Query("SELECT * FROM connection_profiles ORDER BY createdAt DESC")
    fun getAllConnectionProfiles(): Flow<List<ConnectionProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ConnectionProfile): Long

    @Query("DELETE FROM connection_profiles WHERE id = :id")
    suspend fun deleteProfile(id: Int)

    @Query("SELECT * FROM query_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<QueryHistoryItem>>

    @Query("SELECT * FROM query_history WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getHistoryForProfile(profileId: Int): Flow<List<QueryHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: QueryHistoryItem): Long

    @Query("DELETE FROM query_history WHERE id = :id")
    suspend fun deleteHistoryItem(id: Int)

    @Query("DELETE FROM query_history WHERE profileId = :profileId")
    suspend fun clearHistoryForProfile(profileId: Int)

    @Update
    suspend fun updateHistoryItem(item: QueryHistoryItem)
}
