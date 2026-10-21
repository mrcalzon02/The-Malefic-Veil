"""Build a full-detail weapon from actual library components; no decimation or axis stretching."""
import bpy,bmesh,math,json,tempfile,hashlib
from pathlib import Path
from mathutils import Vector,Matrix,Euler
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'assets/runtime/arsenal/source-prototypes';OUT.mkdir(parents=True,exist_ok=True)
TMP=ROOT/'build/blender-temp';TMP.mkdir(parents=True,exist_ok=True);tempfile.tempdir=str(TMP)
bpy.ops.wm.read_factory_settings(use_empty=True)
scene=bpy.context.scene;scene.render.fps=30
scene.world=bpy.data.worlds.new('Studio');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[0].default_value=(.055,.07,.09,1);scene.world.node_tree.nodes['Background'].inputs[1].default_value=.4
scene.render.engine='CYCLES';scene.cycles.samples=40;scene.render.resolution_x=1400;scene.render.resolution_y=1000;scene.render.resolution_percentage=100
collection=bpy.data.collections.new('Vesper actual source assembly');scene.collection.children.link(collection)
root=bpy.data.objects.new('Vesper_ROOT',None);collection.objects.link(root)
objects=[root];manifest=[]
def mat(name,color,metal,rough):
    m=bpy.data.materials.new(name);m.use_nodes=True;m.diffuse_color=(*color,1);bs=m.node_tree.nodes['Principled BSDF'];bs.inputs['Base Color'].default_value=(*color,1);bs.inputs['Metallic'].default_value=metal;bs.inputs['Roughness'].default_value=rough;return m
steel=mat('Dark machined steel',(.11,.16,.19),.7,.37)
light=mat('Receiver ceramic',(.43,.46,.43),.35,.39)
brass=mat('Coupler brass',(.42,.23,.065),.75,.3)
rubber=mat('Grip finish',(.035,.055,.062),.15,.6)
def empty(name,loc):
    o=bpy.data.objects.new(name,None);collection.objects.link(o);o.parent=root;o.location=loc;objects.append(o);return o
def kit(pid,role,scale,loc,rotation=(0,0,0),material=steel,cuts=(),parent=None):
    with bpy.data.libraries.load(str(ROOT/'assets/private/arsenal/normalized_parts.blend'),link=False) as(s,d):d.objects=[pid]
    src=d.objects[0];mesh=src.data.copy();o=bpy.data.objects.new(role,mesh);collection.objects.link(o);o.parent=parent or root;objects.append(o)
    for axis,position,positive in cuts:
        bm=bmesh.new();bm.from_mesh(mesh)
        p=[0,0,0];p[axis]=position;n=[0,0,0];n[axis]=1
        result=bmesh.ops.bisect_plane(bm,geom=list(bm.verts)+list(bm.edges)+list(bm.faces),dist=.000001,plane_co=p,plane_no=n,clear_inner=positive,clear_outer=not positive)
        edges=[e for e in result['geom_cut'] if isinstance(e,bmesh.types.BMEdge) and e.is_boundary]
        if edges:bmesh.ops.holes_fill(bm,edges=edges,sides=0)
        bm.to_mesh(mesh);bm.free()
    mesh.transform(Euler(rotation).to_matrix().to_4x4());mesh.transform(Matrix.Scale(scale,4));o.location=loc
    mesh.materials.clear();mesh.materials.append(material);mesh.materials.append(light);mesh.materials.append(brass)
    mesh.update()
    for f in mesh.polygons:f.material_index=0
    mesh.calc_loop_triangles()
    source=src.get('source_path');original=src.get('source_object')
    o['source_part']=pid;o['original_object']=original;o['uniform_scale']=scale;o['no_decimation']=True
    manifest.append(dict(role=role,part=pid,source_path=source,source_object=original,uniform_scale=scale,rotation=rotation,location=loc,cuts=cuts,triangles=len(mesh.loop_triangles)))
    return o
# Receiver is cut from the actual Body67 mech assembly. Its top rail, armor,
# recessed channels and manufacturing features remain the original geometry.
receiver=kit('G058','receiver',.36,(0,0,0),cuts=[(2,.025,True),(0,.18,False)])
grip=kit('G132','grip',.20,(-.119,0,-.035),rotation=(0,-.18,0),material=rubber,cuts=[(2,.24,False),(2,-.33,True)])
barrel=empty('barrel_group',(.060,0,.083))
# Three intact sleeves make length; no individual sleeve is stretched.
for j in range(2):kit('M025','barrel_sleeve_'+str(j),.071,(.033+j*.066,0,0),rotation=(0,math.pi/2,0),material=steel,parent=barrel)
nozzle=kit('M019','muzzle',.105,(.169,0,0),rotation=(0,math.pi/2,0),material=steel,parent=barrel)
# Actual source collar is oriented normal to X (the original normal is Y).
kit('G013','breech_collar',.063,(.003,0,0),rotation=(0,0,math.pi/2),material=brass,parent=barrel)
# Split the original grip's lower toe into the removable magazine base.
maggroup=empty('magazine_group',(0,0,0))
kit('G132','magazine_toe',.20,(-.119,0,-.035),rotation=(0,-.18,0),material=steel,cuts=[(2,-.33,False)],parent=maggroup)
empty('muzzle_socket',(.282,0,.083));empty('ejection_socket',(-.045,.048,.09));empty('hand_R',(-.126,0,-.08))
def profile(name,poly,depth,material):
    n=len(poly);v=[(x,y*depth/2,z) for y in(-1,1) for x,z in poly]
    f=[tuple(range(n-1,-1,-1)),tuple(range(n,2*n))]+[(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)]
    mesh=bpy.data.meshes.new(name);mesh.from_pydata(v,[],f);mesh.update();o=bpy.data.objects.new(name,mesh);collection.objects.link(o);o.parent=root;o.data.materials.append(material);objects.append(o);return o
