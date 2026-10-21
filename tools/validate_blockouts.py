"""Check blockout hierarchy, sockets, artifact hashes, and source-use provenance."""
import json,struct,hashlib,math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'assets/game/arsenal-blockouts'
report=json.loads((ROOT/'reports/arsenal_blockouts.json').read_text())
def glb(path):
    b=path.read_bytes();magic,version,length=struct.unpack_from('<3I',b)
    assert magic==0x46546c67 and version==2 and length==len(b),path
    n,kind=struct.unpack_from('<2I',b,12);assert kind==0x4e4f534a
    return json.loads(b[20:20+n]),b
assert len(report['weapons'])==32
checks=[]
for w in report['weapons']:
    j,b=glb(OUT/f'{w["id"]}.glb');nodes=j['nodes']
    names=[n.get('name','') for n in nodes]
    assert any(n.endswith('__ROOT') for n in names),w['id']
    assert any(n.endswith('hand_R_SOCKET') for n in names),w['id']
    assert any(n.endswith('ejection_SOCKET') for n in names),w['id']
    assert any('_SOCKET' in n and 'muzzle_' in n for n in names),w['id']
    assert any(n.endswith('_PIVOT') for n in names),w['id']
    assert all('source_part' not in n.get('extras',{}) for n in nodes),'Blockouts must be wholly original primitives'
    assert hashlib.sha256(b).hexdigest()==w['glb_sha256'],w['id']
    assert not w['detached_aabb_groups'],(w['id'],w['detached_aabb_groups'])
    _,static=glb(OUT/f'{w["id"]}-static.glb')
    checks.append({'id':w['id'],'hierarchical_bytes':len(b),'static_bytes':len(static),'components':len(w['roles']),'detached_aabb_groups':[]})
result={'stage':'primitive blockout validation','count':len(checks),'connection_check':'Conservative AABB proximity within 12mm; not exact triangle contact. Suspended field cores explicitly excluded.','source_meshes_in_public_blockouts':0,'items':checks}
(ROOT/'reports/blockout_validation.json').write_text(json.dumps(result,indent=2)+'\n')
print('PASS: 32 blockouts; hierarchies, pivots, sockets, hashes and conservative attachment graph')
