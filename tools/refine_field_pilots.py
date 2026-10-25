"""Three individually authored field-equipment studies; never overwrites the rejected roster.

Blender 5: --factory-startup -b --python tools/refine_field_pilots.py
Every motion below belongs to a specific component. No root scaling is authored.
"""
import bpy,sys,math,json,struct,hashlib,random
from pathlib import Path
from mathutils import Vector,Matrix,Quaternion
sys.path.insert(0,str(Path(__file__).parent))
import build_field_deployables as b

ROOT=Path(__file__).resolve().parents[1]
REVISION=sys.argv[sys.argv.index('--revision')+1] if '--revision' in sys.argv else 'field-pilots-r9'
OUT=ROOT/'build/candidates'/REVISION
LOOP_SIDES=4
PREVIEW=ROOT/'previews/field-pilots-r4'
MOTION={}

def joint(name,loc,parent=None,pack_move=None,pack_axis=None,pack_angle=0,operation=None,idle=None):
    o=b.joint(name,loc,parent)
    MOTION[name]=dict(move=pack_move,axis=pack_axis,angle=pack_angle,operation=operation,idle=idle)
    return o

def profile(name,outline,width,axis,mat,parent=None):
    # A designed cross-section with actual chamfers and shoulders, not a beveled box.
    n=len(outline);verts=[]
    for side in (-width/2,width/2):
        for a,c in outline:
            verts.append((side,a,c) if axis=='X' else (a,side,c))
    faces=[tuple(reversed(range(n))),tuple(range(n,2*n))]
    faces.extend((i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n))
    mesh=bpy.data.meshes.new(name);mesh.from_pydata(verts,[],faces);mesh.update()
    o=bpy.data.objects.new(name,mesh);bpy.context.collection.objects.link(o)
    return b.finish(o,name,mat,parent)

def lathe(name,points,mat,parent=None,sides=12,loc=(0,0,0),axis='Z'):
    verts=[];faces=[];rings=[]
    for r,z in points:
        ids=[]
        for i in range(1 if abs(r)<1e-8 else sides):
            a=i*math.tau/sides;p=(r*math.cos(a),r*math.sin(a),z)
            if axis=='Y':p=(p[0],-p[2],p[1])
            if axis=='X':p=(p[2],p[0],p[1])
            ids.append(len(verts));verts.append(tuple(p[k]+loc[k] for k in range(3)))
        rings.append(ids)
    for a,c in zip(rings,rings[1:]):
        for i in range(sides):
            nxt=(i+1)%sides
            if len(a)==1 and len(c)>1:faces.append((a[0],c[nxt],c[i]))
            elif len(c)==1 and len(a)>1:faces.append((a[i],a[nxt],c[0]))
            elif len(a)>1 and len(c)>1:faces.append((a[i],a[nxt],c[nxt],c[i]))
    mesh=bpy.data.meshes.new(name);mesh.from_pydata(verts,[],faces);mesh.update()
    o=bpy.data.objects.new(name,mesh);bpy.context.collection.objects.link(o)
    return b.finish(o,name,mat,parent)

def loop(name,center,radius,thickness,mat,parent=None,normal=(0,0,1),segments=12):
    n=Vector(normal).normalized();u=n.cross(Vector((0,1,0)))
    if u.length<.01:u=n.cross(Vector((1,0,0)))
    u.normalize();v=n.cross(u);verts=[];faces=[]
    cross=LOOP_SIDES
    for i in range(segments):
        a=i*math.tau/segments;rad=u*math.cos(a)+v*math.sin(a)
        for k in range(cross):
            z=k*math.tau/cross;verts.append(Vector(center)+rad*(radius+thickness*math.cos(z))+n*thickness*math.sin(z))
    for i in range(segments):
        for k in range(cross):faces.append((i*cross+k,((i+1)%segments)*cross+k,((i+1)%segments)*cross+(k+1)%cross,i*cross+(k+1)%cross))
    mesh=bpy.data.meshes.new(name);mesh.from_pydata(verts,[],faces);mesh.update();o=bpy.data.objects.new(name,mesh);bpy.context.collection.objects.link(o)
    return b.finish(o,name,mat,parent)

def pin(name,loc,mat=2,parent=None,axis='X',r=.018):
    return lathe(name,[(0,-.012),(r,-.012),(r,.012),(0,.012)],mat,parent,8,loc,axis)

