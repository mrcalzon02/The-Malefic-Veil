"""Small reusable stencil for an authored receiver panel; no source media copied."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import sys
R=Path(__file__).resolve().parents[1]
out=R/'build/candidates/weapon-refinement/finish-sources';out.mkdir(parents=True,exist_ok=True)
im=Image.new('RGBA',(1024,512),(0,0,0,0));d=ImageDraw.Draw(im)
font=ImageFont.truetype('C:/Windows/Fonts/consolab.ttf',94)
small=ImageFont.truetype('C:/Windows/Fonts/consola.ttf',35)
label=sys.argv[sys.argv.index('--name')+1].upper() if '--name' in sys.argv else 'VESPER'
while d.textbbox((0,0),label,font=font)[2]>560:font=ImageFont.truetype('C:/Windows/Fonts/consolab.ttf',font.size-2)
d.text((345,120),label,font=font,fill=(171,187,183,235))
d.line((351,239,879,239),fill=(139,159,155,200),width=4)
d.text((351,261),'01 / FIELD SERVICE',font=small,fill=(145,163,160,220))
d.polygon([(876,348),(925,431),(827,431)],outline=(186,143,70,240),width=7)
d.line((876,374,876,402),fill=(186,143,70,240),width=7)
d.ellipse((873,413,879,419),fill=(186,143,70,240))
dest=out/('shared-stencil.png' if '--name' in sys.argv else 'vesper-stencil.png');im.save(dest)
if '--name' in sys.argv:
    # Keep markings behind the ejection slot; the opposite panel needs its own
    # placement so correcting text orientation does not put it across the port.
    words=label.split();lines=words if len(words)==2 else [label]
    for opposite in(False,True):
        im=Image.new('RGBA',(1024,512),(0,0,0,0));d=ImageDraw.Draw(im);x=520 if opposite else 200
        font=ImageFont.truetype('C:/Windows/Fonts/consolab.ttf',70)
        while max(d.textbbox((0,0),line,font=font)[2] for line in lines)>300:font=ImageFont.truetype('C:/Windows/Fonts/consolab.ttf',font.size-2)
        for i,line in enumerate(lines):d.text((x,140+i*90),line,font=font,fill=(171,187,183,235))
        d.line((x,337,x+290,337),fill=(139,159,155,200),width=4)
        im.save(dest.with_name('shared-stencil-opposite.png') if opposite else dest)
print(dest)
