"""Reimport pilot GLBs and render their actual exported TRS/morph tracks."""
import bpy,sys,json,struct,math,hashlib
from pathlib import Path
from mathutils import Vector,Quaternion
ROOT=Path(__file__).resolve().parents[1]
REVISION=sys.argv[sys.argv.index('--revision')+1] if '--revision' in sys.argv else 'field-pilots-r4'
SOURCE=ROOT/'build/candidates'/REVISION
DEST=ROOT/'previews'/REVISION

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def read(path):
    file=path.read_bytes();size=struct.unpack_from('<I',file,12)[0];doc=json.loads(file[20:20+size]);offset=20+size;n=struct.unpack_from('<I',file,offset)[0];return doc,file[offset+8:offset+8+n]
def accessor(doc,blob,id):
    a=doc['accessors'][id];v=doc['bufferViews'][a['bufferView']];width={'SCALAR':1,'VEC3':3,'VEC4':4}[a['type']];start=v.get('byteOffset',0)+a.get('byteOffset',0);stride=v.get('byteStride',width*4)
    assert a['componentType']==5126 and 'sparse' not in a
    return [list(struct.unpack_from('<'+'f'*width,blob,start+i*stride)) for i in range(a['count'])]
def pose(doc,blob,clip,time):
    channels={};animation=next(a for a in doc['animations'] if a['name']==clip)
    for channel in animation['channels']:
        target=channel['target'];sampler=animation['samplers'][channel['sampler']];times=accessor(doc,blob,sampler['input']);values=accessor(doc,blob,sampler['output']);path=target['path'];node=target['node']
        if path=='weights':
            count=len(doc['meshes'][doc['nodes'][node]['mesh']]['primitives'][0]['targets']);values=[[values[i*count+j][0] for j in range(count)] for i in range(len(times))]
        index=0
        while index+1<len(times) and times[index+1][0]<=time:index+=1
        nxt=min(index+1,len(times)-1);span=times[nxt][0]-times[index][0];alpha=0 if span<=0 else max(0,min(1,(time-times[index][0])/span))
        if path=='rotation':
            a=values[index];c=values[nxt];q=Quaternion((a[3],a[0],a[1],a[2])).slerp(Quaternion((c[3],c[0],c[1],c[2])),alpha);value=[q.x,q.y,q.z,q.w]
        else:value=[a+(c-a)*alpha for a,c in zip(values[index],values[nxt])]
        channels[node,path]=value
    convert=Quaternion((1,0,0),math.pi/2)
    for i,node in enumerate(doc['nodes']):
        o=bpy.data.objects.get(node.get('name',''))
        if not o:raise ValueError('Imported node missing '+str(node.get('name')))
        t=channels.get((i,'translation'),node.get('translation',[0,0,0]));q=channels.get((i,'rotation'),node.get('rotation',[0,0,0,1]));s=channels.get((i,'scale'),node.get('scale',[1,1,1]))
        o.location=(t[0],-t[2],t[1]);o.rotation_mode='QUATERNION';o.rotation_quaternion=convert@Quaternion((q[3],q[0],q[1],q[2]))@convert.inverted();o.scale=(s[0],s[2],s[1])
        if o.type=='MESH' and o.data.shape_keys:
            weights=channels.get((i,'weights'),node.get('weights',doc['meshes'][node['mesh']].get('weights',[])))
            for key,value in zip(list(o.data.shape_keys.key_blocks)[1:],weights):key.value=value
    bpy.context.view_layer.update()

for record in json.loads((SOURCE/'manifest.json').read_text()):
    file=Path(record['path']);doc,blob=read(file);bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(file),import_pack_images=True)
    scene=bpy.context.scene
    for o in scene.objects:
        if o.animation_data:o.animation_data_clear()
        if o.type=='MESH' and o.data.shape_keys and o.data.shape_keys.animation_data:o.data.shape_keys.animation_data_clear()
    scene.render.engine='CYCLES';scene.cycles.samples=12;scene.render.resolution_percentage=100
    scene.world=bpy.data.worlds.new('review_studio');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[0].default_value=(.11,.13,.16,1);scene.world.node_tree.nodes['Background'].inputs[1].default_value=.55
    canonical=doc.get('asset',{}).get('extras',{}).get('runtime_front')=='-Z'
    direction=-1 if canonical else 1
    target=Vector((0,0,.57))
    for loc,power,size in [((2,-3,4),420,3),((-3,-1,2),230,3),((1,3,3),350,2)]:
        bpy.ops.object.light_add(type='AREA',location=(loc[0]*direction,loc[1]*direction,loc[2]));o=bpy.context.object;o.data.energy=power;o.data.shape='DISK';o.data.size=size;o.rotation_euler=(target-o.location).to_track_quat('-Z','Y').to_euler()
    bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;cam.data.type='ORTHO';cam.data.ortho_scale=1.55
    # A grounded studio receiver, excluded from the exported asset.
    bpy.ops.mesh.primitive_plane_add(size=200);floor=bpy.context.object;floor.name='render_only_ground';floor.location.z=-.004
    mat=bpy.data.materials.new('studio_floor');mat.diffuse_color=(.075,.09,.11,1);floor.data.materials.append(mat)
    dest=DEST/record['id'];dest.mkdir(parents=True,exist_ok=True);evidence=dict(asset_sha256=sha(file),renders={},sequences={})
    clay=bpy.data.materials.new('clay');clay.use_nodes=True;clay.node_tree.nodes.get('Principled BSDF').inputs['Base Color'].default_value=(.30,.30,.30,1);clay.node_tree.nodes.get('Principled BSDF').inputs['Roughness'].default_value=.85
    pose(doc,blob,'ACTIVE',0)
    for view,loc in [('front',(2.4,-3.5,1.9)),('rear',(-2.4,3.5,1.9)),('closeup',(1.6,-3.0,1.75)),('clay',(2.4,-3.5,1.9))]:
        cam.location=(loc[0]*direction,loc[1]*direction,loc[2]);cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.ortho_scale=1.4 if view=='closeup' else 1.6
        scene.render.resolution_x=640;scene.render.resolution_y=640;scene.view_layers[0].material_override=clay if view=='clay' else None
        p=dest/(view+'.png');scene.render.filepath=str(p);bpy.ops.render.render(write_still=True);evidence['renders'][view]=dict(path=str(p),sha256=sha(p))
    scene.view_layers[0].material_override=None;scene.cycles.samples=5;scene.render.resolution_x=320;scene.render.resolution_y=320
    for clip,duration,frames in [('DEPLOYING',1.5,10),('OPERATING',.65,10),('PACKED',1,1)]:
        sequence=[];cam.data.ortho_scale=1.6
        for frame in range(frames):
            time=duration*frame/max(1,frames-1);pose(doc,blob,clip,time);p=dest/('%s-%02d.png'%(clip.lower(),frame));scene.render.filepath=str(p);bpy.ops.render.render(write_still=True);sequence.append(dict(path=str(p),time_s=time,sha256=sha(p)))
        evidence['sequences'][clip]=sequence
    (file.parent/'render-evidence.json').write_text(json.dumps(evidence,indent=2)+'\n')
    print('PILOT_RENDERED',record['id'],flush=True)
