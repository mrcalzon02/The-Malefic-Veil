"""Measure exact imported PACKED meshes; expose which parts set each envelope face."""
import bpy,json,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
exec((ROOT/'tools/render_field_pilots.py').read_text().split('\nfor record in json.loads',1)[0],globals())
results=[]
for record in json.loads((SOURCE/'manifest.json').read_text()):
    path=Path(record['path']);doc,blob=read(path)
    bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path))
    for obj in bpy.context.scene.objects:
        if obj.animation_data:obj.animation_data_clear()
        if obj.type=='MESH' and obj.data.shape_keys and obj.data.shape_keys.animation_data:obj.data.shape_keys.animation_data_clear()
    pose(doc,blob,'PACKED',0);deps=bpy.context.evaluated_depsgraph_get();parts=[]
    for obj in bpy.context.scene.objects:
        if obj.type!='MESH':continue
        evaluated=obj.evaluated_get(deps);mesh=evaluated.to_mesh();points=[evaluated.matrix_world@v.co for v in mesh.vertices]
        parts.append(dict(name=obj.name,min=[min(p[k] for p in points) for k in range(3)],max=[max(p[k] for p in points) for k in range(3)]));evaluated.to_mesh_clear()
    lo=[min(p['min'][k] for p in parts) for k in range(3)];hi=[max(p['max'][k] for p in parts) for k in range(3)]
    faces={}
    for k,axis in enumerate(('X','Y','Z')):
        for side in ('min','max'):faces[axis+'_'+side]=sorted(parts,key=lambda p:p[side][k],reverse=side=='max')[:3]
    results.append(dict(id=record['id'],asset_sha256=sha(path),coordinate_space='Blender reimport +Z up',size_m=[hi[k]-lo[k] for k in range(3)],envelope_faces=faces))
    print('PACKED_ENVELOPE',record['id'],results[-1]['size_m'],flush=True)
(ROOT/'reports'/(REVISION+'-packing.json')).write_text(json.dumps(results,indent=2)+'\n')
