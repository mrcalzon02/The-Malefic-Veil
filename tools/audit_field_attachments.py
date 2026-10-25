"""Measure exported hose-to-socket continuity through every animation state.
Runs in Blender. Evaluates actual imported morphs and node transforms, not build metadata.
"""
import bpy,json,sys,math
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1]
source=(ROOT/'tools/render_field_pilots.py').read_text()
exec(source.split('\nfor record in json.loads',1)[0],globals())
results=[]
for record in json.loads((SOURCE/'manifest.json').read_text()):
    path=Path(record['path']);doc,blob=read(path)
    bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path))
    for o in bpy.context.scene.objects:
        if o.animation_data:o.animation_data_clear()
        if o.type=='MESH' and o.data.shape_keys and o.data.shape_keys.animation_data:o.data.shape_keys.animation_data_clear()
    hose=bpy.data.objects.get('PharmaceuticalHose')
    if not hose:continue
    sockets=[bpy.data.objects['HoseCaseSocket'],bpy.data.objects['HoseInjectorSocket']]
    pose(doc,blob,'ACTIVE',0);deps=bpy.context.evaluated_depsgraph_get();mesh=hose.evaluated_get(deps).to_mesh()
    rings=[]
    for socket in sockets:
        center=socket.matrix_world.translation
        ring=[v.index for v in mesh.vertices if abs((hose.matrix_world@v.co-center).length-.01)<.0001]
        if len(ring)<6:raise ValueError('Missing attachment ring at '+socket.name)
        rings.append(ring)
    hose.evaluated_get(deps).to_mesh_clear()
    measurements=[]
    durations={'ACTIVE':2,'PACKED':1,'THROWN':.7,'IMPACT':.15,'DEPLOYING':1.5,'OPERATING':.65,'DAMAGED':.45,'EXPIRING':1,'DESTROYED':.8}
    for clip,duration in durations.items():
        maximum=[0.,0.]
        for sample in range(61):
            time=duration*sample/60;pose(doc,blob,clip,time);deps=bpy.context.evaluated_depsgraph_get();evaluated=hose.evaluated_get(deps);mesh=evaluated.to_mesh()
            for port,(socket,indices) in enumerate(zip(sockets,rings)):
                # Weld UV/normal splits before measuring the ring center.
                points={tuple(round(float(v),7) for v in (evaluated.matrix_world@mesh.vertices[i].co)) for i in indices}
                center=sum((Vector(point) for point in points),Vector())/len(points)
                maximum[port]=max(maximum[port],(center-socket.matrix_world.translation).length)
            evaluated.to_mesh_clear()
        measurements.append(dict(clip=clip,samples=61,case_error_m=maximum[0],injector_error_m=maximum[1]))
    failure=max(max(m['case_error_m'],m['injector_error_m']) for m in measurements)>.004
    results.append(dict(id=record['id'],asset_sha256=sha(path),socket_tolerance_m=.004,measurements=measurements,status='FAIL' if failure else 'PASS_SCOPED_SOCKET_CONTINUITY'))
    print('SOCKET_AUDIT',record['id'],results[-1]['status'],flush=True)
(ROOT/'reports'/(REVISION+'-attachments.json')).write_text(json.dumps(results,indent=2)+'\n')
