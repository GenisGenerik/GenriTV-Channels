import requests
import os

def download_epg(url, output_path):
    """
    Downloads EPG file from URL and saves it.
    """
    try:
        response = requests.get(url, timeout=30)
        if response.status_code == 200:
            with open(output_path, 'wb') as f:
                f.write(response.content)
            return True
    except Exception as e:
        print(f"Failed to download EPG from {url}: {e}")
    return False

# For simplicity, we can implement a basic merger if needed later.
# For now, this module will focus on downloading the EPG.
