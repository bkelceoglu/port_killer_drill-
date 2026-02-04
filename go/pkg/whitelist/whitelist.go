package whitelist

import (
	"bufio"
	"os"
	"port_scanner/agent/pkg/models"
	"strconv"
	"strings"
)

func Load(filename string) []models.PortRange {
	file, err := os.Open(filename)
	if err != nil {
		return nil
	}
	defer file.Close()

	var rules []models.PortRange
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}

		if strings.Contains(line, "-") {
			parts := strings.Split(line, "-")
			if len(parts) == 2 {
				start, err1 := strconv.ParseUint(strings.TrimSpace(parts[0]), 10, 32)
				end, err2 := strconv.ParseUint(strings.TrimSpace(parts[1]), 10, 32)
				if err1 == nil && err2 == nil {
					rules = append(rules, models.PortRange{Start: uint32(start), End: uint32(end)})
				}
			}
		} else {
			port, err := strconv.ParseUint(line, 10, 32)
			if err == nil {
				rules = append(rules, models.PortRange{Start: uint32(port), End: uint32(port)})
			}
		}
	}
	return rules
}

func IsWhitelisted(port uint32, rules []models.PortRange) bool {
	for _, r := range rules {
		if port >= r.Start && port <= r.End {
			return true
		}
	}
	return false
}
