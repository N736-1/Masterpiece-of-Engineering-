package com.example.data.local

import kotlinx.coroutines.flow.Flow

class DatabaseRepository(private val dao: DatabaseDao) {
    val allProfiles: Flow<List<ConnectionProfile>> = dao.getAllConnectionProfiles()
    val allHistory: Flow<List<QueryHistoryItem>> = dao.getAllHistory()

    fun getHistoryForProfile(profileId: Int): Flow<List<QueryHistoryItem>> = dao.getHistoryForProfile(profileId)

    suspend fun insertProfile(profile: ConnectionProfile): Long = dao.insertProfile(profile)
    suspend fun deleteProfile(id: Int) = dao.deleteProfile(id)

    suspend fun insertHistoryItem(item: QueryHistoryItem): Long = dao.insertHistoryItem(item)
    suspend fun deleteHistoryItem(id: Int) = dao.deleteHistoryItem(id)
    suspend fun clearHistoryForProfile(profileId: Int) = dao.clearHistoryForProfile(profileId)
    suspend fun updateHistoryItem(item: QueryHistoryItem) = dao.updateHistoryItem(item)
}
