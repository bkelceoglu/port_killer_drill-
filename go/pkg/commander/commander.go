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
				KillPid(pidToKill)
			}
		}
	}
	if err := scanner.Err(); err != nil {
		log.Printf("Commander listener error: %v", err)
	}
	log.Printf("Commander command listener stopped.")
}

func KillPid(pid int32) bool {
	proc, err := process.NewProcess(pid)
	if err != nil {
		log.Printf("Could not find process %d: %v", pid, err)
		return false
	}
	if err := proc.Kill(); err != nil {
		log.Printf("Failed to kill process %d: %v", pid, err)
		return false
	}
	log.Printf("Successfully killed process %d", pid)
	return true
}
