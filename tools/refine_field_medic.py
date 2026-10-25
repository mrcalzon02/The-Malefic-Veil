"""Purpose-designed medical pilot: clamshell, retained instruments and articulated injector.
Blender 5 --factory-startup -b --python tools/refine_field_medic.py
"""
import bpy,sys,math,json
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parent))
import refine_field_pilots as p
b=p.b
ROOT=Path(__file__).resolve().parents[1]
REVISION=sys.argv[sys.argv.index('--revision')+1] if '--revision' in sys.argv else 'field-medic-r7'
OUT=ROOT/'build/candidates'/REVISION

from mathutils import Vector,Matrix
FOLD_ANGLES=[math.pi/2*(1-(i/20)**2*(3-2*i/20)) for i in range(20)]
def hose_points(angle):
    pivot=Vector((.115,-.09,.665));end=pivot+Matrix.Rotation(-angle,3,'Z')@(Vector((.13,-.24,.80))-pivot)+Vector((0,0,-.12*angle/(math.pi/2)))
    start=Vector((.04,.025,.61));c1=start+Vector((.035,-.055,.025));c2=end+Vector((.01,.05,-.025))
    return [tuple((1-t)**3*start+3*(1-t)**2*t*c1+3*(1-t)*t*t*c2+t**3*end) for t in [i/8 for i in range(9)]]
def make_hose(parent):
    hose=b.tube('PharmaceuticalHose',hose_points(0),[.010]*9,5,parent,6)
    for face in hose.data.polygons:face.use_smooth=True
    return hose

def hose_keys(hose):
    # Shape keys use the same ring topology as b.tube. Both end sockets remain explicit.
    hose.shape_key_add(name='Basis');hose['fold_angles']=FOLD_ANGLES
    for index,angle in enumerate(FOLD_ANGLES):
        pts=hose_points(angle);key=hose.shape_key_add(name='Fold_%02d'%index)
        for ring,center in enumerate(pts):
            tangent=Vector(pts[min(ring+1,8)])-Vector(pts[max(0,ring-1)]);tangent.normalize();u=tangent.cross(Vector((0,0,1)))
            if u.length<.01:u=tangent.cross(Vector((0,1,0)))
            u.normalize();v=tangent.cross(u).normalized()
            for side in range(6):key.data[ring*6+side].co=Vector(center)+.010*(math.cos(side*math.tau/6)*u+math.sin(side*math.tau/6)*v)

