"""Exported six-barrel axis, bore, bearing and removable guided-drum checks."""
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
def center(o):
    p=[v.co for v in o.data.vertices];return o.matrix_world@Vector([(min(v[i] for v in p)+max(v[i] for v in p))/2 for i in range(3)])
def tree(o):
    o.data.calc_loop_triangles();return BVHTree.FromPolygons([o.matrix_world@v.co for v in o.data.vertices],[tuple(t.vertices) for t in o.data.loop_triangles],all_triangles=True,epsilon=1e-7)
clip('idle',1);barrels=sorted([o for o in scene.objects if '__tube_0_' in o.name],key=lambda o:o.name);assert len(barrels)==6,'Expected six actual barrels';bearing=find('fixed_rotor_bearing');body=find('receiver_chassis');bridge=find('feed_bridge');drum=find('feed_drum_cartridge');shaft=find('drum_lock_spindle');assert tree(bearing).overlap(tree(body)),'Rotor bearing detached from body';assert tree(bridge).overlap(tree(body)),'Feed bridge detached from receiver'
root=find('ROOT');inv=root.matrix_world.inverted();core=sum((inv@center(b) for b in barrels),Vector())/6;fire=[]
for frame in(1,3,6,9,12):
    clip('fire',frame);inv=root.matrix_world.inverted();mean=sum((inv@center(b) for b in barrels),Vector())/6;assert (mean-core).length<.0005,'Rotor axis orbits instead of spinning about its own center';counts={};errors=[]
    for i,b in enumerate(barrels):
        axis=(b.matrix_world.to_3x3()@Vector((1,0,0))).normalized();assert axis.dot(Vector((1,0,0)))>.9999,'Barrels tip sideways while firing';c=center(b);p=[b.matrix_world@v.co for v in b.data.vertices];length=max(v.dot(axis) for v in p)-min(v.dot(axis) for v in p);origin=c-axis*(length/2+.01)
        for part in[b,find('rotor_front_0'),find('rotor_rear_0')]:
            hit=tree(part).ray_cast(origin,axis,length+.02)[0];assert hit is None,'Blocked barrel/rotor aperture '+str(frame)+' '+b.name+' '+part.name+' hit '+str(hit)+' origin '+str(origin)+' axis '+str(axis)
        counts[b.name]=len(tree(b).overlap(tree(bearing)))+len(tree(b).overlap(tree(body)));muzzle=find('muzzle_'+str(i)+'_SOCKET').matrix_world.translation;md=muzzle-c;errors.append((md-axis*md.dot(axis)).length);assert errors[-1]<.0005 and abs(md.dot(axis)-length/2-.012)<.001,'Muzzle socket detached from rotating barrel'
    fire.append({'frame':frame,'fixed_housing_intersections':counts,'muzzle_axis_errors_m':errors});assert not any(counts.values()),'Rotating barrels hit fixed housing'
clip('idle',1);start=center(drum);reload=[]
for frame in(1,4,8,16,23,30,38,46,54,60):
    clip('reload',frame);counts={o.name:len(tree(drum).overlap(tree(o))) for o in(shaft,bridge,body,find('grip_shell'),find('trigger_guard'))};reload.append({'frame':frame,'surface_intersections':counts});assert not any(counts.values()),'Drum hits fixed spindle/feed interface: '+str(reload[-1])
clip('reload',30);travel=(center(drum)-start).length;assert travel>.20,'Drum failed to withdraw';clip('reload',60);assert (center(drum)-start).length<.001,'Drum did not reseat'
result={'asset_id':ident,'asset_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'status':'PASS','barrel_count':6,'rotor_axis_center_m':list(core),'drum_reload_travel_m':travel,'fire_sweep':fire,'reload_sweep':reload,'limits':'Sampled component poses; no belt rounds, feed synchronization, hand IK or bearing contact simulation.'};(folder/'mechanical-check.json').write_text(json.dumps(result,indent=2)+'\n');(R/'reports'/('weapon-mechanical-'+ident+'.json')).write_text(json.dumps(result,indent=2)+'\n');print('PASS ROTARY MECHANICS',ident)
