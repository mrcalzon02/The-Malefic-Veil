"""Refine the approved primitive set once; shared textures, rigid clips and Java component meshes."""
import bpy,math,json,struct,tempfile,random,hashlib
from pathlib import Path
from mathutils import Matrix,Vector
R=Path(__file__).resolve().parents[1];OUT=R/'assets/game/arsenal';OUT.mkdir(parents=True,exist_ok=True)
TMP=R/'build/blender-temp';TMP.mkdir(parents=True,exist_ok=True);tempfile.tempdir=str(TMP)
defs=json.loads((R/'data/runtime-arsenal.json').read_text())
bpy.ops.wm.open_mainfile(filepath=str(R/'assets/game/arsenal-blockouts/arsenal-blockouts.blend'),load_ui=False)
scene=bpy.context.scene;scene.render.fps=30
palette=[(.09,.14,.18),(.38,.46,.50),(.54,.55,.49),(.025,.04,.05),(.49,.28,.10),(.67,.40,.08),(.13,.64,.72),(.55,.18,.65)]
image=bpy.data.images.new('Working arsenal trim',width=512,height=256,alpha=False);rng=random.Random(734211);pixels=[]
for y in range(256):
    for x in range(512):
        band=x//64;c=palette[band];v=rng.uniform(-.012,.012)
        if x%64<2 or y%64<1:v-=.04
        if band==5 and ((x+y)//16)%2:c=(.035,.045,.05)
        if rng.random()<.003:v+=.06
        pixels.extend([*[max(0,min(1,n+v)) for n in c],1])
image.pixels=pixels;image.filepath_raw=str(OUT/'working-trim.png');image.file_format='PNG';image.save();image.pack()
mats=[]
for b in range(8):
    m=bpy.data.materials.new('Working trim '+str(b));m.use_nodes=True;bs=m.node_tree.nodes['Principled BSDF'];tex=m.node_tree.nodes.new('ShaderNodeTexImage');tex.image=image;m.node_tree.links.new(tex.outputs['Color'],bs.inputs['Base Color']);bs.inputs['Metallic'].default_value=.65 if b in(0,1,4) else .1;bs.inputs['Roughness'].default_value=.72 if b==3 else .44
    if b>=6:m.node_tree.links.new(tex.outputs['Color'],bs.inputs['Emission Color']);bs.inputs['Emission Strength'].default_value=1.5
    mats.append(m)
def utf(s):
    b=s.encode('utf-8');return struct.pack('>H',len(b))+b
C=Matrix(((0,1,0,0),(0,0,1,0),(1,0,0,0),(0,0,0,1)))
reports=[]
for wi,d in enumerate(defs):
    col=bpy.data.collections[d['id']];objects=list(col.objects);root=next(o for o in objects if o.name.endswith('__ROOT'));root.location=(0,0,0)
    action=next(o for o in objects if o.name.endswith('__'+d['mechanism']+'_PIVOT'));ammo=next(o for o in objects if o.name.endswith('__magazine_PIVOT'));banks=[o for o in objects if '__rotor_bank_' in o.name]
    for o in objects:
        if o.type!='MESH':continue
        role=o.name.split('__')[-1]
        if len(o.data.polygons)<32 and not any(k in role for k in('bore','trigger','guard')):
            bpy.context.view_layer.objects.active=o;bevel=o.modifiers.new('Working silhouette edge','BEVEL');bevel.width=min(.003,min(o.dimensions)/7);bevel.segments=1
            bpy.ops.object.modifier_apply(modifier=bevel.name)
        band=3 if any(k in role for k in('grip','stock','handle','guard')) else 1 if any(k in role for k in('barrel','prong','rail','ring','disc','tube','breech')) else 4 if any(k in role for k in('magazine','tank','cell','drum','pack')) else 6 if any(k in role for k in('lens','core','aperture','tip','glass')) else 0
        if d['index']>=28 and band==6:band=7
        if 'bore' in role:band=3
        o.data.materials.clear();o.data.materials.append(mats[band]);mesh=o.data;uv=mesh.uv_layers.active or mesh.uv_layers.new(name='WorkingTrimUV')
        low=Vector([min(v.co[i] for v in mesh.vertices) for i in range(3)]);high=Vector([max(v.co[i] for v in mesh.vertices) for i in range(3)])
        for f in mesh.polygons:
            axes=[i for i in range(3) if i!=max(range(3),key=lambda k:abs(f.normal[k]))];a,b=axes
            for j in f.loop_indices:
                v=mesh.vertices[mesh.loops[j].vertex_index].co;u=(v[a]-low[a])/max(1e-5,high[a]-low[a]);v=(v[b]-low[b])/max(1e-5,high[b]-low[b]);uv.data[j].uv=((band+.04+.92*u)/8,.02+.96*v)
        o['trim_band']=band
    bpy.context.view_layer.update()
    meshes=[o for o in objects if o.type=='MESH'];binary=bytearray(struct.pack('>3i',0x4d564153,2,len(meshes)))
    for o in meshes:
        ancestors=[];p=o.parent
        while p is not None:ancestors.append(p);p=p.parent
        pivot=next((p for p in ancestors if p in banks),ammo if ammo in ancestors else action)
        group=2 if ammo in ancestors else 1 if action in ancestors else 0
        pos=C@pivot.matrix_world.translation
        mesh=o.data;mesh.calc_loop_triangles();binary+=utf(o.name.split('__')[-1])+struct.pack('>i3fi',group,*pos,len(mesh.loop_triangles))
        for tri in mesh.loop_triangles:
            uv=mesh.uv_layers.active;u=sum(uv.data[i].uv.x for i in tri.loops)/3;v=sum(uv.data[i].uv.y for i in tri.loops)/3
            x=min(511,int(u*512));y=min(255,int(v*256));c=pixels[(y*512+x)*4:(y*512+x)*4+3];rgb=sum(int(max(0,min(1,n))**(1/2.2)*255)<<shift for n,shift in zip(c,(16,8,0)))
            coords=[C@(o.matrix_world@mesh.vertices[i].co) for i in tri.vertices];binary+=struct.pack('>9fi',*(n for vtx in coords for n in vtx),rgb)
    (OUT/f'{d["id"]}.vmesh').write_bytes(binary)
    driven=[root,action,ammo]+banks;bind={o:(o.location.copy(),o.rotation_euler.copy()) for o in driven}
    for clip,end in [('idle',60),('fire',12),('alternate',24),('reload',round(d['reload_ms']*.03))]:
        for o in driven:
            loc,rot=bind[o];o.animation_data_create();act=bpy.data.actions.new(d['id']+'_'+o.name.split('__')[-1]+'_'+clip);o.animation_data.action=act
            for frame,t in[(1,0),(max(2,end//4),.25),(max(3,end//2),.5),(end,1)]:
                pulse=math.sin(math.pi*t);o.location=loc;o.rotation_euler=rot
                if clip=='idle' and o==root:o.location.z+=math.sin(math.tau*t)*.0015
                if clip in('fire','alternate'):
                    if o==root:o.location.x-=pulse*.016;o.location.z+=pulse*.006
                    if o in banks or o==action and d['mechanism'] in('rotor','rings','disc','cylinder'):
                        if d['mechanism']=='disc':o.rotation_euler.z+=math.tau*2*t
                        else:o.rotation_euler.x+=math.tau*(1 if d['mechanism']=='cylinder' else 2)*t
                    if o==action and d['mechanism'] in('slide','bolt','pump'):o.location.x-=pulse*.045
                    if o==action and d['mechanism']=='hinge':o.rotation_euler.y+=pulse*.06
                if clip=='reload':
                    if o==ammo:o.location.z-=pulse*.28
                    if o==root:o.rotation_euler.x-=pulse*.13
                    if o==action and d['mechanism'] in('hinge','drum','cylinder'):o.rotation_euler.y+=pulse*.55
                for path in('location','rotation_euler'):o.keyframe_insert(data_path=path,frame=frame)
            tr=o.animation_data.nla_tracks.new();tr.name=clip;tr.strips.new(clip,1,act);tr.mute=True;o.animation_data.action=None;o.location=loc;o.rotation_euler=rot
    for c in scene.collection.children:
        if c.name.startswith('w'):c.hide_render=c!=col
    bpy.ops.object.select_all(action='DESELECT')
    for o in objects:o.select_set(True)
    for o in driven:
        for tr in o.animation_data.nla_tracks:tr.mute=False
    scene.frame_start=1;scene.frame_end=max(60,round(d['reload_ms']*.03))
    bpy.ops.export_scene.gltf(filepath=str(OUT/f'{d["id"]}.glb'),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='NLA_TRACKS')
    for o in driven:
        for tr in o.animation_data.nla_tracks:tr.mute=True
        o.location,o.rotation_euler=bind[o]
    scene.frame_set(1);bpy.context.view_layer.update()
    if wi in(0,4,7,10,14,18,22,25,28,31):
        points=[o.matrix_world@Vector(v) for o in meshes for v in o.bound_box];lo=Vector([min(v[i] for v in points) for i in range(3)]);hi=Vector([max(v[i] for v in points) for i in range(3)]);center=(lo+hi)/2;span=hi-lo
        cam=scene.camera;cam.location=center+Vector((span.x*.8,-span.x*1.9,span.x*.66));cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.ortho_scale=max(span.x*1.3,span.z*1.9,span.y*1.7)
        scene.render.filepath=str(R/'previews'/f'working-{d["id"]}.png');bpy.ops.render.render(write_still=True)
    reports.append({'id':d['id'],'triangles':sum(len(o.data.loop_triangles) for o in meshes),'components':len(meshes),'clips':['idle','fire','alternate','reload'],'sha256':hashlib.sha256(binary).hexdigest()})
    root.location=((wi%4)*2.3,(wi//4)*1.8,0)
    print('PREPARED',d['id'],flush=True)
for c in scene.collection.children:c.hide_render=False
bpy.data.libraries.write(str(OUT/'working-arsenal.blend'),{scene},compress=True)
(R/'reports/working_arsenal.json').write_text(json.dumps(reports,indent=2)+'\n')
print('32 WORKING TEXTURED / ANIMATED ASSETS READY',flush=True)
