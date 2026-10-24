"""One candidate, one command: parallel export inspection/rendering with hash caching.
Art review remains a separate human/agent inspection of the actual images.
"""
import argparse, hashlib, json, subprocess, sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
R=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--id',required=True);p.add_argument('--force',action='store_true');p.add_argument('--blender',default='C:/Program Files/Blender Foundation/Blender 5.0/blender.exe');p.add_argument('--image-python');a=p.parse_args()
b=R/'build/candidates/weapon-refinement'/a.id
if b.parent!=R/'build/candidates/weapon-refinement' or not (b/(a.id+'.glb')).is_file():raise ValueError('Unknown staged candidate')
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
inputs=json.loads((b/'inspection-input.json').read_text());tools=['inspect_structure_quality.py','render_weapon_refinements.py','compose_weapon_review.py','check_vesper_mechanics.py','check_revolver_mechanics.py','check_box_magazine_mechanics.py','check_break_action_mechanics.py','check_pump_mechanics.py']
signature={r['path']:sha(R/r['path']) for r in inputs};signature.update({f:sha(R/'tools'/f) for f in tools});stamp=b/'review-pipeline-cache.json'
inspection=R/'reports'/('weapon-'+a.id+'-inspection.json');gate=R/'reports'/('weapon-'+a.id+'-quality.json')
def run(label,cmd):
    with (R/'reports'/('weapon-'+a.id+'-'+label+'.log')).open('w') as log:subprocess.run(cmd,cwd=R,stdout=log,stderr=subprocess.STDOUT,check=True)
def blender(script,*args):return [a.blender,'-b','--factory-startup','--python-exit-code','1','--python','tools/'+script,'--',*map(str,args)]
cached=not a.force and stamp.exists() and json.loads(stamp.read_text())==signature and inspection.exists()
if cached:
    evidence=json.loads((b/'render-evidence.json').read_text())
    cached=evidence['asset_sha256']==sha(b/(a.id+'.glb')) and all((R/v['path']).is_file() and sha(R/v['path'])==v['sha256'] for v in evidence['renders'].values())
if not cached:
    with ThreadPoolExecutor(max_workers=3) as pool:
        jobs=[pool.submit(run,'inspect',blender(tools[0],b/'inspection-input.json',inspection)),pool.submit(run,'render',blender(tools[1],'--id',a.id))]
        if a.id.startswith('w01-vesper-'):jobs.append(pool.submit(run,'mechanics',blender(tools[3],a.id)))
        if a.id.startswith('w02-ossuary-') and 'refined-r1' not in a.id:jobs.append(pool.submit(run,'mechanics',blender(tools[4],a.id)))
        if a.id.startswith(('w03-needle-choir-','w04-rivet-saint-')) and 'ballistic_box_magazine' in json.loads((b/'contract.json').read_text()).get('reload_objects',[]):jobs.append(pool.submit(run,'mechanics',blender(tools[5],a.id)))
        if a.id.startswith(('w05-grave-bell-','w06-cerberus-')) and 'profiled_breech' in ' '.join(json.loads((b/'contract.json').read_text()).get('intentional_separate_meshes',[])):jobs.append(pool.submit(run,'mechanics',blender(tools[6],a.id)))
        if a.id.startswith('w07-reliquary-') and 'front_tube_clamp' in ' '.join(json.loads((b/'contract.json').read_text()).get('intentional_separate_meshes',[])):jobs.append(pool.submit(run,'mechanics',blender(tools[7],a.id)))
        for job in jobs:job.result()
    image_python=a.image_python or str(Path.home()/'.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe')
    run('sheet',[image_python,'tools/'+tools[2],a.id]);stamp.write_text(json.dumps(signature,indent=2)+'\n')
result=subprocess.run([sys.executable,'tools/structure_quality_gate.py','--inspection',str(inspection),'--contracts',str(b/'contracts.json'),'--report',str(gate)],cwd=R)
if result.returncode not in(0,3):sys.exit(result.returncode)
print(json.dumps({'id':a.id,'cached':cached,'gate':str(gate.relative_to(R)),'art_review_required':result.returncode==3}))
