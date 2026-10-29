"""Measured Kenney building/vehicle size studies, before interior kit expansion."""
import sys,json,zipfile,math,hashlib
from pathlib import Path
ROOT=Path(r'C:/Users/Admin/Documents/ChatGPT/The Malefic Veil');sys.path.insert(0,str(ROOT/'tools'))
import bpy
from mathutils import Vector
from structure_art import reset,box,ellipsoid,beam
from bpy_extras.object_utils import world_to_camera_view
OUT=ROOT/'build/candidates/city-scale-comparison';PRE=ROOT/'previews/city-scale-studies';OUT.mkdir(parents=True,exist_ok=True);PRE.mkdir(parents=True,exist_ok=True)
reset();z=zipfile.ZipFile(ROOT/'assets/game/kenney/retro-urban-kit-2.0.zip')
models=['wall-type-b','wall-a-roof','door-type-a','window-wide-type-a','truck-green','truck-grey']
for name in z.namelist():
    if name.startswith('Models/GLB format/Textures/') and name.endswith('.png'):
        p=OUT/'Textures'/Path(name).name;p.parent.mkdir(exist_ok=True);p.write_bytes(z.read(name))
for name in models:(OUT/(name+'.glb')).write_bytes(z.read('Models/GLB format/'+name+'.glb'))
sources={};meshes={}
for name in models:
    before=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(OUT/(name+'.glb')));bpy.context.view_layer.update()
    items=[o for o in set(bpy.context.scene.objects)-before if o.type=='MESH'];pts=[o.matrix_world@v.co for o in items for v in o.data.vertices]
    lo=[min(p[i] for p in pts) for i in range(3)];hi=[max(p[i] for p in pts) for i in range(3)]
    sources[name]=dict(native_bounds_z_up=[lo,hi],sha256=hashlib.sha256((OUT/(name+'.glb')).read_bytes()).hexdigest())
    meshes[name]=[(o.data.copy(),o.matrix_world.copy()) for o in items]
    for o in set(bpy.context.scene.objects)-before:bpy.data.objects.remove(o,do_unlink=True)
instances=[]
def place(name,location,scale=1,yaw=0):
    parent=bpy.data.objects.new(name+' instance',None);bpy.context.collection.objects.link(parent);parent.location=location;parent.rotation_euler.z=yaw;parent.scale=(scale,)*3
    for mesh,world in meshes[name]:
        o=bpy.data.objects.new(name,mesh);bpy.context.collection.objects.link(o);o.parent=parent;o.matrix_basis=world
    instances.append(dict(source=name,position=list(location),uniform_scale=scale,yaw=yaw))
def mat(name,color):
    m=bpy.data.materials.new(name);m.use_nodes=True;p=m.node_tree.nodes['Principled BSDF'];p.inputs['Base Color'].default_value=(*color,1);p.inputs['Roughness'].default_value=.85;return m
ground=mat('Neutral asphalt',(.065,.075,.08));pavement=mat('Sidewalk',(.19,.20,.19));line=mat('Scale guide gold',(.7,.40,.035));human=mat('1.8m human height guide',(.025,.25,.32))
box('Street',(0,-3.4,-.10),(30,21,.2),ground,bevel=0)
box('Sidewalk',(0,-.4,-.02),(22,8,.04),pavement,bevel=0)
labels=[]
for center,storeys in [(-4.6,1),(4.6,2)]:
    # These are source-backed example assemblies, not screenshots of a finished
    # city zone and not the upcoming interior replacement kit.
    for floor in range(storeys):
        base=floor*3.
        for y in (-3,3):place('wall-type-b',(center,y,base),6)
        for x in (-3,3):place('wall-type-b',(center+x,0,base),6,math.pi/2)
        for x in (-1.6,1.6):place('window-wide-type-a',(center+x,-3.24,base+.85),2.3)
    place('wall-a-roof',(center,0,storeys*3),6)
    place('door-type-a',(center,-3.25,0),3)
    labels.append(dict(text=f'{storeys} storey / 6 x 6 m footprint',point=[center,-3,storeys*3+3.4]))
