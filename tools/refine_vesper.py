"""One-weapon geometry/texture pass, staged for independent exported-model review."""
import bpy,bmesh,json,sys,math,tempfile,hashlib
from pathlib import Path
from mathutils import Vector,Matrix
R=Path(__file__).resolve().parents[1]
texture='--texture' in sys.argv
attempt=sys.argv[sys.argv.index('--attempt')+1] if '--attempt' in sys.argv else '1'
ident=('w01-vesper-finish-r' if texture else 'w01-vesper-mechanical-r')+attempt
out=R/'build/candidates/weapon-refinement'/ident;out.mkdir(parents=True,exist_ok=True);tempfile.tempdir=str(out)
source=R/'build/candidates/weapon-refinement'/('w01-vesper-mechanical-r'+attempt+'/w01-vesper-mechanical-r'+attempt+'.blend' if texture else 'w01-vesper-refined-v3/w01-vesper-refined-v3.blend')
bpy.ops.wm.open_mainfile(filepath=str(source),load_ui=False)
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=8
objects=[o for o in scene.objects if o.type=='MESH'];root=next(o for o in scene.objects if o.name.endswith('__ROOT'))
def find(role):return next(o for o in objects if o.name.endswith('__'+role))
def pts(o):return [o.matrix_world@v.co for v in o.data.vertices]
def box_bounds(o):
    p=pts(o);return Vector([min(v[i] for v in p) for i in range(3)]),Vector([max(v[i] for v in p) for i in range(3)])
def cleanup(o):
    bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bmesh.ops.dissolve_degenerate(bm,edges=list(bm.edges),dist=1e-6);bmesh.ops.triangulate(bm,faces=list(bm.faces));bad=[f for f in bm.faces if f.calc_area()<1e-10]
    if bad:bmesh.ops.delete(bm,geom=bad,context='FACES')
    bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(o.data);bm.free();o.data.update()
for o in scene.objects:
    if o.animation_data:
        o.animation_data.action=None
        for tr in o.animation_data.nla_tracks:tr.mute=True
