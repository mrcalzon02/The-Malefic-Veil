import sys,json,zipfile,hashlib
from pathlib import Path
ROOT=Path(r'C:/Users/Admin/Documents/ChatGPT/The Malefic Veil');sys.path.insert(0,str(ROOT/'tools'))
import bpy
from mathutils import Vector
from structure_art import reset,render_export
out=ROOT/'build/candidates/city-interiors-calibration';out.mkdir(parents=True,exist_ok=True)
names=['wall-a','wall-a-flat','wall-a-door','wall-a-window','wall-a-open','wall-b','wall-b-window','door-type-a','window-wide-type-a','wall-a-corner','wall-a-roof']
z=zipfile.ZipFile(ROOT/'assets/game/kenney/retro-urban-kit-2.0.zip');records=[]
for name in z.namelist():
    if name.startswith('Models/GLB format/Textures/') and name.endswith('.png'):
        p=out/'Textures'/Path(name).name;p.parent.mkdir(exist_ok=True);p.write_bytes(z.read(name))
for name in names:
    path=out/(name+'.glb');path.write_bytes(z.read('Models/GLB format/'+name+'.glb'))
    reset();bpy.ops.import_scene.gltf(filepath=str(path));bpy.context.view_layer.update()
    pts=[o.matrix_world@v.co for o in bpy.context.scene.objects if o.type=='MESH' for v in o.data.vertices]
    lo=[min(p[i] for p in pts) for i in range(3)];hi=[max(p[i] for p in pts) for i in range(3)]
    records.append(dict(id=name,bounds_z_up=[lo,hi],dimensions=[hi[i]-lo[i] for i in range(3)],sha256=hashlib.sha256(path.read_bytes()).hexdigest(),images=[dict(name=i.name,size=list(i.size)) for i in bpy.data.images if i.type=='IMAGE']))
    print(name,lo,hi,flush=True)
    if name in ('wall-a-door','wall-b-window','wall-a'):render_export(path,ROOT/'previews/city-interiors/references'/name)
(ROOT/'reports/city-interior-source-calibration.json').write_text(json.dumps(records,indent=2)+'\n')
