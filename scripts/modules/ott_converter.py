def filter_hls_only(channels):
    """
    Filters streams to only include those that are HLS (m3u8).
    """
    hls_channels = {}
    for tvg_id, streams in channels.items():
        hls_streams = [
            stream for stream in streams 
            if ".m3u8" in stream['url'].lower()
        ]
        if hls_streams:
            hls_channels[tvg_id] = hls_streams
    return hls_channels
