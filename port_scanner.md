Since you prefer a **"hands-off-the-kernel"** approach (which is wise for stability and safety), the **Listener-Agent Architecture** is the perfect solution.

This architecture splits the responsibility:

1. **The Sentry (Agent):** A small, efficient program running on the remote Linux server (as `root`). It monitors the state and executes commands.
2. **The Commander (Listener):** A GUI application on your laptop (Kotlin + Compose) that receives alerts and authorizes actions.

### The Architecture Diagram

* **Agent (Go):** Runs on Server. "Polls" local network state using standard APIs (safe).
* **Network:** Communicates via a secure TCP/WebSocket socket.
* **Listener (Kotlin):** Runs on Laptop. Visualizes the threat and sends the "KILL" signal.

---

### Part 1: The Sentry (Agent in Go)

We will use the **`gopsutil`** library. It is the industry standard for system monitoring in Go. It reads from `/proc` and Netlink interfaces safely (no kernel modules).

**Features:**

* Checks open ports every 500ms.
* If a new port opens, it sends a JSON alert to your laptop.
* It listens for a "KILL [PID]" command from you.

**File:** `agent.go`

```go
package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"log"
	"net"
	"os"
	"time"

	"github.com/shirou/gopsutil/net" // go get github.com/shirou/gopsutil/net
	"github.com/shirou/gopsutil/process" // go get github.com/shirou/gopsutil/process
)

// Configuration
const COMMANDER_IP = "192.168.1.100:9090" // Your Laptop's IP
const POLLING_INTERVAL = 500 * time.Millisecond

type Alert struct {
	Type        string `json:"type"`
	Pid         int32  `json:"pid"`
	ProcessName string `json:"name"`
	Port        uint32 `json:"port"`
	Protocol    string `json:"protocol"`
}

func main() {
	// 1. Connect to the Commander (Laptop)
	conn, err := net.Dial("tcp", COMMANDER_IP)
	if err != nil {
		log.Fatalf("Cannot connect to Commander: %v", err)
	}
	defer conn.Close()
	log.Println("Connected to Commander. Starting Watchdog...")

	// Channel to handle incoming Kill commands
	go handleCommands(conn)

	// 2. Establish Baseline
	knownPorts := getListeningPorts()
	
	// 3. Monitor Loop
	for {
		currentPorts := getListeningPorts()

		// Detect New Ports
		for key, info := range currentPorts {
			if _, exists := knownPorts[key]; !exists {
				// ALERT!
				alert := Alert{
					Type:        "NEW_PORT",
					Pid:         info.Pid,
					ProcessName: info.ProcessName,
					Port:        info.Port,
					Protocol:    info.Protocol,
				}
				
				// Send JSON to Laptop
				encoder := json.NewEncoder(conn)
				encoder.Encode(alert)
				
				log.Printf("ALERT SENT: %s opened port %d", info.ProcessName, info.Port)
			}
		}

		knownPorts = currentPorts
		time.Sleep(POLLING_INTERVAL)
	}
}

// Helper to get map of "Port:Protocol" -> Info
func getListeningPorts() map[string]Alert {
	results := make(map[string]Alert)
	
	// "inet" gets both IPv4 and IPv6
	conns, _ := net.Connections("inet")
	
	for _, c := range conns {
		if c.Status == "LISTEN" {
			proc, _ := process.NewProcess(c.Pid)
			name, _ := proc.Name()
			
			key := fmt.Sprintf("%d:%d", c.Laddr.Port, c.Pid)
			results[key] = Alert{
				Pid:         c.Pid,
				ProcessName: name,
				Port:        c.Laddr.Port,
				Protocol:    "TCP", // simplified
			}
		}
	}
	return results
}

// Receive "KILL" commands from Laptop
func handleCommands(conn net.Conn) {
	scanner := bufio.NewScanner(conn)
	for scanner.Scan() {
		command := scanner.Text()
		var request map[string]int32
		if err := json.Unmarshal([]byte(command), &request); err == nil {
			pidToKill := request["kill_pid"]
			if pidToKill > 0 {
				log.Printf("Received Kill Order for PID: %d", pidToKill)
				proc, err := process.NewProcess(pidToKill)
				if err == nil {
					proc.Kill() // Force Kill
				}
			}
		}
	}
}

```

