"""Independent exported revolver chamber, hinge, fire and frame-clearance checks."""
import bpy,json,sys,hashlib,math
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
def bvh(o):
    o.data.calc_loop_triangles();return BVHTree.FromPolygons([o.matrix_world@v.co for v in o.data.vertices],[tuple(t.vertices) for t in o.data.loop_triangles],all_triangles=True,epsilon=1e-7)
clip('idle',1);cylinder=find('swing_out_cylinder');start=center(cylinder);barrel=find('barrel');bl,bh=bounds(barrel);bc=(bl+bh)/2;round0=center(find('revolver_round_0'));assert abs(round0.y-bc.y)<.001 and abs(round0.z-bc.z)<.001,'Top chamber is not coaxial with barrel'
assert bh.x-bl.x>2*max(bh.y-bl.y,bh.z-bl.z),'Barrel axis incorrect'
assert len([o for o in scene.objects if '__revolver_round_' in o.name])==6,'Missing chamber loads'
fixed=[find(s) for s in('receiver_chassis','receiver_service_panel_-1','receiver_service_panel_1','lower_frame_bridge','trigger_guard','grip_shell')];sweep=[]
for frame in(1,4,8,16,23,30,38,46,54,60):
    clip('reload',frame);overlaps={o.name:len(bvh(cylinder).overlap(bvh(o))) for o in fixed};sweep.append({'frame':frame,'surface_intersections':overlaps});assert not any(overlaps.values()),'Cylinder hits fixed frame: '+str(sweep[-1])
clip('reload',30);travel=(center(cylinder)-start).length;assert travel>.08,'Cylinder failed to swing out'
clip('reload',60);assert (center(cylinder)-start).length<.001,'Cylinder did not reseat'
fire=[]
for frame in(1,3,6,9,12):
    clip('fire',frame);drift=(center(cylinder)-start).length;fire.append({'frame':frame,'axis_center_drift_m':drift});assert drift<.018,'Fire wrongly swings the crane or cylinder axis'
result={'asset_id':ident,'asset_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'status':'PASS','top_chamber_yz_m':list(round0)[1:],'barrel_axis_yz_m':list(bc)[1:],'reload_center_travel_m':travel,'reload_sweep':sweep,'fire_axis_sweep':fire,'limits':'Sampled exported component poses; no hand IK or simulated ejection/replacement of cartridges.'};(folder/'mechanical-check.json').write_text(json.dumps(result,indent=2)+'\n');(R/'reports'/('weapon-mechanical-'+ident+'.json')).write_text(json.dumps(result,indent=2)+'\n');print('PASS REVOLVER MECHANICS',ident)
