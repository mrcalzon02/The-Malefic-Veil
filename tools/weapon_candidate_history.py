"""Preserve original exported assets and PNG bytes before rebuilding a candidate."""
import hashlib,json,shutil
from pathlib import Path
def preserve(root,folder):
    path=folder/(folder.name+'.glb')
    if not path.exists():return
    evidence=folder/'render-evidence.json';key=path.read_bytes()+(evidence.read_bytes() if evidence.exists() else b'');ident=folder.name+'-study-'+hashlib.sha256(key).hexdigest()[:10];archive=folder.parent/ident
    if archive.exists():return
    shutil.copytree(folder,archive);(archive/path.name).rename(archive/(ident+'.glb'))
    blend=archive/(folder.name+'.blend')
    if blend.exists():blend.rename(archive/(ident+'.blend'))
    preview=root/'previews/weapon-refinement'/folder.name;dest=preview.parent/ident
    if preview.exists():shutil.copytree(preview,dest)
    if evidence.exists():
        data=json.loads(evidence.read_text())
        for item in data['renders'].values():item['path']=item['path'].replace('/'+folder.name+'/', '/'+ident+'/')
        (archive/'render-evidence.json').write_text(json.dumps(data,indent=2)+'\n')
    (archive/'history-record.json').write_text(json.dumps({'status':'SUPERSEDED_STUDY','source_id':folder.name,'asset_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'reason':'Original bytes retained before rebuilding; archive inclusion is not art approval.'},indent=2)+'\n')
