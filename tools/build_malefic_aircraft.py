#!/usr/bin/env python3
"""Original procedural PSX airframe assets. No concept-sheet raster is used as model texture."""
import os,json,math,struct,random,hashlib
from collections import defaultdict
from PIL import Image,ImageDraw,ImageFont
import numpy as np
import trimesh
OUT=os.environ.get('VEIL_AIRCRAFT_OUT',os.path.abspath(os.path.join(os.path.dirname(__file__),'..','build','aircraft_v2')))
os.makedirs(OUT,exist_ok=True)
PALETTES={
 'unraveled': {'metal':'#665f56','pale':'#b3a38b','dark':'#292a2a','rust':'#854837','red':'#813c32','yellow':'#bba059','glass':'#283e49','edge':'#9f8170','steel':'#4b5053','exhaust':'#df6936','rubber':'#242729','bone':'#d0b6a0'},
 'penitent': {'metal':'#645843','pale':'#9e8e6a','dark':'#24221f','rust':'#82452c','red':'#673a29','yellow':'#ba8c34','glass':'#304b3e','edge':'#6b7959','steel':'#454b46','exhaust':'#e96826','rubber':'#242323','bone':'#bd9c70'},
 'begotten': {'metal':'#863b43','pale':'#cb9e91','dark':'#2c171b','rust':'#62292d','red':'#a3414b','yellow':'#b99178','glass':'#4c1d2b','edge':'#a45f65','steel':'#423c3e','exhaust':'#dd663e','rubber':'#302329','bone':'#d5bfa4'},
}

def rgb(s):
 s=s.lstrip('#');return tuple(int(s[i:i+2],16) for i in (0,2,4))

def vadd(a,b):return tuple(x+y for x,y in zip(a,b))
def vl(a,b,t):return tuple((1-t)*x+t*y for x,y in zip(a,b))