for name,location in [('truck-green',(-4.6,-6,0)),('truck-grey',(4.6,-6,0))]:place(name,location,1.9,math.pi/2)
def person(x,y):
    # Measurement guide, deliberately not a gameplay character asset.
    ellipsoid('Height guide head',(x,y,1.665),(.105,.10,.135),human)
    box('Height guide torso',(x,y,1.31),(.34,.21,.43),human,bevel=.045)
    box('Height guide pelvis',(x,y,1.01),(.29,.20,.19),human,bevel=.035)
    for side in (-1,1):
        beam('Height guide leg',(x+side*.08,y,.98),(x+side*.105,y,.15),.068,human)
        box('Height guide foot',(x+side*.105,y-.055,.055),(.14,.24,.11),human,bevel=.02)
        beam('Height guide arm',(x+side*.18,y,1.47),(x+side*.26,y,1.05),.055,human)
person(-.8,-4.3);person(8.1,-4.3)
for center,storeys in [(-4.6,1),(4.6,2)]:
    x=center-3.65;y=-2.8
    beam('Building height ruler',(x,y,0),(x,y,storeys*3),.018,line)
    for h in range(storeys*3+1):beam('Metre tick',(x-.12,y,h),(x+.12,y,h),.018,line)
# Three-metre bay marks on the sidewalk and a nine-foot comparison tick.
for x in (-7.6,-4.6,-1.6,1.6,4.6,7.6):beam('3m facade bay guide',(x,-3.55,.015),(x,-3.85,.015),.025,line)
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=24;scene.cycles.use_denoising=True
scene.render.resolution_x=1440;scene.render.resolution_y=1000;scene.render.resolution_percentage=100
scene.world=bpy.data.worlds.new('Scale comparison daylight');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[0].default_value=(.55,.65,.8,1);scene.world.node_tree.nodes['Background'].inputs[1].default_value=.45;scene.view_settings.view_transform='AgX'
bpy.ops.object.light_add(type='AREA',location=(-5,-10,18));bpy.context.object.data.energy=3300;bpy.context.object.data.size=12
bpy.context.object.rotation_euler=(Vector((0,0,2))-bpy.context.object.location).to_track_quat('-Z','Y').to_euler()
bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';cam.data.ortho_scale=24;scene.camera=cam
report=dict(kind='SOURCE_ASSET_SCALE_STUDY',status='SCALE_REVIEW_REQUIRED',storey_m=3.,user_approximate_nine_feet_m=2.7432,facade_span_m=6.,vehicle_runtime_scale=1.9,human_guide_m=1.8,building_roof_note='Original wall-a-roof at scale 6 adds 3m above the wall height; total heights are 6m and 9m.',not_runtime_screenshot=True,sources=sources,instances=instances,views={})
for name,position,target in [('street-scale',(8,-29,9),(0,-1,3.7)),('elevated-scale',(15,-24,20),(0,-.5,3))]:
    cam.location=position;cam.rotation_euler=(Vector(target)-cam.location).to_track_quat('-Z','Y').to_euler();bpy.context.view_layer.update()
    path=PRE/(name+'.png');scene.render.filepath=str(path);bpy.ops.render.render(write_still=True)
    projected=[]
    for label in labels:
        p=world_to_camera_view(scene,cam,Vector(label['point']));projected.append(dict(text=label['text'],pixel=[round(p.x*1440),round((1-p.y)*1000)]))
    report['views'][name]=dict(path=path.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),labels=projected)
bpy.ops.file.pack_all();bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'city-scale-comparison.blend'))
for name in ('truck-green','truck-grey'):
    lo,hi=sources[name]['native_bounds_z_up'];sources[name]['runtime_dimensions_m']=[round((hi[i]-lo[i])*1.9,4) for i in range(3)]
(ROOT/'reports/city-scale-comparison.json').write_text(json.dumps(report,indent=2)+'\n')
print('SCALE_COMPARISON_COMPLETE',flush=True)
