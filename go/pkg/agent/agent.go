package agent

import (
	"encoding/json"
	"fmt"
	"log"
	"net"
	"port_scanner/agent/pkg/commander"
	"port_scanner/agent/pkg/models"
	"port_scanner/agent/pkg/monitor"
	"port_scanner/agent/pkg/whitelist"
	"time"
)

type Sentry struct {
	Hostname    string
	CommanderIP string
	Whitelist   []models.PortRange
	Interval    time.Duration
}

func NewSentry(hostname, commanderIP string, interval time.Duration) *Sentry {
	return &Sentry{
		Hostname:    hostname,
		CommanderIP: commanderIP,
		Interval:    interval,
	}
}

func (s *Sentry) Start() {
	log.Printf("Sentry active: [%s] monitoring for [%s]", s.Hostname, s.CommanderIP)

	// Load Whitelist once at startup
	s.Whitelist = whitelist.Load("whitelist.txt")
	if len(s.Whitelist) > 0 {
		log.Printf("Loaded %d whitelist rules", len(s.Whitelist))
	}

	for {
		conn, err := net.Dial("tcp", s.CommanderIP)
		if err != nil {
			log.Printf("Cannot connect to Commander at %s: %v. Retrying in 5s...", s.CommanderIP, err)
			time.Sleep(5 * time.Second)
			continue
		}

		log.Printf("Connected to Commander [%s]. Synchronizing...", s.CommanderIP)
		
		if err := s.handleSession(conn); err != nil {
			log.Printf("Session closed: %v. Reconnecting...", err)
		}
		
		conn.Close()
		time.Sleep(1 * time.Second)
	}
}

func (s *Sentry) handleSession(conn net.Conn) error {
	// 0. Configure Connection
	if tcpConn, ok := conn.(*net.TCPConn); ok {
		tcpConn.SetKeepAlive(true)
		tcpConn.SetKeepAlivePeriod(30 * time.Second)
	}

	// 1. Handshake
	handshake := models.Handshake{
		Type:     "HANDSHAKE",
		Hostname: s.Hostname,
	}
	if err := json.NewEncoder(conn).Encode(handshake); err != nil {
		return err
	}

	// 2. Wait for WELCOME (Confirm bidirectional link)
	log.Printf("Waiting for WELCOME acknowledgment...")
	conn.SetReadDeadline(time.Now().Add(10 * time.Second))
	var response map[string]interface{}
	if err := json.NewDecoder(conn).Decode(&response); err != nil {
		return fmt.Errorf("failed to receive welcome: %v", err)
	}
	if response["type"] != "WELCOME" {
		return fmt.Errorf("unexpected response from commander: %v", response)
	}
	conn.SetReadDeadline(time.Time{}) // Reset deadline
	log.Printf("Bidirectional link established with Commander.")

	// 3. Command Listener
	stopCmd := make(chan bool)
	go func() {
		commander.HandleCommands(conn)
		stopCmd <- true
	}()

	// 4. Monitoring Loop
	return s.runLoop(conn, stopCmd)
}

func (s *Sentry) runLoop(conn net.Conn, stopCmd chan bool) error {
	knownPorts := monitor.GetListeningPorts()
	encoder := json.NewEncoder(conn)
	
	for {
		select {
		case <-stopCmd:
			return fmt.Errorf("command listener exited")
		default:
			currentPorts := monitor.GetListeningPorts()

			for key, info := range currentPorts {
				if _, exists := knownPorts[key]; !exists {
					if whitelist.IsWhitelisted(info.Port, s.Whitelist) {
						continue
					}

					if info.Pid > 0 {
						alert := models.Alert{
							Type:        "NEW_PORT",
							Hostname:    s.Hostname,
							Pid:         info.Pid,
							ProcessName: info.ProcessName,
							Port:        info.Port,
							Protocol:    info.Protocol,
						}
						if err := encoder.Encode(alert); err != nil {
							return err
						}
						log.Printf("ALERT SENT: %s opened port %d", info.ProcessName, info.Port)
					}
				}
			}

			knownPorts = currentPorts
			time.Sleep(s.Interval)
		}
	}
}