class Model:
 def __init__(self,seed,palette):
  self.r=random.Random(seed);self.palette=PALETTES[palette];self.tris=[];self.name=palette
 def tri(self,a,b,c,mat):
  if np.linalg.norm(np.cross(np.subtract(b,a),np.subtract(c,a)))<1e-6:return
  self.tris.append((tuple(a),tuple(b),tuple(c),mat))
 def quad(self,a,b,c,d,mat):self.tri(a,b,c,mat);self.tri(a,c,d,mat)
 def box(self,pos,size,mat):
  x,y,z=pos;sx,sy,sz=(n*.5 for n in size)
  a=(x-sx,y-sy,z-sz);b=(x+sx,y-sy,z-sz);c=(x+sx,y+sy,z-sz);d=(x-sx,y+sy,z-sz)
  e=(x-sx,y-sy,z+sz);f=(x+sx,y-sy,z+sz);g=(x+sx,y+sy,z+sz);h=(x-sx,y+sy,z+sz)
  for face,ma in [((a,b,c,d),mat),((f,e,h,g),mat),((e,a,d,h),mat),((b,f,g,c),mat),((d,c,g,h),mat),((e,f,b,a),mat)]:self.quad(*face,ma)
 def tapered(self,a,b,r1,r2,mat,segments=7,phase=0):
  a=np.array(a,float);b=np.array(b,float);axis=b-a;axis=axis/np.linalg.norm(axis)
  side=np.cross(axis,[0,1,0]);
  if np.linalg.norm(side)<.01:side=np.cross(axis,[1,0,0])
  side=side/np.linalg.norm(side);ortho=np.cross(axis,side)
  v1=[];v2=[]
  for i in range(segments):
   theta=2*math.pi*i/segments+phase;delta=side*math.cos(theta)+ortho*math.sin(theta)
   v1.append(tuple(a+r1*delta));v2.append(tuple(b+r2*delta))
  for i in range(segments):
   j=(i+1)%segments
   self.quad(v1[i],v1[j],v2[j],v2[i],mat)
   if r2==0:continue
   self.tri(tuple(b),v2[j],v2[i],mat)
  if r1>0:forbidden=None # follows basic fan
  for i in range(1,segments-1): self.tri(tuple(a),v1[i],v1[i+1],mat)
 def spike(self,a,b,r,mat):self.tapered(a,b,r,0,mat,5)
 def line(self,pts,r,mat):
  for a,b in zip(pts[:-1],pts[1:]):self.tapered(a,b,r,r*.85,mat,5)
 def hull(self,sections,mat,segments=9,front=True):
  # tuple (z,xcenter,ycenter,rx,ry) cross-sections perpendicular to z
  rings=[]
  for z,x,y,rx,ry in sections:
   rings.append([(x+rx*math.cos(i*2*math.pi/segments),y+ry*math.sin(i*2*math.pi/segments),z) for i in range(segments)])
  for idx,(a,b) in enumerate(zip(rings[:-1],rings[1:])):
   for j in range(segments):
    k=(j+1)%segments
    self.quad(a[j],a[k],b[k],b[j],mat if idx%3 else 'edge')
  for ring in (rings[0],rings[-1]):
   center=tuple(np.mean(np.array(ring),axis=0))
   for j in range(1,segments-1):self.tri(center,ring[j],ring[j+1],mat)
 def sheet(self,points,mat,thickness=.09):
  # top/bottom triangulated fan: polygons generally convex-ish as authored
  points=[np.array(x,dtype=float) for x in points]
  upper=[tuple(x+[0,thickness,0]) for x in points]
  lower=[tuple(x-[0,thickness,0]) for x in points]
  for i in range(1,len(points)-1):
   self.tri(upper[0],upper[i],upper[i+1],mat);self.tri(lower[0],lower[i+1],lower[i],'dark')
  for i in range(len(points)):j=(i+1)%len(points);self.quad(upper[i],lower[i],lower[j],upper[j],'edge')
 def bolts(self,center,size=.05,n=3,mat='yellow'):
  x,y,z=center
  for i in range(n):self.box((x+(i-(n-1)/2)*size*2,y,z),(size,size,size),mat)
 def wing(self,sign,root,shape,mat='metal',ribs=False):
  # shape positions (outwardDist,up,foreAft), world X mirrored, optional contour silhouette
  x,y,z=root
  pts=[(x+sign*u,y+v,z+w) for u,v,w in shape]
  self.sheet(pts,mat,.075)
  if ribs:
   for idx in (1,3,5):
    if idx>=len(pts):continue
    self.tapered(root,pts[idx],.09,.035,'edge',5)
 def cable(self,a,b,mat='dark'):
  midpoint=(vl(a,b,.5)[0],max(a[1],b[1])+.10,vl(a,b,.5)[2])
  self.line([a,midpoint,b],.045,mat)
 def gun(self,x,y,z,length=1.8,scale=1):
  self.box((x,y,z+.15*scale),(.32*scale,.29*scale,.78*scale),'steel')
  for off in (-.09,.09):
   self.tapered((x+off*scale,y,z-.34*scale),(x+off*scale,y,z-length),.07*scale,.07*scale,'dark',8)
   self.tapered((x+off*scale,y,z-length),(x+off*scale,y,z-length-.12*scale),.11*scale,.095*scale,'steel',8)
  self.box((x,y+.24*scale,z+.32*scale),(.38*scale,.12*scale,.45*scale),'yellow')
 def glass(self,x,y,z,sx,sy,sz):
  self.box((x,y,z),(sx,sy,sz),'glass')
  self.box((x,y+sy*.46,z-sz*.48),(sx*.9,.045,.04),'steel')
 def prop(self,x,y,z,scale=.8):
  self.tapered((x,y,z-.25),(x,y,z+.25),.20*scale,.17*scale,'steel',8)
  for k in range(3):
   a=2*math.pi*k/3
   self.sheet([(x,y,z-.25),(x+math.cos(a)*.20*scale,y+math.sin(a)*.2*scale,z-.27),
    (x+math.cos(a+.12)*1.5*scale,y+math.sin(a+.12)*1.5*scale,z-.30),
    (x+math.cos(a-.12)*1.6*scale,y+math.sin(a-.12)*1.6*scale,z-.27)],'dark',.04)
 def wheel(self,x,y,z,r=.45):
  self.tapered((x-.16,y,z),(x+.16,y,z),r,r,'rubber',10)
  self.tapered((x-.18,y,z),(x-.21,y,z),r*.5,r*.50,'yellow',8)
  for k in range(8):
   a=2*math.pi*k/8
   self.box((x,y+math.cos(a)*r,z+math.sin(a)*r),(.40,.09,.22),'dark')
 def chipper(self,x,y,z,scale=1):
  # horizontal-axis serrated air intake front, zero wingtip wheels.
  self.tapered((x,y,z),(x,y,z+1.15*scale),.90*scale,.63*scale,'rust',10)
  self.tapered((x,y,z-.1),(x,y,z+.12),.62*scale,.60*scale,'dark',10)
  for i in range(10):
   a=2*math.pi*i/10
   center=(x+.61*scale*math.cos(a),y+.61*scale*math.sin(a),z-.20)
   tip=(x+.32*scale*math.cos(a+.32),y+.32*scale*math.sin(a+.32),z-.69*scale)
   self.spike(center,tip,.13*scale,'steel')
  for i in range(4):
   a=math.pi*i/2
   self.spike((x+.2*math.cos(a),y+.2*math.sin(a),z-.21),
       (x,y,z-.53),.095*scale,'yellow')
  self.tapered((x,y,z+1.30*scale),(x,y,z+1.45*scale),.66*scale,.52*scale,'dark',9)
  self.tapered((x,y,z+1.48*scale),(x,y,z+1.81*scale),.52*scale,.35*scale,'exhaust',8)
 def organic_wing(self,sign,root,span,reach,mode='bat'):
  x,y,z=root
  if mode=='bat':
   # bat wing, angular fingers; jagged trailing contour
   pts=[(x,y,z),(x+sign*.43*span,y+.23,z-.44*reach),
    (x+sign*.80*span,y+.50,z-.68*reach),
    (x+sign*span,y+.44,z-.82*reach),
    (x+sign*.83*span,y-.16,z-.07*reach),
    (x+sign*.65*span,y-.08,z+.16*reach),
    (x+sign*.47*span,y-.24,z+.10*reach),
    (x+sign*.25*span,y-.10,z+.25*reach)]
  elif mode=='dragon':
   pts=[(x,y,z),(x+sign*.30*span,y+.7,z-.26*reach),
    (x+sign*.72*span,y+1.05,z-.76*reach),
    (x+sign*span,y+.75,z-.98*reach),
    (x+sign*.82*span,y+.25,z-.37*reach),
    (x+sign*.67*span,y+.1,z+.09*reach),
    (x+sign*.47*span,y-.12,z+.17*reach),
    (x+sign*.26*span,y-.08,z+.31*reach)]
  else:
   pts=[(x,y,z),(x+sign*.30*span,y+.25,z-.20*reach),
    (x+sign*.67*span,y+.34,z-.55*reach),
    (x+sign*span,y+.28,z-.65*reach),
    (x+sign*.8*span,y-.15,z+.16*reach),
    (x+sign*.63*span,y-.27,z+.26*reach),
    (x+sign*.32*span,y-.2,z+.32*reach)]
  self.sheet(pts,'red',.085)
  for t in (2,3,4,6):
   if t<len(pts):self.tapered(root,pts[t],.16,.038,'bone',5)
  for j in (3,4,5):
   if j<len(pts):
    cur=pts[j]
    self.spike(cur,(cur[0]+sign*.2,cur[1]-.2,cur[2]+.18),.10,'dark')
 def fur(self,body_x,body_y,body_z,count,scale):
  for i in range(count):
   az=self.r.random()*math.pi*2
   zz=body_z+(self.r.random()-.5)*scale*1.7
   x=body_x+math.cos(az)*scale*.7
   y=body_y+math.sin(az)*scale*.55
   self.spike((x,y,zz),(x+(self.r.random()-.5)*.45,y+.15*self.r.choice([-1,1]),zz+self.r.uniform(.15,.48)*scale),
              .09*scale,'dark' if i%3 else 'edge')
 def boils(self,pos,count,span=1):
  x,y,z=pos
  for i in range(count):
   ox=(self.r.random()-.5)*span;oy=self.r.random()*span*.30;oz=(self.r.random()-.5)*span*1.5
   self.tapered((x+ox,y+oy,z+oz),(x+ox,y+oy+.12*span,z+oz),.13*span,.09*span,
                'red' if i%3 else 'pale',6)
 def jaw(self,pos,length,width,rows=10):
  x,y,z=pos
  self.box((x,y-.06,z-length*.16),(width*.85,.20,length*.75),'dark')
  # rows of triangular teeth, along left and right lip.
  for side in (-1,1):
   self.line([(x+side*width*.47,y+.16,z),(x+side*width*.60,y+.03,z-length)],.09,'bone')
   for i in range(rows):
    zz=z-i*length/(rows-1)
    self.spike((x+side*width*.44,y+.12,zz),
               (x+side*width*.17,y-.13,zz-.05),.11,'bone')
    self.spike((x+side*width*.48,y-.20,zz),
               (x+side*width*.28,y+.02,zz-.04),.09,'bone')
 def details(self,scope='mechanical',amount=12):
  # deterministic small scuffed panels or rivets to visually break surfaces
  pass

