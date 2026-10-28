"""Independent overlapping-plate wall mechanism study; never promotes runtime assets."""
import bpy,sys,math,json
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parent))
import refine_field_pilots as p
from bake_field_lighting import bake
b=p.b;ROOT=p.ROOT
REVISION=sys.argv[sys.argv.index('--revision')+1] if '--revision' in sys.argv else 'field-bastion-r5'
OUT=ROOT/'build/candidates'/REVISION

def build():
    bpy.ops.wm.read_factory_settings(use_empty=True);p.MOTION.clear();b.PARTS.clear();p.paint(0)
    root=p.joint('DeployRoot',(0,0,0))
    b.box('central_drive_cassette',(0,0,.025),(.46,.28,.05),0,root,.006)
    b.box('front_service_cover',(0,-.146,.030),(.29,.012,.040),1,root,.003)
    for x in (-.12,.12):p.pin('service_latch_'+str(x),(x,-.156,.030),2,root,'Y',.011)
    p.lathe('pressure_cylinder',[(0,-.04),(.025,-.04),(.025,.04),(0,.04)],2,root,6,(0,-.095,.085),'Y')
    piston=p.joint('WallPressurePiston',(0,-.15,.085),root,pack_move=(0,.04,0),operation=('move',(0,.025,0)))
    p.lathe('pressure_piston',[(0,-.03),(.012,-.03),(.012,.03),(0,.03)],1,piston,4,(0,-.15,.085),'Y')
    for c in range(5):
        x=(c-2)*.41
        span=p.joint('WallSpan_'+str(c),(x,0,0),root,pack_move=(-x,0,0));p.MOTION[span.name]['deploy_window']=(0,.35)
        p.MOTION[span.name].update(destroy_axis=(1,0,0),destroy_angle=.10*(-1 if c%2 else 1))
        stack=span
        # Nested open sleeves; each upper stage fits inside its lower sleeve.
        stage=stack
        for r in range(4):
            if r:
                stage=p.joint('WallLift_%d_%d'%(c,r),(x,0,.39*r),stage,pack_move=(0,0,-.39));p.MOTION[stage.name]['deploy_window']=(.35,.85)
                p.MOTION[stage.name]['destroy_move']=(0,0,-.25)
            z=.27+.39*r;y=.014*(4*c+r);radius=.025-.004*r;low=.07+.39*r;rear=.056*c+.078
            p.lathe('vertical_sleeve_%d_%d'%(c,r),[(radius,low),(radius,low+.43),(radius-.003,low+.43),(radius-.003,low),(radius,low)],1,stage,8,(x,rear,0))
            # Chamfered lapped plates remain rigid. Rear offsets prevent sheets occupying one plane.
            outline=[(x-.23,z-.215),(x+.23,z-.215),(x+.23,z+.195),(x+.21,z+.215),(x-.21,z+.215),(x-.23,z+.195)]
            plate=p.profile('armor_tile_%d_%d'%(c,r),outline,.012,'Y',1,stage);plate.location.y+=y
            b.box('pressed_edge_%d_%d'%(c,r),(x,y-.009,z-.175),(.40,.008,.012),0,stage,0)
            p.lathe('retaining_bolt_%d_%d'%(c,r),[(0,-.012),(.012,-.012),(.012,.012),(0,.012)],2,stage,4,(x,y-.012,z-.13),'Y')
            b.strut('plate_mount_%d_%d'%(c,r),(x,y+.006,z-.13),(x,rear,z-.13),.010,1,stage)
        # Rear slide bars expose the actual lateral travel; drive/load-path review remains pending.
        b.box('lateral_slide_'+str(c),(x,.056*c+.078,.085),(.46,.045,.045),1,stack,.002)
    for side in (-1,1):
        for front in (-1,1):
            foot=p.joint('GroundShoe_%d_%d'%(side,front),(side*.19,0,.09),root,pack_axis=(1,0,0),pack_angle=front*1.48)
            b.tube('outrigger_%d_%d'%(side,front),[(side*.19,0,.09),(side*.19,front*.22,.07),(side*.19,front*.38,.025)],[.025,.024,.020],1,foot,6)
            p.pin('ground_hinge_%d_%d'%(side,front),(side*.19,0,.09),2,foot,'X',.023)
    meshes=b.unwrap_bake('bastion-panel');bake(meshes,b.MATS,'bastion-panel')
    dest=OUT/'bastion-panel';dest.mkdir(parents=True,exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(dest/'bastion-panel.blend'))
    root.rotation_euler[2]=math.pi;bpy.context.view_layer.update();glb=dest/'bastion-panel.glb'
    bpy.ops.export_scene.gltf(filepath=str(glb),export_format='GLB',export_animations=False,export_yup=True,export_extras=True,export_normals=False,export_morph_normal=False)
    p.write_clips(glb,'bastion-panel')
    record=dict(id='bastion-panel',path=str(glb),category='candidate',faction='unraveled',role='wall',sha256=b.sha(glb),triangles=sum(len(face.vertices)-2 for o in meshes for face in o.data.polygons),status='MECHANISM_STUDY_NOT_CERTIFIED',remaining=['Lateral slide nesting and load-path redesign/verification','Continuous cover silhouette and collider match','Full articulation and part-clearance audit','Purposeful operating motion, failure-specific destruction and persistent damage','Purpose/art acceptance and live-world integration'])
    (OUT/'manifest.json').write_text(json.dumps([record],indent=2)+'\n');print('BASTION_EXPORTED',record['triangles'],flush=True)

if __name__=='__main__':build()
