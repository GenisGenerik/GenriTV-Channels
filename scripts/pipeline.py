import os
import re
import json
import requests
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from scripts.modules.validator import is_active, parse_m3u
from scripts.modules.processor import merge_channels
from scripts.modules.epg_manager import download_epg

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
    downloaded_files = []
    os.makedirs("generated", exist_ok=True)

    for i, url in enumerate(M3U_SOURCES):
        output_path = f"generated/source_{i}.m3u"
        print(f"Downloading playlist from {url}...")
        try:
            response = requests.get(url, timeout=30)
            response.raise_for_status()
            with open(output_path, "wb") as f:
                f.write(response.content)
            downloaded_files.append(output_path)
        except Exception as e:
            print(f"Failed to download {url}: {e}")

    return downloaded_files


def parse_extinf(extinf):
    attrs = {}
    for key, value in re.findall(r'(tvg-id|tvg-logo|group-title)="([^"]*)"', extinf):
        attrs[key] = value

    name = extinf.split(",", 1)[1].strip() if "," in extinf else ""
    return name, attrs


def build_raw_channels(playlist_files):
    all_raw_channels = []

    print("Validating URLs...")
    for file_path in playlist_files:
        parsed = parse_m3u(file_path)
        for tvg_id, streams in parsed.items():
            for stream in streams:
                url = stream["url"].strip()
                if not url or url.startswith("#"):
                    continue

                if not is_active(url):
                    print(f"Skipping dead URL: {url}")
                    continue

                name, attrs = parse_extinf(stream["extinf"])
                if not name:
                    continue

                all_raw_channels.append(
                    {
                        "nama": name,
                        "url": url,
                        "logo": attrs.get("tvg-logo", ""),
                        "grup": attrs.get("group-title", ""),
                        "tvgId": attrs.get("tvg-id") or tvg_id,
                    }
                )

    return all_raw_channels


def save_m3u(channels, output_file):
    with open(output_file, "w", encoding="utf-8") as f:
        f.write("#EXTM3U\n")
        for channel in channels:
            url = channel["urls"][0] if channel.get("urls") else ""
            if not url:
                continue
            f.write(
                f'#EXTINF:-1 tvg-id="{channel.get("tvgId", "")}" '
                f'tvg-logo="{channel.get("logo", "")}" '
                f'group-title="{channel.get("grup", "")}",{channel["nama"]}\n'
            )
            f.write(f"{url}\n")


def run_pipeline():
    print("Running pipeline...")

    playlist_files = download_playlists()
    raw_channels = build_raw_channels(playlist_files)
    final_channels = merge_channels(raw_channels)

    os.makedirs("generated", exist_ok=True)

    with open(OUTPUT_JSON, "w", encoding="utf-8") as f:
        json.dump(final_channels, f, indent=4, ensure_ascii=False)
    print(f"Saved {OUTPUT_JSON}: {len(final_channels)} channels")

    save_m3u(final_channels, OUTPUT_MAIN)

    ott_channels = [
        c for c in final_channels
        if any(".m3u8" in url.lower() for url in c.get("urls", []))
    ]
    save_m3u(ott_channels, OUTPUT_OTT)

    print("Downloading EPG...")
    download_epg(EPG_URL, OUTPUT_EPG)

    print("Pipeline finished successfully.")


if __name__ == "__main__":
    run_pipeline()
