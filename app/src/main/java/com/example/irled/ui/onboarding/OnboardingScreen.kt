package com.example.irled.ui.onboarding

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.irled.ui.theme.IrledTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onNavigateToMain: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.refreshStatus()
    }

    // Refresh status when returning to the app
    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
    }

    // We can also use a LifecycleObserver to refresh status when resumed
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Welcome to irled") }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Let's get your device ready to use the IR LED as a notification indicator.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            StatusItem(
                title = "IR Hardware",
                description = if (uiState.hasIrEmitter) "IR Emitter detected!" else "No IR Emitter found.",
                icon = if (uiState.hasIrEmitter) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                color = if (uiState.hasIrEmitter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )

            StatusItem(
                title = "Notification Access",
                description = if (uiState.isNotificationAccessGranted) "Access granted!" else "Access required to detect notifications.",
                icon = if (uiState.isNotificationAccessGranted) Icons.Rounded.CheckCircle else Icons.Rounded.Notifications,
                color = if (uiState.isNotificationAccessGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )

            StatusItem(
                title = "Phone Permission",
                description = if (uiState.isPhonePermissionGranted) "Permission granted!" else "Required for call alerts.",
                icon = if (uiState.isPhonePermissionGranted) Icons.Rounded.CheckCircle else Icons.Rounded.Call,
                color = if (uiState.isPhonePermissionGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            )

            Spacer(modifier = Modifier.weight(1f))

            if (!uiState.isNotificationAccessGranted) {
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Grant Notification Access")
                }
            }

            if (!uiState.isPhonePermissionGranted) {
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Grant Phone Permission")
                }
            }

            Button(
                onClick = onNavigateToMain,
                enabled = uiState.hasIrEmitter && uiState.isNotificationAccessGranted && uiState.isPhonePermissionGranted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Get Started")
            }
            
            if (!uiState.hasIrEmitter) {
                Text(
                    text = "Note: Your device does not appear to have an IR emitter. This app may not work as intended.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun StatusItem(
    title: String,
    description: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, device = "spec:width=411dp,height=891dp,navigation=buttons")
@Composable
fun OnboardingScreenPreview() {
    IrledTheme {
        OnboardingScreen(onNavigateToMain = {})
    }
}
