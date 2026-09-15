import json
import os
from scripts.modules.validator import parse_m3u
from scripts.modules.processor import merge_channels

INPUTS = [
    "generated/source_0.m3u",
    "generated/source_1.m3u",
    "generated/source_2.m3u",
]
OUTPUT = "generated/channels.json"


def main():
    raw = []
    for path in INPUTS:
        if not os.path.exists(path):
            continue
        parsed = parse_m3u(path)
        for streams in parsed.values():
            for stream in streams:
                raw.append({
                    "nama": stream["nama"],
                    "url": stream["url"],
                    "logo": stream.get("logo", ""),
                    "grup": stream.get("grup", ""),
                    "tvgId": stream.get("tvgId", "unknown"),
                })

    channels = merge_channels(raw)
    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
    with open(OUTPUT, "w", encoding="utf-8") as f:
        json.dump(channels, f, ensure_ascii=False, indent=2)
    print(f"Generated {OUTPUT}: {len(channels)} channels")


if __name__ == "__main__":
    main()