def paint(f):
    global LOOP_SIDES
    LOOP_SIDES=3 if f==1 else 4
    # Surface-specific original pixel source, baked to a shared atlas after unwrapping.
    palettes=[[(.11,.13,.14),(.31,.34,.33),(.56,.43,.23),(.38,.17,.075),(.12,.67,.71),(.035,.043,.041)],
              [(.095,.105,.10),(.32,.22,.12),(.56,.51,.36),(.33,.065,.045),(.86,.37,.09),(.055,.065,.055)],
              [(.16,.055,.12),(.37,.075,.12),(.57,.52,.36),(.065,.075,.09),(.40,.64,.23),(.25,.10,.24)]]
    b.MATS=[]
    for index,color in enumerate(palettes[f]):
        rng=random.Random(4211+f*31+index);im=bpy.data.images.new('source_%d_%d'%(f,index),width=256,height=256);pixels=[]
        for y in range(256):
            for x in range(256):
                value=rng.uniform(.94,1.04)
                if f==0:
                    value*=.94+.06*math.sin(y*.13)
                    if y%61<2 and x%127>5:value*=.55
                    if (x*17+y*31)%4093<7:value*=1.45
                    if y%73==7 and x%79<39:value*=1.25
                elif f==1:
                    value*=.90+.10*math.sin(x*.12)*math.sin(y*.045)
                    if index==1 and (x+int(6*math.sin(y*.07)))%59<3:value*=.55
                    if index==2 and (x*7+y*11)%271<3:value*=.6
                else:
                    value*=.87+.12*math.sin(x*.05)*math.cos(y*.035)
                    if abs((x+int(12*math.sin(y*.06)))%83-41)<2:value*=.55
                pixels.extend([min(1,c*value) for c in color]+[1])
        im.pixels=pixels;im.pack();m=bpy.data.materials.new('pilot_%d_surface_%d'%(f,index));m.use_nodes=True
        shader=m.node_tree.nodes.get('Principled BSDF');tex=m.node_tree.nodes.new('ShaderNodeTexImage');tex.image=im;tex.interpolation='Closest';m.node_tree.links.new(tex.outputs['Color'],shader.inputs['Base Color'])
        shader.inputs['Roughness'].default_value=.78 if f<2 else .54
        shader.inputs['Metallic'].default_value=.6 if f<2 and index in (0,1,2) else 0
        if index==4:shader.inputs['Emission Color'].default_value=(*color,1);shader.inputs['Emission Strength'].default_value=.55
        b.MATS.append(m)

