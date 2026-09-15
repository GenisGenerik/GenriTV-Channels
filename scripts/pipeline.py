import json
import os
import sys

import requests

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from scripts.modules.epg_manager import download_epg
from scripts.modules.processor import merge_channels
from scripts.modules.validator import is_active, parse_m3u

M3U_SOURCES = [
    "https://iptv-org.github.io/iptv/countries/id.m3u",
    "https://raw.githubusercontent.com/riotryulianto/iptv-playlists/master/indonesia.m3u",
    "https://raw.githubusercontent.com/dhasap/dhanytv/main/dhanytv.m3u",
]

OUTPUT_MAIN = "generated/dhanytv.m3u"
OUTPUT_OTT = "generated/dhanytv-ott.m3u"
OUTPUT_JSON = "generated/channels.json"
EPG_URL = "https://raw.githubusercontent.com/dhasap/dhanytv/main/epg.xml"
OUTPUT_EPG = "generated/epg.xml"


def download_playlists():
    os.makedirs("generated", exist_ok=True)
    downloaded_files = []

    for i, url in enumerate(M3U_SOURCES):
        output_path = f"generated/source_{i}.m3u"
        print(f"Downloading playlist from {url}...")
        try:
            response = requests.get(
                url,
                timeout=30,
                headers={"User-Agent": "Mozilla/5.0"},
            )
            response.raise_for_status()
            with open(output_path, "wb") as f:
                f.write(response.content)
            downloaded_files.append(output_path)
        except Exception as exc:
            print(f"Failed to download {url}: {exc}")

    return downloaded_files


def save_m3u(channels, output_file):
    with open(output_file, "w", encoding="utf-8") as f:
        f.write("#EXTM3U\n")
        for channel in channels:
            url = channel["urls"][0] if channel.get("urls") else ""
            if not url:
                continue
            f.write(
                f'#EXTINF:-1 tvg-id="{channel.get("tvgId", "")}" '
                f'group-title="{channel.get("grup", "")}" '
                f'tvg-logo="{channel.get("logo", "")}",{channel["nama"]}\n'
            )
            f.write(f"{url}\n")


def run_pipeline():
    print("Running pipeline...")
    playlist_files = download_playlists()

    all_raw_channels = []
    print("Validating URLs and building channel metadata...")

    for playlist_file in playlist_files:
        parsed = parse_m3u(playlist_file)
        for streams in parsed.values():
            for stream in streams:
                url = stream["url"]
                if not is_active(url):
                    print(f"Skipping unreachable URL: {url}")
                    continue

                all_raw_channels.append(
                    {
                        "nama": stream.get("nama", "Unknown Channel"),
                        "url": url,
                        "logo": stream.get("logo", ""),
                        "grup": stream.get("grup", ""),
                        "tvgId": stream.get("tvgId", "unknown"),
                    }
                )

    print(f"Validated channel streams: {len(all_raw_channels)}")

    final_channels = merge_channels(all_raw_channels)
    print(f"Final channels: {len(final_channels)}")

    with open(OUTPUT_JSON, "w", encoding="utf-8") as f:
        json.dump(final_channels, f, indent=2, ensure_ascii=False)
    print(f"Saved {OUTPUT_JSON}")

    save_m3u(final_channels, OUTPUT_MAIN)

    ott_channels = [
        channel
        for channel in final_channels
        if any(".m3u8" in url.lower() for url in channel.get("urls", []))
    ]
    save_m3u(ott_channels, OUTPUT_OTT)

    print("Downloading EPG...")
    if not download_epg(EPG_URL, OUTPUT_EPG):
        print("EPG download failed; leaving the previous file untouched if available.")

    print("Pipeline finished successfully.")


if __name__ == "__main__":
    run_pipeline()
