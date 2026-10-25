"""Reconcile the authoritative roster with the inspected current pilot exports."""
import hashlib,json,struct
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
CURRENT={'thrown-unraveled-turret':('field-pilots-r9','sentry-mki'),
         'thrown-unraveled-health':('field-medic-r7','field-medic'),
         'thrown-penitent-miasma':('field-pilots-r9','censer-of-dread'),
         'thrown-begotten-health':('field-pilots-r9','regeneration-heart')}
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def write(path,value):path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
index=json.loads((ROOT/'data/field-deployable-asset-index.json').read_text())
records=[]
for item in index['items']:
    if item['id'] not in CURRENT:continue
    revision,identifier=CURRENT[item['id']];folder=Path('build/candidates')/revision/identifier
    asset=ROOT/folder/(identifier+'.glb');raw=asset.read_bytes();length=struct.unpack_from('<I',raw,12)[0];doc=json.loads(raw[20:20+length]);digest=sha(asset)
    for mesh in doc['meshes']:
        for prim in mesh['primitives']:
            assert not {'NORMAL','TANGENT'}&prim['attributes'].keys()
            for morph in prim.get('targets',[]):assert not {'NORMAL','TANGENT'}&morph.keys()
    assert all('normalTexture' not in mat for mat in doc['materials'])
    gpu=Path('previews')/revision/identifier/'gpu';gpu_report=json.loads((ROOT/gpu/'gpu-audit.json').read_text());assert gpu_report['asset_sha256']==digest
    manifest=next(x for x in json.loads((asset.parent.parent/'manifest.json').read_text()) if x['id']==identifier)
    prior={k:item[k] for k in ('candidate','asset_sha256','status') if k in item}
    if prior.get('candidate')!=str(folder/(identifier+'.glb')).replace('\\','/'):
        item.setdefault('superseded_candidates',[]).append(prior)
    item.update(status='REFINED_PILOT_REVIEW_REQUIRED',production_complete=False,
        candidate=(folder/(identifier+'.glb')).as_posix(),asset_sha256=digest,
        modeling_source=(folder/(identifier+'.blend')).as_posix(),animated_source=(folder/(identifier+'-animated.blend')).as_posix(),
        triangles=manifest['triangles'],technical_review='reports/current-field-audit.json',refinement_review='reports/current-field-audit.md')
    item['renders']={name:dict(path=(gpu/(name+'.png')).as_posix(),sha256=sha(ROOT/gpu/(name+'.png'))) for name in ('active','packed','deploying','operating')}
    # Old render sequences are not evidence for the new binary.
    item.pop('animation_sequences',None)
    item['remaining']=['Purpose/art acceptance and final surface refinement','Full articulation/packing clearance beyond declared pairs','Persistent damage, deterioration and destruction integration','Live world gameplay and multiplayer acceptance']
    if identifier=='censer-of-dread':item['remaining'].append('Packing depth 0.593 m exceeds the 0.5 m target')
    if identifier=='sentry-mki':item['remaining'].append('Packing width 0.511 m slightly exceeds the 0.5 m target')
    if identifier=='regeneration-heart':item['remaining'].append('Organic surface/color transitions and sparse dark atlas samples still need inspection')
    evidence=[dict(kind='gpu_upload_and_pose',report=(gpu/'gpu-audit.json').as_posix(),status=gpu_report['status'])]
    for suffix in ('attachments','clearance','motion-inspection'):
        report=ROOT/'reports'/(revision+'-'+suffix+'.json')
        if report.exists():
            for test in json.loads(report.read_text()):
                if test['id']==identifier:
                    assert test['asset_sha256']==digest
                    evidence.append(dict(kind=suffix,report=report.relative_to(ROOT).as_posix(),status=test['status']))
    records.append(dict(id=item['id'],pilot=identifier,asset_sha256=digest,triangles=item['triangles'],runtime_normal_attributes=0,normal_maps=0,vertex_floats=9,texture_size=[256,256],evidence=evidence,production_complete=False,remaining=item['remaining']))
index['counts'].update(refined_pilots=4,candidate_repair_required=0,redesign_required=17)
write(ROOT/'data/field-deployable-asset-index.json',index)
write(ROOT/'reports/current-field-audit.json',dict(status='SCOPED_CHECKS_ONLY_ART_AND_GAMEPLAY_PENDING',render_contract='design/FIELD_RENDER_CONTRACT.md',pilots=records))
lines=['# Current field asset audit\n\nFour refined pilots; 17 original candidates still require independent redesign. Zero production approvals.\n\nThe current exports contain no normal/tangent attributes, morph normals or normal maps. Offline form shading and AO share the existing 256 px color atlas. The runtime vertex layout remains nine floats and the shader adds no texture samples. Corrected atlas display encoding improves readability; this does not certify the art.\n\n']
for item in records:
    lines+=['## '+item['pilot']+'\n\n',f"{item['triangles']:,} triangles. Current binary SHA-256: `{item['asset_sha256']}`.\n\n"]
    lines+=['- '+e['kind']+': '+e['status']+' (`'+e['report']+'`).\n' for e in item['evidence']]
    lines+=['\nRemaining: '+ '; '.join(item['remaining'])+'.\n\n']
lines+=['Java validation on these exports: FieldPilotAnimationTest passed all four, including absence of normals/maps, nine unique-channel clips, texture assignment, 31 deployment floor samples and actual TRS/morph changes. ThrownActivationTest passed all 21 craft/throw and activation-boundary cases, including turret target updates between attacks. ThrownDeployablesTest, ArsenalIntegrationTest and ArenaSystemsTest passed. FIELD tracking packet compatibility and late-join behavior still need a dedicated regression.\n\nThe Medic hose stays within 0.34 mm of its sockets over 61 samples in each of nine states; the injector/case clearance contract passes 8,760 pair samples. Heart cradle contacts pass the declared 292 pair samples. Censer checks cover 31 samples of satellite bowls against declared vessels/slides. None of these checks establishes clearance for undeclared part pairs.\n']
(ROOT/'reports/current-field-audit.md').write_text(''.join(lines),encoding='utf-8')
table=['# Field deployable asset index\n\nActive development. Stop condition: 50% remaining in the selected five-hour allowance.\n\n21 required: 0 production complete, 4 refined pilots awaiting review, 17 awaiting independent redesign.\n\n| # | Faction | Item | Status |\n|---|---|---|---|\n']
table += [f"| {i['index']} | {i['faction']} | {i['name']} | {i['status']} |\n" for i in index['items']]
table+=['\nCurrent scoped evidence and unresolved issues: [current-field-audit.md](current-field-audit.md).\n']
(ROOT/'reports/field-deployable-asset-index.md').write_text(''.join(table),encoding='utf-8')
print('CURRENT_FIELD_CHECKPOINT',len(records),'pilots; zero production approvals')