def sentry():
    root=joint('DeployRoot',(0,0,0))
    # Armored octagonal cassette, rear service panel and replaceable power cartridge.
    lathe('armored_power_cassette',[(0,.035),(.17,.035),(.225,.075),(.225,.175),(.18,.225),(.12,.225),(.12,.185)],0,root,8)
    lathe('turntable_retaining_flange',[(.09,.18),(.15,.18),(.15,.245),(.09,.245)],1,root,12)
    for i in range(4):
        a=math.pi/4+i*math.pi/2;direction=Vector((math.cos(a),math.sin(a),0));p=direction*.15;p.z=.13
        # Tangent hinge rotates each rigid outrigger upward, leaving the cassette accessible.
        hinge_axis=(-math.sin(a),math.cos(a),0)
        leg=joint('Outrigger_%d'%i,p,root,pack_move=(-direction.x*.03,-direction.y*.03,0),pack_axis=hinge_axis,pack_angle=-1.45)
        end=direction*.48;end.z=.055;mid=direction*.34;mid.z=.09
        b.tube('forged_leg_%d'%i,[p,mid,end],[.035,.029,.025],1,leg,6)
        b.strut('leg_tension_link_%d'%i,p+Vector((0,0,.055)),mid,.015,2,leg)
        # Shaped ground shoe and a cylindrical hinge pin, attached to the same rigid link.
        shoe=profile('cleated_shoe_%d'%i,[(-.075,.026),(.075,.026),(.060,.071),(-.035,.085),(-.075,.055)],.11,'Y',0)
        shoe.location=(end.x,end.y,0);shoe.rotation_euler[2]=a;bpy.context.view_layer.update();world=shoe.matrix_world.copy();shoe.parent=leg;shoe.matrix_world=world
        pin('leg_hinge_%d'%i,p,2,leg,r=.025)
    lower=joint('MastLower',(0,0,.23),root,pack_move=(0,0,-.16))
    lathe('outer_mast_sleeve',[(.082,.205),(.098,.205),(.098,.39),(.082,.39)],1,root,10)
    lathe('middle_mast_slide',[(.055,.28),(.073,.28),(.073,.545),(.055,.545)],0,lower,10)
    upper=joint('MastUpper',(0,0,.49),lower,pack_move=(0,0,-.16))
    lathe('upper_mast_slide',[(.038,.44),(.052,.44),(.052,.705),(.038,.705)],1,upper,10)
    yaw=joint('AimYaw',(0,0,.70),upper,idle=('rot',(0,0,1),.18))
    lathe('yaw_bearing',[(0,.67),(.12,.67),(.14,.69),(.14,.735),(.10,.75),(0,.75)],0,yaw,12)
    # U-shaped fork and visible elevation trunnions.
    for side in (-1,1):
        profile('elevation_fork_%s'%side,[(-.085,.70),(.10,.70),(.11,.86),(.04,.90),(-.06,.86)],.035,'X',1,yaw).location.x=side*.145
        pin('trunnion_%s'%side,(side*.166,0,.825),2,yaw,r=.035)
    pitch=joint('AimPitch',(0,0,.825),yaw,pack_move=(0,.075,0))
    profile('receiver_casting',[(-.20,.79),(-.20,.88),(-.13,.94),(.11,.94),(.17,.88),(.17,.80),(.09,.765),(-.12,.765)],.25,'X',0,pitch)
    # Separate feed drum on the port side and a short guided belt entering the breech.
    lathe('ammunition_drum',[(0,-.06),(.105,-.06),(.12,-.045),(.12,.035),(.10,.05),(0,.05)],0,pitch,12,(-.205,.065,.835),'X')
    loop('drum_reinforcing_lip',(-.26,.065,.835),.102,.009,2,pitch,(1,0,0),12)
    pin('drum_axle',(-.268,.065,.835),2,pitch,r=.025)
    feed=joint('FeedIndex',(-.17,.024,.94),pitch,operation=('advance',(.026,0,0)))
    for i in range(5):
        x=-.17+i*.026;b.strut('linked_feed_round_%d'%i,(x,.07,.945),(x,-.015,.945),.012,2,feed)
        b.strut('feed_link_%d'%i,(x,.024,.94),(x+.024,.024,.94),.008,1,feed)
    b.box('ejection_recess',(.128,.04,.85),(.008,.095,.045),5,pitch,.001)
    b.box('breech_cover',(.134,.04,.888),(.012,.12,.018),1,pitch,.002)
    for side in (-1,1):
        recoil=joint('Recoil_%s'%side,(side*.065,-.15,.825),pitch,operation=('move',(0,.045,0)))
        lathe('barrel_jacket_%s'%side,[(.018,.12),(.034,.12),(.034,.28),(.025,.31),(.018,.31)],1,recoil,10,(side*.065,0,.825),'Y')
        slide=joint('BarrelExtension_%s'%side,(side*.065,-.27,.825),recoil,pack_move=(0,.23,0))
        lathe('telescoping_barrel_%s'%side,[(.012,.25),(.021,.25),(.021,.49),(.030,.49),(.030,.535),(.014,.535),(.014,.49),(.012,.49)],0,slide,10,(side*.065,0,.825),'Y')
        for y in (-.18,-.25):loop('jacket_band_%s_%s'%(side,y),(side*.065,y,.825),.035,.007,2,recoil,(0,1,0),10)
    # Recessed sensor visor, not an unprotected glowing bead.
    profile('sensor_visor',[(-.17,.94),(-.17,1.01),(-.12,1.04),(.03,1.02),(.06,.94)],.15,'X',1,pitch)
    b.box('sensor_dark_socket',(0,-.174,.985),(.11,.012,.046),5,pitch,.002)
    b.box('optic_window',(0,-.183,.985),(.061,.008,.025),4,pitch,.001)
    b.tube('protected_power_cable',[(.055,.095,.28),(.10,.13,.42),(.10,.11,.57),(.065,.08,.67)],[.014]*4,5,upper,6)
    b.box('rear_service_plate',(0,.199,.125),(.14,.018,.072),3,root,.004)
    for x in (-.06,.06):pin('service_fastener_%s'%x,(x,.212,.125),2,root,'Y',.009)
    # Model a shorter support stack; adjust authored vertices and joint origins together.
    bpy.context.view_layer.update()
    objects=list(bpy.context.scene.objects)
    cable=bpy.data.objects.get('protected_power_cable')
    if cable:bpy.data.objects.remove(cable,do_unlink=True)
    objects=list(bpy.context.scene.objects);original={o:o.matrix_world.copy() for o in objects};desired={}
    def adjusted(point):
        point=point.copy()
        if point.z>.23:point.z=.23+(point.z-.23)*.70
        return point
    for o in objects:
        matrix=original[o].copy();matrix.translation=adjusted(matrix.translation);desired[o]=matrix
        if o.type=='MESH':
            inv=matrix.inverted()
            for vertex in o.data.vertices:vertex.co=inv@adjusted(original[o]@vertex.co)
    def depth(o):return 0 if o.parent is None else 1+depth(o.parent)
    for o in sorted(objects,key=depth):o.matrix_world=desired[o];bpy.context.view_layer.update()
    return root

def chain(name,a,c,links,parent):
    a=Vector(a);c=Vector(c);distance=(c-a).length
    links=max(3,math.ceil(distance/.026)+1)
    for i in range(links):
        fraction=i/max(1,links-1);point=a.lerp(c,fraction)
        normal=(1,0,0) if i%2 else (0,1,0)
        o=loop(name+'_%d'%i,point,.019,.0045,0,parent,normal,4)
        # Loose chain gathers near its lower attachment instead of shrinking the links.
        if name.startswith('main_chain'):
            MOTION[o.name]=dict(move=(0,0,-.34*(1-fraction)),axis=None,angle=0,operation=None,idle=None)