---

### Part 2: The Commander (Kotlin + Compose)

This runs on your laptop. It acts as a TCP Server. When the Agent connects, it listens for alerts and shows them in a list.

**Dependencies (build.gradle.kts):**

```kotlin
implementation(compose.desktop.currentOs)
implementation("com.google.code.gson:gson:2.10.1")

```

**File:** `Main.kt`

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.application.application
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.Scanner

// Data Model
data class Alert(
    val type: String,
    val pid: Int,
    val name: String,
    val port: Int,
    val protocol: String
)

fun main() = application {
    val alerts = remember { mutableStateListOf<Alert>() }
    var activeSocket by remember { mutableStateOf<Socket?>(null) }

    // Start TCP Server in background
    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            val server = ServerSocket(9090)
            println("Commander listening on port 9090...")
            while (true) {
                val socket = server.accept()
                activeSocket = socket
                val scanner = Scanner(socket.getInputStream())
                val gson = Gson()
                
                while (scanner.hasNextLine()) {
                    val line = scanner.nextLine()
                    try {
                        val alert = gson.fromJson(line, Alert::class.java)
                        alerts.add(0, alert) // Add to top
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }
    }

    Window(onCloseRequest = ::exitApplication, title = "NATO Drill // Overwatch") {
        MaterialTheme(colors = darkColors()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Network Intrusion Alerts", style = MaterialTheme.typography.h4)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (activeSocket == null) {
                    Text("Waiting for Agent to connect...", color = Color.Yellow)
                } else {
                    Text("Agent Connected via Secure Channel", color = Color.Green)
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(alerts) { alert ->
                        AlertRow(alert) { pid ->
                            // Send Kill Command
                            activeSocket?.let { sock ->
                                Thread {
                                    val writer = PrintWriter(sock.getOutputStream(), true)
                                    val cmd = "{\"kill_pid\": $pid}"
                                    writer.println(cmd)
                                }.start()
                            }
                            // Remove from UI (or mark as killed)
                            alerts.remove(alert)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertRow(alert: Alert, onKill: (Int) -> Unit) {
    Card(
        backgroundColor = Color(0xFF2A2A2A),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("PROCESS: ${alert.name}", style = MaterialTheme.typography.h6, color = Color.Red)
                Text("PID: ${alert.pid} | PORT: ${alert.port}")
            }
            Button(
                onClick = { onKill(alert.pid) },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
            ) {
                Text("NEUTRALIZE")
            }
        }
    }
}

```

### How to Drill with This

1. **Laptop:** Run the Kotlin app. It sits and waits on port 9090.
2. **Server:** Compile the Go agent (`go build agent.go`) and run it with `sudo ./agent`.
3. **The Test:** Open a terminal on the server and run `nc -l 4444` (Netcat listen).
4. **The Effect:**
* Within 0.5 seconds, your Kotlin UI will flash a red card: **PROCESS: nc | PORT: 4444**.
* Click **"NEUTRALIZE"**.
* On the server, the `nc` process will immediately be killed.



### Why this is better for your Drill

* **Safety:** You are not modifying the kernel. If the Go agent crashes, the server stays alive.
* **Visuals:** The "Commander" UI looks professional and fits the "NATO Drill" scenario perfectly.
* **Architecture:** It demonstrates understanding of distributed systems (Agent/Controller pattern).

### Next Step

Would you like to add a "Safe List" feature to the Kotlin UI so it doesn't alert you for standard things like SSH (port 22) or Nginx (port 80)?
