import json,struct
from pathlib import Path
R=Path(__file__).resolve().parents[1]
items=[]
for p in sorted((R/'assets/game/arsenal').glob('*.glb')):
    b=p.read_bytes();n=struct.unpack_from('<I',b,12)[0];j=json.loads(b[20:20+n])
    clips={a['name']:len(a['channels']) for a in j.get('animations',[])}
    assert set(clips)=={'idle','fire','alternate','reload'},(p.name,clips)
    assert all(n>0 for n in clips.values()),p.name
    assert j.get('images') and all('bufferView' in im for im in j['images']),p.name
    assert all('TEXCOORD_0' in pr['attributes'] for me in j['meshes'] for pr in me['primitives']),p.name
    items.append({'id':p.stem,'clips':clips,'embedded_images':len(j['images'])})
assert len(items)==32,len(items)
(R/'reports/working_asset_validation.json').write_text(json.dumps({'count':32,'items':items},indent=2)+'\n')
print('PASS: all 32 animated GLBs have four nonempty clips, UVs and embedded working textures')
