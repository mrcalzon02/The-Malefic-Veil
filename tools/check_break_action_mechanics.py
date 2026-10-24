"""Exported break-action: coaxial cartridges, through-bores, fixed-frame clearance."""
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
def points(o):return [o.matrix_world@v.co for v in o.data.vertices]
def center(o):
    p=[v.co for v in o.data.vertices];return o.matrix_world@Vector([(min(v[i] for v in p)+max(v[i] for v in p))/2 for i in range(3)])
def tree(o):
    o.data.calc_loop_triangles();return BVHTree.FromPolygons(points(o),[tuple(t.vertices) for t in o.data.loop_triangles],all_triangles=True,epsilon=1e-7)
clip('idle',1);names=['left_barrel','right_barrel']+(['top_barrel'] if any(o.name.endswith('__top_barrel') for o in scene.objects) else []);barrels=[find(n) for n in names];shells=[find('shot_shell_'+str(i)) for i in range(len(names))];fixed=[find(n) for n in('receiver_chassis','trigger_guard','grip_shell')];sweep=[]
assert tree(find('fixed_hinge_web')).overlap(tree(find('receiver_chassis'))),'Hinge support detached from receiver'
assert tree(find('fixed_hinge_web')).overlap(tree(find('fixed_hinge_cradle'))),'Hinge cradle detached from support web'
body=points(find('receiver_chassis'));breech=points(find('profiled_breech'));front=max(v.x for v in body);back=min(v.x for v in breech);body_seam=[v for v in body if abs(v.x-front)<1e-5];breech_seam=[v for v in breech if abs(v.x-back)<1e-5]
for dim in(1,2):
    for fn in(min,max):assert abs(fn(v[dim] for v in body_seam)-fn(v[dim] for v in breech_seam))<.0005,'Body and breech seam sections differ'
for frame in(1,4,8,16,23,30,38,46,54,60):
    clip('reload',frame);counts={};offsets=[]
    for i,(barrel,shell) in enumerate(zip(barrels,shells)):
        axis=(barrel.matrix_world.to_3x3()@Vector((1,0,0))).normalized();delta=center(shell)-center(barrel);offset=(delta-axis*delta.dot(axis)).length;offsets.append(offset);assert offset<.0005,'Cartridge is not coaxial with barrel at frame '+str(frame)+': '+str(offset)
        counts['shell_'+str(i)+'_barrel']=len(tree(shell).overlap(tree(barrel)))
        for body in fixed:counts['shell_'+str(i)+'_'+body.name]=len(tree(shell).overlap(tree(body)))
        pp=points(barrel);back=min(pp,key=lambda p:p.dot(axis));front=max(pp,key=lambda p:p.dot(axis));length=(front-back).dot(axis);bc=center(barrel);start=bc-axis*(length/2+.004);assert tree(barrel).ray_cast(start,axis,length+.008)[0] is None,'Barrel has a blocked through-bore'
        muzzle=find('muzzle_'+str(i)+'_SOCKET').matrix_world.translation;md=muzzle-bc;assert (md-axis*md.dot(axis)).length<.0005 and abs(md.dot(axis)-length/2-.012)<.001,'Muzzle socket does not follow its barrel'
    for body in fixed:counts['breech_'+body.name]=len(tree(find('profiled_breech')).overlap(tree(body)))
    sweep.append({'frame':frame,'coaxial_offset_m':offsets,'surface_intersections':counts});assert not any(counts.values()),'Reload interference: '+str(sweep[-1])
clip('idle',1);initial=[center(b) for b in barrels];clip('reload',30);motion=[(center(b)-p).length for b,p in zip(barrels,initial)];assert min(motion)>.09,'No visible break action'
result={'asset_id':ident,'asset_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'status':'PASS','barrel_count':len(barrels),'break_action_center_travel_m':motion,'reload_sweep':sweep,'limits':'Sampled cartridge/frame interfaces; hinge bearing contacts intentionally excluded. No hands or loose-shell physics.'};(folder/'mechanical-check.json').write_text(json.dumps(result,indent=2)+'\n');(R/'reports'/('weapon-mechanical-'+ident+'.json')).write_text(json.dumps(result,indent=2)+'\n');print('PASS BREAK ACTION MECHANICS',ident)
