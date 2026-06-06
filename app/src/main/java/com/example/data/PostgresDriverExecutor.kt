package com.example.data

import com.example.data.local.ConnectionProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.Statement
import java.util.Properties

data class QueryResult(
    val columns: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    val rowsAffected: Int = -1,
    val infoMessage: String = "",
    val executionTimeMs: Long = 0,
    val error: String? = null
)

data class TableColumnInfo(
    val columnName: String,
    val dataType: String,
    val isNullable: String,
    val defaultValue: String?
)

data class TableSchemaInfo(
    val tableName: String,
    val schemaName: String
)

object PostgresDriverExecutor {

    init {
        try {
            Class.forName("org.postgresql.Driver")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getConnectionUrl(profile: ConnectionProfile): String {
        return "jdbc:postgresql://${profile.host}:${profile.port}/${profile.database}"
    }

    private fun getConnectionProperties(profile: ConnectionProfile): Properties {
        val props = Properties()
        props.setProperty("user", profile.username)
        props.setProperty("password", profile.password)
        props.setProperty("sslmode", profile.sslMode)
        props.setProperty("connectTimeout", "8") // seconds
        props.setProperty("socketTimeout", "20") // seconds
        return props
    }

    suspend fun testConnection(profile: ConnectionProfile): Boolean = withContext(Dispatchers.IO) {
        var conn: Connection? = null
        try {
            val url = getConnectionUrl(profile)
            val props = getConnectionProperties(profile)
            conn = DriverManager.getConnection(url, props)
            val valid = conn.isValid(5)
            valid
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                conn?.close()
            } catch (ex: Exception) {
                // ignore
            }
        }
    }

    suspend fun listSchemas(profile: ConnectionProfile): List<String> = withContext(Dispatchers.IO) {
        val schemas = mutableListOf<String>()
        var conn: Connection? = null
        try {
            val url = getConnectionUrl(profile)
            val props = getConnectionProperties(profile)
            conn = DriverManager.getConnection(url, props)
            val selectSchemas = """
                SELECT schema_name 
                FROM information_schema.schemata 
                WHERE schema_name NOT IN ('pg_catalog', 'information_schema') 
                ORDER BY schema_name
            """.trimIndent()
            conn.createStatement().use { stmt ->
                stmt.executeQuery(selectSchemas).use { rs ->
                    while (rs.next()) {
                        schemas.add(rs.getString("schema_name"))
                    }
                }
            }
            if (schemas.isEmpty()) {
                schemas.add("public")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (schemas.isEmpty()) schemas.add("public")
        } finally {
            conn?.close()
        }
        schemas
    }

    suspend fun listTables(profile: ConnectionProfile, schema: String = "public"): List<TableSchemaInfo> = withContext(Dispatchers.IO) {
        val tables = mutableListOf<TableSchemaInfo>()
        var conn: Connection? = null
        try {
            val url = getConnectionUrl(profile)
            val props = getConnectionProperties(profile)
            conn = DriverManager.getConnection(url, props)
            val selectTables = """
                SELECT table_name, table_schema
                FROM information_schema.tables 
                WHERE table_schema = ? AND table_type = 'BASE TABLE'
                ORDER BY table_name
            """.trimIndent()
            conn.prepareStatement(selectTables).use { stmt ->
                stmt.setString(1, schema)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        tables.add(
                            TableSchemaInfo(
                                tableName = rs.getString("table_name"),
                                schemaName = rs.getString("table_schema")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn?.close()
        }
        tables
    }

    suspend fun getTableColumns(profile: ConnectionProfile, schema: String, tableName: String): List<TableColumnInfo> = withContext(Dispatchers.IO) {
        val columnsList = mutableListOf<TableColumnInfo>()
        var conn: Connection? = null
        try {
            val url = getConnectionUrl(profile)
            val props = getConnectionProperties(profile)
            conn = DriverManager.getConnection(url, props)
            val selectCols = """
                SELECT column_name, data_type, is_nullable, column_default 
                FROM information_schema.columns 
                WHERE table_schema = ? AND table_name = ? 
                ORDER BY ordinal_position
            """.trimIndent()
            conn.prepareStatement(selectCols).use { stmt ->
                stmt.setString(1, schema)
                stmt.setString(2, tableName)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        columnsList.add(
                            TableColumnInfo(
                                columnName = rs.getString("column_name") ?: "",
                                dataType = rs.getString("data_type") ?: "",
                                isNullable = rs.getString("is_nullable") ?: "YES",
                                defaultValue = rs.getString("column_default")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn?.close()
        }
        columnsList
    }

    suspend fun executeQuery(profile: ConnectionProfile, sql: String): QueryResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var conn: Connection? = null
        var stmt: Statement? = null
        try {
            val url = getConnectionUrl(profile)
            val props = getConnectionProperties(profile)
            conn = DriverManager.getConnection(url, props)
            stmt = conn.createStatement()
            
            val hasResultSet = stmt.execute(sql)
            val executionTimeMs = System.currentTimeMillis() - startTime

            if (hasResultSet) {
                stmt.resultSet.use { rs ->
                    val meta = rs.metaData
                    val colCount = meta.columnCount
                    val columns = ArrayList<String>(colCount)
                    for (i in 1..colCount) {
                        columns.add(meta.getColumnLabel(i) ?: meta.getColumnName(i) ?: "Column_$i")
                    }

                    val rows = ArrayList<List<String>>()
                    var rowCount = 0
                    while (rs.next() && rowCount < 500) {
                        val row = ArrayList<String>(colCount)
                        for (i in 1..colCount) {
                            val value = rs.getObject(i)
                            row.add(value?.toString() ?: "NULL")
                        }
                        rows.add(row)
                        rowCount++
                    }
                    
                    val info = if (rowCount >= 500) {
                        "Successful execution. Showing first 500 rows."
                    } else {
                        "Successful execution. $rowCount row(s) returned."
                    }

                    QueryResult(
                        columns = columns,
                        rows = rows,
                        rowsAffected = rowCount,
                        infoMessage = info,
                        executionTimeMs = executionTimeMs
                    )
                }
            } else {
                val updateCount = stmt.updateCount
                QueryResult(
                    rowsAffected = updateCount,
                    infoMessage = "Successful execution. Rows affected: $updateCount",
                    executionTimeMs = executionTimeMs
                )
            }
        } catch (e: Exception) {
            val executionTimeMs = System.currentTimeMillis() - startTime
            QueryResult(
                error = e.message ?: e.toString(),
                executionTimeMs = executionTimeMs
            )
        } finally {
            try {
                stmt?.close()
            } catch (ex: Exception) { /* ignored */ }
            try {
                conn?.close()
            } catch (ex: Exception) { /* ignored */ }
        }
    }
}
