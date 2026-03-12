package whitelist

import (
	"crypto/md5"
	"encoding/hex"
	"encoding/json"
	"io"
	"os"
	"port_scanner/agent/pkg/models"
	"strconv"
	"strings"
)

type Config struct {
	Whitelist []string `json:"whitelist"`
	Blacklist []string `json:"blacklist"`
}

func GetMD5(filename string) (string, error) {
	file, err := os.Open(filename)
	if err != nil {
		return "", err
	}
	defer file.Close()

	hash := md5.New()
	if _, err := io.Copy(hash, file); err != nil {
		return "", err
	}
	return hex.EncodeToString(hash.Sum(nil)), nil
}

func LoadJSON(filename string) ([]models.PortRange, []models.PortRange, error) {
	data, err := os.ReadFile(filename)
	if err != nil {
		return nil, nil, err
	}

	var config Config
	if err := json.Unmarshal(data, &config); err != nil {
		return nil, nil, err
	}

	return parseRanges(config.Whitelist), parseRanges(config.Blacklist), nil
}

func parseRanges(raw []string) []models.PortRange {
	var ranges []models.PortRange
	for _, entry := range raw {
		trimmed := strings.TrimSpace(entry)
		if strings.Contains(trimmed, "-") {
			parts := strings.Split(trimmed, "-")
			if len(parts) == 2 {
				start, err1 := strconv.ParseUint(strings.TrimSpace(parts[0]), 10, 32)
				end, err2 := strconv.ParseUint(strings.TrimSpace(parts[1]), 10, 32)
				if err1 == nil && err2 == nil {
					ranges = append(ranges, models.PortRange{Start: uint32(start), End: uint32(end)})
				}
			}
		} else {
			port, err := strconv.ParseUint(trimmed, 10, 32)
			if err == nil {
				ranges = append(ranges, models.PortRange{Start: uint32(port), End: uint32(port)})
			}
		}
	}
	return ranges
}

func IsPortInRanges(port uint32, ranges []models.PortRange) bool {
	for _, r := range ranges {
		if port >= r.Start && port <= r.End {
			return true
		}
	}
	return false
}
