"""Reconcile the sweep queue against hash-matching reviewed packages."""
import json,hashlib
from pathlib import Path
R=Path(__file__).resolve().parents[1];weapons=json.loads((R/'data/weapons.json').read_text())['weapons'];rows=[]
for index,w in enumerate(weapons):
    row={'weapon_id':w['id'],'name':w['name'],'access':'world pickup' if index<8 else 'base/faction crafted','status':'PENDING'}
    for asset in sorted((R/'assets/game/arsenal-refined-approved').glob(w['id']+'-*.glb')):
        review=R/'reports/structure-art-reviews'/(asset.stem+'.json')
        if not review.exists():continue
        v=json.loads(review.read_text());sha=hashlib.sha256(asset.read_bytes()).hexdigest()
        if v.get('decision')=='pass' and v.get('asset_sha256')==sha:row.update(status='REFINED_PROTOTYPE',asset=asset.relative_to(R).as_posix(),sha256=sha)
    repair=R/'reports'/('weapon-'+w['id'].split('-',1)[1]+'-repair.json')
    if row['status']=='PENDING' and repair.exists():row.update(status='REJECTED_REDESIGN_REQUIRED',repair_report=repair.relative_to(R).as_posix())
    rows.append(row)
data={'scope':'Art refinement packages; existing runtime arsenal and spawn/crafting rules unchanged.','stop_rule':'Continue until handled or either usage window has 50% remaining.','counts':{s:sum(r['status']==s for r in rows) for s in('REFINED_PROTOTYPE','REJECTED_REDESIGN_REQUIRED','PENDING')},'weapons':rows}
(R/'data/weapon-refinement-progress.json').write_text(json.dumps(data,indent=2)+'\n');print(json.dumps(data['counts']))
