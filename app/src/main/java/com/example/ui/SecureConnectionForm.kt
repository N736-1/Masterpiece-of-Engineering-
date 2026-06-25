package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A highly secure database connection form component designed to gather connection parameters
 * with maximum visual clarity, robust live validation, and secure password mask handling.
 * Employs a visual SSL Mode Switch and an informative database security health-card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureConnectionForm(
    profileName: String,
    onProfileNameChange: (String) -> Unit,
    host: String,
    onHostChange: (String) -> Unit,
    port: String,
    onPortChange: (String) -> Unit,
    database: String,
    onDatabaseChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    sslMode: String,
    onSslModeChange: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onConnect: () -> Unit,
    isConnecting: Boolean,
    modifier: Modifier = Modifier
) {
    var showPassword by remember { mutableStateOf(false) }

    // Live validation states
    val isProfileNameError = profileName.isBlank()
    val isHostError = host.isBlank()
    val isPortError = port.toIntOrNull() == null || port.toInt() !in 1..65535
    val isDatabaseError = database.isBlank()
    val isUsernameError = username.isBlank()

    val isFormValid = !isProfileNameError && !isHostError && !isPortError && !isDatabaseError && !isUsernameError

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2C323D), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with Security Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1F242E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security Form Indicator",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Secure Connection Profile",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Configure encrypted credentials & database endpoints",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile Name Input
            OutlinedTextField(
                value = profileName,
                onValueChange = onProfileNameChange,
                label = { Text("Profile Name") },
                placeholder = { Text("e.g. Production Replica, Helium DB Cloud") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isProfileNameError) Color(0xFFFF5252) else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                },
                isError = isProfileNameError,
                supportingText = {
                    if (isProfileNameError) {
                        Text("Profile name cannot be blank", color = Color(0xFFFF5252))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_name_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFF9800),
                    unfocusedBorderColor = Color(0xFF2C323D),
                    errorBorderColor = Color(0xFFFF5252),
                    focusedContainerColor = Color(0xFF0F1115),
                    unfocusedContainerColor = Color(0xFF0F1115)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Host Endpoint Input
            OutlinedTextField(
                value = host,
                onValueChange = onHostChange,
                label = { Text("Database Host / Endpoint") },
                placeholder = { Text("e.g. pg.helium-db.internal or localhost") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = if (isHostError) Color(0xFFFF5252) else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                },
                isError = isHostError,
                supportingText = {
                    if (isHostError) {
                        Text("Database host address is required", color = Color(0xFFFF5252))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("host_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFF9800),
                    unfocusedBorderColor = Color(0xFF2C323D),
                    errorBorderColor = Color(0xFFFF5252),
                    focusedContainerColor = Color(0xFF0F1115),
                    unfocusedContainerColor = Color(0xFF0F1115)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Port & Database Name Twin Inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = port,
                    onValueChange = onPortChange,
                    label = { Text("Port") },
                    placeholder = { Text("5432") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = if (isPortError) Color(0xFFFF5252) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    isError = isPortError,
                    supportingText = {
                        if (isPortError) {
                            Text("Port 1-65535", color = Color(0xFFFF5252), fontSize = 10.sp)
                        }
                    },
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("port_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF9800),
                        unfocusedBorderColor = Color(0xFF2C323D),
                        errorBorderColor = Color(0xFFFF5252),
                        focusedContainerColor = Color(0xFF0F1115),
                        unfocusedContainerColor = Color(0xFF0F1115)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = database,
                    onValueChange = onDatabaseChange,
                    label = { Text("Database Name") },
                    placeholder = { Text("e.g. postgres") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = null,
                            tint = if (isDatabaseError) Color(0xFFFF5252) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    isError = isDatabaseError,
                    supportingText = {
                        if (isDatabaseError) {
                            Text("Database is required", color = Color(0xFFFF5252), fontSize = 10.sp)
                        }
                    },
                    modifier = Modifier
                        .weight(2f)
                        .testTag("db_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF9800),
                        unfocusedBorderColor = Color(0xFF2C323D),
                        errorBorderColor = Color(0xFFFF5252),
                        focusedContainerColor = Color(0xFF0F1115),
                        unfocusedContainerColor = Color(0xFF0F1115)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Username Input
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("Username") },
                placeholder = { Text("e.g. postgres, admin") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isUsernameError) Color(0xFFFF5252) else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                },
                isError = isUsernameError,
                supportingText = {
                    if (isUsernameError) {
                        Text("Username cannot be empty", color = Color(0xFFFF5252))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFF9800),
                    unfocusedBorderColor = Color(0xFF2C323D),
                    errorBorderColor = Color(0xFFFF5252),
                    focusedContainerColor = Color(0xFF0F1115),
                    unfocusedContainerColor = Color(0xFF0F1115)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Password Input (Secure Visual Transformation)
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                placeholder = { Text("Enter account password") },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Info else Icons.Default.Search, // Eye toggle metaphors safely in Default set
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = if (showPassword) Color(0xFFFF9800) else Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("password_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFF9800),
                    unfocusedBorderColor = Color(0xFF2C323D),
                    focusedContainerColor = Color(0xFF0F1115),
                    unfocusedContainerColor = Color(0xFF0F1115)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- SSL Toggle & Advanced Custom Control ---
            val isSslEnabled = sslMode != "disable"

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101318)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1C222B), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isSslEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isSslEnabled) Color(0xFF4CAF50) else Color(0xFFE91E63),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Enable SSL Mode",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Encrypt credentials & query payloads",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Toggle Switch for SSL Mode
                        Switch(
                            checked = isSslEnabled,
                            onCheckedChange = { checked ->
                                onSslModeChange(if (checked) "require" else "disable")
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(0xFFFF9800),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFF1E222B)
                            )
                        )
                    }

                    // Expandable Details based on whether SSL is active
                    AnimatedVisibility(
                        visible = isSslEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Select SSL Strictness Mode",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("require", "prefer", "allow").forEach { mode ->
                                    val isSelected = sslMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFFFF9800) else Color(0xFF23272F))
                                            .clickable { onSslModeChange(mode) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.uppercase(),
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security Status Health-Card Indicator
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSslEnabled) Color(0xFF112217) else Color(0xFF2E1B1E)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isSslEnabled) Color(0xFF1B5E20) else Color(0xFF881E2F),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSslEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Security Status Badge",
                        tint = if (isSslEnabled) Color(0xFF4CAF50) else Color(0xFFFF5252),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isSslEnabled) "HIGH SECURITY CONNECTION ACTIVE" else "SECURITY LEVEL WARNING",
                            color = if (isSslEnabled) Color(0xFF81C784) else Color(0xFFFF8A80),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isSslEnabled) {
                                "Your connection uses TLS encryption. Password and data transfers are protected against interceptors."
                            } else {
                                "Eavesdropping risk. Credentials and database contents will be transmitted in unencrypted plaintext."
                            },
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons Row (Save Profile + Connect)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onSaveProfile,
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2C323D),
                        disabledContainerColor = Color(0xFF1C2026),
                        contentColor = Color.White,
                        disabledContentColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Save Connection Icon",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Profile", fontSize = 13.sp)
                }

                Button(
                    onClick = onConnect,
                    enabled = isFormValid && !isConnecting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800),
                        disabledContainerColor = Color(0xFF2C2215),
                        contentColor = Color.Black,
                        disabledContentColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(48.dp)
                        .testTag("connect_btn")
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Connect Icon",
                                modifier = Modifier.size(18.dp),
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Connect",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