def build():
    bpy.ops.wm.read_factory_settings(use_empty=True);p.MOTION.clear();b.PARTS.clear();p.paint(0)
    material=bpy.data.materials.new('worn_medical_enamel');material.use_nodes=True;shader=material.node_tree.nodes.get('Principled BSDF');shader.inputs['Base Color'].default_value=(.51,.54,.47,1);shader.inputs['Roughness'].default_value=.86;b.MATS.append(material)
    root=p.joint('DeployRoot',(0,0,0))
    # An open skid stand, distinct from the turret's armored circular power cassette.
    for side in (-1,1):
        b.tube('ground_skid_%s'%side,[(side*.15,-.23,.025),(side*.15,-.18,.045),(side*.15,.18,.045),(side*.15,.23,.025)],[.028]*4,0,root,6)
        b.strut('stand_upright_%s'%side,(side*.15,0,.05),(side*.15,0,.24),.027,1,root)
    b.strut('stand_crossmember',(-.15,0,.075),(.15,0,.075),.025,1,root)
    lower=p.joint('MedicalMastLower',(0,0,.22),root,pack_move=(0,0,-.18))
    mast=p.joint('MedicalMast',(0,0,.40),lower,pack_move=(0,0,-.18))
    for side in (-1,1):
        for name,z0,z1,outer,inner,parent in [('outer',.04,.26,.035,.029,root),('middle',.22,.44,.026,.021,lower),('upper',.40,.60,.018,.012,mast)]:
            p.lathe('telescopic_%s_%s'%(name,side),[(inner,z0),(outer,z0),(outer,z1),(inner,z1),(inner,z0)],1,parent,8,(side*.13,0,0))
    # Hollow clamshell with an accessible interior, not a solid cabinet bearing a cross.
    # Four floor plates leave a real mounting slot for the retracting injector socket.
    for name,loc,size in [('left',(-.075,0,.565),(.27,.34,.035)),('right',(.1825,0,.565),(.055,.34,.035)),('front',(.1075,-.1525,.565),(.095,.035,.035)),('rear',(.1075,.06,.565),(.095,.22,.035))]:
        b.box('case_floor_'+name,loc,size,0,mast,.004)
    p.lathe('injector_mounting_socket',[(.026,.515),(.036,.515),(.036,.60),(.026,.60),(.026,.515)],1,mast,10,(.115,-.09,0))
    b.box('front_retaining_lip',(0,-.16,.595),(.40,.025,.05),1,mast,.006)
    b.box('rear_hinge_rail',(0,.15,.645),(.42,.025,.15),0,mast,.007)
    for side in (-1,1):
        b.box('case_side_%s'%side,(side*.20,0,.635),(.025,.31,.13),0,mast,.007)
        p.pin('lid_hinge_pin_%s'%side,(side*.17,.145,.735),2,mast,'X',.017)
    lid=p.joint('MedicalCaseLid',(0,.145,.735),mast,pack_axis=(1,0,0),pack_angle=math.pi/2)
    shell=p.profile('back_lid_shell',[(-.20,.735),(.20,.735),(.20,1.00),(.17,1.035),(-.17,1.035),(-.20,1.00)],.025,'Y',0)
    shell.location.y=.145;bpy.context.view_layer.update();world=shell.matrix_world.copy();shell.parent=lid;shell.matrix_world=world
    b.box('cream_inner_instrument_panel',(0,.125,.88),(.35,.014,.24),6,lid,.008)
    # A retained pharmaceutical cartridge bank with distinct bottles and locking collars.
    for i in range(3):
        x=-.11+i*.075
        p.lathe('drug_cartridge_%d'%i,[(0,.59),(.027,.59),(.027,.69),(.021,.71),(0,.71)],6,mast,8,(x,.035,0))
        p.loop('cartridge_retainer_%d'%i,(x,.035,.625),.03,.007,1,mast,(0,0,1),8)
        b.box('cartridge_label_%d'%i,(x,.005,.65),(.031,.006,.028),4,mast,.001)
    for i in range(3):b.box('treatment_status_%d'%i,(.105,.111,.82+i*.052),(.045,.008,.018),4,lid,.002)
    b.box('secondary_medical_mark_v',(-.075,.110,.93),(.026,.007,.09),4,lid,.001)
    b.box('secondary_medical_mark_h',(-.075,.108,.93),(.09,.007,.026),4,lid,.001)
    # Side trays fold into the case. Rims retain equipment through deployment.
    for side in (-1,1):
        tray=p.joint('InstrumentTray_%s'%side,(side*.20,0,.615),mast,pack_axis=(0,1,0),pack_angle=-side*math.pi/2)
        b.box('tray_surface_%s'%side,(side*.265,0,.615),(.13,.27,.018),6,tray,.004)
        for y in (-.13,.13):b.box('tray_end_rim_%s_%s'%(side,y),(side*.265,y,.634),(.13,.012,.025),1,tray,.002)
        for k in range(2):
            y=-.055+k*.105
            b.strut('retained_instrument_%s_%d'%(side,k),(side*.23,y,.642),(side*.31,y,.642),.010,1,tray)
            p.loop('instrument_grip_%s_%d'%(side,k),(side*.315,y,.642),.018,.004,2,tray,(0,0,1),8)
    # The treatment arm is recognizable as an injector/pump assembly even without the emblem.
    arm=p.joint('InjectorArm',(.115,-.09,.665),mast,pack_move=(0,0,-.12),pack_axis=(0,0,1),pack_angle=-math.pi/2)
    b.strut('injector_mounting_pushrod',(.115,-.09,.57),(.115,-.09,.665),.018,1,arm)
    b.tube('articulated_injector_support',[(.115,-.09,.665),(.145,-.17,.79),(.115,-.29,.79)],[.025,.024,.02],1,arm,6)
    p.pin('injector_elbow',(.145,-.17,.79),2,arm,'X',.027)
    p.lathe('injector_reservoir',[(0,.255),(.029,.255),(.029,.35),(.019,.36),(0,.36)],6,arm,8,(.115,0,.79),'Y')
    plunger=p.joint('InjectorPlunger',(.115,-.25,.79),arm,operation=('move',(0,-.035,0)))
    b.strut('injector_pushrod',(.115,-.20,.79),(.115,-.285,.79),.010,1,plunger)
    b.box('injector_thumb_plate',(.115,-.205,.79),(.07,.012,.05),4,plunger,.003)
    p.joint('HoseCaseSocket',(.04,.025,.61),mast)
    p.joint('HoseInjectorSocket',(.13,-.24,.80),arm)
    hose=make_hose(mast)
    pump=p.joint('MeteringPump',(-.08,.05,.69),mast,idle=('rot',(0,0,1),.035),operation=('rot',(0,0,1),.42))
    p.loop('pump_drive_wheel',(-.08,.05,.69),.044,.008,2,pump,(0,0,1),10)
    b.box('patient_contact_cup',(.115,-.362,.79),(.065,.018,.056),5,arm,.005)
    bpy.context.view_layer.update()
    floor=min((o.matrix_world@v.co).z for o in bpy.context.scene.objects if o.type=='MESH' for v in o.data.vertices)
    for child in root.children:child.location.z-=floor
    bpy.context.view_layer.update();meshes=b.unwrap_bake('field-medic');
    from bake_field_lighting import bake
    bake(meshes,b.MATS,'field-medic');hose_keys(hose);dest=OUT/'field-medic';dest.mkdir(parents=True,exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(dest/'field-medic.blend'))
    root.rotation_euler[2]=math.pi;bpy.context.view_layer.update()
    glb=dest/'field-medic.glb';bpy.ops.export_scene.gltf(filepath=str(glb),export_format='GLB',export_animations=False,export_yup=True,export_extras=True,export_normals=False,export_morph_normal=False)
    p.write_clips(glb,'field-medic')
    record=dict(id='field-medic',path=str(glb),category='candidate',faction='unraveled',role='health',sha256=b.sha(glb),triangles=sum(len(face.vertices)-2 for o in meshes for face in o.data.polygons),status='REVIEW_REQUIRED')
    (OUT/'manifest.json').write_text(json.dumps([record],indent=2)+'\n');print('MEDIC_EXPORTED',record['triangles'],flush=True)

if __name__=='__main__':build()
