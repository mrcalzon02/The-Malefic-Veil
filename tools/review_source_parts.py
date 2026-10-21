import bpy,math
from pathlib import Path
from mathutils import Vector,Matrix
ROOT=Path(__file__).resolve().parents[1]
bpy.ops.wm.read_factory_settings(use_empty=True)
scene=bpy.context.scene;scene.world=bpy.data.worlds.new('Studio');scene.world.color=(.1,.1,.1)
scene.render.engine='CYCLES';scene.cycles.samples=24;scene.render.resolution_x=1200;scene.render.resolution_y=900;scene.render.resolution_percentage=100
m=bpy.data.materials.new('Full-detail source metal');m.use_nodes=True;bs=m.node_tree.nodes['Principled BSDF'];bs.inputs['Base Color'].default_value=(.22,.28,.32,1);bs.inputs['Metallic'].default_value=.6;bs.inputs['Roughness'].default_value=.4
for loc,energy in[((1,-2,3),160),((-1,2,2),220)]:
    bpy.ops.object.light_add(type='AREA',location=loc);o=bpy.context.object;o.data.energy=energy;o.data.size=3;o.rotation_euler=(-o.location).to_track_quat('-Z','Y').to_euler()
bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;cam.data.type='ORTHO';cam.data.ortho_scale=1.65
for pid in ['G058','G159','G132','G131','G238','G085']:
    with bpy.data.libraries.load(str(ROOT/'assets/private/arsenal/normalized_parts.blend'),link=False) as(s,d):d.objects=[pid]
    o=d.objects[0];scene.collection.objects.link(o);o.data.materials.clear();o.data.materials.append(m)
    for view,loc in [('angle',(1.2,-2.8,1.2)),('side',(0,-3,0))]:
        cam.location=loc;cam.rotation_euler=(-cam.location).to_track_quat('-Z','Y').to_euler();scene.render.filepath=str(ROOT/'previews'/f'source-{pid}-{view}.png');bpy.ops.render.render(write_still=True)
    bpy.data.objects.remove(o,do_unlink=True)
