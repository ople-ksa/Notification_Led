# Project Plan

An Android app called 'irled' that listens to notifications and triggers the device's IR LED to mimic a notification LED. The app should allow users to configure which apps trigger the IR LED and potentially different patterns. Add features for call alerts and charging status.

## Project Brief

# Project Brief: irled

A specialized utility for Android devices with an Infrared (IR) emitter, designed to emulate the functionality of legacy hardware notification LEDs. The app listens for system events and triggers the IR LED to provide visual alerts for notifications, calls, and charging status.

### Features
- **Intelligent Event Interceptor**: Monitors incoming notifications, call states (ringing and missed), and battery status (connected/full) to trigger corresponding IR signals.
- **Hardware Pulse Controller**: Directly interfaces with the `ConsumerIrManager` to execute precise IR blink patterns, including continuous pulses for active calls and quick bursts for charger connection.
- **Global App Toggle List**: Provides a comprehensive list of all installed applications, allowing users to granularly select which apps are permitted to trigger the IR alert.
- **Context-Aware Patterns**: Pre-configured behaviors for specific events: double-blink for charger connection, continuous blinking for incoming calls, and a specific 0.5-second pulse for a full battery.
- **Unified Permission Management**: A guided setup flow to handle sensitive access requirements, including Notification Access and Phone State permissions.

### High-Level Technical Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Navigation**: Jetpack Navigation 3 (State-driven)
- **Adaptive Strategy**: Compose Material Adaptive Library for a responsive, edge-to-edge layout.
- **Asynchrony**: Kotlin Coroutines for non-blocking hardware control and event processing.
- **Core APIs**:
    - `NotificationListenerService`: To detect and filter notifications from all apps.
    - `TelephonyManager` & `PhoneStateListener`: To monitor incoming and missed calls.
    - `ConsumerIrManager`: To drive the infrared hardware.
    - `BroadcastReceiver` & `BatteryManager`: To detect charging and power states.

## Implementation Steps

### Task_1_Setup_Permissions: Initialize the project structure, implement the NotificationListenerService shell, and create a permission onboarding screen for Notification Access and IR hardware verification.
- **Status:** COMPLETED
- **Updates:** The coder_agent has successfully initialized the project, implemented the NotificationListenerService shell, and created the permission onboarding screen.
- **Acceptance Criteria:**
  - NotificationListenerService is registered in AndroidManifest.xml
  - The app correctly identifies if the device has an IR emitter using ConsumerIrManager
  - Permission request flow for Notification Access is implemented

### Task_2_Persistence_SelectionUI: Set up Room database for storing selected app package names and IR patterns. Implement the Jetpack Compose UI to list installed apps and configure blink settings.
- **Status:** COMPLETED
- **Updates:** The coder_agent has successfully set up the Room database and implemented the MainScreen for app selection and IR pattern configuration.
- **Acceptance Criteria:**
  - Room database correctly saves and retrieves app selections
  - The UI displays a list of all installed apps with toggle switches
  - Customizable blink patterns (duration, frequency) can be saved in settings

### Task_3_Service_Logic_Integration: Implement the ConsumerIrManager pulse logic. Integrate the NotificationListenerService with the persistence layer to trigger IR pulses when notifications arrive from selected apps.
- **Status:** COMPLETED
- **Updates:** The coder_agent has successfully integrated the IR pulse logic with the NotificationListenerService.
- **Acceptance Criteria:**
  - NotificationListenerService correctly filters notifications based on user selection
  - IR emitter triggers pulses in the defined pattern upon receiving a notification
  - Background processing is efficient and respects system constraints

### Task_4_Polish_Verification: Apply Material 3 theme with energetic colors, implement Edge-to-Edge display, create an adaptive app icon, and perform final verification.
- **Status:** COMPLETED
- **Updates:** Task Task_4_Polish_Verification is complete.
- **Acceptance Criteria:**
  - Material 3 theme and Edge-to-Edge are fully implemented
  - Adaptive app icon is created and matches app function
  - Build passes and app does not crash during notification handling
  - UI is responsive across different form factors

### Task_5_System_Event_Interceptors: Implement the TelephonyManager/TelephonyCallback listener for call states (ringing, missed) and a BroadcastReceiver for battery status (connected, full). Integrate these with ConsumerIrManager for event-specific IR pulse patterns.
- **Status:** COMPLETED
- **Updates:** Task Task_5_System_Event_Interceptors is complete.
- **Acceptance Criteria:**
  - App detects incoming/missed calls and triggers continuous/specific IR pulses
  - App detects charger connection and battery full states with double-blink and 0.5s pulses
  - Background monitoring for phone and battery states is active and stable

### Task_6_UI_Configuration_Verify: Update the Compose UI to include configuration toggles for new system alerts (Calls, Charging). Ensure the app list reliably displays all installed applications. Conduct final verification of application stability and performance.
- **Status:** COMPLETED
- **Updates:** Task Task_6_UI_Configuration_Verify is complete.
- Updated UI to include configuration toggles for Call and Charging alerts.
- Ensured the app list displays all installed apps by checking the PackageManager query and QUERY_ALL_PACKAGES permission.
- Integrated DataStore for persistent system alert settings.
- Final verification by critic_agent confirms stability, functionality, and UI quality.
- Build is successful.
- **Acceptance Criteria:**
  - UI provides configuration options for call and charging alerts
  - App list correctly displays all installed apps
  - Project builds successfully, app does not crash, and all existing tests pass
  - Verify application stability (no crashes) and alignment with user requirements
- **Duration:** N/A