def make_unraveled(name):
 d={'needlewing':0,'harrier':1,'leviathan':2}[name];m=Model(141+d,'unraveled')
 L=[4.2,6.0,9.4][d]; W=[5.2,7.1,10.3][d];th=[.7,.95,1.55][d]
 # Unraveled: authentic awkward civilian aircraft, riveted militarized warthog nose
 m.hull([(-L*.65,0,.25,.02,.06),(-L*.4,0,.25,.37*th,.42*th),(-L*.1,0,.25,.52*th,.54*th),
    (L*.45,0,.24,.60*th,.57*th),(L*.56,0,.27,.30*th,.28*th),(L*.69,0,.28,.12,.15)],'metal',9)
 m.box((0,.20,L*.1),(1.15*th,.70*th,.95*L),'pale')
 m.glass(0,.62*th,-L*.24,.66*th,.46*th,.87*th)
 m.box((0,.18,-L*.49),(.78*th,.62*th,.63),'steel')
 for side in (-1,1):
  shape=[(0,0,-1.15),(.25*W,.1,-1.0),(.79*W,.06,-.65),(.95*W,-.1,-.23),(.68*W,-.1,.24),(.18*W,0,.18)]
  m.wing(side,(side*.33*th,.32,L*.10),[(u,v,z) for u,v,z in shape],'rust',True)
  m.wing(side,(side*.23*th,.37,L*.48),[(0,0,-.12),(.33*W,.25,-.10),(.46*W,.35,.34),(.08*W,.18,.31)],'metal')
  m.box((side*.56*W,.35,L*.11),(.55,.23,.94),'steel')
  m.prop(side*(W*.44),.35,L*.42,.52+d*.11)
  m.tapered((side*.36*W,-.28,L*.25),(side*.44*W,-.38,L*.03),.12,.09,'steel',5)
  for z in (-.7,.0,.7):
   m.box((side*.12*W,.35,L*.08+z*.6),(.31,.06,.35),'edge')
 m.gun(0,-.03,-L*.57,1.8*(.9+d*.14),1.1+d*.1)
 if d==1:
  m.box((0,.93,L*.3),(1.1,.25,.70),'yellow')
  for s in (-1,1):m.tapered((s*1.25,.28,-.2),(s*1.25,.28,-1.1),.22,.15,'steel',7)
 if d==2:
  # Salvage-freighter gunship: boxy utility cabin, underside cargo, giant twin pusher prop stacks
  m.box((0,-.18,L*.25),(2.15,1.25,L*.7),'pale')
  m.box((0,-.8,L*.30),(1.75,.35,1.45),'steel')
  for s in (-1,1):
   m.box((s*1.2,.05,L*.30),(.9,.77,1.55),'rust')
   m.gun(s*1.20,-.39,-1.3,2.0,.83)
  for z in (.1,.6,1.1):m.box((0,.95,z+.3),(1.7,.12,.07),'yellow')
 return m

