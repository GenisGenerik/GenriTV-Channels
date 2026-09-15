import sys
import os

# Add root to path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from scripts.modules.merger import merge_m3u
from scripts.modules.cleaner import apply_blocklist
from scripts.modules.ott_converter import filter_hls_only
from scripts.modules.validator import is_active
from scripts.modules.epg_manager import download_epg

# Configuration
INPUT_SOURCES = ["indonesia_premium.m3u"] # Simplified for now
BLOCKLIST = "blocklist.txt"
OUTPUT_MAIN = "generated/dhanytv.m3u"
OUTPUT_OTT = "generated/dhanytv-ott.m3u"
EPG_URL = "https://raw.githubusercontent.com/dhasap/dhanytv/main/epg.xml"
OUTPUT_EPG = "generated/epg.xml"

def save_m3u(channels, output_file):
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write("#EXTM3U\n")
        for tvg_id, streams in channels.items():
            # For simplicity, pick first active stream
            for stream in streams:
                if is_active(stream['url']):
                    f.write(f"{stream['extinf']}\n{stream['url']}\n")
                    break

def run_pipeline():
    print("Running pipeline...")
    # 1. Merge
    all_channels = merge_m3u(INPUT_SOURCES)
    
    # 2. Clean
    cleaned_channels = apply_blocklist(all_channels, BLOCKLIST)
    
    # 3. Save Main (Full)
    save_m3u(cleaned_channels, OUTPUT_MAIN)
    
    # 4. Save OTT (HLS only)
    ott_channels = filter_hls_only(cleaned_channels)
    save_m3u(ott_channels, OUTPUT_OTT)
    
    # 5. EPG
    print("Downloading EPG...")
    download_epg(EPG_URL, OUTPUT_EPG)
    
    print("Pipeline finished successfully.")

if __name__ == "__main__":
    run_pipeline()
