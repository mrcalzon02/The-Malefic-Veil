"""Check reimported Cautery cell docking, optical axis and coolant actuation."""
import bpy,json,sys,hashlib
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
R=Path(__file__).resolve().parents[1];ident=sys.argv[sys.argv.index('--')+1];folder=R/'build/candidates/weapon-refinement'/ident;path=folder/(ident+'.glb')
bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path));scene=bpy.context.scene
def find(role):return next(o for o in scene.objects if o.name.endswith('__'+role))
def clip(name,frame):
 for o in scene.objects:
  if o.animation_data:
   o.animation_data.action=None
   for t in o.animation_data.nla_tracks:t.mute=name not in t.name.lower()
 scene.frame_set(frame);bpy.context.view_layer.update()
def bounds(o):
 p=[o.matrix_world@v.co for v in o.data.vertices];return Vector([min(v[i] for v in p) for i in range(3)]),Vector([max(v[i] for v in p) for i in range(3)])
def center(o):a,b=bounds(o);return (a+b)/2
def tree(o):
 o.data.calc_loop_triangles();return BVHTree.FromPolygons([o.matrix_world@v.co for v in o.data.vertices],[tuple(t.vertices) for t in o.data.loop_triangles],all_triangles=True,epsilon=1e-7)
clip('idle',1);body=find('receiver_chassis');well=find('power_cartridge_well');cell=find('power_cartridge');guard=find('trigger_guard');barrel=find('barrel');lens=find('recessed_focusing_lens');lip=find('protective_emitter_lip');shroud=find('vented_emitter_shroud');root=find('ROOT')
wl,wh=bounds(well);gl,gh=bounds(guard);assert wl.x>gh.x+.008,'Cell socket obstructs trigger space'
assert tree(well).overlap(tree(body)),'Cell socket detached'
assert tree(shroud).overlap(tree(body)),'Emitter shroud detached'
bl,bh=bounds(barrel);ll,lh=bounds(lens);pl,ph=bounds(lip);assert bh.x-bl.x>4*(bh.y-bl.y),'Sideways emitter'
assert lh.x<ph.x-.010 and abs(center(lens).y)<.001 and abs(center(lens).z-.035)<.001,'Lens is not recessed and coaxial'
socket=find('muzzle_0_SOCKET').matrix_world.translation;assert socket.x>ph.x and abs(socket.y)<.001 and abs(socket.z-.035)<.001,'Muzzle aim mismatch'
start=center(cell);moving=[o for o in scene.objects if o.type=='MESH' and (o.get('is_reload_object') or '__cell_contact_' in o.name)]
fixed=[body,well,guard,find('grip_shell'),shroud];reload=[]
for frame in(1,4,8,16,23,30,38,46,54,60):
 clip('reload',frame);counts={o.name:sum(len(tree(p).overlap(tree(o))) for p in moving) for o in fixed};reload.append({'frame':frame,'surface_intersections':counts});assert not any(counts.values()),'Power cell collision: '+str(reload[-1])
clip('reload',30);travel=(center(cell)-start).length;assert travel>.24,'Insufficient cell withdrawal';clip('reload',60);assert (center(cell)-start).length<.001,'Cell did not reseat'
firing=[]
for name,end in [('fire',12),('alternate',24)]:
 clip(name,1);sleeve=find('coolant_shutter');origin=root.matrix_world.inverted()@center(sleeve);maxstroke=0
 for frame in(1,end//4,end//2,3*end//4,end):
  clip(name,frame);counts={o.name:len(tree(sleeve).overlap(tree(o))) for o in [body,barrel,shroud,find('coolant_guide_-1'),find('coolant_guide_1')]};assert not any(counts.values()),'Coolant shutter collision: '+str(counts);maxstroke=max(maxstroke,abs((root.matrix_world.inverted()@center(sleeve)-origin).x));firing.append({'clip':name,'frame':frame,'surface_intersections':counts})
 assert maxstroke>.015,'No coolant actuation'
result={'asset_id':ident,'asset_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'status':'PASS','trigger_cell_gap_m':wl.x-gh.x,'cell_withdrawal_m':travel,'lens_recess_m':ph.x-lh.x,'reload_sweep':reload,'firing_sweep':firing,'limits':'Sampled component geometry. Beam VFX, heat simulation, hands and runtime integration are not part of this art package.'}
for p in [folder/'mechanical-check.json',R/'reports'/('weapon-mechanical-'+ident+'.json')]:p.write_text(json.dumps(result,indent=2)+'\n')
print('PASS ENERGY CELL MECHANICS',ident)
