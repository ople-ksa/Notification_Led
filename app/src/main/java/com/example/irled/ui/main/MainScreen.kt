package com.example.irled.ui.main

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.irled.data.AppCategory
import com.example.irled.data.AppSetting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val priorityList by viewModel.priorityList.collectAsStateWithLifecycle()
    val chargingEnabled by viewModel.chargingAlertsEnabled.collectAsStateWithLifecycle()
    val batteryFullEnabled by viewModel.batteryFullAlertsEnabled.collectAsStateWithLifecycle()
    val lowBatteryEnabled by viewModel.lowBatteryAlertsEnabled.collectAsStateWithLifecycle()
    val lowBatteryThreshold by viewModel.lowBatteryThreshold.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Notification LED") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        val userApps = remember(uiState) { uiState.filter { !it.isSystemApp } }
        val systemApps = remember(uiState) { uiState.filter { it.isSystemApp } }
        
        var userAppsExpanded by remember { mutableStateOf(true) }
        var systemAppsExpanded by remember { mutableStateOf(false) }
        var priorityExpanded by remember { mutableStateOf(false) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PrioritySection(
                    priorityList = priorityList,
                    expanded = priorityExpanded,
                    onToggle = { priorityExpanded = !priorityExpanded }
                )
            }

            item {
                SystemAlertsSection(
                    chargingEnabled = chargingEnabled,
                    onChargingToggle = { viewModel.toggleChargingAlerts(it) },
                    batteryFullEnabled = batteryFullEnabled,
                    onBatteryFullToggle = { viewModel.toggleBatteryFullAlerts(it) },
                    lowBatteryEnabled = lowBatteryEnabled,
                    onLowBatteryToggle = { viewModel.toggleLowBatteryAlerts(it) },
                    lowBatteryThreshold = lowBatteryThreshold,
                    onLowBatteryThresholdChange = { viewModel.setLowBatteryThreshold(it.toInt()) }
                )
            }

            item {
                CategoryHeader(
                    title = "User Applications",
                    count = userApps.size,
                    expanded = userAppsExpanded,
                    onExpandToggle = { userAppsExpanded = !userAppsExpanded }
                )
            }

            if (userAppsExpanded) {
                items(userApps, key = { it.packageName }) { appItem ->
                    AppCard(
                        appItem = appItem,
                        onToggle = { viewModel.toggleApp(appItem.packageName, it) },
                        onUpdatePattern = { on, off, repeat, indef, isMsg, isGrad, isHigh, category ->
                            viewModel.updatePattern(appItem.packageName, on, off, repeat, indef, isMsg, isGrad, isHigh, category)
                        }
                    )
                }
            }

            item {
                CategoryHeader(
                    title = "System Applications",
                    count = systemApps.size,
                    expanded = systemAppsExpanded,
                    onExpandToggle = { systemAppsExpanded = !systemAppsExpanded }
                )
            }

            if (systemAppsExpanded) {
                items(systemApps, key = { it.packageName }) { appItem ->
                    AppCard(
                        appItem = appItem,
                        onToggle = { viewModel.toggleApp(appItem.packageName, it) },
                        onUpdatePattern = { on, off, repeat, indef, isMsg, isGrad, isHigh, category ->
                            viewModel.updatePattern(appItem.packageName, on, off, repeat, indef, isMsg, isGrad, isHigh, category)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PrioritySection(
    priorityList: List<PriorityItem>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    ElevatedCard(onClick = onToggle) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Active Priority Queue",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    priorityList.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.width(24.dp)
                            )
                            Column {
                                Text(text = item.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = item.detail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    onExpandToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
        IconButton(onClick = onExpandToggle) {
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null
            )
        }
    }
}

@Composable
fun SystemAlertsSection(
    chargingEnabled: Boolean,
    onChargingToggle: (Boolean) -> Unit,
    batteryFullEnabled: Boolean,
    onBatteryFullToggle: (Boolean) -> Unit,
    lowBatteryEnabled: Boolean,
    onLowBatteryToggle: (Boolean) -> Unit,
    lowBatteryThreshold: Int,
    onLowBatteryThresholdChange: (Float) -> Unit
) {
    ElevatedCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "System Alerts", style = MaterialTheme.typography.titleMedium)
            
            SystemToggleItem("Power Connected", Icons.Rounded.Settings, chargingEnabled, onChargingToggle)
            SystemToggleItem("Battery Full", Icons.Rounded.Settings, batteryFullEnabled, onBatteryFullToggle)
            SystemToggleItem("Low Battery Alert", Icons.Rounded.Settings, lowBatteryEnabled, onLowBatteryToggle)
        }
    }
}