def censer():
    root=joint('DeployRoot',(0,0,0))
    # A low iron cradle with three bent support branches, no cabinet or power pedestal.
    lathe('reliquary_cradle',[(0,.035),(.14,.035),(.19,.07),(.19,.15),(.15,.18),(.11,.18)],1,root,10)
    for i in range(3):
        a=i*math.tau/3+math.pi/6;c,s=math.cos(a),math.sin(a)
        leg=joint('IronLeg_%d'%i,(c*.12,s*.12,.13),root,pack_axis=(-s,c,0),pack_angle=-1.25)
        b.tube('curved_iron_foot_%d'%i,[(c*.12,s*.12,.13),(c*.27,s*.27,.12),(c*.40,s*.40,.055),(c*.46,s*.46,.05)],[.027,.032,.026,.016],0,leg,6)
        for k in range(2):pin('foot_rivet_%d_%d'%(i,k),(c*(.16+k*.05),s*(.16+k*.05),.15),1,leg,r=.012)
    lower=joint('CanopySlideLower',(0,0,.30),root,pack_move=(0,0,-.33))
    lift=joint('CanopySlideUpper',(0,0,.70),lower,pack_move=(0,0,-.33))
    for i in range(3):
        a=i*math.tau/3+math.pi/6;c,s=math.cos(a),math.sin(a)
        # Nested iron channels have visible overlapping sleeves and bronze retaining collars.
        b.tube('outer_iron_channel_%d'%i,[(c*.11,s*.11,.14),(c*.21,s*.21,.29),(c*.24,s*.24,.46)],[.035,.035,.034],0,root,6)
        b.tube('middle_iron_slide_%d'%i,[(c*.24,s*.24,.42),(c*.24,s*.24,.64),(c*.24,s*.24,.81)],[.026,.026,.025],0,lower,6)
        b.tube('inner_iron_slide_%d'%i,[(c*.24,s*.24,.76),(c*.24,s*.24,.98),(c*.14,s*.14,1.12),(0,0,1.16)],[.018,.018,.019,.016],0,lift,6)
        for z,r,parent in ((.45,.24,root),(.80,.24,lower)):
            loop('slide_retaining_collar_%d_%s'%(i,z),(c*r,s*r,z),.038,.009,1,parent,(0,0,1),8)
    loop('suspension_eye',(0,0,1.15),.030,.009,1,lift,(0,1,0),10)
    pendulum=joint('CenserPendulum',(0,0,1.17),lift,pack_move=(0,0,.30),idle=('rot',(1,0,0),.055),operation=('rot',(1,0,0),.11))
    for i in range(3):
        a=i*math.tau/3;c,s=math.cos(a),math.sin(a)
        chain('main_chain_%d'%i,(c*.025,s*.025,1.15),(c*.15,s*.15,.78),8,pendulum)
    # Hollow turned bowl: inner wall and vented lid are actual surfaces.
    lathe('bronze_censer_bowl',[(0,.43),(.075,.43),(.15,.47),(.205,.57),(.205,.66),(.17,.72),(.15,.72),(.183,.65),(.182,.58),(.13,.50),(.06,.47),(0,.47)],1,pendulum,12)
    for z,r in ((.52,.17),(.65,.205),(.72,.163)):
        loop('engraved_bowl_band_%s'%z,(0,0,z),r,.009,2,pendulum,(0,0,1),10)
    for i in range(6):
        a=i*math.tau/6;c,s=math.cos(a),math.sin(a)
        b.tube('cast_bowl_rib_%d'%i,[(c*.08,s*.08,.45),(c*.168,s*.168,.53),(c*.209,s*.209,.62),(c*.165,s*.165,.72)],[.012,.014,.013,.009],0,pendulum,5)
    for side in (-1,1):
        lid=joint('VentedLid_%s'%side,(side*.15,0,.72),pendulum,operation=('open',(0,1,0),side*.28))
        # Open sectors between curved ribs make genuine emission apertures.
        for i in range(5):
            a=(i+.5)*math.pi/5+(0 if side>0 else math.pi);c,s=math.cos(a),math.sin(a)
            b.tube('pierced_lid_rib_%s_%d'%(side,i),[(c*.16,s*.16,.72),(c*.12,s*.12,.79),(c*.045,s*.045,.85),(0,0,.86)],[.013,.014,.012,.01],1,lid,5)
    lathe('inner_incense_basket',[(0,.48),(.10,.48),(.12,.56),(.12,.60),(.10,.60),(.09,.53),(0,.53)],5,pendulum,8)
    for i in range(3):
        a=i*math.tau/3;bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=1,radius=.035,location=(.065*math.cos(a),.065*math.sin(a),.57));b.finish(bpy.context.object,'banked_incense_%d'%i,4,pendulum)
    # Smaller hanging reliquaries have lids, vessels and chains, rather than colored beads.
    for i in range(3):
        a=i*math.tau/3+math.pi/6;c,s=math.cos(a),math.sin(a)
        pendant=joint('SatelliteCenser_%d'%i,(c*.27,s*.27,.88),lift,pack_move=(.165*math.cos(a+math.pi/3)-.27*c,.165*math.sin(a+math.pi/3)-.27*s,.30),pack_axis=(0,0,1),pack_angle=math.pi/3,idle=('rot',(c,s,0),.08),operation=('rot',(c,s,0),.16))
        MOTION[pendant.name]['satellite_angle']=a
        chain('satellite_chain_%d'%i,(c*.28,s*.28,.87),(c*.34,s*.34,.60),5,pendant)
        dish=joint('SatelliteDish_%d'%i,(c*.34,s*.34,.56),pendant,pack_axis=(-s,c,0),pack_angle=math.pi/2)
        lathe('satellite_bowl_%d'%i,[(0,-.020),(.040,-.020),(.075,.005),(.075,.020),(.060,.020),(.035,-.005),(0,-.005)],1,dish,6,(c*.34,s*.34,.56))
        for k in range(4):
            angle=k*math.pi/2;b.strut('satellite_lid_%d_%d'%(i,k),(c*.34+.055*math.cos(angle),s*.34+.055*math.sin(angle),.58),(c*.34,s*.34,.60),.008,0,dish)
    # Folded cloth banner attached to the front cradle with visible retaining pins.
    verts=[(-.065,-.178,.19),(0,-.192,.20),(.065,-.178,.19),(-.06,-.19,.055),(0,-.205,.035),(.06,-.19,.055)]
    mesh=bpy.data.meshes.new('folded_votive_cloth');mesh.from_pydata(verts,[],[(0,1,4,3),(1,2,5,4)]);mesh.update();o=bpy.data.objects.new('folded_votive_cloth',mesh);bpy.context.collection.objects.link(o);b.finish(o,o.name,3,root)
    for x in (-.05,.05):pin('cloth_pin_%s'%x,(x,-.186,.184),1,root,'Y',.008)
    return root

