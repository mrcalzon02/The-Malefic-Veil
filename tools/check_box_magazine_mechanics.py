"""Exported ballistic magazine, bore, trigger space and bolt-travel checks."""
import bpy,json,sys,hashlib
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
R=Path(__file__).resolve().parents[1];ident=sys.argv[sys.argv.index('--')+1];folder=R/'build/candidates/weapon-refinement'/ident;path=folder/(ident+'.glb');bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path));scene=bpy.context.scene
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
clip('idle',1);barrel=find('barrel');lo,hi=bounds(barrel);assert hi.x-lo.x>2*max(hi.y-lo.y,hi.z-lo.z),'Wrong barrel axis';muzzle=find('muzzle_0_SOCKET').matrix_world.translation;assert abs(muzzle.z-(lo.z+hi.z)/2)<.006 and muzzle.x>=hi.x-.005,'Misaligned muzzle socket'
mag=find('ballistic_box_magazine');well=find('ballistic_box_magazine_well');body=find('receiver_chassis');guard=find('trigger_guard');ml,mh=bounds(mag);gl,gh=bounds(guard);wl,wh=bounds(well);assert wl.x>gh.x+.008,'Magazine well blocks the trigger finger space';assert mh.z-ml.z>.12,'Incomplete removable magazine'
start=center(mag);sweep=[];moving=[mag,find('ballistic_box_magazine_feed_lips')]
fixed_parts=[well,body,guard]+[o for o in scene.objects if o.name.endswith('__profiled_handguard')]
for frame in(1,4,8,16,23,30,38,46,54,60):
    clip('reload',frame);counts={fixed.name:sum(len(tree(part).overlap(tree(fixed))) for part in moving) for fixed in fixed_parts};sweep.append({'frame':frame,'surface_intersections':counts});assert not any(counts.values()),'Magazine interface collision: '+str(sweep[-1])
clip('reload',30);travel=(center(mag)-start).length;assert travel>.20,'Magazine failed to withdraw';clip('reload',60);assert (center(mag)-start).length<.001,'Magazine failed to reseat'
clip('idle',1);bolt=find('visible_bolt_carrier');boltstart=Vector(find('ROOT').matrix_world.inverted()@center(bolt));clip('fire',6);delta=Vector(find('ROOT').matrix_world.inverted()@center(bolt))-boltstart;assert delta.x<-.015,'No visible bolt travel'
result={'asset_id':ident,'asset_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'status':'PASS','trigger_magwell_gap_m':wl.x-gh.x,'magazine_length_m':mh.z-ml.z,'reload_travel_m':travel,'bolt_travel_m':list(delta),'reload_sweep':sweep,'limits':'Sampled component geometry; no hand IK or cartridge simulation.'};(folder/'mechanical-check.json').write_text(json.dumps(result,indent=2)+'\n');(R/'reports'/('weapon-mechanical-'+ident+'.json')).write_text(json.dumps(result,indent=2)+'\n');print('PASS BOX MAGAZINE MECHANICS',ident)
