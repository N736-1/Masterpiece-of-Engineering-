package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GeminiService
import com.example.data.PostgresDriverExecutor
import com.example.data.QueryResult
import com.example.data.TableColumnInfo
import com.example.data.TableSchemaInfo
import com.example.data.local.ConnectionProfile
import com.example.data.local.DatabaseRepository
import com.example.data.local.QueryHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ConnectionState {
    object Disconnected : ConnectionState
    object Connecting : ConnectionState
    data class Connected(val profile: ConnectionProfile) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

class HeliumDbViewModel(
    application: Application,
    private val repository: DatabaseRepository
) : AndroidViewModel(application) {

    val connectionProfiles: StateFlow<List<ConnectionProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queryHistory: StateFlow<List<QueryHistoryItem>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _hostInput = MutableStateFlow("postgresqlpostgrespasswordheliumheliumdbsslmod--fazalnaeem47870.replit.app")
    val hostInput = _hostInput.asStateFlow()

    val portInput = MutableStateFlow("5432")
    val dbInput = MutableStateFlow("heliumdb")
    val userInput = MutableStateFlow("postgres")
    val passInput = MutableStateFlow("password")
    val sslInput = MutableStateFlow("disable")
    val profileNameInput = MutableStateFlow("Helium Database")

    private val _activeTab = MutableStateFlow("connections")
    val activeTab = _activeTab.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private val _activeSchema = MutableStateFlow("public")
    val activeSchema = _activeSchema.asStateFlow()

    private val _schemas = MutableStateFlow<List<String>>(listOf("public"))
    val schemas = _schemas.asStateFlow()

    private val _tables = MutableStateFlow<List<TableSchemaInfo>>(emptyList())
    val tables = _tables.asStateFlow()

    private val _selectedTable = MutableStateFlow<TableSchemaInfo?>(null)
    val selectedTable = _selectedTable.asStateFlow()

    private val _selectedTableColumns = MutableStateFlow<List<TableColumnInfo>>(emptyList())
    val selectedTableColumns = _selectedTableColumns.asStateFlow()

    private val _consoleQueryInput = MutableStateFlow("SELECT * FROM test_table LIMIT 10;")
    val consoleQueryInput = _consoleQueryInput.asStateFlow()

    private val _queryResult = MutableStateFlow<QueryResult?>(null)
    val queryResult = _queryResult.asStateFlow()

    private val _isQueryRunning = MutableStateFlow(false)
    val isQueryRunning = _isQueryRunning.asStateFlow()

    private val _geminiPromptInput = MutableStateFlow("")
    val geminiPromptInput = _geminiPromptInput.asStateFlow()

    private val _geminiOutput = MutableStateFlow("")
    val geminiOutput = _geminiOutput.asStateFlow()

    private val _isGeminiGenerating = MutableStateFlow(false)
    val isGeminiGenerating = _isGeminiGenerating.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allProfiles.collect { list ->
                if (list.isEmpty()) {
                    repository.insertProfile(
                        ConnectionProfile(
                            name = "Helium Cloud (Quickstart)",
                            host = "postgresqlpostgrespasswordheliumheliumdbsslmod--fazalnaeem47870.replit.app",
                            port = 5432,
                            database = "heliumdb",
                            username = "postgres",
                            password = "password",
                            sslMode = "disable"
                        )
                    )
                }
            }
        }
    }

    fun selectTab(tab: String) {
        _activeTab.value = tab
    }

    fun updateHost(value: String) { _hostInput.value = value }
    fun updateQueryInput(value: String) { _consoleQueryInput.value = value }
    fun updateGeminiPrompt(value: String) { _geminiPromptInput.value = value }

    fun setProfileForm(profile: ConnectionProfile) {
        profileNameInput.value = profile.name
        _hostInput.value = profile.host
        portInput.value = profile.port.toString()
        dbInput.value = profile.database
        userInput.value = profile.username
        passInput.value = profile.password
        sslInput.value = profile.sslMode
    }

    fun saveProfile() {
        viewModelScope.launch {
            val portVal = portInput.value.toIntOrNull() ?: 5432
            val profile = ConnectionProfile(
                name = profileNameInput.value.ifBlank { "Helium Connection" },
                host = _hostInput.value.trim(),
                port = portVal,
                database = dbInput.value.trim(),
                username = userInput.value.trim(),
                password = passInput.value,
                sslMode = sslInput.value
            )
            repository.insertProfile(profile)
        }
    }

    fun deleteProfile(profileId: Int) {
        viewModelScope.launch {
            repository.deleteProfile(profileId)
            val active = _connectionState.value
            if (active is ConnectionState.Connected && active.profile.id == profileId) {
                disconnect()
            }
        }
    }

    fun connectToProfile(profile: ConnectionProfile) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting
            val success = PostgresDriverExecutor.testConnection(profile)
            if (success) {
                _connectionState.value = ConnectionState.Connected(profile)
                loadDatabaseMetadata(profile)
                selectTab("console")
            } else {
                _connectionState.value = ConnectionState.Error("Failed to connect to ${profile.host}. Verify network access, credentials, or remote connection parameters.")
            }
        }
    }

    fun disconnect() {
        _connectionState.value = ConnectionState.Disconnected
        _tables.value = emptyList()
        _selectedTable.value = null
        _selectedTableColumns.value = emptyList()
        _queryResult.value = null
        selectTab("connections")
    }

    fun changeSchema(profile: ConnectionProfile, schemaName: String) {
        _activeSchema.value = schemaName
        viewModelScope.launch {
            _tables.value = PostgresDriverExecutor.listTables(profile, schemaName)
        }
    }

    private suspend fun loadDatabaseMetadata(profile: ConnectionProfile) {
        val schemasList = PostgresDriverExecutor.listSchemas(profile)
        _schemas.value = schemasList
        val defaultSchema = if (schemasList.contains("public")) "public" else schemasList.firstOrNull() ?: "public"
        _activeSchema.value = defaultSchema
        _tables.value = PostgresDriverExecutor.listTables(profile, defaultSchema)
    }

    fun inspectTable(profile: ConnectionProfile, tableInfo: TableSchemaInfo) {
        _selectedTable.value = tableInfo
        viewModelScope.launch {
            _selectedTableColumns.value = PostgresDriverExecutor.getTableColumns(
                profile,
                tableInfo.schemaName,
                tableInfo.tableName
            )
            _consoleQueryInput.value = "SELECT * FROM \"${tableInfo.schemaName}\".\"${tableInfo.tableName}\" LIMIT 20;"
            selectTab("tables")
        }
    }

    fun runConsoleQuery(profile: ConnectionProfile) {
        val sql = _consoleQueryInput.value.trim()
        if (sql.isEmpty()) return

        viewModelScope.launch {
            _isQueryRunning.value = true
            val start = System.currentTimeMillis()
            val result = PostgresDriverExecutor.executeQuery(profile, sql)
            val elapsed = System.currentTimeMillis() - start

            _queryResult.value = result

            repository.insertHistoryItem(
                QueryHistoryItem(
                    profileId = profile.id,
                    sqlText = sql,
                    timestamp = System.currentTimeMillis(),
                    isFavorite = false,
                    executionTimeMs = result.executionTimeMs,
                    success = result.error == null,
                    errorMessage = result.error
                )
            )
            
            if (sql.contains("create", ignoreCase = true) || sql.contains("drop", ignoreCase = true) || sql.contains("alter", ignoreCase = true)) {
                loadDatabaseMetadata(profile)
            }
            _isQueryRunning.value = false
        }
    }

    fun executeHistorySql(profile: ConnectionProfile, sql: String) {
        _consoleQueryInput.value = sql
        selectTab("console")
        runConsoleQuery(profile)
    }

    fun toggleFavoriteHistory(item: QueryHistoryItem) {
        viewModelScope.launch {
            repository.updateHistoryItem(item.copy(isFavorite = !item.isFavorite))
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun getGeminiAiSuggestion() {
        val prompt = _geminiPromptInput.value.trim()
        if (prompt.isEmpty()) return

        val activeConn = _connectionState.value
        val schemaDetails = StringBuilder()
        if (activeConn is ConnectionState.Connected) {
            val sTbls = _tables.value
            if (sTbls.isNotEmpty()) {
                schemaDetails.append("The active database schemas contain the following tables:\n")
                sTbls.take(15).forEach { table ->
                    schemaDetails.append("- Table: ${table.tableName} (schema: ${table.schemaName})\n")
                }
                val currentTbl = _selectedTable.value
                val currentCols = _selectedTableColumns.value
                if (currentTbl != null && currentCols.isNotEmpty()) {
                    schemaDetails.append("Inspected Table Columns for table ${currentTbl.tableName}:\n")
                    currentCols.forEach { col ->
                        schemaDetails.append("  - Column Name: ${col.columnName}, Data Type: ${col.dataType}, Nullable: ${col.isNullable}\n")
                    }
                }
            } else {
                schemaDetails.append("No active database tables exist yet.")
            }
        } else {
            schemaDetails.append("No active database connection. Offer standard generic PostgreSQL instructions.")
        }

        viewModelScope.launch {
            _isGeminiGenerating.value = true
            _geminiOutput.value = "Generating optimal SQL query..."
            val output = GeminiService.generateSql(prompt, schemaDetails.toString())
            _geminiOutput.value = output
            _isGeminiGenerating.value = false
        }
    }

    fun loadAiSqlIntoConsole(sql: String) {
        var cleaned = sql.trim()
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringBeforeLast("```")
            if (cleaned.startsWith("```sql")) {
                cleaned = cleaned.removePrefix("```sql")
            } else {
                cleaned = cleaned.removePrefix("```")
            }
        }
        _consoleQueryInput.value = cleaned.trim()
        selectTab("console")
    }

    class Factory(
        private val application: Application,
        private val repository: DatabaseRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HeliumDbViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HeliumDbViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
