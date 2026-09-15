import sys
import os
import requests
import json

# Add root to path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from scripts.modules.merger import merge_m3u
from scripts.modules.cleaner import apply_blocklist
from scripts.modules.ott_converter import filter_hls_only
from scripts.modules.validator import is_active
from scripts.modules.epg_manager import download_epg
from scripts.modules.processor import merge_channels

# Configuration
M3U_SOURCES = [
    "https://iptv-org.github.io/iptv/countries/id.m3u",
    "https://raw.githubusercontent.com/riotryulianto/iptv-playlists/master/indonesia.m3u",
    "https://raw.githubusercontent.com/dhasap/dhanytv/main/dhanytv.m3u"
]
BLOCKLIST = "blocklist.txt"
OUTPUT_MAIN = "generated/dhanytv.m3u"
OUTPUT_OTT = "generated/dhanytv-ott.m3u"
OUTPUT_JSON = "generated/channels.json"
EPG_URL = "https://raw.githubusercontent.com/dhasap/dhanytv/main/epg.xml"
OUTPUT_EPG = "generated/epg.xml"

def download_playlists():
    downloaded_files = []
    for i, url in enumerate(M3U_SOURCES):
        output_path = f"generated/source_{i}.m3u"
        print(f"Downloading playlist from {url}...")
        try:
            response = requests.get(url)
            response.raise_for_status()
            with open(output_path, 'wb') as f:
                f.write(response.content)
            downloaded_files.append(output_path)
        except Exception as e:
            print(f"Failed to download {url}: {e}")
    return downloaded_files

def save_m3u(channels, output_file):
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write("#EXTM3U\n")
        for channel in channels:
            for url in channel['urls']:
                # Already validated during ingestion, just write it
                f.write(f"#EXTINF:-1 tvg-id=\"{channel.get('tvgId', '')}\" group-title=\"{channel.get('grup', '')}\" tvg-logo=\"{channel.get('logo', '')}\",{channel['nama']}\n{url}\n")
                break

def run_pipeline():
    print("Running pipeline...")
    
    # 1. Download
    playlist_files = download_playlists()
        
    # 2. Merge (Old way to get raw list)
    from scripts.modules.merger import parse_m3u # Import here to avoid circular
    all_raw_channels = []
    print("Validating URLs...")
    for f in playlist_files:
        parsed = parse_m3u(f)
        for tvg_id, streams in parsed.items():
            for stream in streams:
                # Validate URL before adding
                if is_active(stream['url']):
                    # Map back to dict expected by processor
                    all_raw_channels.append({
                        'nama': stream['extinf'].split(',')[-1],
                        'url': stream['url'],
                        'logo': '', # Need to parse this out from extinf if possible
                        'grup': '',
                        'tvgId': tvg_id
                    })
                else:
                    print(f"Skipping dead URL: {stream['url']}")
    
    # 3. Process/Clean/Deduplicate
    final_channels = merge_channels(all_raw_channels)
    
    # 4. Save JSON
    with open(OUTPUT_JSON, 'w', encoding='utf-8') as f:
        json.dump(final_channels, f, indent=4)
    print(f"Saved {OUTPUT_JSON}")
    
    # 5. Save Main (Full)
    save_m3u(final_channels, OUTPUT_MAIN)
    
    # 6. Save OTT (HLS only - simplistic filter)
    ott_channels = [c for c in final_channels if any(u.endswith('.m3u8') for u in c['urls'])]
    save_m3u(ott_channels, OUTPUT_OTT)
    
    # 7. EPG
    print("Downloading EPG...")
    download_epg(EPG_URL, OUTPUT_EPG)
    
    print("Pipeline finished successfully.")

if __name__ == "__main__":
    run_pipeline()
