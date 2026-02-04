package commander

import (
	"bufio"
	"encoding/json"
	"log"
	"net"

	"github.com/shirou/gopsutil/process"
)

func HandleCommands(conn net.Conn) {
	scanner := bufio.NewScanner(conn)
	for scanner.Scan() {
		command := scanner.Text()
		var request map[string]interface{}
		if err := json.Unmarshal([]byte(command), &request); err == nil {
			rawPid, ok := request["kill_pid"].(float64)
			if ok {
				pidToKill := int32(rawPid)
				log.Printf("Received Kill Order for PID: %d", pidToKill)
				proc, err := process.NewProcess(pidToKill)
				if err == nil {
					if err := proc.Kill(); err != nil {
						log.Printf("Failed to kill process %d: %v", pidToKill, err)
					} else {
						log.Printf("Successfully killed process %d", pidToKill)
					}
				} else {
					log.Printf("Could not find process %d: %v", pidToKill, err)
				}
			}
		}
	}
	if err := scanner.Err(); err != nil {
		log.Printf("Commander listener error: %v", err)
	}
	log.Printf("Commander command listener stopped.")
}
