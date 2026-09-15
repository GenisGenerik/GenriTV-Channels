import re


def clean_channel_name(name):
    return re.sub(r"[^A-Z0-9]", "", name.upper().replace("INDONESIA", "").replace("IDN", "").replace(" HD", "").replace(" SD", "").replace("CHANNEL", "").replace("TV", "")).strip()


def is_national(grup, nama):
    g = (grup or "").lower()
    n = (nama or "").lower()
    keywords = ["rcti", "sctv", "indosiar", "trans", "antv", "tvone", "metro", "kompas", "mnc", "global", "gtv", "inews", "tvri", "rtv", "net", "garuda", "moji", "daai"]
    return "nasional" in g or "indonesia" in g or any(k in n for k in keywords)


def calculate_score(channel):
    score = 50 if is_national(channel.get("grup"), channel.get("nama")) else 0
    if "HD" in channel["nama"].upper(): score += 20
    if channel.get("logo"): score += 15
    if channel.get("tvgId") and channel.get("tvgId") != "unknown": score += 10
    return score + len(channel["urls"]) * 5


def merge_channels(all_channels):
    merged = {}
    for channel in all_channels:
        tvg_id = channel.get("tvgId", "unknown")
        key = tvg_id if tvg_id != "unknown" else clean_channel_name(channel["nama"])
        if key not in merged:
            merged[key] = {
                "nama": channel["nama"],
                "urls": [],
                "logo": channel.get("logo"),
                "grup": channel.get("grup"),
                "tvgId": tvg_id,
                "headers": [],
            }
        if channel["url"] not in merged[key]["urls"]:
            merged[key]["urls"].append(channel["url"])
            merged[key]["headers"].extend(channel.get("headers", []))
        if not merged[key]["logo"] and channel.get("logo"): merged[key]["logo"] = channel["logo"]
        if not merged[key]["grup"] and channel.get("grup"): merged[key]["grup"] = channel["grup"]
    result = list(merged.values())
    for channel in result:
        channel["headers"] = list(dict.fromkeys(channel["headers"]))
    return sorted(result, key=lambda x: (calculate_score(x), x["nama"]), reverse=True)
