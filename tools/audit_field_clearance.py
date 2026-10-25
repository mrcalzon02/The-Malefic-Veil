"""Declared part clearance and organic-layer attachment checks on reimported animated GLBs.
Uses evaluated morph geometry; reports the tested pairs rather than claiming full collision proof.
"""
import bpy,sys,json,fnmatch
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1]
source=(ROOT/'tools/render_field_pilots.py').read_text()
exec(source.split('\nfor record in json.loads',1)[0],globals())
contracts=json.loads((ROOT/'data/field-clearance-contracts.json').read_text())
durations={'PACKED':1,'DEPLOYING':1.5,'ACTIVE':2,'OPERATING':.65,'DAMAGED':.45,'EXPIRING':1}

def inside(tree,point):
    direction=Vector((.993,.041,.11)).normalized();origin=point.copy();hits=0
    for _ in range(256):
        location,normal,index,distance=tree.ray_cast(origin,direction)
        if location is None:return hits%2==1
        hits+=1;origin=location+direction*.00001
    raise ValueError('Ambiguous containment ray')

output=[]
for record in json.loads((SOURCE/'manifest.json').read_text()):
    contract=contracts.get(record['id'])
    if contract is None:continue
    path=Path(record['path']);doc,blob=read(path);bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path))
    for o in bpy.context.scene.objects:
        if o.animation_data:o.animation_data_clear()
        if o.type=='MESH' and o.data.shape_keys and o.data.shape_keys.animation_data:o.data.shape_keys.animation_data_clear()
    meshes=[o for o in bpy.context.scene.objects if o.type=='MESH'];rules=[]
    for rule in contract['rules']:
        selected=lambda patterns:[o for o in meshes if any(fnmatch.fnmatchcase(o.name,p) for p in patterns)]
        moving=selected(rule['moving']);obstacles=selected(rule['obstacles'])
        if not moving or not obstacles:raise ValueError('Missing declared mesh group '+rule['id'])
        rules.append((rule,moving,obstacles))
    failures=[];tested=0
    for clip,duration in durations.items():
        samples=1 if clip=='PACKED' else 61 if clip=='DEPLOYING' else 21
        for sample in range(samples):
            time=duration*sample/max(1,samples-1);pose(doc,blob,clip,time);deps=bpy.context.evaluated_depsgraph_get();trees={};vertices={}
            for o in meshes:
                evaluated=o.evaluated_get(deps);mesh=evaluated.to_mesh();vertices[o.name]=[evaluated.matrix_world@v.co for v in mesh.vertices]
                trees[o.name]=BVHTree.FromPolygons(vertices[o.name],[tuple(p.vertices) for p in mesh.polygons],all_triangles=False,epsilon=0);evaluated.to_mesh_clear()
            for rule,moving,obstacles in rules:
                for part in moving:
                    for obstacle in obstacles:
                        tested+=1;overlap=bool(trees[part.name].overlap(trees[obstacle.name]))
                        attached=overlap or rule['relation']=='attached' and inside(trees[obstacle.name],vertices[part.name][0])
                        fail=overlap if rule['relation']=='separate' else not attached
                        if fail:failures.append(dict(rule=rule['id'],clip=clip,time_s=time,part=part.name,obstacle=obstacle.name,defect='INTERSECTION' if rule['relation']=='separate' else 'DETACHED_LAYER'))
    output.append(dict(id=record['id'],asset_sha256=sha(path),tested_pair_samples=tested,scope=contract,failures=failures,status='FAIL' if failures else 'PASS_DECLARED_PAIRS'))
    print('CLEARANCE_AUDIT',record['id'],len(failures),'failures in',tested,'pair samples',flush=True)
(ROOT/'reports'/(REVISION+'-clearance.json')).write_text(json.dumps(output,indent=2)+'\n')