@Composable
fun SystemToggleItem(label: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun AppCard(
    appItem: AppItemState,
    onToggle: (Boolean) -> Unit,
    onUpdatePattern: (Long, Long, Int, Boolean, Boolean, Boolean, Boolean, AppCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showHighBrightnessDialog1 by remember { mutableStateOf(false) }
    var showHighBrightnessDialog2 by remember { mutableStateOf(false) }

    val setting = appItem.setting ?: AppSetting(appItem.packageName)

    if (showHighBrightnessDialog1) {
        AlertDialog(
            onDismissRequest = { showHighBrightnessDialog1 = false },
            title = { Text("Warning") },
            text = { Text("Do this at your own risk, this may cause eye damage if looked directly at. Only use this option when outside or in the sun.") },
            confirmButton = {
                Button(onClick = {
                    showHighBrightnessDialog1 = false
                    showHighBrightnessDialog2 = true
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showHighBrightnessDialog1 = false }) { Text("Cancel") }
            }
        )
    }

    if (showHighBrightnessDialog2) {
        AlertDialog(
            onDismissRequest = { showHighBrightnessDialog2 = false },
            title = { Text("Final Warning") },
            text = { Text("Setting the LED to maximum brightness will increase battery consumption and may cause the device to heat up. Prolonged viewing at extreme brightness may cause eye strain.") },
            confirmButton = {
                Button(onClick = {
                    showHighBrightnessDialog2 = false
                    onUpdatePattern(setting.onDurationMs, setting.offDurationMs, setting.repeatCount, setting.repeatIndefinitelyIfLocked, setting.isMessageApp, setting.isGradient, true, setting.category)
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showHighBrightnessDialog2 = false }) { Text("Cancel") }
            }
        )
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    bitmap = appItem.icon.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = appItem.appName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = appItem.isEnabled,
                    onCheckedChange = onToggle
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!setting.isMessageApp) {
                        PatternSlider(
                            label = "On Duration: ${setting.onDurationMs}ms",
                            value = setting.onDurationMs.toFloat(),
                            range = 10f..1000f,
                            onValueChange = { onUpdatePattern(it.toLong(), setting.offDurationMs, setting.repeatCount, setting.repeatIndefinitelyIfLocked, setting.isMessageApp, setting.isGradient, setting.isHighBrightness, setting.category) }
                        )

                        PatternSlider(
                            label = "Off Duration: ${setting.offDurationMs}ms",
                            value = setting.offDurationMs.toFloat(),
                            range = 100f..5000f,
                            onValueChange = { onUpdatePattern(setting.onDurationMs, it.toLong(), setting.repeatCount, setting.repeatIndefinitelyIfLocked, setting.isMessageApp, setting.isGradient, setting.isHighBrightness, setting.category) }
                        )

                        PatternSlider(
                            label = "Repeat Count: ${setting.repeatCount}",
                            value = setting.repeatCount.toFloat(),
                            range = 1f..20f,
                            onValueChange = { onUpdatePattern(setting.onDurationMs, setting.offDurationMs, it.toInt(), setting.repeatIndefinitelyIfLocked, setting.isMessageApp, setting.isGradient, setting.isHighBrightness, setting.category) }
                        )
                    } else {
                        val predefinedText = when(setting.category) {
                            AppCategory.CELLULAR -> "Cellular: 100ms ON / 2500ms OFF"
                            AppCategory.EMAIL -> "Email: 400ms ON / 2500ms OFF"
                            else -> "Social: 700ms ON / 2500ms OFF"
                        }
                        Text(
                            text = "Predefined Pattern: $predefinedText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Is Gradient?", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = setting.isGradient,
                            onCheckedChange = { onUpdatePattern(setting.onDurationMs, setting.offDurationMs, setting.repeatCount, setting.repeatIndefinitelyIfLocked, setting.isMessageApp, it, setting.isHighBrightness, setting.category) }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Is High brightness?", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = setting.isHighBrightness,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showHighBrightnessDialog1 = true
                                } else {
                                    onUpdatePattern(setting.onDurationMs, setting.offDurationMs, setting.repeatCount, setting.repeatIndefinitelyIfLocked, setting.isMessageApp, setting.isGradient, false, setting.category)
                                }
                            }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Repeat Indefinitely", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = setting.repeatIndefinitelyIfLocked,
                            onCheckedChange = { onUpdatePattern(setting.onDurationMs, setting.offDurationMs, setting.repeatCount, it, setting.isMessageApp, setting.isGradient, setting.isHighBrightness, setting.category) }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Is Higher Priority?", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = setting.isMessageApp,
                            onCheckedChange = { onUpdatePattern(setting.onDurationMs, setting.offDurationMs, setting.repeatCount, setting.repeatIndefinitelyIfLocked, it, setting.isGradient, setting.isHighBrightness, setting.category) }
                        )
                    }
                    
                    if (setting.isMessageApp) {
                        Text(text = "Priority Category", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppCategory.entries.filter { it != AppCategory.GENERAL }.forEach { cat ->
                                FilterChip(
                                    selected = setting.category == cat,
                                    onClick = { onUpdatePattern(setting.onDurationMs, setting.offDurationMs, setting.repeatCount, setting.repeatIndefinitelyIfLocked, setting.isMessageApp, setting.isGradient, setting.isHighBrightness, cat) },
                                    label = { Text(cat.name) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatternSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.height(24.dp)
        )
    }
}
