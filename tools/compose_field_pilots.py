"""Compose authored-only pilot comparisons and actual exported-animation frame sequences.
Run with a Python environment containing Pillow. Does not publish or approve assets.
"""
from pathlib import Path
import json,hashlib
from PIL import Image,ImageDraw,ImageFont

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'build/candidates/field-pilots-r4'
DEST=ROOT/'previews/field-pilots-r4'
PILOTS=[('sentry-mki','Sentry Mk.I','thrown-unraveled-turret'),('censer-of-dread','Censer of Dread','thrown-penitent-miasma'),('regeneration-heart','Regeneration Heart','thrown-begotten-health')]
try:
    font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',21)
    small=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',16)
except OSError:font=small=ImageFont.load_default()

def tile(path,size):
    im=Image.open(path).convert('RGB');im.thumbnail((size,size));return im

overview=Image.new('RGB',(1440,540),(24,29,34));draw=ImageDraw.Draw(overview)
comparison=Image.new('RGB',(1440,1060),(24,29,34));cdraw=ImageDraw.Draw(comparison)
for index,(id,name,old) in enumerate(PILOTS):
    evidence=json.loads((SOURCE/id/'render-evidence.json').read_text())
    assert hashlib.sha256((SOURCE/id/(id+'.glb')).read_bytes()).hexdigest()==evidence['asset_sha256']
    for item in evidence['renders'].values():assert hashlib.sha256(Path(item['path']).read_bytes()).hexdigest()==item['sha256']
    im=tile(DEST/id/'front.png',480);overview.paste(im,(index*480,0));draw.text((index*480+18,488),name,font=font,fill='#ede5d6');draw.text((index*480+18,518),'Refinement study / not production approved',font=small,fill='#b6bdc1')
    comparison.paste(tile(ROOT/'previews/field-deployables'/old/'front.png',480),(index*480,35));comparison.paste(im,(index*480,560))
    cdraw.text((index*480+12,8),'REJECTED ORIGINAL — '+name,font=small,fill='#e0ab9f');cdraw.text((index*480+12,533),'REFINEMENT STUDY — '+name,font=small,fill='#c8d7c8')
    for clip in ('DEPLOYING','OPERATING'):
        frames=evidence['sequences'][clip];sheet=Image.new('RGB',(1200,540),(24,29,34));sd=ImageDraw.Draw(sheet);gif=[]
        for frame,item in enumerate(frames):
            file=Path(item['path']);assert hashlib.sha256(file.read_bytes()).hexdigest()==item['sha256']
            im=tile(file,240);x=(frame%5)*240;y=(frame//5)*270;sheet.paste(im,(x,y));sd.text((x+7,y+243),'%.2f s'%item['time_s'],font=small,fill='#ede5d6')
            gif.append(Image.open(file).convert('RGB'))
        sheet.save(DEST/id/(clip.lower()+'-sequence.png'))
        gif[0].save(DEST/id/(clip.lower()+'.gif'),save_all=True,append_images=gif[1:]+[gif[-1]]*3,duration=150 if clip=='DEPLOYING' else 90,loop=0,optimize=False)
overview.save(DEST/'pilot-overview.png');comparison.save(DEST/'before-after.png')
print('Composed three pilot views, authored before/after comparison, six timed sequence sheets and six GIFs.')
