"""Promote only a reviewed candidate and preserve editable sources, PBR and checks."""
import argparse,json,subprocess,sys,shutil,hashlib
from pathlib import Path
R=Path(__file__).resolve().parents[1];p=argparse.ArgumentParser(description=__doc__);p.add_argument('--id',required=True);a=p.parse_args();b=R/'build/candidates/weapon-refinement'/a.id
if b.parent!=R/'build/candidates/weapon-refinement':raise ValueError('Candidate identifier only')
review=R/'reports/structure-art-reviews'/(a.id+'.json');asset=b/(a.id+'.glb');sha=hashlib.sha256(asset.read_bytes()).hexdigest();v=json.loads(review.read_text());assert v['decision']=='pass' and v['asset_sha256']==sha,'Current visual pass required'
mechanics=json.loads((b/'mechanical-check.json').read_text());assert mechanics['status']=='PASS' and mechanics['asset_sha256']==sha,'Current exported mechanical pass required'
inspection='reports/weapon-'+a.id+'-inspection.json';quality='reports/weapon-'+a.id+'-quality.json'
subprocess.run([sys.executable,'tools/structure_quality_gate.py','--inspection',inspection,'--contracts',str(b/'contracts.json'),'--report',quality,'--promote','--destination','assets/game/arsenal-refined-approved'],cwd=R,check=True)
dest=R/'assets/authoring/weapon-refinement'/a.id;dest.mkdir(parents=True,exist_ok=True)
for name in [a.id+'.blend','albedo.png','orm.png','contract.json','design-record.json','mechanical-check.json']:
    if (b/name).exists():shutil.copyfile(b/name,dest/name)
evidence=json.loads((b/'render-evidence.json').read_text());evidence['renders'].pop('comparison',None);(R/'reports'/('weapon-'+a.id+'-render-evidence.json')).write_text(json.dumps(evidence,indent=2)+'\n')
cpath=R/'data/weapon-refinement-contracts.json';contracts=json.loads(cpath.read_text());contracts.update(json.loads((b/'contracts.json').read_text()));cpath.write_text(json.dumps(contracts,indent=2)+'\n');print('PACKAGED',a.id,sha)