def heart():
    root=joint('DeployRoot',(0,0,0));body=[]
    # Unequal ventricles with a deep interventricular cleft and a tapered lower apex.
    def flesh(name,loc,size,material=0):
        o=b.orb(name,loc,size,material);body.append(o);return o
    flesh('left_ventricle',(-.15,.025,.59),(.235,.205,.36),0)
    flesh('right_ventricle',(.155,.035,.57),(.19,.18,.295),5)
    flesh('tapered_apex',(-.055,.035,.34),(.15,.14,.23),0)
    flesh('atrial_crown',(-.15,.065,.89),(.13,.14,.15),1)
    flesh('secondary_atrium',(.17,.07,.83),(.11,.12,.13),1)
    flesh('rooted_nutrient_organ',(0,.045,.18),(.225,.185,.13),0)
    def duct(name,pts,rs,mat=1):
        o=b.tube(name,pts,rs,mat,None,8);body.append(o);return o
    # Crossing great vessels and a rear return loop merge into real chamber tissue.
    duct('ascending_vessel',[(-.13,.10,.80),(-.15,.13,1.03),(.02,.12,1.10),(.12,.13,.94),(.10,.10,.78)],[.065,.065,.059,.052,.055])
    duct('return_vessel',[(.17,.11,.75),(.30,.12,.88),(.28,.13,1.01),(.10,.16,1.03),(-.02,.13,.88)],[.05,.055,.05,.044,.045])
    # Raised branching coronary routes, anchored along the cleft instead of painted at random.
    duct('coronary_trunk',[(.02,-.13,.91),(.015,-.185,.76),(-.025,-.185,.58),(-.08,-.13,.36)],[.025,.028,.024,.018])
    for side in (-1,1):
        for k in range(2):
            z=.73-k*.16;duct('coronary_branch_%s_%s'%(side,k),[(0,-.18,z),(side*.12,-.20,z-.045),(side*.24,-.13,z-.12)],[.022,.019,.012])
    for i in range(5):
        a=i*math.tau/5;c,s=math.cos(a),math.sin(a)
        duct('anchoring_root_%d'%i,[(c*.08,s*.08,.20),(c*.27,s*.24,.13),(c*.42,s*.37,.045)],[.075,.055,.025],0)
    for side in (-1,1):
        flesh('nutrient_sac_%s'%side,(side*.28,.10,.23),(.11,.10,.14),5)
        duct('nutrient_umbilical_%s'%side,[(side*.26,.10,.26),(side*.28,.14,.40),(side*.20,.12,.53)],[.04,.039,.05])
    for side in (-1,1):
        duct('restoration_tendril_%s'%side,[(side*.12,-.10,.25),(side*.19,-.23,.28),(side*.23,-.34,.35),(side*.18,-.40,.42)],[.048,.040,.032,.025],1)
        flesh('treatment_bud_%s'%side,(side*.18,-.40,.42),(.035,.045,.045),4)
    # Join/voxel union; material separation is retained on the resulting continuous anatomy.
    bpy.ops.object.select_all(action='DESELECT')
    for o in body:o.select_set(True)
    bpy.context.view_layer.objects.active=body[0];bpy.ops.object.join();o=body[0];o.name='CardiovascularSurface'
    mod=o.modifiers.new('actual_tissue_union','REMESH');mod.mode='VOXEL';mod.voxel_size=.017;mod.use_smooth_shade=True;bpy.ops.object.modifier_apply(modifier=mod.name)
    mod=o.modifiers.new('tissue_transition_refinement','SMOOTH');mod.factor=.60;mod.iterations=2;bpy.ops.object.modifier_apply(modifier=mod.name)
    mod=o.modifiers.new('surface_runtime_budget','DECIMATE');mod.ratio=min(1,2900/max(1,len(o.data.polygons)*2));bpy.ops.object.modifier_apply(modifier=mod.name)
    o.data.materials.clear()
    for material in b.MATS:o.data.materials.append(material)
    coronary=[[(.02,-.13,.91),(.015,-.185,.76),(-.025,-.185,.58),(-.08,-.13,.36)]]
    for side in (-1,1):
        for k in range(2):
            z=.73-k*.16;coronary.append([(0,-.18,z),(side*.12,-.20,z-.045),(side*.24,-.13,z-.12)])
    segments=[(Vector(a),Vector(c)) for path in coronary for a,c in zip(path,path[1:])]
    # Paint smooth anatomical material transitions onto the connected mesh, then bake them.
    colors=o.data.color_attributes.new(name='TissueColor',type='FLOAT_COLOR',domain='CORNER')
    def smooth(a,c,value):
        t=max(0,min(1,(value-a)/(c-a)));return t*t*(3-2*t)
    for loopdata in o.data.loops:
        point=o.matrix_world@o.data.vertices[loopdata.vertex_index].co;distances=[]
        for a,c in segments:
            line=c-a;t=max(0,min(1,(point-a).dot(line)/line.length_squared));distances.append((point-a-line*t).length)
        vessel=max(1-smooth(.019,.048,min(distances)),smooth(.86,.99,point.z))
        right=smooth(-.035,.17,point.x)
        base=Vector((.045,.010,.030)).lerp(Vector((.070,.015,.055)),right)
        color=base.lerp(Vector((.12,.015,.025)),vessel)
        if point.y<-.28:color=color.lerp(Vector((.10,.025,.040)),smooth(-.28,-.40,point.y))
        colors.data[loopdata.index].color=(*color,1)
    for polygon in o.data.polygons:polygon.material_index=0
    material=b.MATS[0];nodes=material.node_tree.nodes;links=material.node_tree.links
    attribute=nodes.new('ShaderNodeVertexColor');attribute.layer_name='TissueColor'
    noise=nodes.new('ShaderNodeTexNoise');noise.inputs['Scale'].default_value=25;noise.inputs['Detail'].default_value=2
    remap=nodes.new('ShaderNodeMapRange');remap.inputs['To Min'].default_value=.87;remap.inputs['To Max'].default_value=1.03;links.new(noise.outputs['Fac'],remap.inputs['Value'])
    multiply=nodes.new('ShaderNodeVectorMath');multiply.operation='SCALE';links.new(attribute.outputs['Color'],multiply.inputs[0]);links.new(remap.outputs['Result'],multiply.inputs['Scale'])
    links.new(multiply.outputs[0],nodes.get('Principled BSDF').inputs['Base Color'])
    # Bake before shape keys. These keys deform actual connected tissue, leaving root contacts fixed.
    o['add_cardiac_morphs']=True;world=o.matrix_world.copy();o.parent=root;o.matrix_world=world
    # Distinct bone cradle protects the rear and cradles the lower chamber; no loose front paddles.
    for side in (-1,1):
        rib=joint('CradleRib_%s'%side,(side*.13,.09,.22),root,pack_axis=(0,1,0),pack_angle=-side*.90)
        b.tube('load_bearing_cradle_%s'%side,[(side*.13,.09,.20),(side*.28,.15,.34),(side*.29,.19,.47),(side*.20,.20,.56)],[.04,.037,.030,.012],2,rib,7)
    return root

