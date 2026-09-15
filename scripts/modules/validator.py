import requests
import re

TIMEOUT = 5


def is_active(url):
    """Check whether a stream is reachable using GET, with a safe fallback for servers that reject HEAD."""
    try:
        response = requests.get(
            url,
            timeout=TIMEOUT,
            allow_redirects=True,
            stream=True,
            headers={"User-Agent": "Mozilla/5.0"},
        )
        return response.status_code == 200
    except requests.RequestException:
        return False


def parse_m3u(file_path):
    """Parse EXTINF entries and preserve channel metadata plus the following stream URL."""
    channels = {}

    with open(file_path, "r", encoding="utf-8", errors="replace") as f:
        lines = [line.strip() for line in f if line.strip()]

    i = 0
    while i < len(lines):
        line = lines[i]
        if line.startswith("#EXTINF"):
            tvg_id_match = re.search(r'tvg-id="([^"]*)"', line)
            tvg_logo_match = re.search(r'tvg-logo="([^"]*)"', line)
            group_match = re.search(r'group-title="([^"]*)"', line)

            tvg_id = tvg_id_match.group(1).strip() if tvg_id_match else "unknown"
            tvg_logo = tvg_logo_match.group(1).strip() if tvg_logo_match else ""
            group_title = group_match.group(1).strip() if group_match else ""
            name = line.rsplit(",", 1)[1].strip() if "," in line else "Unknown Channel"

            url = ""
            j = i + 1
            while j < len(lines):
                candidate = lines[j]
                if candidate.startswith("#EXTINF"):
                    break
                if not candidate.startswith("#"):
                    url = candidate
                    break
                j += 1

            if url:
                channels.setdefault(tvg_id, []).append(
                    {
                        "extinf": line,
                        "url": url,
                        "nama": name,
                        "logo": tvg_logo,
                        "grup": group_title,
                        "tvgId": tvg_id,
                    }
                )

            i = max(i + 1, j)
        else:
            i += 1

    return channels
