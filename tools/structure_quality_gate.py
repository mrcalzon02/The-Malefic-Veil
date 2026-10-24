"""Fail-closed structure delivery gate and bounded build/inspect/repair runner.

This gate measures exported files, not generator assurances. Organic continuity
contracts are authored separately from builders. A technical pass is followed by
a hash-bound visual comparison record; never automatically call that art approval.
"""
import argparse
import hashlib
import json
import shutil
import subprocess
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
POLICY=ROOT/'data/structure-quality-policy.json'
PLOTS=ROOT/'data/construction-plots.json'

def read(p):return json.loads(Path(p).read_text(encoding='utf-8'))
def digest(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def local(path):
    p=(ROOT/path).resolve()
    if not p.is_relative_to(ROOT):raise ValueError('Path outside project: '+str(p))
    return p

def plot_failures(asset,contract):
    standard=read(PLOTS)
    plot=next((p for p in standard['allowed_sizes'] if p['id']==contract.get('plot_id')),None)
    if plot is None:return ['PLOT_NOT_ASSIGNED: choose plot-5 or plot-10 before delivery']
    lo,hi=plot['bounds_z_up'];tol=standard['containment']['numerical_tolerance_m']
    errors=[]
    for mesh in asset['meshes']:
        bounds=mesh.get('bounds')
        if not bounds:
            errors.append('PLOT_BOUNDS_UNAVAILABLE: '+mesh['name']);continue
        for axis,label in enumerate(('X','Y','Z')):
            if bounds[0][axis]<lo[axis]-tol or bounds[1][axis]>hi[axis]+tol:
                errors.append('OUTSIDE_BUILD_VOLUME: '+mesh['name']+' '+label+'; '+plot['id'])
    return errors

def technical(asset,policy,contract=None):
    limits=policy['limits']
    failures=[]
    if asset.get('inspection_error'):
        return ['IMPORT_FAILED: '+asset['inspection_error']]
    if not asset.get('meshes'):return ['NO_GEOMETRY']
    if asset['uv_coverage']<limits['min_uv_area_coverage']:
        failures.append('UV_INCOMPLETE: unwrap all visible surfaces before baking')
    if asset['textured_fraction']<limits['min_textured_area_fraction']:
        failures.append('UNTEXTURED_SURFACE: bake/export coherent image textures; flat colors do not qualify')
    images=[i for i in asset['images'] if i['loaded']]
    if not images or min(min(i['width'],i['height']) for i in images)<limits['min_texture_dimension']:
        failures.append('MISSING_OR_UNDERSIZED_TEXTURE')
    if asset['triangles']>limits['max_triangles']:
        failures.append('TRIANGLE_BUDGET: simplify without destroying the silhouette')
    for mesh in asset['meshes']:
        name=mesh['name']
        if mesh['nonfinite_triangles']:
            failures.append('NONFINITE_GEOMETRY: '+name)
        if mesh['degenerate_triangles']/max(mesh['triangles'],1)>limits['max_degenerate_fraction']:
            failures.append('DEGENERATE_GEOMETRY: '+name)
    if asset.get('category','').startswith('reference-'):return failures
    if not contract:
        failures.append('MISSING_CONTINUITY_CONTRACT: identify main surfaces and intentional separate parts')
        return failures
    if contract.get('placement_kind')=='snap-component':
        # Components have assembly frames (a ceiling can extend below its port).
        # A measured local envelope is separate from the completed plot envelope.
        bounds=contract.get('local_bounds_z_up')
        if not bounds:failures.append('COMPONENT_ENVELOPE_MISSING')
        else:
            for mesh in asset['meshes']:
                for axis in range(3):
                    if mesh['bounds'][0][axis]<bounds[0][axis]-.0001 or mesh['bounds'][1][axis]>bounds[1][axis]+.0001:
                        failures.append('OUTSIDE_COMPONENT_ENVELOPE: '+mesh['name'])
    else:failures.extend(plot_failures(asset,contract))
    if contract.get('isolated_atlas_cells'):
        cells=[]
        for m in asset['meshes']:
            cell=m.get('properties',{}).get('atlas_uv_cell');uv=m.get('uv_bounds')
            if not cell or not uv:failures.append('ATLAS_CELL_UNVERIFIED: '+m['name']);continue
            if any(uv[0][i]<cell[i]-.00001 or uv[1][i]>cell[i+2]+.00001 for i in range(2)):
                failures.append('UV_OUTSIDE_ATLAS_CELL: '+m['name'])
            for name,c in cells:
                if min(c[2],cell[2])-max(c[0],cell[0])>.00001 and min(c[3],cell[3])-max(c[1],cell[1])>.00001:
                    failures.append('OVERLAPPING_ATLAS_CELLS: '+m['name']+' / '+name)
            cells.append((m['name'],cell))
    groups=contract.get('continuous_surfaces',[])
    if contract.get('organic') and not groups:
        failures.append('ORGANIC_BODY_NOT_DECLARED')
    consumed=set()
    for group in groups:
        found=[m for m in asset['meshes'] if m['name']==group['mesh']]
        if len(found)!=1:
            failures.append('CONTINUOUS_SURFACE_MISSING_OR_SPLIT: '+group['mesh'])
            continue
        m=found[0];consumed.add(m['name'])
        if m['components']>limits['organic_max_components_per_surface'] or m['largest_component_area_fraction']<limits['organic_min_connected_area_fraction']:
            failures.append('DISCONNECTED_ORGANIC_BODY: '+m['name']+'; join/voxel-union the actual surface, not only object names')
        if m['nonmanifold_edges']>limits['organic_max_nonmanifold_edges'] or m['boundary_edges']:
            failures.append('OPEN_OR_NONMANIFOLD_BODY: '+m['name'])
        if m['normal_seam_fraction']>limits['organic_max_normal_seam_fraction']:
            failures.append('FACETED_ORGANIC_SURFACE: '+m['name'])
    intentional=set(contract.get('intentional_separate_meshes',[]))
    if contract.get('organic'):
        for m in asset['meshes']:
            if m['name'] not in consumed|intentional:
                failures.append('UNREVIEWED_DETACHED_PART: '+m['name'])
    for token in contract.get('forbidden_name_tokens',[]):
        if any(token.lower() in m['name'].lower() for m in asset['meshes']):
            failures.append('FORBIDDEN_SYMBOLIC_PROP: '+token)
    return failures

def visual(asset,contract,references):
    review_path=ROOT/'reports/structure-art-reviews'/(asset['id']+'.json')
    if not review_path.exists():return ['VISUAL_COMPARISON_PENDING']
    r=read(review_path)
    failures=[]
    if r.get('asset_sha256')!=asset['sha256']:failures.append('STALE_VISUAL_REVIEW')
    if r.get('decision')!='pass':failures.append('ART_DIRECTION_REJECTED')
    for k in ('surface_continuity','material_coherence','silhouette','reference_comparison','faction_identity'):
        if r.get('scores',{}).get(k,0)<4:failures.append('ART_CRITERION_FAILED: '+k)
    if not r.get('reviewer') or not r.get('findings'):failures.append('MISSING_REVIEW_REASONING')
    required=('front','rear','closeup','clay','comparison')
    for view in required:
        evidence=r.get('renders',{}).get(view,{})
        try:
            p=local(evidence['path'])
            if digest(p)!=evidence['sha256']:failures.append('STALE_RENDER: '+view)
        except (KeyError,OSError,ValueError):failures.append('RENDER_MISSING: '+view)
    if not r.get('compared_reference_ids'):failures.append('NO_REFERENCE_COMPARISON')
    for ident in r.get('compared_reference_ids',[]):
        if ident not in references or r.get('reference_sha256',{}).get(ident)!=references[ident]:
            failures.append('STALE_OR_UNKNOWN_REFERENCE: '+ident)
    return failures

def audit(inspection,contracts,policy):
    refs=[a for a in inspection if a.get('category','').startswith('reference-')]
    ref_errors={r['id']:technical(r,policy) for r in refs}
    baseline_ok=len(refs)>=4 and not any(ref_errors.values())
    results=[]
    for a in inspection:
        if a in refs:continue
        contract=contracts.get(a['id'])
        errors=technical(a,policy,contract)
        if not baseline_ok:errors.append('REFERENCE_BASELINE_UNAVAILABLE')
        if not Path(a['path']).is_file() or digest(a['path'])!=a.get('sha256'):
            errors.append('STALE_GEOMETRY_INSPECTION')
        art=visual(a,contract,{r['id']:r.get('sha256') for r in refs}) if not errors else []
        art_rejected=any(e=='ART_DIRECTION_REJECTED' or e.startswith('ART_CRITERION_FAILED:') for e in art)
        results.append(dict(id=a['id'],path=a['path'],sha256=a.get('sha256'),
            status='REJECTED' if errors or art_rejected else 'ART_REVIEW_REQUIRED' if art else 'DELIVERY_ELIGIBLE',
            technical_failures=errors,art_failures=art))
    return dict(policy_version=policy['version'],reference_baseline_valid=baseline_ok,
        reference_results=ref_errors,reference_metrics=[{k:a[k] for k in ('id','sha256','triangles','uv_coverage','textured_fraction')} for a in refs if 'sha256' in a],
        results=results,counts={s:sum(r['status']==s for r in results) for s in ('REJECTED','ART_REVIEW_REQUIRED','DELIVERY_ELIGIBLE')})

def main():
    p=argparse.ArgumentParser()
    p.add_argument('--inspection',default='reports/structure-quality-inspection.json')
    p.add_argument('--contracts',default='data/structure-continuity-contracts.json')
    p.add_argument('--report',default='reports/structure-quality-gate.json')
    p.add_argument('--recipe',help='JSON with candidate record, build_argv, repair_argv and bounded max_attempts')
    p.add_argument('--blender',default=r'C:/Program Files/Blender Foundation/Blender 5.0/blender.exe')
    p.add_argument('--promote',action='store_true')
    p.add_argument('--destination',default='assets/game/faction-structures-approved')
    args=p.parse_args()
    policy=read(POLICY)
    contracts=read(local(args.contracts)) if local(args.contracts).exists() else {}
    if args.recipe:
        recipe=read(local(args.recipe))
        candidate=recipe['candidate']
        candidate['path']=str(local(candidate['path']))
        input_path=ROOT/'build/quality-run-input.json'
        input_path.parent.mkdir(exist_ok=True)
        input_path.write_text(json.dumps(policy['reference_sources']+[candidate]))
        attempts=max(1,min(3,int(recipe.get('max_attempts',2))))
        history=[]
        for attempt in range(attempts):
            command=recipe['build_argv'] if attempt==0 else recipe.get('repair_argv')
            if not command:break
            subprocess.run(command,cwd=ROOT,check=True,shell=False)
            subprocess.run([args.blender,'--factory-startup','--background','--threads','6','--python-exit-code','1','--python',str(ROOT/'tools/inspect_structure_quality.py'),'--',str(input_path),str(local(args.inspection))],cwd=ROOT,check=True)
            result=audit(read(local(args.inspection)),contracts,policy)
            history.append(json.loads(json.dumps(result)))
            if not any(r['technical_failures'] for r in result['results']):
                # Generic cleanup cannot fix art direction. The agent reviews
                # rendered evidence and changes the design before another run.
                break
        result['attempt_history']=history.copy()
    else:
        result=audit(read(local(args.inspection)),contracts,policy)
    local(args.report).write_text(json.dumps(result,indent=2)+'\n')
    if args.promote:
        destination=local(args.destination)
        destination.mkdir(parents=True,exist_ok=True)
        for item in result['results']:
            target=destination/(item['id']+'.glb')
            if item['status']=='DELIVERY_ELIGIBLE':
                shutil.copy2(item['path'],target)
            elif target.exists():
                # A stale prior approval must not remain deliverable after rejection.
                quarantine=ROOT/'build/rejected/stale-approved'
                quarantine.mkdir(parents=True,exist_ok=True)
                shutil.move(str(target),str(quarantine/target.name))
    print(json.dumps(result['counts']))
    return 2 if result['counts']['REJECTED'] else 3 if result['counts']['ART_REVIEW_REQUIRED'] else 0

if __name__=='__main__':sys.exit(main())
