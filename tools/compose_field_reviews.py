from pathlib import Path
import json
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1];rows=json.loads((root/'build/candidates/field-deployables/manifest.json').read_text());dest=root/'previews/field-deployables'
for view in ('front','rear','closeup','clay'):
 sheet=Image.new('RGB',(1568,750),(24,27,31));draw=ImageDraw.Draw(sheet)
 for i,r in enumerate(rows):
  im=Image.open(dest/r['id']/(view+'.png')).convert('RGB');im.thumbnail((224,224));x=(i%7)*224;y=(i//7)*250;sheet.paste(im,(x,y));draw.text((x+5,y+226),r['name'],fill=(230,228,220))
 sheet.save(dest/(view+'-roster.png'))
a=json.loads((root/'reports/field-deployables-inspection.json').read_text());contracts={}
for r in a:
 if r['id'].startswith('thrown-'):
  contracts[r['id']]={'plot_id':'plot-5','placement_kind':'temporary-equipment','organic':r['faction']=='begotten','continuous_surfaces':[{'mesh':'LivingSurface'}] if r['faction']=='begotten' else [],'intentional_separate_meshes':[m['name'] for m in r.get('meshes',[]) if m['name']!='LivingSurface']}
(root/'data/field-deployables-contracts.json').write_text(json.dumps(contracts,indent=2))
