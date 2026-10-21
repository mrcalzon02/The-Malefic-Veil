import bpy, json, math
from pathlib import Path
from mathutils import Vector, Matrix
ROOT=Path(__file__).resolve().parents[1]
bpy.ops.wm.read_factory_settings(use_empty=True)
src=Path(r'C:\Users\Admin\Documents\Models\mega-pack-kitbash-320-details\Mech_Warrior_Builder_Kit_ 10\Mech_Warrior_Builder_Kit_ 10\Fbx\Part_01.FBX')
bpy.ops.import_scene.fbx(filepath=str(src))
parts=[]
for o in list(bpy.data.objects):
    if o.type!='MESH':
        bpy.data.objects.remove(o,do_unlink=True); continue
    o.data.transform(o.matrix_world)
    o.matrix_world=Matrix.Identity(4)
    vs=[v.co.copy() for v in o.data.vertices]
    lo=Vector([min(v[i] for v in vs) for i in range(3)])
    hi=Vector([max(v[i] for v in vs) for i in range(3)])
    center=(lo+hi)/2
    scale=1/max(hi-lo)
    for v in o.data.vertices: v.co=(v.co-center)*scale
    o.data.calc_loop_triangles()
    name=f'M{len(parts):03d}'
    original=o.name; o.name=name
    o['source_path']=str(src); o['source_object']=original
    parts.append({'id':name,'source_object':original,'source_path':str(src),'triangles':len(o.data.loop_triangles),'dimensions':list((hi-lo)*scale)})
with bpy.data.libraries.load(r'C:\Users\Admin\Documents\Models\Greebles.blend',link=False) as (s,d): d.objects=list(s.objects)
for o in d.objects:
    if not o or o.type!='MESH': continue
    bpy.context.collection.objects.link(o)
    o.data.transform(o.matrix_world); o.matrix_world=Matrix.Identity(4)
    vs=[v.co.copy() for v in o.data.vertices]
    if not vs: continue
    lo=Vector([min(v[i] for v in vs) for i in range(3)])
    hi=Vector([max(v[i] for v in vs) for i in range(3)])
    if max(hi-lo)==0: continue
    center=(lo+hi)/2; scale=1/max(hi-lo)
    for v in o.data.vertices: v.co=(v.co-center)*scale
    o.data.calc_loop_triangles()
    name=f'G{len([p for p in parts if p["id"].startswith("G")]):03d}'
    original=o.name; o.name=name
    o['source_path']=r'C:\Users\Admin\Documents\Models\Greebles.blend'; o['source_object']=original
    parts.append({'id':name,'source_object':original,'source_path':o['source_path'],'triangles':len(o.data.loop_triangles),'dimensions':list((hi-lo)*scale)})
private=ROOT/'assets/private/arsenal'; private.mkdir(parents=True,exist_ok=True)
(ROOT/'reports/mech_inventory.json').write_text(json.dumps(parts,indent=2))
bpy.ops.wm.save_as_mainfile(filepath=str(private/'normalized_parts.blend'),compress=True)
# First page of mech pack; four pages of Greebles. Render flat contact sheets.
allparts={o.name:o for o in bpy.data.objects if o.type=='MESH'}
mat=bpy.data.materials.new('catalog steel'); mat.diffuse_color=(.42,.52,.60,1)
for o in allparts.values():
    o.hide_render=True; o.data.materials.clear(); o.data.materials.append(mat)
scene=bpy.context.scene
scene.render.engine='CYCLES'; scene.cycles.samples=8
scene.render.resolution_x=1800; scene.render.resolution_y=1500; scene.render.resolution_percentage=100
scene.world=bpy.data.worlds.new('Catalog world')
scene.world.color=(.18,.18,.18)
bpy.ops.object.camera_add(location=(0,-18,22)); cam=bpy.context.object
cam.rotation_euler=(Vector((0,0,0))-cam.location).to_track_quat('-Z','Y').to_euler()
cam.data.type='ORTHO'; cam.data.ortho_scale=13; scene.camera=cam
for loc,power,size in [((0,-7,12),2200,12),((5,5,8),1700,8)]:
    bpy.ops.object.light_add(type='AREA',location=loc); l=bpy.context.object; l.data.energy=power; l.data.shape='DISK'; l.data.size=size
texts=[]
groups=[('mech',[p for p in parts if p['id'].startswith('M')])]
g=[p for p in parts if p['id'].startswith('G')]
groups += [(f'greebles_{i//60+1}',g[i:i+60]) for i in range(0,len(g),60)]
for label,group in groups:
    for o in allparts.values():o.hide_render=True
    for t in texts:bpy.data.objects.remove(t,do_unlink=True)
    texts=[]
    for i,p in enumerate(group):
        o=allparts[p['id']]; o.hide_render=False
        x=(i%10-4.5)*1.2; y=(2.5-i//10)*1.65
        o.location=(x,y,.15)
        bpy.ops.object.text_add(location=(x-.35,y-.60,.03),rotation=(math.pi/2,0,0))
        t=bpy.context.object; t.data.body=p['id'];t.data.size=.16;texts.append(t)
    scene.render.filepath=str(ROOT/'previews'/f'parts_{label}.png')
    bpy.ops.render.render(write_still=True)
    print('CONTACT',label,flush=True)
