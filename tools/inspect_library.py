import bpy, json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
sources = [Path(r'C:\Users\Admin\Documents\Models\scifi-building-dressing-kitbash-pack\blend_kitbash.blend'), Path(r'C:\Users\Admin\Documents\Models\Greebles.blend')]
report = []
for p in sources:
    with bpy.data.libraries.load(str(p), link=False) as (src, dst):
        names = list(src.objects)
    report.append({'path': str(p), 'bytes': p.stat().st_size, 'objects': names})
    print(p.name, len(names), names[:30], flush=True)
(ROOT/'reports/library_index.json').write_text(json.dumps(report, indent=2))
bpy.ops.wm.read_factory_settings(use_empty=True)
with bpy.data.libraries.load(str(sources[0]), link=False) as (src,dst):
    dst.objects = list(src.objects)
parts=[]
for obj in dst.objects:
    if obj and obj.type == 'MESH':
        bpy.context.collection.objects.link(obj)
        obj.data.calc_loop_triangles()
        parts.append({'name':obj.name,'vertices':len(obj.data.vertices),'triangles':len(obj.data.loop_triangles),'bounds':[list(v) for v in obj.bound_box], 'dimensions':list(obj.dimensions)})
(ROOT/'reports/parts_inventory.json').write_text(json.dumps(parts,indent=2))
print('INVENTORY',len(parts),flush=True)
bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'assets/source_working.blend'),compress=True)
