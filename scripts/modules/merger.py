def merge_m3u(source_list):
    """
    Merges multiple M3U file paths into a single combined dictionary of channels.
    """
    merged_channels = {}
    for source in source_list:
        # Assuming parse_m3u returns dict: {tvg_id: [{'extinf': str, 'url': str}, ...]}
        # We need a way to combine these dictionaries
        # For this implementation, we will append streams for same tvg_id
        from scripts.modules.validator import parse_m3u
        current_channels = parse_m3u(source)
        for tvg_id, streams in current_channels.items():
            if tvg_id not in merged_channels:
                merged_channels[tvg_id] = []
            merged_channels[tvg_id].extend(streams)
    return merged_channels