def make_penitent(name):
 d={'shrike':0,'censer':1,'cathedral':2}[name];m=Model(531+d,'penitent')
 W=[4.7,6.6,10.7][d];L=[4.4,6.5,9.8][d]; scale=[.75,1.,1.45][d]
 # unholy flying farm equipment, tractor/harvester cab and mismatched equipment frames
 m.box((0,.02,0),(2*scale,.4,L),'steel')
 m.box((0,.16,L*.05),(1.9*scale,.88,L*.70),'rust')
 m.box((-.26*scale,.96,-L*.10),(1.13*scale,1.60*scale,1.45*scale),'yellow')
 m.glass(-.27*scale,1.35,-L*.50,.92*scale,.60*scale,.14)
 m.box((0,1.95*scale,-L*.05),(1.63*scale,.16*scale,2.0*scale),'steel')
 # stupid farm wheels clearly attached to aircraft, not magically playing air-foils
 for s in (-1,1):
  m.wheel(s*(.83*scale),-.85,-L*.01,.48*(1+d*.22))
  m.box((s*(W*.26),.11,.10),(W*.62,.14,L*.55),'metal')
  m.sheet([(s*.32*scale,.17,-L*.38),(s*W*.50,.20,-L*.42),(s*W*.90,.15,-L*.26),
    (s*W,.10,L*.05),(s*W*.81,.05,L*.30),(s*.52*scale,.18,L*.25)],'rust',.08)
  # asymmetric pipes, visible pressure cylinders and girders; no spinning wingtip wheel
  for o in (.0,.35):
   m.tapered((s*(W*.25+o),.45,L*.30),(s*(W*.25+o),.48,-L*.36),.10,.08,'steel',7)
  m.box((s*W*.52,-.12,L*.07),(.7,.63,.93),'steel')
  m.tapered((s*W*.39,-.22,L*.13),(s*W*.43,-.22,-L*.65),.12,.08,'dark',7)
 # A GREAT WOOD CHIPPER JAW AT THE FRONT OF MAIN THRUSTER; intake consumes air/flesh
 m.chipper(0,.13,-L*.49,scale)
 for side in (-1,1):
  m.box((side*1.0,1.05,L*.46),(.2,1.2,.18),'steel')
  m.cable((side*1.0,1.6,L*.43),(side*W*.72,.17,L*.05),'rubber')
 m.box((0,1.75*scale,L*.32),(.9*scale,.28,1.6),'dark')
 if d==0:
  # A flying compact dozer — light rotorless frame with small harvesting forks
  for s in (-1,1):
   m.tapered((s*.77,-.1,-L*.40),(s*1.14,-.31,-L*.84),.18,.05,'steel',7)
 if d==1:
  # combine-thresher mid craft, grain elevator/augers, big translucent reactor
  for s in (-1,1):
   m.tapered((s*1.0,1.2,L*.3),(s*(W*.67),1.5,L*.05),.18,.12,'steel',7)
   m.box((s*(W*.60),1.1,-.1),(.5,.55,1.45),'yellow')
  m.tapered((0,1.9,L*.02),(0,2.45,L*.28),.53,.27,'edge',9)
 if d==2:
  # heavy floating abattoir, 4 giant tread wheels, 3 serrated chippers and exhausts
  m.box((0,1.27,L*.16),(3.5,2.2,L*.63),'yellow')
  for s in (-1,1):
   for z in (-L*.32,L*.28):m.wheel(s*1.7,-1.25,z,1.12)
   m.chipper(s*1.20,.28,-L*.47,.62)
   m.box((s*2.25,.73,L*.16),(.45,1.8,L*.65),'steel')
  for s in (-1,1):
   m.tapered((s*1.2,2.1,L*.27),(s*1.2,3.25,L*.46),.25,.21,'dark',8)
   m.tapered((s*1.2,3.25,L*.46),(s*1.2,3.60,L*.52),.29,.11,'exhaust',7)
 return m

