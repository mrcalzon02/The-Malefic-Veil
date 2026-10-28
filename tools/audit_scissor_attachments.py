"""Check actual imported rod endpoint rings against moving axle/guide centers in all nine states."""
import bpy,json,sys,math
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1]
exec((ROOT/'tools/render_field_pilots.py').read_text().split('\nfor record in json.loads',1)[0],globals())
results=[]
for record in json.loads((SOURCE/'manifest.json').read_text()):
    if record['id']!='razorframe':continue
    path=Path(record['path']);doc,blob=read(path);bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path))
    for obj in bpy.context.scene.objects:
        if obj.animation_data:obj.animation_data_clear()
    pose(doc,blob,'ACTIVE',0);bars=[]
    for bay in range(5):
        for sign in (-1,1):
            obj=bpy.data.objects['rigid_scissor_bar_%d_%d'%(bay,sign)]
            a=bpy.data.objects['ScissorGuide_%d_%d'%(bay,-sign)];c=bpy.data.objects['ScissorGuide_%d_%d'%(bay+1,sign)]
            direction=(c.matrix_world.translation-a.matrix_world.translation).normalized();points=[obj.matrix_world@v.co for v in obj.data.vertices]
            projections=[point.dot(direction) for point in points];lo=min(projections);hi=max(projections)
            rings=[[i for i,t in enumerate(projections) if abs(t-extreme)<.0001] for extreme in (lo,hi)]
            if any(len(ring)<6 for ring in rings):raise ValueError('Missing physical endpoint ring '+obj.name)
            bars.append((obj,a,c,sign,rings))
    durations={'PACKED':1,'THROWN':.7,'IMPACT':.15,'DEPLOYING':1.5,'ACTIVE':2,'OPERATING':.65,'DAMAGED':.45,'EXPIRING':1,'DESTROYED':.8};measurements=[]
    for clip,duration in durations.items():
        error=length_error=0
        for sample in range(121):
            pose(doc,blob,clip,duration*sample/120)
            for obj,a,c,sign,rings in bars:
                centers=[]
                for ring,guide in zip(rings,(a,c)):
                    points={tuple(round(float(v),7) for v in (obj.matrix_world@obj.data.vertices[i].co)) for i in ring}
                    center=sum((Vector(p) for p in points),Vector())/len(points);expected=guide.matrix_world@Vector((0,sign*.035,0))
                    centers.append(center);error=max(error,(center-expected).length)
                length_error=max(length_error,abs((centers[1]-centers[0]).length-.42))
        measurements.append(dict(clip=clip,samples=121,max_endpoint_error_m=error,max_length_error_m=length_error))
    status='PASS_SCOPED_RIGID_LINKAGE' if max(m['max_endpoint_error_m'] for m in measurements)<.003 and max(m['max_length_error_m'] for m in measurements)<.0001 else 'FAIL'
    results.append(dict(id=record['id'],asset_sha256=sha(path),status=status,scope='Ten rigid rod endpoint rings against declared guide axle centers; 121 samples per state, including half-key interpolation times. Does not establish every part clearance or load capacity.',measurements=measurements))
    print('SCISSOR_ATTACHMENT_AUDIT',status,max(m['max_endpoint_error_m'] for m in measurements),flush=True)
(ROOT/'reports'/(REVISION+'-linkage.json')).write_text(json.dumps(results,indent=2)+'\n')
