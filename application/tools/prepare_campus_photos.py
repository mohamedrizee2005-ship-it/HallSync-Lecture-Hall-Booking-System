"""Restore licensed, unchanged campus photographs from their recorded source URLs."""
import json,urllib.request,urllib.error,time
from pathlib import Path
root=Path(__file__).resolve().parents[1]
data=json.loads((root/'src/main/resources/campus.json').read_text(encoding='utf-8'))
assets=root/'src/main/resources/web/assets'
for photo in data['photos']:
    target=assets/photo['file']
    if target.exists():
        assert target.read_bytes().startswith(b'\xff\xd8'), 'Invalid bundled JPEG: '+photo['file']
        continue
    request=urllib.request.Request(photo['download'],headers={'User-Agent':'HallSync/2.1 (University campus booking project; Wikimedia licensed asset restore)'})
    for attempt in range(2):
        try:
            with urllib.request.urlopen(request,timeout=45) as response:
                body=response.read(5_000_001)
            break
        except urllib.error.HTTPError as error:
            if error.code!=429 or attempt:raise
            delay=int(error.headers.get('Retry-After','35'))
            if delay>60:raise
            time.sleep(max(delay,2))
    assert body.startswith(b'\xff\xd8') and len(body)<=5_000_000,'Invalid campus JPEG download'
    target.write_bytes(body)
    print('Restored licensed campus photo: '+photo['file'])
    time.sleep(3)
