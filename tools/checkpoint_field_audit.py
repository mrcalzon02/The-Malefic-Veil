"""Reconcile the authoritative roster with the inspected current pilot exports."""
import hashlib,json,struct
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
CURRENT={'thrown-unraveled-wall':('field-bastion-r5','bastion-panel'),
         'thrown-unraveled-barricade':('field-razorframe-r3','razorframe'),
         'thrown-unraveled-turret':('field-pilots-r10','sentry-mki'),
         'thrown-unraveled-health':('field-medic-r7','field-medic'),
         'thrown-penitent-miasma':('field-pilots-r13','censer-of-dread'),
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
    item['renders']={frame['clip'].lower():dict(path=(gpu/(frame['clip'].lower()+'.png')).as_posix(),sha256=sha(ROOT/gpu/(frame['clip'].lower()+'.png'))) for frame in gpu_report['evidence']}
    # Old render sequences are not evidence for the new binary.
    item.pop('animation_sequences',None)
    item['remaining']=['Purpose/art acceptance and final surface refinement','Full articulation/packing clearance beyond declared pairs','Persistent damage, deterioration and convincing failure-specific destruction poses','Live world gameplay and multiplayer acceptance beyond socket regression']
    if identifier=='bastion-panel':item['remaining'].append('Lateral rail nesting, locks and load path remain unproven')
    if identifier=='razorframe':item['remaining']+=['Full part clearance, top-rail support and drive transmission remain unproven']
    if identifier=='censer-of-dread':item['remaining'].append('Packing approximately 0.557 x 0.557 x 0.499 m still exceeds the 0.5 m target')
    if identifier=='regeneration-heart':item['remaining'].append('Organic surface/color transitions and sparse dark atlas samples still need inspection')
    evidence=[dict(kind='gpu_upload_and_pose',report=(gpu/'gpu-audit.json').as_posix(),status=gpu_report['status'])]
    for suffix in ('attachments','clearance','motion-inspection','packing','linkage'):
        report=ROOT/'reports'/(revision+'-'+suffix+'.json')
        if report.exists():
            for test in json.loads(report.read_text()):
                if test['id']==identifier:
                    assert test['asset_sha256']==digest
                    evidence.append(dict(kind=suffix,report=report.relative_to(ROOT).as_posix(),status=test.get('status','MEASURED_ENVELOPE')))
    records.append(dict(id=item['id'],pilot=identifier,asset_sha256=digest,triangles=item['triangles'],runtime_normal_attributes=0,normal_maps=0,vertex_floats=9,texture_size=[256,256],evidence=evidence,production_complete=False,remaining=item['remaining']))
index['counts'].update(refined_pilots=6,candidate_repair_required=0,redesign_required=15)
for item in index['items']:
    if item['id'] in CURRENT:item.pop('mechanism_study',None)
index['counts'].pop('rejected_mechanism_studies',None)
write(ROOT/'data/field-deployable-asset-index.json',index)
write(ROOT/'reports/current-field-audit.json',dict(status='SCOPED_CHECKS_ONLY_ART_AND_GAMEPLAY_PENDING',render_contract='design/FIELD_RENDER_CONTRACT.md',pilots=records))
lines=['# Current field asset audit\n\nSix refined pilots; 15 original candidates still require independent redesign. Zero production approvals.\n\nThe current exports contain no normal/tangent attributes, morph normals or normal maps. Offline form shading and AO share the existing 256 px color atlas. The runtime vertex layout remains nine floats and the shader adds no texture samples. Corrected atlas display encoding improves readability; this does not certify the art.\n\n']
for item in records:
    lines+=['## '+item['pilot']+'\n\n',f"{item['triangles']:,} triangles. Current binary SHA-256: `{item['asset_sha256']}`.\n\n"]
    lines+=['- '+e['kind']+': '+e['status']+' (`'+e['report']+'`).\n' for e in item['evidence']]
    lines+=['\nRemaining: '+ '; '.join(item['remaining'])+'.\n\n']
lines+=['Java validation on these exports: FieldPilotAnimationTest passed the four earlier pilots and both new studies, including absence of normals/maps, nine unique-channel clips, texture assignment, 31 deployment floor samples and actual TRS/morph changes. ThrownActivationTest passed all 21 craft/throw and activation-boundary cases, including turret target updates between attacks. ThrownDeployablesTest, ArsenalIntegrationTest and ArenaSystemsTest passed. FieldReplicationTest passed real sockets for legacy/current snapshots, tracking/stock/clocks, an active late join, collapse snapshots, wire craft/throw, and two-peer collapse/removal. All 21 role tests cover immediate effect shutdown, 800 ms visual lifetime and one harmless burst/removal, including exhausted shields. The actual field asset loader selects DESTROYED from the host destruction age for the staged fixtures.\n\nThe Medic hose stays within 0.34 mm of its sockets over 61 samples in each of nine states; the injector/case clearance contract passes 8,760 pair samples. Heart cradle contacts pass the declared 292 pair samples. Censer r13 expanded checks cover 31 deployment samples of satellites against main vessels, bands/ribs/lid ribs, slides, feet and one another, plus central vessels against slides/feet. The expanded audit found 214 intersections in the superseded r9 study; r10/r11 also failed their narrower tested scope. r13 passes this expanded scope, but its packed width/depth still exceed the target. None of these checks establishes clearance for undeclared part pairs.\n']
lines+=['\n## Latest work\n\nBastion r5: 3,468 triangles; packed approximately 0.460 x 0.500 x 0.495 m; no armor tile intersections in 31,540 declared pair samples. Its operating pressure piston reacts to authoritative damage events; destruction now uses column sag/lean instead of the packed transform. Lateral slide nesting/load paths and other part pairs remain unproven.\n\nRazorframe r3: 2,492 triangles; five rigid scissor bays, bored guide sleeves and nested vertical posts. The exact exported rod endpoints remain within 0.014 mm of declared guide centers across 121 samples per state in all nine states. Packed envelope approximately 0.500 x 0.485 x 0.370 m meets the target. The expanded six-state clearance check passes 89,936 directed pair samples across opposing rod layers, rod/guide housings, header/hardware, sleeve/post and sleeve/sleeve pairs. r1 failed 2,920 rod/solid-guide pair samples; r2 retained four packed sleeve/neighbouring-post contacts, repaired with wider packed post spacing in r3. Full clearance, top-rail supports/nesting, drive transmission, failure motion and art acceptance remain pending.\n\nThe GPU audit camera now frames the full active bounds and keeps that camera fixed across states. This fixes clipped wall evidence without hiding packing differences. Both current studies pass nine-state GPU checks. No runtime normal attributes/maps were added.\n']
(ROOT/'reports/current-field-audit.md').write_text(''.join(lines),encoding='utf-8')
table=['# Field deployable asset index\n\nContinued beyond the earlier cutoff at the user request. Six refined pilots await review; 15 require independent redesign; zero production complete.\n\n| # | Faction | Item | Status |\n|---|---|---|---|\n']
table += [f"| {i['index']} | {i['faction']} | {i['name']} | {i['status']} |\n" for i in index['items']]
table+=['\nCurrent scoped evidence and unresolved issues: [current-field-audit.md](current-field-audit.md).\n']
(ROOT/'reports/field-deployable-asset-index.md').write_text(''.join(table),encoding='utf-8')
print('CURRENT_FIELD_CHECKPOINT',len(records),'pilots; zero production approvals')
