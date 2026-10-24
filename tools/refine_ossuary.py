"""Targeted revolver repair. Blender --python ... -- [--texture]."""
import bpy,bmesh,json,sys,math,hashlib,tempfile
from pathlib import Path
from mathutils import Vector,Matrix
R=Path(__file__).resolve().parents[1];sys.path.insert(0,str(R/'tools'));texture='--texture' in sys.argv
ident='w02-ossuary-finish-r3' if texture else 'w02-ossuary-mechanical-r3';out=R/'build/candidates/weapon-refinement'/ident;out.mkdir(parents=True,exist_ok=True);tempfile.tempdir=str(out)
source=R/'build/candidates/weapon-refinement'/('w02-ossuary-mechanical-r3/w02-ossuary-mechanical-r3.blend' if texture else 'w02-ossuary-refined-r1/w02-ossuary-refined-r1.blend')
bpy.ops.wm.open_mainfile(filepath=str(source),load_ui=False);scene=bpy.context.scene
for o in scene.objects:
    if o.animation_data:
        o.animation_data.action=None
        for tr in o.animation_data.nla_tracks:tr.mute=True
scene.frame_set(1);bpy.context.view_layer.update()
def find(s):return next(o for o in scene.objects if o.name.endswith('__'+s))
root=find('ROOT');receiver=find('receiver_group');ammo=find('magazine_PIVOT')
def clean(o):
    bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bmesh.ops.dissolve_degenerate(bm,edges=list(bm.edges),dist=1e-6);bmesh.ops.triangulate(bm,faces=list(bm.faces));bad=[f for f in bm.faces if f.calc_area()<1e-10]
    if bad:bmesh.ops.delete(bm,geom=bad,context='FACES')
    bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(o.data);bm.free();o.data.update()
def mesh(name,verts,faces,parent=receiver,band=0):
    me=bpy.data.meshes.new(name);me.from_pydata(verts,[],faces);me.update();o=bpy.data.objects.new('w02-ossuary__'+name,me);scene.collection.objects.link(o);o.parent=parent;o.matrix_parent_inverse=parent.matrix_world.inverted();o.matrix_basis=Matrix.Identity(4);o['trim_band']=band;clean(o);return o
def box(name,center,size,parent=receiver,band=0):
    x,y,z=center;a,b,c=[v/2 for v in size];o=mesh(name,[(x+sx*a,y+sy*b,z+sz*c) for sx,sy,sz in[(-1,-1,-1),(1,-1,-1),(1,1,-1),(-1,1,-1),(-1,-1,1),(1,-1,1),(1,1,1),(-1,1,1)]],[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)],parent,band);bpy.context.view_layer.objects.active=o;m=o.modifiers.new('Manufactured edge','BEVEL');m.width=min(.0015,min(size)*.1);m.segments=1;bpy.ops.object.modifier_apply(modifier=m.name);clean(o);return o
def cut(o,center,size):
    c=box('temporary cutter',center,size);bpy.context.view_layer.objects.active=o;m=o.modifiers.new('Mechanical clearance','BOOLEAN');m.operation='DIFFERENCE';m.solver='EXACT';m.object=c;bpy.ops.object.modifier_apply(modifier=m.name);bpy.data.objects.remove(c,do_unlink=True);clean(o)
def cyl(name,x0,x1,y,z,r,parent=receiver,band=1,n=20):
    verts=[(x,y+math.cos(j*math.tau/n)*r,z+math.sin(j*math.tau/n)*r) for x in(x0,x1) for j in range(n)];faces=[tuple(range(n-1,-1,-1)),tuple(n+j for j in range(n))];faces.extend((j,(j+1)%n,(j+1)%n+n,j+n) for j in range(n));return mesh(name,verts,faces,parent,band)
