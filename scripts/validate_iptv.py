import sys
import os

# Tambahkan direktori root ke path agar bisa import modules
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from scripts.modules.validator import parse_m3u, is_active

# Konfigurasi
INPUT_FILE = "indonesia_premium.m3u"
OUTPUT_FILE = "generated/final_playlist.m3u"

def process_playlist():
    print(f"Membaca {INPUT_FILE}...")
    channels = parse_m3u(INPUT_FILE)
    
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        f.write("#EXTM3U\n")
        
        for tvg_id, stream_list in channels.items():
            found = False
            for stream in stream_list:
                print(f"Testing {tvg_id} - {stream['url']}...", end=" ")
                if is_active(stream['url']):
                    print("OK")
                    f.write(f"{stream['extinf']}\n{stream['url']}\n")
                    found = True
                    break # Gunakan stream pertama yang aktif
                else:
                    print("Mati")
            if not found:
                print(f"Channel {tvg_id} tidak ada yang aktif.")

    print(f"\nValidasi selesai. Playlist bersih disimpan di: {OUTPUT_FILE}")

if __name__ == "__main__":
    process_playlist()
