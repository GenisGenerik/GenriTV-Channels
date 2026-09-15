import re
import requests

TIMEOUT = 8


def is_active(url):
    try:
        response = requests.get(
            url,
            timeout=TIMEOUT,
            allow_redirects=True,
            stream=True,
            headers={
                "User-Agent": "Mozilla/5.0 GenriTV/1.0",
                "Accept": "*/*",
                "Range": "bytes=0-2048",
            },
        )
        return response.status_code in (200, 206)
    except requests.RequestException:
        return False


def parse_m3u(file_path):
    channels = {}
    with open(file_path, "r", encoding="utf-8-sig", errors="ignore") as f:
        lines = [line.rstrip("\r\n") for line in f]

    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if not line.startswith("#EXTINF"):
            i += 1
            continue

        tvg_id_match = re.search(r'tvg-id="([^"]*)"', line)
        tvg_logo_match = re.search(r'tvg-logo="([^"]*)"', line)
        group_match = re.search(r'group-title="([^"]*)"', line)
        tvg_id = tvg_id_match.group(1).strip() if tvg_id_match else "unknown"
        tvg_logo = tvg_logo_match.group(1).strip() if tvg_logo_match else ""
        group_title = group_match.group(1).strip() if group_match else ""
        name = line.rsplit(",", 1)[1].strip() if "," in line else tvg_id

        directives = []
        url = ""
        j = i + 1
        while j < len(lines):
            candidate = lines[j].strip()
            if candidate.startswith("#EXTINF"):
                break
            if candidate.startswith(("#EXTVLCOPT:", "#KODIPROP:", "#EXTHTTP:")):
                directives.append(candidate)
            elif candidate and not candidate.startswith("#"):
                url = candidate
                break
            j += 1

        if url:
            channels.setdefault(tvg_id, []).append({
                "extinf": line,
                "url": url,
                "nama": name,
                "logo": tvg_logo,
                "grup": group_title,
                "tvgId": tvg_id,
                "headers": directives,
            })
        i = max(i + 1, j)

    return channels
