def apply_blocklist(channels, blocklist_file):
    """
    Removes channels from the dictionary based on a blocklist file.
    """
    if not os.path.exists(blocklist_file):
        return channels
    
    with open(blocklist_file, 'r', encoding='utf-8') as f:
        blocklist = set(line.strip() for line in f if line.strip())
        
    filtered_channels = {
        tvg_id: streams 
        for tvg_id, streams in channels.items() 
        if tvg_id not in blocklist
    }
    return filtered_channels

import os
