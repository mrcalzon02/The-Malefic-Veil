package veil;

import java.util.*;
import java.awt.geom.*;

/**
 * Authored, redistributable low-poly *interim* gameplay geometry.
 * These are original project meshes, NOT copies/conversions of the
 * Characters_psx_1.2 FBX archive (license + conversion pending).
 * All world-space shapes enter the very same shaded triangle renderer as CC0 urban GLBs.
 */
public final class GameplayMeshes {
    public static final String[] HERO={"hero-warden","hero-medic","hero-shade","hero-occultist"};
    public static final String[] PICKUP={"pickup-health","pickup-sanity","pickup-pistol-ammo",
        "pickup-shells","pickup-cash","pickup-supplies","pickup-medkit"};
    public static final String[] DEVICE={"device-turret","device-healing","device-revival","device-barricade","device-field-camp","device-field-fire","device-field-workbench","device-field-storage"};
    public static final String[] WEAPON={"weapon-handgun","weapon-shotgun"};
    private final Map<String,GlbMesh> meshes=new LinkedHashMap<>();
    private static final class Model {
        final List<GlbMesh.Triangle> tris=new ArrayList<>();
        void tri(float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,int c){
            tris.add(new GlbMesh.Triangle(ax,ay,az,bx,by,bz,cx,cy,cz,c));
        }
        void rect(float[] a,float[] b,float[] c,float[] d,int color){
            tri(a[0],a[1],a[2],b[0],b[1],b[2],c[0],c[1],c[2],color);
            tri(a[0],a[1],a[2],c[0],c[1],c[2],d[0],d[1],d[2],color);
        }
        void box(double x,double y,double z,double sx,double sy,double sz,int c){
            float left=(float)(x-sx/2),right=(float)(x+sx/2);
            float low=(float)(y-sy/2),high=(float)(y+sy/2);
            float front=(float)(z-sz/2),back=(float)(z+sz/2);
            float[] lbf={left,low,front},rbf={right,low,front},rtf={right,high,front},ltf={left,high,front};
            float[] lbb={left,low,back},rbb={right,low,back},rtb={right,high,back},ltb={left,high,back};
            rect(lbf,rbf,rtf,ltf,dim(c,.88));rect(rbb,lbb,ltb,rtb,dim(c,.68));
            rect(lbb,lbf,ltf,ltb,dim(c,.73));rect(rbf,rbb,rtb,rtf,dim(c,.83));
            rect(ltf,rtf,rtb,ltb,c);rect(lbb,rbb,rbf,lbf,dim(c,.50));
        }
        void diamond(double x,double y,double z,double radius,double height,int color){
            float X=(float)x,Y=(float)y,Z=(float)z,r=(float)radius,h=(float)(height/2);
            float[][] ring={{X-r,Y,Z},{X,Y,Z-r},{X+r,Y,Z},{X,Y,Z+r}};
            for(int i=0;i<4;i++){
                float[] a=ring[i],b=ring[(i+1)%4];
                tri(X,Y+h,Z,a[0],a[1],a[2],b[0],b[1],b[2],dim(color,1-.08*i));
                tri(X,Y-h,Z,b[0],b[1],b[2],a[0],a[1],a[2],dim(color,.65));
            }
        }
        void barrel(double x,double y,double z,double radius,double length,int c){
            // Eight-sided low-poly cylinder, its axis points along world Z.
            float near=(float)(z-length/2),far=(float)(z+length/2);
            for(int i=0;i<8;i++){
                double angle=i*Math.PI/4,next=(i+1)*Math.PI/4;
                float xa=(float)(x+radius*Math.cos(angle)),xb=(float)(x+radius*Math.cos(next));
                float ya=(float)(y+radius*Math.sin(angle)),yb=(float)(y+radius*Math.sin(next));
                rect(new float[]{xa,ya,near},new float[]{xb,yb,near},
                    new float[]{xb,yb,far},new float[]{xa,ya,far},dim(c,.64+.035*i));
                tri((float)x,(float)y,near,xb,yb,near,xa,ya,near,dim(c,.32));
            }
        }
        GlbMesh done(){return GlbMesh.authored(tris);}
    }
    private static int dim(int rgb,double f){
        return (Math.min(255,(int)(((rgb>>>16)&255)*f))<<16)|
            (Math.min(255,(int)(((rgb>>>8)&255)*f))<<8)|
            Math.min(255,(int)((rgb&255)*f));
    }
    public GameplayMeshes(){
        int[] coats={0x536D8C,0xD0C5AE,0x343B55,0x5B3D73};
        int[] trims={0xC2AD59,0x5DA993,0x806BA8,0xA95F9A};
        for(int i=0;i<4;i++){
            Model p=new Model();int coat=coats[i],trim=trims[i];
            p.box(-.15,.35,0,.20,.70,.25,dim(coat,.55));
            p.box(.15,.35,0,.20,.70,.25,dim(coat,.52));
            p.box(0,1.08,0,.60,.81,.32,coat);
            p.box(-.40,1.05,0,.17,.70,.22,dim(coat,.78));
            p.box(.40,1.05,0,.17,.70,.22,dim(coat,.78));
            p.box(0,1.66,0,.39,.37,.36,0x9C8170);
            p.box(0,1.86,0,.42,.12,.38,dim(coat,.57));
            p.box(0,1.37,-.17,.40,.13,.03,trim);
            p.box(0,.72,.16,.45,.15,.12,dim(trim,.68));
            if(i==0){ // Warden: heavy shoulder armor and hard helmet
                p.box(-.40,1.46,0,.30,.23,.35,0x737D87);
                p.box(.40,1.46,0,.30,.23,.35,0x737D87);
                p.box(0,1.84,-.20,.48,.08,.11,0xD0BC6F);
            }else if(i==1){ // Medic: coat, cross, satchel
                p.box(0,.92,-.19,.22,.22,.035,0xBC4D52);
                p.box(0,.92,-.21,.07,.36,.035,0xBC4D52);
                p.box(.40,.87,.15,.19,.34,.27,0x576A54);
            }else if(i==2){ // Shade: dark hood and pale narrow visor
                p.box(0,1.68,-.23,.43,.27,.15,0x27273B);
                p.box(0,1.67,-.31,.26,.045,.025,0x8392A9);
            }else{ // Occultist: mantle and occult gem
                p.box(0,1.36,.18,.72,.19,.10,0x281A40);
                p.diamond(0,1.19,-.21,.11,.23,0xA858D2);
            }
            meshes.put(HERO[i],p.done());
        }
        Model enemy=new Model();
        enemy.box(0,.91,0,.55,.94,.34,0x765154);
        enemy.box(-.16,.27,0,.16,.54,.20,0x4C3D42);
        enemy.box(.16,.27,0,.16,.54,.20,0x4C3D42);
        enemy.box(-.40,1.08,-.03,.16,.65,.18,0x7B5351);
        enemy.box(.40,1.08,-.03,.16,.65,.18,0x7B5351);
        enemy.box(0,1.62,-.035,.38,.40,.39,0xA58C7F);
        enemy.box(-.10,1.67,-.238,.08,.05,.02,0xEA3B42);
        enemy.box(.10,1.67,-.238,.08,.05,.02,0xEA3B42);
        meshes.put("enemy-walker",enemy.done());

        Model handgun=new Model();
        handgun.box(0,.13,0,.23,.17,.40,0x55575C);
        handgun.box(0,.20,-.22,.20,.16,.35,0x777D80);
        handgun.box(0,-.04,.14,.14,.35,.15,0x373137);
        handgun.box(0,.13,-.42,.08,.05,.10,0xA9A7A0);
        meshes.put(WEAPON[0],handgun.done());

        Model shotgun=new Model();
        shotgun.box(0,.06,.20,.17,.22,.75,0x6A4533);
        shotgun.box(0,.18,-.17,.22,.15,.55,0x4F5559);
        shotgun.barrel(0,.23,-.60,.087,.73,0x636970);
        shotgun.box(0,-.07,.37,.14,.21,.22,0x45382D);
        meshes.put(WEAPON[1],shotgun.done());

        Model hp=new Model();hp.box(0,.25,0,.46,.47,.28,0xCFD2C5);
        hp.box(0,.26,-.155,.12,.35,.024,0xB54947);
        hp.box(0,.26,-.18,.34,.12,.024,0xB54947);
        hp.box(0,.53,0,.30,.10,.13,0x6B7471);
        meshes.put(PICKUP[0],hp.done());
        Model sanity=new Model();sanity.diamond(0,.34,0,.31,.68,0x8496E7);
        sanity.box(0,.06,0,.33,.09,.33,0x3B3B65);meshes.put(PICKUP[1],sanity.done());
        Model ammo=new Model();ammo.box(0,.18,0,.46,.28,.32,0x777855);
        for(int i=0;i<4;i++)ammo.barrel(-.15+i*.10,.35,-.04,.039,.15,0xB99A53);
        meshes.put(PICKUP[2],ammo.done());
        Model shells=new Model();shells.box(0,.11,0,.48,.21,.35,0x876442);
        for(int i=0;i<4;i++)shells.barrel(-.15+i*.10,.25,-.03,.045,.13,0xC33C33);
        meshes.put(PICKUP[3],shells.done());
        Model cash=new Model();cash.box(0,.12,0,.58,.16,.38,0xA09253);
        cash.box(0,.23,0,.51,.07,.32,0xD8C578);meshes.put(PICKUP[4],cash.done());
        Model supplies=new Model();supplies.box(0,.22,0,.48,.43,.45,0x936F48);
        supplies.box(0,.22,-.235,.38,.07,.025,0xD7B770);
        supplies.box(0,.22,.235,.38,.07,.025,0xD7B770);
        meshes.put(PICKUP[5],supplies.done());
        Model kit=new Model();kit.box(0,.25,0,.45,.38,.30,0xADAD9E);
        kit.box(0,.30,-.17,.36,.10,.023,0xDC4C54);
        kit.box(0,.30,-.19,.10,.28,.023,0xDC4C54);
        kit.box(0,.48,0,.22,.10,.14,0x696D64);
        meshes.put(PICKUP[6],kit.done());

        Model turret=new Model();
        turret.box(0,.22,0,.60,.45,.60,0x53636E);
        turret.box(0,.52,0,.47,.21,.42,0x7F8589);
        turret.barrel(-.11,.56,-.37,.068,.49,0xA0A1A2);
        turret.barrel(.11,.56,-.37,.068,.49,0xA0A1A2);
        meshes.put(DEVICE[0],turret.done());
        Model healing=new Model();
        healing.box(0,.30,0,.62,.58,.50,0x728D83);
        healing.box(0,.64,0,.38,.13,.39,0xBED7B7);
        healing.box(0,.35,-.27,.10,.31,.02,0xDDEDE0);
        healing.box(0,.35,-.29,.31,.10,.02,0xDDEDE0);
        meshes.put(DEVICE[1],healing.done());
        Model revive=new Model();
        revive.box(0,.15,0,.80,.28,.72,0x604F83);
        revive.box(-.29,.55,0,.14,.78,.16,0x8C72B8);
        revive.box(.29,.55,0,.14,.78,.16,0x8C72B8);
        revive.diamond(0,1.02,0,.30,.35,0xB68AEE);
        meshes.put(DEVICE[2],revive.done());
        Model barricade=new Model();
        barricade.box(0,.53,0,1.35,1.04,.18,0x847E70);
        barricade.box(0,.53,-.11,1.17,.18,.04,0xC6A963);
        barricade.box(0,.18,.12,1.44,.12,.24,0x5A6068);
        meshes.put(DEVICE[3],barricade.done());
        // Crude, constructed field camp / claim stake. Actual 3D canvas tarp,
        // timber supports, ground stakes and a supply crate—not a UI marker.
        Model camp=new Model();
        camp.box(0,.04,0,2.30,.08,1.80,0x564934);
        for(int side=-1;side<=1;side+=2){
            camp.box(side*.97,.48,.60,.10,.95,.12,0x6B5238);
            camp.box(side*.97,.48,-.60,.10,.95,.12,0x6B5238);
            camp.box(side*.97,1.00,0,.11,.09,1.54,0x8A714B);
            for(int sign=-1;sign<=1;sign+=2)
                camp.tri(0,1.48f,0,side*1.04f,.07f,sign*.89f,
                    0,.09f,sign*.90f,sign>0?0x7B6950:0x625545);
        }
        camp.box(0,1.49,0,.13,.13,1.97,0x635039);
        camp.box(.81,.22,-.48,.48,.37,.44,0x876443);
        camp.box(.81,.27,-.71,.40,.09,.06,0xB19668);
        camp.box(-1.24,.75,0,.14,1.50,.14,0x725A3F);
        camp.box(-1.24,1.42,.15,.39,.19,.08,0x957349);
        meshes.put(DEVICE[4],camp.done());
        Model fieldFire=new Model();
        for(int i=0;i<8;i++){
            double a=i*Math.PI/4;
            fieldFire.box(Math.cos(a)*.40,.115,Math.sin(a)*.40,.23,.23,.20,0x77716B);
        }
        fieldFire.box(-.12,.20,0,.63,.14,.15,0x62432D);
        fieldFire.box(.12,.21,0,.16,.14,.60,0x715038);
        fieldFire.diamond(0,.46,0,.20,.67,0xE9A23C);
        fieldFire.diamond(.07,.42,-.04,.11,.43,0xF3CB65);
        meshes.put(DEVICE[5],fieldFire.done());
        // Real deployable E0 workstation; one original low-poly authored
        // solid mesh with tabletop, braced legs, tool roll and clamp.
        Model bench=new Model();
        bench.box(0,.70,0,1.55,.16,.75,0x8C6C49);
        bench.box(0,.80,0,1.62,.055,.80,0xB49266);
        for(int side=-1;side<=1;side+=2)
            for(int back=-1;back<=1;back+=2)
                bench.box(side*.61,.37,back*.24,.13,.72,.15,0x614632);
        bench.box(0,.27,0,1.30,.12,.52,0x564832);
        bench.box(-.41,.90,-.08,.22,.18,.27,0x54636C);
        bench.box(.33,.90,.10,.38,.06,.16,0x857F72);
        bench.box(.55,.91,-.18,.13,.20,.09,0xB9AB89);
        meshes.put(DEVICE[6],bench.done());
        Model storage=new Model();
        storage.box(0,.12,0,1.4,.2,1.05,0x59422F);
        storage.box(0,.62,0,1.4,.8,1.05,0x816040);
        storage.box(0,1.08,0,1.5,.14,1.14,0xA5855E);
        for(int side=-1;side<=1;side+=2){
            storage.box(side*.63,.62,0,.10,.85,1.12,0x4D4D49);
            storage.box(0,.62,side*.47,1.48,.85,.08,0x66645A);
        }
        storage.box(0,1.18,0,1.1,.05,.08,0x483928);
        meshes.put(DEVICE[7],storage.done());

        // Reuse selected CC0 Beckenburg models, not the full source library.
        try {
            for(String name:new String[]{"broadleaf","conifer"}){
                byte[] source=java.nio.file.Files.readAllBytes(java.nio.file.Path.of(
                    "assets","game","beckenburg","level_tree_"+name+".glb"));
                GlbMesh tree=GlbMesh.read(source);
                if(tree.triangleCount()<50)throw new java.io.IOException("Empty imported tree");
                meshes.put("resource-tree-"+name,tree);
            }
        }catch(java.io.IOException exception){
            throw new IllegalStateException("Required Beckenburg CC0 resource trees missing",exception);
        }
        Model stone=new Model();
        stone.diamond(-.19,.27,0,.31,.50,0x74767A);
        stone.diamond(.22,.20,.13,.23,.37,0x9C9B91);
        stone.diamond(.02,.13,-.23,.19,.27,0x505C5F);
        meshes.put("resource-stone",stone.done());
        Model ore=new Model();
        ore.diamond(-.12,.32,0,.35,.64,0x444A54);
        ore.diamond(.23,.22,.12,.24,.42,0x5B6877);
        ore.diamond(-.13,.41,-.20,.10,.18,0xC2A04B);
        ore.diamond(.20,.35,-.08,.09,.16,0xCFAE65);
        meshes.put("resource-ore",ore.done());
        Model fiber=new Model();
        for(int i=0;i<7;i++){
            double a=i*Math.PI*2/7;
            fiber.box(Math.cos(a)*.16,.27,Math.sin(a)*.16,
                .044,.54+(i%3)*.14,.043,i%2==0?0x758657:0x9D9363);
        }
        fiber.box(0,.17,0,.23,.13,.19,0x6E583D);
        meshes.put("resource-fiber",fiber.done());
        Model forage=new Model();
        forage.box(0,.18,0,.10,.35,.09,0x5D6D38);
        for(int i=0;i<6;i++){
            double a=i*Math.PI/3;
            forage.diamond(Math.cos(a)*.23,.31+((i%3)*.04),Math.sin(a)*.23,
                .18,.17,0x436D43);
            forage.diamond(Math.cos(a)*.22,.23,Math.sin(a)*.22,.055,.10,
                i%2==0?0xB24D3F:0xBE8B45);
        }
        meshes.put("resource-edibles",forage.done());
        Model herbs=new Model();
        herbs.box(0,.18,0,.09,.35,.09,0x5E6641);
        for(int i=0;i<6;i++){
            double a=i*Math.PI/3;
            herbs.diamond(Math.cos(a)*.15,.32,Math.sin(a)*.15,.15,.30,0x4B8261);
            herbs.diamond(Math.cos(a)*.17,.52,Math.sin(a)*.17,.075,.13,0xAC758B);
        }
        meshes.put("resource-herbs",herbs.done());
        Model water=new Model();
        water.box(0,.045,0,.85,.075,.72,0x4D6561);
        water.box(.08,.091,.04,.49,.011,.45,0x557F86);
        water.diamond(-.17,.12,.02,.09,.06,0x7A9C9B);
        meshes.put("resource-water",water.done());

        meshes.putAll(ThrownDeployableMeshes.build());
        // Nine real, authored static GLBs are immediately available to the
        // common shaded-triangle renderer under stable airframe IDs. There are
        // deliberately no invented flight actors or aircraft-spawn shortcuts.
        try {
            meshes.putAll(AirPowerMeshes.load(java.nio.file.Path.of(".")));
            meshes.putAll(VeilWeather.loadMeshes(java.nio.file.Path.of(".")));
        }catch(java.io.IOException error){
            throw new IllegalStateException("Required PSX faction aircraft invalid or missing",error);
        }
    }
    public Map<String,GlbMesh> meshes(){return Collections.unmodifiableMap(meshes);}
}
