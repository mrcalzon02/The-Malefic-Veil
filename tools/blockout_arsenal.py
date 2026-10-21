"""32 original primitive blockouts. No purchased source geometry, no image generation.
Each named solid is a replacement role; articulation roots and sockets are stable.
"""
import bpy,math,json,tempfile,sys,hashlib
from pathlib import Path
from mathutils import Vector,Matrix
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'assets/game/arsenal-blockouts';OUT.mkdir(parents=True,exist_ok=True)
PRE=ROOT/'previews/blockouts';PRE.mkdir(parents=True,exist_ok=True)
TMP=ROOT/'build/blender-temp';TMP.mkdir(parents=True,exist_ok=True);tempfile.tempdir=str(TMP)
weapons=json.loads((ROOT/'data/weapons.json').read_text())['weapons']
bpy.ops.wm.read_factory_settings(use_empty=True)
scene=bpy.context.scene;scene.render.fps=30;scene.world=bpy.data.worlds.new('Blockout studio');scene.world.use_nodes=True
scene.world.node_tree.nodes['Background'].inputs[0].default_value=(.05,.065,.085,1);scene.world.node_tree.nodes['Background'].inputs[1].default_value=.45
scene.render.engine='CYCLES';scene.cycles.samples=12;scene.render.resolution_x=800;scene.render.resolution_y=500;scene.render.resolution_percentage=100
scene.view_settings.view_transform='AgX'
def material(name,c):
    m=bpy.data.materials.new(name);m.use_nodes=True;m.diffuse_color=(*c,1);b=m.node_tree.nodes['Principled BSDF'];b.inputs['Base Color'].default_value=(*c,1);b.inputs['Roughness'].default_value=.65;return m
bodymat=material('01 Receiver volume',(.22,.27,.31))
gripmat=material('02 Hand contact volume',(.055,.075,.088))
movingmat=material('03 Moving mechanism volume',(.47,.52,.54))
ammomat=material('04 Replaceable ammo volume',(.48,.30,.12))
voidmat=material('05 Bore / opening',(.007,.012,.016))
materials=[bodymat,gripmat,movingmat,ammomat,voidmat]
for loc,power,size in [((1,-3,4),450,5),((-1,3,3),550,4),((4,1,2),150,3)]:
    bpy.ops.object.light_add(type='AREA',location=loc);o=bpy.context.object;o.data.energy=power;o.data.size=size;o.rotation_euler=(-o.location).to_track_quat('-Z','Y').to_euler()
bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';scene.camera=cam
reports=[];layout=[];allcollections=[]
for wi,w in enumerate(weapons):
    col=bpy.data.collections.new(w['id']);scene.collection.children.link(col);allcollections.append(col)
    objects=[];roles=[]
    root=bpy.data.objects.new(w['id']+'__ROOT',None);col.objects.link(root);objects.append(root)
    root['weapon_id']=w['id'];root['status']='PRIMITIVE BLOCKOUT — no final art or combat behavior'
    shape=w['silhouette'];L=w['length_m'];accent=material(w['id']+' family cue',tuple(int(w['accent'][j:j+2],16)/255*.75 for j in(0,2,4)))
    def group(name,loc=(0,0,0),parent=root):
        o=bpy.data.objects.new(w['id']+'__'+name,None);col.objects.link(o);o.parent=parent;o.location=loc;objects.append(o);return o
    receiver_group=group('receiver_group');action=group(w['mechanism']+'_PIVOT');ammo=group('magazine_PIVOT')
    def finish(o,role,mat,parent):
        for c in list(o.users_collection):c.objects.unlink(o)
        col.objects.link(o);o.name=w['id']+'__'+role;o.parent=parent;objects.append(o);o.data.materials.append(mat);o['replacement_role']=role;o['stage']='blockout'
        roles.append({'role':role,'shape':'primitive','parent':parent.name.split('__')[-1]});return o
    def box(role,loc,dims,mat=bodymat,parent=receiver_group,rot=(0,0,0)):
        bpy.ops.mesh.primitive_cube_add(size=1,location=loc);o=bpy.context.object;o.scale=dims;o.rotation_euler=rot
        bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
        return finish(o,role,mat,parent)
    def profile(role,points,depth,mat=bodymat,parent=receiver_group):
        n=len(points);verts=[(x,y*depth/2,z) for y in(-1,1) for x,z in points]
        faces=[tuple(range(n-1,-1,-1)),tuple(range(n,2*n))]+[(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)]
        me=bpy.data.meshes.new(role);me.from_pydata(verts,[],faces);me.update();o=bpy.data.objects.new(role,me);scene.collection.objects.link(o);return finish(o,role,mat,parent)
    def cyl(role,loc,radius,length,mat=movingmat,parent=action,axis='X',sides=12):
        rotation=(0,math.pi/2,0) if axis=='X' else (math.pi/2,0,0) if axis=='Y' else (0,0,0)
        bpy.ops.mesh.primitive_cylinder_add(vertices=sides,radius=radius,depth=length,location=loc,rotation=rotation);o=bpy.context.object
        bpy.ops.object.transform_apply(location=False,rotation=False,scale=True);return finish(o,role,mat,parent)
    def ring(role,x,rad,depth=.025,parent=action,mat=movingmat):
        bpy.ops.mesh.primitive_torus_add(major_segments=16,minor_segments=4,location=(x,0,.035),rotation=(0,math.pi/2,0),major_radius=rad,minor_radius=depth/2)
        o=bpy.context.object;return finish(o,role,mat,parent)
    muzzle_positions=[]
    def barrel(x1,x2,y=0,z=.035,r=.026,prefix='barrel',parent=action):
        cyl(prefix,((x1+x2)/2,y,z),r,x2-x1,parent=parent)
        cyl(prefix+'_bore',(x2+.001,y,z),r*.72,.004,voidmat,parent)
        bpy.context.view_layer.update()
        muzzle_positions.append(list(parent.matrix_world@Vector((x2+.008,y,z))))
    def grip(x,z=-.12,scale=1):
        group('hand_R_SOCKET',(x-.01,0,(z-.065*scale)*.55))
        return profile('grip',[(x-.027*scale,-.01),(x+.037*scale,-.01),(x+.008*scale,z-.065*scale),(x-.054*scale,z-.065*scale)],.052*scale,gripmat)
    def guard(x):
        # Deliberately thin, with usable hand clearance.
        box('guard_bottom',(x+.052,0,-.071),(.10,.012,.009),gripmat)
        box('guard_front',(x+.103,0,-.041),(.009,.012,.064),gripmat,rot=(0,.13,0))
        box('trigger',(x+.037,0,-.034),(.008,.011,.032),ammomat,rot=(0,-.15,0))
    def stock(x,width=.23):
        end=max(x+width,-.18)
        profile('stock',[(x,.035),(end,.035),(end,-.013),(x+.055,-.081),(x,-.09)],.065,gripmat)
    def base(length=.40,height=.13,width=.10):
        profile('receiver',[(-length/2,-.015),(-length/2,height*.70),(-length*.36,height),(.10,height),(.15,height*.60),(.15,-.015),(-.03,-.035)],width)
    if shape in('pistol','revolver'):
        base(.25,.085,.065);grip(-.077,-.10,.85);guard(-.058)
        if shape=='pistol':
            box('slide',(-.010,0,.065),(.24,.072,.042),movingmat,action)
            barrel(.085,.24,r=.022)
        else:
            cyl('revolver_cylinder',(-.005,0,.036),.052,.11,ammomat)
            barrel(.050,.31,r=.027);box('hammer',(-.135,0,.08),(.025,.033,.036),movingmat,action)
        box('magazine_toe',(-.09,0,-.164),(.075,.060,.012),ammomat,ammo)
    elif shape in('smg','rifle','shotgun','double','triple'):
        base(.36 if shape=='smg' else .42,.13,.11);grip(-.12);guard(-.095);stock(-L*.46,.22)
        if shape=='smg':
            barrel(.10,.30,r=.027);box('magazine',(.075,0,-.16),(.070,.06,.25),ammomat,ammo,rot=(0,-.12,0));box('short_foregrip',(.16,0,-.075),(.055,.06,.14),gripmat)
        elif shape=='rifle':
            barrel(.12,L*.49,r=.027);box('forearm',(.22,0,.01),(.25,.09,.10));box('magazine',(.015,0,-.14),(.09,.07,.22),ammomat,ammo,rot=(0,.12,0))
        else:
            end=L*.50
            if shape=='double':
                barrel(.10,end,-.043,.04,.035,'left_barrel');barrel(.10,end,.043,.04,.035,'right_barrel')
            elif shape=='triple':
                barrel(.10,end,-.047,.025,.035,'left_barrel');barrel(.10,end,.047,.025,.035,'right_barrel');barrel(.10,end,0,.110,.035,'top_barrel')
            else:
                barrel(.10,end,r=.034);barrel(.10,end-.06,z=-.045,r=.018,prefix='shell_tube')
                box('pump',(.23,0,-.026),(.17,.11,.085),movingmat,action)
            if shape!='shotgun':box('breech',(.11,0,.01),(.085,.20 if shape=='triple' else .18,.16),ammomat,action)
    elif shape in('rotary','rotary_heavy','arc_rotary'):
        base(.43,.20,.24);grip(-.16,-.16,1.15);guard(-.13);stock(-L*.45,.24)
        banks=2 if shape=='rotary_heavy' else 1;count=4 if shape=='arc_rotary' else 6
        for bank in range(banks):
            center=(bank-(banks-1)/2)*.28
            rotor=group('rotor_bank_'+str(bank),(0,center,.04),action)
            for j in range(count):
                a=j*math.tau/count;barrel(.10,L*.49,math.cos(a)*.085,math.sin(a)*.085,.024,'tube_'+str(bank)+'_'+str(j),rotor)
            cyl('rotor_rear_'+str(bank),(.12,center,.04),.125,.05,bodymat,receiver_group)
            cyl('rotor_front_'+str(bank),(L*.41,center,.04),.125,.04,bodymat,receiver_group)
        cyl('feed_drum',(-.16,-.24,-.035),.15,.16,ammomat,ammo,'Y')
        box('feed_bridge',(-.16,-.15,-.02),(.10,.18,.10),bodymat)
        box('carry_handle_top',(-.07,0,.31),(.28,.04,.035),gripmat)
        for x in(-.20,.06):box('carry_handle_mount_'+str(x),(x,0,.25),(.035,.04,.10),gripmat)
    elif shape in('laser','rail','plasma','beam_heavy'):
        base(.42,.145,.12 if shape!='beam_heavy' else .22);grip(-.13);guard(-.105);stock(-L*.46,.25)
        box('power_cell',(-.01,0,-.14),(.11,.095,.22),ammomat,ammo)
        if shape=='rail':
            barrel(.11,L*.49,r=.019)
            for z in(-.030,.11):box('accelerator_rail_'+str(z),(L*.28,0,z),(L*.51,.065,.026),movingmat,action)
            cyl('scope',(-.06,0,.23),.031,.25,gripmat,receiver_group)
            for x in(-.14,.025):box('scope_mount_'+str(x),(x,0,.184),(.025,.032,.085),gripmat)
        elif shape=='laser':
            barrel(.10,L*.48,r=.032)
            for y in(-.052,.052):box('laser_heat_rail_'+str(y),(L*.27,y,.035),(L*.43,.022,.065),movingmat,action)
            cyl('focusing_lens',(L*.49,0,.035),.030,.012,accent,action)
        elif shape=='plasma':
            barrel(.10,L*.42,r=.078)
            for y in(-.09,.09):cyl('side_capacitor_'+str(y),(.04,y,.08),.045,.24,ammomat,ammo)
            ring('field_collar',L*.41,.09,.035);cyl('plasma_aperture',(L*.43,0,.035),.052,.01,accent)
        else:
            barrel(.08,L*.46,r=.115)
            for j in range(6):ring('heat_fin_'+str(j),.18+j*.048,.135,.025)
            box('rear_power_pack',(-.23,0,.11),(.17,.29,.25),ammomat,ammo)
    elif shape in('tesla','fork','prism'):
        base(.38,.13,.14);grip(-.12);guard(-.095);stock(-L*.43,.20);box('power_cell',(-.01,0,-.13),(.10,.09,.20),ammomat,ammo)
        count=3 if shape=='prism' else 2
        for j in range(count):
            if count==2:y=(-1 if j==0 else 1)*.11;z=.055
            else:a=j*math.tau/count;y=math.cos(a)*.115;z=.035+math.sin(a)*.115
            if shape=='fork':z=.15 if j==0 else -.06
            p=group('electrode_'+str(j),(0,y,z),action)
            cyl('coil_'+str(j),(.15,0,0),.048,.18,ammomat,p)
            box('prong_'+str(j),(.32,0,0),(.30,.028,.035),movingmat,p)
            cyl('electrode_tip_'+str(j),(.475,0,0),.026,.026,accent,p)
        cyl('central_emitter',(.17,0,.035),.065,.10,accent)
        if shape=='fork':box('fork_crosshead',(.13,0,.05),(.08,.27,.27),bodymat)
        muzzle_positions.append((.49,0,.035))
    elif shape in('orb','bounce','gravity','singularity'):
        heavy=shape=='singularity';base(.43,.16,.19);grip(-.14);guard(-.115);stock(-L*.46,.25)
        count=4 if heavy else 3 if shape=='gravity' else 1 if shape=='bounce' else 2
        r=.17 if heavy else .13 if shape=='gravity' else .10
        for j in range(count):
            ring('field_ring_'+str(j),.16+j*.095,r+(j%2)*.018,.024)
        box('field_spine',(.28,0,.025-r),(.47,.055,.030),bodymat)
        box('field_spine_mount',(.11,0,-r*.45),(.08,.055,r+.03),bodymat)
        bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=1,radius=r*.52,location=(.27,0,.035));finish(bpy.context.object,'suspended_core',accent,action)
        box('reactor_pack',(-.19,0,.18),(.16,.17,.15),ammomat,ammo)
        muzzle_positions.append((.20+(count-1)*.095,0,.035))
    elif shape in('rocket','missile','rocket_heavy'):
        # Shoulder tube geometry, rather than a rifle receiver under a fat barrel.
        tube_end=L*.48;grip(-.05,-.18,1.15);guard(-.025)
        box('shoulder_rest',(-.29,0,-.16),(.32,.16,.08),gripmat)
        box('shoulder_rest_mount',(-.25,0,-.085),(.14,.13,.09),bodymat)
        if shape=='missile':
            for j in range(4):barrel(-.35,.47,(j%2-.5)*.16,(j//2-.5)*.16+.08,.066,'launch_tube_'+str(j))
            box('tube_cluster_frame',(-.09,0,.08),(.32,.34,.33),bodymat)
        else:
            r=.095 if shape=='rocket' else .135;barrel(-L*.42,tube_end,z=.10,r=r)
            cyl('rear_exhaust',(-L*.42,0,.10),r*1.17,.06,gripmat,receiver_group)
            cyl('muzzle_collar',(tube_end-.03,0,.10),r*1.12,.05,bodymat,receiver_group)
        box('targeting_unit',(-.12,-.17,.23),(.18,.08,.12),bodymat)
        box('targeting_mount',(-.12,-.11,.16),(.07,.12,.08),gripmat)
        box('sight_glass',(-.12,-.215,.23),(.12,.008,.075),accent)
        box('reload_tray',(-.13,.06,-.025),(.35,.14,.07),ammomat,ammo)
    elif shape in('grenade','flak','scrap'):
        base(.42,.16,.15);grip(-.14);guard(-.115);stock(-L*.46,.25)
        cyl('feed_drum',(.035,0,-.025),.125,.20,ammomat,ammo,'Y')
        barrel(.15,L*.47,z=.07,r=.056 if shape=='grenade' else .085)
        if shape=='flak':
            box('upper_jaw',(.31,0,.18),(.28,.23,.035),movingmat,action)
            box('lower_jaw',(.31,0,-.045),(.28,.23,.035),movingmat,action)
            box('pump',(.15,0,-.10),(.19,.115,.08),gripmat,action)
        elif shape=='scrap':
            for y in(-.115,.115):box('magnetic_scoop_'+str(y),(.35,y,.07),(.32,.05,.22),movingmat,action)
            box('scrap_hopper',(-.06,0,.24),(.20,.18,.17),ammomat,ammo)
    elif shape in('mortar','artillery'):
        box('cradle',(-.12,0,-.055),(.44,.23,.15));grip(-.22,-.19,1.1)
        angle=-.65 if shape=='mortar' else -.20
        action.rotation_euler.y=angle
        barrel(-.10,.56 if shape=='artillery' else .42,z=.08,r=.12)
        cyl('breech',(-.13,0,.08),.15,.12,ammomat,ammo)
        box('brace_crossbar',(-.05,0,-.07),(.11,.45,.06),bodymat)
        for y in(-.20,.20):
            leg=group('brace_'+str(y),(0,y,0))
            box('brace_leg_'+str(y),(-.05,0,-.27),(.055,.06,.40),movingmat,leg,rot=(0,-.25,0))
            box('brace_foot_'+str(y),(-.10,0,-.48),(.20,.15,.045),gripmat,leg)
        if shape=='artillery':
            stock(-.68,.40);box('rangefinder',(-.12,-.18,.22),(.26,.10,.09),accent)
            box('rangefinder_bridge',(-.12,-.08,.025),(.09,.25,.035),gripmat)
            box('rangefinder_post',(-.12,-.18,.11),(.035,.04,.20),gripmat)
        else:
            box('carry_handle',(-.19,0,.24),(.26,.04,.03),gripmat)
            for x in(-.30,-.08):box('carry_handle_mount_'+str(x),(x,0,.13),(.030,.040,.22),gripmat)
    elif shape=='disc':
        base(.37,.13,.12);grip(-.13);guard(-.105);stock(-L*.44,.20)
        cyl('saw_disc',(.25,0,.035),.18,.025,movingmat,action,'Z',24)
        for j in range(12):
            a=j*math.tau/12
            box('saw_tooth_'+str(j),(.25+math.cos(a)*.18,math.sin(a)*.18,.035),(.048,.026,.025),movingmat,action,(0,0,a))
        for z in(-.015,.085):box('disc_guide_'+str(z),(.24,0,z),(.39,.065,.025),bodymat)
        muzzle_positions.append((.46,0,.035))
    elif shape in('bio','cryo'):
        base(.40,.14,.13);grip(-.13);guard(-.105);stock(-L*.46,.22)
        barrel(.12,.43,z=.04,r=.045 if shape=='bio' else .030)
        for y in(-.13,.13):
            if shape=='bio':
                cyl('fluid_tank_'+str(y),(-.20,y,.18),.061,.29,ammomat,ammo,'Z')
                cyl('tank_cap_'+str(y),(-.20,y,.335),.065,.025,accent,ammo,'Z')
            else:
                cyl('fluid_tank_'+str(y),(-.14,y,.21),.055,.32,ammomat,ammo,'X')
                cyl('tank_cap_'+str(y),(.027,y,.21),.059,.025,accent,ammo,'X')
        box('fluid_manifold',(-.16,0,.075 if shape=='bio' else .15),(.22,.33,.08),bodymat)
        for j in range(3):ring('nozzle_fin_'+str(j),.22+j*.055,.075 if shape=='cryo' else .058,.025)
        cyl('fluid_aperture',(.435,0,.04),.027,.012,accent)
    else:raise RuntimeError('Missing silhouette '+shape)
    if not muzzle_positions:raise RuntimeError('No muzzle '+w['id'])
    for j,pos in enumerate(muzzle_positions):group('muzzle_'+str(j)+'_SOCKET',pos,root)
    group('hand_L_SOCKET',(.16,0,-.07));group('ejection_SOCKET',(-.015,.08,.10))
    # Put mechanism pivots at functional joints, preserving all visible world positions.
    pivot_by_shape={'double':(.10,0,.025),'triple':(.10,0,.025),'revolver':(-.005,0,.036),'shotgun':(.22,0,-.026),'disc':(.25,0,.035),'mortar':(-.10,0,.08),'artillery':(-.10,0,.08),'rotary':(.10,0,.04),'rotary_heavy':(.10,0,.04),'arc_rotary':(.10,0,.04)}
    pivot=Vector(pivot_by_shape.get(shape,(.10,0,.035)))
    children=list(action.children)
    action.matrix_basis=action.matrix_basis@Matrix.Translation(pivot)
    for child in children:child.matrix_basis=Matrix.Translation(-pivot)@child.matrix_basis
    # Bake only into export duplicates, keeping the editable hierarchy and pivots intact.
    bpy.context.view_layer.update();points=[o.matrix_world@Vector(v) for o in objects if o.type=='MESH' for v in o.bound_box]
    low=Vector([min(v[i] for v in points) for i in range(3)]);high=Vector([max(v[i] for v in points) for i in range(3)])
    triangle_count=sum(sum(len(p.vertices)-2 for p in o.data.polygons) for o in objects if o.type=='MESH')
    # Conservative disconnected-volume diagnostic. It is not exact triangle collision.
    solids=[o for o in objects if o.type=='MESH' and 'suspended_core' not in o.name]
    boxes={o:([min((o.matrix_world@Vector(v))[i] for v in o.bound_box) for i in range(3)],[max((o.matrix_world@Vector(v))[i] for v in o.bound_box) for i in range(3)]) for o in solids}
    def gap(a,b):
        al,ah=boxes[a];bl,bh=boxes[b]
        return math.sqrt(sum(max(0,al[i]-bh[i],bl[i]-ah[i])**2 for i in range(3)))
    connected={solids[0]};todo=list(solids[1:])
    while True:
        new=[o for o in todo if any(gap(o,a)<=.012 for a in connected)]
        if not new:break
        connected.update(new);todo=[o for o in todo if o not in connected]
    detached=[o.name.split('__')[-1] for o in todo]
    bpy.ops.object.select_all(action='DESELECT')
    for o in objects:o.select_set(True)
    bpy.ops.export_scene.gltf(filepath=str(OUT/f'{w["id"]}.glb'),export_format='GLB',use_selection=True,export_animations=False)
    # Hierarchical GLBs are for DCC inspection. Baked companions suit the current Java static reader.
    baked=[]
    for o in objects:
        if o.type!='MESH':continue
        cp=o.copy();cp.data=o.data.copy();col.objects.link(cp);cp.parent=None;cp.data.transform(o.matrix_world);cp.matrix_world=Matrix.Identity(4);baked.append(cp)
    bpy.ops.object.select_all(action='DESELECT')
    for o in baked:o.select_set(True)
    bpy.ops.export_scene.gltf(filepath=str(OUT/f'{w["id"]}-static.glb'),export_format='GLB',use_selection=True,export_animations=False)
    for o in baked:bpy.data.objects.remove(o,do_unlink=True)
    # Automatic framing from actual extents; no cropped pistol or artillery feet.
    center=(low+high)/2;span=high-low
    cam.location=center+Vector((span.x*.80,-span.x*1.9,max(span.x*.66,span.z*.5)));cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.ortho_scale=max(span.x*1.30,span.z*1.90,span.y*1.7)
    for c in allcollections:c.hide_render=c!=col
    scene.render.filepath=str(PRE/f'{w["id"]}.png')
    if '--skip-render' not in sys.argv:bpy.ops.render.render(write_still=True)
    # Side silhouette proof for multi-barrel and ergonomic comparisons.
    cam.location=center+Vector((0,-3,0));cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler()
    scene.render.filepath=str(PRE/f'{w["id"]}-side.png')
    if '--skip-render' not in sys.argv:bpy.ops.render.render(write_still=True)
    row={'id':w['id'],'name':w['name'],'silhouette':shape,'primitive_triangles':triangle_count,'bounds_m':[list(low),list(high)],'roles':roles,'detached_aabb_groups':detached,'intentional_noncontact':['suspended_core'] if shape in('orb','bounce','gravity','singularity') else [],'status':'primitive blockout; connection and silhouette review','glb_sha256':hashlib.sha256((OUT/f'{w["id"]}.glb').read_bytes()).hexdigest()}
    reports.append(row)
    # Display master at a common scale to retain size relationships.
    root.location=((wi%4)*2.3,(wi//4)*1.8,0)
    layout.append({'id':w['id'],'grid_position':list(root.location)})
    print('BLOCKED',w['id'],triangle_count,flush=True)
for c in allcollections:c.hide_render=False
cam.location=(4,-10,14);cam.rotation_euler=(Vector((3,5,0))-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.ortho_scale=20
for screen in bpy.data.screens:
    for area in screen.areas:
        if area.type=='VIEW_3D':area.spaces.active.clip_end=100;area.spaces.active.overlay.show_floor=False
bpy.data.libraries.write(str(OUT/'arsenal-blockouts.blend'),{scene},compress=True)
(ROOT/'reports/arsenal_blockouts.json').write_text(json.dumps({'schema':1,'stage':'primitive blockouts; not final meshes or implemented weapons','weapons':reports,'master_layout':layout},indent=2)+'\n')
print('ALL 32 BLOCKOUTS COMPLETE',flush=True)