def make_begotten(name):
 d={'gnat':0,'mantis':1,'behemoth':2}[name];m=Model(1077+d,'begotten')
 if d==0:
  # mangy hunching cave bat; huge machine-gun legs, ragged fur and boils
  m.hull([(-1.8,0,.23,.07,.12),(-1.15,0,.32,.66,.54),(-.25,0,.50,1.02,.91),
    (.72,0,.64,1.13,1.20),(1.40,0,.55,.61,.66),(1.85,0,.32,.14,.19)],'red',9)
  m.hull([(-2.34,0,.17,.03,.07),(-1.85,0,.18,.30,.32),(-1.35,0,.15,.42,.34)],'dark',7)
  for s in (-1,1):
   m.organic_wing(s,(s*.71,1.02,.08),4.8,3.2,'bat')
   m.spike((s*.43,.83,-1.56),(s*.64,1.63,-1.87),.28,'bone') # ears
   m.gun(s*.65,-.88,-1.02,2.45,1.35) # legs entirely replaced by machine guns
   m.line([(s*.85,.8,.87),(s*1.17,1.42,.73),(s*.89,1.48,-.73),(s*.8,-.14,-1.0)],.16,'steel')
   for z in (-.6,-.1,.4):m.box((s*.75,1.33,z),(.53,.2,.18),'rubber')
  m.box((0,1.47,.62),(1.95,.35,1.62),'steel')
  m.box((0,1.68,.5),(1.12,.22,.90),'rust')
  m.fur(0,.61,.62,45,1)
  m.boils((0,1.19,.13),16,1.35)
  for s in (-1,1):m.spike((s*.33,.10,-2.0),(s*.36,-.26,-2.35),.16,'bone')
 elif d==1:
  # vulture beak and feather mane x quadrupedal lion, four long taloned paws
  m.hull([(-3.0,0,.14,.04,.08),(-2.22,0,.28,.38,.48),(-1.35,0,.40,.97,.88),
    (.15,0,.44,1.03,1.06),(1.45,0,.35,.72,.65),(2.17,0,.33,.16,.20)],'red',9)
  m.hull([(-3.42,0,.11,.03,.02),(-3.0,0,.11,.30,.20),(-2.45,0,.16,.44,.35)],'bone',7)
  m.spike((0,-.07,-3.45),(0,-.49,-3.75),.31,'bone') # hooked bird beak
  for s in (-1,1):
   m.organic_wing(s,(s*.72,1.09,-.68),6.1,4.0,'feather')
   # four distinct legs, front/back, long bony claws
   for z in (-1.19,1.32):
    a=(s*.84,-.31,z);b=(s*1.18,-1.33,z+.37);foot=(s*1.33,-1.87,z-.18)
    m.tapered(a,b,.31,.19,'red',7);m.tapered(b,foot,.23,.17,'dark',7)
    for q in (-1,0,1):m.spike((foot[0]+q*.11,foot[1],foot[2]-.06),
     (foot[0]+q*.15,foot[1]-.20,foot[2]-.75),.13,'bone')
  m.fur(0,.70,-.80,70,1.35)
  m.boils((0,1.10,.25),11,1.2)
  m.box((0,1.55,.35),(1.15,.3,1.0),'steel')
  for s in (-1,1):m.spike((s*.55,1.30,-1.6),(s*.68,1.96,-1.9),.27,'dark')
 else:
  # Enormous RAW RED FLESH WHALE with peeled skin, SHARK TEETH and exactly FOUR dragon wings
  m.hull([(-8.4,0,-.12,.07,.11),(-7.2,0,-.12,1.55,1.42),(-5.35,0,-.08,2.30,1.89),
    (-2.6,0,.02,2.83,2.28),(.8,0,.05,3.05,2.41),(3.85,0,.03,2.58,1.96),
    (6.50,0,.16,1.60,1.18),(7.95,0,.18,.15,.13)],'red',12)
  m.hull([(-8.5,0,-.12,.04,.07),(-7.7,0,-.37,1.22,.65),(-6.1,0,-.55,1.68,.64)],'dark',10)
  # Deliberately gaping exposed shark-mouth instead of a conventional whale jaw.
  m.box((0,-.49,-8.25),(2.42,.75,2.38),'dark')
  m.tapered((-1.30,-.76,-7.38),(-1.19,-.97,-9.30),.26,.19,'bone',6)
  m.tapered((1.30,-.76,-7.38),(1.19,-.97,-9.30),.26,.19,'bone',6)
  m.tapered((-1.42,.17,-7.40),(-1.11,.32,-9.39),.19,.12,'pale',6)
  m.tapered((1.42,.17,-7.40),(1.11,.32,-9.39),.19,.12,'pale',6)
  m.jaw((0,-.67,-7.71),3.4,2.65,20)
  for i in range(16):
   zz=-7.42-i*.126
   for side in (-1,1):
    m.spike((side*1.09,.23,zz),(side*.90,-.60,zz-.06),.16,'bone')
    m.spike((side*1.19,-.82,zz),(side*.91,-.13,zz-.03),.16,'bone')
  for s in (-1,1):
   m.organic_wing(s,(s*2.1,1.15,-2.75),10.9,6.6,'dragon')
   m.organic_wing(s,(s*1.9,1.05,2.85),8.9,5.7,'dragon')
   # skin peeled back, pale ligament scaffolds
   for z in (-4.9,-2.0,.8,3.5):
    m.tapered((s*2.1,.55,z),(s*2.43,1.87,z+.53),.32,.16,'bone',6)
    m.box((s*2.7,.39,z+.6),(.26,.28,.97),'pale')
   for z in (-4.15,-2.1,.05,2.1,4.2):
    m.spike((s*2.33,.67,z),(s*2.74,1.51,z+.18),.27,'bone')
  m.boils((0,2.2,.3),36,3.3)
  # massive red sacs and organic bombardment glands
  for x in (-1.38,1.38):
   for z in (-1.0,1.5,4.1):
    m.hull([(z-.67,x,-1.90,.17,.11),(z-.25,x,-2.55,.66,.49),
       (z+.40,x,-2.45,.63,.49),(z+.82,x,-1.94,.12,.11)],'rust',7)
  # eye ridges / multiple eyes
  for s in (-1,1):
   m.box((s*1.82,.32,-5.94),(.44,.18,.45),'dark')
   m.box((s*1.82,.38,-5.98),(.18,.11,.23),'yellow')
 return m


