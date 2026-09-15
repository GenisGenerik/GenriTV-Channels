import re


def clean_channel_name(name):
    """Normalize channel names while preserving meaningful channel identity."""
    return re.sub(
        r"[^A-Z0-9]",
        "",
        name.upper()
        .replace("INDONESIA", "")
        .replace("IDN", "")
        .replace(" HD", "")
        .replace(" SD", "")
        .replace("CHANNEL", "")
        .replace("TV", ""),
    ).strip()


def is_national(grup, nama):
    """Determine whether a channel belongs to the national TV category."""
    g = (grup or "").lower()
    n = (nama or "").lower()
    national_keywords = [
        "rcti", "sctv", "indosiar", "trans", "antv", "tvone", "metro",
        "kompas", "mnc", "global", "gtv", "inews", "tvri", "rtv", "net",
        "garuda", "moji", "daai",
    ]
    return "nasional" in g or "indonesia" in g or any(k in n for k in national_keywords)


def calculate_score(channel):
    """Rank a channel without dropping channels solely because it scored lower."""
    score = 0

    if is_national(channel.get("grup"), channel.get("nama")):
        score += 50

    nama = channel["nama"].upper()
    if "HD" in nama:
        score += 20

    if channel.get("logo"):
        score += 15

    tvg_id = channel.get("tvgId")
    if tvg_id and tvg_id != "unknown":
        score += 10

    score += len(channel["urls"]) * 5
    return score


def merge_channels(all_channels):
    """Merge duplicate channel identities and retain the full source catalogue.

    The upstream dhanytv playlist can contain hundreds of channels. We deliberately
    avoid an arbitrary top-N truncation so categories such as National, Regional,
    News and Sports are populated from the complete source data.
    """
    merged = {}

    for channel in all_channels:
        cleaned_name = clean_channel_name(channel["nama"])
        tvg_id = channel.get("tvgId", "unknown")
        composite_key = tvg_id if tvg_id and tvg_id != "unknown" else cleaned_name

        if composite_key not in merged:
            merged[composite_key] = {
                "nama": channel["nama"],
                "urls": [],
                "logo": channel.get("logo"),
                "grup": channel.get("grup"),
                "tvgId": tvg_id,
            }

        if channel["url"] not in merged[composite_key]["urls"]:
            merged[composite_key]["urls"].append(channel["url"])

        if not merged[composite_key]["logo"] and channel.get("logo"):
            merged[composite_key]["logo"] = channel["logo"]
        if not merged[composite_key]["grup"] and channel.get("grup"):
            merged[composite_key]["grup"] = channel["grup"]

    final_list = list(merged.values())
    final_list.sort(key=lambda x: (calculate_score(x), x["nama"]), reverse=True)
    return final_list
