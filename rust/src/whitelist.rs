use crate::models::PortRange;
use tracing::{error, info};
use std::fs;

pub fn load(filename: &str) -> Vec<PortRange> {
    let mut whitelist = Vec::new();
    match fs::read_to_string(filename) {
        Ok(contents) => {
            for line in contents.lines() {
                let l = line.trim();
                if l.is_empty() || l.starts_with("#") {
                    continue;
                }
                if !l.contains("-") {
                    whitelist.push(PortRange {
                        start_port: l.parse::<u16>().unwrap(), 
                        end_port: l.parse::<u16>().unwrap() });
                } else {
                    let parts: Vec<&str> = l.split("-").collect();
                    if parts.len() == 2 {
                        whitelist.push(PortRange { 
                            start_port: parts[0].trim().parse::<u16>().unwrap(), 
                            end_port: parts[1].trim().parse::<u16>().unwrap()});
                    }
                }
            }
            info!("Parsed whitelist from {}", filename);
        },
        Err(e) => {
            error!("Failed to read whitelist {}: {}", filename, e);
        }
    }
    whitelist
}

pub fn is_whitelisted(port: u16, whitelist: &[PortRange]) -> bool {
    for range in whitelist {
        if port >= range.start_port && port <= range.end_port {
            return true;
        }
    }
    false
}