def refine_model_v2(m,palette,name):
 """Second authored geometry pass: material faction identity and readable silhouette.
 No model-plane texture fakery. All accents are real independent triangles.
 """
 if palette=='unraveled':
  d={'needlewing':0,'harrier':1,'leviathan':2}[name]; L=[4.2,6.0,9.4][d];W=[5.2,7.1,10.3][d]
  # Asymmetrical salvaged skin plates, A-10-style military nose attachment and bolts.
  for s in (-1,1):
   for idx in range(4+d):
    z=-L*.37+idx*.42
    m.box((s*(.42+d*.10),.52+d*.03,z),(.14,.03,.24),'rust' if idx%2 else 'steel')
    m.bolts((s*(.41+d*.11),.56+d*.03,z),.045,2,'yellow')
   # Production-repair seams on mismatched wing pieces.
   for idx in range(3+d):
    a=s*(1.15+idx*.54)
    m.box((a,.45,-.43),(.20,.035,.40),'pale' if idx%2 else 'rust')
   # Dual pusher prop support spars, not a decorative flying-wing blade.
   m.tapered((s*W*.44,.32,L*.20),(s*W*.44,.38,L*.46),.06,.044,'steel',6)
  m.box((0,.12,-L*.52),(.90,.22,.15),'yellow')
  for j in range(3):m.box((-.30+j*.30,-.01,-L*.63),(.07,.20,.10),'dark')
  if d==2:
   for s in (-1,1):
    m.box((s*1.58,1.35,L*.13),(.36,.18,1.24),'dark')
    m.tapered((s*1.58,1.35,-.12),(s*1.58,1.42,-1.14),.12,.06,'steel',6)
 if palette=='penitent':
  d={'shrike':0,'censer':1,'cathedral':2}[name];L=[4.4,6.5,9.8][d];scl=[.75,1.,1.45][d]
  # Cab bars and exposed drive hydraulics on the visible top; definitely no chapel ornament.
  for side in (-1,1):
   s=side
   m.tapered((s*.60*scl,.75,-L*.35),(s*.62*scl,1.95*scl,-L*.39),.060,.053,'steel',5)
   m.tapered((s*.52*scl,1.95*scl,-L*.39),(s*.52*scl,1.95*scl,L*.12),.07,.056,'dark',5)
   for i in range(2+d):
    z=-L*.31+i*.43
    m.box((s*(1.06+.11*d),.36,z),(.27,.16,.22),'yellow' if i%2 else 'rust')
   m.line([(s*.62*scl,.35,L*.28),(s*.93*scl,.94,L*.27),(s*1.03*scl,.5,-L*.30)],.055,'rubber')
  # 3 serrated splitter bars in front of intake, angled sheet-metal shredder jaw.
  for s in (-1,0,1):
   m.tapered((s*.33*scl,.38,-L*.47),(s*.42*scl,.15,-L*.65),.11,.018,'steel',5)
  m.box((0,2.12*scl,L*.25),(.72,.08,.48),'yellow')
  if d==2:
   for s in (-1,1):
    m.line([(s*2.05,2.19,L*.36),(s*2.25,2.38,0),(s*1.51,.46,-L*.42)],.092,'dark')
 if palette=='begotten':
  d={'gnat':0,'mantis':1,'behemoth':2}[name]
  if d==0:
   # Physical straps and leather tabs frame the weapon-leg sockets, scar seams.
   for s in (-1,1):
    m.line([(s*.78,1.20,1.06),(s*1.03,1.60,-.09),(s*.52,.93,-1.37)],.115,'steel')
    for k in range(3):
     m.spike((s*.56,.62,-1.6+k*.36),(s*(.71+k*.03),.89,-1.64+k*.37),.095,'bone')
   m.box((0,1.79,.30),(.80,.14,.18),'rust')
  elif d==1:
   # Claw articulation and ragged mane masses stay anatomically four-legged.
   for s in (-1,1):
    for z in (-1.19,1.32):
     m.box((s*1.18,-1.27,z+.31),(.31,.19,.20),'bone')
   for i in range(6):
    x=(-.58+i*.23);m.spike((x,.98,-1.37),(x,1.36,-1.9),.08,'dark')
  else:
   # Exposed muscle ligaments and scar plates on raw skinned whale, no extra wing pairs.
   for s in (-1,1):
    for i,z in enumerate((-5.3,-3.1,-.9,1.4,3.6)):
     m.tapered((s*2.18,1.14,z),(s*2.48,1.68,z+.76),.075,.028,'bone',5)
     m.spike((s*2.62,1.30,z+.43),(s*2.79,1.59,z+.73),.07,'dark')

