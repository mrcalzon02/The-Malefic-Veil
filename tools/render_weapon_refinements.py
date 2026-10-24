"""Render exported/reimported weapon candidates with full framing and reload evidence."""
import bpy,json,sys,math,hashlib
from pathlib import Path
from mathutils import Vector
from bpy_extras.object_utils import world_to_camera_view
R=Path(__file__).resolve().parents[1]
records=json.loads((R/'build/candidates/weapon-refinement/inspection-input.json').read_text())
if '--id' in sys.argv:
    ident=sys.argv[sys.argv.index('--id')+1]
    records=[r for r in json.loads((R/'build/candidates/weapon-refinement'/ident/'inspection-input.json').read_text()) if r['id']==ident]
    if len(records)!=1:raise ValueError('Expected exactly one candidate: '+ident)
for record in records:
    path=R/record['path'];pre=R/'previews/weapon-refinement'/record['id'];pre.mkdir(parents=True,exist_ok=True)
    bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path))
    scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=10;scene.cycles.use_denoising=True
    scene.render.resolution_x=896;scene.render.resolution_y=640;scene.render.resolution_percentage=100;scene.view_settings.view_transform='AgX'
    scene.world=bpy.data.worlds.new('Weapon review');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[0].default_value=(.055,.070,.09,1);scene.world.node_tree.nodes['Background'].inputs[1].default_value=.45
    objects=[o for o in scene.objects if o.type=='MESH']
    def clip(label):
        found=0
        for o in scene.objects:
            if not o.animation_data:continue
            o.animation_data.action=None
            for tr in o.animation_data.nla_tracks:
                active=label in tr.name.lower();tr.mute=not active;found+=active
        if not found:raise RuntimeError('No reimported clip '+label+' '+record['id'])
        scene.frame_set(30 if label=='reload' else 0);bpy.context.view_layer.update()
    def points():return [o.matrix_world@v.co for o in objects for v in o.data.vertices]
    clip('idle');pts=points();low=Vector([min(v[i] for v in pts) for i in range(3)]);high=Vector([max(v[i] for v in pts) for i in range(3)]);center=(low+high)/2;span=max(high-low)
    for direction,power in [((1,-2,3),170),((-2,-1,1.5),105),((1,2,3),210)]:
        bpy.ops.object.light_add(type='AREA',location=center+Vector(direction)*span);o=bpy.context.object;o.data.energy=power*span*span;o.data.size=span*1.7;o.rotation_euler=(center-o.location).to_track_quat('-Z','Y').to_euler()
    bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';scene.camera=cam
    clay=bpy.data.materials.new('Inspection clay');clay.use_nodes=True;clay.node_tree.nodes['Principled BSDF'].inputs['Base Color'].default_value=(.30,.34,.35,1);clay.node_tree.nodes['Principled BSDF'].inputs['Roughness'].default_value=.8
    evidence={}
    for view,direction in [('front',(1,-2,.8)),('rear',(-1,2,.7)),('closeup',(.5,-2,.35)),('clay',(1,-2,.8)),('side',(0,-3,0)),('reload',(1,-2,.8))]:
        clip('reload' if view=='reload' else 'idle');pts=points();lo=Vector([min(v[i] for v in pts) for i in range(3)]);hi=Vector([max(v[i] for v in pts) for i in range(3)]);target=(lo+hi)/2;s=max(hi-lo)
        scene.view_layers[0].material_override=clay if view in('clay','reload','side') else None
        if view=='closeup':target=Vector((-.035,0,.02))
        cam.location=target+Vector(direction)*s*2;cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.ortho_scale=s*(.72 if view=='closeup' else 1.4)
        if view!='closeup':
            bpy.context.view_layer.update();p=[world_to_camera_view(scene,cam,v) for v in pts];extent=max(max(abs(v.x-.5),abs(v.y-.5))*2 for v in p);cam.data.ortho_scale*=extent*1.12
            bpy.context.view_layer.update();assert all(.001<p.x<.999 and .001<p.y<.999 for p in [world_to_camera_view(scene,cam,v) for v in pts]),'Exported geometry cropped: '+record['id']+' '+view
        image=pre/(view+'.png');scene.render.filepath=str(image);bpy.ops.render.render(write_still=True)
        evidence[view]={'path':image.relative_to(R).as_posix(),'sha256':hashlib.sha256(image.read_bytes()).hexdigest()}
    (path.parent/'render-evidence.json').write_text(json.dumps({'asset_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'renders':evidence},indent=2)+'\n')
    print('RENDERED_WEAPON',record['id'],flush=True)
