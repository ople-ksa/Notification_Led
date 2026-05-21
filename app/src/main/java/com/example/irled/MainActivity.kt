package com.example.irled

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.irled.ui.SettingsScreen
import com.example.irled.ui.main.MainScreen
import com.example.irled.ui.onboarding.OnboardingScreen
import com.example.irled.ui.theme.IrledTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val hardwareManager = HardwareManager(this)
        val startDestination = if (hardwareManager.isNotificationAccessGranted() && hardwareManager.hasIrEmitter()) {
            Destination.Main
        } else {
            Destination.Onboarding
        }

        setContent {
            IrledTheme {
                val context = LocalContext.current

                val navigationState = rememberNavigationState(
                    startRoute = startDestination,
                    topLevelRoutes = setOf(Destination.Onboarding, Destination.Main, Destination.Settings)
                )
                val navigator = remember { Navigator(navigationState) }
                
                val entryProvider = entryProvider {
                    entry<Destination.Onboarding> {
                        OnboardingScreen(
                            onNavigateToMain = {
                                context.startService(Intent(context, SystemEventService::class.java))
                                navigator.navigate(Destination.Main)
                            }
                        )
                    }
                    entry<Destination.Main> {
                        LaunchedEffect(Unit) {
                            context.startService(Intent(context, SystemEventService::class.java))
                        }
                        MainScreen(
                            onNavigateToSettings = { navigator.navigate(Destination.Settings) }
                        )
                    }
                    entry<Destination.Settings> {
                        SettingsScreen(onBack = { navigator.goBack() })
                    }
                }

                NavDisplay(
                    entries = navigationState.toEntries(entryProvider),
                    onBack = { navigator.goBack() }
                )
            }
        }
    }
}