ROSTER=[('unraveled','needlewing','Light / scrap A-10 nose, pusher prop'),
 ('unraveled','harrier','Medium / improvised salvage attack aircraft'),
 ('unraveled','leviathan','Heavy / bolted salvage freight gunship'),
 ('penitent','shrike','Light / airborne tractor-dozer shredder'),
 ('penitent','censer','Medium / thresher crop-harvest rig'),
 ('penitent','cathedral','Heavy / flying industrial abattoir'),
 ('begotten','gnat','Light / mangy bat, machine gun legs'),
 ('begotten','mantis','Medium / vulture-lion four-clawed hunter'),
 ('begotten','behemoth','Heavy / raw whale, shark jaw, four dragon wings')]

def atlas(path,palette,seed):
 rng=random.Random(seed)
 im=Image.new('RGB',(128,128));p=im.load();pal=list(PALETTES[palette].values())
 for y in range(128):
  for x in range(128):
   co=rgb(pal[((y//32)*4+(x//32))%len(pal)])
   jitter=rng.choice([-22,-15,-7,-4,0,0,4,7,12,18])
   if x%32 in (0,1) or y%32 in (0,1):jitter-=17
   p[x,y]=tuple(max(0,min(255,v+jitter)) for v in co)
 im.save(path,optimize=True)
 return im

def glb_export(model,name,palette):
 # Group triangle data by material; positions are pre-baked so older Java reader
 # (which ignores nodes.transforms) sees the correct fully assembled craft.
 mats=list(model.palette)
 blob=bytearray();views=[];access=[];primitives=[]
 def alloc(data,target=None):
  while len(blob)%4:blob.append(0)
  pos=len(blob);blob.extend(data)
  v={'buffer':0,'byteOffset':pos,'byteLength':len(data)}
  if target:v['target']=target
  views.append(v);return len(views)-1
 def acc(view,count,type_,component,mi=None,ma=None):
  obj={'bufferView':view,'componentType':component,'count':count,'type':type_}
  if mi is not None:obj['min']=mi;obj['max']=ma
  access.append(obj);return len(access)-1
 for mat in mats:
  tris=[face for face in model.tris if face[3]==mat]
  if not tris:continue
  xyz=np.array([v for t in tris for v in t[:3]],dtype=np.float32)
  uv=np.tile(np.array([[.07,.07],[.94,.06],[.55,.91]],dtype=np.float32),(len(tris),1))
  indices=np.arange(len(xyz),dtype=np.uint32)
  pv=alloc(xyz.tobytes(),34962);uvv=alloc(uv.tobytes(),34962);iv=alloc(indices.tobytes(),34963)
  pa=acc(pv,len(xyz),'VEC3',5126,xyz.min(axis=0).tolist(),xyz.max(axis=0).tolist())
  ua=acc(uvv,len(xyz),'VEC2',5126)
  ia=acc(iv,len(indices),'SCALAR',5125)
  primitives.append({'attributes':{'POSITION':pa,'TEXCOORD_0':ua},'indices':ia,'material':mats.index(mat),'mode':4})
 atlas_path=os.path.join(OUT,palette+'_palette_128.png')
 if not os.path.isfile(atlas_path):atlas(atlas_path,palette,20261021+len(palette))
 # One tiny embedded 128² PSX noise tile creates intentional mottled pixel
 # sampling in external glTF viewers; Java2D uses baseColorFactor faithfully.
 from io import BytesIO
 texture=Image.new('RGB',(128,128));pixels=texture.load();r=random.Random(7771)
 for yy in range(128):
  for xx in range(128):
   value=r.choice([144,158,177,192,206,218,231,241,247,255])
   if xx%16 in (0,1) or yy%16 in (0,1):value=max(0,value-12)
   pixels[xx,yy]=(value,value,value)
 encoded=BytesIO();texture.save(encoded,format='PNG',optimize=True)
 img_view=alloc(encoded.getvalue())
 material=[{'name':n,'pbrMetallicRoughness':{
               'baseColorTexture':{'index':0},
               'baseColorFactor':[*(v/255 for v in rgb(model.palette[n])),1],
               'metallicFactor':0,'roughnessFactor':1},'doubleSided':True} for n in mats]
 j={'asset':{'version':'2.0','generator':'The Malefic Veil procedural PSX aircraft v2'},
    'scene':0,'scenes':[{'nodes':[0]}],'nodes':[{'mesh':0,'name':name}],
    'meshes':[{'name':name,'primitives':primitives}],
    'images':[{'bufferView':img_view,'mimeType':'image/png'}],
    'textures':[{'source':0,'sampler':0}],
    'samplers':[{'magFilter':9728,'minFilter':9728,'wrapS':10497,'wrapT':10497}],
    'materials':material,'buffers':[{'byteLength':len(blob)}],
    'bufferViews':views,'accessors':access,
    'extras':{'author':'Original game asset (procedural)','variant':palette,
       'target':'Java17 GlbMesh, baked mesh triangles, no skinned/animated rig'}}
 js=json.dumps(j,separators=(',',':'),ensure_ascii=True).encode()
 while len(js)%4:js+=b' '
 while len(blob)%4:blob.append(0)
 size=12+8+len(js)+8+len(blob)
 data=struct.pack('<III',0x46546c67,2,size)+struct.pack('<II',len(js),0x4e4f534a)+js+struct.pack('<II',len(blob),0x004e4942)+blob
 path=os.path.join(OUT,name+'.glb')
 open(path,'wb').write(data)
 # Validate using trimesh loader and manifest-level geometry limits
 scene=trimesh.load(path,force='scene',process=False)
 assert len(scene.geometry)>0
 assert len(model.tris)>120 and len(model.tris)<8000,(name,len(model.tris))
 bounds=np.array([v for t in model.tris for v in t[:3]])
 assert np.isfinite(bounds).all()
 return {'id':name,'faction':palette,'triangles':len(model.tris),'vertices':len(model.tris)*3,
  'size_bytes':len(data),'sha256':hashlib.sha256(data).hexdigest(),
  'bounds_min':np.round(bounds.min(axis=0),3).tolist(),
  'bounds_max':np.round(bounds.max(axis=0),3).tolist(),
  'parts':'static mesh; per-face RGB materials; authored airframe visual reference only'}

# Orthographic projection for design contact sheet (not a game screenshot)
from PIL import ImageDraw,ImageFont

def preview_render(m,size=(500,330),orientation='iso'):
 w,h=size
 canvas=Image.new('RGB',size,(37,41,48));d=ImageDraw.Draw(canvas)
 verts=np.array([v for t in m.tris for v in t[:3]],dtype=float)
 # Aircraft along -Z axis, y up; side/isometric camera
 if orientation=='iso':
  ry=np.deg2rad(37);rx=np.deg2rad(-23)
 elif orientation=='top':
  ry=0;rx=np.deg2rad(-89)
 else:ry=0;rx=0
 RY=np.array([[math.cos(ry),0,math.sin(ry)],[0,1,0],[-math.sin(ry),0,math.cos(ry)]])
 RX=np.array([[1,0,0],[0,math.cos(rx),-math.sin(rx)],[0,math.sin(rx),math.cos(rx)]])
 T=verts@RY.T@RX.T
 center=(T.min(0)+T.max(0))*.5
 spans=T.max(0)-T.min(0)
 scale=min((w-38)/max(spans[0],.1),(h-40)/max(spans[1],.1))
 projections=[]
 for i,tri in enumerate(m.tris):
  v=T[i*3:i*3+3]
  pts=[(float((p[0]-center[0])*scale+w/2),float(h/2-(p[1]-center[1])*scale)) for p in v]
  clr=np.array(rgb(m.palette[tri[3]]),dtype=float)
  normal=np.cross(v[1]-v[0],v[2]-v[0]);n=np.linalg.norm(normal)
  if n>0:normal/=n
  diffuse=.72+.28*abs(float(np.dot(normal,np.array([.30,.65,.65]))))
  color=tuple(int(max(0,min(255,x*diffuse))) for x in clr)
  projections.append((float(v[:,2].mean()),pts,color))
 # depth coordinates smaller towards camera depends rotation, painter sorting
 for _,pts,color in sorted(projections,key=lambda p:p[0],reverse=True):
  d.polygon(pts,fill=color)
  if max(np.linalg.norm(np.subtract(pts[j],pts[(j+1)%3])) for j in range(3))>8:
   d.line(pts+[pts[0]],fill=tuple(max(0,int(x*.59)) for x in color),width=1)
 return canvas

def create():
 metadata=[];preview={}
 for palette,name,desc in ROSTER:
  m={'unraveled':make_unraveled,'penitent':make_penitent,'begotten':make_begotten}[palette](name)
  refine_model_v2(m,palette,name)
  ident=f'{palette}_{name}'
  meta=glb_export(m,ident,palette)
  meta['description']=desc
  metadata.append(meta)
  preview[ident]=preview_render(m)
  print(ident,'tris',meta['triangles'],'bounds',meta['bounds_min'],meta['bounds_max'])
 with open(os.path.join(OUT,'aircraft_manifest.json'),'w') as f:json.dump({'schema':1,'generator':'build_malefic_aircraft.py v2',
  'game':'The Malefic Veil','status':'static geometry; no flight mechanics or animation',
  'models':metadata},f,indent=2)
 contact=Image.new('RGB',(1500,1170),(29,31,35));pen=ImageDraw.Draw(contact)
 for i,(faction,name,desc) in enumerate(ROSTER):
  image=preview[f'{faction}_{name}'];x=(i%3)*500;y=(i//3)*390
  contact.paste(image,(x,y+26));pen.rectangle([x,y,x+499,y+25],fill=(21,21,24))
  pen.text((x+12,y+7),f'{faction.upper()}  /  {name.upper()}  /  {metadata[i]["triangles"]} TRI',fill=(233,217,185))
  pen.text((x+10,y+358),desc,fill=(203,187,169))
  pen.rectangle([x,y,x+499,y+389],outline=(100,91,80),width=2)
 path=os.path.join(OUT,'aircraft_contact_sheet.png');contact.save(path)
 print('Wrote',path,os.path.getsize(path))

if __name__=='__main__':create()
