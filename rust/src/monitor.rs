use sysinfo::System;
use std::collections::HashMap;
use crate::models::PortInfo;
use netstat2::{get_sockets_info, AddressFamilyFlags, ProtocolFlags, ProtocolSocketInfo};

pub async fn get_listening_ports() -> HashMap<String, PortInfo> {
    let mut ports = HashMap::new();
    let mut sys = System::new_all();
    sys.refresh_all();

    let af_flags = AddressFamilyFlags::IPV4 | AddressFamilyFlags::IPV6;
    let proto_flags = ProtocolFlags::TCP | ProtocolFlags::UDP;

    let sockets_info = get_sockets_info(af_flags, proto_flags).unwrap_or_else(|_| vec![]);

    for si in sockets_info {
        // Here we simulate the logic of fetching port bindings.
        // We look for tcp/udp sockets in 'LISTEN' state or similar.
        match si.protocol_socket_info {
            ProtocolSocketInfo::Tcp(tcp_si) => {
                // If the state is Listen, usually `tcp_si.state == netstat2::TcpState::Listen`
                if tcp_si.state == netstat2::TcpState::Listen {
                    let port = tcp_si.local_port;
                    
                    for p in &si.associated_pids {
                        if let Some(process) = sys.process(sysinfo::Pid::from_u32(*p)) {
                            let process_name = process.name().to_string_lossy().to_string();
                            
                            let info = PortInfo {
                                port,
                                pid: *p,
                                process_name,
                                protocol: "TCP".to_string(),
                            };
                            
                            let key = format!("{}:{}", info.process_name, info.port);
                            ports.insert(key, info);
                        }
                    }
                }
            }
            ProtocolSocketInfo::Udp(udp_si) => {
                let port = udp_si.local_port;
                for p in &si.associated_pids {
                    if let Some(process) = sys.process(sysinfo::Pid::from_u32(*p)) {
                        let process_name = process.name().to_string_lossy().to_string();
                        
                        let info = PortInfo {
                            port,
                            pid: *p,
                            process_name,
                            protocol: "UDP".to_string(),
                        };
                        
                        let key = format!("{}:{}", info.process_name, info.port);
                        ports.insert(key, info);
                    }
                }
            }
        }
    }

    ports
}