def cardiac_keys(o):
    o.shape_key_add(name='Basis');left=o.shape_key_add(name='LeftSystole');right=o.shape_key_add(name='RightSystole');seed=o.shape_key_add(name='PackedSeed')
    for i,v in enumerate(o.data.vertices):
        p=v.co.copy();world=o.matrix_world@p;z=world.z
        attachment=max(0,min(1,(z-.23)/.22))
        for key,cx,cz,sx,sz in [(left,-.15,.59,.24,.36),(right,.155,.57,.20,.30)]:
            weight=math.exp(-((world.x-cx)/sx)**2-((z-cz)/sz)**2)*attachment
            q=world.copy();q.x=cx+(q.x-cx)*(1-.17*weight);q.y=.035+(q.y-.035)*(1-.20*weight);q.z+=.035*weight
            if key==right and world.y<-.20:
                treatment=max(0,min(1,(-world.y-.20)/.20));q.y-=.06*treatment;q.z+=.025*treatment
            key.data[i].co=o.matrix_world.inverted()@q
        # Biological seed geometry is deformed per vertex; roots curl inward while vessels collapse.
        q=world.copy();q.x*=.43;q.y*=.43;q.z=.035+(z-.02)*.34
        q.x+=.022*math.sin(z*11)*attachment;q.y+=.018*math.cos(z*9)*attachment
        seed.data[i].co=o.matrix_world.inverted()@q