if not texture:
    # Rebuild the cylinder so its top chamber is coaxial with +X barrel at z=.035.
    for o in list(scene.objects):
        if any(o.name.endswith('__'+s) for s in('swing_out_cylinder','cylinder_crane','hammer','ejection_recess')) or '__revolver_round_' in o.name:bpy.data.objects.remove(o,do_unlink=True)
    cylinder=cyl('swing_out_cylinder',-.021,.091,0,.008,.045,ammo,1)
    chamber_centers=[]
    for j in range(6):
        a=math.pi/2+j*math.tau/6;y=math.cos(a)*.027;z=.008+math.sin(a)*.027;chamber_centers.append([y,z]);c=cyl('chamber cutter',-.027,.097,y,z,.0108)
        bpy.context.view_layer.objects.active=cylinder;m=cylinder.modifiers.new('Six actual chambers','BOOLEAN');m.operation='DIFFERENCE';m.solver='EXACT';m.object=c;bpy.ops.object.modifier_apply(modifier=m.name);bpy.data.objects.remove(c,do_unlink=True)
        cyl('revolver_round_'+str(j),-.013,.083,y,z,.009,ammo,4,12)
    clean(cylinder);cylinder['chamber_centers_yz']=chamber_centers;cylinder['barrel_chamber_index']=0
    for name in('receiver_chassis','receiver_service_panel_-1','receiver_service_panel_1'):cut(find(name),(.035,0,.008),(.123,.13,.099))
    # The lower frame clears both the cylinder and a usable trigger space.
    outline=[(-.070,-.020),(-.028,-.020),(-.028,-.039),(.095,-.039),(.095,-.014),(.122,-.014),(.122,-.054),(-.070,-.054)];vv=[(x,y,z) for y in(-.0175,.0175) for x,z in outline];n=len(outline);ff=[tuple(range(n-1,-1,-1)),tuple(n+i for i in range(n))];ff.extend((i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n));mesh('lower_frame_bridge',vv,ff)
    guard=find('trigger_guard')
    for v in guard.data.vertices:v.co.z-=.028
    trigger=find('trigger');outline=[(-.042,-.036),(-.034,-.036),(-.032,-.050),(-.036,-.064),(-.044,-.075),(-.052,-.072),(-.042,-.061),(-.040,-.050)];vv=[(x,y,z) for y in(-.005,.005) for x,z in outline];ff=[tuple(range(7,-1,-1)),tuple(8+i for i in range(8))];ff.extend((i,(i+1)%8,(i+1)%8+8,i+8) for i in range(8));me=bpy.data.meshes.new('Curved trigger');me.from_pydata(vv,[],ff);me.update();trigger.data=me;trigger.matrix_parent_inverse=trigger.parent.matrix_world.inverted();trigger.matrix_basis=Matrix.Identity(4);trigger['trim_band']=1;clean(trigger)
    # Remove the grip's inappropriate magazine cavity: this weapon loads its cylinder.
    # Solid backstrap infill stays entirely within the old grip shell.
    box('solid_revolver_backstrap',(-.086,0,-.081),(.028,.030,.119),band=3)
    # Connected hammer profile, with a visible pivot seat in the rear frame.
    outline=[(-.124,.027),(-.103,.027),(-.101,.064),(-.115,.082),(-.151,.101),(-.157,.093),(-.130,.068)];vv=[(x,y,z) for y in(-.010,.010) for x,z in outline];n=len(outline);ff=[tuple(range(n-1,-1,-1)),tuple(n+i for i in range(n))];ff.extend((i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n));hammer=mesh('hammer',vv,ff,band=1)
    # Front/rear sights sit on the spine/barrel; no detached sight cubes.
    box('front_sight',(.269,0,.064),(.016,.010,.014),band=1);box('rear_sight',(-.092,0,.103),(.019,.031,.012),band=1);cut(find('rear_sight'),(-.092,0,.111),(.025,.012,.013))
    # Trim the barrel rear to the cylinder gap without rotating its local axis again.
    barrel=find('barrel')
    for v in barrel.data.vertices:
        if v.co.x<.080:v.co.x+=.046
    clean(barrel)
    for side in(-1,1):
        fast=find('panel_fastener_'+str(side))
        for v in fast.data.vertices:v.co.y-=side*.004
    # Real crane rotates with the cylinder around a fixed longitudinal hinge.
    hinge=Vector((-.021,-.045,-.054));old={o:o.matrix_world.copy() for o in ammo.children};ammo.matrix_world=Matrix.Translation(hinge)
    for o,m in old.items():o.matrix_world=m
    cyl('crane_hinge',-.039,.001,-.045,-.054,.011,receiver,1)
    box('crane_hinge_seat',(-.025,-.029,-.048),(.031,.040,.025),band=0)
    start=hinge;end=Vector((-.012,0,.008));direction=(end-start).normalized();u=Vector((1,0,0));v=direction.cross(u).normalized();vv=[p+u*su*.009+v*sv*.007 for p in(start,end) for su,sv in[(-1,-1),(1,-1),(1,1),(-1,1)]];mesh('cylinder_crane',vv,[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)],ammo,1)
    index=bpy.data.objects.new('w02-ossuary__cylinder_index_PIVOT',None);scene.collection.objects.link(index);index.parent=ammo;index.matrix_world=Matrix.Translation((.035,0,.008))
    for o in list(ammo.children):
        if o==cylinder or '__revolver_round_' in o.name:m=o.matrix_world.copy();o.parent=index;o.matrix_world=m
    hammer_pivot=bpy.data.objects.new('w02-ossuary__hammer_PIVOT',None);scene.collection.objects.link(hammer_pivot);hammer_pivot.parent=root;hammer_pivot.matrix_world=Matrix.Translation((-.114,0,.038));m=hammer.matrix_world.copy();hammer.parent=hammer_pivot;hammer.matrix_world=m
    drivers=[root,ammo,index,hammer_pivot];bind={o:o.matrix_basis.copy() for o in drivers}
    for o in scene.objects:
        if o.animation_data:o.animation_data_clear()
    for clip,end in [('idle',60),('fire',12),('alternate',24),('reload',60)]:
        for o in drivers:
            o.animation_data_create();act=bpy.data.actions.new(ident+'_'+o.name+'_'+clip);o.animation_data.action=act
            for frame,t in [(1,0),(max(2,end//4),.25),(max(3,end//2),.5),(max(4,end*3//4),.75),(end,1)]:
                o.matrix_basis=bind[o];pulse=math.sin(math.pi*t)
                if clip=='idle' and o==root:o.location.z+=math.sin(math.tau*t)*.0015
                if clip in('fire','alternate'):
                    if o==root:o.location.x-=.014*pulse
                    if o==index:o.rotation_euler.x+=math.tau/6*t
                    if o==hammer_pivot:o.rotation_euler.y-=.32*pulse
                if clip=='reload' and o==ammo:o.rotation_euler.x+=1.65*pulse
                for path in('location','rotation_euler','scale'):o.keyframe_insert(data_path=path,frame=frame)
            tr=o.animation_data.nla_tracks.new();tr.name=clip;tr.strips.new(clip,1,act);tr.mute=True;o.animation_data.action=None;o.matrix_basis=bind[o]
    for o in scene.objects:
        if o.type!='MESH':continue
        uv=o.data.uv_layers.active or o.data.uv_layers.new();band=int(o.get('trim_band',0));mat=next(m for m in bpy.data.materials if m.use_nodes and any(n.type=='TEX_IMAGE' and n.image for n in m.node_tree.nodes));o.data.materials.clear();o.data.materials.append(mat)
        lo=Vector([min(v.co[i] for v in o.data.vertices) for i in range(3)]);hi=Vector([max(v.co[i] for v in o.data.vertices) for i in range(3)])
        for f in o.data.polygons:
            axes=[i for i in range(3) if i!=max(range(3),key=lambda k:abs(f.normal[k]))];a,b=axes
            for i in f.loop_indices:
                v=o.data.vertices[o.data.loops[i].vertex_index].co;uv.data[i].uv=((band+.04+.92*(v[a]-lo[a])/max(hi[a]-lo[a],1e-6))/8,.02+.96*(v[b]-lo[b])/max(hi[b]-lo[b],1e-6))
else:
    from weapon_materials import bake
    scene.render.engine='CYCLES';scene.cycles.samples=8;bake(scene,[o for o in scene.objects if o.type=='MESH'],out,R/'build/candidates/weapon-refinement/finish-sources/ossuary-stencil.png')
scene.frame_set(1);bpy.context.view_layer.update();objects=[o for o in scene.objects if o.type=='MESH'];bpy.ops.object.select_all(action='SELECT')
for o in scene.objects:
    if o.animation_data:
        for tr in o.animation_data.nla_tracks:tr.mute=False
path=out/(ident+'.glb');bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='NLA_TRACKS',export_extras=True)
for o in scene.objects:
    if o.animation_data:
        for tr in o.animation_data.nla_tracks:tr.mute=True
scene.frame_set(1);bpy.context.view_layer.update();bpy.data.libraries.write(str(out/(ident+'.blend')),{scene},compress=True)
pts=[o.matrix_world@v.co for o in objects for v in o.data.vertices];lo=[min(v[i] for v in pts)-.002 for i in range(3)];hi=[max(v[i] for v in pts)+.002 for i in range(3)]
c={'placement_kind':'snap-component','component_function':'handheld revolver','local_bounds_z_up':[lo,hi],'organic':False,'continuous_surfaces':[],'intentional_separate_meshes':[o.name for o in objects],'isolated_atlas_cells':texture,'reload_method':'hinged crane and six-chamber cylinder rotate about longitudinal fixed hinge','reload_objects':['swing_out_cylinder','cylinder_crane']}
record={'id':ident,'path':path.relative_to(R).as_posix(),'category':'weapon-rough-candidate'};refs=json.loads((R/'data/structure-quality-policy.json').read_text())['reference_sources']
for name,data in [('contract.json',c),('contracts.json',{ident:c}),('inspection-input.json',refs+[record]),('design-record.json',{'id':ident,'source':source.relative_to(R).as_posix(),'asset_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'status':'UNREVIEWED','geometry_frozen_for_texturing':texture})]:(out/name).write_text(json.dumps(data,indent=2)+'\n')
print('OSSUARY STAGED',ident)
