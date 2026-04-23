# Ext Kernel Manager

A powerful, high-performance kernel management utility for Android, engineered with **Jetpack Compose** and a focus on deep hardware orchestration. Ext Kernel Manager is designed to bridge the gap between complex kernel tunables and a clean, intuitive user experience.

---

## 📖 Table of Contents
- [Project Philosophy](#-project-philosophy)
- [System Architecture](#-system-architecture)
- [Key Features](#-key-features)
- [Screen Breakdown](#-screen-breakdown)
- [Technical Specifications](#-technical-specifications)
- [Building from Source](#-building-from-source)
- [License](#-license)

---

## 🧩 Project Philosophy

Ext Kernel Manager was born out of a need for a tool that doesn't just "toggle switches" but understands the underlying system. Most kernel managers rely on hardcoded paths which break across devices. Ext uses a **dynamic discovery engine** that probes the filesystem in real-time, mapping out available hardware interfaces without manual configuration.

The goal is to provide **Layered Insight**:
1. **At a Glance**: Critical vitals (Temp, Frequency, Load).
2. **Interactive Control**: Direct tuning of governors and schedulers.
3. **Deep Diagnostics**: Detailed system metadata, uptime analytics, and engineering heritage.

---

## 🏗 System Architecture

The codebase follows a strict **Clean Architecture** pattern to ensure stability and modularity:

- **UI Layer (Jetpack Compose)**: Utilizes a reactive StateFlow-driven architecture. Every component is built with Material 3, ensuring a premium, high-contrast look that scales across resolutions.
- **HAL (Hardware Abstraction Layer)**: A collection of specialized controllers (CPU, GPU, Battery, Thermal) that communicate with the kernel via a robust shell interface.
- **Intelligence Engine**: A recursive scanner that identifies hardware signatures, SoC models, and path variations dynamically. This is what makes the app "mature" and flexible.
- **Data Persistence**: Uses Room/Jetpack DataStore to securely handle user settings and performance profiles.

---

## ✨ Key Features

### 1. The Pulse Dashboard
- **Real-time Telemetry**: High-frequency polling of CPU cores, GPU load, and battery health.
- **Human-Centric Data**: Sanity cleaning for kernel strings and technical build identifiers.
- **Tiered Polling**: A specialized loop that ensures Uptime and Frequencies "tick" every second, while heavier background tasks run on a separate 30s interval to save resources.

### 2. Fine-Grained Hardware Control
- **CPU Management**: Full control over clusters, governors, and individual core frequencies.
- **GPU Orchestration**: Support for multiple GPU vendors with frequency capping and governor selection.
- **I/O & Memory**: Real-time I/O scheduler switching and advanced LMK (Low Memory Killer) profile tuning.

### 3. Engineering Heritage
- **Device Identity**: Intelligent marketing name resolution (e.g., showing "Redmi Note 12" instead of technical model codes).
- **Deep Analytics**: Tracks Deep Sleep efficiency with percentage-based reporting and high-precision uptime (down to the second).

---

## 📱 Screen Breakdown

- **Dashboard**: Your command center. Features the Hero Card (Branding & Arch) and the Vitals Strip.
- **CPU Screen**: Detailed per-cluster graphing and frequency statistics.
- **GPU Screen**: Real-time load monitoring and power-saving controls.
- **I/O & Memory**: Disk scheduler tuning and ZRAM management.
- **More**: A comprehensive system diagnostic suite including SELinux status, baseband versions, and bootloader IDs.

---

## 🛠 Technical Specifications

- **Language**: Kotlin 1.9+
- **Min SDK**: API 30 (Android 11)
- **Root Engine**: libsu (Standardized root shell)
- **Dependency Injection**: Manual / ViewModel-based
- **Asynchronous Logic**: Kotlin Coroutines & Flows

---

## 🚀 Building from Source

Ensure you have the latest Android SDK and NDK installed.

1. Clone the repository:
   ```bash
   git clone https://github.com/itswill00/ExtKernelManager.git
   ```
2. Open in Android Studio (Ladybug or newer).
3. Build the APK:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📜 License

Copyright © 2026 **itswill00**.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0).

---
*Created and maintained by itswill00.*
