"""Independent porous barricade with a five-bay rigid scissor linkage and sliding guides."""
import bpy,sys,math,json
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parent))
import refine_field_pilots as p
from bake_field_lighting import bake
from field_scissor_tracks import LENGTH,OPEN,BAYS
b=p.b;ROOT=p.ROOT
REVISION=sys.argv[sys.argv.index('--revision')+1] if '--revision' in sys.argv else 'field-razorframe-r3'
OUT=ROOT/'build/candidates'/REVISION

def link(name,loc,kind,index,**extra):
    obj=p.joint(name,loc,bpy.data.objects['DeployRoot']);spec=dict(kind=kind,index=index,**extra)
    p.MOTION[name]['scissor']=spec;obj['scissor']=spec;return obj

def build():
    bpy.ops.wm.read_factory_settings(use_empty=True);p.MOTION.clear();b.PARTS.clear();p.paint(0)
    root=p.joint('DeployRoot',(0,0,0));height=math.sqrt(LENGTH*LENGTH-OPEN*OPEN)
    b.box('release_drive_cassette',(0,.20,.03),(.30,.18,.06),0,root,.005)
    b.box('cassette_access_plate',(0,.105,.032),(.18,.012,.035),1,root,.002)
    piston=p.joint('BracePressurePiston',(0,.105,.085),root,operation=('move',(0,-.02,0)))
    p.lathe('brace_pressure_rod',[(0,-.035),(.015,-.035),(.015,.035),(0,.035)],2,piston,6,(0,.105,.085),'Y')
    for end in range(BAYS+1):
        x=(end-BAYS/2)*OPEN;post=link('ScissorPost_'+str(end),(x,0,.04),'post',end)
        for upper,(radius,z) in enumerate(((.018,.035),(.012,.43))):
            parent=p.joint('PostTop_'+str(end),(x,0,.43),post,pack_move=(0,0,-.395)) if upper else post
            length=.43 if upper else .40
            p.lathe('guide_post_%d_%d'%(end,upper),[(radius,z),(radius,z+length),(radius-.003,z+length),(radius-.003,z),(radius,z)],1,parent,6,(x,0,0))
        for level in (-1,1):
            z=.42+level*height/2;guide=link('ScissorGuide_%d_%d'%(end,level),(x,0,z),'guide',end,level=level)
            outer,inner=(.022,.020) if level<0 else (.016,.014)
            p.lathe('sliding_guide_%d_%d'%(end,level),[(outer,z-.014),(outer,z+.014),(inner,z+.014),(inner,z-.014),(outer,z-.014)],0,guide,8,(x,0,0))
            p.lathe('guide_axle_%d_%d'%(end,level),[(0,-.05),(.010,-.05),(.010,.05),(0,.05)],2,guide,6,(x,0,z),'Y')
        if end in (0,BAYS):
            for side in (-1,1):
                foot=p.joint('EndGroundShoe_%d_%d'%(end,side),(x,0,.065),post,pack_axis=(1,0,0),pack_angle=side*1.48)
                b.tube('ground_claw_%d_%d'%(end,side),[(x,0,.065),(x,side*.16,.05),(x,side*.27,.022)],[.022,.020,.016],0,foot,6)
    for bay in range(BAYS):
        x=(bay-(BAYS-1)/2)*OPEN
        for sign in (-1,1):
            y=sign*.035;bar=link('ScissorBar_%d_%d'%(bay,sign),(x,y,.42),'bar',bay,sign=sign)
            b.tube('rigid_scissor_bar_%d_%d'%(bay,sign),[(x-OPEN/2,y,.42-sign*height/2),(x+OPEN/2,y,.42+sign*height/2)],[.012,.012],1,bar,6)
        axle=link('ScissorCenter_'+str(bay),(x,0,.42),'center',bay)
        p.lathe('center_axle_'+str(bay),[(0,-.052),(.013,-.052),(.013,.052),(0,.052)],2,axle,6,(x,0,.42),'Y')
        rail=link('ScissorRail_'+str(bay),(x,.14+bay*.025,.84),'rail',bay)
        b.box('overlap_top_rail_'+str(bay),(x,.14+bay*.025,.84),(.32,.020,.025),0,rail,.002)
        for k in (-1,1):
            p.profile('raised_warning_tooth_%d_%d'%(bay,k),[(x+k*.11-.022,.852),(x+k*.11+.022,.852),(x+k*.11,.885)],.012,'Y',2,rail).location.y+=.14+bay*.025
    meshes=b.unwrap_bake('razorframe');bake(meshes,b.MATS,'razorframe');dest=OUT/'razorframe';dest.mkdir(parents=True,exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(dest/'razorframe.blend'));root.rotation_euler[2]=math.pi;bpy.context.view_layer.update();glb=dest/'razorframe.glb'
    bpy.ops.export_scene.gltf(filepath=str(glb),export_format='GLB',export_yup=True,export_animations=False,export_extras=True,export_normals=False,export_morph_normal=False)
    p.write_clips(glb,'razorframe')
    record=dict(id='razorframe',path=str(glb),category='candidate',faction='unraveled',role='barricade',sha256=b.sha(glb),triangles=sum(len(face.vertices)-2 for o in meshes for face in o.data.polygons),status='MECHANISM_STUDY_REVIEW_REQUIRED',remaining=['Full part clearance, guide contact and load-path audit','Drive transmission and top-rail supports/nesting review','Distinct destruction/aging and material refinement','Collider match and live-world purpose/art acceptance'])
    (OUT/'manifest.json').write_text(json.dumps([record],indent=2)+'\n');print('RAZORFRAME_EXPORTED',record['triangles'],flush=True)

if __name__=='__main__':build()
