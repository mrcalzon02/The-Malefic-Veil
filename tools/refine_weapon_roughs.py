"""Refine the existing working arsenal into staged, mechanical rough candidates.
No new textures. Existing trim is retained for export checks; clay is the art focus.
Blender --factory-startup -b --python ... -- [--ids w01-vesper ...]
"""
import bpy,bmesh,math,json,sys,hashlib,struct,tempfile
from pathlib import Path
from mathutils import Vector,Matrix
R=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(R/'tools'));from weapon_candidate_history import preserve
BASE=R/'build/candidates/weapon-refinement'
BASE.mkdir(parents=True,exist_ok=True);tempfile.tempdir=str(BASE)
defs=json.loads((R/'data/weapons.json').read_text())['weapons']
args=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []
revision=args[args.index('--revision')+1] if '--revision' in args else 'v1'
if '--ids' in args:defs=[d for d in defs if d['id'] in args[args.index('--ids')+1:]]
records=[];contracts={};report=[]
C=Matrix(((0,1,0,0),(0,0,1,0),(1,0,0,0),(0,0,0,1)));Ci=C.inverted()
for d in defs:
    idx=int(d['id'][1:3])-1
    ident=d['id']+'-refined-'+revision;out=BASE/ident;preserve(R,out);out.mkdir(exist_ok=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    with bpy.data.libraries.load(str(R/'assets/game/arsenal/working-arsenal.blend'),link=False) as(src,dst):dst.collections=[d['id']]
    col=dst.collections[0];bpy.context.scene.collection.children.link(col)
    root=next(o for o in col.objects if o.name.endswith('__ROOT'));root.animation_data_clear();root.location=(0,0,0)
    for o in col.objects:o.animation_data_clear()
    action=next(o for o in col.objects if o.name.endswith('__'+d['mechanism']+'_PIVOT'))
    ammo=next(o for o in col.objects if o.name.endswith('__magazine_PIVOT'))
    receiver=next(o for o in col.objects if o.name.endswith('__receiver_group'))
    bpy.context.view_layer.update()
    generated=[];reload_objects=[];loading=[]
    def role(o):return o.name.split('__')[-1]
    def find(name):return next((o for o in col.objects if role(o)==name),None)
    def remove(o):
        if o:bpy.data.objects.remove(o,do_unlink=True)
    def mesh(name,verts,faces,parent=receiver,band=0):
        me=bpy.data.meshes.new(name);me.from_pydata(verts,[],faces);me.update()
        bm=bmesh.new();bm.from_mesh(me);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(me);bm.free()
        o=bpy.data.objects.new(d['id']+'__'+name,me);col.objects.link(o);o.parent=parent
        # Geometry helpers take weapon-space positions, preserving pivots on parenting.
        o.matrix_parent_inverse=parent.matrix_world.inverted()
        o.matrix_basis=Matrix.Identity(4)
        o['mechanical_role']=name;o['refinement_stage']='geometry review';o['trim_band']=band;generated.append(o)
        return o
    def bevel(o,w=.0012):
        bpy.context.view_layer.objects.active=o
        mod=o.modifiers.new('Manufactured edges','BEVEL');mod.width=w;mod.segments=1
        bpy.ops.object.modifier_apply(modifier=mod.name);return o
    def box(name,center,size,parent=receiver,band=0,bevel_w=.0012):
        x,y,z=center;a,b,c=[v/2 for v in size]
        verts=[(x+sx*a,y+sy*b,z+sz*c) for sx,sy,sz in [(-1,-1,-1),(1,-1,-1),(1,1,-1),(-1,1,-1),(-1,-1,1),(1,-1,1),(1,1,1),(-1,1,1)]]
        o=mesh(name,verts,[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)],parent,band)
        return bevel(o,min(bevel_w,min(size)*.15)) if bevel_w else o
    def loft(name,sections,axis='X',sides=8,parent=receiver,band=0):
        # Sections = (axis position, center a, center b, half a, half b).
        axes={'X':(0,1,2),'Y':(1,2,0),'Z':(2,0,1)};axis_i,a,b=axes[axis];verts=[]
        for pos,ca,cb,ra,rb in sections:
            for j in range(sides):
                angle=math.tau*(j+.5)/sides;v=[0,0,0];v[axis_i]=pos;v[a]=ca+math.cos(angle)*ra;v[b]=cb+math.sin(angle)*rb;verts.append(v)
        faces=[tuple(range(sides-1,-1,-1)),tuple((len(sections)-1)*sides+j for j in range(sides))]
        for k in range(len(sections)-1):
            for j in range(sides):a=k*sides+j;b=k*sides+(j+1)%sides;faces.append((a,b,b+sides,a+sides))
        return mesh(name,verts,faces,parent,band)
    def cylinder(name,center,r,length,axis='X',parent=receiver,band=1,sides=16):
        ai={'X':0,'Y':1,'Z':2}[axis];other={'X':(1,2),'Y':(2,0),'Z':(0,1)}[axis]
        return loft(name,[(center[ai]-length/2,center[other[0]],center[other[1]],r*.91,r*.91),(center[ai]-length/2+.002,center[other[0]],center[other[1]],r,r),(center[ai]+length/2-.002,center[other[0]],center[other[1]],r,r),(center[ai]+length/2,center[other[0]],center[other[1]],r*.91,r*.91)],axis,sides,parent,band)
    def open_tube(name,x0,x1,y,z,outer,inner,parent=receiver,band=1):
        rings=[(x0,outer*.98),(x0+.003,outer),(x1-.003,outer),(x1,outer*.98),(x1,inner),(x0,inner)];n=20;vv=[(x,y+math.cos(j*math.tau/n)*r,z+math.sin(j*math.tau/n)*r) for x,r in rings for j in range(n)];ff=[]
        for k in range(len(rings)):
            kk=(k+1)%len(rings)
            for j in range(n):ff.append((k*n+j,k*n+(j+1)%n,kk*n+(j+1)%n,kk*n+j))
        return mesh(name,vv,ff,parent,band)
    def beam(name,a,b,width,depth,parent=receiver,band=1):
        a=Vector(a);b=Vector(b);direction=(b-a).normalized();u=direction.cross(Vector((0,1,0)))
        if u.length<.01:u=direction.cross(Vector((0,0,1)))
        u.normalize();v=direction.cross(u).normalized();verts=[]
        for p in(a,b):
            for du,dv in[(-1,-1),(1,-1),(1,1),(-1,1)]:verts.append(p+u*du*width/2+v*dv*depth/2)
        return bevel(mesh(name,verts,[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)],parent,band),min(width,depth)*.15)
    def cut_box(body,center,size):
        cut=box('temporary_cut',center,size,bevel_w=0)
        bpy.context.view_layer.objects.active=body;mod=body.modifiers.new('Functional opening','BOOLEAN');mod.operation='DIFFERENCE';mod.solver='EXACT';mod.object=cut
        bpy.ops.object.modifier_apply(modifier=mod.name);generated.remove(cut);remove(cut)
    def bounds(o):
        pts=[o.matrix_world@v.co for v in o.data.vertices];return Vector([min(v[i] for v in pts) for i in range(3)]),Vector([max(v[i] for v in pts) for i in range(3)])
    def preserve_parent(o,p):
        m=o.matrix_world.copy();o.parent=p;o.matrix_world=m
    def empty(name,parent=root):
        o=bpy.data.objects.new(d['id']+'__'+name,None);col.objects.link(o);o.parent=parent;return o
    # A real stepped bore: outer profile, front lip, inner wall and recessed throat.
    def barrel_body(o):
        local=[v.co.copy() for v in o.data.vertices];lo=Vector([min(v[i] for v in local) for i in range(3)]);hi=Vector([max(v[i] for v in local) for i in range(3)])
        # Existing cylinders keep their local-Z axis and carry a rotation on the object.
        # Rebuild in that measured local frame, then bake the original transform once.
        native=max(range(3),key=lambda i:hi[i]-lo[i]);radial=[i for i in range(3) if i!=native]
        length=hi[native]-lo[native];r=max(hi[i]-lo[i] for i in radial)/2;center=(lo+hi)/2;n=16;matrix=o.matrix_world.copy()
        step=min(.012,length*.12);inner=r*.61;depth=min(length*.60,.085)
        start=lo[native];end=hi[native]
        rings=[(start,r*.88),(start+step,r),(end-step,r*.97),(end,r*.82),(end,inner),(end-depth,inner)]
        verts=[]
        for x,rad in rings:
            for j in range(n):
                v=center.copy();v[native]=x;v[radial[0]]+=math.sin(j*math.tau/n)*rad;v[radial[1]]+=math.cos(j*math.tau/n)*rad;verts.append(matrix@v)
        faces=[tuple(range(n-1,-1,-1)),tuple((len(rings)-1)*n+j for j in range(n))]
        for k in range(len(rings)-1):
            for j in range(n):a=k*n+j;b=k*n+(j+1)%n;faces.append((a,b,b+n,a+n))
        me=bpy.data.meshes.new(role(o)+'_physical_bore');me.from_pydata(verts,[],faces);me.update();o.data=me;o.matrix_parent_inverse=o.parent.matrix_world.inverted();o.matrix_basis=Matrix.Identity(4);o['trim_band']=1;o['has_physical_bore']=True
        bm=bmesh.new();bm.from_mesh(me);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(me);bm.free()
        return o
    # Preserve the established lengths and layouts; replace only the coarse functional volumes.
    for o in list(col.objects):
        if o.type!='MESH':continue
        name=role(o)
        if name.endswith('_bore'):remove(o);continue
        if name in('barrel','left_barrel','right_barrel','top_barrel') or name.startswith(('launch_tube_','tube_')):barrel_body(o)
    old=find('receiver')
    if old:
        lo,hi=bounds(old);cx=(lo.x+hi.x)/2;cy=(lo.y+hi.y)/2;z=(lo.z+hi.z)/2;W=(hi.y-lo.y)/2;H=(hi.z-lo.z)/2;L=hi.x-lo.x
        new=loft('receiver_chassis',[(lo.x,cy,z,W*.70,H*.78),(lo.x+L*.14,cy,z,W,H),(hi.x-L*.22,cy,z,W*.94,H*.91),(hi.x,cy,z-.006,W*.72,H*.65)],sides=8)
        remove(old)
        # Actual loading/ejection recess in the near side; the cover is separate machinery.
        portx=cx+L*.10;portz=z+H*.08
        cut_box(new,(portx,cy-W,portz),(L*.21,W*.46,H*.50))
        box('ejection_recess',(portx,cy-W*.83,portz),(L*.19,.003,H*.45),band=3)
        beam('upper_spine',(lo.x+.02,0,hi.z+.004),(hi.x-.025,0,hi.z+.004),.022,.035,band=1)
        for side in(-1,1):
            panel=loft('receiver_service_panel_'+str(side),[(lo.x+L*.12,side*(W*.84+.003),z,.004,H*.55),(hi.x-L*.28,side*(W*.84+.003),z,.004,H*.62)],sides=4,band=0)
            cylinder('panel_fastener_'+str(side),(lo.x+L*.18,side*(W*.84+.006),z),.008,.006,'Y',band=1,sides=8)
    # Palm-sized grip, not a flat slab, with a hollow straight insertion tunnel.
    old=find('grip');grip_bounds=None
    if old:
        lo,hi=bounds(old);grip_bounds=(lo,hi);height=hi.z-lo.z;top=(lo.x+hi.x)/2+.012;bottom=top-.022
        palm=loft('grip_shell',[(hi.z,top,0,(hi.x-lo.x)*.45,.031),(hi.z-height*.30,top-.008,0,.034,.032),(lo.z+.017,bottom,0,.034,.028),(lo.z,bottom,0,.037,.030)],'Z',8,band=3)
        remove(old)
        if idx==0:cut_box(palm,(bottom+.006,0,lo.z+height*.30),(.038,.033,height*.83))
        for side in(-1,1):
            for j in range(3):cut_box(palm,(bottom,side*.0275,lo.z+.030+j*.020),(.034,.007,.0035))
    # One continuous trigger guard with an open, comfortable finger space.
    for name in('guard_bottom','guard_front','trigger'):remove(find(name))
    if grip_bounds:
        lo,hi=grip_bounds;rear=hi.x-.004;front=rear+.097;top=-.009;bottom=-.073
        outer=[(rear,top),(front-.006,top),(front+.009,top-.014),(front-.003,bottom+.008),(front-.018,bottom),(rear-.007,bottom)]
        inner=[(rear+.006,top-.009),(front-.013,top-.009),(front-.002,top-.016),(front-.012,bottom+.017),(front-.023,bottom+.009),(rear+.002,bottom+.009)]
        n=len(outer);verts=[(x,y,z) for y in(-.008,.008) for loop in(outer,inner) for x,z in loop];faces=[]
        for j in range(n):
            k=(j+1)%n;faces.extend([(j,k,n+k,n+j),(2*n+j,3*n+j,3*n+k,2*n+k),(j,2*n+j,2*n+k,k),(n+j,n+k,3*n+k,3*n+j)])
        bevel(mesh('trigger_guard',verts,faces,band=3),.001)
        tx=rear+.035;outline=[(tx-.004,top+.008),(tx+.004,top+.008),(tx+.007,top-.009),(tx+.004,top-.023),(tx-.004,top-.035),(tx-.012,top-.032),(tx-.004,top-.021),(tx-.001,top-.009)];vv=[(x,y,z) for y in(-.005,.005) for x,z in outline];nn=len(outline);ff=[tuple(range(nn-1,-1,-1)),tuple(nn+j for j in range(nn))];ff.extend((j,(j+1)%nn,(j+1)%nn+nn,j+nn) for j in range(nn));mesh('trigger',vv,ff,band=1)
    # Replace the wedge stock with a connected cheek support / lower strut / butt pad.
    old=find('stock')
    if old:
        lo,hi=bounds(old);width=hi.x-lo.x;remove(old)
        beam('stock_upper',(lo.x+.025,0,hi.z-.009),(hi.x+.005,0,hi.z-.009),.041,.052,band=3)
        beam('stock_lower',(lo.x+.026,0,lo.z+.014),(hi.x-.012,0,-.005),.022,.035,band=0)
        box('butt_pad',(lo.x+.014,0,(lo.z+hi.z)/2),(.032,.079,hi.z-lo.z+.018),band=3)
        socketx=bounds(find('receiver_chassis'))[0].x+.018
        box('stock_socket',(socketx,0,.005),(.048,.075,.070),band=0)
    # Reload architecture is authored per family. Empty ammo groups are never accepted.
    family=d['silhouette'];idx=int(d['id'][1:3])-1;method='detachable magazine';axis=(0,0,-.25)
    for o in list(ammo.children):
        if o.type=='MESH':remove(o)
    def mark(o):reload_objects.append(role(o));o['is_reload_object']=True;return o
    def magazine(name,x,y,z,length=.17,width=.070,depth=.060):
        mark(loft(name,[(z,x,y,width*.43,depth*.46),(z-length*.24,x-.004,y,width*.48,depth*.50),(z-length+.010,x-.009,y,width*.52,depth*.50),(z-length,x-.009,y,width*.56,depth*.55)],'Z',8,parent=ammo,band=4))
        mark(box(name+'_floorplate',(x-.009,y,z-length+.005),(width*1.10,depth*1.16,.018),ammo,4))
        mark(box(name+'_feed_lips',(x,y,z+.009),(width*.70,depth*.77,.019),ammo,1))
        box(name+'_well',(x,y,z-.009),(width*1.12,depth*1.16,.030),band=0)
        box(name+'_catch',(x+width*.30,y-depth*.5-.002,z-.024),(.012,.009,.016),band=1)
        loading.append(name+'_well')
    def shell(name,center,length=.092,r=.014,axis_name='X',parent=ammo):
        ai={'X':0,'Y':1,'Z':2}[axis_name];other=[i for i in range(3) if i!=ai];a,b=[center[i] for i in other];p=center[ai]
        mark(loft(name,[(p-length/2,a,b,r*.88,r*.88),(p-length/2+.009,a,b,r,r),(p+length*.29,a,b,r*.94,r*.94),(p+length/2,a,b,r*.48,r*.48)],axis_name,12,parent,4))
    def pack(name,x,y,z,size=(.12,.09,.17)):
        w,dep,h=size;magazine(name,x,y,z+h*.45,h,w,dep)
        for side in(-1,1):mark(box(name+'_pull_ear_'+str(side),(x,side*(dep*.52)+y,z-h*.40),(.044,.015,.032),ammo,1))
    if idx in(2,3):
        magazine('ballistic_box_magazine',.079,0,-.018,.175,.065,.055)
        method='detachable ballistic box magazine forward of trigger guard'
        cut_box(find('ballistic_box_magazine_well'),(.079,0,-.026),(.066,.058,.068))
        cut_box(find('receiver_chassis'),(.079,0,-.026),(.066,.058,.068))
        spine=find('upper_spine');sl,sh=bounds(spine)
        box('front_sight',(sh.x-.016,0,sh.z+.005),(.012,.009,.012),band=1)
        rear=box('rear_sight',(sl.x+.016,0,sh.z+.005),(.017,.029,.014),band=1)
        cut_box(rear,(sl.x+.016,0,sh.z+.011),(.023,.011,.014))
        # The bolt and charging handle are seated inside the open ejection slot.
        for side in(-1,1):cut_box(find('receiver_service_panel_'+str(side)),(.018,side*.050,.055),(.085,.025,.038))
        box('visible_bolt_carrier',(.016,-.046,.055),(.070,.008,.025),action,1)
        cylinder('charging_handle_stem',(.006,-.058,.055),.006,.023,'Y',action,1,12)
        cylinder('charging_handle_knob',(.006,-.073,.055),.010,.012,'Y',action,3,12)
        fore=find('short_foregrip') or find('foregrip')
        if fore:
            fl,fh=bounds(fore);fc=(fl+fh)/2;remove(fore)
            loft('profiled_foregrip',[(fl.z,fc.x,0,.023,.025),(fl.z+.018,fc.x-.004,0,.025,.028),(fh.z,fc.x,0,.024,.028)],'Z',8,band=3)
            box('foregrip_saddle',(fc.x,0,.004),(.060,.062,.029),band=0)
    elif idx==0:
        lo,hi=grip_bounds;magazine('pistol_box_magazine',(lo.x+hi.x)/2-.004,0,hi.z-.020,.139,.034,.030)
        method='grip magazine; complete removable body and floorplate'
    elif idx==1:
        old=find('revolver_cylinder');lo,hi=bounds(old);center=(lo+hi)/2;remove(old)
        # Cylinder has six real chamber bores, not a solid polygonal drum.
        body=cylinder('swing_out_cylinder',center,.055,.112,'X',ammo,0)
        for j in range(6):
            a=j*math.tau/6;y=center.y+math.cos(a)*.034;z=center.z+math.sin(a)*.034
            cutter=cylinder('chamber_cut',(center.x,y,z),.014,.14,'X',receiver,3)
            bpy.context.view_layer.objects.active=body;mod=body.modifiers.new('Six separate chambers','BOOLEAN');mod.operation='DIFFERENCE';mod.object=cutter;bpy.ops.object.modifier_apply(modifier=mod.name);generated.remove(cutter);remove(cutter)
            shell('revolver_round_'+str(j),(center.x,y,z),.085,.0115)
        mark(body);beam('cylinder_crane',(-.067,0,.017),(-.005,0,.017),.015,.018,band=1)
        method='six-round swing-out cylinder';axis=(0,-.16,0);loading.append('cylinder_crane')
    elif idx in(4,5):
        method='break action; full through-bores with coaxial shells and fixed transverse hinge';axis=(-.080,0,0)
        names=['left_barrel','right_barrel']+(['top_barrel'] if idx==5 else []);channels=[]
        for name in names:
            old=find(name);a,b=bounds(old);channels.append(((a.y+b.y)/2,(a.z+b.z)/2,a.x,b.x,(b.y-a.y)/2));remove(old)
        remove(find('breech'));action.matrix_world=Matrix.Translation((.145,0,-.025));ammo.parent=action;ammo.matrix_parent_inverse=Matrix.Identity(4);ammo.matrix_basis=Matrix.Identity(4);bpy.context.view_layer.update()
        def through_tube(name,x0,x1,y,z,outer,inner):
            rings=[(x0,outer*.95),(x0+.008,outer),(x1-.009,outer),(x1,outer*.90),(x1,inner),(x0,inner)];n=20;vv=[(x,y+math.cos(j*math.tau/n)*r,z+math.sin(j*math.tau/n)*r) for x,r in rings for j in range(n)];ff=[]
            for k in range(len(rings)):
                kk=(k+1)%len(rings)
                for j in range(n):ff.append((k*n+j,k*n+(j+1)%n,kk*n+(j+1)%n,kk*n+j))
            return mesh(name,vv,ff,action,1)
        ymax=max(abs(c[0])+c[4] for c in channels)+.012;zmin=min(c[1]-c[4] for c in channels)-.016;zmax=max(c[1]+c[4] for c in channels)+.016;cz=(zmin+zmax)/2;rz=(zmax-zmin)/2/.92388
        # One matched section at the standing/hinged breech seam; taper only aft.
        remove(find('receiver_chassis'));remove(find('ejection_recess'));remove(find('upper_spine'))
        loft('receiver_chassis',[(-.210,0,.040,.045,.052),(-.170,0,.040,.055,.060),(-.045,0,cz,ymax/.92388,rz),(.074,0,cz,ymax/.92388,rz)],'X',8,receiver,0)
        beam('upper_spine',(-.045 if idx==5 else -.190,0,cz+rz*.92388+.006),(.060,0,cz+rz*.92388+.006),.014,.035,receiver,1)
        for side in(-1,1):
            remove(find('receiver_service_panel_'+str(side)))
            loft('receiver_service_panel_'+str(side),[(-.155,side*(.055*.92388+(ymax-.055*.92388)*.015/.125+.002),.040+(cz-.040)*.015/.125,.003,(.060+(rz-.060)*.015/.125)*.50),(-.045,side*(ymax+.002),cz,.003,rz*.50),(.055,side*(ymax+.002),cz,.003,rz*.50)],'X',4,receiver,0)
            fast=find('panel_fastener_'+str(side));fl,fh=bounds(fast);newy=side*(.055*.92388+(ymax-.055*.92388)*.025/.125+.005)
            for v in fast.data.vertices:v.co.y+=newy-(fl.y+fh.y)/2;v.co.z+=.040+(cz-.040)*.025/.125-(fl.z+fh.z)/2
        breech=loft('profiled_breech',[(.080,0,cz,ymax/.92388,rz),(.092,0,cz,ymax/.92388,rz),(.155,0,cz,ymax/.92388,rz)],'X',8,action,0)
        for j,(y,z,x0,x1,rad) in enumerate(channels):
            through_tube(names[j],.080,x1,y,z,rad,.022)
            socket=find('muzzle_'+str(j)+'_SOCKET')
            if socket:socket.parent=action;socket.matrix_parent_inverse=action.matrix_world.inverted();socket.matrix_basis=Matrix.Translation((x1+.012,y,z))
            cutter=cylinder('breech_chamber_cut',(.1175,y,z),.022,.125,'X',receiver,3,20);bpy.context.view_layer.objects.active=breech;m=breech.modifiers.new('Chamber through breech','BOOLEAN');m.operation='DIFFERENCE';m.solver='EXACT';m.object=cutter;bpy.ops.object.modifier_apply(modifier=m.name);generated.remove(cutter);remove(cutter)
            mark(loft('shot_shell_'+str(j),[(.078,y,z,.021,.021),(.084,y,z,.021,.021),(.086,y,z,.019,.019),(.154,y,z,.019,.019)],'X',20,ammo,4))
        box('fixed_hinge_cradle',(.098,0,-.034),(.114,.074,.024),receiver,0)
        beam('fixed_hinge_web',(.055,0,-.030),(.055,0,.003),.023,.056,receiver,0)
        cylinder('breech_hinge',(.145,0,-.025),.017,max(.18,ymax*2+.014),'Y',receiver,1);loading.append('breech_hinge')
    elif idx==6:
        method='bottom loading gate; axial shell withdrawal then straight down through open tray';axis=(0,0,-.105)
        remove(find('pump'));remove(find('shell_tube'))
        tube=open_tube('shell_tube',.100,.450,0,-.045,.019,.0145)
        cylinder('magazine_front_cap',(.453,0,-.045),.020,.012,'X',receiver,1)
        neck=loft('loading_gate_frame',[(.025,0,-.025,.033,.045),(.135,0,-.025,.033,.045)],'X',8,receiver,0)
        cut_box(neck,(.070,0,-.055),(.086,.034,.100));cut_box(find('receiver_chassis'),(.070,0,-.055),(.086,.034,.100))
        cutter=cylinder('magazine_feed_channel',(.126,0,-.045),.0155,.080,'X',receiver,3,20);bpy.context.view_layer.objects.active=neck;m=neck.modifiers.new('Axial channel into magazine','BOOLEAN');m.operation='DIFFERENCE';m.solver='EXACT';m.object=cutter;bpy.ops.object.modifier_apply(modifier=m.name);generated.remove(cutter);remove(cutter)
        pump=loft('pump',[(.185,0,-.045,.047,.035),(.202,0,-.045,.053,.038),(.335,0,-.045,.053,.038),(.355,0,-.045,.047,.035)],'X',8,action,3)
        cutter=cylinder('pump_slide_clearance',(.270,0,-.045),.0205,.220,'X',receiver,3,20);bpy.context.view_layer.objects.active=pump;m=pump.modifiers.new('Pump sleeve around magazine tube','BOOLEAN');m.operation='DIFFERENCE';m.solver='EXACT';m.object=cutter;bpy.ops.object.modifier_apply(modifier=m.name);generated.remove(cutter);remove(cutter)
        for side in(-1,1):
            for x in(.213,.240,.267,.294,.321):cut_box(pump,(x,side*.048,-.045),(.006,.014,.028))
        clamp=loft('front_tube_clamp',[(.414,0,-.005,.045,.085),(.436,0,-.005,.045,.085)],'X',8,receiver,1)
        for z,radius in((.035,.032),(-.045,.018)):
            cutter=cylinder('clamp_tube_cut',(.425,0,z),radius,.065,'X',receiver,3,20);bpy.context.view_layer.objects.active=clamp;m=clamp.modifiers.new('Clamped tube channel','BOOLEAN');m.operation='DIFFERENCE';m.solver='EXACT';m.object=cutter;bpy.ops.object.modifier_apply(modifier=m.name);generated.remove(cutter);remove(cutter)
        mark(loft('reload_shot_shell',[(.117,0,-.045,.013,.013),(.123,0,-.045,.013,.013),(.125,0,-.045,.012,.012),(.183,0,-.045,.012,.012)],'X',20,ammo,4))
        loading.append('loading_gate_frame')
    elif idx in(7,8,17):
        method='removable fluted ammunition drum' if idx!=17 else 'rotary charge drum';axis=(0,-.25,0)
        center=(-.16,-.24,-.035)
        mark(loft('feed_drum_cartridge',[(-.335,-.035,-.16,.128,.128),(-.317,-.035,-.16,.150,.150),(-.170,-.035,-.16,.144,.144),(-.150,-.035,-.16,.130,.130)],'Y',16,ammo,4))
        mark(beam('drum_pull_handle',(-.23,-.336,-.035),(-.09,-.336,-.035),.020,.025,ammo,1))
        cylinder('drum_lock_spindle',center,.039,.21,'Y',receiver,1);loading.append('feed_bridge')
    elif idx in(18,19,20):
        method='rear-loading rocket / removable missile cassette';axis=(-.29,0,0)
        launcher=find('barrel') or find('launch_tube_0');lo,hi=bounds(launcher)
        rear=lo.x+.028;z=.10
        if idx==19:
            for j in range(4):shell('micro_missile_'+str(j),(rear+.15,(j%2-.5)*.16,(j//2-.5)*.16+.08),.28,.043)
            mark(box('missile_cartridge_back',(rear-.010,0,.08),(.030,.33,.33),ammo,4))
        else:shell('rocket_cartridge',(rear+.15,0,z),.30,.069 if idx==18 else .101)
        mark(beam('rear_loader_handle',(rear-.025,-.095,z+.10),(rear-.025,.095,z+.10),.023,.026,ammo,1))
        cylinder('breech_lock_ring',(rear,0,.08 if idx==19 else z),.183 if idx==19 else .111 if idx==18 else .15,.024,'X',receiver,1)
        loading.append('breech_lock_ring')
    elif idx in(21,24):
        method='side-removable six-chamber drum' if idx==21 else 'side-removable flak cartridge';axis=(0,-.24,0)
        center=(.035,0,-.025)
        mark(cylinder('ammunition_drum',center,.122,.18,'X',ammo,4,16))
        for j in range(6):
            a=j*math.tau/6;shell('drum_round_'+str(j),(.03,math.cos(a)*.084,-.025+math.sin(a)*.084),.175,.027)
        box('drum_docking_saddle',(-.045,0,-.078),(.080,.21,.050),band=1);loading.append('drum_docking_saddle')
    elif idx in(22,23):
        method='hinged breech; individual artillery shell';axis=(-.23,0,.055)
        ammo.parent=action;ammo.matrix_world=action.matrix_world.copy()
        # Shell is constructed in the barrel's own launch frame.
        shell_obj=loft('artillery_shell',[(-.18,0,.08,.067,.067),(-.15,0,.08,.084,.084),(.015,0,.08,.081,.081),(.095,0,.08,.020,.020)],'X',16,ammo,4)
        shell_obj.matrix_parent_inverse=Matrix.Identity(4);shell_obj.matrix_basis=Matrix.Identity(4);mark(shell_obj)
        box('breech_hinge_lug',(-.15,0,.06),(.055,.27,.070),band=1);loading.append('breech_hinge_lug')
    elif idx==25:
        method='top-mounted stacked-disc cassette';axis=(0,0,.21)
        for j in range(3):mark(cylinder('spare_disc_'+str(j),(-.080,0,.195+j*.020),.094,.014,'Z',ammo,1,24))
        for side in(-1,1):mark(beam('disc_cassette_rail_'+str(side),(-.18,side*.096,.18),(.02,side*.096,.18),.024,.021,ammo,0))
        mark(box('disc_cassette_lid',(-.08,0,.257),(.214,.22,.019),ammo,0))
        for x in(-.15,0):box('disc_cassette_dock_'+str(x),(x,0,.144),(.026,.11,.040),band=1)
        loading.extend(['disc_cassette_dock_-0.15','disc_cassette_dock_0'])
    elif idx==27:
        method='removable open scrap hopper';axis=(0,0,.23)
        # Open box: sloping connected walls, a real mouth and a floor.
        verts=[(-.15,-.105,.17),(.03,-.105,.17),(.03,.105,.17),(-.15,.105,.17),(-.19,-.125,.34),(.07,-.125,.34),(.07,.125,.34),(-.19,.125,.34)]
        mark(mesh('scrap_hopper_body',verts,[(0,3,2,1),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)],ammo,4))
        for j in range(5):mark(box('visible_salvage_'+str(j),(-.12+j*.035,(-1 if j%2 else 1)*.04,.24+j*.01),(.055,.035,.024),ammo,1))
        box('hopper_feed_neck',(-.06,0,.145),(.095,.11,.070),band=1);loading.append('hopper_feed_neck')
        old=find('feed_drum')
        if old:preserve_parent(old,receiver)
    elif idx in(28,29):
        method='latched twin pressure-tank pack';axis=(0,.26,0)
        for side in(-1,1):
            y=side*.13
            if idx==28:
                mark(loft('pressure_tank_'+str(side),[(.035,-.20,y,.044,.044),(.060,-.20,y,.061,.061),(.29,-.20,y,.061,.061),(.325,-.20,y,.033,.033)],'Z',16,ammo,4))
                mark(cylinder('tank_valve_'+str(side),(-.20,y,.339),.028,.025,'Z',ammo,1))
            else:
                mark(loft('pressure_tank_'+str(side),[(-.30,y,.21,.033,.033),(-.27,y,.21,.055,.055),(-.005,y,.21,.055,.055),(.035,y,.21,.030,.030)],'X',16,ammo,4))
                mark(cylinder('tank_valve_'+str(side),(.047,y,.21),.025,.025,'X',ammo,1))
        mark(beam('tank_pack_crossbar',(-.20,-.13,.27 if idx==28 else .24),(-.20,.13,.27 if idx==28 else .24),.027,.033,ammo,0))
        beam('tank_pack_latch',(-.20,-.17,.11),(-.20,.17,.11),.025,.028,band=1);loading.append('fluid_manifold')
    else:
        method='keyed removable power / reactor cartridge';axis=(0,0,-.24)
        if idx in(16,26,30,31):
            pack('reactor_cartridge',-.19,0,.18,(.13,.115,.14));axis=(0,0,.21)
        else:pack('power_cartridge',-.012,0,-.115,(.092,.080,.19))
    if idx==3:
        old=find('forearm');remove(old)
        guard=loft('profiled_handguard',[(.125,0,.035,.043,.052),(.146,0,.035,.045,.054),(.315,0,.035,.042,.050),(.345,0,.035,.037,.042)],'X',8,band=0)
        bore=cylinder('temporary_handguard_bore',(.235,0,.035),.030,.27,'X',receiver,3,24)
        bpy.context.view_layer.objects.active=guard;mod=guard.modifiers.new('Barrel clearance through handguard','BOOLEAN');mod.operation='DIFFERENCE';mod.solver='EXACT';mod.object=bore;bpy.ops.object.modifier_apply(modifier=mod.name);generated.remove(bore);remove(bore)
        for side in(-1,1):
            for x in(.174,.207,.240,.273,.306):cut_box(guard,(x,side*.039,.042),(.017,.028,.022))
    # Panels and contact collars give long prongs an engineered profile, rather than flat sticks.
    for o in list(col.objects):
        if o.type!='MESH':continue
        name=role(o)
        if name.startswith('prong_'):
            lo,hi=bounds(o);parent=o.parent;remove(o)
            midy=(lo.y+hi.y)/2
            loft(name,[(lo.x,midy,(lo.z+hi.z)/2,.024,.026),(lo.x+.045,midy,(lo.z+hi.z)/2,.020,.022),(hi.x-.025,midy,(lo.z+hi.z)/2,.013,.014),(hi.x,midy,(lo.z+hi.z)/2,.009,.009)],'X',8,parent,1)
        if name=='suspended_core':
            lo,hi=bounds(o);mid=(lo+hi)/2
            for side in(-1,1):beam('core_retention_strut_'+str(side),(mid.x-.065,side*.042,.035),(mid.x+.025,side*.042,.035),.012,.014,band=1)
    # Positive docking seam and a graspable latch on all cartridge types.
    if not reload_objects:raise RuntimeError('No reload geometry: '+d['id'])
    # Preserve current trim only. Reproject additions into the existing source image bands.
    source_image=bpy.data.images.load(str(R/'assets/game/arsenal/working-trim.png'),check_existing=True)
    _=source_image.pixels[0];source_image.pack()
    if source_image is None:raise RuntimeError('Existing working trim missing')
    mats=[]
    for band in range(8):
        m=bpy.data.materials.new('Preserved working trim '+str(band));m.use_nodes=True;tex=m.node_tree.nodes.new('ShaderNodeTexImage');tex.image=source_image
        bs=m.node_tree.nodes['Principled BSDF'];m.node_tree.links.new(tex.outputs['Color'],bs.inputs['Base Color']);bs.inputs['Roughness'].default_value=.62;bs.inputs['Metallic'].default_value=.35 if band!=3 else 0;mats.append(m)
    for o in col.objects:
        if o.type!='MESH':continue
        # Remove numerical slivers from the actual Boolean cut results before export.
        bm=bmesh.new();bm.from_mesh(o.data)
        bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001)
        bmesh.ops.dissolve_degenerate(bm,edges=list(bm.edges),dist=.000001)
        bmesh.ops.triangulate(bm,faces=list(bm.faces))
        tiny=[f for f in bm.faces if f.calc_area()<1e-10]
        if tiny:bmesh.ops.delete(bm,geom=tiny,context='FACES')
        bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(o.data);bm.free();o.data.update()
        band=int(o.get('trim_band',0));o.data.materials.clear();o.data.materials.append(mats[band]);uv=o.data.uv_layers.active or o.data.uv_layers.new(name='PreservedTrimUV')
        lo=Vector([min(v.co[i] for v in o.data.vertices) for i in range(3)]);hi=Vector([max(v.co[i] for v in o.data.vertices) for i in range(3)])
        for p in o.data.polygons:
            p.material_index=0;axes=[i for i in range(3) if i!=max(range(3),key=lambda a:abs(p.normal[a]))];a,b=axes
            for j in p.loop_indices:
                v=o.data.vertices[o.data.loops[j].vertex_index].co;u=(v[a]-lo[a])/max(hi[a]-lo[a],1e-6);vv=(v[b]-lo[b])/max(hi[b]-lo[b],1e-6);uv.data[j].uv=((band+.04+u*.92)/8,.02+.96*vv)
    # Author articulation around visible loading objects. No texture changes or combat changes.
    bpy.context.view_layer.update();drivers=[root,action,ammo];bind={o:o.matrix_basis.copy() for o in drivers}
    for clip,end in [('idle',60),('fire',12),('alternate',24),('reload',60)]:
        for o in drivers:
            basis=bind[o];o.animation_data_create();act=bpy.data.actions.new(ident+'_'+role(o)+'_'+clip);o.animation_data.action=act
            for frame,t in[(1,0),(16 if end==60 else max(2,end//4),.25),(31 if end==60 else max(3,end//2),.5),(46 if end==60 else max(4,end*3//4),.75),(end,1)]:
                pulse=math.sin(math.pi*t);o.matrix_basis=basis
                if clip=='idle' and o==root:o.location.z+=.0015*math.sin(math.tau*t)
                if clip in('fire','alternate'):
                    if o==root:o.location.x-=.012*pulse
                    if o==action and d['mechanism'] in('slide','pump','bolt'):o.location.x-=(.045 if idx==6 else .027)*pulse
                    if o==action and d['mechanism'] in('rotor','rings','disc','cylinder'):o.rotation_euler.x+=t*math.tau if d['mechanism']!='disc' else 0
                if clip=='reload':
                    if o==ammo:
                        if idx==6:o.location+=Vector((-.088 if t in(.25,.5,.75) else 0,0,-.105 if t==.5 else 0))
                        else:o.location+=Vector(axis)*(max(0,1-abs(t-.5)*4) if idx in(4,5) else pulse)
                    if o==action and idx in(4,5):o.rotation_euler.y+=1.02*pulse
                    if o==action and idx in(22,23):o.rotation_euler.y+=.22*pulse
                for path in('location','rotation_euler','scale'):o.keyframe_insert(data_path=path,frame=frame)
            if idx==6 and o==ammo:
                for layer in act.layers:
                    for strip in layer.strips:
                        for bag in strip.channelbags:
                            for curve in bag.fcurves:
                                for key in curve.keyframe_points:key.interpolation='LINEAR'
            tr=o.animation_data.nla_tracks.new();tr.name=clip;tr.strips.new(clip,1,act);tr.mute=True;o.animation_data.action=None;o.matrix_basis=basis
    bpy.context.scene.frame_start=1;bpy.context.scene.frame_end=60;bpy.context.scene.frame_set(1)
    bpy.ops.object.select_all(action='DESELECT')
    for o in col.objects:o.select_set(True)
    for o in drivers:
        for tr in o.animation_data.nla_tracks:tr.mute=False
    path=out/(ident+'.glb')
    bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='NLA_TRACKS',export_extras=True)
    for o in drivers:
        for tr in o.animation_data.nla_tracks:tr.mute=True
        o.matrix_basis=bind[o]
    bpy.context.scene.frame_set(1);bpy.context.view_layer.update()
    bpy.data.libraries.write(str(out/(ident+'.blend')),{bpy.context.scene},compress=True)
    # A handheld component contract has a local mechanical envelope, not a construction plot.
    pts=[o.matrix_world@v.co for o in col.objects if o.type=='MESH' for v in o.data.vertices];lo=[min(v[i] for v in pts) for i in range(3)];hi=[max(v[i] for v in pts) for i in range(3)]
    contracts[ident]={'placement_kind':'snap-component','component_function':'handheld weapon assembly; no building placement or plot claim','local_bounds_z_up':[[x-.002 for x in lo],[x+.002 for x in hi]],'organic':False,'continuous_surfaces':[],'intentional_separate_meshes':[o.name for o in col.objects if o.type=='MESH'],'reload_method':method,'reload_objects':reload_objects,'loading_interfaces':loading,'forbidden_name_tokens':[]}
    record={'id':ident,'path':path.relative_to(R).as_posix(),'category':'weapon-rough-candidate'};records.append(record)
    (out/'contract.json').write_text(json.dumps(contracts[ident],indent=2)+'\n');(out/'contracts.json').write_text(json.dumps({ident:contracts[ident]},indent=2)+'\n');(out/'inspection-input.json').write_text(json.dumps(json.loads((R/'data/structure-quality-policy.json').read_text())['reference_sources']+[record],indent=2)+'\n')
    info={'id':ident,'source_weapon':d['id'],'source_glb_sha256':hashlib.sha256((R/'assets/game/arsenal'/f'{d["id"]}.glb').read_bytes()).hexdigest(),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'status':'UNREVIEWED','reload_method':method,'reload_objects':reload_objects,'loading_interfaces':loading,'new_texture_images':0}
    report.append(info);(out/'design-record.json').write_text(json.dumps(info,indent=2)+'\n');print('REFINED',ident,method,flush=True)
(BASE/'inspection-input.json').write_text(json.dumps(records,indent=2)+'\n')
(BASE/'contracts.json').write_text(json.dumps(contracts,indent=2)+'\n')
(R/'reports/weapon-refinement-build.json').write_text(json.dumps(report,indent=2)+'\n')
