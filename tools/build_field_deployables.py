"""Deterministic original field equipment. Blender 5; outputs candidates only.
Run: blender -b --python tools/build_field_deployables.py
Use -- --render to reimport exported GLBs and render evidence.
"""
import bpy, math, json, random, struct, hashlib, sys
from pathlib import Path
from mathutils import Vector, Matrix
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/candidates/field-deployables'
PREVIEW=ROOT/'previews/field-deployables'
NAMES=[['Bastion Panel','Razorframe','Sentry Mk.I','Field Medic','Munition Feeder','Vapor Engine','Aegis Projector'],['Wall of Contrition','Warding Cross','Litany Engine','Shrine of Mercy','Arsenal Reliquary','Censer of Dread','Sanctuary Ward'],['Ossuary Rampart','Grasping Thicket','Spore Spitter','Regeneration Heart','Brood Magazine','Blight Spore','Living Mantle']]
ROLES=['wall','barricade','turret','health','ammo','miasma','shield']
FACTIONS=['unraveled','penitent','begotten']
PALETTES=[[(.16,.18,.20),(.37,.40,.40),(.42,.22,.12),(.45,.35,.18),(.12,.65,.69)],[(.20,.18,.17),(.24,.28,.25),(.70,.63,.45),(.36,.08,.07),(.92,.52,.14)],[(.12,.05,.16),(.18,.12,.21),(.69,.66,.51),(.40,.06,.12),(.35,.65,.16)]]
PARTS=[];MATS=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def activate(o):
    bpy.ops.object.select_all(action='DESELECT');o.select_set(True);bpy.context.view_layer.objects.active=o

def joint(name,loc=(0,0,0),parent=None):
    o=bpy.data.objects.new(name,None);bpy.context.collection.objects.link(o);o.parent=parent;o.matrix_world=Matrix.Translation(loc);bpy.context.view_layer.update();return o

def finish(o,name,mat,parent=None):
    o.name=name;o.data.materials.append(MATS[mat]);activate(o)
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    if parent:
        world=o.matrix_world.copy();o.parent=parent;o.matrix_world=world
    PARTS.append(o);return o

def box(name,loc,size,mat=0,parent=None,bevel=.015):
    bpy.ops.mesh.primitive_cube_add(size=1,location=loc);o=bpy.context.object;o.scale=size
    activate(o);bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    if bevel:
        m=o.modifiers.new('pressed_edges','BEVEL');m.width=min(bevel,min(size)*.16);m.segments=1;bpy.ops.object.modifier_apply(modifier=m.name)
    return finish(o,name,mat,parent)

def orb(name,loc,size,mat=0,parent=None):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=12,ring_count=6,location=loc);o=bpy.context.object;o.scale=size
    for p in o.data.polygons:p.use_smooth=True
    return finish(o,name,mat,parent)

def tube(name,points,radii,mat=1,parent=None,sides=8):
    verts=[];faces=[]
    for i,p in enumerate(points):
        tangent=Vector(points[min(i+1,len(points)-1)])-Vector(points[max(0,i-1)])
        tangent.normalize();u=tangent.cross(Vector((0,0,1)))
        if u.length<.01:u=tangent.cross(Vector((0,1,0)))
        u.normalize();v=tangent.cross(u).normalized()
        for k in range(sides):verts.append(Vector(p)+radii[i]*(math.cos(k*math.tau/sides)*u+math.sin(k*math.tau/sides)*v))
    for i in range(len(points)-1):
        for k in range(sides):a=i*sides+k;b=i*sides+(k+1)%sides;faces.append((a,b,b+sides,a+sides))
    faces.extend([tuple(reversed(range(sides))),tuple((len(points)-1)*sides+k for k in range(sides))])
    mesh=bpy.data.meshes.new(name);mesh.from_pydata(verts,[],faces);mesh.update();o=bpy.data.objects.new(name,mesh);bpy.context.collection.objects.link(o)
    return finish(o,name,mat,parent)

