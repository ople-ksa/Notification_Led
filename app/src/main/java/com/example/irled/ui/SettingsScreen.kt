package com.example.irled.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.irled.HardwareManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val hardwareManager = HardwareManager(context)
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    
    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }

    // Refresh status when returning to the screen
    LaunchedEffect(Unit) {
        isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoSection(
                title = "Background Reliability",
                items = emptyList(),
                content = {
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "To ensure the app runs reliably in the background, please disable battery optimizations and enable Autostart.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            },
                            enabled = !isIgnoringBatteryOptimizations,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.BatteryAlert, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isIgnoringBatteryOptimizations) "Optimizations Disabled" else "Disable Optimizations")
                        }

                        if (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)) {
                            Text(
                                text = "Xiaomi/HyperOS: You must manually enable 'Autostart' in App Info for the service to run after reboot or when idle.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent()
                                        intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Enable Xiaomi Autostart")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open App Info")
                        }
                    }
                }
            )

            InfoSection(
                title = "Hardware Info",
                items = listOf(
                    "IR Emitter: ${if (hardwareManager.hasIrEmitter()) "Available" else "Not Found"}",
                    "Frequencies: ${hardwareManager.getIrFrequencies()}"
                )
            )

            InfoSection(
                title = "About",
                items = listOf(
                    "Version: 3.0.1",
                    "Description: Uses device IR LED as a notification indicator."
                )
            )
        }
    }
}

@Composable
fun InfoSection(title: String, items: List<String>, content: @Composable (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            if (items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = item, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (content != null) {
                content()
            }
        }
    }
}
