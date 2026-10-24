"""Check both exported Hailstorm rotors and the guided removable drum independently."""
import bpy,json,sys,hashlib,math
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
clip('idle',1);root=find('ROOT');body=find('receiver_chassis');groups=[[find('tube_'+str(g)+'_'+str(i)) for i in range(6)] for g in range(2)];bearings=[find('fixed_rotor_bearing_'+str(g)) for g in range(2)];bridge=find('feed_bridge');drum=find('feed_drum_cartridge');shaft=find('drum_lock_spindle');cores=[];reference=[]
for g,barrels in enumerate(groups):
    assert tree(bearings[g]).overlap(tree(body)),'Bearing detached from receiver: '+str(g)
    inv=root.matrix_world.inverted();c=sum((inv@center(b) for b in barrels),Vector())/6;cores.append(c);reference.append(inv@center(barrels[0])-c)
assert tree(bridge).overlap(tree(body)),'Feed bridge detached from receiver'
assert tree(find('upper_spine')).overlap(tree(body)),'Upper rail floats above the receiver'
shoulders=[find('gearcase_shoulder_'+str(g)) for g in range(2)];fixed_housing=[body]+bearings+shoulders
for g,shoulder in enumerate(shoulders):assert tree(shoulder).overlap(tree(body)) and tree(shoulder).overlap(tree(bearings[g])),'Gearcase shoulder not seated at both interfaces'
assert abs(cores[0].y-cores[1].y)>.27,'Distinct rotor axes collapsed'
fire=[];angles_at_quarter=[]
for firing_clip,frame in [('fire',f) for f in(1,2,3,4,6,8,9,11,12)]+[('alternate',f) for f in(1,3,6,9,12,16,18,22,24)]:
    clip(firing_clip,frame);inv=root.matrix_world.inverted();counts={};errors=[];axis_errors=[];angles=[]
    for g,barrels in enumerate(groups):
        c=sum((inv@center(b) for b in barrels),Vector())/6;axis_errors.append((c-cores[g]).length);assert axis_errors[-1]<.0005,'Cluster orbits instead of spinning on its own axis'
        radial=inv@center(barrels[0])-c;a=reference[g];angles.append(math.atan2(a.y*radial.z-a.z*radial.y,a.y*radial.y+a.z*radial.z))
        for i,b in enumerate(barrels):
            axis=(b.matrix_world.to_3x3()@Vector((1,0,0))).normalized();assert axis.dot(Vector((1,0,0)))>.9999,'Barrel tips sideways';bc=center(b);pp=[b.matrix_world@v.co for v in b.data.vertices];length=max(v.dot(axis) for v in pp)-min(v.dot(axis) for v in pp);origin=bc-axis*(length/2+.01)
            for part in(b,find('rotor_front_'+str(g)),find('rotor_rear_'+str(g))):assert tree(part).ray_cast(origin,axis,length+.02)[0] is None,'Blocked bore/rotor aperture: '+part.name
            counts[b.name]=sum(len(tree(b).overlap(tree(f))) for f in fixed_housing)
            muzzle=find('muzzle_'+str(g*6+i)+'_SOCKET').matrix_world.translation;md=muzzle-bc;errors.append((md-axis*md.dot(axis)).length);assert errors[-1]<.0005 and abs(md.dot(axis)-length/2-.012)<.001,'Muzzle does not follow its barrel'
        for station in('front','rear'):
            plate=find('rotor_'+station+'_'+str(g));counts[plate.name]=sum(len(tree(plate).overlap(tree(f))) for f in fixed_housing)
    if (firing_clip=='fire' and frame==3) or (firing_clip=='alternate' and frame==6):angles_at_quarter=angles;assert angles[0]>1.4 and angles[1]<-1.4 and abs(sum(angles))<.001,'Rotors do not counter-rotate'
    fire.append({'clip':firing_clip,'frame':frame,'axis_center_errors_m':axis_errors,'signed_spin_radians':angles,'muzzle_axis_errors_m':errors,'fixed_housing_intersections':counts});assert not any(counts.values()),'Rotating parts hit fixed housing: '+str(fire[-1])
clip('idle',1);start=center(drum);reload=[];moving=[drum,find('drum_pull_handle'),find('drum_guide_collar')]
for frame in(1,4,8,16,23,30,38,46,54,60):
    clip('reload',frame);counts={o.name:sum(len(tree(part).overlap(tree(o))) for part in moving) for o in [shaft,bridge,body,find('grip_shell'),find('trigger_guard')]+bearings+shoulders};reload.append({'frame':frame,'surface_intersections':counts});assert not any(counts.values()),'Drum assembly hits its feed interface: '+str(reload[-1])
clip('reload',30);travel=(center(drum)-start).length;assert travel>.20,'Drum failed to withdraw';clip('reload',60);assert (center(drum)-start).length<.001,'Drum did not reseat'
result={'asset_id':ident,'asset_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'status':'PASS','barrel_count':12,'rotor_axis_centers_m':[list(c) for c in cores],'quarter_clip_signed_spin_radians':angles_at_quarter,'drum_reload_travel_m':travel,'fire_sweep':fire,'reload_sweep':reload,'limits':'Sampled component clearances; no synchronized ammunition feed, hands, bearing contact simulation or gameplay changes.'};(folder/'mechanical-check.json').write_text(json.dumps(result,indent=2)+'\n');(R/'reports'/('weapon-mechanical-'+ident+'.json')).write_text(json.dumps(result,indent=2)+'\n');print('PASS TWIN ROTARY MECHANICS',ident)
