"""Inventory actual game-model files without treating folder names or metadata as approval.
Optional --authoring-root includes the project's separate authoring checkout read-only.
"""
from pathlib import Path
import argparse,json,struct,hashlib,collections

ROOT=Path(__file__).resolve().parents[1]

def glb_metadata(file):
    data=file.read_bytes()
    if len(data)<28:raise ValueError('Truncated GLB')
    magic,version,length=struct.unpack_from('<III',data)
    if magic!=0x46546c67 or version!=2 or length!=len(data):raise ValueError('Invalid GLB header')
    size,kind=struct.unpack_from('<II',data,12)
    if kind!=0x4e4f534a or size>len(data)-20:raise ValueError('Invalid JSON chunk')
    doc=json.loads(data[20:20+size]);accessors=doc.get('accessors',[])
    primitives=[p for mesh in doc.get('meshes',[]) for p in mesh.get('primitives',[])]
    stored=sum(accessors[p['indices']]['count']//3 if 'indices' in p else accessors[p['attributes']['POSITION']]['count']//3 for p in primitives if p.get('mode',4)==4)
    return dict(stored_mesh_triangles=stored,mesh_count=len(doc.get('meshes',[])),node_count=len(doc.get('nodes',[])),material_count=len(doc.get('materials',[])),image_count=len(doc.get('images',[])),primitives_with_uv=sum('TEXCOORD_0' in p.get('attributes',{}) for p in primitives),primitive_count=len(primitives),animation_names=[a.get('name','unnamed') for a in doc.get('animations',[])])

def category(folder):
    if 'arsenal' in folder:return 'weapons'
    if 'aircraft' in folder:return 'aircraft'
    if 'structure' in folder:return 'permanent_structures'
    if any(token in folder for token in ('snap','foundation','material-refresh')):return 'construction_components'
    if folder=='beckenburg':return 'vegetation'
    if folder=='kenney':return 'environment_props'
    return 'other_models'

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--authoring-root',type=Path);args=parser.parse_args()
    locations=[('runtime',ROOT)]
    if args.authoring_root and args.authoring_root.resolve()!=ROOT:locations.append(('authoring_checkout',args.authoring_root.resolve()))
    models=[]
    for checkout,base in locations:
        for file in sorted((base/'assets/game').rglob('*.glb')):
            relative=file.relative_to(base);folder=relative.parts[2]
            row=dict(id=file.stem,category=category(folder),checkout=checkout,path=relative.as_posix(),sha256=hashlib.sha256(file.read_bytes()).hexdigest(),bytes=file.stat().st_size,status='NOT_AUDITED_FOR_CURRENT_PRODUCT')
            try:row['metadata']=glb_metadata(file)
            except Exception as error:row.update(status='METADATA_IMPORT_FAILED',error=str(error))
            models.append(row)
    fields=json.loads((ROOT/'data/field-deployable-asset-index.json').read_text())
    weapon_roster=json.loads((ROOT/'data/weapons.json').read_text())['weapons']
    inventory=dict(scope='Actual stored GLBs plus explicit deployable and weapon rosters; metadata and approved folder names are not certification.',expected_rosters=dict(deployables=len(fields['items']),weapons=len(weapon_roster)),models=models,counts=dict(collections.Counter(m['category'] for m in models)),separate_authoring_checkout_included=bool(args.authoring_root),terrain=dict(status='PROCEDURAL_RUNTIME_REQUIRES_AUDIT',sources=['gpu-client/src/main/java/veil/gpu/GpuContinentTerrain.java','src/main/java/veil/ContinentLayout.java'],note='No completed terrain-feature count inferred from procedural code.'),procedural_gameplay_models=dict(status='TEMPLATE_GEOMETRY_REQUIRES_AUDIT',sources=['src/main/java/veil/GameplayMeshes.java','src/main/java/veil/ThrownDeployableMeshes.java'],note='Runtime generated actors/items/devices are separate from authored GLB readiness.'))
    (ROOT/'reports/product-model-inventory.json').write_text(json.dumps(inventory,indent=2)+'\n')
    lines=['# Product-model audit inventory\n\nThis is an inventory, not a product-completion certificate. The deployable audit remains first priority.\n\n']
    lines+=['Explicit rosters: **21 field deployables** and **32 weapons**. Stored GLBs are counted by checkout and may include alternate versions of the same asset. No model receives approval from its directory name.\n\n| Family | Stored GLBs | Audit state |\n|---|---|---|\n']
    for family,count in sorted(inventory['counts'].items()):lines.append(f'| {family} | {count} | Current-product audit pending |\n')
    lines+=['\n## Ordered work\n\n1. Complete deployable geometry, articulation, packing, texture/organic-layer, purpose and runtime audits; repair or retain rejection.\n2. Audit all 32 weapon IDs against their actual mechanisms, materials, animations and current renderer bindings; reconcile alternate authored candidates without overwriting them.\n3. Audit permanent faction structures and construction components for usable placement, collision, materials and faction/function readability.\n4. Audit procedural terrain, vegetation and scenery in actual gameplay scenes, including traversal, scale and visual continuity.\n5. Audit remaining actors, pickups, medical/supply props, vehicles and aircraft against the functions implemented by the game.\n\nPrivate reference archives are excluded. Detailed paths, hashes and GLB metadata are in `product-model-inventory.json`. The main runtime and the separate authoring checkout remain distinct; nothing is silently copied or promoted.\n']
    (ROOT/'reports/product-model-audit-roadmap.md').write_text(''.join(lines),encoding='utf-8')
    print(json.dumps(dict(stored_glbs=len(models),families=inventory['counts'],expected_rosters=inventory['expected_rosters'])))

if __name__=='__main__':main()
