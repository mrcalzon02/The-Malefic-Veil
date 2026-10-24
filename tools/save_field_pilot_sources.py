"""Save editable Blender animation/NLA projects from the exact exported pilot GLBs.
The original modeling projects are retained alongside these animated projects.
"""
import bpy,json,sys
from pathlib import Path

root=Path(__file__).resolve().parents[1]
for record in json.loads((root/'build/candidates'/(sys.argv[sys.argv.index('--revision')+1] if '--revision' in sys.argv else 'field-pilots-r4')/'manifest.json').read_text()):
    path=Path(record['path'])
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(path),import_pack_images=True)
    bpy.context.scene['review_status']='REFINEMENT STUDY: not approved for production'
    bpy.context.scene['exported_asset_sha256']=record['sha256']
    bpy.ops.wm.save_as_mainfile(filepath=str(path.with_name(path.stem+'-animated.blend')),compress=True)
    print('ANIMATED_SOURCE_SAVED',record['id'],len(bpy.data.actions),flush=True)
