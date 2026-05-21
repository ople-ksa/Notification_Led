# Notification LED
## Introduction

img

Project's main purpose is to bring back those simple and old school notification LEDs. Useful for modern devices without a built-in LED indicator. This is just a fun project i thought of doing, with all the hardware mods and stuff. I tried to make the app with more controls, like blink rate on-off durations, fading in or out or repeating indefinitely if needed. 
<p>Hope you like this project!</p>

{/video}

## Key Features

- **IR-as-LED:** Uses the `ConsumerIrManager` to pulse the IR emitter (standard 38kHz) for system events and app notifications.
- **Per-App Customization:** Configure ON/OFF durations, repeat counts, and gradient/brightness settings for each installed application.
- **Priority-Based Queue:** Handles multiple overlapping notifications (e.g., Battery > Missed Call > App Message) with a sophisticated priority system.
- **Software-Based PWM Dimming:** 
    - **Gradient Mode:** Smooth "breathing" pulses using software PWM (250Hz) for a more aesthetic look.
    - **5% Intensity Lock:** Default safety lock limits IR output to 5% duty cycle to prevent hardware strain and ensure eye safety.
- **High Brightness Mode:** Unlock full power (100% duty cycle) for use in direct sunlight. Includes a double-verification safety prompt.
- **Intelligent Power Management:**
    - Automatically clears notifications when the screen is turned on or the device is unlocked.
    - 5-minute hard timeout for non-critical notification pulses.
    - Maintains a partial wake-lock only while pulsing to ensure timing accuracy without draining the battery.

## Tech Stack

- **UI:** Jetpack Compose with Material 3 Adaptive layouts.
- **Architecture:** MVVM with Kotlin Coroutines/Flow.
- **Navigation:** Navigation 3 (Experimental).
- **Persistence:** Room (App Settings) and DataStore (User Preferences).
- **Background Services:** 
    - `SystemEventService`: Monitors battery, power, and telephony states.
    - `IrNotificationListenerService`: Intercepts status bar notifications.
    - `IrPulseManager`: Core logic for managing the pulse queue and hardware timing.

## Safety Warning
- **Replaced LED could get intense. Due to the internal circuit designed for powerful flash/bursts** 
- The "High Brightness" mode should only be used in bright outdoor environments. Avoid looking directly into the IR emitter at close range for extended periods when full power is enabled. The app defaults to a 5% intensity limit for indoor use.

## Core Architecture
The tool adheres to the reactive modern Android app architecture principles:
UI Layer: Completely built on Jetpack Compose technology, including experimental Navigation 3 component and Material 3 Adaptive layout.
Data Layer: Utilizes DataStore (Preferences) for settings and Room for data storage.
Hardware Layer: Wraps ConsumerIrManager for pulse timing control and frequency (38kHz) configuration.

## What, Why, How

### What is it doing?
This project intercepts system-wide notifications and battery events, translating them into specific temporal patterns transmitted via the device's Infrared emitter. Unlike standard LEDs, the IR emitter is accessed through the `ConsumerIrManager` API, typically used for remote controls.

### Why was it built this way?
- **Hardware Repurposing:** Many modern phones have removed the notification LED to achieve "all-screen" designs. This app gives that functionality back without requiring external hardware.
- **Safety First:** IR light is invisible to the human eye, meaning a user wouldn't know if they were being overexposed to a high-intensity beam. The **5% Intensity Lock** and **Safety Dialogs** are critical because you can't rely on your blink reflex for protection against IR.
- **Battery Efficiency:** By using a `PARTIAL_WAKE_LOCK` only during the short ON/OFF cycles and implementing a 5-minute timeout, the app minimizes its footprint on battery life.

### How does it work?
- **Pulse Synthesis:** The `IrPulseManager` manages a priority-sorted queue. When a high-priority event occurs, it cancels lower-priority pulses and takes control of the hardware.
- **Software PWM:** Since IR hardware is usually binary (`ON` at a carrier frequency or `OFF`), we achieve dimming and gradients by rapidly switching the emitter at 250Hz. By varying the "ON" time within each `4ms` window (duty cycle), we trick the hardware (and the human eye's persistence of vision when viewed through a camera) into seeing different brightness levels.
- **Persistence:** A `Foreground Service` coupled with a `NotificationListenerService` ensures the app survives Android's aggressive memory management. It also includes a "Rebind" loop to recover the listener if it's disconnected by the OS.

## Screenshots

| Main Dashboard | App Configuration | Safety Prompts |
| :---: | :---: | :---: |
|img | img | jmg |


## Important Modules
### 1. IrPulseManager (The Brain):
Manages all IR operations using a priority queue.
Maintains `PowerManager.PARTIAL_WAKE_LOCK` to guarantee accurate pulse timings regardless of the screen state.
Features an emergency 5-minute hard timeout for less important notifications to preserve battery power.
Employs the ConcurrentHashMap and mutex for safe manipulation of IR patterns by multiple processes.

### 2. HardwareManager (The Driver):
Communicates with `ConsumerIrManager`.
Works with various hardware-related issues, e.g., Xiaomi HALs' need for an odd-sized array for IR patterns.
Features utility methods that allow the app to determine if IR LED exists and if the user has granted notification

### 3. SystemEventService (The Watcher):
A Foreground Service that ensures the app stays active.
Listens for system-wide Intent broadcasts (Battery Low, Charger Connected, Screen On).
Monitors telephony states to pulse the IR LED during ongoing calls.
Implements a "persistence check" to rebind the notification listener if the system kills it.

### 4. IrNotificationListenerService:
Extends NotificationListenerService to intercept incoming app notifications and trigger specific IR patterns based on the notification type.
Technical Strengths
- **Efficient Pulse Logic:** The IrPulseManager prevents redundant restarts when receiving frequent battery updates, reducing CPU churn.
- **User-Centric Design:** Patterns are automatically cleared when the user turns on the screen or unlocks the device, assuming the notification has been seen.
Robustness: Use of `START_STICKY` and foreground notifications minimizes the chance of the service being killed by Android's memory management.
Potential Considerations
- **Hardware Support:** The app relies on the ConsumerIrService. On some devices, the IR hardware is restricted to the camera subsystem and may not be accessible via this standard API.
Battery Impact: Holding a partial wake lock and pulsing the IR LED is power-intensive; the current 5-minute timeout is a vital safeguard.

## 🚀 Getting Started

1.  **Enable Notification Access:** The app requires permission to listen to notifications to trigger the IR LED.
2.  **Configure Apps:** Expand any app card to enable its notification LED and customize the pattern.
3.  **System Alerts:** Enable alerts for low battery, charging connection, and ongoing calls.
4.  **Hardware Check:** The app will automatically detect if your device has a supported `ConsumerIrManager` interface.

## Final Product/ Behaviours [Xiaomi M6 PRO 5G]
### Non-System Alerts

{/video}

{/video}

### System Alerts

{/video}

---
*Note: This app is specialized for devices with a dedicated IR blaster. Performance may vary depending on the hardware's HAL implementation (e.g., Xiaomi/POCO devices are specifically optimized for).*
