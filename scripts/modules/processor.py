import re

def clean_channel_name(name):
    """Normalizes channel name by removing common suffixes and non-alphanumeric chars."""
    return re.sub(r'[^A-Z0-9]', '', 
                  name.upper()
                  .replace("INDONESIA", "")
                  .replace("IDN", "")
                  .replace("HD", "")
                  .replace("SD", "")
                  .replace("CHANNEL", "")
                  .replace("TV", "")
                  ).strip()

def is_national(grup, nama):
    """Determines if a channel is national based on group or name keywords."""
    g = (grup or "").lower()
    n = (nama or "").lower()
    national_keywords = [
        "rcti", "sctv", "indosiar", "trans", "antv", "tvone", "metro", 
        "kompas", "mnc", "global", "gtv", "inews", "tvri", "rtv", "net"
    ]
    return "nasional" in g or "indo" in g or any(k in n for k in national_keywords)

def calculate_score(channel):
    """Calculates a quality score for a channel to ensure best ranking."""
    score = 0
    
    # National priority
    if is_national(channel.get('grup'), channel.get('nama')):
        score += 50
    
    # HD/SD Quality
    nama = channel['nama'].upper()
    if "HD" in nama:
        score += 20
    
    # Logo availability
    if channel.get('logo'):
        score += 15
        
    # EPG availability (tvgId exists and is not 'unknown')
    tvg_id = channel.get('tvgId')
    if tvg_id and tvg_id != 'unknown':
        score += 10
        
    # URL reliability/count (more URLs = more robust)
    score += len(channel['urls']) * 5
    
    return score

def merge_channels(all_channels):
    """
    Merges channels based on a composite key, ranks by quality, and limits to top 200.
    """
    merged = {}
    
    for channel in all_channels:
        cleaned_name = clean_channel_name(channel['nama'])
        tvg_id = channel.get('tvgId', 'unknown')
        composite_key = f"{cleaned_name}_{tvg_id}"
        
        if composite_key not in merged:
            merged[composite_key] = {
                'nama': channel['nama'],
                'urls': [],
                'logo': channel.get('logo'),
                'grup': channel.get('grup'),
                'tvgId': tvg_id
            }
        
        if channel['url'] not in merged[composite_key]['urls']:
            merged[composite_key]['urls'].append(channel['url'])
            
        if not merged[composite_key]['logo'] and channel.get('logo'):
            merged[composite_key]['logo'] = channel['logo']
        if not merged[composite_key]['grup'] and channel.get('grup'):
            merged[composite_key]['grup'] = channel['grup']

    # Convert to list
    final_list = list(merged.values())
    
    # Rank by score descending, then by name
    final_list.sort(key=lambda x: (calculate_score(x), x['nama']), reverse=True)
    
    # Return top 200
    return final_list[:200]
