package monitor

import (
	"fmt"
	"log"
	"port_scanner/agent/pkg/models"

	psutilnet "github.com/shirou/gopsutil/net"
	"github.com/shirou/gopsutil/process"
)

func GetListeningPorts() map[string]models.Alert {
	results := make(map[string]models.Alert)

	conns, err := psutilnet.Connections("inet")
	if err != nil {
		log.Printf("Error getting net connections: %v", err)
		return results
	}

	for _, c := range conns {
		if c.Status == "LISTEN" {
			name := "Unknown"
			if c.Pid > 0 {
				proc, err := process.NewProcess(c.Pid)
				if err == nil {
					n, err := proc.Name()
					if err == nil {
						name = n
					}
				}
			}

			key := fmt.Sprintf("%d:%d", c.Laddr.Port, c.Pid)
			results[key] = models.Alert{
				Pid:         c.Pid,
				ProcessName: name,
				Port:        c.Laddr.Port,
				Protocol:    "TCP",
			}
		}
	}
	return results
}
