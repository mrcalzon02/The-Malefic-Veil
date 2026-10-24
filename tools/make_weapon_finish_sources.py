"""Small reusable stencil for an authored receiver panel; no source media copied."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
R=Path(__file__).resolve().parents[1]
out=R/'build/candidates/weapon-refinement/finish-sources';out.mkdir(parents=True,exist_ok=True)
im=Image.new('RGBA',(1024,512),(0,0,0,0));d=ImageDraw.Draw(im)
font=ImageFont.truetype('C:/Windows/Fonts/consolab.ttf',94)
small=ImageFont.truetype('C:/Windows/Fonts/consola.ttf',35)
d.text((345,120),'VESPER',font=font,fill=(171,187,183,235))
d.line((351,239,879,239),fill=(139,159,155,200),width=4)
d.text((351,261),'01 / FIELD SERVICE',font=small,fill=(145,163,160,220))
d.polygon([(876,348),(925,431),(827,431)],outline=(186,143,70,240),width=7)
d.line((876,374,876,402),fill=(186,143,70,240),width=7)
d.ellipse((873,413,879,419),fill=(186,143,70,240))
im.save(out/'vesper-stencil.png')
print(out/'vesper-stencil.png')
