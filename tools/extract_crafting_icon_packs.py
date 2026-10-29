#!/usr/bin/env python3
"""Expand and verify The Malefic Veil's five committed economy icon packs.

Run from the repository root:
  python tools/extract_crafting_icon_packs.py

Read-only with respect to the checked-in archives. Fails instead of silently
creating an incomplete or tampered art collection. Requires stdlib only.
"""
from pathlib import Path
from zipfile import ZipFile
import csv,hashlib,argparse
GROUPS=('shared_core','shared_equipment','unraveled_industry','penitent_ritual','begotten_biology')

def extract(repo, dest):
    root=repo/'assets/game/crafting'
    rows=list(csv.DictReader((root/'icons_index.csv').open(encoding='utf-8',newline='')))
    if len(rows)!=150 or len(set(r['id'] for r in rows))!=150:
        raise ValueError('Expected exactly 150 unique icon IDs')
    expect={r['id']:r for r in rows}
    for group in GROUPS:
        group_entries=[r for r in rows if r['pack']==group]
        if len(group_entries)!=30:raise ValueError('Missing sprites for '+group)
        path=root/'packs'/f'{group}_sprites_only.zip'
        with ZipFile(path) as archive:
            corrupt=archive.testzip()
            if corrupt:raise ValueError('Damaged ZIP entry '+corrupt)
            contents=set(archive.namelist())
            expected=set(r['entry'] for r in group_entries)
            if not contents.issuperset(expected) or contents-expected!={'ids.txt'}:
                raise ValueError('Missing or unexpected sprites in '+group)
            for r in group_entries:
                name=r['entry']
                if name!=f"icons/{r['id']}_small.png" or '..' in name:
                    raise ValueError('Invalid image entry '+name)
                raw=archive.read(name)
                if hashlib.sha256(raw).hexdigest()!=r['sha256']:
                    raise ValueError('Checksum mismatch '+r['id'])
                if raw[:8]!=b'\x89PNG\r\n\x1a\n':
                    raise ValueError('Not a PNG: '+r['id'])
                width=int.from_bytes(raw[16:20],'big')
                height=int.from_bytes(raw[20:24],'big')
                if (width,height)!=(128,128):
                    raise ValueError('Wrong PNG size '+r['id'])
                dst=dest/f"{r['id']}.png"
                dst.parent.mkdir(parents=True,exist_ok=True)
                dst.write_bytes(raw)
    if len(list(dest.glob('*.png')))!=150:
        raise ValueError('Extraction output incomplete')
    print('Verified 150 crafting sprites, five economy sheets:',dest)

if __name__=='__main__':
    ap=argparse.ArgumentParser()
    ap.add_argument('--repo',type=Path,default=Path('.'))
    ap.add_argument('--dest',type=Path,default=Path('build/generated/crafting-icons'))
    a=ap.parse_args();extract(a.repo,a.dest)
