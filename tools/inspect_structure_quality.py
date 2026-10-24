"""Blender-side independent asset inspection; imports the exported deliverable.

Run blender -b --python tools/inspect_structure_quality.py -- input.json output.json
Input is a list of {id, path, category} records. This never modifies source assets.
"""
import bpy
import json
import math
import sys
import hashlib
from pathlib import Path
from collections import defaultdict
from mathutils import Vector

def property_value(value):
    if hasattr(value,'to_list'):return value.to_list()
    if hasattr(value,'to_dict'):return {k:property_value(v) for k,v in value.items()}
    return value

def inspect(record):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    path=Path(record['path']).resolve()
    if path.suffix.lower()=='.glb':
        bpy.ops.import_scene.gltf(filepath=str(path))
    elif path.suffix.lower()=='.fbx':
        bpy.ops.import_scene.fbx(filepath=str(path),use_image_search=True)
    elif path.suffix.lower()=='.obj':
        bpy.ops.wm.obj_import(filepath=str(path))
    else:
        raise ValueError('Unsupported asset '+str(path))
    # FBX references may use old absolute texture locations. Relink only exact
    # filenames within the same private reference archive, never arbitrary images.
    for image in bpy.data.images:
        if image.source=='FILE' and not image.has_data:
            stem=Path(image.filepath.replace('\\','/')).name
            base=path
            while base.parent!=base and base.name not in ('characters','cars'):
                base=base.parent
            matches=list(base.rglob(stem)) if base.name in ('characters','cars') else []
            if len(matches)==1:
                image.filepath=str(matches[0])
                image.reload()
        # Blender loads decoded pixels lazily, even when paths already resolve.
        if image.source=='FILE':
            try:
                _=image.size[0]
                if len(image.pixels):_=image.pixels[0]
            except (RuntimeError, OSError):
                pass
    mesh_records=[]
    for obj in bpy.context.scene.objects:
        if obj.type!='MESH':continue
        mesh=obj.data
        mesh.calc_loop_triangles()
        pts=[obj.matrix_world@v.co for v in mesh.vertices]
        # Weld glTF UV/material/normal splits by spatial position. Merely joining
        # object names does NOT merge overlapping spheroids into a surface.
        keys={}
        weld=[]
        for p in pts:
            key=tuple(round(float(v),5) for v in p)
            if key not in keys:keys[key]=len(keys)
            weld.append(keys[key])
        # Surface connectivity is through shared EDGES. Touching at a single
        # vertex must not turn two intersecting primitive shells into one body.
        parent=list(range(len(mesh.loop_triangles)))
        def find(a):
            while parent[a]!=a:
                parent[a]=parent[parent[a]]
                a=parent[a]
            return a
        def union(a,b):
            a,b=find(a),find(b)
            if a!=b:parent[b]=a
        areas=[]
        edges=defaultdict(list)
        edge_faces=defaultdict(list)
        invalid=0
        textured=0
        uv_area=0
        total_area=0
        degenerate=0
        sharp=0
        valid_uv=mesh.uv_layers.active
        corners=mesh.corner_normals
        for tri_index,tri in enumerate(mesh.loop_triangles):
            ix=list(tri.vertices)
            a,b,c=[pts[i] for i in ix]
            area=(b-a).cross(c-a).length/2
            if area<1e-10:degenerate+=1
            if not all(math.isfinite(x) for p in (a,b,c) for x in p):invalid+=1
            total_area+=area
            ids=[weld[i] for i in ix]
            areas.append((tri_index,area))
            for k in range(3):
                e=tuple(sorted((ids[k],ids[(k+1)%3])))
                n=[corners[tri.loops[j]].vector.copy() for j in (k,(k+1)%3)]
                if ids[k]>ids[(k+1)%3]:n.reverse()
                edges[e].append(n)
                edge_faces[e].append(tri_index)
            if valid_uv:
                uvs=[valid_uv.data[i].uv for i in tri.loops]
                uvtri=abs((uvs[1].x-uvs[0].x)*(uvs[2].y-uvs[0].y)-(uvs[2].x-uvs[0].x)*(uvs[1].y-uvs[0].y))
                if uvtri>1e-12 and all(math.isfinite(v) for uv in uvs for v in uv):uv_area+=area
            mat=mesh.materials[tri.material_index] if mesh.materials else None
            if mat and mat.use_nodes:
                shader=next((n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED'),None)
                if shader:
                    visited=set()
                    def has_image(socket):
                        for link in socket.links:
                            n=link.from_node
                            if n in visited:continue
                            visited.add(n)
                            if n.type=='TEX_IMAGE' and n.image and n.image.has_data:return True
                            if any(has_image(s) for s in n.inputs if s.is_linked):return True
                        return False
                    if has_image(shader.inputs['Base Color']):textured+=area
        comp=defaultdict(float)
        for faces in edge_faces.values():
            for f in faces[1:]:union(faces[0],f)
        for i,a in areas:comp[find(i)]+=a
        component_points=defaultdict(list)
        for i,tri in enumerate(mesh.loop_triangles):
            component_points[find(i)].extend(pts[j] for j in tri.vertices)
        for pairs in edges.values():
            if len(pairs)==2 and any(pairs[0][i].dot(pairs[1][i])<math.cos(math.radians(12)) for i in (0,1)):
                sharp+=1
        measured_uv=[list(loop.uv) for loop in mesh.uv_layers.active.data] if mesh.uv_layers.active else []
        mesh_records.append(dict(name=obj.name,triangles=len(areas),vertices=len(mesh.vertices),
            uv_bounds=[[min(v[i] for v in measured_uv) for i in range(2)],[max(v[i] for v in measured_uv) for i in range(2)]] if measured_uv else None,
            welded_vertices=len(keys),components=len(comp),
            largest_component_area_fraction=max(comp.values(),default=0)/max(total_area,1e-10),
            component_details=[dict(area=area,bounds=[[min(p[i] for p in component_points[key]) for i in range(3)],[max(p[i] for p in component_points[key]) for i in range(3)]]) for key,area in sorted(comp.items(),key=lambda kv:-kv[1])[:30]],
            surface_area=total_area,uv_coverage=uv_area/max(total_area,1e-10),
            textured_fraction=textured/max(total_area,1e-10),
            boundary_edges=sum(len(v)==1 for v in edges.values()),
            nonmanifold_edges=sum(len(v)>2 for v in edges.values()),
            normal_seam_fraction=sharp/max(1,len(edges)),
            smooth_face_fraction=sum(p.use_smooth for p in mesh.polygons)/max(1,len(mesh.polygons)),
            degenerate_triangles=degenerate,nonfinite_triangles=invalid,
            bounds=[[min(p[i] for p in pts) for i in range(3)],[max(p[i] for p in pts) for i in range(3)]],
            properties={k:property_value(v) for k,v in obj.items()}))
    images=[dict(name=i.name,width=i.size[0],height=i.size[1],loaded=i.has_data,
        packed=bool(i.packed_file)) for i in bpy.data.images if i.type=='IMAGE']
    total=sum(m['surface_area'] for m in mesh_records)
    return dict(**record,sha256=hashlib.sha256(path.read_bytes()).hexdigest(),meshes=mesh_records,images=images,
        triangles=sum(m['triangles'] for m in mesh_records),
        uv_coverage=sum(m['uv_coverage']*m['surface_area'] for m in mesh_records)/max(total,1e-10),
        textured_fraction=sum(m['textured_fraction']*m['surface_area'] for m in mesh_records)/max(total,1e-10))

if __name__=='__main__':
    args=sys.argv[sys.argv.index('--')+1:]
    output=[]
    for record in json.loads(Path(args[0]).read_text()):
        try:
            output.append(inspect(record))
        except Exception as exc:
            output.append(dict(**record,inspection_error=str(exc)))
        print('INSPECTED',record['id'],flush=True)
    Path(args[1]).write_text(json.dumps(output,indent=2,default=str)+'\n')
