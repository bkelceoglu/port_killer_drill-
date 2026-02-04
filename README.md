# NATO LOCKED SHIELD ** PORT WATCHDOG

A real-time intrusion monitoring and response system designed for the NATO Locked Shield drill. This system provides a tactical dashboard to visualize agent connections, monitor port activity, and neutralize unauthorized processes automatically or manually.

## 🚀 Key Features

- **Tactical Dashboard**: Real-time visualization of intrusion alerts including hostname, IP, PID, and protocol.
- **Sentry Go Agent**: Highly efficient monitoring agent built in Go for low-overhead host analysis.
- **Active Agent Monitoring**: Live status bar tracking all registered monitoring agents.
- **Auto-Kill Response**: Configurable automated process termination for rapid threat containment.
- **Manual Neutralization**: Grainular control over process termination via a single-click UI.
- **Modular Architecture**: Cleanly separated UI components and domain logic following modern Kotlin patterns.
- **Export System**: Capture and export incident logs for post-drill analysis.

## 🛠 Tech Stack

- **Backend (Commander)**: Kotlin 2.3.0 (JVM 25)
- **Agent (Sentry)**: Go 1.23+
- **UI Framework**: Jetpack Compose for Desktop
- **Networking**: Virtual Threads (Project Loom) for high-concurrency socket handling
- **State Management**: Compose State với SnapshotStateMap
- **Serialization**: Gson

## 📁 Project Structure

```text
.
├── go/                # Sentry Monitoring Agent (Go)
│   ├── pkg/           # Agent, Monitor, and Whitelist logic
│   └── main.go        # Agent entry point
├── kotlin/            # Tactical Commander UI (Kotlin)
│   └── src/main/kotlin/
│       ├── com/natodrill/
│       │   ├── model/   # Domain Models
│       │   ├── network/ # Server logic
│       │   └── state/   # Central State
│       └── ui/          # Modular Dashboard Components
```

## 🛠 Getting Started

### Prerequisites

- JDK 25
- Gradle 8+

### Running the Commander

```bash
cd kotlin
./gradlew run
```

### Running the Sentry Agent
should be run on the target machine with root privileges

```bash
cd go
go run main.go [COMMANDER_IP] [HOST_NAME]
```

### Building for Distribution

```bash
./gradlew build
```

## 🛡 Security & Compliance

This tool is intended for use in controlled cyber-defense exercises. Automated process termination ("Auto-Kill") should be used in accordance with drill Rules of Engagement (RoE).

---
*Developed for the NATO Locked Shield 2026 Exercise.*