scene.frame_set(1);bpy.context.view_layer.update()
if not texture:
    grip=find('grip_shell');well=find('pistol_box_magazine_well');body=find('receiver_chassis');ammo=next(o for o in scene.objects if o.name.endswith('__magazine_PIVOT'))
    parent=grip.parent;oldmat=grip.data.materials[0];oldlo,oldhi=box_bounds(grip)
    ztop=oldhi.z;zbottom=oldlo.z;slope=.025/(ztop-zbottom)
    def cx(z):return -.077+slope*(z-ztop)
    def shape(name,sections,parent,band):
        vertices=[]
        for z,x,rx,ry,ch in sections:
            for dx,dy in [(-rx+ch,-ry),(rx-ch,-ry),(rx,-ry+ch),(rx,ry-ch),(rx-ch,ry),(-rx+ch,ry),(-rx,ry-ch),(-rx,-ry+ch)]:vertices.append((x+dx,dy,z))
        faces=[tuple(range(7,-1,-1)),tuple((len(sections)-1)*8+i for i in range(8))]
        for k in range(len(sections)-1):
            for i in range(8):a=k*8+i;b=k*8+(i+1)%8;faces.append((a,b,b+8,a+8))
        me=bpy.data.meshes.new(name);me.from_pydata(vertices,[],faces);me.update();o=bpy.data.objects.new('w01-vesper__'+name,me);scene.collection.objects.link(o);o.parent=parent;o.matrix_parent_inverse=parent.matrix_world.inverted();o.matrix_basis=Matrix.Identity(4);o['trim_band']=band;o.data.materials.append(oldmat);objects.append(o);cleanup(o);return o
    def remove(o):objects.remove(o);bpy.data.objects.remove(o,do_unlink=True)
    remove(grip);remove(well)
    for name in('pistol_box_magazine','pistol_box_magazine_floorplate','pistol_box_magazine_feed_lips'):remove(find(name))
    grip=shape('grip_shell',[(zbottom,cx(zbottom),.040,.033,.007),(zbottom+.017,cx(zbottom+.017),.035,.029,.006),(ztop-.035,cx(ztop-.035),.036,.031,.006),(ztop,cx(ztop),.033,.031,.006)],parent,3)
    well=shape('pistol_box_magazine_well',[(ztop-.023,cx(ztop-.023),.030,.027,.005),(ztop+.003,cx(ztop+.003),.030,.027,.005)],parent,0)
    magazine=shape('pistol_box_magazine',[(zbottom-.004,cx(zbottom-.004),.018,.015,.003),(ztop-.005,cx(ztop-.005),.017,.014,.003)],ammo,1)
    shape('pistol_box_magazine_floorplate',[(zbottom-.014,cx(zbottom-.014),.026,.021,.004),(zbottom-.003,cx(zbottom-.003),.026,.021,.004)],ammo,1)
    shape('pistol_box_magazine_feed_lips',[(ztop-.007,cx(ztop-.007),.014,.013,.002),(ztop+.006,cx(ztop+.006),.014,.013,.002)],ammo,1)
    cutter=shape('Temporary magazine clearance',[(zbottom-.035,cx(zbottom-.035),.0195,.0165,.003),(ztop+.010,cx(ztop+.010),.0195,.0165,.003)],parent,0)
    for o in(grip,well,body):
        bpy.context.view_layer.objects.active=o;mod=o.modifiers.new('Measured magazine insertion clearance','BOOLEAN');mod.operation='DIFFERENCE';mod.solver='EXACT';mod.object=cutter;bpy.ops.object.modifier_apply(modifier=mod.name);cleanup(o)
    remove(cutter)
    # Recessed grip grooves remain part of the continuous housing, with wall behind them.
    for side in(-1,1):
        for j in range(3):
            z=zbottom+.038+j*.022;bpy.ops.mesh.primitive_cube_add(size=1,location=(cx(z),side*.0295,z));cut=bpy.context.object;cut.scale=(.034,.004,.0035);bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
            bpy.context.view_layer.objects.active=grip;m=grip.modifiers.new('Cut grip groove','BOOLEAN');m.operation='DIFFERENCE';m.object=cut;bpy.ops.object.modifier_apply(modifier=m.name);bpy.data.objects.remove(cut,do_unlink=True)
    cleanup(grip)
    # Removal is parallel to the magazine and its well, avoiding wall penetration.
    drivers=[root,ammo,next(o for o in scene.objects if o.name.endswith('__slide_PIVOT'))];bind={o:o.matrix_basis.copy() for o in drivers}
    for o in drivers:o.animation_data_clear()
    for clip,end in [('idle',60),('fire',12),('alternate',24),('reload',60)]:
        for o in drivers:
            o.animation_data_create();act=bpy.data.actions.new(ident+'_'+o.name+'_'+clip);o.animation_data.action=act
            for frame,t in[(1,0),(max(2,end//4),.25),(max(3,end//2),.5),(max(4,end*3//4),.75),(end,1)]:
                o.matrix_basis=bind[o];pulse=math.sin(math.pi*t)
                if clip=='idle' and o==root:o.location.z+=math.sin(math.tau*t)*.0015
                if clip in('fire','alternate'):
                    if o==root:o.location.x-=.012*pulse
                    if o not in(root,ammo):o.location.x-=.027*pulse
                if clip=='reload' and o==ammo:o.location+=Vector((-slope*.25,0,-.25))*pulse
                for path in('location','rotation_euler','scale'):o.keyframe_insert(data_path=path,frame=frame)
            tr=o.animation_data.nla_tracks.new();tr.name=clip;tr.strips.new(clip,1,act);tr.mute=True;o.animation_data.action=None;o.matrix_basis=bind[o]
    scene.frame_set(1);bpy.context.view_layer.update()
    # Sights sit on the receiver spine; they are functional connected pieces.
    spine=find('upper_spine');_,stop=box_bounds(spine);z=stop.z
    def box(name,center,size):
        bpy.ops.mesh.primitive_cube_add(size=1,location=center);o=bpy.context.object;o.name='w01-vesper__'+name;o.scale=size;bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
        m=o.matrix_world.copy();o.parent=spine.parent;o.matrix_world=m
        bpy.context.view_layer.objects.active=o;b=o.modifiers.new('Sight edge','BEVEL');b.width=.0007;b.segments=1;bpy.ops.object.modifier_apply(modifier=b.name)
        o['mechanical_role']=name;o['trim_band']=1;objects.append(o);return o
    box('front_sight',(.104,0,z+.007),(.013,.011,.016))
    # A connected U-shaped rear sight, rather than two unattached blocks.
    rear=box('rear_sight',(-.105,0,z+.007),(.018,.040,.018))
    bpy.ops.mesh.primitive_cube_add(size=1,location=(-.105,0,z+.014));notch=bpy.context.object;notch.scale=(.024,.015,.016);bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    bpy.context.view_layer.objects.active=rear;b=rear.modifiers.new('Open rear sight notch','BOOLEAN');b.operation='DIFFERENCE';b.object=notch;bpy.ops.object.modifier_apply(modifier=b.name);bpy.data.objects.remove(notch,do_unlink=True);cleanup(rear)
    if int(attempt)>=3:
        # Replace the slide block with a manufactured cross-section and tapered nose.
        slide=find('slide');sl,sh=box_bounds(slide);sp=slide.parent;smat=slide.data.materials[0]
        cross=[(-.030,-.021),(.030,-.021),(.036,-.014),(.036,.011),(.024,.021),(-.024,.021),(-.036,.011),(-.036,-.014)]
        verts=[]
        for x,scale in((sl.x,.92),(sl.x+.012,1),(sh.x-.032,1),(sh.x,.78)):
            verts.extend((x,y*scale,.065+z*scale) for y,z in cross)
        faces=[tuple(range(7,-1,-1)),tuple(24+i for i in range(8))]
        for k in range(3):
            for i in range(8):a=k*8+i;b=k*8+(i+1)%8;faces.append((a,b,b+8,a+8))
        me=bpy.data.meshes.new('Profiled reciprocating slide');me.from_pydata(verts,[],faces);me.update();slide.data=me;slide.matrix_parent_inverse=sp.matrix_world.inverted();slide.matrix_basis=Matrix.Identity(4);slide.data.materials.append(smat);slide['trim_band']=0;cleanup(slide)
        def recess(o,center,size,label):
            bpy.ops.mesh.primitive_cube_add(size=1,location=center);c=bpy.context.object;c.scale=size;bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
            bpy.context.view_layer.objects.active=o;m=o.modifiers.new(label,'BOOLEAN');m.operation='DIFFERENCE';m.solver='EXACT';m.object=c;bpy.ops.object.modifier_apply(modifier=m.name);bpy.data.objects.remove(c,do_unlink=True);cleanup(o)
        for side in(-1,1):
            for j in range(5):recess(slide,(-.109+j*.010,side*.036,.060),(.004,.006,.027),'Recessed cocking serration')
        # This port exposes the breech in the receiver, rather than a floating black sticker.
        recess(slide,(.016,-.034,.064),(.045,.020,.024),'Open ejection port')
        # Shape the trigger into one continuous curved blade inside the existing guard.
        trigger=find('trigger');tl,th=box_bounds(trigger);tx=(tl.x+th.x)/2
        outline=[(tx-.005,th.z),(tx+.003,th.z),(tx+.006,th.z-.015),(tx+.003,th.z-.027),(tx-.004,tl.z),(tx-.013,tl.z+.003),(tx-.005,th.z-.026),(tx-.002,th.z-.015)]
        vv=[(x,y,z) for y in(-.005,.005) for x,z in outline];n=len(outline);ff=[tuple(range(n-1,-1,-1)),tuple(n+i for i in range(n))]
        ff.extend((i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n));me=bpy.data.meshes.new('Continuous curved trigger blade');me.from_pydata(vv,[],ff);me.update();mat=trigger.data.materials[0];trigger.data=me;trigger.matrix_parent_inverse=trigger.parent.matrix_world.inverted();trigger.matrix_basis=Matrix.Identity(4);trigger.data.materials.append(mat);cleanup(trigger)
        # Pull fasteners into their service plates; previous bosses stood off the housing.
        for side in(-1,1):
            fast=find('panel_fastener_'+str(side));panel=find('receiver_service_panel_'+str(side));fl,fh=box_bounds(fast);pl,ph=box_bounds(panel);delta=(ph.y-.001-fl.y) if side==1 else (pl.y+.001-fh.y)
            for v in fast.data.vertices:v.co.y+=delta
    for o in objects:
        # Keep the existing trim during geometry review, including new Boolean faces.
        if not o.data.materials:o.data.materials.append(spine.data.materials[0])
        band=int(o.get('trim_band',0));uv=o.data.uv_layers.active or o.data.uv_layers.new();v=o.data.vertices;lo=Vector([min(a.co[i] for a in v) for i in range(3)]);hi=Vector([max(a.co[i] for a in v) for i in range(3)])
        for f in o.data.polygons:
            axes=[i for i in range(3) if i!=max(range(3),key=lambda k:abs(f.normal[k]))];a,b=axes
            for i in f.loop_indices:
                p=v[o.data.loops[i].vertex_index].co;u=(p[a]-lo[a])/max(hi[a]-lo[a],1e-6);vv=(p[b]-lo[b])/max(hi[b]-lo[b],1e-6);uv.data[i].uv=((band+.04+u*.92)/8,.02+.96*vv)
else:
    # Mechanical geometry is frozen. Author materials, unwrap and bake disjoint UV tiles.
    palette={0:(.070,.095,.115),1:(.28,.34,.37),3:(.020,.029,.033),4:(.39,.22,.075)}
    finishes={0:(.46,.65),1:(.32,.90),3:(.82,.0),4:(.42,.80)}
    stencil=bpy.data.images.load(str(R/'build/candidates/weapon-refinement/finish-sources/vesper-stencil.png'))
    for o in objects:
        band=int(o.get('trim_band',0));c=palette.get(band,palette[0]);m=bpy.data.materials.new(o.name+' finish source');m.use_nodes=True;n=m.node_tree.nodes;l=m.node_tree.links;bs=n.get('Principled BSDF')
        bs.inputs['Base Color'].default_value=(*c,1);bs.inputs['Metallic'].default_value=.65 if band in(0,1,4) else .02;bs.inputs['Roughness'].default_value=.82 if band==3 else .46
        coord=n.new('ShaderNodeTexCoord');noise=n.new('ShaderNodeTexNoise');noise.inputs['Scale'].default_value=140;noise.inputs['Detail'].default_value=2;l.new(coord.outputs['Generated'],noise.inputs['Vector'])
        ramp=n.new('ShaderNodeValToRGB');ramp.color_ramp.elements[0].color=(*[v*.88 for v in c],1);ramp.color_ramp.elements[1].color=(*[min(1,v*1.10) for v in c],1);l.new(noise.outputs['Fac'],ramp.inputs[0]);l.new(ramp.outputs['Color'],bs.inputs['Base Color'])
        if 'receiver_service_panel' in o.name:
            sep=n.new('ShaderNodeSeparateXYZ');uv=n.new('ShaderNodeCombineXYZ');l.new(coord.outputs['Generated'],sep.inputs[0]);l.new(sep.outputs['X'],uv.inputs['X']);l.new(sep.outputs['Z'],uv.inputs['Y'])
            if o.name.endswith('panel_1'):
                flip=n.new('ShaderNodeMath');flip.operation='SUBTRACT';flip.inputs[0].default_value=1;l.new(sep.outputs['X'],flip.inputs[1]);l.new(flip.outputs[0],uv.inputs['X'])
            t=n.new('ShaderNodeTexImage');t.image=stencil;t.extension='CLIP';l.new(uv.outputs[0],t.inputs['Vector']);mix=n.new('ShaderNodeMixRGB');l.new(t.outputs['Alpha'],mix.inputs[0]);l.new(ramp.outputs['Color'],mix.inputs[1]);l.new(t.outputs['Color'],mix.inputs[2]);l.new(mix.outputs[0],bs.inputs['Base Color'])
        if band==3:
            wave=n.new('ShaderNodeTexWave');wave.wave_type='BANDS';wave.bands_direction='DIAGONAL';wave.inputs['Scale'].default_value=60;l.new(coord.outputs['Generated'],wave.inputs['Vector']);mix=n.new('ShaderNodeMixRGB');mix.inputs[1].default_value=(*[v*.72 for v in c],1);mix.inputs[2].default_value=(*[v*1.3 for v in c],1);l.new(wave.outputs['Fac'],mix.inputs[0]);l.new(mix.outputs[0],bs.inputs['Base Color'])
        o.data.materials.clear();o.data.materials.append(m)
        for f in o.data.polygons:f.material_index=0
    for o in objects:
        bpy.ops.object.select_all(action='DESELECT');o.select_set(True);bpy.context.view_layer.objects.active=o;bpy.ops.object.mode_set(mode='EDIT');bpy.ops.mesh.select_all(action='SELECT');bpy.ops.uv.smart_project(angle_limit=1.1,island_margin=.025);bpy.ops.object.mode_set(mode='OBJECT')
    # Every physical part has its own padded atlas cell, including hidden magazine surfaces.
    grid=math.ceil(math.sqrt(len(objects)));size=1024;pad=3/size
    for i,o in enumerate(objects):
        x=i%grid/grid;y=i//grid/grid;cell=[x+pad,y+pad,x+1/grid-pad,y+1/grid-pad];o['atlas_uv_cell']=cell
        for u in o.data.uv_layers.active.data:u.uv=(cell[0]+u.uv.x*(cell[2]-cell[0]),cell[1]+u.uv.y*(cell[3]-cell[1]))
    atlas=bpy.data.images.new('Vesper baked albedo',size,size);scene.render.bake.use_pass_direct=False;scene.render.bake.use_pass_indirect=False;scene.render.bake.use_pass_color=True;scene.render.bake.margin=2
    for o in objects:
        n=o.data.materials[0].node_tree.nodes.new('ShaderNodeTexImage');n.image=atlas;o.data.materials[0].node_tree.nodes.active=n
    bpy.ops.object.select_all(action='DESELECT')
    for o in objects:o.select_set(True)
    bpy.context.view_layer.objects.active=objects[0]
    bpy.ops.object.bake(type='DIFFUSE');atlas.filepath_raw=str(out/'vesper-albedo.png');atlas.file_format='PNG';atlas.save();atlas.pack()
    # A linear ORM image preserves rubber versus coated receiver versus bare steel.
    orm=bpy.data.images.new('Vesper baked ORM',size,size);orm.colorspace_settings.name='Non-Color'
    for o in objects:
        mat=o.data.materials[0];n=mat.node_tree.nodes;l=mat.node_tree.links;rough,metal=finishes.get(int(o.get('trim_band',0)),finishes[0]);em=n.new('ShaderNodeEmission');em.inputs['Color'].default_value=(1,rough,metal,1);l.new(em.outputs[0],n.get('Material Output').inputs['Surface']);t=n.new('ShaderNodeTexImage');t.image=orm;n.active=t
    bpy.ops.object.bake(type='EMIT');orm.filepath_raw=str(out/'vesper-orm.png');orm.file_format='PNG';orm.save();orm.pack()
    shared=bpy.data.materials.new('Vesper shared baked PBR material');shared.use_nodes=True;n=shared.node_tree.nodes;l=shared.node_tree.links;t=n.new('ShaderNodeTexImage');t.image=atlas;bs=n.get('Principled BSDF');l.new(t.outputs['Color'],bs.inputs['Base Color']);pbr=n.new('ShaderNodeTexImage');pbr.image=orm;split=n.new('ShaderNodeSeparateColor');l.new(pbr.outputs['Color'],split.inputs[0]);l.new(split.outputs['Green'],bs.inputs['Roughness']);l.new(split.outputs['Blue'],bs.inputs['Metallic'])
    for o in objects:o.data.materials.clear();o.data.materials.append(shared)
bpy.context.view_layer.update()
path=out/(ident+'.glb');bpy.ops.object.select_all(action='SELECT')
for o in scene.objects:
    if o.animation_data:
        for tr in o.animation_data.nla_tracks:tr.mute=False
bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='NLA_TRACKS',export_extras=True)
for o in scene.objects:
    if o.animation_data:
        for tr in o.animation_data.nla_tracks:tr.mute=True
scene.frame_set(1);bpy.data.libraries.write(str(out/(ident+'.blend')),{scene},compress=True)
pts_all=[o.matrix_world@v.co for o in objects for v in o.data.vertices];low=[min(v[i] for v in pts_all) for i in range(3)];high=[max(v[i] for v in pts_all) for i in range(3)]
contract={'placement_kind':'snap-component','component_function':'handheld assembly only; not a construction plot','local_bounds_z_up':[[v-.002 for v in low],[v+.002 for v in high]],'organic':False,'continuous_surfaces':[],'intentional_separate_meshes':[o.name for o in objects],'isolated_atlas_cells':texture,'reload_method':'complete box magazine withdraws along the raked grip well','reload_objects':['pistol_box_magazine','pistol_box_magazine_floorplate','pistol_box_magazine_feed_lips'],'forbidden_name_tokens':['grip_channel']}
(out/'contract.json').write_text(json.dumps(contract,indent=2)+'\n')
(out/'design-record.json').write_text(json.dumps({'id':ident,'source':source.relative_to(R).as_posix(),'asset_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'status':'UNREVIEWED','geometry_frozen_for_texturing':texture,'changes':['preserve measured barrel axis','cut magazine-sized clearance through grip and well','connected sights with open rear notch','grip grooves are subtracted geometry, no separate floating strips','profiled slide, cut cocking serrations, ejection port and curved trigger for r3 onward'],'texture_bake':'1024 shared albedo and linear ORM, disjoint padded UV cells' if texture else 'existing trim retained'},indent=2)+'\n')
policy=json.loads((R/'data/structure-quality-policy.json').read_text());record={'id':ident,'path':path.relative_to(R).as_posix(),'category':'weapon-rough-candidate'}
(out/'inspection-input.json').write_text(json.dumps(policy['reference_sources']+[record],indent=2)+'\n')
(out/'contracts.json').write_text(json.dumps({ident:contract},indent=2)+'\n')
(R/'build/candidates/weapon-refinement/inspection-input.json').write_text(json.dumps([record],indent=2)+'\n')
print('VESPER CANDIDATE',ident,flush=True)
