use std::env;
use tracing::{info, Level};

mod agent;
mod commander;
mod models;
mod monitor;
mod whitelist;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    tracing_subscriber::fmt()
        .with_max_level(Level::INFO)
        .init();
    let mut hostname = hostname::get()
        .map(|s| s.to_string_lossy().into_owned())
        .unwrap_or_else(|_| "unknown-host".to_string());

    let mut commander_ip = "127.0.0.1:9090".to_string(); // this should be remote ip adddress
    let args: Vec<String> = env::args().collect();
    if args.len() > 1 {
        commander_ip = args[1].clone();
        if !commander_ip.contains(':') {
            commander_ip.push_str(":9090");
        }
    }
    if args.len() > 2 {
        hostname = args[2].clone();
    }
    info!("Initializing Sentry for {}...", hostname);
    let mut sentry = agent::Sentry::new(
        hostname,
        commander_ip,
        std::time::Duration::from_millis(500),
    );
    sentry.start().await?;

    Ok(())
}
