"""Sample exported pump sleeve and staged axial/bottom shell loading interfaces."""
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
clip('idle',1);pump=find('pump');shell=find('reload_shot_shell');neck=find('loading_gate_frame');tube=find('shell_tube');body=find('receiver_chassis');clamp=find('front_tube_clamp');guard=find('trigger_guard');barrel=find('barrel');assert tree(neck).overlap(tree(body)),'Loading gate detached from receiver';assert tree(clamp).overlap(tree(tube)) and tree(clamp).overlap(tree(barrel)),'Front tube clamp does not seat on both tubes'
start=center(shell);reload=[]
for frame in(1,4,8,12,16,23,30,38,46,54,60):
    clip('reload',frame);counts={o.name:len(tree(shell).overlap(tree(o))) for o in(neck,tube,body,guard)};reload.append({'frame':frame,'shell_center_m':list(center(shell)),'surface_intersections':counts});assert not any(counts.values()),'Shell loading interference: '+str(reload[-1])
clip('reload',30);travel=(center(shell)-start).length;assert travel>.12,'Shell failed to leave the loading tray';clip('reload',60);assert (center(shell)-start).length<.001,'Shell did not return to magazine'
clip('idle',1);bind=scene.objects.get('w07-reliquary__ROOT');ps=bind.matrix_world.inverted()@center(pump);fire=[]
for frame in(1,3,6,9,12):
    clip('fire',frame);counts={o.name:len(tree(pump).overlap(tree(o))) for o in(neck,tube,body,clamp,barrel)};fire.append({'frame':frame,'surface_intersections':counts});assert not any(counts.values()),'Pump interference: '+str(fire[-1])
clip('fire',6);stroke=abs((bind.matrix_world.inverted()@center(pump)-ps).x);assert stroke>.035,'Pump stroke missing'
result={'asset_id':ident,'asset_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'status':'PASS','pump_stroke_m':stroke,'shell_reload_travel_m':travel,'reload_sweep':reload,'fire_sweep':fire,'limits':'Sampled component clearances; no hands, follower spring or loose-shell physics.'};(folder/'mechanical-check.json').write_text(json.dumps(result,indent=2)+'\n');(R/'reports'/('weapon-mechanical-'+ident+'.json')).write_text(json.dumps(result,indent=2)+'\n');print('PASS PUMP MECHANICS',ident)
