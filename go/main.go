package main

import (
	"log"
	"net"
	"os"
	"port_scanner/agent/pkg/agent"
	"time"
)

func containsPort(s string) bool {
	_, _, err := net.SplitHostPort(s)
	return err == nil
}

func main() {
	hostname, _ := os.Hostname()
	commanderIP := "127.0.0.1:9090"

	if len(os.Args) > 1 {
		commanderIP = os.Args[1]
		if !containsPort(commanderIP) {
			commanderIP = commanderIP + ":9090"
		}
	}
	if len(os.Args) > 2 {
		hostname = os.Args[2]
	}

	if hostname == "" {
		hostname = "unknown-host"
	}

	log.Printf("Initializing Sentry for %s...", hostname)
	sentry := agent.NewSentry(hostname, commanderIP, 500*time.Millisecond)
	sentry.Start()
}
