"""QA sheets and private calibration comparison; never alter original render bytes."""
import sys,json,hashlib
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
R=Path(__file__).resolve().parents[1];ident=sys.argv[1];pre=R/'previews/weapon-refinement'/ident;folder=R/'build/candidates/weapon-refinement'/ident
font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',20);bold=ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf',26)
sheet=Image.new('RGB',(1792,1392),(20,27,34));draw=ImageDraw.Draw(sheet);draw.text((20,14),ident+' / exported geometry review',font=bold,fill='white')
for i,label in enumerate(('front','rear','closeup','clay','side','reload')):
    im=Image.open(pre/(label+'.png')).convert('RGB');im.thumbnail((880,400));x=(i%2)*896;y=60+(i//2)*444;sheet.paste(im,(x+(896-im.width)//2,y));draw.text((x+18,y+402),label,font=font,fill=(220,226,230))
sheet.save(pre/'review-sheet.png')
# Reference panels are private, and intentionally stored outside the gallery source roots.
comparison=Image.new('RGB',(1792,1220),(20,27,34));draw=ImageDraw.Draw(comparison);draw.text((18,10),'PRIVATE calibration comparison — source references are not redistributed',font=bold,fill='white')
asset=Image.open(pre/'front.png').convert('RGB');asset.thumbnail((880,560));comparison.paste(asset,(8,75));draw.text((18,650),'Original weapon candidate: grip, receiver, bore and magazine detail',font=font,fill='white')
for i,ref in enumerate(('car-01','car-08','character-01','character-17')):
    p=R/'previews/faction-structures/references'/(ref+'.png');im=Image.open(p).convert('RGB');im.thumbnail((425,480));x=896+(i%2)*448;y=65+(i//2)*560;comparison.paste(im,(x,y));draw.text((x+8,y+485),ref,font=font,fill='white')
comparison_path=folder/'comparison.png';comparison.save(comparison_path)
evidence=json.loads((folder/'render-evidence.json').read_text());evidence['renders']['comparison']={'path':comparison_path.relative_to(R).as_posix(),'sha256':hashlib.sha256(comparison_path.read_bytes()).hexdigest()};(folder/'render-evidence.json').write_text(json.dumps(evidence,indent=2)+'\n')
print('Review sheet and private reference comparison ready',ident)