def write_clips(path,pilot):
    raw=path.read_bytes();length=struct.unpack_from('<I',raw,12)[0];doc=json.loads(raw[20:20+length]);off=20+length;n=struct.unpack_from('<I',raw,off)[0];binary=bytearray(raw[off+8:off+8+n]);doc['animations']=[]
    doc['asset'].setdefault('extras',{}).update(authoring_up='+Z',authoring_front='-Y',runtime_up='+Y',runtime_front='-Z',runtime_normals=False,baked_form_lighting=bool(bpy.context.scene.get('baked_form_lighting')))
    def accessor(values,width):
        while len(binary)%4:binary.append(0)
        start=len(binary)
        for value in values:binary.extend(struct.pack('<'+'f'*width,*value))
        view=len(doc['bufferViews']);doc['bufferViews'].append(dict(buffer=0,byteOffset=start,byteLength=len(binary)-start))
        a=dict(bufferView=view,componentType=5126,count=len(values),type={1:'SCALAR',3:'VEC3',4:'VEC4'}[width])
        if width==1:a.update(min=[min(v[0] for v in values)],max=[max(v[0] for v in values)])
        ident=len(doc['accessors']);doc['accessors'].append(a);return ident
    def cv(v):return [v[0],v[2],-v[1]]
    def quaternion(axis,angle):
        q=Quaternion(Vector(axis),angle);convert=Quaternion((1,0,0),-math.pi/2);q=convert@q@convert.inverted();return [q.x,q.y,q.z,q.w]
    durations={'ACTIVE':2.,'PACKED':1.,'THROWN':.7,'IMPACT':.15,'DEPLOYING':1.5,'OPERATING':.65,'DAMAGED':.45,'EXPIRING':1.,'DESTROYED':.8}
    for clip,duration in durations.items():
        anim=dict(name=clip,samplers=[],channels=[]);fractions=[i/20 for i in range(21)] if pilot=='field-medic' else [0,.15,.35,.55,.75,1];time=accessor([[f*duration] for f in fractions],1)
        def track(node,path,values):
            width=1 if path=='weights' else len(values[0]);sampler=len(anim['samplers']);anim['samplers'].append(dict(input=time,output=accessor(values,width),interpolation='LINEAR'));anim['channels'].append(dict(sampler=sampler,target=dict(node=node,path=path)))
        for i,node in enumerate(doc['nodes']):
            name=node.get('name','');motion=MOTION.get(name);base=node.get('translation',[0,0,0]);rotation=node.get('rotation',[0,0,0,1])
            if name=='DeployRoot':
                if clip=='THROWN':
                    values=[];rest=Quaternion((rotation[3],rotation[0],rotation[1],rotation[2]))
                    for f in fractions:
                        spin=quaternion((1,.2,0),f*math.pi*1.4);combined=rest@Quaternion((spin[3],spin[0],spin[1],spin[2]));values.append([combined.x,combined.y,combined.z,combined.w])
                    track(i,'rotation',values)
                elif clip=='IMPACT':track(i,'translation',[[0,.04*math.sin(f*math.pi),0] for f in fractions])
                continue
            if name=='PharmaceuticalHose':
                angles=node['extras']['fold_angles'];values=[]
                for f in fractions:
                    angle=math.pi/2 if clip in ('PACKED','THROWN','IMPACT') else math.pi/2*(1-f*f*(3-2*f)) if clip=='DEPLOYING' else math.pi/2*f if clip=='DESTROYED' else 0
                    weights=[0.]*len(angles)
                    if angle>0:
                        for target,upper in enumerate(angles):
                            lower=angles[target+1] if target+1<len(angles) else 0
                            if lower<=angle<=upper+1e-8:
                                alpha=(angle-lower)/(upper-lower);weights[target]=alpha
                                if target+1<len(angles):weights[target+1]=1-alpha
                                break
                    values.extend([[w] for w in weights])
                track(i,'weights',values)
            if name=='CardiovascularSurface':
                values=[]
                for f in fractions:
                    if clip in ('PACKED','THROWN','IMPACT'):weights=[0,0,1]
                    elif clip=='DEPLOYING':weights=[0,0,1-f*f*(3-2*f)]
                    elif clip in ('ACTIVE','OPERATING'):
                        strength=.3 if clip=='ACTIVE' else 1.;weights=[strength*max(0,math.sin(f*math.tau)),strength*max(0,math.sin((f-.20)*math.tau)),0]
                    elif clip=='DESTROYED':weights=[f,.7*f,.45*f]
                    else:weights=[.4*abs(math.sin(f*math.tau)),.3*abs(math.sin((f+.2)*math.tau)),0]
                    values.extend([[w] for w in weights])
                track(i,'weights',values)
            if not motion:continue
            packed=clip in ('PACKED','THROWN','IMPACT');deploy=clip=='DEPLOYING'
            if name.startswith('SatelliteCenser') and deploy:
                angle=motion['satellite_angle'];values=[]
                for f in fractions:
                    angular=max(0,min(1,(f-.35)/.65));angular=angular*angular*(3-2*angular)
                    radial=min(1,f/.35);radial=radial*radial*(3-2*radial)
                    radius=.165+.125*radial if f<=.35 else .29-.02*angular
                    theta=angle+math.pi/3*(1-angular)
                    delta=cv((radius*math.cos(theta)-.27*math.cos(angle),radius*math.sin(theta)-.27*math.sin(angle),.30*(1-f*f*(3-2*f))))
                    values.append([base[k]+delta[k] for k in range(3)])
                track(i,'translation',values)
            if motion['move'] is not None and not(name.startswith('SatelliteCenser') and deploy):
                delta=cv(motion['move']);track(i,'translation',[[base[k]+delta[k]*(1 if packed else 1-f*f*(3-2*f) if deploy else f if clip=='DESTROYED' else 0) for k in range(3)] for f in fractions])
            if name.startswith('IronLeg') and deploy:
                # Lift the curved toe during its hinge sweep, then settle it onto the floor.
                track(i,'translation',[[base[0],base[1]+.020*math.sin(f*math.pi),base[2]] for f in fractions])
            if motion['axis'] and (packed or deploy or clip=='DESTROYED'):
                values=[]
                for f in fractions:
                    progress=max(0,min(1,(f-.35)/.65)) if name.startswith(('SatelliteCenser','SatelliteDish')) and deploy else f
                    factor=1 if packed else 1-progress*progress*(3-2*progress) if deploy else f if clip=='DESTROYED' else 0
                    values.append(quaternion(motion['axis'],motion['angle']*factor))
                track(i,'rotation',values)
            action=motion['operation'] if clip=='OPERATING' else motion['idle'] if clip=='ACTIVE' else None
            if action:
                if action[0] in ('move','advance'):
                    delta=cv(action[1]);track(i,'translation',[[base[k]+delta[k]*(f if action[0]=='advance' else max(0,math.sin(f*math.tau))) for k in range(3)] for f in fractions])
                else:track(i,'rotation',[quaternion(action[1],action[2]*(math.sin(f*math.pi) if action[0]=='open' else math.sin(f*math.tau))) for f in fractions])
            if clip in ('DAMAGED','EXPIRING') and ('Mast' in name or 'Pendulum' in name or 'Cradle' in name):
                track(i,'rotation',[quaternion((1,0,0),(.045 if clip=='DAMAGED' else .025)*math.sin(f*math.tau)) for f in fractions])
        # Packed clips require nonempty channels even when only biological weights are present.
        doc['animations'].append(anim)
    doc['buffers'][0]['byteLength']=len(binary);encoded=json.dumps(doc,separators=(',',':')).encode();encoded+=b' '*((-len(encoded))%4);binary+=b'\0'*((-len(binary))%4)
    path.write_bytes(struct.pack('<III',0x46546c67,2,28+len(encoded)+len(binary))+struct.pack('<II',len(encoded),0x4e4f534a)+encoded+struct.pack('<II',len(binary),0x004e4942)+binary)

