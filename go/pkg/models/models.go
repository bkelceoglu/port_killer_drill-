package models

type PortRange struct {
	Start uint32
	End   uint32
}

type Handshake struct {
	Type     string `json:"type"`
	Hostname string `json:"hostname"`
}

type Alert struct {
	Type        string `json:"type"`
	Hostname    string `json:"hostname"`
	Pid         int32  `json:"pid"`
	ProcessName string `json:"name"`
	Port        uint32 `json:"port"`
	Protocol    string `json:"protocol"`
	IsKilled    bool   `json:"is_killed"`
}
