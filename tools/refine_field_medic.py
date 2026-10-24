"""Purpose-designed medical pilot: clamshell, retained instruments and articulated injector.
Blender 5 --factory-startup -b --python tools/refine_field_medic.py
"""
import bpy,sys,math,json
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parent))
import refine_field_pilots as p
b=p.b
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/candidates/field-medic-r1'

def build():
    bpy.ops.wm.read_factory_settings(use_empty=True);p.MOTION.clear();b.PARTS.clear();p.paint(0)
    material=bpy.data.materials.new('worn_medical_enamel');material.use_nodes=True;shader=material.node_tree.nodes.get('Principled BSDF');shader.inputs['Base Color'].default_value=(.51,.54,.47,1);shader.inputs['Roughness'].default_value=.86;b.MATS.append(material)
    root=p.joint('DeployRoot',(0,0,0))
    # An open skid stand, distinct from the turret's armored circular power cassette.
    for side in (-1,1):
        b.tube('ground_skid_%s'%side,[(side*.15,-.23,.025),(side*.15,-.18,.045),(side*.15,.18,.045),(side*.15,.23,.025)],[.028]*4,0,root,6)
        b.strut('stand_upright_%s'%side,(side*.15,0,.05),(side*.15,0,.24),.027,1,root)
    b.strut('stand_crossmember',(-.15,0,.075),(.15,0,.075),.025,1,root)
    mast=p.joint('MedicalMast',(0,0,.23),root,pack_move=(0,0,-.34))
    for side in (-1,1):
        b.strut('telescopic_lower_%s'%side,(side*.13,0,.16),(side*.13,0,.38),.034,0,root)
        b.strut('telescopic_upper_%s'%side,(side*.13,0,.32),(side*.13,0,.58),.023,1,mast)
    # Hollow clamshell with an accessible interior, not a solid cabinet bearing a cross.
    b.box('case_floor',(0,0,.565),(.42,.34,.035),0,mast,.012)
    b.box('front_retaining_lip',(0,-.16,.595),(.40,.025,.05),1,mast,.006)
    b.box('rear_hinge_rail',(0,.15,.645),(.42,.025,.15),0,mast,.007)
    for side in (-1,1):
        b.box('case_side_%s'%side,(side*.20,0,.635),(.025,.31,.13),0,mast,.007)
        p.pin('lid_hinge_pin_%s'%side,(side*.17,.145,.735),2,mast,'X',.017)
    lid=p.joint('MedicalCaseLid',(0,.145,.735),mast,pack_axis=(1,0,0),pack_angle=math.pi/2)
    p.profile('back_lid_shell',[(-.20,.735),(.20,.735),(.20,1.00),(.17,1.035),(-.17,1.035),(-.20,1.00)],.025,'Y',0,lid).location.y=.145
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
    arm=p.joint('InjectorArm',(.115,-.09,.665),mast,pack_axis=(1,0,0),pack_angle=math.pi/2)
    b.tube('articulated_injector_support',[(.115,-.09,.665),(.17,-.17,.79),(.115,-.29,.82)],[.025,.024,.02],1,arm,6)
    p.pin('injector_elbow',(.17,-.17,.79),2,arm,'X',.027)
    p.lathe('injector_reservoir',[(0,.255),(.029,.255),(.029,.35),(.019,.36),(0,.36)],6,arm,8,(.115,0,.82),'Y')
    plunger=p.joint('InjectorPlunger',(.115,-.25,.82),arm,operation=('move',(0,-.035,0)))
    b.strut('injector_pushrod',(.115,-.20,.82),(.115,-.285,.82),.010,1,plunger)
    b.box('injector_thumb_plate',(.115,-.205,.82),(.07,.012,.05),4,plunger,.003)
    b.tube('pharmaceutical_hose',[(.04,.025,.61),(.08,-.03,.63),(.13,-.07,.67),(.16,-.13,.75),(.13,-.24,.80)],[.012]*5,5,arm,7)
    pump=p.joint('MeteringPump',(-.08,.05,.69),mast,idle=('rot',(0,0,1),.035),operation=('rot',(0,0,1),.42))
    p.loop('pump_drive_wheel',(-.08,.05,.69),.044,.008,2,pump,(0,0,1),10)
    b.box('patient_contact_cup',(.115,-.362,.82),(.065,.018,.056),5,arm,.005)
    bpy.context.view_layer.update()
    floor=min((o.matrix_world@v.co).z for o in bpy.context.scene.objects if o.type=='MESH' for v in o.data.vertices)
    for child in root.children:child.location.z-=floor
    bpy.context.view_layer.update();meshes=b.unwrap_bake('field-medic');dest=OUT/'field-medic';dest.mkdir(parents=True,exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(dest/'field-medic.blend'))
    glb=dest/'field-medic.glb';bpy.ops.export_scene.gltf(filepath=str(glb),export_format='GLB',export_animations=False,export_yup=True,export_extras=True)
    p.write_clips(glb,'field-medic')
    record=dict(id='field-medic',path=str(glb),category='candidate',faction='unraveled',role='health',sha256=b.sha(glb),triangles=sum(len(face.vertices)-2 for o in meshes for face in o.data.polygons),status='REVIEW_REQUIRED')
    (OUT/'manifest.json').write_text(json.dumps([record],indent=2)+'\n');print('MEDIC_EXPORTED',record['triangles'],flush=True)

if __name__=='__main__':build()
