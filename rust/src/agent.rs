use crate::models::{Alert, Handshake, PortRange};
use crate::monitor;
use crate::whitelist;
use std::time::Duration;
use tokio::net::TcpStream;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::time::sleep;
use tracing::{error, info, warn};

pub struct Sentry {
    pub hostname: String,
    pub commander_ip: String,
    pub whitelist: Vec<PortRange>,
    pub interval: Duration,
}

impl Sentry {
    pub fn new(hostname: String, commander_ip: String, interval: Duration) -> Self {
        Self {
            hostname,
            commander_ip,
            whitelist: Vec::new(),
            interval,
        }
    }

    pub async fn start(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        info!(
            "Sentry active: [{}] monitoring for [{}]",
            self.hostname, self.commander_ip
        );
        self.whitelist = whitelist::load("whitelist.txt");
        if !self.whitelist.is_empty() {
            info!("Loaded {} whitelist rules", self.whitelist.len());
        }
        loop {
            match TcpStream::connect(&self.commander_ip).await {
                Ok(stream) => {
                    info!("Connected to Commander [{}]. Synchronizing...", self.commander_ip);
                    if let Err(e) = self.handle_session(stream).await {
                        warn!("Session closed: {}. Reconnecting...", e);
                    }
                }
                Err(e) => {
                    warn!(
                        "Cannot connect to Commander at {}: {}. Retrying in 5s...",
                        self.commander_ip, e
                    );
                }
            }
            sleep(Duration::from_secs(5)).await;
        }
    }

    async fn handle_session(&mut self, mut stream: TcpStream) -> Result<(), Box<dyn std::error::Error>> {
        
        stream.set_nodelay(true)?;
        let handshake = Handshake {
            msg_type: "HANDSHAKE".to_string(),
            hostname: self.hostname.clone(),
        };
        let payload = serde_json::to_string(&handshake)? + "\n";
        stream.write_all(payload.as_bytes()).await?;
        info!("Handshake sent");
        let mut buf = [0; 1024];
        let n = stream.read(&mut buf).await?;
        if n == 0 {
            return Err("Connection closed by commander".into());
        }
        let response = String::from_utf8_lossy(&buf[..n]);
        info!("Response: {}", response);
        let (read_half, write_half) = stream.into_split();
        let (tx, rx) = tokio::sync::oneshot::channel();
        
        tokio::spawn(async move {
            crate::commander::handle_commands(read_half).await;
            let _ = tx.send(());
        });
        
        self.run_loop(write_half, rx).await
    }

    async fn run_loop(&self, mut write_half: tokio::net::tcp::OwnedWriteHalf, mut stop_rx: tokio::sync::oneshot::Receiver<()>) -> Result<(), Box<dyn std::error::Error>> {
        let mut known_ports = monitor::get_listening_ports().await;
        loop {
            tokio::select! {
                _ = &mut stop_rx => return Err("Command listener exited".into()),
                _ = sleep(self.interval) => {}
            }
            
            let current_ports = monitor::get_listening_ports().await;
            for (key, info) in current_ports.iter() {
                if !known_ports.contains_key(key) {
                    if whitelist::is_whitelisted(info.port, &self.whitelist) {
                        continue;
                    }
                    if info.pid > 0 {
                        let _alert = Alert {
                            msg_type: "NEW_PORT".to_string(),
                            hostname: self.hostname.clone(),
                            pid: info.pid,
                            name: info.process_name.clone(),
                            port: info.port,
                            protocol: info.protocol.clone(),
                        };
                        let payload = serde_json::to_string(&_alert)? + "\n";
                        if let Err(e) = write_half.write_all(payload.as_bytes()).await {
                            error!("Failed to send alert: {}", e);
                            return Err(e.into());
                        } else {
                            info!("ALERT SENT: {} opened port {}", info.process_name, info.port);
                        }
                    }
                }
            }
            known_ports = current_ports;
        }
    }
}
