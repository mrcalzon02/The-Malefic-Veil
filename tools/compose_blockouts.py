"""Compose original primitive renders into readable review sheets (Pillow)."""
import json
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1]
weapons=json.loads((ROOT/'data/weapons.json').read_text())['weapons']
OUT=ROOT/'assets/game/arsenal-blockouts'
font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',22)
title=ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf',34)
small=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',18)
for page in range(4):
    sheet=Image.new('RGB',(1600,1760),(15,23,32));draw=ImageDraw.Draw(sheet)
    draw.text((30,18),f'THE MALEFIC VEIL  /  Arsenal blockouts  {page+1}/4',font=title,fill=(226,233,235))
    draw.text((30,68),'Primitive silhouettes only · automatic framing · moving mechanisms in light grey · ammo in ochre',font=small,fill=(169,185,193))
    for i,w in enumerate(weapons[page*8:page*8+8]):
        x=(i%2)*800;y=115+(i//2)*405
        im=Image.open(ROOT/'previews/blockouts'/f'{w["id"]}.png').convert('RGB');im.thumbnail((780,330))
        sheet.paste(im,(x+(800-im.width)//2,y))
        draw.text((x+22,y+332),f"{w['id'][:3].upper()}  {w['name']}  /  {w['silhouette']}",font=font,fill=(226,233,235))
        draw.text((x+22,y+367),f"{w['length_m']:.2f} m target · {w['mechanism']} pivot · {w['ammo']}",font=small,fill=(169,185,193))
    sheet.save(OUT/f'arsenal-review-{page+1}.png')
sheet=Image.new('RGB',(1920,2880),(15,23,32));draw=ImageDraw.Draw(sheet)
for i,w in enumerate(weapons):
    x=(i%4)*480;y=(i//4)*360
    im=Image.open(ROOT/'previews/blockouts'/f'{w["id"]}.png').convert('RGB');im=im.resize((480,300))
    sheet.paste(im,(x,y));draw.text((x+12,y+304),f"{w['id'][:3].upper()}  {w['name']}",font=font,fill=(226,233,235))
sheet.save(OUT/'arsenal-overview.png')
rows=['id\tname\tsilhouette\tprimary\talternate\tmechanism']
rows += ['\t'.join(w[k] for k in('id','name','silhouette','primary','alternate','mechanism')) for w in weapons]
(OUT/'catalog.tsv').write_text('\n'.join(rows)+'\n',encoding='utf-8')
print('4 review pages, overview, and gallery catalog written')
