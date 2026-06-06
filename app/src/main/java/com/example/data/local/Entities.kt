package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "connection_profiles")
data class ConnectionProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val host: String,
    val port: Int = 5432,
    val database: String,
    val username: String,
    val password: String,
    val sslMode: String = "disable",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "query_history")
data class QueryHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int, // Refers to ConnectionProfile.id
    val sqlText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val label: String? = null,
    val executionTimeMs: Long = 0,
    val success: Boolean = true,
    val errorMessage: String? = null
) : Serializable
