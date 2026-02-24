use tokio::net::tcp::OwnedReadHalf;
use tokio::io::{AsyncBufReadExt, BufReader};
use tracing::{info, warn};
use sysinfo::System;
use serde_json::Value;

pub async fn handle_commands(stream: OwnedReadHalf) {
    info!("Starting command listener");
    
    let reader = BufReader::new(stream);
    let mut lines = reader.lines();

    while let Ok(Some(line)) = lines.next_line().await {
        if let Ok(request) = serde_json::from_str::<Value>(&line) {
            if let Some(kill_pid) = request.get("kill_pid").and_then(|v| v.as_f64()) {
                let pid_to_kill = kill_pid as u32;
                info!("Received Kill Order for PID: {}", pid_to_kill);
                
                let mut sys = System::new_all();
                sys.refresh_all();
                
                if let Some(process) = sys.process(sysinfo::Pid::from_u32(pid_to_kill)) {
                    if process.kill() {
                        info!("Successfully killed process {}", pid_to_kill);
                    } else {
                        warn!("Failed to kill process {}", pid_to_kill);
                    }
                } else {
                    warn!("Could not find process {}", pid_to_kill);
                }
            }
        }
    }

    info!("Commander command listener stopped.");
}