# Small connective geometry is purpose-built for hand clearance.
outer=[(-.087,.008),(.025,.008),(.033,-.009),(.019,-.060),(-.075,-.066),(-.090,-.050)]
inner=[(-.078,-.001),(.018,-.001),(.023,-.010),(.012,-.052),(-.071,-.057),(-.080,-.047)]
n=len(outer);verts=[(x,y,z) for y in(-.007,.007) for loop in(outer,inner) for x,z in loop];faces=[]
for j in range(n):
    k=(j+1)%n
    faces.extend([(j,k,n+k,n+j),(2*n+j,3*n+j,3*n+k,2*n+k),(j,2*n+j,2*n+k,k),(n+j,n+k,3*n+k,3*n+j)])
mesh=bpy.data.meshes.new('guard');mesh.from_pydata(verts,[],faces);mesh.update();guard=bpy.data.objects.new('thin_trigger_guard',mesh);collection.objects.link(guard);guard.parent=root;mesh.materials.append(steel);objects.append(guard)
trigger=profile('seated_trigger',[(-.057,.006),(-.048,.006),(-.044,-.027),(-.050,-.040),(-.058,-.037),(-.052,-.026)],.010,brass)
for o in(guard,trigger):
    bpy.context.view_layer.objects.active=o;mod=o.modifiers.new('Manufactured edge','BEVEL');mod.width=.001;mod.segments=2;bpy.ops.object.modifier_apply(modifier=mod.name)
for o in(root,barrel,maggroup):
    loc=o.location.copy();rot=o.rotation_euler.copy();o.animation_data_create()
    for clip,end in [('idle',60),('fire',12),('alternate',24),('reload',60)]:
        act=bpy.data.actions.new(o.name+'_'+clip);o.animation_data.action=act
        for frame,t in[(1,0),(max(2,end//4),.25),(end//2,.5),(end,1)]:
            pulse=math.sin(math.pi*t);o.location=loc;o.rotation_euler=rot
            if clip in('fire','alternate'):
                if o==root:o.location.x-=.014*pulse;o.rotation_euler.y-=.045*pulse
                if o==barrel:o.location.x-=.012*pulse
            if clip=='reload':
                if o==maggroup:o.location.z-=.17*pulse
                if o==root:o.rotation_euler.x-=.12*pulse
            for path in('location','rotation_euler'):o.keyframe_insert(data_path=path,frame=frame)
        tr=o.animation_data.nla_tracks.new();tr.name=clip;tr.strips.new(clip,1,act);tr.mute=True;o.animation_data.action=None
    o.location=loc;o.rotation_euler=rot
scene.frame_start=1;scene.frame_end=60;scene.frame_set(1)
for o in(root,barrel,maggroup):
    for tr in o.animation_data.nla_tracks:tr.mute=False
bpy.ops.object.select_all(action='DESELECT')
for o in objects:o.select_set(True)
bpy.ops.export_scene.gltf(filepath=str(OUT/'vesper-source.glb'),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='NLA_TRACKS')
for o in(root,barrel,maggroup):
    for tr in o.animation_data.nla_tracks:tr.mute=True
scene.frame_set(1)
for loc,energy,size in [((.3,-.8,1),45,.9),((-.2,.7,.8),60,.8),((.7,.2,.4),18,.4)]:
    bpy.ops.object.light_add(type='AREA',location=loc);o=bpy.context.object;o.data.energy=energy;o.data.size=size;o.rotation_euler=(Vector((0,0,0))-o.location).to_track_quat('-Z','Y').to_euler()
bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';cam.data.ortho_scale=.69;scene.camera=cam
bpy.ops.mesh.primitive_plane_add(size=200,location=(0,0,-.192));floor=bpy.context.object;floor.data.materials.append(mat('Ground',(.025,.035,.045),.1,.7))
for view,loc in [('angle',(.47,-.95,.32)),('side',(.06,-1,.02))]:
    cam.location=loc;cam.rotation_euler=(Vector((.06,0,.01))-cam.location).to_track_quat('-Z','Y').to_euler();scene.render.filepath=str(ROOT/'previews'/f'vesper-source-{view}.png');bpy.ops.render.render(write_still=True)
bpy.data.libraries.write(str(OUT/'vesper-source.blend'),{scene},compress=True)
(ROOT/'reports/vesper_source_assembly.json').write_text(json.dumps({'status':'full-detail sourced assembly; no simplification','components':manifest,'source_triangles':sum(p['triangles'] for p in manifest),'authored_triangles':sum(sum(len(p.vertices)-2 for p in o.data.polygons) for o in(guard,trigger)),'clips':['idle','fire','alternate','reload']},indent=2)+'\n')
print('SOURCE ASSEMBLY COMPLETE',flush=True)
