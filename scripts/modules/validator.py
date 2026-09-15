import requests
import re

TIMEOUT = 3  # Detik

def is_active(url):
    try:
        # Gunakan HEAD untuk kecepatan
        return requests.head(url, timeout=TIMEOUT, allow_redirects=True).status_code == 200
    except:
        return False

def parse_m3u(file_path):
    channels = {}
    
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    for i, line in enumerate(lines):
        if line.startswith("#EXTINF"):
            # Ekstrak tvg-id
            match = re.search(r'tvg-id="([^"]+)"', line)
            tvg_id = match.group(1) if match else "unknown"
            
            # Simpan baris EXTINF dan URL berikutnya
            if tvg_id not in channels:
                channels[tvg_id] = []
            if i + 1 < len(lines):
                channels[tvg_id].append({'extinf': line.strip(), 'url': lines[i+1].strip()})
    return channels
