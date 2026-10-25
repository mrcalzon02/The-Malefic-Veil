"""Sample exported pilot motion for specific unintended vessel/frame intersections.
This deliberately excludes intended chain/band attachment contacts. Not a full collision proof.
"""
import bpy,json,sys,hashlib
from pathlib import Path
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1]
# Reuse the exact exported-track evaluator used for render evidence; do not run its render loop.
source=(ROOT/'tools/render_field_pilots.py').read_text()
exec(source.split('\nfor record in json.loads',1)[0],globals())
revision=ROOT/'build/candidates'/REVISION
results=[]
for record in json.loads((revision/'manifest.json').read_text()):
    if record['id']!='censer-of-dread':continue
    path=Path(record['path']);doc,blob=read(path)
    bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path))
    for o in bpy.context.scene.objects:
        if o.animation_data:o.animation_data_clear()
    failures=[]
    for sample in range(31):
        time=sample/20.;pose(doc,blob,'DEPLOYING',time)
        bowls=[o for o in bpy.context.scene.objects if o.type=='MESH' and o.name.startswith('satellite_bowl')]
        obstacles=[o for o in bpy.context.scene.objects if o.type=='MESH' and (o.name in ('bronze_censer_bowl','inner_incense_basket') or o.name.startswith(('outer_iron_channel','middle_iron_slide','inner_iron_slide')))]
        if not bowls or not obstacles:raise ValueError('Missing declared Censer vessel/support groups')
        trees={o.name:BVHTree.FromPolygons([o.matrix_world@v.co for v in o.data.vertices],[tuple(p.vertices) for p in o.data.polygons],all_triangles=False,epsilon=0) for o in bowls+obstacles}
        for bowl in bowls:
            for obstacle in obstacles:
                if trees[bowl.name].overlap(trees[obstacle.name]):failures.append(dict(time_s=time,vessel=bowl.name,obstacle=obstacle.name))
    results.append(dict(id=record['id'],asset_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),samples=31,scope='Satellite bowl surfaces against main bowl, incense basket, and support slides',intersections=failures,status='INTERSECTIONS_FOUND' if failures else 'NO_INTERSECTIONS_IN_SAMPLED_PAIRS'))
    print('MOTION_INSPECTION',record['id'],len(failures),flush=True)
(ROOT/'reports'/(REVISION+'-motion-inspection.json')).write_text(json.dumps(results,indent=2)+'\n')