def build():
    records=[]
    for f,role,identifier,builder in [(0,'turret','sentry-mki',sentry),(1,'miasma','censer-of-dread',censer),(2,'health','regeneration-heart',heart)]:
        if '--only' in sys.argv and sys.argv[sys.argv.index('--only')+1]!=identifier:continue
        bpy.ops.wm.read_factory_settings(use_empty=True);MOTION.clear();b.PARTS.clear();paint(f);builder();bpy.context.view_layer.update()
        # Coherent reusable atlas per pilot, baked from actual material surfaces.
        if identifier=='censer-of-dread':
            for mesh in [o for o in bpy.context.scene.objects if o.type=='MESH']:
                if 'bowl' in mesh.name or 'bowl_band' in mesh.name:continue
                b.activate(mesh);mod=mesh.modifiers.new('authored_runtime_lod','DECIMATE');mod.ratio=.52 if 'chain' in mesh.name else .54;bpy.ops.object.modifier_apply(modifier=mod.name)
        lowest=min((o.matrix_world@v.co).z for o in bpy.context.scene.objects if o.type=='MESH' for v in o.data.vertices)
        for child in list(bpy.data.objects['DeployRoot'].children):child.location.z-=lowest
        bpy.context.view_layer.update()
        meshes=b.unwrap_bake(identifier)
        from bake_field_lighting import bake
        bake(meshes,b.MATS,identifier)
        for o in meshes:
            if o.get('add_cardiac_morphs'):cardiac_keys(o)
        dest=OUT/identifier;dest.mkdir(parents=True,exist_ok=True)
        bpy.ops.wm.save_as_mainfile(filepath=str(dest/(identifier+'.blend')))
        bpy.data.objects['DeployRoot'].rotation_euler[2]=math.pi;bpy.context.view_layer.update()
        glb=dest/(identifier+'.glb');bpy.ops.export_scene.gltf(filepath=str(glb),export_format='GLB',export_yup=True,export_animations=False,export_morph=True,export_normals=False,export_morph_normal=False,export_try_sparse_sk=False,export_extras=True)
        write_clips(glb,identifier)
        pts=[o.matrix_world@v.co for o in meshes for v in o.data.vertices]
        bounds=[[min(p[k] for p in pts) for k in range(3)],[max(p[k] for p in pts) for k in range(3)]]
        records.append(dict(id=identifier,path=str(glb),category='candidate',faction=b.FACTIONS[f],role=role,sha256=b.sha(glb),triangles=sum(len(p.vertices)-2 for o in meshes for p in o.data.polygons),bounds_z_up=bounds,status='REVIEW_REQUIRED',motion_design=MOTION.copy()))
        (OUT/'manifest.json').write_text(json.dumps(records,indent=2)+'\n');print('PILOT_EXPORTED',identifier,records[-1]['triangles'],bounds,flush=True)

if __name__=='__main__':build()
