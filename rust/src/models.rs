use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PortRange {
    pub start_port: u16,
    pub end_port: u16,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Handshake {
    #[serde(rename = "type")]
    pub msg_type: String,
    pub hostname: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Alert {
    #[serde(rename = "type")]
    pub msg_type: String,
    pub hostname: String,
    pub pid: u32,
    pub name: String,
    pub port: u16,
    pub protocol: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PortInfo {
    pub port: u16,
    pub pid: u32,
    pub process_name: String,
    pub protocol: String,
}
