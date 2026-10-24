"""Bake the shared finish onto one visually accepted mechanical candidate."""
import bpy,json,sys,hashlib,tempfile
from pathlib import Path
R=Path(__file__).resolve().parents[1];sys.path.insert(0,str(R/'tools'));from weapon_materials import bake
args=sys.argv[sys.argv.index('--')+1:];source=args[args.index('--source')+1];ident=args[args.index('--id')+1];base=R/'build/candidates/weapon-refinement';src=base/source;out=base/ident
if src.parent!=base or out.parent!=base:raise ValueError('Candidate identifiers only')
out.mkdir(exist_ok=True);tempfile.tempdir=str(out);bpy.ops.wm.open_mainfile(filepath=str(src/(source+'.blend')),load_ui=False);scene=bpy.context.scene
for o in scene.objects:
    if o.animation_data:
        o.animation_data.action=None
        for t in o.animation_data.nla_tracks:t.mute=True
scene.frame_set(1);bpy.context.view_layer.update();scene.render.engine='CYCLES';scene.cycles.samples=8
bake(scene,[o for o in scene.objects if o.type=='MESH'],out,R/'build/candidates/weapon-refinement/finish-sources/shared-stencil.png')
bpy.ops.object.select_all(action='SELECT')
for o in scene.objects:
    if o.animation_data:
        for t in o.animation_data.nla_tracks:t.mute=False
path=out/(ident+'.glb');bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='NLA_TRACKS',export_extras=True)
for o in scene.objects:
    if o.animation_data:
        for t in o.animation_data.nla_tracks:t.mute=True
scene.frame_set(1);bpy.data.libraries.write(str(out/(ident+'.blend')),{scene},compress=True)
c=json.loads((src/'contract.json').read_text());c['isolated_atlas_cells']=True;record={'id':ident,'path':path.relative_to(R).as_posix(),'category':'weapon-rough-candidate'};refs=json.loads((R/'data/structure-quality-policy.json').read_text())['reference_sources']
for name,data in [('contract.json',c),('contracts.json',{ident:c}),('inspection-input.json',refs+[record]),('design-record.json',{'id':ident,'source':source,'asset_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'geometry_frozen_for_texturing':True,'status':'UNREVIEWED'})]:(out/name).write_text(json.dumps(data,indent=2)+'\n')
print('BAKED WEAPON',ident)
