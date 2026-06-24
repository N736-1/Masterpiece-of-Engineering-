package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.QueryResult
import com.example.data.TableColumnInfo
import com.example.data.TableSchemaInfo
import com.example.data.local.ConnectionProfile
import com.example.data.local.DatabaseRepository
import com.example.data.local.HeliumDatabase
import com.example.data.local.QueryHistoryItem
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ConnectionState
import com.example.viewmodel.HeliumDbViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = HeliumDatabase.getDatabase(this)
        val repository = DatabaseRepository(database.databaseDao())

        val viewModel: HeliumDbViewModel by viewModels {
            HeliumDbViewModel.Factory(application, repository)
        }

        setContent {
            MyApplicationTheme {
                HeliumDbApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HeliumDbApp(viewModel: HeliumDbViewModel) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0F1115), // Elegant Slate Carbon background
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            HeliumHeader(
                connectionState = connectionState,
                onDisconnect = { viewModel.disconnect() }
            )
        },
        bottomBar = {
            HeliumBottomNavigation(
                activeTab = activeTab,
                connectionState = connectionState,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "connections" -> ConnectionScreen(viewModel = viewModel)
                "console" -> ConsoleScreen(viewModel = viewModel)
                "tables" -> TablesScreen(viewModel = viewModel)
                "history" -> HistoryScreen(viewModel = viewModel)
                "gemini" -> GeminiScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HeliumHeader(
    connectionState: ConnectionState,
    onDisconnect: () -> Unit
) {
    Surface(
        color = Color(0xFF16191E),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Branding & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF9800)), // Helium Yellow/Orange
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "He",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Helium DB",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "PostgreSQL Query Hub",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }

            // Connection Status and Actions
            when (connectionState) {
                is ConnectionState.Disconnected -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Red)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Offline",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                is ConnectionState.Connecting -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF9800),
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Connecting",
                            color = Color(0xFFFFC107),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                is ConnectionState.Connected -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = connectionState.profile.name,
                                color = Color(0xFF4CAF50),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${connectionState.profile.database}@${connectionState.profile.host.take(15)}...",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = onDisconnect,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("disconnect_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Disconnect database",
                                tint = Color.LightGray
                            )
                        }
                    }
                }
                is ConnectionState.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Connection error",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Err Profile",
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeliumBottomNavigation(
    activeTab: String,
    connectionState: ConnectionState,
    onTabSelected: (String) -> Unit
) {
    val isConnected = connectionState is ConnectionState.Connected

    NavigationBar(
        containerColor = Color(0xFF16191E),
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = activeTab == "connections",
            onClick = { onTabSelected("connections") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = Color(0xFFFF9800),
                indicatorColor = Color(0xFFFF9800),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            label = { Text("Profile", fontSize = 11.sp) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Connections Tab") }
        )
        
        NavigationBarItem(
            selected = activeTab == "console",
            onClick = { if (isConnected) onTabSelected("console") },
            enabled = isConnected,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = Color(0xFFFF9800),
                indicatorColor = Color(0xFFFF9800),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            label = { Text("Console", fontSize = 11.sp) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Query Console") }
        )

        NavigationBarItem(
            selected = activeTab == "tables",
            onClick = { if (isConnected) onTabSelected("tables") },
            enabled = isConnected,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = Color(0xFFFF9800),
                indicatorColor = Color(0xFFFF9800),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            label = { Text("Tables", fontSize = 11.sp) },
            icon = { Icon(Icons.Default.Menu, contentDescription = "Tables Inspector") }
        )

        NavigationBarItem(
            selected = activeTab == "history",
            onClick = { onTabSelected("history") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = Color(0xFFFF9800),
                indicatorColor = Color(0xFFFF9800),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            label = { Text("Logs", fontSize = 11.sp) },
            icon = { Icon(Icons.Default.Refresh, contentDescription = "History Logs") }
        )

        NavigationBarItem(
            selected = activeTab == "gemini",
            onClick = { onTabSelected("gemini") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = Color(0xFFFF9800),
                indicatorColor = Color(0xFFFF9800),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            label = { Text("AI Tutor", fontSize = 11.sp) },
            icon = { Icon(Icons.Default.Favorite, contentDescription = "Gemini API Assistant") }
        )
    }
}

// --- Connection Screen ---
@Composable
fun ConnectionScreen(viewModel: HeliumDbViewModel) {
    val profiles by viewModel.connectionProfiles.collectAsStateWithLifecycle()
    val hostInput by viewModel.hostInput.collectAsStateWithLifecycle()
    val portInput by viewModel.portInput.collectAsStateWithLifecycle()
    val dbInput by viewModel.dbInput.collectAsStateWithLifecycle()
    val userInput by viewModel.userInput.collectAsStateWithLifecycle()
    val passInput by viewModel.passInput.collectAsStateWithLifecycle()
    val sslInput by viewModel.sslInput.collectAsStateWithLifecycle()
    val nameInput by viewModel.profileNameInput.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    var showPass by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Warning/Notice if Error occurs
        val state = connectionState
        if (state is ConnectionState.Error) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF351212)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Alert", tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connection Error", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Setup Form Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "New Connection Profile",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tip: You can paste a full PostgreSQL connection URI (e.g. postgresql://...) directly into the Database Host field to automatically parse and fill out this entire form.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { viewModel.profileNameInput.value = it },
                        label = { Text("Profile Name") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF9800),
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = hostInput,
                        onValueChange = { viewModel.updateHost(it) },
                        label = { Text("Database Host") },
                        modifier = Modifier.fillMaxWidth().testTag("host_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF9800),
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { viewModel.portInput.value = it },
                            label = { Text("Port") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("port_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF9800),
                                unfocusedBorderColor = Color.DarkGray
                            )
                        )
                        OutlinedTextField(
                            value = dbInput,
                            onValueChange = { viewModel.dbInput.value = it },
                            label = { Text("Database Name") },
                            modifier = Modifier.weight(2f).testTag("db_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF9800),
                                unfocusedBorderColor = Color.DarkGray
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { viewModel.userInput.value = it },
                        label = { Text("User") },
                        modifier = Modifier.fillMaxWidth().testTag("user_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF9800),
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = passInput,
                        onValueChange = { viewModel.passInput.value = it },
                        label = { Text("Password") },
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(
                                    imageVector = if (showPass) Icons.Default.Info else Icons.Default.Search,
                                    contentDescription = "Toggle password visibility",
                                    tint = Color.LightGray
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("password_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF9800),
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("SSL Mode", color = Color.Gray, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("disable", "require", "prefer").forEach { mode ->
                            Button(
                                onClick = { viewModel.sslInput.value = mode },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (sslInput == mode) Color(0xFFFF9800) else Color(0xFF23272F)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = mode,
                                    color = if (sslInput == mode) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveProfile()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C323D)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Profile", color = Color.White)
                        }

                        Button(
                            onClick = {
                                val current = ConnectionProfile(
                                    name = nameInput,
                                    host = hostInput.trim(),
                                    port = portInput.toIntOrNull() ?: 5432,
                                    database = dbInput.trim(),
                                    username = userInput.trim(),
                                    password = passInput,
                                    sslMode = sslInput
                                )
                                viewModel.connectToProfile(current)
                            },
                            enabled = connectionState !is ConnectionState.Connecting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("connect_btn")
                        ) {
                            Text("Connect", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Saved Connections Header
        item {
            Text(
                text = "Saved Server Connections",
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // Profiles list
        if (profiles.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No saved connections. Fill form to add.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(profiles) { profile ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2026)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).clickable {
                            viewModel.setProfileForm(profile)
                        }) {
                            Text(
                                text = profile.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${profile.username}@${profile.host}:${profile.port}/${profile.database}",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "SSL: ${profile.sslMode}",
                                color = Color(0xFFFF9800),
                                fontSize = 10.sp
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.connectToProfile(profile) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Connect profile", tint = Color(0xFF4CAF50))
                            }
                            IconButton(onClick = { viewModel.deleteProfile(profile.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Profile", tint = Color(0xFFFF5252))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Query Console Screen ---
@Composable
fun ConsoleScreen(viewModel: HeliumDbViewModel) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val consoleQueryInput by viewModel.consoleQueryInput.collectAsStateWithLifecycle()
    val queryResult by viewModel.queryResult.collectAsStateWithLifecycle()
    val isQueryRunning by viewModel.isQueryRunning.collectAsStateWithLifecycle()

    val copyManager = LocalClipboardManager.current

    val state = connectionState
    if (state !is ConnectionState.Connected) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select & connect to a database profile first.", color = Color.Gray, textAlign = TextAlign.Center)
            }
        }
        return
    }

    val activeProfile = state.profile

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // SQL code editor styling area
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SQL Statement Console", color = Color.Gray, fontSize = 11.sp)
                    Row {
                        TextButton(onClick = { viewModel.updateQueryInput("") }) {
                            Text("Clear", color = Color(0xFFFF5252), fontSize = 12.sp)
                        }
                    }
                }
                
                TextField(
                    value = consoleQueryInput,
                    onValueChange = { viewModel.updateQueryInput(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("sql_input_field"),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color.White
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0F1115),
                        unfocusedContainerColor = Color(0xFF0F1115),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                // Quick templates helpers for direct typing joy on mobile
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "SELECT * FROM",
                        "LIMIT 10;",
                        "ORDER BY",
                        "WHERE",
                        "INSERT INTO",
                        "CREATE TABLE",
                        "GROUP BY",
                        "COUNT(*)"
                    ).forEach { keyword ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    val currentText = consoleQueryInput
                                    viewModel.updateQueryInput("$currentText $keyword")
                                },
                            color = Color(0xFF2C323D)
                        ) {
                            Text(
                                text = keyword,
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Executing Query floating row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DB: ${activeProfile.database}",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = activeProfile.host,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = { viewModel.runConsoleQuery(activeProfile) },
                enabled = !isQueryRunning,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("run_query_btn")
            ) {
                if (isQueryRunning) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Execute SQL", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Query results block
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Text(
                    text = "Execution Output Results",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F1115))
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (queryResult == null) {
                        Text("Query outputs will render here.", color = Color.DarkGray, fontSize = 12.sp)
                    } else {
                        val res = queryResult!!
                        if (res.error != null) {
                            SelectionContainer {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "Query Error Logged:",
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = res.error,
                                        color = Color(0xFFFF5252),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Info bar stats
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF1B1E24))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = res.infoMessage,
                                        color = Color(0xFF4CAF50),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${res.executionTimeMs} ms",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }
                                
                                buildHorizontalVerticalResultsGrid(res)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom Grid UI implementation to scroll columns and rows gracefully
@Composable
fun buildHorizontalVerticalResultsGrid(result: QueryResult) {
    if (result.columns.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Operation successful. No relational set returned.", color = Color.LightGray, fontSize = 12.sp)
        }
        return
    }

    // Scroll state for columns
    val horizontalScrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Render tabular matrix
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxHeight(),
            ) {
                // Headers Row
                item {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF242933))
                            .border(1.dp, Color.DarkGray)
                    ) {
                        result.columns.forEach { colName ->
                            Box(
                                modifier = Modifier
                                    .width(140.dp)
                                    .border(0.5.dp, Color.DarkGray)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = colName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Values List Rows
                items(result.rows) { rowValues ->
                    Row(
                        modifier = Modifier
                            .border(0.2.dp, Color.DarkGray)
                            .background(Color(0xFF0F1115))
                    ) {
                        rowValues.forEach { cellValue ->
                            Box(
                                modifier = Modifier
                                    .width(140.dp)
                                    .border(0.2.dp, Color(0xFF1E222B))
                                    .padding(8.dp)
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = cellValue,
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Databases Table Schema Explorer Screen ---
@Composable
fun TablesScreen(viewModel: HeliumDbViewModel) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val schemas by viewModel.schemas.collectAsStateWithLifecycle()
    val activeSchema by viewModel.activeSchema.collectAsStateWithLifecycle()
    val tables by viewModel.tables.collectAsStateWithLifecycle()
    val selectedTable by viewModel.selectedTable.collectAsStateWithLifecycle()
    val selectedColumns by viewModel.selectedTableColumns.collectAsStateWithLifecycle()

    var showSchemaDropdown by remember { mutableStateOf(false) }

    val state = connectionState
    if (state !is ConnectionState.Connected) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Connect to a database to browse schemas & tables.", color = Color.Gray, textAlign = TextAlign.Center)
            }
        }
        return
    }

    val activeProfile = state.profile

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column list of tables
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
        ) {
            // Schema Selector Bar
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showSchemaDropdown = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16191E)),
                    modifier = Modifier.fillMaxWidth().testTag("schema_dropdown_trigger")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Schema: $activeSchema", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.LightGray)
                    }
                }
                
                DropdownMenu(
                    expanded = showSchemaDropdown,
                    onDismissRequest = { showSchemaDropdown = false },
                    modifier = Modifier.background(Color(0xFF16191E))
                ) {
                    schemas.forEach { schema ->
                        DropdownMenuItem(
                            text = { Text(schema, color = Color.White) },
                            onClick = {
                                viewModel.changeSchema(activeProfile, schema)
                                showSchemaDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tables Explorer list
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Tables ($activeSchema)",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (tables.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No tables discovered.", color = Color.DarkGray, fontSize = 11.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(tables) { tableInfo ->
                                val isSelected = selectedTable?.tableName == tableInfo.tableName
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFFF9800) else Color(0xFF1E222B))
                                        .clickable {
                                            viewModel.inspectTable(activeProfile, tableInfo)
                                        }
                                        .padding(12.dp)
                                        .testTag("table_row_${tableInfo.tableName}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.Black else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = tableInfo.tableName,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Right side details schema inspector
        Column(
            modifier = Modifier
                .weight(1.8f)
                .fillMaxHeight()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                if (selectedTable == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a table to inspect schema details.", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    val table = selectedTable!!
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = table.tableName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Schema: ${table.schemaName}",
                                    color = Color(0xFFFF9800),
                                    fontSize = 11.sp
                                )
                            }
                            Button(
                                onClick = {
                                    // Query button to load limits list in Console
                                    viewModel.runConsoleQuery(activeProfile)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C323D)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Auto Query", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Structural Schema Definitions",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedColumns) { col ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F1115))
                                        .border(0.5.dp, Color.DarkGray, RoundedCornerShape(6.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = col.columnName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                if (col.isNullable == "NO") {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "★",
                                                        color = Color(0xFFFF9800),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Default: ${col.defaultValue ?: "NULL"}",
                                                color = Color.DarkGray,
                                                fontSize = 9.sp
                                            )
                                        }
                                        
                                        Surface(
                                            color = Color(0xFF1E242F),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = col.dataType,
                                                color = Color(0xFFFF9800),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- History & Logs Library Screen ---
@Composable
fun HistoryScreen(viewModel: HeliumDbViewModel) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val history by viewModel.queryHistory.collectAsStateWithLifecycle()
    val copyManager = LocalClipboardManager.current

    val state = connectionState
    val activeProfile = if (state is ConnectionState.Connected) state.profile else null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "History & SQL Execution Logs",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        if (history.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No query execution logs captured.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(history) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (item.success) Color(0xFF4CAF50) else Color.Red)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (item.success) "Success" else "Error",
                                    color = if (item.success) Color(0xFF4CAF50) else Color.Red,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${item.executionTimeMs} ms",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }

                            Row {
                                IconButton(onClick = { viewModel.toggleFavoriteHistory(item) }) {
                                    Icon(
                                        imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.Info,
                                        contentDescription = "Favorite query option",
                                        tint = if (item.isFavorite) Color(0xFFFF9800) else Color.LightGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteHistoryItem(item.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete from history",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        SelectionContainer {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F1115))
                                    .clip(RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = item.sqlText,
                                    color = Color.LightGray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (!item.success && item.errorMessage != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Details: ${item.errorMessage}",
                                color = Color(0xFFFF5252),
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { copyManager.setText(AnnotatedString(item.sqlText)) }) {
                                Text("Copy", color = Color.White, fontSize = 11.sp)
                            }
                            if (activeProfile != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(
                                    onClick = { viewModel.executeHistorySql(activeProfile, item.sqlText) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF23272F))
                                ) {
                                    Text("Load", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Gemini AI Assistant Screen ---
@Composable
fun GeminiScreen(viewModel: HeliumDbViewModel) {
    val geminiPromptInput by viewModel.geminiPromptInput.collectAsStateWithLifecycle()
    val geminiOutput by viewModel.geminiOutput.collectAsStateWithLifecycle()
    val isGeminiGenerating by viewModel.isGeminiGenerating.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Gemini SQL Assistant",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = "Describe your database goal in natural language to construct custom PostgreSQL queries.",
            color = Color.Gray,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Prompt input Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = geminiPromptInput,
                    onValueChange = { viewModel.updateGeminiPrompt(it) },
                    placeholder = { Text("e.g., 'Find the average scores from game_history grouped by player_id'", color = Color.DarkGray) },
                    modifier = Modifier.fillMaxWidth().testTag("gemini_prompt"),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF9800),
                        unfocusedBorderColor = Color.DarkGray
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Button(
                        onClick = { viewModel.getGeminiAiSuggestion() },
                        enabled = !isGeminiGenerating,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        if (isGeminiGenerating) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Generate Query", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI suggestion text output pane
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Text(
                    text = "AI Suggestion Result:",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0F1115))
                        .clip(RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer {
                        Text(
                            text = geminiOutput.ifEmpty { "AI suggestions will display here." },
                            color = if (geminiOutput.isEmpty()) Color.DarkGray else Color.LightGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }

                if (geminiOutput.isNotEmpty() && !geminiOutput.startsWith("Thinking") && !geminiOutput.contains("Error")) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { clipboardManager.setText(AnnotatedString(geminiOutput)) }
                        ) {
                            Text("Copy Code", color = Color.White)
                        }
                        val state = connectionState
                        if (state is ConnectionState.Connected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.loadAiSqlIntoConsole(geminiOutput) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C323D))
                            ) {
                                Text("Load in Console", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