def strut(name,a,b,r=.035,mat=1,parent=None):return tube(name,[a,b],[r,r],mat,parent)
def ring(name,center,r,mat=1,parent=None):
    pts=[(center[0]+r*math.cos(i*math.tau/16),center[1]+r*math.sin(i*math.tau/16),center[2]) for i in range(17)]
    return tube(name,pts,[.025]*17,mat,parent,6)

def materials(f):
    global MATS
    MATS=[];rng=random.Random(794+f)
    # Original painted pixel source sheets: substrate grain, seams, pits, scars and veins.
    for n,color in enumerate(PALETTES[f]):
        image=bpy.data.images.new('painted_%s_%s'%(f,n),width=128,height=128)
        pixels=[]
        for y in range(128):
            for x in range(128):
                grain=rng.uniform(.83,1.12)
                if f==0:
                    if (y%31<2 or x%47==0):grain*=.55
                    if (x*13+y*7)%101<3:grain*=.45
                    if y%19==3 and x%24<15:grain*=1.2
                elif f==1:
                    if (x+y//32*11)%43<2 or y%32<2:grain*=.55
                    if (x*7+y*11)%83<3:grain*=.48
                else:
                    if abs((x+int(7*math.sin(y*.13)))%27-13)<2:grain*=.55
                    grain*=.92+.12*math.sin(x*.13)*math.cos(y*.11)
                pixels.extend([min(1,c*grain) for c in color]+[1])
        image.pixels=pixels;image.pack()
        m=bpy.data.materials.new('surface_%s_%s'%(f,n));m.use_nodes=True;p=m.node_tree.nodes.get('Principled BSDF')
        tex=m.node_tree.nodes.new('ShaderNodeTexImage');tex.image=image;tex.interpolation='Closest';m.node_tree.links.new(tex.outputs['Color'],p.inputs['Base Color'])
        p.inputs['Roughness'].default_value=.8 if n!=4 else .4;p.inputs['Metallic'].default_value=.55 if f==0 and n<4 else .12
        if n==4:p.inputs['Emission Color'].default_value=(*color,1);p.inputs['Emission Strength'].default_value=.35
        MATS.append(m)

def supports(f,root):
    for i in range(4):
        a=math.pi/4+i*math.pi/2;c,s=math.cos(a),math.sin(a)
        j=joint('Leg_%d'%i,(c*.16,s*.16,.22),root)
        strut('outrigger_%d'%i,(c*.16,s*.16,.22),(c*.49,s*.49,.08),.045,1,j)
        box('ground_pad_%d'%i,(c*.50,s*.50,.05),(.25,.15,.10),0,j)
        if f==0:strut('hydraulic_%d'%i,(c*.12,s*.12,.32),(c*.39,s*.39,.13),.022,3,j)
        if f==1:
            tube('retaining_iron_%d'%i,[(c*.10,s*.10,.28),(c*.27,s*.27,.30),(c*.50,s*.50,.10)],[.03,.035,.02],1,j)

def mechanical(f,role,root):
    supports(f,root)
    shell=0;trim=1 if f==0 else 2
    box('reinforced_reliquary' if f else 'folding_chassis',(0,0,.22),(.64,.42,.25),0,root)
    if role in (0,1):
        width=2.2 if role==0 else 2.05
        for side in (-1,1):
            j=joint('Panel_%s'%side,(side*.12,0,.28),root)
            if role==0:
                box('segmented_armor_%s'%side,(side*.55,0,.98),(.98,.15,1.48),shell,j)
                for k in range(3):
                    box('raised_seam_%s_%s'%(side,k),(side*.55,-.094,.45+k*.47),(.89,.035,.045),trim,j)
                strut('diagonal_retainer_%s'%side,(side*.16,.12,.30),(side*.98,.12,1.53),.045,trim,j)
                box('indicator_%s'%side,(side*.88,-.10,1.52),(.09,.04,.06),4,j)
                for z in (.40,1.45):
                    strut('hinge_pin_%s_%s'%(side,z),(side*.12,-.04,z),(side*.12,.08,z),.045,3,j)
            else:
                strut('cross_member_%s'%side,(-.95,0,.25 if side<0 else 1.13),(.95,0,1.13 if side<0 else .25),.07,trim,j)
                box('retaining_post_%s'%side,(side*.91,0,.64),(.13,.17,1.18),shell,j)
        if role==1:
            for k in range(7):strut('obstruction_rail_%d'%k,(-.9+k*.3,-.06,.80),(-.9+k*.3,-.12,1.22),.026,1,root)
    else:
        mast=joint('Mast',(0,0,.32),root)
        strut('central_column',(0,0,.26),(0,0,.94),.12,trim,mast)
        if role==2:
            yaw=joint('AimYaw',(0,0,.91),mast);pitch=joint('AimPitch',(0,0,.91),yaw)
            box('receiver',(0,.02,.98),(.54,.46,.28),0,pitch)
            for side in (-1,1):
                recoil=joint('Recoil_%s'%side,(side*.16,-.2,.98),pitch)
                strut('twin_barrel_%s'%side,(side*.16,-.15,.98),(side*.16,-.84,.98),.055,1,recoil)
                ringo=ring('breech_collar_%s'%side,(side*.16,0,.98),.11,3,recoil)
            orb('sensor_eye',(0,-.25,1.16),(.07,.04,.07),4,pitch)
            if f==1:
                for k in range(5):
                    a=k*math.tau/5;tube('votive_crown_%s'%k,[(math.cos(a)*.20,math.sin(a)*.20,.86),(math.cos(a)*.36,math.sin(a)*.36,1.15),(math.cos(a)*.24,math.sin(a)*.24,1.44)],[.025,.034,.01],2,yaw)
        elif role==3:
            box('treatment_cabinet',(0,0,.78),(.65,.36,.70),0,mast)
            for side in (-1,1):
                tray=joint('Tray_%s'%side,(side*.30,0,.77),mast)
                box('treatment_tray_%s'%side,(side*.47,-.05,.77),(.34,.40,.06),trim,tray)
                for k in range(3):strut('reservoir_%s_%s'%(side,k),(side*.20,-.02+k*.10,.85),(side*.20,-.02+k*.10,1.15),.035,2,mast)
            box('treatment_cross_v',(0,-.20,.90),(.07,.025,.28),4,mast);box('treatment_cross_h',(0,-.22,.90),(.26,.025,.07),4,mast)
            tube('treatment_hose',[(.27,.12,1.02),(.42,.15,.85),(.43,.14,.54),(.24,.08,.42)],[.025]*4,1,mast)
        elif role==4:
            box('munition_hopper',(0,.05,.80),(.70,.44,.62),0,mast)
            rack=joint('FeedRack',(0,-.12,.70),mast)
            box('dispensing_rail',(0,-.34,.59),(.66,.64,.07),trim,rack)
            for k in range(6):
                strut('cartridge_%s'%k,(-.25+k*.1,-.16,.72),(-.25+k*.1,-.16,1.10),.035,3,rack)
                orb('capsule_tip_%s'%k,(-.25+k*.1,-.16,1.10),(.035,.035,.065),2,rack)
            box('stock_indicator',(0,-.21,1.04),(.28,.025,.05),4,mast)
        elif role==5:
            for side in (-1,1):
                strut('pressure_tank_%s'%side,(side*.21,0,.35),(side*.21,0,1.02),.14,0,mast)
                vent=joint('Vent_%s'%side,(side*.23,0,1),mast)
                tube('exhaust_%s'%side,[(side*.21,0,.87),(side*.37,0,1.04),(side*.37,0,1.45)],[.07,.07,.09],trim,vent)
                ring('valve_%s'%side,(side*.26,-.18,.67),.10,3,mast)
            fan=joint('Rotor',(0,-.24,.62),mast)
            for k in range(4):
                a=k*math.pi/2;strut('fan_blade_%d'%k,(0,-.24,.62),(.17*math.cos(a),-.24,.62+.17*math.sin(a)),.035,1,fan)
        elif role==6:
            strut('emitter_column',(0,0,.74),(0,0,1.15),.13,trim,mast)
            orb('emitter_lantern',(0,0,1.18),(.18,.18,.24),4,mast)
            rotor=joint('Rotor',(0,0,1.10),mast)
            for z in (.92,1.32):ring('projection_band_%s'%z,(0,0,z),.30,3,rotor)
            for k in range(4):
                a=k*math.pi/2;pillar=joint('Pillar_%d'%k,(math.cos(a)*.44,math.sin(a)*.44,.30),root)
                strut('ward_pillar_%d'%k,(math.cos(a)*.44,math.sin(a)*.44,.27),(math.cos(a)*.44,math.sin(a)*.44,1.14),.055,trim,pillar)
                orb('ward_lamp_%d'%k,(math.cos(a)*.44,math.sin(a)*.44,1.17),(.07,.07,.10),4,pillar)
    if f==1:
        for side in (-1,1):
            hanger=joint('Pendant_%s'%side,(side*.27,-.17,.77),root)
            strut('censer_chain_%s'%side,(side*.27,-.17,.77),(side*.27,-.17,.48),.012,1,hanger)
            orb('censer_bowl_%s'%side,(side*.27,-.17,.45),(.09,.08,.08),3,hanger)
        box('faded_ceremonial_cloth',(0,-.235,.47),(.23,.018,.36),3,root,0)

def organic(role,root):
    # All roots, organ lobes and vascular ducts become ONE actual remeshed living surface.
    body=[]
    body.append(orb('core',(0,0,.40),(.39,.31,.37),0))
    for k in range(5):
        a=k*math.tau/5;body.append(tube('root',[(0,0,.30),(.36*math.cos(a),.36*math.sin(a),.17),(.65*math.cos(a),.58*math.sin(a),.055)],[.13,.09,.045],0))
    if role==0:
        for k in range(7):
            x=-.90+k*.30;body.append(tube('rampart',[(x*.75,0,.22),(x,0,.76),(x*.93,.04,1.42)],[.17,.15,.10],0))
        body.append(tube('vertebral_bridge',[(-.84,0,.70),(0,0,.78),(.84,0,.70)],[.16]*3,0))
    elif role==1:
        for k in range(6):
            a=k*math.tau/6;body.append(tube('grasping_root',[(0,0,.25),(.50*math.cos(a),.37*math.sin(a),.38),(.86*math.cos(a),.50*math.sin(a),.71),(.80*math.cos(a),.44*math.sin(a),.93)],[.13,.11,.065,.015],0))
    elif role==2:
        body.extend([tube('muscular_stalk',[(0,0,.30),(0,.13,.70),(0,0,1.02)],[.20,.18,.24],0),orb('gland',(0,.09,1.05),(.34,.30,.32),0)])
    elif role==3:
        body.extend([orb('left_ventricle',(-.15,0,.75),(.30,.28,.42),0),orb('right_ventricle',(.16,0,.74),(.28,.27,.35),0)])
        for side in (-1,1):body.append(tube('vascular_arch',[(side*.20,0,.60),(side*.42,.02,.84),(side*.22,.08,1.13),(0,.1,.91)],[.06,.065,.055,.06],0))
    elif role==4:
        for k in range(5):
            a=k*math.tau/5;body.append(orb('brood_sac',(.30*math.cos(a),.25*math.sin(a),.66),(.21,.20,.36),0))
    elif role==5:
        body.append(orb('spore_organ',(0,0,.79),(.44,.37,.52),0))
        for k in range(4):a=k*math.pi/2;body.append(orb('secondary_pustule',(.30*math.cos(a),.29*math.sin(a),1.00),(.19,.17,.23),0))
    else:
        body.append(orb('mantle_organ',(0,0,.77),(.28,.24,.41),0))
        for k in range(6):
            a=k*math.tau/6;body.append(tube('mantle_tendon',[(0,0,.58),(.39*math.cos(a),.39*math.sin(a),.74),(.61*math.cos(a),.56*math.sin(a),1.03)],[.11,.075,.035],0))
    bpy.ops.object.select_all(action='DESELECT')
    for o in body:o.select_set(True)
    bpy.context.view_layer.objects.active=body[0];bpy.ops.object.join();o=body[0];o.name='LivingSurface'
    mod=o.modifiers.new('Actual_connected_surface','REMESH');mod.mode='VOXEL';mod.voxel_size=.055;mod.use_smooth_shade=True;bpy.ops.object.modifier_apply(modifier=mod.name)
    mod=o.modifiers.new('refined_transitions','SMOOTH');mod.factor=1;mod.iterations=3;bpy.ops.object.modifier_apply(modifier=mod.name)
    mod=o.modifiers.new('runtime_surface_budget','DECIMATE');mod.ratio=min(1,1500/max(1,len(o.data.polygons)*2));bpy.ops.object.modifier_apply(modifier=mod.name)
    world=o.matrix_world.copy();o.parent=root;o.matrix_world=world
    # Connected body rooted above Z=0; deliberately separate articulated bone/chitin jaws.
    if role in (0,1):
        for k in range(6):
            x=-.83+k*.33;j=joint('Rib_%d'%k,(x,0,.45),root)
            tube('bone_rib_%d'%k,[(x,-.10,.42),(x,-.23,.86),(x*.96,-.12,1.40 if role==0 else .90)],[.055,.065,.015],2,j)
    elif role==2:
        yaw=joint('AimYaw',(0,0,.96),root);pitch=joint('AimPitch',(0,0,.96),yaw)
        orb('tracking_eye',(0,-.26,1.10),(.10,.06,.10),4,pitch)
        for side in (-1,1):
            j=joint('Jaw_%s'%side,(side*.15,-.15,.99),pitch)
            tube('articulated_mandible_%s'%side,[(side*.20,-.20,.98),(side*.31,-.43,.94),(side*.13,-.62,1.02)],[.07,.06,.015],2,j)
    else:
        for k in range(4):
            a=k*math.pi/2;j=joint('Valve_%d'%k,(math.cos(a)*.18,math.sin(a)*.18,.80),root)
            tube('protective_carapace_%d'%k,[(math.cos(a)*.29,math.sin(a)*.29,.45),(math.cos(a)*.43,math.sin(a)*.43,.82),(math.cos(a)*.25,math.sin(a)*.25,1.16)],[.075,.09,.025],1,j)
        orb('bioluminescent_duct',(0,-.27,.72),(.09,.025,.18),4,root)

def unwrap_bake(identifier):
    meshes=[o for o in bpy.context.scene.objects if o.type=='MESH']
    bpy.ops.object.select_all(action='DESELECT')
    for o in meshes:o.select_set(True)
    bpy.context.view_layer.objects.active=meshes[0];bpy.ops.object.mode_set(mode='EDIT');bpy.ops.mesh.select_all(action='SELECT');bpy.ops.uv.smart_project(island_margin=.02);bpy.ops.object.mode_set(mode='OBJECT')
    # Dedicated low-resolution baked atlas per model; all material classes remain distinct.
    atlas=bpy.data.images.new(identifier+'_baked',width=256,height=256)
    for m in MATS:
        node=m.node_tree.nodes.new('ShaderNodeTexImage');node.image=atlas;m.node_tree.nodes.active=node;node.select=True
    scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=1
    scene.render.bake.use_pass_direct=False;scene.render.bake.use_pass_indirect=False;scene.render.bake.use_pass_color=True;scene.render.bake.margin=2;scene.render.bake.use_clear=False
    bpy.ops.object.bake(type='DIFFUSE');atlas.pack()
    for m in MATS:
        p=m.node_tree.nodes.get('Principled BSDF');tex=m.node_tree.nodes.new('ShaderNodeTexImage');tex.image=atlas;tex.interpolation='Closest';m.node_tree.links.new(tex.outputs['Color'],p.inputs['Base Color'])
    return meshes

def append_clips(path,role,f):
    raw=path.read_bytes();n=struct.unpack_from('<I',raw,12)[0];doc=json.loads(raw[20:20+n]);offset=20+n;size=struct.unpack_from('<I',raw,offset)[0];data=bytearray(raw[offset+8:offset+8+size]);doc['animations']=[]
    def acc(values,width):
        while len(data)%4:data.append(0)
        start=len(data)
        for row in values:data.extend(struct.pack('<'+'f'*width,*row))
        view=len(doc['bufferViews']);doc['bufferViews'].append({'buffer':0,'byteOffset':start,'byteLength':len(data)-start})
        record={'bufferView':view,'componentType':5126,'count':len(values),'type':{1:'SCALAR',3:'VEC3',4:'VEC4'}[width]}
        if width==1:record.update(min=[min(v[0] for v in values)],max=[max(v[0] for v in values)])
        ident=len(doc['accessors']);doc['accessors'].append(record);return ident
    durations={'ACTIVE':2,'PACKED':1,'THROWN':.7,'IMPACT':.15,'DEPLOYING':1.5,'OPERATING':.65,'DAMAGED':.45,'EXPIRING':1,'DESTROYED':.8}
    for clip,duration in durations.items():
        animation={'name':clip,'samplers':[],'channels':[]};times=acc([[0],[duration*.5],[duration]],1)
        def track(i,path,values):
            sampler=len(animation['samplers']);animation['samplers'].append({'input':times,'output':acc(values,len(values[0])),'interpolation':'LINEAR'});animation['channels'].append({'sampler':sampler,'target':{'node':i,'path':path}})
        for i,node in enumerate(doc['nodes']):
            name=node.get('name','');base=node.get('translation',[0,0,0]);rot=node.get('rotation',[0,0,0,1]);scale=node.get('scale',[1,1,1])
            isroot=name=='DeployRoot';isjoint=not ('mesh' in node) and not isroot
            def angle(axis,a):
                q=[0,0,0,math.cos(a/2)];q[axis]=math.sin(a/2);return q
            if isroot and clip in ('PACKED','THROWN','IMPACT','DEPLOYING'):
                packed=[.19,.19,.19]
                values=[packed]*3 if clip in ('PACKED','THROWN','IMPACT') else [packed,[.62,.76,.62],[1,1,1]]
                track(i,'scale',values)
                if clip=='THROWN':track(i,'rotation',[angle(0,0),angle(0,2.0+role*.12),angle(0,4.4+f*.3)])
                if clip=='IMPACT':track(i,'translation',[[0,.06,0],[0,.025,0],[0,0,0]])
            if isjoint:
                axis=2 if any(t in name for t in ('Panel','Leg','Rib','Jaw','Valve','Tray')) else 1
                sign=-1 if base[0]<0 else 1
                fold=(1.25 if 'Panel' in name else .9)*sign
                if clip in ('PACKED','THROWN','IMPACT'):track(i,'rotation',[angle(axis,fold)]*3)
                elif clip=='DEPLOYING':track(i,'rotation',[angle(axis,fold),angle(axis,fold*.42),rot])
                elif clip=='ACTIVE':
                    amplitude=.09 if f==2 else .06 if 'Pendant' in name else .018
                    if 'Rotor' in name:track(i,'rotation',[angle(1,0),angle(1,math.pi),angle(1,math.tau)])
                    else:track(i,'rotation',[rot,angle(axis,amplitude*sign),rot])
                elif clip=='OPERATING':
                    amplitude=.40 if any(t in name for t in ('Recoil','Jaw','Valve','FeedRack','Tray','Vent')) else .13
                    track(i,'rotation',[rot,angle(axis,amplitude*sign),rot])
                    if 'Recoil' in name:track(i,'translation',[base,[base[0],base[1],base[2]+.11],base])
                elif clip=='DAMAGED':track(i,'rotation',[rot,angle(axis,.17*sign),angle(axis,.045*sign)])
                elif clip=='EXPIRING':track(i,'rotation',[angle(axis,.07*sign),angle(axis,-.09*sign),angle(axis,.07*sign)])
                elif clip=='DESTROYED':track(i,'rotation',[rot,angle(axis,.7*sign),angle(axis,1.4*sign)])
        doc['animations'].append(animation)
    doc['buffers'][0]['byteLength']=len(data);jsonbytes=json.dumps(doc,separators=(',',':')).encode();jsonbytes+=b' '*((-len(jsonbytes))%4);data+=b'\0'*((-len(data))%4)
    path.write_bytes(struct.pack('<III',0x46546c67,2,28+len(jsonbytes)+len(data))+struct.pack('<II',len(jsonbytes),0x4e4f534a)+jsonbytes+struct.pack('<II',len(data),0x004e4942)+data)

def build():
    manifest=[]
    for f in range(3):
        for role in range(7):
            identifier='thrown-'+FACTIONS[f]+'-'+ROLES[role];dest=OUT/identifier;dest.mkdir(parents=True,exist_ok=True)
            bpy.ops.wm.read_factory_settings(use_empty=True);PARTS.clear();materials(f);root=joint('DeployRoot')
            if f==2:organic(role,root)
            else:mechanical(f,role,root)
            meshes=unwrap_bake(identifier)
            # Explicit envelope check; never rescale or conceal below-ground vertices.
            pts=[o.matrix_world@v.co for o in meshes for v in o.data.vertices]
            if min(v.z for v in pts)<-.002:raise ValueError(identifier+' below ground')
            dims=[max(v[i] for v in pts)-min(v[i] for v in pts) for i in range(3)]
            bpy.ops.wm.save_as_mainfile(filepath=str(dest/(identifier+'.blend')))
            glb=dest/(identifier+'.glb');bpy.ops.export_scene.gltf(filepath=str(glb),export_format='GLB',export_animations=False,export_yup=True,export_extras=True)
            append_clips(glb,role,f)
            triangles=sum(len(p.vertices)-2 for o in meshes for p in o.data.polygons)
            record={'id':identifier,'name':NAMES[f][role],'faction':FACTIONS[f],'role':ROLES[role],'path':str(glb),'category':'candidate','triangles':triangles,'dimensions_m':dims,'sha256':sha(glb),'status':'UNREVIEWED'}
            manifest.append(record);(OUT/'manifest.json').write_text(json.dumps(manifest,indent=2))
            print('FIELD_CANDIDATE',identifier,triangles,flush=True)

def render():
    for record in json.loads((OUT/'manifest.json').read_text()):
        bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=record['path'],import_pack_images=True)
        scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=8;scene.render.resolution_x=448;scene.render.resolution_y=448;scene.render.resolution_percentage=100
        scene.world=bpy.data.worlds.new('neutral_studio');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[0].default_value=(.13,.15,.18,1);scene.world.node_tree.nodes['Background'].inputs[1].default_value=.6
        # Imported actions are left at rest for deployed geometry, with tracks muted explicitly.
        for o in scene.objects:
            if o.animation_data:o.animation_data_clear()
        target=Vector((0,0,.78))
        for loc,power,size in [((3,-4,6),650,4),((-3,-1,3),400,3),((0,4,4),550,3)]:
            bpy.ops.object.light_add(type='AREA',location=loc);o=bpy.context.object;o.data.energy=power;o.data.shape='DISK';o.data.size=size;o.rotation_euler=(target-o.location).to_track_quat('-Z','Y').to_euler()
        bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;cam.data.type='ORTHO';cam.data.ortho_scale=2.9
        dest=PREVIEW/record['id'];dest.mkdir(parents=True,exist_ok=True);evidence={'asset_sha256':sha(Path(record['path'])),'renders':{}}
        clay=bpy.data.materials.new('neutral_clay');clay.diffuse_color=(.55,.55,.55,1)
        for view,loc in [('front',(2.7,-4.5,2.5)),('rear',(-2.7,4.5,2.5)),('closeup',(1.6,-3.3,2.1)),('clay',(2.7,-4.5,2.5))]:
            cam.location=loc;cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.ortho_scale=2.20 if view=='closeup' else 2.9
            scene.view_layers[0].material_override=clay if view=='clay' else None
            file=dest/(view+'.png');scene.render.filepath=str(file);bpy.ops.render.render(write_still=True)
            evidence['renders'][view]={'path':str(file),'sha256':sha(file)}
        (OUT/record['id']/'render-evidence.json').write_text(json.dumps(evidence,indent=2))
        print('FIELD_RENDERED',record['id'],flush=True)
if __name__=='__main__':
    if '--render' in sys.argv:render()
    else:build()
