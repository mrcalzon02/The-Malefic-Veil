"""Independent checks on a GLB, in its imported frame; records sweep intersections."""
import bpy,json,sys,math,hashlib
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
R=Path(__file__).resolve().parents[1];ident=sys.argv[sys.argv.index('--')+1];folder=R/'build/candidates/weapon-refinement'/ident;path=folder/(ident+'.glb')
bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path));scene=bpy.context.scene
def find(r):return next(o for o in scene.objects if o.name.endswith('__'+r))
def clip(name,frame):
    for o in scene.objects:
        if o.animation_data:
            o.animation_data.action=None
            for tr in o.animation_data.nla_tracks:tr.mute=name not in tr.name.lower()
    scene.frame_set(frame);bpy.context.view_layer.update()
def bounds(o):
    p=[o.matrix_world@v.co for v in o.data.vertices];return Vector([min(v[i] for v in p) for i in range(3)]),Vector([max(v[i] for v in p) for i in range(3)])
def bvh(o):
    o.data.calc_loop_triangles();return BVHTree.FromPolygons([o.matrix_world@v.co for v in o.data.vertices],[list(t.vertices) for t in o.data.loop_triangles],all_triangles=True,epsilon=1e-7)
clip('idle',0);barrel=find('barrel');lo,hi=bounds(barrel);span=hi-lo;muzzle=find('muzzle_0_SOCKET').matrix_world.translation
assert span.x>2*max(span.y,span.z),'Barrel axis is not forward'
assert abs(muzzle.z-(lo.z+hi.z)/2)<.006 and muzzle.x>=hi.x-.005,'Muzzle socket does not align with barrel'
grip=find('grip_shell');mag=find('pistol_box_magazine');ml,mh=bounds(mag);assert mh.z-ml.z>.1,'Reload object is just a cap'
gl,gh=bounds(grip);walls=[];tree=bvh(grip)
for frac in(.25,.50,.75):
    z=gl.z+(gh.z-gl.z)*frac;origin=Vector((gl.x-.04,0,z));direction=Vector((1,0,0));first=tree.ray_cast(origin,direction,.3)[0];assert first is not None,'Missing rear grip surface'
    second=tree.ray_cast(first+direction*.00001,direction,.2)[0];assert second is not None,'Open rear grip wall'
    thickness=(second-first).length;walls.append(thickness);assert thickness>.005,'Grip wall too thin or magazine cuts through exterior'
assert not any('grip_channel' in o.name for o in scene.objects),'Detached grip strip returned'
start=mag.matrix_world.translation.copy();sweep=[]
for frame in(1,4,8,16,23,30,38,46,54,60):
    clip('reload',frame);overlaps=bvh(grip).overlap(bvh(mag));sweep.append({'frame':frame,'grip_mag_surface_intersections':len(overlaps)})
    assert not overlaps,'Magazine intersects grip on reload frame '+str(frame)
clip('reload',30);travel=(mag.matrix_world.translation-start).length;assert travel>.20,'Magazine did not withdraw'
clip('reload',60);assert (mag.matrix_world.translation-start).length<.003,'Magazine did not return to seated position'
result={'asset_id':ident,'asset_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'barrel_axis_z_up':'+X','barrel_dimensions_m':list(span),'muzzle_socket_m':list(muzzle),'magazine_length_m':mh.z-ml.z,'rear_grip_wall_thickness_m':walls,'reload_travel_m':travel,'reload_surface_sweep':sweep,'status':'PASS','limits':'Checks stated interfaces and poses; not hand IK, ammunition simulation or exhaustive collision proof.'}
(folder/'mechanical-check.json').write_text(json.dumps(result,indent=2)+'\n');(R/'reports'/('weapon-mechanical-'+ident+'.json')).write_text(json.dumps(result,indent=2)+'\n')
print('PASS VESPER MECHANICS',ident,flush=True)
